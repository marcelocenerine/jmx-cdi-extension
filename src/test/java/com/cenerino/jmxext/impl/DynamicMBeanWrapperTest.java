package com.cenerino.jmxext.impl;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import javax.management.IntrospectionException;
import javax.management.MBeanInfo;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

@SuppressWarnings("unused")
public class DynamicMBeanWrapperTest {

    @Test
    public void shouldReturnMBeanInfoForClassWithNoAttributesOrMethods() throws IntrospectionException {
        Car car = new Car();

        MBeanInfo mBeanInfo = new DynamicMBeanWrapper(car, car.toString()).getMBeanInfo();

        assertThat(mBeanInfo.getDescription(), is(car.toString()));
        assertThat(mBeanInfo.getAttributes().length, is(0));
        assertThat(mBeanInfo.getOperations().length, is(0));
    }

    @Test
    public void shouldReturnMBeanInfoForClassWithSingleAttributeWithoutAccessorMethods() throws IntrospectionException {
        Fruit fruit = new Fruit();

        MBeanInfo mBeanInfo = new DynamicMBeanWrapper(fruit, fruit.toString()).getMBeanInfo();

        assertThat(mBeanInfo.getDescription(), is(fruit.toString()));
        assertThat(mBeanInfo.getAttributes().length, is(0));
    }

    @Test
    public void shouldReturnMBeanInfoForClassWithSingleAttributeWithSetter() throws IntrospectionException {
        Sport sport = new Sport();

        MBeanInfo mBeanInfo = new DynamicMBeanWrapper(sport, sport.toString()).getMBeanInfo();

        assertThat(mBeanInfo.getDescription(), is(sport.toString()));
        assertThat(mBeanInfo.getAttributes().length, is(1));
        assertThat(mBeanInfo.getAttributes()[0].getName(), is("name"));
        assertThat(mBeanInfo.getAttributes()[0].getType(), is(String.class.getName()));
        assertThat(mBeanInfo.getAttributes()[0].isReadable(), is(false));
        assertThat(mBeanInfo.getAttributes()[0].isWritable(), is(true));
        assertThat(mBeanInfo.getAttributes()[0].isIs(), is(false));
    }

    @Test
    public void shouldReturnMBeanInfoForClassWithSingleAttributeWithGetter() throws IntrospectionException {
        Country country = new Country();

        MBeanInfo mBeanInfo = new DynamicMBeanWrapper(country, country.toString()).getMBeanInfo();

        assertThat(mBeanInfo.getDescription(), is(country.toString()));
        assertThat(mBeanInfo.getAttributes().length, is(1));
        assertThat(mBeanInfo.getAttributes()[0].getName(), is("president"));
        assertThat(mBeanInfo.getAttributes()[0].getType(), is(Person.class.getName()));
        assertThat(mBeanInfo.getAttributes()[0].isReadable(), is(true));
        assertThat(mBeanInfo.getAttributes()[0].isWritable(), is(false));
        assertThat(mBeanInfo.getAttributes()[0].isIs(), is(false));
    }

    @Test
    public void shouldReturnMBeanInfoForClassWithSingleAttributeWithIsMethodReturningPrimitiveBoolean() throws IntrospectionException {
        Person person = new Person();

        MBeanInfo mBeanInfo = new DynamicMBeanWrapper(person, person.toString()).getMBeanInfo();

        assertThat(mBeanInfo.getDescription(), is(person.toString()));
        assertThat(mBeanInfo.getAttributes().length, is(1));
        assertThat(mBeanInfo.getAttributes()[0].getName(), is("retired"));
        assertThat(mBeanInfo.getAttributes()[0].getType(), is(boolean.class.getName()));
        assertThat(mBeanInfo.getAttributes()[0].isReadable(), is(true));
        assertThat(mBeanInfo.getAttributes()[0].isWritable(), is(false));
        assertThat(mBeanInfo.getAttributes()[0].isIs(), is(true));
    }
    
    @Test
    public void shouldReturnMBeanInfoForClassWithSingleAttributeWithIsMethodReturningWrapperBoolean() throws IntrospectionException {
        Drug drug = new Drug();

        MBeanInfo mBeanInfo = new DynamicMBeanWrapper(drug, drug.toString()).getMBeanInfo();

        assertThat(mBeanInfo.getDescription(), is(drug.toString()));
        assertThat(mBeanInfo.getAttributes().length, is(1));
        assertThat(mBeanInfo.getAttributes()[0].getName(), is("legal"));
        assertThat(mBeanInfo.getAttributes()[0].getType(), is(Boolean.class.getName()));
        assertThat(mBeanInfo.getAttributes()[0].isReadable(), is(true));
        assertThat(mBeanInfo.getAttributes()[0].isWritable(), is(false));
        assertThat(mBeanInfo.getAttributes()[0].isIs(), is(true));
    }
    
