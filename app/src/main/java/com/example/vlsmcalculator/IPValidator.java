package com.example.vlsmcalculator;

import java.util.regex.Pattern;

/**
 * Utility class for validating IPv4 and IPv6 addresses
 */
public class IPValidator {
    
    // IPv4 pattern: 0.0.0.0 to 255.255.255.255
    private static final Pattern IPv4_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    
    // IPv6 pattern (simplified - handles standard format)
    private static final Pattern IPv6_PATTERN = Pattern.compile(
        "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|^::1$|^::$|^([0-9a-fA-F]{1,4}:){1,7}:$|^:([0-9a-fA-F]{1,4}:){1,7}$|^([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}$|^([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}$|^([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}$|^([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}$|^([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}$|^[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})$"
    );
    
    /**
     * Validates if the given string is a valid IPv4 address
     */
    public static boolean isValidIPv4(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        return IPv4_PATTERN.matcher(ip.trim()).matches();
    }
    
    /**
     * Validates if the given string is a valid IPv6 address
     */
    public static boolean isValidIPv6(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        String trimmed = ip.trim();
        // Check standard format
        if (IPv6_PATTERN.matcher(trimmed).matches()) {
            return true;
        }
        // Check for IPv4-mapped IPv6 addresses (::ffff:192.168.1.1)
        if (trimmed.toLowerCase().startsWith("::ffff:") && trimmed.length() > 7) {
            String ipv4Part = trimmed.substring(7);
            return isValidIPv4(ipv4Part);
        }
        return false;
    }
    
    /**
     * Determines if an IP address is IPv4 or IPv6
     * @return 4 for IPv4, 6 for IPv6, 0 for invalid
     */
    public static int getIPVersion(String ip) {
        if (isValidIPv4(ip)) {
            return 4;
        } else if (isValidIPv6(ip)) {
            return 6;
        }
        return 0;
    }
}


