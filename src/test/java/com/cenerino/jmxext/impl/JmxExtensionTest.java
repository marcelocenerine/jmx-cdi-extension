package com.cenerino.jmxext.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.*;

import javax.enterprise.inject.spi.*;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import com.cenerino.jmxext.MBean;

@RunWith(MockitoJUnitRunner.class)
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
    public void setUp() {
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