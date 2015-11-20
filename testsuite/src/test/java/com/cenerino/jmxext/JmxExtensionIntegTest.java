package com.cenerino.jmxext;

import static com.cenerino.jmxext.util.JmxUtil.invokeMethod;
import static com.cenerino.jmxext.util.JmxUtil.isRegistered;
import static com.cenerino.jmxext.util.JmxUtil.readBeanAttributeValue;
import static com.cenerino.jmxext.util.JmxUtil.readBeanAttributeValues;
import static com.cenerino.jmxext.util.JmxUtil.setBeanAttributeValue;
import static com.cenerino.jmxext.util.JmxUtil.setBeanAttributeValues;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.shrinkwrap.api.asset.EmptyAsset.INSTANCE;

import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.cenerino.jmxext.beans.Car;
import com.cenerino.jmxext.beans.Country;
import com.cenerino.jmxext.beans.Order;
import com.cenerino.jmxext.beans.Person;

@RunWith(Arquillian.class)
public class JmxExtensionIntegTest {

    @Inject
    private Person person;

    @Inject
    private Country country;

    @Inject
    private Order order;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class).addPackages(true, "com.cenerino.jmxext").addAsManifestResource(INSTANCE, "beans.xml");
    }

    @Test
    public void shouldExposeAnnotatedClasses() {
        assertThat(isRegistered(Person.class)).isTrue();
        assertThat(isRegistered(Country.class)).isTrue();
        assertThat(isRegistered(Order.class)).isTrue();
    }

    @Test
    public void shouldIgnoreNonAnnotatedClass() {
        assertThat(isRegistered(Car.class)).isFalse();
    }

    @Test
    public void shouldReturnValueOfBeanAttributes() {
        assertThat(readBeanAttributeValue(Person.class, "name")).isEqualTo("anonymous");
        assertThat(readBeanAttributeValue(Person.class, "age")).isEqualTo(0);
    }

    @Test
    public void shouldReturnValuesOfBeanAttributes() {
        List<Object> attributeValues = readBeanAttributeValues(Person.class, "name", "age");

        assertThat(attributeValues.get(0)).isEqualTo("anonymous");
        assertThat(attributeValues.get(1)).isEqualTo(0);
    }

    @Test
    public void shouldSetValuesToBeanAttribute() {
        setBeanAttributeValue(Country.class, "name", "Canada");
        setBeanAttributeValue(Country.class, "population", 35160000);

        assertThat(country.getName()).isEqualTo("Canada");
        assertThat(country.getPopulation()).isEqualTo(35160000);
    }

    @Test
    public void shouldSetValuesToBeanAttributes() {
        setBeanAttributeValues(Country.class, asList("name", "population"), asList("Brazil", 200400000));

        assertThat(country.getName()).isEqualTo("Brazil");
        assertThat(country.getPopulation()).isEqualTo(200400000);
    }

    @Test
    public void shouldInvokeBeanMethod() {
        assertThat(person.wasFooInvoked()).isFalse();

        invokeMethod(Person.class, "foo", "anyValue");

        assertThat(person.wasFooInvoked()).isTrue();
    }

    @Test
    public void shouldNotReferenceSameInstanceWhenBeanIsNotApplicationScoped() {
        assertThat(order.getStatus()).isEqualTo("Initial");

        setBeanAttributeValue(Order.class, "status", "Delivered");

        assertThat(order.getStatus()).isEqualTo("Initial");
        assertThat(readBeanAttributeValue(Order.class, "status")).isEqualTo("Initial");
    }
}
