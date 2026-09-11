package com.example.vlsmcalculator;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * VLSM Calculator for IPv6 using Binary Masking and Prefix Arithmetic
 * IPv6 subnetting follows similar principles but uses 128-bit addresses
 */
public class IPv6VLSMCalculator {
    
    private static final int IPv6_BITS = 128;
    
    /**
     * Calculate VLSM subnets for IPv6
     * 
     * @param baseNetwork The base network address (e.g., "2001:db8::/64")
     * @param departments List of departments with host requirements
     * @return List of calculated subnet results
     */
    public static List<IPv6SubnetResult> calculateSubnets(String baseNetwork, List<Department> departments) {
        List<IPv6SubnetResult> results = new ArrayList<>();
        
        if (departments == null || departments.isEmpty()) {
            return results;
        }
        
        // Parse base network
        String[] parts = baseNetwork.split("/");
        if (parts.length != 2) {
            return results; // Invalid format
        }
        
        String baseIP = parts[0];
        int basePrefix;
        try {
            basePrefix = Integer.parseInt(parts[1]);
            // Validate prefix range
            if (basePrefix < 0 || basePrefix > IPv6_BITS) {
                return results; // Invalid prefix range
            }
        } catch (NumberFormatException e) {
            return results; // Invalid prefix format
        }
        
        // Convert base IP to BigInteger for 128-bit arithmetic
        BigInteger baseNetworkBigInt = ipv6ToBigInteger(baseIP);
        
        // Validate network alignment (host bits must be zero)
        if (!isIPv6NetworkAligned(baseNetworkBigInt, basePrefix)) {
            return results; // Network not properly aligned
        }
        
        // Sort departments by host requirement (descending order)
        List<Department> sortedDepartments = new ArrayList<>(departments);
        Collections.sort(sortedDepartments, new Comparator<Department>() {
            @Override
            public int compare(Department d1, Department d2) {
                return Integer.compare(d2.getRequiredHosts(), d1.getRequiredHosts());
            }
        });
        
        BigInteger currentNetwork = baseNetworkBigInt;
        
        // Calculate subnet for each department
        for (Department dept : sortedDepartments) {
            SubnetInfo info = calculateSubnetForDepartment(
                dept, currentNetwork, basePrefix
            );
            
            if (info != null && info.result != null) {
                // Validate subnet is within base network range
                if (!isIPv6SubnetWithinBaseNetwork(currentNetwork, info.subnetPrefix, 
                        baseNetworkBigInt, basePrefix)) {
                    return results; // Subnet exceeds base network range
                }
                
                results.add(info.result);
                // Calculate next network address using prefix arithmetic
                currentNetwork = getNextNetworkAddress(currentNetwork, info.subnetPrefix);
                
                // Validate next network is still within base network
                if (!isIPv6AddressWithinBaseNetwork(currentNetwork, baseNetworkBigInt, basePrefix)) {
                    break; // Next network would exceed base network
                }
            }
        }
        
        return results;
    }
    
    /**
     * Helper class to return both result and prefix
     */
    private static class SubnetInfo {
        IPv6SubnetResult result;
        int subnetPrefix;
        
        SubnetInfo(IPv6SubnetResult result, int subnetPrefix) {
            this.result = result;
            this.subnetPrefix = subnetPrefix;
        }
    }
    
    /**
     * Calculate subnet for a single department using Binary Masking for IPv6
     */
    private static SubnetInfo calculateSubnetForDepartment(
            Department dept, BigInteger networkAddress, int basePrefix) {
        
        int requiredHosts = dept.getRequiredHosts();
        
        // Step 1: Calculate required host bits using binary masking
        int hostBits = calculateRequiredHostBits(requiredHosts);
        
        // Step 2: Calculate subnet prefix using prefix arithmetic
        int subnetPrefix = IPv6_BITS - hostBits;
        
        // For IPv6, typically use /64 for subnets, but allow smaller for VLSM
        // Ensure subnet prefix doesn't exceed base prefix
        if (subnetPrefix < basePrefix) {
            return new SubnetInfo(null, subnetPrefix);
        }
        
        // Step 3: Calculate subnet mask using binary masking (128-bit)
        BigInteger subnetMask = calculateIPv6SubnetMask(subnetPrefix);
        
        // Step 4: Apply subnet mask to network address using binary AND
        BigInteger networkBigInt = networkAddress.and(subnetMask);
        
        // Step 5: Calculate subnet range
        // First address is network address
        // Last address is network + (2^hostBits - 1)
        BigInteger increment = BigInteger.valueOf(2).pow(hostBits);
        BigInteger lastAddress = networkBigInt.add(increment).subtract(BigInteger.ONE);
        
        // Create result object
        IPv6SubnetResult result = new IPv6SubnetResult(dept.getName());
        result.setAssignedPrefix(bigIntegerToIPv6(networkBigInt) + "/" + subnetPrefix);
        result.setSubnetRangeStart(bigIntegerToIPv6(networkBigInt));
        result.setSubnetRangeEnd(bigIntegerToIPv6(lastAddress));
        
        return new SubnetInfo(result, subnetPrefix);
    }
    
    /**
     * Calculate required host bits using binary masking for IPv6
     */
    private static int calculateRequiredHostBits(int requiredHosts) {
        // IPv6 doesn't reserve network/broadcast like IPv4, but we still need
        // enough bits to represent all required hosts
        int hostBits = 0;
        long totalAddresses = 1;
        
        while (totalAddresses < requiredHosts) {
            hostBits++;
            if (hostBits >= 64) break; // Prevent overflow
            totalAddresses = totalAddresses << 1;
        }
        
        // IPv6 subnets typically use at least /64, so minimum host bits = 64
        // But for VLSM with small departments, we allow smaller
        return Math.max(hostBits, 1);
    }
    
