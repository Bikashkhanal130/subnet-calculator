package com.example.vlsmcalculator;

/**
 * department requirement ko lagi
 */
public class Department {
    private String name;
    private int requiredHosts;

    public Department(String name, int requiredHosts) {
        this.name = name;
        this.requiredHosts = requiredHosts;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRequiredHosts() {
        return requiredHosts;
    }

    public void setRequiredHosts(int requiredHosts) {
        this.requiredHosts = requiredHosts;
    }
}


