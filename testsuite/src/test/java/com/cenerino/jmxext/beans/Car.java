package com.cenerino.jmxext.beans;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Car {

    private String color;
    private String maker;

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getMaker() {
        return maker;
    }

    public void setMaker(String maker) {
        this.maker = maker;
    }
}
