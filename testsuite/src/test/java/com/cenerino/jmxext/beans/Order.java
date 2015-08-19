package com.cenerino.jmxext.beans;

import com.cenerino.jmxext.MBean;

@MBean
public class Order {

    private String status = "Initial";

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
