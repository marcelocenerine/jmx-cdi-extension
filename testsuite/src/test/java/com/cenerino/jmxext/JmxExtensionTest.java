package com.cenerino.jmxext;

import static com.cenerino.jmxext.util.JmxUtil.invokeMethod;
import static com.cenerino.jmxext.util.JmxUtil.isRegistered;
import static com.cenerino.jmxext.util.JmxUtil.readBeanAttributeValue;
import static com.cenerino.jmxext.util.JmxUtil.readBeanAttributeValues;
import static com.cenerino.jmxext.util.JmxUtil.setBeanAttributeValue;
import static com.cenerino.jmxext.util.JmxUtil.setBeanAttributeValues;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.jboss.shrinkwrap.api.asset.EmptyAsset.INSTANCE;
import static org.junit.Assert.assertThat;

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
public class JmxExtensionTest {

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
        assertThat(isRegistered(Person.class), is(true));
        assertThat(isRegistered(Country.class), is(true));
        assertThat(isRegistered(Order.class), is(true));
    }

    @Test
    public void shouldIgnoreNonAnnotatedClass() {
        assertThat(isRegistered(Car.class), is(false));
    }

    @Test
    public void shouldReturnValueOfBeanAttributes() {
        assertThat(readBeanAttributeValue(Person.class, "name"), is("anonymous"));
        assertThat(readBeanAttributeValue(Person.class, "age"), is(0));
    }

    @Test
    public void shouldReturnValuesOfBeanAttributes() {
        List<Object> attributeValues = readBeanAttributeValues(Person.class, "name", "age");

        assertThat(attributeValues.get(0), is("anonymous"));
        assertThat(attributeValues.get(1), is(0));
    }

    @Test
    public void shouldSetValuesToBeanAttribute() {
        setBeanAttributeValue(Country.class, "name", "Canada");
        setBeanAttributeValue(Country.class, "population", 35160000);

        assertThat(country.getName(), is("Canada"));
        assertThat(country.getPopulation(), is(35160000));
    }

    @Test
    public void shouldSetValuesToBeanAttributes() {
        setBeanAttributeValues(Country.class, asList("name", "population"), asList("Brazil", 200400000));

        assertThat(country.getName(), is("Brazil"));
        assertThat(country.getPopulation(), is(200400000));
    }

    @Test
    public void shouldInvokeBeanMethod() {
        assertThat(person.wasFooInvoked(), is(false));

        invokeMethod(Person.class, "foo", "anyValue");

        assertThat(person.wasFooInvoked(), is(true));
    }

    @Test
    public void shouldNotReferenceSameInstanceWhenBeanIsNotApplicationScoped() {
        assertThat(order.getStatus(), is("Initial"));

        setBeanAttributeValue(Order.class, "status", "Delivered");

        assertThat(order.getStatus(), is("Initial"));
        assertThat(readBeanAttributeValue(Order.class, "status"), is("Initial"));
    }
}