    /**
     * Calculate IPv6 subnet mask from CIDR prefix using binary operations
     * Creates a mask with prefix number of 1s in MSB positions, followed by 0s
     */
    private static BigInteger calculateIPv6SubnetMask(int prefix) {
        if (prefix == 0) {
            return BigInteger.ZERO;
        }
        if (prefix == IPv6_BITS) {
            return new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16);
        }
        
        // Create mask with prefix number of 1s followed by (128-prefix) number of 0s
        // Calculate: (2^prefix - 1) << (128 - prefix)
        // But since we need to work with 128 bits, we use BigInteger operations
        BigInteger fullMask = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16);
        int hostBits = IPv6_BITS - prefix;
        
        // Create a mask with hostBits number of 1s in LSB
        BigInteger hostMask = BigInteger.ONE.shiftLeft(hostBits).subtract(BigInteger.ONE);
        
        // Invert to get network mask (1s in MSB, 0s in LSB)
        BigInteger networkMask = fullMask.xor(hostMask);
        
        return networkMask;
    }
    
    /**
     * Calculate next network address for IPv6 VLSM continuation
     */
    private static BigInteger getNextNetworkAddress(BigInteger currentNetwork, int prefix) {
        int hostBits = IPv6_BITS - prefix;
        BigInteger increment = BigInteger.valueOf(2).pow(hostBits);
        return currentNetwork.add(increment);
    }
    
    /**
     * Convert IPv6 address string to BigInteger
     */
    private static BigInteger ipv6ToBigInteger(String ip) {
        // Expand shortened IPv6 addresses
        String expanded = expandIPv6(ip);
        String[] parts = expanded.split(":");
        StringBuilder hexString = new StringBuilder();
        
        for (String part : parts) {
            hexString.append(String.format("%4s", part).replace(' ', '0'));
        }
        
        return new BigInteger(hexString.toString(), 16);
    }
    
    /**
     * Convert BigInteger to IPv6 address string
     */
    private static String bigIntegerToIPv6(BigInteger ip) {
        String hex = ip.toString(16);
        // Pad to 32 hex characters (128 bits)
        while (hex.length() < 32) {
            hex = "0" + hex;
        }
        
        // Format as IPv6: xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (i > 0) result.append(":");
            result.append(hex.substring(i * 4, (i * 4) + 4));
        }
        
        return compressIPv6(result.toString());
    }
    
    /**
     * Expand shortened IPv6 address (e.g., "::1" -> "0000:0000:0000:0000:0000:0000:0000:0001")
     */
    private static String expandIPv6(String ip) {
        if (ip.contains("::")) {
            String[] parts = ip.split("::");
            int leftParts = parts[0].isEmpty() ? 0 : parts[0].split(":").length;
            int rightParts = parts.length > 1 && !parts[1].isEmpty() ? parts[1].split(":").length : 0;
            int missingParts = 8 - leftParts - rightParts;
            
            StringBuilder expanded = new StringBuilder();
            if (leftParts > 0) {
                expanded.append(parts[0]).append(":");
            }
            for (int i = 0; i < missingParts; i++) {
                expanded.append("0000:");
            }
            if (rightParts > 0) {
                expanded.append(parts[1]);
            }
            return expanded.toString().replaceAll(":$", "");
        }
        return ip;
    }
    
    /**
     * Compress IPv6 address by removing leading zeros and consecutive zero groups
     */
    private static String compressIPv6(String ip) {
        // Remove leading zeros from each group
        String[] parts = ip.split(":");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].replaceFirst("^0+", "");
            if (parts[i].isEmpty()) parts[i] = "0";
        }
        return String.join(":", parts);
    }
    
    /**
     * Validate that IPv6 network address is properly aligned (host bits are zero)
     */
    private static boolean isIPv6NetworkAligned(BigInteger networkAddress, int prefix) {
        int hostBits = IPv6_BITS - prefix;
        if (hostBits == 0) {
            return true; // /128 is always aligned
        }
        BigInteger hostMask = BigInteger.ONE.shiftLeft(hostBits).subtract(BigInteger.ONE);
        return networkAddress.and(hostMask).equals(BigInteger.ZERO);
    }
    
    /**
     * Check if an IPv6 address is within the base network range
     */
    private static boolean isIPv6AddressWithinBaseNetwork(BigInteger address, 
            BigInteger baseNetwork, int basePrefix) {
        BigInteger baseMask = calculateIPv6SubnetMask(basePrefix);
        BigInteger networkPortion = address.and(baseMask);
        BigInteger baseNetworkPortion = baseNetwork.and(baseMask);
        return networkPortion.equals(baseNetworkPortion);
    }
    
    /**
     * Check if an IPv6 subnet fits within the base network
     */
    private static boolean isIPv6SubnetWithinBaseNetwork(BigInteger subnetNetwork, int subnetPrefix, 
            BigInteger baseNetwork, int basePrefix) {
        // Subnet prefix must be >= base prefix (subnet must be equal or smaller)
        if (subnetPrefix < basePrefix) {
            return false;
        }
        
        // Subnet network must be within base network
        return isIPv6AddressWithinBaseNetwork(subnetNetwork, baseNetwork, basePrefix);
    }
}

