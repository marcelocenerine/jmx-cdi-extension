package com.cenerino.jmxext.util;

import static org.junit.Assert.assertEquals;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public final class JmxUtil {

    private static final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

    public static boolean isRegistered(Class<?> mbeanClass) {
        try {
            return mbeanServer.isRegistered(getObjectNameFor(mbeanClass));
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object readBeanAttributeValue(Class<?> mbeanClass, String attribute) {
        try {
            return mbeanServer.getAttribute(getObjectNameFor(mbeanClass), attribute);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Object> readBeanAttributeValues(Class<?> mbeanClass, String... attributes) {
        try {
            AttributeList attributeList = mbeanServer.getAttributes(getObjectNameFor(mbeanClass), attributes);
            return attributeList.asList().stream().map(x -> x.getValue()).collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void setBeanAttributeValue(Class<?> mbeanClass, String attribute, Object value) {
        try {
            mbeanServer.setAttribute(getObjectNameFor(mbeanClass), new Attribute(attribute, value));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void setBeanAttributeValues(Class<?> mbeanClass, List<String> attributes, List<Object> values) {
        assertEquals(attributes.size(), values.size());

        try {
            AttributeList attributeList = new AttributeList();

            for (int x = 0; x < attributes.size(); x++) {
                attributeList.add(new Attribute(attributes.get(x), values.get(x)));
            }

            mbeanServer.setAttributes(getObjectNameFor(mbeanClass), attributeList);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void invokeMethod(Class<?> mbeanClass, String methodName, Object... args) {
        try {
            mbeanServer.invoke(getObjectNameFor(mbeanClass), methodName, args, Stream.of(args).map(x -> x.getClass().getName()).toArray(String[]::new));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ObjectName getObjectNameFor(Class<?> clazz) throws MalformedObjectNameException {
        return new ObjectName(String.format("%s:type=%s", clazz.getPackage().getName(), clazz.getSimpleName()));
    }
}
