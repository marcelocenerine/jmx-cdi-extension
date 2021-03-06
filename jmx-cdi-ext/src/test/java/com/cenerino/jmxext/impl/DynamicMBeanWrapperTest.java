package com.cenerino.jmxext.impl;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;

import java.beans.IntrospectionException;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;
import org.mockito.runners.MockitoJUnitRunner;

import com.cenerino.jmxext.MBean;

@SuppressWarnings({ "unused", "rawtypes", "unchecked" })
@RunWith(MockitoJUnitRunner.class)
public class DynamicMBeanWrapperTest {

    @Mock
    private Bean bean;

    @Mock
    private BeanManager beanManager;

    @Before
    public void setUp() throws IntrospectionException {
        given(beanManager.isQualifier(MBean.class)).willReturn(true);
        Set<Bean<?>> beans = Sets.newSet(bean);
        given(beanManager.getBeans(notNull(Class.class), notNull(MBean.class))).willReturn(beans);
        given(beanManager.resolve(beans)).willReturn(bean);
        given(beanManager.createCreationalContext(bean)).willReturn(mock(CreationalContext.class));
    }

    @Test
    public void shouldReturnMBeanDescription() throws IntrospectionException {
        configureBeanManagerToReturn(new Car());

        MBeanInfo mBeanInfo = DynamicMBeanWrapper.wrap(bean, beanManager).getMBeanInfo();

        assertThat(mBeanInfo.getDescription()).isEqualTo("a car");
    }

    @Test
    public void shouldReturnMBeanInfoForClassWithoutAttributesOrMethods() throws IntrospectionException {
        configureBeanManagerToReturn(new Car());

        MBeanInfo mBeanInfo = DynamicMBeanWrapper.wrap(bean, beanManager).getMBeanInfo();

        assertThat(mBeanInfo.getAttributes()).isEmpty();
        assertThat(mBeanInfo.getOperations()).isEmpty();
    }

    @Test
    public void shouldReturnMBeanInfoForClassWithAttributeWithoutAccessorMethods() throws IntrospectionException {
        configureBeanManagerToReturn(new Fruit());

        MBeanInfo mBeanInfo = DynamicMBeanWrapper.wrap(bean, beanManager).getMBeanInfo();

        assertThat(mBeanInfo.getAttributes()).isEmpty();
        assertThat(mBeanInfo.getOperations()).isEmpty();
    }

    @Test
    public void shouldReturnMBeanInfoForClassWithAttributeWithSetter() throws IntrospectionException {
        configureBeanManagerToReturn(new Sport());

        MBeanInfo mBeanInfo = DynamicMBeanWrapper.wrap(bean, beanManager).getMBeanInfo();

        assertThat(mBeanInfo.getAttributes()).hasSize(1);
        assertThat(mBeanInfo.getAttributes()[0].getName()).isEqualTo("name");
        assertThat(mBeanInfo.getAttributes()[0].getType()).isEqualTo(String.class.getName());
        assertThat(mBeanInfo.getAttributes()[0].isReadable()).isFalse();
        assertThat(mBeanInfo.getAttributes()[0].isWritable()).isTrue();
        assertThat(mBeanInfo.getAttributes()[0].isIs()).isFalse();
        assertThat(mBeanInfo.getOperations()).hasSize(1);
        assertThat(mBeanInfo.getOperations()[0].getName()).isEqualTo("setName");
    }

    @Test
    public void shouldReturnMBeanInfoForClassWithAttributeWithGetter() throws IntrospectionException {
        configureBeanManagerToReturn(new Country());

        MBeanInfo mBeanInfo = DynamicMBeanWrapper.wrap(bean, beanManager).getMBeanInfo();

        assertThat(mBeanInfo.getAttributes()).hasSize(1);
        assertThat(mBeanInfo.getAttributes()[0].getName()).isEqualTo("president");
        assertThat(mBeanInfo.getAttributes()[0].getType()).isEqualTo(Person.class.getName());
        assertThat(mBeanInfo.getAttributes()[0].isReadable()).isTrue();
        assertThat(mBeanInfo.getAttributes()[0].isWritable()).isFalse();
        assertThat(mBeanInfo.getAttributes()[0].isIs()).isFalse();
        assertThat(mBeanInfo.getOperations()[0].getName()).isEqualTo("getPresident");
    }

