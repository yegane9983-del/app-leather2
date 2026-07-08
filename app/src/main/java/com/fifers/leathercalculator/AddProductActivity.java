package com.fifers.leathercalculator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class AddProductActivity extends Activity {

    private static final String PREF_NAME = "leather_calculator_prefs";
    private static final String KEY_PRODUCTS = "products";

    private final ArrayList<Product> products = new ArrayList<>();
    private ProductListAdapter listAdapter;
    private SharedPreferences preferences;

    private EditText productNameEditText;
    private EditText defaultUsageEditText;
    private TextView emptyProductsTextView;
    private TextView catalogCountTextView;
    private ListView productsListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);
        Utils.applyBottomSystemInset(findViewById(R.id.addProductScreenRoot));

        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        bindViews();
        loadProducts();
        listAdapter = new ProductListAdapter();
        productsListView.setAdapter(listAdapter);
        refreshUiState();
    }

    private void bindViews() {
        productNameEditText = findViewById(R.id.productNameEditText);
        defaultUsageEditText = findViewById(R.id.defaultUsageEditText);
        emptyProductsTextView = findViewById(R.id.emptyProductsTextView);
        catalogCountTextView = findViewById(R.id.catalogCountTextView);
        productsListView = findViewById(R.id.productsListView);

        Button backButton = findViewById(R.id.backButton);
        Button addProductButton = findViewById(R.id.addProductButton);

        backButton.setOnClickListener(v -> finish());
        addProductButton.setOnClickListener(v -> addProduct());
    }

    private void addProduct() {
        String name = productNameEditText.getText().toString().trim();
        double defaultUsage = Utils.parseNumber(defaultUsageEditText.getText().toString());

        if (name.isEmpty()) {
            showToast("نام محصول را وارد کنید.");
            return;
        }
        if (findProductByName(name) != null) {
            showToast("محصولی با این نام قبلاً ثبت شده است.");
            return;
        }
        if (Double.isNaN(defaultUsage) || defaultUsage <= 0) {
            showToast("مصرف استاندارد باید عددی بزرگ‌تر از صفر باشد.");
            return;
        }

        products.add(new Product(name, defaultUsage));
        saveProducts();
        refreshUiState();

        productNameEditText.setText("");
        defaultUsageEditText.setText("");
        showToast("محصول با موفقیت ثبت شد.");
    }

    private void showEditUsageDialog(int position) {
        Product product = products.get(position);

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(plainNumber(product.defaultUsage));
        input.setSelection(input.getText().length());
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle("ویرایش مصرف «" + product.name + "»")
                .setMessage("مصرف استاندارد جدید (پا برای هر عدد) را وارد کنید:")
                .setView(input)
                .setPositiveButton("ذخیره", (dialog, which) -> {
                    double newUsage = Utils.parseNumber(input.getText().toString());
                    if (Double.isNaN(newUsage) || newUsage <= 0) {
                        showToast("مصرف استاندارد باید عددی بزرگ‌تر از صفر باشد.");
                        return;
                    }
                    products.set(position, new Product(product.name, newUsage));
                    saveProducts();
                    refreshUiState();
                    showToast("مصرف محصول به‌روزرسانی شد.");
                })
                .setNegativeButton("لغو", null)
                .show();
    }

    private void confirmDeleteProduct(int position) {
        Product product = products.get(position);
        new AlertDialog.Builder(this)
                .setTitle("حذف محصول")
                .setMessage("محصول «" + product.name + "» حذف شود؟")
                .setPositiveButton("حذف", (dialog, which) -> {
                    products.remove(position);
                    saveProducts();
                    refreshUiState();
                    showToast("محصول حذف شد.");
                })
                .setNegativeButton("لغو", null)
                .show();
    }

    private Product findProductByName(String name) {
        for (Product product : products) {
            if (product.name.equalsIgnoreCase(name.trim())) {
                return product;
            }
        }
        return null;
    }

    private void loadProducts() {
        products.clear();
        String savedJson = preferences.getString(KEY_PRODUCTS, null);

        if (savedJson == null) {
            products.add(new Product("دلار", 4));
            saveProducts();
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
        } catch (JSONException e) {
            products.clear();
            products.add(new Product("دلار", 4));
            saveProducts();
        }
    }

    private void saveProducts() {
        JSONArray array = new JSONArray();
        for (Product product : products) {
            JSONObject object = new JSONObject();
            try {
                object.put("name", product.name);
                object.put("defaultUsage", product.defaultUsage);
                array.put(object);
            } catch (JSONException ignored) {
            }
        }
        preferences.edit().putString(KEY_PRODUCTS, array.toString()).apply();
    }

    private void refreshUiState() {
        listAdapter.notifyDataSetChanged();
        catalogCountTextView.setText(Utils.toPersianDigits(String.valueOf(products.size())) + " محصول");
        boolean isEmpty = products.isEmpty();
        emptyProductsTextView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        productsListView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private String plainNumber(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private class ProductListAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return products.size();
        }

        @Override
        public Product getItem(int position) {
            return products.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(AddProductActivity.this).inflate(R.layout.row_product, parent, false);
            }
            Product product = getItem(position);
            TextView nameView = view.findViewById(R.id.productRowNameTextView);
            TextView usageView = view.findViewById(R.id.productRowUsageTextView);
            View editButton = view.findViewById(R.id.productRowEditButton);
            View deleteButton = view.findViewById(R.id.productRowDeleteButton);

            nameView.setText(product.name);
            usageView.setText("مصرف استاندارد: " + Utils.formatNumber(product.defaultUsage) + " پا برای هر عدد");
            editButton.setOnClickListener(v -> showEditUsageDialog(position));
            deleteButton.setOnClickListener(v -> confirmDeleteProduct(position));
            return view;
        }
    }
}
