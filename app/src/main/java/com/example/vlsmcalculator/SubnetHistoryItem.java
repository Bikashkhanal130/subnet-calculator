package com.example.vlsmcalculator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Model class to store a history item containing calculation details
 */
public class SubnetHistoryItem {
    private String baseNetwork;
    private boolean isIPv4;
    private List<Department> departments;
    private List<IPv4SubnetResult> ipv4Results;
    private List<IPv6SubnetResult> ipv6Results;
    private String timestamp;

    public SubnetHistoryItem(String baseNetwork, boolean isIPv4, List<Department> departments) {
        this.baseNetwork = baseNetwork;
        this.isIPv4 = isIPv4;
        this.departments = departments;
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        this.timestamp = sdf.format(new Date());
    }

    public String getBaseNetwork() {
        return baseNetwork;
    }

    public void setBaseNetwork(String baseNetwork) {
        this.baseNetwork = baseNetwork;
    }

    public boolean isIPv4() {
        return isIPv4;
    }

    public void setIPv4(boolean IPv4) {
        isIPv4 = IPv4;
    }

    public List<Department> getDepartments() {
        return departments;
    }

    public void setDepartments(List<Department> departments) {
        this.departments = departments;
    }

    public List<IPv4SubnetResult> getIPv4Results() {
        return ipv4Results;
    }

    public void setIPv4Results(List<IPv4SubnetResult> ipv4Results) {
        this.ipv4Results = ipv4Results;
    }

    public List<IPv6SubnetResult> getIPv6Results() {
        return ipv6Results;
    }

    public void setIPv6Results(List<IPv6SubnetResult> ipv6Results) {
        this.ipv6Results = ipv6Results;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}