    @Test
    public void shouldReturnMBeanInfoForClassWithBooleanIsGetterMethod() throws IntrospectionException {
        configureBeanManagerToReturn(new Person());

        MBeanInfo mBeanInfo = DynamicMBeanWrapper.wrap(bean, beanManager).getMBeanInfo();

        assertThat(mBeanInfo.getAttributes()).hasSize(1);
        assertThat(mBeanInfo.getAttributes()[0].getName()).isEqualTo("retired");
        assertThat(mBeanInfo.getAttributes()[0].getType()).isEqualTo(boolean.class.getName());
        assertThat(mBeanInfo.getAttributes()[0].isReadable()).isTrue();
        assertThat(mBeanInfo.getAttributes()[0].isWritable()).isFalse();
        assertThat(mBeanInfo.getAttributes()[0].isIs()).isTrue();
        assertThat(mBeanInfo.getOperations()).hasSize(1);
        assertThat(mBeanInfo.getOperations()[0].getName()).isEqualTo("isRetired");
    }

    @Test
    public void shouldReturnMBeanInfoForClassWithStaticMembers() throws IntrospectionException {
        configureBeanManagerToReturn(new Math());

        MBeanInfo mBeanInfo = DynamicMBeanWrapper.wrap(bean, beanManager).getMBeanInfo();

        assertThat(mBeanInfo.getAttributes()).isEmpty();
        assertThat(mBeanInfo.getOperations()).hasSize(2);
    }

    @Test
    public void shouldReturnMBeanInfoForClassWithNoPublicAccessorForItsAttributes() throws IntrospectionException {
        configureBeanManagerToReturn(new Appliance());

        MBeanInfo mBeanInfo = DynamicMBeanWrapper.wrap(bean, beanManager).getMBeanInfo();

        assertThat(mBeanInfo.getAttributes()).isEmpty();
        assertThat(mBeanInfo.getOperations()).isEmpty();
    }

    @Test
    public void shouldReturnMBeanInfoForClassWithInheritedAttributes() throws IntrospectionException {
        configureBeanManagerToReturn(new Canada());

        MBeanInfo mBeanInfo = DynamicMBeanWrapper.wrap(bean, beanManager).getMBeanInfo();

        assertThat(mBeanInfo.getAttributes()).hasSize(2);
        assertThat(mBeanInfo.getAttributes()[0].getName()).isEqualTo("population");
        assertThat(mBeanInfo.getAttributes()[0].getType()).isEqualTo(int.class.getName());
        assertThat(mBeanInfo.getAttributes()[0].isReadable()).isTrue();
        assertThat(mBeanInfo.getAttributes()[0].isWritable()).isTrue();
        assertThat(mBeanInfo.getAttributes()[0].isIs()).isFalse();

        assertThat(mBeanInfo.getAttributes()[1].getName()).isEqualTo("president");
        assertThat(mBeanInfo.getAttributes()[1].getType()).isEqualTo(Person.class.getName());
        assertThat(mBeanInfo.getAttributes()[1].isReadable()).isTrue();
        assertThat(mBeanInfo.getAttributes()[1].isWritable()).isFalse();
        assertThat(mBeanInfo.getAttributes()[1].isIs()).isFalse();

        assertThat(mBeanInfo.getOperations()).hasSize(3);
        assertThat(mBeanInfo.getOperations()[0].getName()).isEqualTo("setPopulation");
        assertThat(mBeanInfo.getOperations()[1].getName()).isEqualTo("getPresident");
        assertThat(mBeanInfo.getOperations()[2].getName()).isEqualTo("getPopulation");
    }

    @Test
    public void shouldReturnAttributeUsingGetterMethod() throws Exception {
        Player player = new Player();
        player.name = "Ibrahimovic";
        configureBeanManagerToReturn(player);

        DynamicMBeanWrapper mBean = DynamicMBeanWrapper.wrap(bean, beanManager);

        assertThat(mBean.getAttribute("name")).isEqualTo("Ibrahimovic");
    }

    @Test
    public void shouldReturnAttributeUsingIsMethod() throws Exception {
        Person person = new Person();
        person.retired = true;
        configureBeanManagerToReturn(person);

        DynamicMBeanWrapper mBean = DynamicMBeanWrapper.wrap(bean, beanManager);

        assertThat(mBean.getAttribute("retired")).isEqualTo(true);
    }

