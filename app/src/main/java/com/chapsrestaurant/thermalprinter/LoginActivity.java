package com.chapsrestaurant.thermalprinter;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends Activity {
    static final String AUTH_PREFS = "auth_preferences";
    static final String KEY_IS_LOGGED_IN = "is_logged_in";
    static final String KEY_USER_NAME = "user_name";

    private static final String LOGIN_URL = "https://tu-dominio.com/api/login.php";
    private static final int CONNECTION_TIMEOUT_MS = 15000;

    private final ExecutorService loginExecutor = Executors.newSingleThreadExecutor();

    private EditText userInput;
    private EditText passwordInput;
    private Button loginButton;
    private ProgressBar progressBar;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isLoggedIn()) {
            openPrinterScreen();
            return;
        }
        buildInterface();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        loginExecutor.shutdownNow();
    }

    private void buildInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setPadding(40, 40, 40, 40);
        root.setBackgroundColor(0xFFF7F2EF);

        TextView title = new TextView(this);
        title.setText("Chaps Restaurant");
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(0xFF3E2723);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title, fullWidthParams());

        TextView subtitle = new TextView(this);
        subtitle.setText("Inicia sesión para usar la impresora térmica");
        subtitle.setTextSize(16);
        subtitle.setTextColor(0xFF5D4037);
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        subtitle.setPadding(0, 8, 0, 28);
        root.addView(subtitle, fullWidthParams());

        userInput = new EditText(this);
        userInput.setHint("Usuario o correo");
        userInput.setSingleLine(true);
        userInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        root.addView(userInput, fullWidthParams());

        passwordInput = new EditText(this);
        passwordInput.setHint("Contraseña");
        passwordInput.setSingleLine(true);
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        root.addView(passwordInput, fullWidthParams());

        loginButton = new Button(this);
        loginButton.setText("Entrar");
        loginButton.setAllCaps(false);
        loginButton.setOnClickListener(view -> attemptLogin());
        root.addView(loginButton, fullWidthParams());

        progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        progressParams.gravity = Gravity.CENTER_HORIZONTAL;
        progressParams.setMargins(0, 18, 0, 18);
        root.addView(progressBar, progressParams);

        statusText = new TextView(this);
        statusText.setText("Configura LOGIN_URL en LoginActivity.java con tu API MySQL.");
        statusText.setTextColor(0xFF6D4C41);
        statusText.setTextSize(14);
        statusText.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(statusText, fullWidthParams());

        setContentView(root);
    }

    private LinearLayout.LayoutParams fullWidthParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 8, 0, 8);
        return params;
    }

    private void attemptLogin() {
        String user = userInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        if (user.isEmpty()) {
            userInput.setError("Ingresa tu usuario o correo");
            return;
        }
        if (password.isEmpty()) {
            passwordInput.setError("Ingresa tu contraseña");
            return;
        }

        setBusy(true, "Validando credenciales en MySQL...");
        loginExecutor.execute(() -> {
            try {
                LoginResponse response = requestLogin(user, password);
                runOnUiThread(() -> handleLoginResponse(response));
            } catch (IOException | JSONException exception) {
                runOnUiThread(() -> {
                    setBusy(false, "No se pudo conectar con el servidor: " + exception.getMessage());
                    toast("Error de conexión");
                });
            }
        });
    }

    private LoginResponse requestLogin(String user, String password) throws IOException, JSONException {
        HttpURLConnection connection = (HttpURLConnection) new URL(LOGIN_URL).openConnection();
        connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
        connection.setReadTimeout(CONNECTION_TIMEOUT_MS);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        JSONObject requestBody = new JSONObject();
        requestBody.put("user", user);
        requestBody.put("password", password);
        byte[] payload = requestBody.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(payload);
        }

        int responseCode = connection.getResponseCode();
        InputStream stream = responseCode >= 200 && responseCode < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        String responseText = readStream(stream);
        connection.disconnect();

        JSONObject jsonResponse = new JSONObject(responseText);
        boolean success = jsonResponse.optBoolean("success", false);
        String message = jsonResponse.optString("message", success ? "Acceso concedido" : "Acceso denegado");
        String displayName = jsonResponse.optString("name", user);
        return new LoginResponse(success, message, displayName);
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

    private void handleLoginResponse(LoginResponse response) {
        setBusy(false, response.message);
        if (response.success) {
            getSharedPreferences(AUTH_PREFS, MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_IS_LOGGED_IN, true)
                    .putString(KEY_USER_NAME, response.displayName)
                    .apply();
            toast("Bienvenido " + response.displayName);
            openPrinterScreen();
        } else {
            passwordInput.setText("");
            passwordInput.requestFocus();
        }
    }

    private boolean isLoggedIn() {
        SharedPreferences preferences = getSharedPreferences(AUTH_PREFS, MODE_PRIVATE);
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    private void openPrinterScreen() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void setBusy(boolean busy, String message) {
        progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!busy);
        userInput.setEnabled(!busy);
        passwordInput.setEnabled(!busy);
        statusText.setText(message);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private static class LoginResponse {
        final boolean success;
        final String message;
        final String displayName;

        LoginResponse(boolean success, String message, String displayName) {
            this.success = success;
            this.message = message;
            this.displayName = displayName;
        }
    }
}
