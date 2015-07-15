package com.cenerino.jmxext.impl;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;

import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.management.IntrospectionException;
import javax.management.MBeanInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.cenerino.jmxext.MBean;

@SuppressWarnings({ "unused", "rawtypes", "unchecked" })
@RunWith(MockitoJUnitRunner.class)
public class DynamicMBeanWrapperTest {

    private DynamicMBeanWrapper mBeanWrapper;

    @Mock
    private Bean bean;

    @Mock
    private BeanManager beanManager;

    @Before
    public void setUp() throws IntrospectionException {
        Set<Bean<?>> beans = newHashSet(bean);
        given(beanManager.getBeans(notNull(Class.class), notNull(MBean.class))).willReturn(beans);
        given(beanManager.resolve(beans)).willReturn(bean);
        given(beanManager.createCreationalContext(bean)).willReturn(mock(CreationalContext.class));
    }

    @Test
    public void shouldReturnMBeanInfoForClassWithNoAttributesOrMethods() throws IntrospectionException {
        configureBeanManagerToReturn(new Car());

        MBeanInfo mBeanInfo = new DynamicMBeanWrapper(bean, beanManager).getMBeanInfo();

        assertThat(mBeanInfo.getDescription(), is("a car"));
        assertThat(mBeanInfo.getAttributes().length, is(0));
        assertThat(mBeanInfo.getOperations().length, is(0));
    }

    @Test
    public void shouldReturnMBeanInfoForClassWithSingleAttributeWithoutAccessorMethods() throws IntrospectionException {
        configureBeanManagerToReturn(new Fruit());

        MBeanInfo mBeanInfo = new DynamicMBeanWrapper(bean, beanManager).getMBeanInfo();

        assertThat(mBeanInfo.getDescription(), is("a fruit"));
        assertThat(mBeanInfo.getAttributes().length, is(0));
    }

    @Test
    public void shouldReturnMBeanInfoForClassWithSingleAttributeWithSetter() throws IntrospectionException {
        configureBeanManagerToReturn(new Sport());

        MBeanInfo mBeanInfo = new DynamicMBeanWrapper(bean, beanManager).getMBeanInfo();

        assertThat(mBeanInfo.getDescription(), is("a sport"));
        assertThat(mBeanInfo.getAttributes().length, is(1));
        assertThat(mBeanInfo.getAttributes()[0].getName(), is("name"));
        assertThat(mBeanInfo.getAttributes()[0].getType(), is(String.class.getName()));
        assertThat(mBeanInfo.getAttributes()[0].isReadable(), is(false));
        assertThat(mBeanInfo.getAttributes()[0].isWritable(), is(true));
        assertThat(mBeanInfo.getAttributes()[0].isIs(), is(false));
    }

    @Test
    public void shouldReturnMBeanInfoForClassWithSingleAttributeWithGetter() throws IntrospectionException {
        configureBeanManagerToReturn(new Country());

        MBeanInfo mBeanInfo = new DynamicMBeanWrapper(bean, beanManager).getMBeanInfo();

        assertThat(mBeanInfo.getDescription(), is("a country"));
        assertThat(mBeanInfo.getAttributes().length, is(1));
        assertThat(mBeanInfo.getAttributes()[0].getName(), is("president"));
        assertThat(mBeanInfo.getAttributes()[0].getType(), is(Person.class.getName()));
        assertThat(mBeanInfo.getAttributes()[0].isReadable(), is(true));
        assertThat(mBeanInfo.getAttributes()[0].isWritable(), is(false));
        assertThat(mBeanInfo.getAttributes()[0].isIs(), is(false));
    }

    @Test
    public void shouldReturnMBeanInfoForClassWithSingleAttributeWithIsMethodReturningPrimitiveBoolean() throws IntrospectionException {
        configureBeanManagerToReturn(new Person());

        MBeanInfo mBeanInfo = new DynamicMBeanWrapper(bean, beanManager).getMBeanInfo();

        assertThat(mBeanInfo.getDescription(), is("a person"));
        assertThat(mBeanInfo.getAttributes().length, is(1));
        assertThat(mBeanInfo.getAttributes()[0].getName(), is("retired"));
        assertThat(mBeanInfo.getAttributes()[0].getType(), is(boolean.class.getName()));
        assertThat(mBeanInfo.getAttributes()[0].isReadable(), is(true));
        assertThat(mBeanInfo.getAttributes()[0].isWritable(), is(false));
        assertThat(mBeanInfo.getAttributes()[0].isIs(), is(true));
    }

    @Test
    public void shouldReturnMBeanInfoForClassWithSingleAttributeWithIsMethodReturningWrapperBoolean() throws IntrospectionException {
        configureBeanManagerToReturn(new Planet());

        MBeanInfo mBeanInfo = new DynamicMBeanWrapper(bean, beanManager).getMBeanInfo();

        assertThat(mBeanInfo.getDescription(), is("a planet"));
        assertThat(mBeanInfo.getAttributes().length, is(1));
        assertThat(mBeanInfo.getAttributes()[0].getName(), is("smallerThanEarth"));
        assertThat(mBeanInfo.getAttributes()[0].getType(), is(Boolean.class.getName()));
        assertThat(mBeanInfo.getAttributes()[0].isReadable(), is(true));
        assertThat(mBeanInfo.getAttributes()[0].isWritable(), is(false));
        assertThat(mBeanInfo.getAttributes()[0].isIs(), is(true));
    }

    @Test
    public void shouldReturnMBeanInfoForClassWithSingleAttributeWithNonBooleanIsMethod() throws IntrospectionException {
        configureBeanManagerToReturn(new Color());

        MBeanInfo mBeanInfo = new DynamicMBeanWrapper(bean, beanManager).getMBeanInfo();

        assertThat(mBeanInfo.getAttributes().length, is(0));
    }

