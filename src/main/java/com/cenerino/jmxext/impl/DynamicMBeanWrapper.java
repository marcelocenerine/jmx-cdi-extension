package com.cenerino.jmxext.impl;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

class DynamicMBeanWrapper implements DynamicMBean {

    private Object instance;
    private MBeanInfo mBeanInfo;
    private Map<String, MBeanAttributeInfo> fields = new HashMap<>();

    public DynamicMBeanWrapper(Object instance, String description) throws IntrospectionException {
        this.instance = instance;
        populateFields();
        this.mBeanInfo = new MBeanInfo(instance.getClass().getName(), description, fields.values().toArray(new MBeanAttributeInfo[fields.size()]), null, null, null);
    }

    private void populateFields() throws IntrospectionException {
        Class<?> clazz = instance.getClass();

        for (Field field : FieldUtils.getAllFields(clazz)) {
            String capitalizedFieldName = StringUtils.capitalize(field.getName());
            Class<?> fieldType = field.getType();
            boolean hasGetMethod = findNonStaticPublicMethod(clazz, "get" + capitalizedFieldName, fieldType) != null;
            boolean hasSetMethod = findNonStaticPublicMethod(clazz, "set" + capitalizedFieldName, void.class, fieldType) != null;
            boolean hasIsMethod = asList(boolean.class, Boolean.class).contains(fieldType) && findNonStaticPublicMethod(clazz, "is" + capitalizedFieldName, fieldType) != null;
            boolean isReadable = hasGetMethod || hasIsMethod;
            boolean isWritable = hasSetMethod;

            if (hasGetMethod || hasIsMethod || hasSetMethod) {
                fields.put(field.getName(), new MBeanAttributeInfo(field.getName(), fieldType.getName(), null, isReadable, isWritable, hasIsMethod));
            }
        }
    }

    private static Method findNonStaticPublicMethod(Class<?> clazz, String methodName, Class<?> returnType, Class<?>... parameterTypes) {
        try {
            Method method = clazz.getMethod(methodName, parameterTypes);

            if (method != null 
                    && method.getReturnType().equals(returnType) 
                    && isPublic(method.getModifiers()) 
                    && !isStatic(method.getModifiers())) {
                return method;
            }
        } catch (NoSuchMethodException | SecurityException e) {}

        return null;
    }

    @Override
    public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
        validateAttributeExistsAndIsReadable(attribute);

        try {
            return FieldUtils.readField(instance, attribute, true);
        } catch (IllegalAccessException e) {
            throw new ReflectionException(e);
        }
    }

    @Override
    public AttributeList getAttributes(String[] attributes) {
        AttributeList result = new AttributeList();

        for (String attribute : attributes) {
            try {
                result.add(new Attribute(attribute, getAttribute(attribute)));
            } catch (Exception e) {}
        }

        return result;
    }

    private void validateAttributeExistsAndIsReadable(String attribute) throws AttributeNotFoundException {
        if (isBlank(attribute))
            throw new IllegalArgumentException("Attribute name cannot be null.");

        MBeanAttributeInfo attributeInfo = fields.get(attribute);

        if (attributeInfo == null || !attributeInfo.isReadable())
            throw new AttributeNotFoundException("Attribute '" + attribute + "' does not exist or is not readable.");
    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        validateAttributeExistsAndIsWritable(attribute.getName());

        try {
            FieldUtils.writeField(instance, attribute.getName(), attribute.getValue(), true);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new InvalidAttributeValueException(String.format("Cannot set attribute '%s' to '%s'. Error: %s.", attribute.getName(), attribute.getValue(), e.getMessage()));
        }
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        AttributeList result = new AttributeList();

        for (Attribute attribute : attributes.asList()) {
            try {
                setAttribute(attribute);
                result.add(attribute);
            } catch (Exception e) {}
        }

        return result;
    }

    private void validateAttributeExistsAndIsWritable(String attribute) throws AttributeNotFoundException {
        if (isBlank(attribute))
            throw new IllegalArgumentException("Attribute name cannot be null.");

        MBeanAttributeInfo attributeInfo = fields.get(attribute);

        if (attributeInfo == null || !attributeInfo.isWritable())
            throw new AttributeNotFoundException("Attribute '" + attribute + "' does not exist or is not writable.");
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return mBeanInfo;
    }
}
