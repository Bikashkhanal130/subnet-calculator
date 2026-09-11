package com.example.vlsmcalculator;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import android.widget.RadioGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Main Activity for VLSM Calculator App
 * Handles user input and displays calculation results with history
 */
public class MainActivity extends AppCompatActivity {

    // UI Components
    private RadioGroup rgIPVersion;
    private MaterialRadioButton rbIPv4;
    private MaterialRadioButton rbIPv6;
    private TextInputLayout tilBaseNetwork;
    private TextInputEditText etBaseNetwork;
    
    private LinearLayout llResults;
    private LinearLayout llHistory;
    private MaterialCardView cardResults;
    private MaterialCardView cardHistory;
    private MaterialButton btnCalculate;
    private MaterialButton btnClear;
    private MaterialButton btnClearHistory;

    // Data storage
    private List<SubnetHistoryItem> historyList;
    private boolean isIPv4Selected = true; // Default to IPv4

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupClickListeners();
        
        // Load history from local storage asynchronously
        historyList = new ArrayList<>(); // Initialize empty list
        new LoadHistoryTask(this, historyList1 -> {
            historyList = historyList1;
            updateHistoryDisplay();
        }).execute();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Save history when app goes to background
        HistoryStorage.saveHistory(this, historyList);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Save history before app is destroyed
        HistoryStorage.saveHistory(this, historyList);
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews() {
        rgIPVersion = findViewById(R.id.rgIPVersion);
        rbIPv4 = findViewById(R.id.rbIPv4);
        rbIPv6 = findViewById(R.id.rbIPv6);
        tilBaseNetwork = findViewById(R.id.tilBaseNetwork);
        etBaseNetwork = findViewById(R.id.etBaseNetwork);
        llResults = findViewById(R.id.llResults);
        llHistory = findViewById(R.id.llHistory);
        cardResults = findViewById(R.id.cardResults);
        cardHistory = findViewById(R.id.cardHistory);
        btnCalculate = findViewById(R.id.btnCalculate);
        btnClear = findViewById(R.id.btnClear);
        btnClearHistory = findViewById(R.id.btnClearHistory);
    }

