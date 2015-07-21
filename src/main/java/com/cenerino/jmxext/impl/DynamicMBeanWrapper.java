package com.cenerino.jmxext.impl;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;

import org.apache.commons.lang3.reflect.FieldUtils;

import com.cenerino.jmxext.MBean;

class DynamicMBeanWrapper implements DynamicMBean {

    private Bean<?> bean;
    private BeanManager beanManager;
    private Map<String, MBeanAttributeInfo> mBeanAttributes = new HashMap<>();
    private Map<String, AttributeAccessor> mBeanAttributeAccessors = new HashMap<>();
    private List<MBeanOperationInfo> mBeanOperations = new ArrayList<>();

    public DynamicMBeanWrapper(Bean<?> bean, BeanManager beanManager) throws IntrospectionException {
        this.bean = bean;
        this.beanManager = beanManager;
        loadAttributesAndAccessorMethods();
        loadOperations();
    }

    private void loadAttributesAndAccessorMethods() throws IntrospectionException {
        Class<?> clazz = bean.getBeanClass();

        for (Field field : FieldUtils.getAllFields(clazz)) {
            Class<?> fieldType = field.getType();
            String capitalizedFieldName = capitalize(field.getName());
            Method getMethod = findNonStaticPublicMethod(clazz, "get" + capitalizedFieldName, fieldType);
            Method setMethod = findNonStaticPublicMethod(clazz, "set" + capitalizedFieldName, void.class, fieldType);
            Method isMethod = isBooleanType(fieldType) ? findNonStaticPublicMethod(clazz, "is" + capitalizedFieldName, fieldType) : null;
            boolean isReadable = getMethod != null || isMethod != null;
            boolean isWritable = setMethod != null;

            if (isReadable || isWritable) {
                mBeanAttributes.put(field.getName(), new MBeanAttributeInfo(field.getName(), fieldType.getName(), null, isReadable, isWritable, isMethod != null));
                mBeanAttributeAccessors.put(field.getName(), new AttributeAccessor(defaultIfNull(getMethod, isMethod), setMethod));
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

    private static boolean isBooleanType(Class<?> fieldType) {
        return asList(boolean.class, Boolean.class).contains(fieldType);
    }

    private void loadOperations() {
        for (Method method : bean.getBeanClass().getMethods()) {
            if(isPublic(method.getModifiers())
                    && !isStatic(method.getModifiers())
                    && !method.getDeclaringClass().equals(Object.class)) {
                mBeanOperations.add(new MBeanOperationInfo(method.getName(), method));
            }
        }
    }

    @Override
    public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
        validateAttributeExistsAndIsReadable(attribute);

        try {
            return mBeanAttributeAccessors.get(attribute).getter.invoke(instance());
        } catch (IllegalAccessException | InvocationTargetException e) {
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

        MBeanAttributeInfo attributeInfo = mBeanAttributes.get(attribute);

        if (attributeInfo == null || !attributeInfo.isReadable())
            throw new AttributeNotFoundException("Attribute '" + attribute + "' does not exist or is not readable.");
    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        validateAttributeExistsAndIsWritable(attribute.getName());

        try {
            mBeanAttributeAccessors.get(attribute.getName()).setter.invoke(instance(), attribute.getValue());
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
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

        MBeanAttributeInfo attributeInfo = mBeanAttributes.get(attribute);

        if (attributeInfo == null || !attributeInfo.isWritable())
            throw new AttributeNotFoundException("Attribute '" + attribute + "' does not exist or is not writable.");
    }

    @Override
    public Object invoke(String actionName, Object[] args, String[] signature) throws MBeanException, ReflectionException {
        try {
            Object instance = instance();
            return instance.getClass().getMethod(actionName, getArgumentTypes(args)).invoke(instance, args);
        } catch (Exception e) {
            throw new MBeanException(e);
        }
    }

    private static Class<?>[] getArgumentTypes(Object...args) {
        return Stream.of(args).map(param -> param.getClass()).toArray(Class<?>[]::new);
    }

    private Object instance() {
        Class<?> clazz = bean.getBeanClass();
        Annotation[] qualifiers = selectQualifiers(clazz.getDeclaredAnnotations());
        Set<Bean<?>> beans = beanManager.getBeans(clazz, qualifiers);
        Bean<?> resolved = beanManager.resolve(beans);
        CreationalContext<?> creationalContext = beanManager.createCreationalContext(resolved);
        return beanManager.getReference(resolved, clazz, creationalContext);
    }

    private Annotation[] selectQualifiers(Annotation[] annotations) {
        return Stream.of(annotations).filter(annotation -> beanManager.isQualifier(annotation.annotationType())).toArray(Annotation[]::new);
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        Class<?> clazz = bean.getBeanClass();
        String description = clazz.getAnnotation(MBean.class).description();
        MBeanAttributeInfo[] attributes = mBeanAttributes.values().toArray(new MBeanAttributeInfo[mBeanAttributes.size()]);
        MBeanOperationInfo[] operations = mBeanOperations.toArray(new MBeanOperationInfo[mBeanOperations.size()]);

        return new MBeanInfo(clazz.getName(), description, attributes, null, operations, null);
    }

    private static class AttributeAccessor {

        private Method getter;
        private Method setter;

        public AttributeAccessor(Method getter, Method setter) {
            this.getter = getter;
            this.setter = setter;
        }
    }
}