package com.example.vlsmcalculator;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Utility class to save and load subnet calculation history to/from local storage
 */
public class HistoryStorage {
    private static final String HISTORY_FILE_NAME = "vlsm_history.json";
    private static final String TAG = "HistoryStorage";

    /**
     * Save history list to local storage (asynchronous)
     */
    public static void saveHistory(Context context, List<SubnetHistoryItem> historyList) {
        new SaveHistoryTask(context, historyList).execute();
    }
    
    /**
     * Save history synchronously (internal use)
     */
    private static void saveHistorySync(Context context, List<SubnetHistoryItem> historyList) {
        try {
            JSONArray jsonArray = new JSONArray();
            
            for (SubnetHistoryItem item : historyList) {
                JSONObject itemJson = new JSONObject();
                itemJson.put("baseNetwork", item.getBaseNetwork());
                itemJson.put("isIPv4", item.isIPv4());
                itemJson.put("timestamp", item.getTimestamp());
                
                // Save departments
                JSONArray deptArray = new JSONArray();
                for (Department dept : item.getDepartments()) {
                    JSONObject deptJson = new JSONObject();
                    deptJson.put("name", dept.getName());
                    deptJson.put("hosts", dept.getRequiredHosts());
                    deptArray.put(deptJson);
                }
                itemJson.put("departments", deptArray);
                
                // Save IPv4 results
                if (item.getIPv4Results() != null) {
                    JSONArray ipv4ResultsArray = new JSONArray();
                    for (IPv4SubnetResult result : item.getIPv4Results()) {
                        JSONObject resultJson = new JSONObject();
                        resultJson.put("departmentName", result.getDepartmentName());
                        resultJson.put("networkAddress", result.getNetworkAddress());
                        resultJson.put("subnetMask", result.getSubnetMask());
                        resultJson.put("cidrPrefix", result.getCidrPrefix());
                        resultJson.put("firstUsableIP", result.getFirstUsableIP());
                        resultJson.put("lastUsableIP", result.getLastUsableIP());
                        resultJson.put("broadcastAddress", result.getBroadcastAddress());
                        resultJson.put("totalUsableHosts", result.getTotalUsableHosts());
                        ipv4ResultsArray.put(resultJson);
                    }
                    itemJson.put("ipv4Results", ipv4ResultsArray);
                }
                
                // Save IPv6 results
                if (item.getIPv6Results() != null) {
                    JSONArray ipv6ResultsArray = new JSONArray();
                    for (IPv6SubnetResult result : item.getIPv6Results()) {
                        JSONObject resultJson = new JSONObject();
                        resultJson.put("departmentName", result.getDepartmentName());
                        resultJson.put("assignedPrefix", result.getAssignedPrefix());
                        resultJson.put("subnetRangeStart", result.getSubnetRangeStart());
                        resultJson.put("subnetRangeEnd", result.getSubnetRangeEnd());
                        ipv6ResultsArray.put(resultJson);
                    }
                    itemJson.put("ipv6Results", ipv6ResultsArray);
                }
                
                jsonArray.put(itemJson);
            }
            
            // Write to file
            FileOutputStream fos = context.openFileOutput(HISTORY_FILE_NAME, Context.MODE_PRIVATE);
            fos.write(jsonArray.toString().getBytes());
            fos.close();
            
            Log.d(TAG, "History saved successfully: " + historyList.size() + " items");
            
        } catch (JSONException | IOException e) {
            Log.e(TAG, "Error saving history", e);
        }
    }

