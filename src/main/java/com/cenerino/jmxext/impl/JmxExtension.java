package com.cenerino.jmxext.impl;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessManagedBean;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cenerino.jmxext.MBean;

public class JmxExtension implements Extension {

    private static final Logger logger = LoggerFactory.getLogger(JmxExtension.class);
    private Map<String, DynamicMBeanWrapper> mBeansRegistry = new HashMap<>();

    protected void processBean(@Observes ProcessManagedBean<?> event, final BeanManager bm) throws Exception {
        if (isDecoratedWithMBeanAnnotation(event)) {
            registerMBean(event, bm);
        }
    }

    private static boolean isDecoratedWithMBeanAnnotation(ProcessManagedBean<?> bean) {
        return bean.getAnnotated().isAnnotationPresent(MBean.class);
    }

    private void registerMBean(ProcessManagedBean<?> event, final BeanManager bm) {
        Class<?> beanClass = event.getBean().getBeanClass();
        logger.debug("Identified class '{}' with annotation '{}.", beanClass.getName(), MBean.class.getName());
        String mBeanName = getObjectNameFor(beanClass);

        try {
            DynamicMBeanWrapper wrapper = new DynamicMBeanWrapper(event.getBean(), bm);
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            logger.debug("Registering MBean with name '{}' for class '{}'...", mBeanName, beanClass.getName());
            mBeanServer.registerMBean(wrapper, new ObjectName(mBeanName));
            mBeansRegistry.put(mBeanName, wrapper);
            logger.debug("MBean '{}' registered successfully.", mBeanName);
        } catch (Exception e) {
            logger.error(String.format("Class '%s' could not be registered as an MBean.", mBeanName), e);
        }
    }

    private String getObjectNameFor(Class<?> beanClass) {
        return String.format("%s:type=%s", beanClass.getPackage().getName(), beanClass.getSimpleName());
    }

    protected void shutdown(@Observes final BeforeShutdown shutdown) {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

        for (String mBeanName : mBeansRegistry.keySet()) {
            try {
                logger.debug("Unregistering MBean '{}'...", mBeanName);
                mBeanServer.unregisterMBean(new ObjectName(mBeanName));
                logger.debug("MBean '{}' unregistered successfully.", mBeanName);
            } catch (MBeanRegistrationException | InstanceNotFoundException | MalformedObjectNameException e) {
                logger.error(String.format("Error to unregister MBean '%s'.", mBeanName), e);
            }
        }

        mBeansRegistry.clear();
    }
}
