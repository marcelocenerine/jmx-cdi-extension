package com.cenerino.jmxext.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private BeanManager beanManager;
    private Class<?> beanClass;
    private Map<String, PropertyDescriptor> exposedAttributes = new LinkedHashMap<>();
    private List<Method> exposedMethods = new ArrayList<>();
    private MBeanInfo mbeanInfo;

    public DynamicMBeanWrapper(Bean<?> bean, BeanManager beanManager) throws IntrospectionException {
        this.beanClass = bean.getBeanClass();
        this.beanManager = beanManager;
        BeanInfo beanInfo = Introspector.getBeanInfo(beanClass, Object.class);
        loadProperties(beanInfo);
        loadOperations(beanInfo);
        createMBeanInfo();
    }

    private void loadProperties(BeanInfo beanInfo) {
        Stream.of(beanInfo.getPropertyDescriptors())
        .filter(prop -> isReadable(prop) || isWritable(prop))
        .forEach(prop -> {
            logger.debug("Attribute '{}' will be exposed in the MBean.", prop.getName());
            exposedAttributes.put(prop.getName(), prop);
        });
    }

    private void loadOperations(BeanInfo beanInfo) {
        Stream.of(beanInfo.getMethodDescriptors())
        .forEach(methodDesc -> {
            logger.debug("Method '{}' will be exposed in the MBean.", methodDesc.getName());
            exposedMethods.add(methodDesc.getMethod());
        });
    }

    private void createMBeanInfo() {
        String description = beanClass.getAnnotation(MBean.class).description();
        MBeanAttributeInfo[] attributes = getAttributeInfos();
        MBeanOperationInfo[] operations = getOperationInfos();
        mbeanInfo = new MBeanInfo(beanClass.getName(), description, attributes, null, operations, null);
    }

    private MBeanOperationInfo[] getOperationInfos() {
        return exposedMethods.stream()
                .map(method -> new MBeanOperationInfo(method.getName(), method))
                .toArray(MBeanOperationInfo[]::new);
    }

    private MBeanAttributeInfo[] getAttributeInfos() {
        return exposedAttributes.values().stream()
                .map(p -> new MBeanAttributeInfo(p.getName(), p.getPropertyType().getName(), null, isReadable(p), isWritable(p), isIs(p)))
                .toArray(MBeanAttributeInfo[]::new);
    }

    private static boolean isWritable(PropertyDescriptor property) {
        return property.getWriteMethod() != null;
    }

    private static boolean isReadable(PropertyDescriptor property) {
        return property.getReadMethod() != null;
    }

    private static boolean isIs(PropertyDescriptor property) {
        return isReadable(property) && property.getReadMethod().getName().startsWith("is");
    }

    @Override
    public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
        validateAttributeExistsAndIsReadable(attribute);

        try {
            return exposedAttributes.get(attribute).getReadMethod().invoke(instance());
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

        PropertyDescriptor property = exposedAttributes.get(attribute);

        if (property == null || !isReadable(property))
            throw new AttributeNotFoundException("Attribute '" + attribute + "' does not exist or is not readable.");
    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        validateAttributeExistsAndIsWritable(attribute.getName());

        try {
            exposedAttributes.get(attribute.getName()).getWriteMethod().invoke(instance(), attribute.getValue());
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new InvalidAttributeValueException(String.format("Cannot set attribute '%s'. Error: %s.", attribute, e.getMessage()));
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

        PropertyDescriptor property = exposedAttributes.get(attribute);

        if (property == null || !isWritable(property))
            throw new AttributeNotFoundException("Attribute '" + attribute + "' does not exist or is not writable.");
    }

    @Override
    public Object invoke(String methodName, Object[] args, String[] signature) throws MBeanException, ReflectionException {
        Optional<Method> method = exposedMethods.stream().filter(m -> methodMatches(m, methodName, signature)).findFirst();

        if (!method.isPresent())
            throw new IllegalArgumentException(String.format("Method '%s' with arg types %s not found.", methodName, Arrays.toString(signature)));

        try {
            return method.get().invoke(instance(), args);
        } catch (Exception e) {
            throw new MBeanException(e);
        }
    }

    private boolean methodMatches(Method method, String name, String[] parameterTypes) {
        if (!method.getName().equals(name)) return false;

        String[] signature = Stream.of(method.getParameterTypes()).map(Class::getName).toArray(String[]::new);
        return Arrays.equals(signature, parameterTypes);
    }

    private Object instance() {
        Annotation[] qualifiers = selectQualifiers(beanClass.getDeclaredAnnotations());
        Set<Bean<?>> beans = beanManager.getBeans(beanClass, qualifiers);
        Bean<?> resolved = beanManager.resolve(beans);
        CreationalContext<?> context = beanManager.createCreationalContext(resolved);
        return beanManager.getReference(resolved, beanClass, context);
    }

    private Annotation[] selectQualifiers(Annotation[] annotations) {
        return Stream.of(annotations).filter(annotation -> beanManager.isQualifier(annotation.annotationType())).toArray(Annotation[]::new);
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return mbeanInfo;
    }
}