    /**
     * Setup click listeners for buttons
     */
    private void setupClickListeners() {
        // IP Version selection listener
        rgIPVersion.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbIPv4) {
                isIPv4Selected = true;
                tilBaseNetwork.setHint(getString(R.string.base_network_hint_ipv4));
                etBaseNetwork.setText("192.168.1.0/24");
            } else if (checkedId == R.id.rbIPv6) {
                isIPv4Selected = false;
                tilBaseNetwork.setHint(getString(R.string.base_network_hint_ipv6));
                etBaseNetwork.setText("2001:db8::/64");
            }
        });

        btnCalculate.setOnClickListener(v -> performCalculation());

        btnClear.setOnClickListener(v -> clearAll());
        
        btnClearHistory.setOnClickListener(v -> clearHistory());
    }

    /**
     * Collect all department data from fixed input fields
     */
    private List<Department> collectDepartments() {
        List<Department> departments = new ArrayList<>();
        
        // Collect from fixed inputs
        List<TextInputEditText> nameFields = new ArrayList<>();
        List<TextInputEditText> hostFields = new ArrayList<>();
        
        // Find all department inputs
        // Note: Using child.findViewById() searches within each child view, so even though
        // multiple <include> layouts have the same IDs, we correctly find views within each
        LinearLayout llDepartments = findViewById(R.id.llDepartments);
        if (llDepartments != null) {
            for (int i = 0; i < llDepartments.getChildCount(); i++) {
                View child = llDepartments.getChildAt(i);
                // Search within the specific child view to avoid ID conflicts
                TextInputEditText nameField = child.findViewById(R.id.etDepartmentName);
                TextInputEditText hostField = child.findViewById(R.id.etHostCount);
                if (nameField != null && hostField != null) {
                    nameFields.add(nameField);
                    hostFields.add(hostField);
                }
            }
        }

        for (int i = 0; i < nameFields.size(); i++) {
            TextInputEditText etName = nameFields.get(i);
            TextInputEditText etHosts = hostFields.get(i);

            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String hostsStr = etHosts.getText() != null ? etHosts.getText().toString().trim() : "";

            // Skip empty rows
            if (TextUtils.isEmpty(name) && TextUtils.isEmpty(hostsStr)) {
                continue;
            }

            // Validate department name
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, R.string.error_empty_department, Toast.LENGTH_SHORT).show();
                return null;
            }

            // Validate host count
            if (TextUtils.isEmpty(hostsStr)) {
                Toast.makeText(this, R.string.error_invalid_hosts, Toast.LENGTH_SHORT).show();
                return null;
            }

            try {
                int hosts = Integer.parseInt(hostsStr);
                if (hosts <= 0) {
                    Toast.makeText(this, R.string.error_invalid_hosts, Toast.LENGTH_SHORT).show();
                    return null;
                }
                // Validate host count limits (prevent integer overflow)
                int maxHosts = isIPv4Selected ? 16777214 : Integer.MAX_VALUE; // IPv4: /8 max, IPv6: unlimited
                if (hosts > maxHosts) {
                    Toast.makeText(this, getString(R.string.error_host_count_too_large, maxHosts), Toast.LENGTH_LONG).show();
                    return null;
                }
                departments.add(new Department(name, hosts));
            } catch (NumberFormatException e) {
                Toast.makeText(this, R.string.error_invalid_hosts, Toast.LENGTH_SHORT).show();
                return null;
            }
        }

        return departments;
    }

    /**
     * Perform VLSM calculation and display results
     */
    private void performCalculation() {
        // Get base network input
        String baseNetwork = etBaseNetwork.getText() != null 
                ? etBaseNetwork.getText().toString().trim() 
                : "";

        if (TextUtils.isEmpty(baseNetwork)) {
            Toast.makeText(this, R.string.error_invalid_ip, Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate IP format
        String[] parts = baseNetwork.split("/");
        if (parts.length != 2) {
            Toast.makeText(this, R.string.error_invalid_ip, Toast.LENGTH_SHORT).show();
            return;
        }

        String ipAddress = parts[0];
        
        // Validate IP address matches selected version
        int ipVersion = IPValidator.getIPVersion(ipAddress);
        
        if (ipVersion == 0) {
            Toast.makeText(this, R.string.error_invalid_ip, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if IP version matches selection
        if (isIPv4Selected && ipVersion != 4) {
            Toast.makeText(this, R.string.error_invalid_ipv4_format, Toast.LENGTH_LONG).show();
            return;
        }
        
        if (!isIPv4Selected && ipVersion != 6) {
            Toast.makeText(this, R.string.error_invalid_ipv6_format, Toast.LENGTH_LONG).show();
            return;
        }

        // Collect departments
        List<Department> departments = collectDepartments();
        if (departments == null || departments.isEmpty()) {
            Toast.makeText(this, R.string.error_no_departments, Toast.LENGTH_SHORT).show();
            return;
        }

        // Perform calculation based on selected IP version
        try {
            SubnetHistoryItem historyItem = new SubnetHistoryItem(baseNetwork, isIPv4Selected, departments);
            
            if (isIPv4Selected) {
                List<IPv4SubnetResult> results = IPv4VLSMCalculator.calculateSubnets(baseNetwork, departments);
                if (results == null || results.isEmpty()) {
                    Toast.makeText(this, R.string.error_insufficient_address_space, Toast.LENGTH_LONG).show();
                    return;
                }
                historyItem.setIPv4Results(results);
                calculateAndDisplayIPv4(results);
            } else {
                List<IPv6SubnetResult> results = IPv6VLSMCalculator.calculateSubnets(baseNetwork, departments);
                if (results == null || results.isEmpty()) {
                    Toast.makeText(this, R.string.error_insufficient_address_space, Toast.LENGTH_LONG).show();
                    return;
                }
                historyItem.setIPv6Results(results);
                calculateAndDisplayIPv6(results);
            }
            
            // Add to history
            historyList.add(0, historyItem); // Add at beginning (most recent first)
            updateHistoryDisplay();
            
            // Save history to local storage
            HistoryStorage.saveHistory(this, historyList);
            
        } catch (Exception e) {
            Toast.makeText(this, 
                    getString(R.string.error_insufficient_address_space) + ": " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Calculate and display IPv4 subnets
     */
    private void calculateAndDisplayIPv4(List<IPv4SubnetResult> results) {
        // Clear previous results
        llResults.removeAllViews();
        cardResults.setVisibility(View.VISIBLE);

        // Display each result
        for (IPv4SubnetResult result : results) {
            View resultView = createIPv4ResultView(result);
            llResults.addView(resultView);
        }
    }

    /**
     * Calculate and display IPv6 subnets
     */
    private void calculateAndDisplayIPv6(List<IPv6SubnetResult> results) {
        // Clear previous results
        llResults.removeAllViews();
        cardResults.setVisibility(View.VISIBLE);

        // Display each result
        for (IPv6SubnetResult result : results) {
            View resultView = createIPv6ResultView(result);
            llResults.addView(resultView);
        }
    }

    /**
     * Create a view to display IPv4 subnet result
     */
    private View createIPv4ResultView(IPv4SubnetResult result) {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.item_subnet_result_ipv4, llResults, false);

        TextView tvDepartmentName = view.findViewById(R.id.tvDepartmentName);
        TextView tvNetworkAddress = view.findViewById(R.id.tvNetworkAddress);
        TextView tvSubnetMask = view.findViewById(R.id.tvSubnetMask);
        TextView tvCidrPrefix = view.findViewById(R.id.tvCidrPrefix);
        TextView tvFirstUsableIP = view.findViewById(R.id.tvFirstUsableIP);
        TextView tvLastUsableIP = view.findViewById(R.id.tvLastUsableIP);
        TextView tvBroadcastAddress = view.findViewById(R.id.tvBroadcastAddress);
        TextView tvTotalUsableHosts = view.findViewById(R.id.tvTotalUsableHosts);

        tvDepartmentName.setText(result.getDepartmentName());
        tvNetworkAddress.setText(result.getNetworkAddress());
        tvSubnetMask.setText(result.getSubnetMask());
        tvCidrPrefix.setText("/" + result.getCidrPrefix());
        tvFirstUsableIP.setText(result.getFirstUsableIP());
        tvLastUsableIP.setText(result.getLastUsableIP());
        tvBroadcastAddress.setText(result.getBroadcastAddress());
        tvTotalUsableHosts.setText(String.valueOf(result.getTotalUsableHosts()));

        return view;
    }

    /**
     * Create a view to display IPv6 subnet result
     */
    private View createIPv6ResultView(IPv6SubnetResult result) {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.item_subnet_result_ipv6, llResults, false);

        TextView tvDepartmentName = view.findViewById(R.id.tvDepartmentName);
        TextView tvAssignedPrefix = view.findViewById(R.id.tvAssignedPrefix);
        TextView tvSubnetRangeStart = view.findViewById(R.id.tvSubnetRangeStart);
        TextView tvSubnetRangeEnd = view.findViewById(R.id.tvSubnetRangeEnd);

        tvDepartmentName.setText(result.getDepartmentName());
        tvAssignedPrefix.setText(result.getAssignedPrefix());
        tvSubnetRangeStart.setText(result.getSubnetRangeStart());
        tvSubnetRangeEnd.setText(result.getSubnetRangeEnd());

        return view;
    }
    
    /**
     * Update history display
     */
    private void updateHistoryDisplay() {
        llHistory.removeAllViews();
        
        if (historyList.isEmpty()) {
            return;
        }
        
        for (SubnetHistoryItem item : historyList) {
            View historyView = createHistoryView(item);
            llHistory.addView(historyView);
        }
    }
    
    /**
     * Create a view for a history item
     */
    private View createHistoryView(SubnetHistoryItem item) {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.item_history_entry, llHistory, false);
        
        TextView tvBaseNetwork = view.findViewById(R.id.tvHistoryBaseNetwork);
        TextView tvTimestamp = view.findViewById(R.id.tvHistoryTimestamp);
        TextView tvIPVersion = view.findViewById(R.id.tvHistoryIPVersion);
        TextView tvDepartments = view.findViewById(R.id.tvHistoryDepartments);
        MaterialButton btnExpand = view.findViewById(R.id.btnExpandHistory);
        LinearLayout llDetails = view.findViewById(R.id.llHistoryDetails);
        LinearLayout llHistoryResults = view.findViewById(R.id.llHistoryResults);
        
        // Set base network and timestamp
        tvBaseNetwork.setText(item.getBaseNetwork());
        tvTimestamp.setText(item.getTimestamp());
        
        // Set IP version badge
        tvIPVersion.setText(item.isIPv4() ? "IPv4" : "IPv6");
        
        // Set departments summary
        StringBuilder deptSummary = new StringBuilder();
        for (Department dept : item.getDepartments()) {
            if (deptSummary.length() > 0) deptSummary.append(", ");
            deptSummary.append(dept.getName()).append(" (").append(dept.getRequiredHosts()).append(")");
        }
        tvDepartments.setText(deptSummary.toString());
        
        // Setup expand/collapse functionality
        btnExpand.setOnClickListener(v -> {
            if (llDetails.getVisibility() == View.GONE) {
                llDetails.setVisibility(View.VISIBLE);
                btnExpand.setText(getString(R.string.hide_details));
                
                // Populate results if not already done
                if (llHistoryResults.getChildCount() == 0) {
                    if (item.isIPv4() && item.getIPv4Results() != null) {
                        for (IPv4SubnetResult result : item.getIPv4Results()) {
                            View resultView = createIPv4ResultView(result);
                            llHistoryResults.addView(resultView);
                        }
                    } else if (!item.isIPv4() && item.getIPv6Results() != null) {
                        for (IPv6SubnetResult result : item.getIPv6Results()) {
                            View resultView = createIPv6ResultView(result);
                            llHistoryResults.addView(resultView);
                        }
                    }
                }
            } else {
                llDetails.setVisibility(View.GONE);
                btnExpand.setText(getString(R.string.view_details));
            }
        });
        
        return view;
    }
    
    /**
     * Clear history
     */
    private void clearHistory() {
        historyList.clear();
        llHistory.removeAllViews();
        // Clear history from local storage
        HistoryStorage.clearHistory(this);
        Toast.makeText(this, R.string.history_cleared, Toast.LENGTH_SHORT).show();
    }

    /**
     * Clear all inputs and results
     */
    private void clearAll() {
        // Reset IP version to IPv4
        rbIPv4.setChecked(true);
        isIPv4Selected = true;
        tilBaseNetwork.setHint(getString(R.string.base_network_hint_ipv4));
        etBaseNetwork.setText("192.168.1.0/24");

        // Clear department inputs
        LinearLayout llDepartments = findViewById(R.id.llDepartments);
        if (llDepartments != null) {
            for (int i = 0; i < llDepartments.getChildCount(); i++) {
                View child = llDepartments.getChildAt(i);
                TextInputEditText nameField = child.findViewById(R.id.etDepartmentName);
                TextInputEditText hostField = child.findViewById(R.id.etHostCount);
                if (nameField != null) nameField.setText("");
                if (hostField != null) hostField.setText("");
            }
        }

        // Clear results
        llResults.removeAllViews();
        cardResults.setVisibility(View.GONE);
    }
}