    @Test
    public void shouldReturnMBeanInfoForClassWithSingleStaticAttribute() throws IntrospectionException {
        configureBeanManagerToReturn(new Math());

        MBeanInfo mBeanInfo = new DynamicMBeanWrapper(bean, beanManager).getMBeanInfo();

        assertThat(mBeanInfo.getAttributes().length, is(0));
    }

    @Test
    public void shouldReturnMBeanInfoForClassWithSingleStaticAttributeAndGetterWhichReturnTypeDoesNotMatchAttributeType() throws IntrospectionException {
        configureBeanManagerToReturn(new Book());

        MBeanInfo mBeanInfo = new DynamicMBeanWrapper(bean, beanManager).getMBeanInfo();

        assertThat(mBeanInfo.getAttributes().length, is(0));
    }

    @Test
    public void shouldReturnMBeanInfoForClassWithSingleStaticAttributeAndGetterTakingParameters() throws IntrospectionException {
        configureBeanManagerToReturn(new City());

        MBeanInfo mBeanInfo = new DynamicMBeanWrapper(bean, beanManager).getMBeanInfo();

        assertThat(mBeanInfo.getAttributes().length, is(0));
    }

    @Test
    public void shouldReturnMBeanInfoForClassWithSingleStaticAttributeAndSetterWhichReturnTypeIsNotVoid() throws IntrospectionException {
        configureBeanManagerToReturn(new Animal());

        MBeanInfo mBeanInfo = new DynamicMBeanWrapper(bean, beanManager).getMBeanInfo();

        assertThat(mBeanInfo.getAttributes().length, is(0));
    }

    @Test
    public void shouldReturnMBeanInfoForClassWithSingleStaticAttributeAndSetterWhichParameterDoesNotMatchAttributeType() throws IntrospectionException {
        configureBeanManagerToReturn(new State());

        MBeanInfo mBeanInfo = new DynamicMBeanWrapper(bean, beanManager).getMBeanInfo();

        assertThat(mBeanInfo.getAttributes().length, is(0));
    }

    @Test
    public void shouldReturnMBeanInfoForClassWithInheritedAttributes() throws IntrospectionException {
        configureBeanManagerToReturn(new Canada());

        MBeanInfo mBeanInfo = new DynamicMBeanWrapper(bean, beanManager).getMBeanInfo();

        assertThat(mBeanInfo.getDescription(), is("canada"));
        assertThat(mBeanInfo.getAttributes().length, is(2));
        assertThat(mBeanInfo.getAttributes()[0].getName(), is("population"));
        assertThat(mBeanInfo.getAttributes()[0].getType(), is(int.class.getName()));
        assertThat(mBeanInfo.getAttributes()[0].isReadable(), is(true));
        assertThat(mBeanInfo.getAttributes()[0].isWritable(), is(true));
        assertThat(mBeanInfo.getAttributes()[0].isIs(), is(false));

        assertThat(mBeanInfo.getAttributes()[1].getName(), is("president"));
        assertThat(mBeanInfo.getAttributes()[1].getType(), is(Person.class.getName()));
        assertThat(mBeanInfo.getAttributes()[1].isReadable(), is(true));
        assertThat(mBeanInfo.getAttributes()[1].isWritable(), is(false));
        assertThat(mBeanInfo.getAttributes()[1].isIs(), is(false));
    }

    private void configureBeanManagerToReturn(Object object) {
        given(bean.getBeanClass()).willReturn(object.getClass());
        given(beanManager.getReference(eq(bean), eq(object.getClass()), notNull(CreationalContext.class))).willReturn(object);
    }

    // TODO attributes with non public get/set/is methods

    // TODO test cases for operations

    // Dummy classes used by the tests above

    @MBean(description = "a car")
    private static class Car {}

    @MBean(description = "a fruit")
    private static class Fruit {
        private String color;
    }

    @MBean(description = "a sport")
    private static class Sport {
        private String name;

        public void setName(String name) {
            this.name = name;
        }
    }

    @MBean(description = "a country")
    private static class Country {
        private Person president;

        public Person getPresident() {
            return president;
        }
    }

    @MBean(description = "a person")
    private static class Person {

        private boolean retired;

        public boolean isRetired() {
            return retired;
        }
    }

    @MBean(description = "a planet")
    private static class Planet {
        private Boolean smallerThanEarth;

        public Boolean isSmallerThanEarth() {
            return smallerThanEarth;
        }
    }

    @MBean(description = "a color")
    private static class Color {
        private boolean primitive;

        public String isPrimitive() {
            return Boolean.toString(primitive);
        }
    }

    @MBean(description = "math")
    private static class Math {

        private static double pi = 3.1415;

        public static double getPi() {
            return pi;
        }

        public static void setPi(double pi) {
            Math.pi = pi;
        }
    }

    @MBean(description = "a book")
    private static class Book {
        private int pages;

        public Integer getPages() {
            return pages;
        }
    }

    @MBean(description = "a city")
    private static class City {

        private String name;

        public String getName(String parameter) {
            return name;
        }
    }

    @MBean(description = "an animal")
    private static class Animal {
        private Color color;

        public Color setColor(Color color) {
            Color oldColor = this.color;
            this.color = color;
            return oldColor;
        }
    }

    @MBean(description = "a state")
    private static class State {
        private int population;

        public void setPopulation(Integer population) {
            this.population = population;
        }
    }

    @MBean(description = "canada")
    private static class Canada extends Country {

        private int population;

        public int getPopulation() {
            return population;
        }

        public void setPopulation(int population) {
            this.population = population;
        }
    }
}
