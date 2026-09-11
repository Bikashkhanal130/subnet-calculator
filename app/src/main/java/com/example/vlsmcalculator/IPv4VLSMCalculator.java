package com.example.vlsmcalculator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 *  Binary Masking and Prefix Arithmetic
 */
public class IPv4VLSMCalculator {
    

    public static List<IPv4SubnetResult> calculateSubnets(String baseNetwork, List<Department> departments) {
        List<IPv4SubnetResult> results = new ArrayList<>();
        
        if (departments == null || departments.isEmpty()) {
            return results;
        }
        
        // check base network
        String[] parts = baseNetwork.split("/");
        if (parts.length != 2) {
            return results; // Invalid vayema
        }
        
        String baseIP = parts[0];
        int basePrefix;
        try {
            basePrefix = Integer.parseInt(parts[1]);
            // Validate prefix range
            if (basePrefix < 0 || basePrefix > 32) {
                return results; // Invalid prefix range ma
            }
        } catch (NumberFormatException e) {
            return results; // Invalid prefix vayema
        }
        
        // Convert base IP to integer for arithmetic operations
        long baseNetworkLong;
        try {
            baseNetworkLong = ipToLong(baseIP);
        } catch (IllegalArgumentException e) {
            return results; // Invalid IP format
        }
        
        // Validate network alignment (host bits must be zero)
        if (!isNetworkAligned(baseNetworkLong, basePrefix)) {
            return results; // Network not properly aligned
        }
        
        // department halne by host requirement (thulo dekhi sano)
        List<Department> sortedDepartments = new ArrayList<>(departments);
        Collections.sort(sortedDepartments, new Comparator<Department>() {
            @Override
            public int compare(Department d1, Department d2) {
                return Integer.compare(d2.getRequiredHosts(), d1.getRequiredHosts());
            }
        });
        
        long currentNetwork = baseNetworkLong;
        
        // Calculate subnet for each department
        for (Department dept : sortedDepartments) {
            IPv4SubnetResult result = calculateSubnetForDepartment(
                dept, currentNetwork, basePrefix
            );
            
            if (result != null) {
                // Validate subnet is within base network range
                if (!isSubnetWithinBaseNetwork(currentNetwork, result.getCidrPrefix(), 
                        baseNetworkLong, basePrefix)) {
                    return results; // Subnet exceeds base network range
                }
                
                results.add(result);
                // Calculate next network address using prefix arithmetic
                currentNetwork = getNextNetworkAddress(
                    currentNetwork, 
                    result.getCidrPrefix()
                );
                
                // Validate next network is still within base network
                if (!isAddressWithinBaseNetwork(currentNetwork, baseNetworkLong, basePrefix)) {
                    break; // Next network would exceed base network
                }
            }
        }
        
        return results;
    }
    
    /**
     * Binary Masking
     * 
     * Algorithm:
     * 1. Bit khojne according to require (2^n >= hosts + 2)
     * 2. total bit dekhi naya bit ghataune for new subnet (32 - host_bits)
     * 3. bianary operation garera mask dine naya 255.255.255.240 eg
     * 4. antim ma network id host usuable ra broadcast
     */
    private static IPv4SubnetResult calculateSubnetForDepartment(
            Department dept, long networkAddress, int basePrefix) {
        
        int requiredHosts = dept.getRequiredHosts();
        
        // Step 1
        // Formula: 2^hostBits >= requiredHosts + 2 (network + broadcast)
        int hostBits = calculateRequiredHostBits(requiredHosts);
        
        // Step 2: total bit dekhi naya bit ghataune for new subnet (32 - host_bits)
        int subnetPrefix = 32 - hostBits;
        
        // exceed na hoss
        if (subnetPrefix < basePrefix) {
            return null; // Invalid vanne
        }
        
        // Step 3:  bianary operation calculate vayo
        long subnetMask = calculateSubnetMask(subnetPrefix);
        
        // Step 4:  subnet mask halne network address ma using binary AND operation
        long networkLong = networkAddress & subnetMask;
        
        // Step 5: Aba broadcast address using binary OR with inverted mask
        long invertedMask = ~subnetMask;
        long broadcastLong = networkLong | invertedMask;
        
        // Step 6: aba chai first and last usable IPs
        long firstUsableLong = networkLong + 1;
        long lastUsableLong = broadcastLong - 1;
        
        // Step 7:total usable hosts
        int totalUsableHosts = (int) Math.pow(2, hostBits) - 2;
        
        // result object banaune
        IPv4SubnetResult result = new IPv4SubnetResult(dept.getName());
        result.setNetworkAddress(longToIP(networkLong));
        result.setSubnetMask(longToIP(subnetMask));
        result.setCidrPrefix(subnetPrefix);
        result.setFirstUsableIP(longToIP(firstUsableLong));
        result.setLastUsableIP(longToIP(lastUsableLong));
        result.setBroadcastAddress(longToIP(broadcastLong));
        result.setTotalUsableHosts(totalUsableHosts);
        
        return result;
    }
    