    @Test(expected = AttributeNotFoundException.class)
    public void shouldNotReturnAttributeIfItDoesNotHaveValidAccessorMethod() throws Exception {
        Appliance appliance = new Appliance();
        appliance.manufacturer = "Bosch";
        configureBeanManagerToReturn(appliance);

        DynamicMBeanWrapper mBean = DynamicMBeanWrapper.wrap(bean, beanManager);

        mBean.getAttribute("manufacturer");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotReturnAttributeIfInformedAttributeNameIsInvalid() throws Exception {
        configureBeanManagerToReturn(new Appliance());

        DynamicMBeanWrapper mBean = DynamicMBeanWrapper.wrap(bean, beanManager);

        mBean.getAttribute(null);
    }

    @Test
    public void shouldReturnAttributes() throws Exception {
        Player player = new Player();
        player.name = "Ibrahimovic";
        player.age = 33;
        configureBeanManagerToReturn(player);

        DynamicMBeanWrapper mBean = DynamicMBeanWrapper.wrap(bean, beanManager);
        List<Attribute> attributes = mBean.getAttributes(new String[] { "name", "age" }).asList();

        assertThat(attributes).hasSize(2);
        assertThat(attributes.get(0).getName()).isEqualTo("name");
        assertThat(attributes.get(0).getValue()).isEqualTo("Ibrahimovic");
        assertThat(attributes.get(1).getName()).isEqualTo("age");
        assertThat(attributes.get(1).getValue()).isEqualTo(33);
    }

    @Test
    public void shouldReturnAttributesAndIgnoreInvalidOnes() throws Exception {
        Player player = new Player();
        player.name = "Pirlo";
        configureBeanManagerToReturn(player);

        DynamicMBeanWrapper mBean = DynamicMBeanWrapper.wrap(bean, beanManager);
        List<Attribute> attributes = mBean.getAttributes(new String[] { "name", "nationality", "height" }).asList();

        assertThat(attributes).hasSize(1);
        assertThat(attributes.get(0).getName()).isEqualTo("name");
        assertThat(attributes.get(0).getValue()).isEqualTo("Pirlo");
    }

    @Test
    public void shouldSetAttributeValue() throws Exception {
        configureBeanManagerToReturn(new Player());

        DynamicMBeanWrapper mBean = DynamicMBeanWrapper.wrap(bean, beanManager);
        mBean.setAttribute(new Attribute("name", "Buffon"));

        assertThat(mBean.getAttribute("name")).isEqualTo("Buffon");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotSetAttributeIfNameIsNotInformed() throws Exception {
        configureBeanManagerToReturn(new Player());

        DynamicMBeanWrapper mBean = DynamicMBeanWrapper.wrap(bean, beanManager);
        mBean.setAttribute(new Attribute("", "Buffon"));
    }

    @Test(expected = AttributeNotFoundException.class)
    public void shouldNotSetValueToAttributeThatDoesNotExist() throws Exception {
        configureBeanManagerToReturn(new Player());

        DynamicMBeanWrapper mBean = DynamicMBeanWrapper.wrap(bean, beanManager);
        mBean.setAttribute(new Attribute("nationality", "Italian"));
    }

    @Test(expected = AttributeNotFoundException.class)
    public void shouldNotSetValueToAttributeThatIsNotWritable() throws Exception {
        configureBeanManagerToReturn(new Person());

        DynamicMBeanWrapper mBean = DynamicMBeanWrapper.wrap(bean, beanManager);
        mBean.setAttribute(new Attribute("retired", false));
    }

    @Test(expected = InvalidAttributeValueException.class)
    public void shouldNotSetValueToAttributeIfValueIsNotCompatible() throws Exception {
        configureBeanManagerToReturn(new Player());

        DynamicMBeanWrapper mBean = DynamicMBeanWrapper.wrap(bean, beanManager);
        mBean.setAttribute(new Attribute("name", new Object()));
    }

    @Test
    public void shouldSetValueToMultipleAttributes() throws Exception {
        configureBeanManagerToReturn(new Player());

        DynamicMBeanWrapper mBean = DynamicMBeanWrapper.wrap(bean, beanManager);
        AttributeList attributes = new AttributeList(asList(new Attribute("name", "Lampard"), new Attribute("age", 37)));
        AttributeList returnedAttributes = mBean.setAttributes(attributes);

        assertThat(returnedAttributes).hasSize(2);
        assertThat(mBean.getAttribute("name")).isEqualTo("Lampard");
        assertThat(mBean.getAttribute("age")).isEqualTo(37);
    }

    @Test
    public void shouldSetValueToMultipleAttributesIgnoringTheOnesThatFail() throws Exception {
        configureBeanManagerToReturn(new Player());

        DynamicMBeanWrapper mBean = DynamicMBeanWrapper.wrap(bean, beanManager);
        AttributeList attributes = new AttributeList(asList(new Attribute("name", "Drogba"), new Attribute("nationality", "Ivorian"), new Attribute(
                "height", 1.84)));
        List<Attribute> returnedAttributes = mBean.setAttributes(attributes).asList();

        assertThat(returnedAttributes).hasSize(1);
        assertThat(mBean.getAttribute("name")).isEqualTo("Drogba");
        assertThat(returnedAttributes.get(0).getName()).isEqualTo("name");
        assertThat(returnedAttributes.get(0).getValue()).isEqualTo("Drogba");
    }

    @Test
    public void shouldInvokeOperationWithoutParameters() throws Exception {
        configureBeanManagerToReturn(new Person());

        DynamicMBeanWrapper.wrap(bean, beanManager).invoke("isRetired", new Object[] {}, new String[] {});
    }

    @Test
    public void shouldInvokeOperationWithParameters() throws Exception {
        configureBeanManagerToReturn(new Player());

        DynamicMBeanWrapper mBean = DynamicMBeanWrapper.wrap(bean, beanManager);
        Object result = mBean.invoke("setName", new Object[] { "Eto'o" }, new String[] { "java.lang.String" });

        assertThat(result).isNull();
        assertThat(mBean.getAttribute("name")).isEqualTo("Eto'o");
    }

    @Test
    public void shouldInvokeStaticOperation() throws Exception {
        configureBeanManagerToReturn(new Math());

        Object result = DynamicMBeanWrapper.wrap(bean, beanManager)
                .invoke("max", new Object[] { new Integer(10), 50 }, new String[] { "int", "int" });

        assertThat(result).isEqualTo(50);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailIfOperationCannotBeFound() throws Exception {
        configureBeanManagerToReturn(new Person());

        DynamicMBeanWrapper.wrap(bean, beanManager).invoke("isRetired", new Object[] { false }, new String[] { "java.lang.Boolean" });
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailIfOperationIsNotPublic() throws Exception {
        configureBeanManagerToReturn(new Appliance());

        DynamicMBeanWrapper.wrap(bean, beanManager).invoke("getManufacturer", new Object[] {}, new String[] {});
    }

    private void configureBeanManagerToReturn(Object object) {
        given(bean.getBeanClass()).willReturn(object.getClass());
        given(beanManager.getReference(eq(bean), eq(object.getClass()), notNull(CreationalContext.class))).willReturn(object);
    }

    // Dummy classes used by the tests above

    @MBean(description = "a car")
    private static class Car {
    }

    @MBean(description = "a fruit")
    private static class Fruit {
        private String color;
    }

    @MBean
    private static class Sport {
        private String name;

        public void setName(String name) {
            this.name = name;
        }
    }

    @MBean
    private static class Country {
        private Person president;

        public Person getPresident() {
            return president;
        }
    }

    @MBean
    private static class Person {

        private boolean retired;

        public boolean isRetired() {
            return retired;
        }
    }

    @MBean
    private static class Math {

        public static final double PI = 3.1415;

        public static double getPi() {
            return PI;
        }

        public static int max(int n1, int n2) {
            return java.lang.Math.max(n1, n2);
        }
    }

    @MBean
    private static class Canada extends Country {

        private int population;

        public int getPopulation() {
            return population;
        }

        public void setPopulation(int population) {
            this.population = population;
        }
    }

    @MBean
    private static class Appliance {

        private String manufacturer;
        private boolean imported;

        String getManufacturer() {
            return manufacturer;
        }

        private void setManufacturer(String manufacturer) {
            this.manufacturer = manufacturer;
        }

        protected boolean isImported() {
            return imported;
        }
    }

    @MBean
    private static class Player {

        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int height) {
            this.age = height;
        }
    }
}