    /**
     * Load history list from local storage (synchronous - call from background thread)
     */
    public static List<SubnetHistoryItem> loadHistory(Context context) {
        List<SubnetHistoryItem> historyList = new ArrayList<>();
        
        try {
            FileInputStream fis = context.openFileInput(HISTORY_FILE_NAME);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            
            fis.close();
            
            String jsonString = sb.toString();
            if (jsonString.isEmpty()) {
                return historyList;
            }
            
            JSONArray jsonArray = new JSONArray(jsonString);
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject itemJson = jsonArray.getJSONObject(i);
                
                String baseNetwork = itemJson.getString("baseNetwork");
                boolean isIPv4 = itemJson.getBoolean("isIPv4");
                String timestamp = itemJson.getString("timestamp");
                
                // Load departments
                List<Department> departments = new ArrayList<>();
                JSONArray deptArray = itemJson.getJSONArray("departments");
                for (int j = 0; j < deptArray.length(); j++) {
                    JSONObject deptJson = deptArray.getJSONObject(j);
                    String name = deptJson.getString("name");
                    int hosts = deptJson.getInt("hosts");
                    departments.add(new Department(name, hosts));
                }
                
                SubnetHistoryItem item = new SubnetHistoryItem(baseNetwork, isIPv4, departments);
                item.setTimestamp(timestamp);
                
                // Load IPv4 results
                if (itemJson.has("ipv4Results") && !itemJson.isNull("ipv4Results")) {
                    List<IPv4SubnetResult> ipv4Results = new ArrayList<>();
                    JSONArray ipv4ResultsArray = itemJson.getJSONArray("ipv4Results");
                    for (int j = 0; j < ipv4ResultsArray.length(); j++) {
                        JSONObject resultJson = ipv4ResultsArray.getJSONObject(j);
                        IPv4SubnetResult result = new IPv4SubnetResult(resultJson.getString("departmentName"));
                        result.setNetworkAddress(resultJson.getString("networkAddress"));
                        result.setSubnetMask(resultJson.getString("subnetMask"));
                        result.setCidrPrefix(resultJson.getInt("cidrPrefix"));
                        result.setFirstUsableIP(resultJson.getString("firstUsableIP"));
                        result.setLastUsableIP(resultJson.getString("lastUsableIP"));
                        result.setBroadcastAddress(resultJson.getString("broadcastAddress"));
                        result.setTotalUsableHosts(resultJson.getInt("totalUsableHosts"));
                        ipv4Results.add(result);
                    }
                    item.setIPv4Results(ipv4Results);
                }
                
                // Load IPv6 results
                if (itemJson.has("ipv6Results") && !itemJson.isNull("ipv6Results")) {
                    List<IPv6SubnetResult> ipv6Results = new ArrayList<>();
                    JSONArray ipv6ResultsArray = itemJson.getJSONArray("ipv6Results");
                    for (int j = 0; j < ipv6ResultsArray.length(); j++) {
                        JSONObject resultJson = ipv6ResultsArray.getJSONObject(j);
                        IPv6SubnetResult result = new IPv6SubnetResult(resultJson.getString("departmentName"));
                        result.setAssignedPrefix(resultJson.getString("assignedPrefix"));
                        result.setSubnetRangeStart(resultJson.getString("subnetRangeStart"));
                        result.setSubnetRangeEnd(resultJson.getString("subnetRangeEnd"));
                        ipv6Results.add(result);
                    }
                    item.setIPv6Results(ipv6Results);
                }
                
                historyList.add(item);
            }
            
            Log.d(TAG, "History loaded successfully: " + historyList.size() + " items");
            
        } catch (IOException | JSONException e) {
            // File doesn't exist yet or error reading - return empty list
            Log.d(TAG, "No existing history file or error loading: " + e.getMessage());
        }
        
        return historyList;
    }

    /**
     * Clear history from storage
     */
    public static void clearHistory(Context context) {
        try {
            context.deleteFile(HISTORY_FILE_NAME);
            Log.d(TAG, "History file deleted");
        } catch (Exception e) {
            Log.e(TAG, "Error deleting history file", e);
        }
    }
    
    /**
     * AsyncTask for saving history in background
     */
    private static class SaveHistoryTask extends AsyncTask<Void, Void, Void> {
        private Context context;
        private List<SubnetHistoryItem> historyList;
        
        SaveHistoryTask(Context context, List<SubnetHistoryItem> historyList) {
            this.context = context.getApplicationContext(); // Use application context to avoid leaks
            this.historyList = new ArrayList<>(historyList); // Create copy to avoid concurrency issues
        }
        
        @Override
        protected Void doInBackground(Void... voids) {
            saveHistorySync(context, historyList);
            return null;
        }
    }
}


