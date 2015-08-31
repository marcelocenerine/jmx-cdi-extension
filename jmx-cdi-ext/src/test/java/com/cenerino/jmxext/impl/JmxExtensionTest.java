package com.cenerino.jmxext.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.beans.IntrospectionException;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.ProcessManagedBean;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cenerino.jmxext.MBean;

@RunWith(PowerMockRunner.class)
@PrepareForTest(DynamicMBeanWrapper.class)
@SuppressWarnings("rawtypes")
public class JmxExtensionTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ProcessManagedBean event;

    @Mock
    private BeanManager beanManager;

    @Mock
    private MBeanServer mBeanServer;

    @InjectMocks
    private JmxExtension jmxExtension;

    @Before
    public void setUp() throws IntrospectionException {
        mockStatic(DynamicMBeanWrapper.class);
        given(DynamicMBeanWrapper.wrap(notNull(Bean.class), notNull(BeanManager.class))).willReturn(mock(DynamicMBeanWrapper.class));

        given(event.getBean().getBeanClass()).willReturn(getClass());
    }

    @Test
    public void shouldRegisterClassAnnotatedWithMBean() throws Exception {
        given(event.getAnnotated().isAnnotationPresent(MBean.class)).willReturn(true);

        jmxExtension.processBean(event, beanManager);

        assertThat(jmxExtension.getMBeanCount(), is(1));
        verify(mBeanServer).registerMBean(notNull(DynamicMBeanWrapper.class), notNull(ObjectName.class));
    }

    @Test
    public void shouldNotRegisterClassWithoutMBeanAnnotation() throws Exception {
        jmxExtension.processBean(event, beanManager);

        assertThat(jmxExtension.getMBeanCount(), is(0));
        verifyZeroInteractions(mBeanServer);
    }

    @Test
    public void shouldIgnoreErrorsWhenRegisteringMBeans() throws Exception {
        given(event.getAnnotated().isAnnotationPresent(MBean.class)).willReturn(true);
        doThrow(RuntimeException.class).when(mBeanServer).registerMBean(any(DynamicMBeanWrapper.class), any(ObjectName.class));

        jmxExtension.processBean(event, beanManager);

        assertThat(jmxExtension.getMBeanCount(), is(0));
        verify(mBeanServer).registerMBean(notNull(DynamicMBeanWrapper.class), notNull(ObjectName.class));
    }

    @Test
    public void shouldUnregisterMBeans() throws Exception {
        shouldRegisterClassAnnotatedWithMBean();

        jmxExtension.shutdown(mock(BeforeShutdown.class));

        assertThat(jmxExtension.getMBeanCount(), is(0));
        verify(mBeanServer).unregisterMBean(notNull(ObjectName.class));
    }
}