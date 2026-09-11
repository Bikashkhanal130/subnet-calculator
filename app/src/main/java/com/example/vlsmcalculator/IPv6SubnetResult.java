package com.example.vlsmcalculator;

/**
 * Model class representing IPv6 subnet calculation result
 */
public class IPv6SubnetResult {
    private String departmentName;
    private String assignedPrefix;
    private String subnetRangeStart;
    private String subnetRangeEnd;

    public IPv6SubnetResult(String departmentName) {
        this.departmentName = departmentName;
    }

    // Getters and Setters
    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getAssignedPrefix() {
        return assignedPrefix;
    }

    public void setAssignedPrefix(String assignedPrefix) {
        this.assignedPrefix = assignedPrefix;
    }

    public String getSubnetRangeStart() {
        return subnetRangeStart;
    }

    public void setSubnetRangeStart(String subnetRangeStart) {
        this.subnetRangeStart = subnetRangeStart;
    }

    public String getSubnetRangeEnd() {
        return subnetRangeEnd;
    }

    public void setSubnetRangeEnd(String subnetRangeEnd) {
        this.subnetRangeEnd = subnetRangeEnd;
    }
}


