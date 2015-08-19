package com.cenerino.jmxext.beans;

import javax.enterprise.context.ApplicationScoped;

import com.cenerino.jmxext.MBean;

@ApplicationScoped
@MBean(description = "Country bean")
public class Country {

    private String name;
    private int population;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPopulation() {
        return population;
    }

    public void setPopulation(int population) {
        this.population = population;
    }
}
