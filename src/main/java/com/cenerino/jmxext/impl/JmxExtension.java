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
import javax.management.JMException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.cenerino.jmxext.MBean;

public class JmxExtension implements Extension {

    private Map<String, DynamicMBeanWrapper> mBeansRegistry = new HashMap<>();

    protected void processBean(@Observes ProcessManagedBean<?> event, final BeanManager bm) throws Exception {
        if (hasMBeanAnnotation(event)) {
            registerMBean(event, bm);
        }
    }

    private static boolean hasMBeanAnnotation(ProcessManagedBean<?> bean) {
        return bean.getAnnotated().isAnnotationPresent(MBean.class);
    }

    private void registerMBean(ProcessManagedBean<?> event, final BeanManager bm) throws JMException {
        Class<?> beanClass = event.getBean().getBeanClass();

        DynamicMBeanWrapper wrapper = new DynamicMBeanWrapper(event.getBean(), bm);
        String mBeanName = getObjectNameFor(beanClass);

        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        mBeanServer.registerMBean(wrapper, new ObjectName(mBeanName));
        mBeansRegistry.put(mBeanName, wrapper);
    }

    private String getObjectNameFor(Class<?> beanClass) throws MalformedObjectNameException {
        return String.format("%s:type=%s", beanClass.getPackage().getName(), beanClass.getSimpleName());
    }

    protected void shutdown(@Observes final BeforeShutdown shutdown) {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

        for (String mBeanName : mBeansRegistry.keySet()) {
            try {
                mBeanServer.unregisterMBean(new ObjectName(mBeanName));
            } catch (MBeanRegistrationException | InstanceNotFoundException | MalformedObjectNameException e) {
                // ignore
            }
        }

        mBeansRegistry.clear();
    }
}