    /**
     * Calculate required host bits using binary masking
     * Finds minimum n where 2^n >= (requiredHosts + 2)
     * +2 accounts for network and broadcast addresses
     */
    private static int calculateRequiredHostBits(int requiredHosts) {
        int totalRequired = requiredHosts + 2; // Network + Broadcast
        int hostBits = 0;
        long totalAddresses = 1;
        
        // Binary masking approach: find power of 2 that covers requirements
        while (totalAddresses < totalRequired) {
            hostBits++;
            totalAddresses = totalAddresses << 1; // Left shift = multiply by 2
        }
        
        return hostBits;
    }
    
    /**
     * Calculate subnet mask from CIDR prefix using binary operations
     * Creates a mask with prefix number of 1s followed by 0s
     */
    private static long calculateSubnetMask(int prefix) {
        if (prefix == 0) {
            return 0L;
        }
        if (prefix == 32) {
            return 0xFFFFFFFFL;
        }
        
        // Create mask by shifting: (2^prefix - 1) << (32 - prefix)
        // This creates prefix number of 1s in the most significant bits
        long mask = (1L << (32 - prefix)) - 1;
        mask = ~mask; // Invert to get 1s in MSB positions
        return mask & 0xFFFFFFFFL; // Ensure 32-bit unsigned
    }
    
    /**
     * Calculate next network address for VLSM continuation
     * Uses prefix arithmetic: next = current + 2^(32 - prefix)
     */
    private static long getNextNetworkAddress(long currentNetwork, int prefix) {
        int hostBits = 32 - prefix;
        long increment = 1L << hostBits; // 2^hostBits
        return currentNetwork + increment;
    }
    
    /**
     * Convert IP address string to long integer for binary operations
     */
    private static long ipToLong(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid IP address format");
        }
        long result = 0;
        try {
            for (int i = 0; i < 4; i++) {
                long octet = Long.parseLong(parts[i]);
                if (octet < 0 || octet > 255) {
                    throw new IllegalArgumentException("IP octet out of range: " + octet);
                }
                result = (result << 8) | octet;
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid IP address format", e);
        }
        return result & 0xFFFFFFFFL;
    }
    
    /**
     * Convert long integer to IP address string
     */
    private static String longToIP(long ip) {
        return String.format("%d.%d.%d.%d",
            (ip >> 24) & 0xFF,
            (ip >> 16) & 0xFF,
            (ip >> 8) & 0xFF,
            ip & 0xFF
        );
    }
    
    /**
     * Validate that network address is properly aligned (host bits are zero)
     */
    private static boolean isNetworkAligned(long networkAddress, int prefix) {
        int hostBits = 32 - prefix;
        long hostMask = (1L << hostBits) - 1; // Mask for host bits
        return (networkAddress & hostMask) == 0;
    }
    
    /**
     * Check if an address is within the base network range
     */
    private static boolean isAddressWithinBaseNetwork(long address, long baseNetwork, int basePrefix) {
        long baseMask = calculateSubnetMask(basePrefix);
        long networkPortion = address & baseMask;
        return networkPortion == (baseNetwork & baseMask);
    }
    
    /**
     * Check if a subnet fits within the base network
     */
    private static boolean isSubnetWithinBaseNetwork(long subnetNetwork, int subnetPrefix, 
            long baseNetwork, int basePrefix) {
        // Subnet prefix must be >= base prefix (subnet must be equal or smaller)
        if (subnetPrefix < basePrefix) {
            return false;
        }
        
        // Subnet network must be within base network
        return isAddressWithinBaseNetwork(subnetNetwork, baseNetwork, basePrefix);
    }
}


