package com.cenerino.jmxext.beans;

import javax.enterprise.context.ApplicationScoped;

import com.cenerino.jmxext.MBean;

@ApplicationScoped
@MBean(description="Person bean")
public class Person {

    private String name = "anonymous";
    private int age;
    private boolean fooInvoked;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void foo(String value) {
        fooInvoked = true;
    }

    public boolean wasFooInvoked() {
        return fooInvoked;
    }
}
