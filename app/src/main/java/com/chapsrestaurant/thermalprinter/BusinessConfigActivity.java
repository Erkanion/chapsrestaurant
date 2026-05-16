package com.chapsrestaurant.thermalprinter;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedHashMap;
import java.util.Map;

public class BusinessConfigActivity extends Activity {
    private static final int COLOR_BACKGROUND = 0xFFFAFAFA;
    private static final int COLOR_SURFACE = 0xFFFFFFFF;
    private static final int COLOR_TEXT = 0xFF212121;
    private static final int COLOR_MUTED = 0xFF6B7280;
    private static final int COLOR_ACCENT = 0xFF2F6F5E;

    private static final String BUSINESS_PREFS = "business_configuration";
    private static final String KEY_BUSINESS_COUNT = "business_count";
    private static final String KEY_ACTIVE_BUSINESS = "active_business";

    private final Map<String, EditText> fields = new LinkedHashMap<>();

    private SharedPreferences preferences;
    private TextView businessCounterText;
    private int activeBusinessIndex;
    private int businessCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isLoggedIn()) {
            openLoginScreen();
            return;
        }
        configureSystemBars();
        preferences = getSharedPreferences(BUSINESS_PREFS, MODE_PRIVATE);
        businessCount = Math.max(1, preferences.getInt(userScopedKey(KEY_BUSINESS_COUNT), 1));
        activeBusinessIndex = preferences.getInt(userScopedKey(KEY_ACTIVE_BUSINESS), 0);
        if (activeBusinessIndex >= businessCount) {
            activeBusinessIndex = 0;
        }
        buildInterface();
        loadBusiness();
    }

    private void configureSystemBars() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(COLOR_BACKGROUND);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    private boolean isLoggedIn() {
        return getSharedPreferences(LoginActivity.AUTH_PREFS, MODE_PRIVATE)
                .getBoolean(LoginActivity.KEY_IS_LOGGED_IN, false);
    }

    private void logout() {
        getSharedPreferences(LoginActivity.AUTH_PREFS, MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
        openLoginScreen();
    }

    private void openLoginScreen() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void openMainMenu() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void addTopBar(LinearLayout root) {
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, 8, 0, 8);
        topBar.setBackgroundColor(COLOR_SURFACE);
        root.addView(topBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        Button menuButton = new Button(this);
        menuButton.setText("☰");
        menuButton.setTextSize(22);
        menuButton.setTextColor(COLOR_ACCENT);
        menuButton.setBackgroundColor(COLOR_SURFACE);
        menuButton.setAllCaps(false);
        menuButton.setOnClickListener(view -> openMainMenu());
        topBar.addView(menuButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        View spacer = new View(this);
        topBar.addView(spacer, new LinearLayout.LayoutParams(
                0,
                1,
                1));

        TextView userText = new TextView(this);
        userText.setText(getCurrentUserName());
        userText.setTextColor(COLOR_MUTED);
        userText.setTextSize(15);
        userText.setTypeface(Typeface.DEFAULT_BOLD);
        userText.setPadding(0, 0, 12, 0);
        topBar.addView(userText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        Button logoutButton = new Button(this);
        logoutButton.setText("⎋");
        logoutButton.setTextSize(18);
        logoutButton.setTextColor(COLOR_MUTED);
        logoutButton.setBackgroundColor(COLOR_SURFACE);
        logoutButton.setAllCaps(false);
        logoutButton.setOnClickListener(view -> logout());
        topBar.addView(logoutButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    private String getCurrentUserName() {
        String userName = getSharedPreferences(LoginActivity.AUTH_PREFS, MODE_PRIVATE)
                .getString(LoginActivity.KEY_USER_NAME, "Administrador");
        if (userName == null || userName.trim().isEmpty()) {
            return "Administrador";
        }
        return userName;
    }

    private void buildInterface() {
        LinearLayout screenRoot = new LinearLayout(this);
        screenRoot.setOrientation(LinearLayout.VERTICAL);
        screenRoot.setPadding(20, 18 + getStatusBarHeight(), 20, 0);
        screenRoot.setBackgroundColor(COLOR_BACKGROUND);

        addTopBar(screenRoot);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(COLOR_BACKGROUND);
        screenRoot.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(4, 0, 4, 24);
        root.setBackgroundColor(COLOR_BACKGROUND);
        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("Configuracion de Negocio");
        title.setTextColor(COLOR_TEXT);
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title, fullWidthParams());

        TextView description = new TextView(this);
        description.setText("Cada usuario puede administrar uno o varios negocios.");
        description.setTextColor(COLOR_MUTED);
        description.setTextSize(15);
        description.setPadding(0, 4, 0, 12);
        root.addView(description, fullWidthParams());

        businessCounterText = new TextView(this);
        businessCounterText.setTextColor(COLOR_ACCENT);
        businessCounterText.setTextSize(16);
        businessCounterText.setTypeface(Typeface.DEFAULT_BOLD);
        businessCounterText.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(businessCounterText, fullWidthParams());

        LinearLayout navigationRow = new LinearLayout(this);
        navigationRow.setOrientation(LinearLayout.HORIZONTAL);
        navigationRow.setGravity(Gravity.CENTER);
        root.addView(navigationRow, fullWidthParams());

        Button previousButton = new Button(this);
        previousButton.setText("Anterior");
        previousButton.setAllCaps(false);
        previousButton.setOnClickListener(view -> moveBusiness(-1));
        navigationRow.addView(previousButton, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button nextButton = new Button(this);
        nextButton.setText("Siguiente");
        nextButton.setAllCaps(false);
        nextButton.setOnClickListener(view -> moveBusiness(1));
        navigationRow.addView(nextButton, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        addField(root, "Nombre de la Tienda / Negocio", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        addField(root, "Razon Social", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        addField(root, "RFC", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        addField(root, "Regimen Fiscal", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        addField(root, "Telefono", InputType.TYPE_CLASS_PHONE);
        addField(root, "Direccion Completa", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        addField(root, "Correo Electronico", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        addField(root, "Slogan", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        addField(root, "Logo", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);

        Button saveButton = new Button(this);
        saveButton.setText("Guardar configuracion");
        saveButton.setAllCaps(false);
        saveButton.setTextColor(COLOR_SURFACE);
        saveButton.setBackgroundColor(COLOR_ACCENT);
        saveButton.setOnClickListener(view -> saveBusiness());
        root.addView(saveButton, fullWidthParams());

        Button newBusinessButton = new Button(this);
        newBusinessButton.setText("Agregar otro negocio");
        newBusinessButton.setAllCaps(false);
        newBusinessButton.setOnClickListener(view -> addNewBusiness());
        root.addView(newBusinessButton, fullWidthParams());

        Button backButton = new Button(this);
        backButton.setText("Volver al menu");
        backButton.setAllCaps(false);
        backButton.setOnClickListener(view -> finish());
        root.addView(backButton, fullWidthParams());

        setContentView(screenRoot);
    }

    private void addField(LinearLayout root, String label, int inputType) {
        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(COLOR_TEXT);
        labelView.setTextSize(14);
        labelView.setTypeface(Typeface.DEFAULT_BOLD);
        labelView.setPadding(0, 12, 0, 4);
        root.addView(labelView, fullWidthParams());

        EditText editText = new EditText(this);
        editText.setSingleLine((inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0);
        editText.setInputType(inputType);
        editText.setHint(label);
        editText.setTextColor(COLOR_TEXT);
        editText.setHintTextColor(COLOR_MUTED);
        editText.setBackgroundColor(COLOR_SURFACE);
        editText.setPadding(14, 10, 14, 10);
        root.addView(editText, fullWidthParams());
        fields.put(label, editText);
    }

    private LinearLayout.LayoutParams fullWidthParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 6, 0, 6);
        return params;
    }

    private int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    private void moveBusiness(int direction) {
        saveBusinessSilently();
        activeBusinessIndex = (activeBusinessIndex + direction + businessCount) % businessCount;
        preferences.edit().putInt(userScopedKey(KEY_ACTIVE_BUSINESS), activeBusinessIndex).apply();
        loadBusiness();
    }

    private void addNewBusiness() {
        saveBusinessSilently();
        businessCount += 1;
        activeBusinessIndex = businessCount - 1;
        preferences.edit()
                .putInt(userScopedKey(KEY_BUSINESS_COUNT), businessCount)
                .putInt(userScopedKey(KEY_ACTIVE_BUSINESS), activeBusinessIndex)
                .apply();
        clearFields();
        updateCounter();
        Toast.makeText(this, "Nuevo negocio agregado", Toast.LENGTH_SHORT).show();
    }

    private void saveBusiness() {
        saveBusinessSilently();
        Toast.makeText(this, "Configuracion guardada", Toast.LENGTH_SHORT).show();
    }

    private void saveBusinessSilently() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(userScopedKey(KEY_BUSINESS_COUNT), businessCount);
        editor.putInt(userScopedKey(KEY_ACTIVE_BUSINESS), activeBusinessIndex);
        for (Map.Entry<String, EditText> entry : fields.entrySet()) {
            editor.putString(fieldKey(entry.getKey()), entry.getValue().getText().toString().trim());
        }
        editor.apply();
    }

    private void loadBusiness() {
        for (Map.Entry<String, EditText> entry : fields.entrySet()) {
            entry.getValue().setText(preferences.getString(fieldKey(entry.getKey()), ""));
        }
        updateCounter();
    }

    private void clearFields() {
        for (EditText field : fields.values()) {
            field.setText("");
        }
    }

    private void updateCounter() {
        businessCounterText.setText("Negocio " + (activeBusinessIndex + 1) + " de " + businessCount);
    }

    private String fieldKey(String fieldName) {
        return userScopedKey("business_" + activeBusinessIndex + "_" + fieldName.replace(" ", "_").replace("/", "_"));
    }

    private String userScopedKey(String key) {
        String userName = getSharedPreferences(LoginActivity.AUTH_PREFS, MODE_PRIVATE)
                .getString(LoginActivity.KEY_USER_NAME, "Administrador");
        if (userName == null || userName.trim().isEmpty()) {
            userName = "Administrador";
        }
        return userName + "_" + key;
    }
}
