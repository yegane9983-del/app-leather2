package com.fifers.leathercalculator;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CalculateActivity extends Activity {

    private static final String PREF_NAME = "leather_calculator_prefs";
    private static final String KEY_PRODUCTS = "products";
    private static final String KEY_CALCULATIONS = "calculation_records";

    private final ArrayList<Product> products = new ArrayList<>();
    private SharedPreferences preferences;
    private List<String> yearOptions;

    private EditText quantityEditText;
    private EditText actualLeatherEditText;
    private TextView resultTextView;
    private TextView resultStatusTextView;
    private TextView resultDifferenceTextView;
    private TextView resultProductTextView;
    private TextView resultExpectedTextView;
    private TextView resultActualTextView;
    private View resultCard;
    private AutoCompleteTextView productSearchEditText;
    private Spinner monthSpinner;
    private Spinner yearSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculate);
        Utils.applyBottomSystemInset(findViewById(R.id.calculateScreenRoot));

        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        bindViews();
        loadProducts();
        setupAdapters();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProducts();
        refreshProductSuggestions();
    }

    private void bindViews() {
        quantityEditText = findViewById(R.id.quantityEditText);
        actualLeatherEditText = findViewById(R.id.actualLeatherEditText);
        resultCard = findViewById(R.id.resultCard);
        resultTextView = findViewById(R.id.resultTextView);
        resultStatusTextView = findViewById(R.id.resultStatusTextView);
        resultDifferenceTextView = findViewById(R.id.resultDifferenceTextView);
        resultProductTextView = findViewById(R.id.resultProductTextView);
        resultExpectedTextView = findViewById(R.id.resultExpectedTextView);
        resultActualTextView = findViewById(R.id.resultActualTextView);
        productSearchEditText = findViewById(R.id.productSearchEditText);
        monthSpinner = findViewById(R.id.monthSpinner);
        yearSpinner = findViewById(R.id.yearSpinner);

        Button backButton = findViewById(R.id.backButton);
        Button calculateButton = findViewById(R.id.calculateButton);

        backButton.setOnClickListener(v -> finish());
        calculateButton.setOnClickListener(v -> calculateDifference());
    }

    private ArrayAdapter<String> productSearchAdapter;

    private void setupAdapters() {
        productSearchAdapter = new ArrayAdapter<>(this, R.layout.autocomplete_dropdown_item,
                R.id.dropdownText, productNames());
        productSearchEditText.setAdapter(productSearchAdapter);
        productSearchEditText.setThreshold(1);
        productSearchEditText.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus && !products.isEmpty()) {
                productSearchEditText.showDropDown();
            }
        });
        productSearchEditText.setOnClickListener(view -> {
            if (!products.isEmpty()) {
                productSearchEditText.showDropDown();
            }
        });

        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, R.layout.spinner_selected_item,
                R.id.spinnerText, Utils.MONTHS);
        monthAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        monthSpinner.setAdapter(monthAdapter);

        yearOptions = Utils.jalaliYearOptions();
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, R.layout.spinner_selected_item,
                R.id.spinnerText, yearOptions);
        yearAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        yearSpinner.setAdapter(yearAdapter);
        yearSpinner.setSelection(Utils.currentJalaliYearIndexIn(yearOptions));
    }

    private void refreshProductSuggestions() {
        if (productSearchAdapter == null) return;
        productSearchAdapter.clear();
        productSearchAdapter.addAll(productNames());
        productSearchAdapter.notifyDataSetChanged();
    }

    private void calculateDifference() {
        if (products.isEmpty()) {
            showToast("ابتدا حداقل یک محصول از بخش «افزودن محصول» اضافه کنید.");
            return;
        }

        String searchedProductName = productSearchEditText.getText().toString().trim();
        Product product = findProductByName(searchedProductName);
        if (product == null) {
            showToast("یک محصول معتبر از نتایج جستجو انتخاب کنید.");
            return;
        }

        String year = String.valueOf(yearSpinner.getSelectedItem());
        String month = String.valueOf(monthSpinner.getSelectedItem());
        double quantity = Utils.parseNumber(quantityEditText.getText().toString());
        double actualLeather = Utils.parseNumber(actualLeatherEditText.getText().toString());

        if (Double.isNaN(quantity) || quantity <= 0) {
            showToast("تعداد محصول را درست وارد کنید.");
            return;
        }
        if (Double.isNaN(actualLeather) || actualLeather < 0) {
            showToast("چرم مصرفی واقعی را درست وارد کنید.");
            return;
        }

        double expectedLeather = quantity * product.defaultUsage;
        double difference = actualLeather - expectedLeather;
        String status = cuttingStatus(difference);

        showCalculationResult(year, month, product, quantity, actualLeather, expectedLeather, difference, status);
        saveCalculation(year, month, product, quantity, actualLeather, expectedLeather, difference, status);
        showToast("نتیجه در سوابق «" + month + " " + year + "» ذخیره شد.");
    }

    private void showCalculationResult(String year, String month, Product product, double quantity, double actualLeather,
                                       double expectedLeather, double difference, String status) {
        resultCard.setVisibility(View.VISIBLE);
        resultStatusTextView.setText(status);
        resultDifferenceTextView.setText(Utils.formatSignedNumber(difference) + " پا");
        resultProductTextView.setText("سال «" + year + "»  •  ماه «" + month + "»  •  " + product.name);
        resultExpectedTextView.setText(Utils.formatNumber(expectedLeather) + " پا");
        resultActualTextView.setText(Utils.formatNumber(actualLeather) + " پا");
        applyStatusStyle(resultStatusTextView, resultDifferenceTextView, difference);

        String details = "مصرف هر عدد: " + Utils.formatNumber(product.defaultUsage) + " پا"
                + "   •   تعداد تولید: " + Utils.formatNumber(quantity)
                + "\nنتیجه ثبت شد و در گزارش ماهانه قابل پیگیری است.";
        resultTextView.setText(details);
    }

    private void applyStatusStyle(TextView statusView, TextView differenceView, double difference) {
        if (difference < 0) {
            resultCard.setBackgroundResource(R.drawable.bg_result_card_good);
            statusView.setBackgroundResource(R.drawable.bg_badge_good);
            statusView.setTextColor(getColor(R.color.success_dark));
            differenceView.setTextColor(getColor(R.color.success_dark));
        } else if (difference > 0) {
            resultCard.setBackgroundResource(R.drawable.bg_result_card_bad);
            statusView.setBackgroundResource(R.drawable.bg_badge_bad);
            statusView.setTextColor(getColor(R.color.danger_dark));
            differenceView.setTextColor(getColor(R.color.danger_dark));
        } else {
            resultCard.setBackgroundResource(R.drawable.bg_result_card_neutral);
            statusView.setBackgroundResource(R.drawable.bg_badge_neutral);
            statusView.setTextColor(getColor(R.color.neutral_dark));
            differenceView.setTextColor(getColor(R.color.text_primary));
        }
    }

    private String cuttingStatus(double difference) {
        if (difference > 0) {
            return "برش بد";
        } else if (difference < 0) {
            return "برش خوب";
        }
        return "برش بدون اختلاف";
    }

    private Product findProductByName(String name) {
        for (Product product : products) {
            if (product.name.equalsIgnoreCase(name.trim())) {
                return product;
            }
        }
        return null;
    }

    private void saveCalculation(String year, String month, Product product, double quantity, double actualLeather,
                                 double expectedLeather, double difference, String status) {
        JSONArray records = readJsonArray(KEY_CALCULATIONS);
        JSONObject object = new JSONObject();
        try {
            object.put("year", year);
            object.put("month", month);
            object.put("productName", product.name);
            object.put("defaultUsage", product.defaultUsage);
            object.put("quantity", quantity);
            object.put("actualLeather", actualLeather);
            object.put("expectedLeather", expectedLeather);
            object.put("difference", difference);
            object.put("status", status);
            object.put("createdAt", System.currentTimeMillis());
            records.put(object);
            preferences.edit().putString(KEY_CALCULATIONS, records.toString()).apply();
        } catch (JSONException ignored) {
            showToast("ذخیره سابقه انجام نشد.");
        }
    }

    private JSONArray readJsonArray(String key) {
        String json = preferences.getString(key, "[]");
        try {
            return new JSONArray(json);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private void loadProducts() {
        products.clear();
        String savedJson = preferences.getString(KEY_PRODUCTS, null);
        if (savedJson == null) {
            return;
        }
        try {
            JSONArray array = new JSONArray(savedJson);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                String name = object.optString("name", "").trim();
                double defaultUsage = object.optDouble("defaultUsage", 0);
                if (!name.isEmpty() && defaultUsage > 0) {
                    products.add(new Product(name, defaultUsage));
                }
            }
        } catch (JSONException ignored) {
        }
    }

    private List<String> productNames() {
        ArrayList<String> names = new ArrayList<>();
        for (Product product : products) {
            names.add(product.name);
        }
        return names;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
