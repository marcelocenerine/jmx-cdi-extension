package com.cenerino.jmxext.impl;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.*;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cenerino.jmxext.MBean;

class JmxExtension implements Extension {

    private static final Logger logger = LoggerFactory.getLogger(JmxExtension.class);
    private Map<String, DynamicMBeanWrapper> mBeansRegistry = new HashMap<>();
    private MBeanServer mBeanServer;

    void init(@Observes BeforeBeanDiscovery event) {
        mBeanServer = ManagementFactory.getPlatformMBeanServer();
    }

    void processBean(@Observes ProcessManagedBean<?> event, final BeanManager beanManager) throws Exception {
        if (isDecoratedWithMBeanAnnotation(event)) {
            registerMBean(event, beanManager);
        }
    }

    private static boolean isDecoratedWithMBeanAnnotation(ProcessManagedBean<?> event) {
        return event.getAnnotated().isAnnotationPresent(MBean.class);
    }

    private void registerMBean(ProcessManagedBean<?> event, final BeanManager beanManager) {
        Class<?> beanClass = event.getBean().getBeanClass();
        logger.debug("Identified class '{}' with annotation '{}.", beanClass.getName(), MBean.class.getName());
        String mBeanName = getObjectNameFor(beanClass);

        try {
            DynamicMBeanWrapper wrapper = new DynamicMBeanWrapper(event.getBean(), beanManager);
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

    void shutdown(@Observes final BeforeShutdown shutdown) {
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

    int getMBeanCount() {
        return mBeansRegistry.size();
    }
}
