package com.example.vlsmcalculator;

/**
 * Model class representing IPv4 subnet calculation result
 */
public class IPv4SubnetResult {
    private String departmentName;
    private String networkAddress;
    private String subnetMask;
    private int cidrPrefix;
    private String firstUsableIP;
    private String lastUsableIP;
    private String broadcastAddress;
    private int totalUsableHosts;

    public IPv4SubnetResult(String departmentName) {
        this.departmentName = departmentName;
    }

    // Getters and Setters
    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getNetworkAddress() {
        return networkAddress;
    }

    public void setNetworkAddress(String networkAddress) {
        this.networkAddress = networkAddress;
    }

    public String getSubnetMask() {
        return subnetMask;
    }

    public void setSubnetMask(String subnetMask) {
        this.subnetMask = subnetMask;
    }

    public int getCidrPrefix() {
        return cidrPrefix;
    }

    public void setCidrPrefix(int cidrPrefix) {
        this.cidrPrefix = cidrPrefix;
    }

    public String getFirstUsableIP() {
        return firstUsableIP;
    }

    public void setFirstUsableIP(String firstUsableIP) {
        this.firstUsableIP = firstUsableIP;
    }

    public String getLastUsableIP() {
        return lastUsableIP;
    }

    public void setLastUsableIP(String lastUsableIP) {
        this.lastUsableIP = lastUsableIP;
    }

    public String getBroadcastAddress() {
        return broadcastAddress;
    }

    public void setBroadcastAddress(String broadcastAddress) {
        this.broadcastAddress = broadcastAddress;
    }

    public int getTotalUsableHosts() {
        return totalUsableHosts;
    }

    public void setTotalUsableHosts(int totalUsableHosts) {
        this.totalUsableHosts = totalUsableHosts;
    }
}


