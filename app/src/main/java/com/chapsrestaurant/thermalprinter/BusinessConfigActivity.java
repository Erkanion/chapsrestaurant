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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BusinessConfigActivity extends Activity {
    private static final int COLOR_BACKGROUND = 0xFFFAFAFA;
    private static final int COLOR_SURFACE = 0xFFFFFFFF;
    private static final int COLOR_TEXT = 0xFF212121;
    private static final int COLOR_MUTED = 0xFF6B7280;
    private static final int COLOR_ACCENT = 0xFF2F6F5E;

    private static final String BUSINESS_PREFS = "business_configuration";
    private static final String KEY_BUSINESS_COUNT = "business_count";
    private static final String KEY_ACTIVE_BUSINESS = "active_business";
    private static final String KEY_REMOTE_BUSINESS_IDS = "remote_business_ids";
    private static final String BUSINESS_SEPARATOR = "\u001F";
    private static final String BUSINESS_API_URL = LoginActivity.API_BASE_URL + "businesses.php";

    private final Map<String, EditText> fields = new LinkedHashMap<>();
    private final ExecutorService businessExecutor = Executors.newSingleThreadExecutor();
    private final ArrayList<Integer> remoteBusinessIds = new ArrayList<>();

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
        fetchBusinessesFromDatabase();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        businessExecutor.shutdownNow();
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
        saveBusinessToDatabase();
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
        loadRemoteBusinessIds();
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

    private void fetchBusinessesFromDatabase() {
        int userId = getCurrentUserId();
        if (userId <= 0) {
            return;
        }
        businessExecutor.execute(() -> {
            try {
                ArrayList<JSONObject> businesses = requestBusinesses(userId);
                runOnUiThread(() -> applyRemoteBusinesses(businesses));
            } catch (IOException | JSONException exception) {
                runOnUiThread(() -> Toast.makeText(
                        this,
                        "No se pudieron cargar negocios de la base de datos: " + exception.getMessage(),
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    private ArrayList<JSONObject> requestBusinesses(int userId) throws IOException, JSONException {
        HttpURLConnection connection = (HttpURLConnection) new URL(BUSINESS_API_URL + "?user_id=" + userId).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");

        int responseCode = connection.getResponseCode();
        InputStream stream = responseCode >= 200 && responseCode < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        JSONObject response = new JSONObject(readStream(stream));
        connection.disconnect();

        if (!response.optBoolean("success", false)) {
            throw new IOException(response.optString("message", "Respuesta invalida"));
        }

        ArrayList<JSONObject> businesses = new ArrayList<>();
        JSONArray businessArray = response.optJSONArray("businesses");
        if (businessArray != null) {
            for (int index = 0; index < businessArray.length(); index++) {
                businesses.add(businessArray.getJSONObject(index));
            }
        }
        return businesses;
    }

    private void applyRemoteBusinesses(ArrayList<JSONObject> businesses) {
        if (businesses.isEmpty()) {
            return;
        }

        SharedPreferences.Editor editor = preferences.edit();
        businessCount = businesses.size();
        activeBusinessIndex = Math.min(activeBusinessIndex, businessCount - 1);
        remoteBusinessIds.clear();
        editor.putInt(userScopedKey(KEY_BUSINESS_COUNT), businessCount);
        editor.putInt(userScopedKey(KEY_ACTIVE_BUSINESS), activeBusinessIndex);

        for (int index = 0; index < businesses.size(); index++) {
            JSONObject business = businesses.get(index);
            remoteBusinessIds.add(business.optInt("id", 0));
            for (Map.Entry<String, String> field : businessFieldMap().entrySet()) {
                editor.putString(fieldKey(index, field.getKey()), business.optString(field.getValue(), ""));
            }
        }
        editor.putString(userScopedKey(KEY_REMOTE_BUSINESS_IDS), joinRemoteBusinessIds());
        editor.apply();
        loadBusiness();
        Toast.makeText(this, "Datos del negocio cargados de la base de datos", Toast.LENGTH_SHORT).show();
    }

    private void saveBusinessToDatabase() {
        int userId = getCurrentUserId();
        if (userId <= 0) {
            Toast.makeText(this, "Configuracion guardada localmente", Toast.LENGTH_SHORT).show();
            return;
        }
        JSONObject requestBody = buildBusinessRequestBody(userId);
        businessExecutor.execute(() -> {
            try {
                JSONObject response = postBusiness(requestBody);
                runOnUiThread(() -> handleBusinessSaveResponse(response));
            } catch (IOException | JSONException exception) {
                runOnUiThread(() -> Toast.makeText(
                        this,
                        "Guardado local. No se pudo guardar en la base de datos: " + exception.getMessage(),
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    private JSONObject buildBusinessRequestBody(int userId) {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("user_id", userId);
            requestBody.put("id", getActiveRemoteBusinessId());
            for (Map.Entry<String, String> field : businessFieldMap().entrySet()) {
                EditText editText = fields.get(field.getKey());
                requestBody.put(field.getValue(), editText == null ? "" : editText.getText().toString().trim());
            }
        } catch (JSONException exception) {
            throw new IllegalStateException(exception);
        }
        return requestBody;
    }

    private JSONObject postBusiness(JSONObject requestBody) throws IOException, JSONException {
        HttpURLConnection connection = (HttpURLConnection) new URL(BUSINESS_API_URL).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        byte[] payload = requestBody.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(payload);
        }

        int responseCode = connection.getResponseCode();
        InputStream stream = responseCode >= 200 && responseCode < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        JSONObject response = new JSONObject(readStream(stream));
        connection.disconnect();
        return response;
    }

    private void handleBusinessSaveResponse(JSONObject response) {
        if (!response.optBoolean("success", false)) {
            Toast.makeText(this, response.optString("message", "No se pudo guardar en la base de datos"), Toast.LENGTH_LONG).show();
            return;
        }
        int remoteBusinessId = response.optInt("business_id", getActiveRemoteBusinessId());
        setActiveRemoteBusinessId(remoteBusinessId);
        Toast.makeText(this, "Configuracion guardada en la base de datos", Toast.LENGTH_SHORT).show();
    }

    private String readStream(InputStream stream) throws IOException {
        if (stream == null) {
            return "{}";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private Map<String, String> businessFieldMap() {
        Map<String, String> fieldMap = new LinkedHashMap<>();
        fieldMap.put("Nombre de la Tienda / Negocio", "store_name");
        fieldMap.put("Razon Social", "legal_name");
        fieldMap.put("RFC", "rfc");
        fieldMap.put("Regimen Fiscal", "tax_regime");
        fieldMap.put("Telefono", "phone");
        fieldMap.put("Direccion Completa", "full_address");
        fieldMap.put("Correo Electronico", "email");
        fieldMap.put("Slogan", "slogan");
        fieldMap.put("Logo", "logo");
        return fieldMap;
    }

    private int getCurrentUserId() {
        return getSharedPreferences(LoginActivity.AUTH_PREFS, MODE_PRIVATE)
                .getInt(LoginActivity.KEY_USER_ID, 0);
    }

    private void loadRemoteBusinessIds() {
        remoteBusinessIds.clear();
        String savedIds = preferences.getString(userScopedKey(KEY_REMOTE_BUSINESS_IDS), "");
        if (!savedIds.isEmpty()) {
            String[] ids = savedIds.split(BUSINESS_SEPARATOR, -1);
            for (String id : ids) {
                try {
                    remoteBusinessIds.add(Integer.parseInt(id));
                } catch (NumberFormatException exception) {
                    remoteBusinessIds.add(0);
                }
            }
        }
        while (remoteBusinessIds.size() < businessCount) {
            remoteBusinessIds.add(0);
        }
    }

    private String joinRemoteBusinessIds() {
        StringBuilder builder = new StringBuilder();
        for (int id : remoteBusinessIds) {
            if (builder.length() > 0) {
                builder.append(BUSINESS_SEPARATOR);
            }
            builder.append(id);
        }
        return builder.toString();
    }

    private int getActiveRemoteBusinessId() {
        loadRemoteBusinessIds();
        if (activeBusinessIndex >= 0 && activeBusinessIndex < remoteBusinessIds.size()) {
            return remoteBusinessIds.get(activeBusinessIndex);
        }
        return 0;
    }

    private void setActiveRemoteBusinessId(int remoteBusinessId) {
        loadRemoteBusinessIds();
        while (remoteBusinessIds.size() <= activeBusinessIndex) {
            remoteBusinessIds.add(0);
        }
        remoteBusinessIds.set(activeBusinessIndex, remoteBusinessId);
        preferences.edit()
                .putString(userScopedKey(KEY_REMOTE_BUSINESS_IDS), joinRemoteBusinessIds())
                .apply();
    }

    private String fieldKey(int businessIndex, String fieldName) {
        return userScopedKey("business_" + businessIndex + "_" + fieldName.replace(" ", "_").replace("/", "_"));
    }

    private String fieldKey(String fieldName) {
        return fieldKey(activeBusinessIndex, fieldName);
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
