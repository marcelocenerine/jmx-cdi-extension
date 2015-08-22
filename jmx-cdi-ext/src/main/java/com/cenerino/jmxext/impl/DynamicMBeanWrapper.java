package com.cenerino.jmxext.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
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
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cenerino.jmxext.MBean;

class DynamicMBeanWrapper implements DynamicMBean {

    private static final Logger logger = LoggerFactory.getLogger(DynamicMBeanWrapper.class);
    private Class<?> beanClass;
    private BeanManager beanManager;
    private Map<String, MBeanAttributeInfo> mBeanAttributes = new HashMap<>();
    private Map<String, AttributeAccessor> mBeanAttributeAccessors = new HashMap<>();
    private List<MBeanOperationInfo> mBeanOperations = new ArrayList<>();

    public DynamicMBeanWrapper(Bean<?> bean, BeanManager beanManager) throws IntrospectionException {
        this.beanClass = bean.getBeanClass();
        this.beanManager = beanManager;
        BeanInfo beanInfo = Introspector.getBeanInfo(beanClass, Object.class);
        loadAttributesAndAccessorMethods(beanInfo);
        loadOperations(beanInfo);
    }

    private void loadAttributesAndAccessorMethods(BeanInfo beanInfo) {
        for (PropertyDescriptor prop : beanInfo.getPropertyDescriptors()) {
            Method getter = prop.getReadMethod();
            Method setter = prop.getWriteMethod();
            boolean isReadable = getter != null;
            boolean isWritable = setter != null;
            boolean isIs = isReadable && getter.getName().startsWith("is");

            if (isReadable || isWritable) {
                MBeanAttributeInfo attributeInfo = new MBeanAttributeInfo(prop.getName(), prop.getPropertyType().getName(), null, isReadable, isWritable, isIs);
                logger.debug("Exposing attribute {}", attributeInfo);
                mBeanAttributes.put(prop.getName(), attributeInfo);
                mBeanAttributeAccessors.put(prop.getName(), new AttributeAccessor(getter, setter));
            }
        }
    }

    private void loadOperations(BeanInfo beanInfo) {
        for (MethodDescriptor methodDescr : beanInfo.getMethodDescriptors()) {
            MBeanOperationInfo operationInfo = new MBeanOperationInfo(methodDescr.getName(), methodDescr.getMethod());
            logger.debug("Exposing operation {}", operationInfo);
            mBeanOperations.add(operationInfo);
        }
    }

    @Override
    public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
        validateAttributeExistsAndIsReadable(attribute);

        try {
            return mBeanAttributeAccessors.get(attribute).getter.invoke(instance());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ReflectionException(e, "Attribute '" + attribute + "' could not be read.");
        }
    }

    @Override
    public AttributeList getAttributes(String[] attributes) {
        AttributeList result = new AttributeList();

        for (String attribute : attributes) {
            try {
                result.add(new Attribute(attribute, getAttribute(attribute)));
            } catch (Exception e) {
                logger.error("Error to read attribute. It will not be added to the resulting list.", e);
            }
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
            } catch (Exception e) {
                logger.error("Error to set attribute value. It will not be added to the resulting list.", e);
            }
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
        Annotation[] qualifiers = selectQualifiers(beanClass.getDeclaredAnnotations());
        Set<Bean<?>> beans = beanManager.getBeans(beanClass, qualifiers);
        Bean<?> resolved = beanManager.resolve(beans);
        CreationalContext<?> creationalContext = beanManager.createCreationalContext(resolved);
        return beanManager.getReference(resolved, beanClass, creationalContext);
    }

    private Annotation[] selectQualifiers(Annotation[] annotations) {
        return Stream.of(annotations).filter(annotation -> beanManager.isQualifier(annotation.annotationType())).toArray(Annotation[]::new);
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        String description = beanClass.getAnnotation(MBean.class).description();
        MBeanAttributeInfo[] attributes = mBeanAttributes.values().toArray(new MBeanAttributeInfo[mBeanAttributes.size()]);
        MBeanOperationInfo[] operations = mBeanOperations.toArray(new MBeanOperationInfo[mBeanOperations.size()]);
        return new MBeanInfo(beanClass.getName(), description, attributes, null, operations, null);
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