    @Test
    public void shouldReturnMBeanInfoForClassWithSingleAttributeWithNonBooleanIsMethod() throws IntrospectionException {
        Color color = new Color();

        MBeanInfo mBeanInfo = new DynamicMBeanWrapper(color, color.toString()).getMBeanInfo();

        assertThat(mBeanInfo.getAttributes().length, is(0));
    }
    
    @Test
    public void shouldReturnMBeanInfoForClassWithSingleStaticAttribute() throws IntrospectionException {
        Math math = new Math();

        MBeanInfo mBeanInfo = new DynamicMBeanWrapper(math, math.toString()).getMBeanInfo();

        assertThat(mBeanInfo.getAttributes().length, is(0));
    }
    
    @Test
    public void shouldReturnMBeanInfoForClassWithSingleStaticAttributeAndGetterWhichReturnTypeDoesNotMatchAttributeType() throws IntrospectionException {
        Book book = new Book();

        MBeanInfo mBeanInfo = new DynamicMBeanWrapper(book, book.toString()).getMBeanInfo();

        assertThat(mBeanInfo.getAttributes().length, is(0));
    }
    
    @Test
    public void shouldReturnMBeanInfoForClassWithSingleStaticAttributeAndGetterTakingParameters() throws IntrospectionException {
        City city = new City();

        MBeanInfo mBeanInfo = new DynamicMBeanWrapper(city, city.toString()).getMBeanInfo();

        assertThat(mBeanInfo.getAttributes().length, is(0));
    }
    
    @Test
    public void shouldReturnMBeanInfoForClassWithSingleStaticAttributeAndSetterWhichReturnTypeIsNotVoid() throws IntrospectionException {
        Animal animal = new Animal();

        MBeanInfo mBeanInfo = new DynamicMBeanWrapper(animal, animal.toString()).getMBeanInfo();

        assertThat(mBeanInfo.getAttributes().length, is(0));
    }
    
    @Test
    public void shouldReturnMBeanInfoForClassWithSingleStaticAttributeAndSetterWhichParameterDoesNotMatchAttributeType() throws IntrospectionException {
        State state = new State();

        MBeanInfo mBeanInfo = new DynamicMBeanWrapper(state, state.toString()).getMBeanInfo();

        assertThat(mBeanInfo.getAttributes().length, is(0));
    }
    
    @Test
    public void shouldReturnMBeanInfoForClassWithInheritedAttributes() throws IntrospectionException {
        Canada canada = new Canada();

        MBeanInfo mBeanInfo = new DynamicMBeanWrapper(canada, canada.toString()).getMBeanInfo();

        assertThat(mBeanInfo.getDescription(), is(canada.toString()));
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
    
    // getter, setter, is are not public

    // Dummy classes used by the tests above
    
    private static class Car {}

    private static class Fruit {
        private String color;
    }

    private static class Sport {
        private String name;

        public void setName(String name) {
            this.name = name;
        }
    }

    private static class Country {
        private Person president;

        public Person getPresident() {
            return president;
        }
    }
    
    private static class Person {
        
        private boolean retired;
        
        public boolean isRetired() {
            return retired;
        }
    }
    
    private static class Drug {
        private Boolean legal;
        
        public Boolean isLegal() {
            return legal;
        }
    }
    
    private static class Color {
        private boolean primitive;
        
        public String isPrimitive() {
            return Boolean.toString(primitive);
        }
    }
    
    private static class Math {
        
        private static  double pi = 3.1415;
        
        public static double getPi() {
            return pi;
        }
        
        public static void setPi(double pi) {
            Math.pi = pi;
        }
    }
    
    private static class Book {
        private int pages;
        
        public Integer getPages() {
            return pages;
        }
    }
    
    private static class City {
        
        private String name;
        
        public String getName(String parameter) {
            return name;
        }
    }
    
    private static class Animal {
        private Color color;
        
        public Color setColor(Color color) {
            Color oldColor = this.color;
            this.color = color;
            return oldColor;
        }
    }
    
    private static class State {
        private int population;
        
        public void setPopulation(Integer population) {
            this.population = population;
        }
    }
    
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
