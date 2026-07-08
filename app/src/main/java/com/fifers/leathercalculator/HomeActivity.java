package com.fifers.leathercalculator;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

public class HomeActivity extends Activity {

    private static final String PREF_NAME = "leather_calculator_prefs";
    private static final String KEY_PRODUCTS = "products";
    private static final String KEY_CALCULATIONS = "calculation_records";

    private SharedPreferences preferences;
    private TextView dashboardProductCountTextView;
    private TextView dashboardRecordCountTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Utils.applyBottomSystemInset(findViewById(R.id.homeScreenRoot));

        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        dashboardProductCountTextView = findViewById(R.id.dashboardProductCountTextView);
        dashboardRecordCountTextView = findViewById(R.id.dashboardRecordCountTextView);

        findViewById(R.id.homeAddProductButton).setOnClickListener(v ->
                startActivity(new Intent(this, AddProductActivity.class)));
        findViewById(R.id.homeCalculateButton).setOnClickListener(v ->
                startActivity(new Intent(this, CalculateActivity.class)));
        findViewById(R.id.homeReportsButton).setOnClickListener(v ->
                startActivity(new Intent(this, MonthlyResultsActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDashboardStats();
    }

    private void updateDashboardStats() {
        dashboardProductCountTextView.setText(Utils.toPersianDigits(String.valueOf(readArray(KEY_PRODUCTS).length())));
        dashboardRecordCountTextView.setText(Utils.toPersianDigits(String.valueOf(readArray(KEY_CALCULATIONS).length())));
    }

    private JSONArray readArray(String key) {
        String json = preferences.getString(key, "[]");
        try {
            return new JSONArray(json);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }
}
