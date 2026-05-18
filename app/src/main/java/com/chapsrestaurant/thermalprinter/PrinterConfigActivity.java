package com.chapsrestaurant.thermalprinter;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PrinterConfigActivity extends Activity {
    private static final int REQUEST_ENABLE_BLUETOOTH = 100;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 101;
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final Charset PRINTER_CHARSET = Charset.forName("windows-1252");
    private static final String PRINTER_PREFS = "printer_configuration";
    private static final String KEY_CONFIGURED_PRINTERS = "configured_printers";
    private static final String PRINTER_SEPARATOR = "\u001F";
    private static final String PRINTER_API_URL = LoginActivity.API_BASE_URL + "printers.php";
    private static final String METHOD_BLUETOOTH = "Bluetooth";
    private static final String METHOD_ETHERNET = "Ethernet";
    private static final String METHOD_WIFI = "Wifi";
    private static final String DEFAULT_PAPER_SIZE = "80mm";
    private static final int COLOR_BACKGROUND = 0xFFFAFAFA;
    private static final int COLOR_SURFACE = 0xFFFFFFFF;
    private static final int COLOR_TEXT = 0xFF212121;
    private static final int COLOR_MUTED = 0xFF6B7280;
    private static final int COLOR_ACCENT = 0xFF2F6F5E;
    private static final int COLOR_DEVICE_HEADER = 0xFFFF7A00;
    private static final int COLOR_DEVICE_FOOTER = 0xFF000000;

    private final Map<String, BluetoothDevice> devices = new LinkedHashMap<>();
    private final ArrayList<String> deviceLabels = new ArrayList<>();
    private final ArrayList<String> configuredPrinterLabels = new ArrayList<>();
    private final ExecutorService printerExecutor = Executors.newSingleThreadExecutor();

    private ArrayAdapter<String> listAdapter;
    private ArrayAdapter<String> configuredPrinterAdapter;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice selectedDevice;
    private Button scanButton;
    private Button printButton;
    private ProgressBar progressBar;
    private TextView statusText;
    private TextView emptyConfiguredPrintersText;
    private Button wifiEthernetButton;
    private ImageView emptyPrinterIcon;
    private boolean discoveryReceiverRegistered;

    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                addDevice(device);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                setBusy(true, "Buscando impresoras Bluetooth cercanas...");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setBusy(false, "Búsqueda finalizada. Selecciona una impresora para imprimir una prueba.");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isLoggedIn()) {
            openLoginScreen();
            return;
        }

        configureSystemBars();
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager == null ? null : bluetoothManager.getAdapter();
        buildInterface();
        registerDiscoveryReceiver();
        fetchConfiguredPrintersFromDatabase();

        if (bluetoothAdapter == null) {
            scanButton.setEnabled(false);
            printButton.setEnabled(false);
            statusText.setText("Este dispositivo no tiene Bluetooth disponible.");
            return;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothAdapter != null && hasBluetoothPermission() && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        if (discoveryReceiverRegistered) {
            unregisterReceiver(discoveryReceiver);
            discoveryReceiverRegistered = false;
        }
        printerExecutor.shutdownNow();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (hasBluetoothPermission()) {
                loadPairedDevices();
                statusText.setText("Permisos concedidos. Puedes buscar impresoras Bluetooth.");
            } else {
                statusText.setText("Se necesitan permisos Bluetooth para buscar impresoras e imprimir.");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                startDiscovery();
            } else {
                statusText.setText("Bluetooth debe estar activo para buscar impresoras.");
            }
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

    private void configureSystemBars() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(COLOR_DEVICE_HEADER);
            window.setNavigationBarColor(COLOR_DEVICE_FOOTER);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getDecorView().setSystemUiVisibility(0);
        }
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

    private void openMainMenu() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
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
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 18 + getStatusBarHeight(), 20, 24 + getNavigationBarHeight());
        root.setBackgroundColor(COLOR_BACKGROUND);

        addTopBar(root);

        TextView title = new TextView(this);
        title.setText("Configuracion de Impresora");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(COLOR_TEXT);
        root.addView(title);

        TextView description = new TextView(this);
        description.setText("Impresoras configuradas para este negocio.");
        description.setTextSize(16);
        description.setTextColor(COLOR_MUTED);
        description.setPadding(0, 10, 0, 12);
        root.addView(description);

        emptyConfiguredPrintersText = new TextView(this);
        emptyConfiguredPrintersText.setText("Aun no Hay Impresoras\nAgrega una primera impresora a su negocio");
        emptyConfiguredPrintersText.setTextSize(16);
        emptyConfiguredPrintersText.setTextColor(COLOR_MUTED);
        emptyConfiguredPrintersText.setGravity(Gravity.CENTER_HORIZONTAL);
        emptyConfiguredPrintersText.setPadding(0, 18, 0, 18);
        root.addView(emptyConfiguredPrintersText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        emptyPrinterIcon = new ImageView(this);
        emptyPrinterIcon.setImageResource(R.drawable.ic_printer);
        emptyPrinterIcon.setContentDescription("Icono de impresora");
        emptyPrinterIcon.setAdjustViewBounds(true);
        LinearLayout.LayoutParams emptyIconParams = new LinearLayout.LayoutParams(96, 96);
        emptyIconParams.gravity = Gravity.CENTER_HORIZONTAL;
        emptyIconParams.setMargins(0, 0, 0, 10);
        root.addView(emptyPrinterIcon, emptyIconParams);

        configuredPrinterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, configuredPrinterLabels);
        ListView configuredPrinterList = new ListView(this);
        configuredPrinterList.setAdapter(configuredPrinterAdapter);
        configuredPrinterList.setDividerHeight(1);
        root.addView(configuredPrinterList, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView addPrinterButton = new TextView(this);
        addPrinterButton.setText("+");
        addPrinterButton.setTextSize(34);
        addPrinterButton.setTypeface(Typeface.DEFAULT_BOLD);
        addPrinterButton.setTextColor(0xFFFFFFFF);
        addPrinterButton.setGravity(Gravity.CENTER);
        addPrinterButton.setBackgroundColor(COLOR_ACCENT);
        addPrinterButton.setOnClickListener(view -> showAddPrinterDialog());
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(72, 72);
        addParams.gravity = Gravity.CENTER_HORIZONTAL;
        addParams.setMargins(0, 12, 0, 12);
        root.addView(addPrinterButton, addParams);

        scanButton = new Button(this);
        scanButton.setText("Buscar por Bluetooth");
        scanButton.setAllCaps(false);
        scanButton.setVisibility(View.GONE);
        scanButton.setOnClickListener(view -> startDiscovery());
        root.addView(scanButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        wifiEthernetButton = new Button(this);
        wifiEthernetButton.setText("Buscar por Wifi Ethernet");
        wifiEthernetButton.setAllCaps(false);
        wifiEthernetButton.setVisibility(View.GONE);
        wifiEthernetButton.setOnClickListener(view -> showNetworkPrinterDialog());
        root.addView(wifiEthernetButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        printButton = new Button(this);
        printButton.setText("Imprimir prueba térmica");
        printButton.setAllCaps(false);
        printButton.setEnabled(false);
        printButton.setOnClickListener(view -> printTestTicket());
        root.addView(printButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));


        progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        progressParams.gravity = Gravity.CENTER_HORIZONTAL;
        progressParams.setMargins(0, 16, 0, 16);
        root.addView(progressBar, progressParams);

        statusText = new TextView(this);
        statusText.setText("Presiona + para agregar una impresora al negocio.");
        statusText.setTextColor(COLOR_TEXT);
        statusText.setTextSize(15);
        statusText.setPadding(0, 8, 0, 8);
        root.addView(statusText);

        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, deviceLabels);
        ListView listView = new ListView(this);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(this::selectPrinter);
        root.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1));

        setContentView(root);
        loadConfiguredPrinters();
    }

    private void showAddPrinterDialog() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(36, 18, 36, 0);

        TextView title = new TextView(this);
        title.setText("Agregar una impresa");
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(COLOR_TEXT);
        content.addView(title);

        String[] points = new String[]{
                "1. Selecciona el tipo de conexion de la impresora.",
                "2. Busca la impresora por Bluetooth o por Wifi Ethernet.",
                "3. Guarda la impresora y envia una prueba de impresion."
        };
        int[] colors = new int[]{0xFF2F6F5E, 0xFF6B7280, 0xFF212121};
        for (int index = 0; index < points.length; index++) {
            TextView pointView = new TextView(this);
            pointView.setText(points[index]);
            pointView.setTextSize(15);
            pointView.setTextColor(colors[index]);
            pointView.setPadding(0, 12, 0, 0);
            content.addView(pointView);
        }

        TextView helpText = new TextView(this);
        helpText.setText("Color puntos relevantes de la funcion para impresion");
        helpText.setTextSize(14);
        helpText.setTextColor(0xFF2F6F5E);
        helpText.setPadding(0, 14, 0, 0);
        content.addView(helpText);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(content)
                .setPositiveButton("Continuar", null)
                .create();
        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            dialog.dismiss();
            showPrinterSearchOptions();
        }));
        dialog.show();
    }

    private void showPrinterSearchOptions() {
        scanButton.setVisibility(View.VISIBLE);
        wifiEthernetButton.setVisibility(View.VISIBLE);
        statusText.setText("Elige si deseas buscar la impresora por Bluetooth o por Wifi Ethernet.");
    }

    private void loadConfiguredPrinters() {
        configuredPrinterLabels.clear();
        String savedPrinters = getPrinterPreferences().getString(userScopedKey(KEY_CONFIGURED_PRINTERS), "");
        if (!savedPrinters.isEmpty()) {
            String[] printers = savedPrinters.split(PRINTER_SEPARATOR, -1);
            for (String printer : printers) {
                if (!printer.trim().isEmpty()) {
                    configuredPrinterLabels.add(printer);
                }
            }
        }
        configuredPrinterAdapter.notifyDataSetChanged();
        updateConfiguredPrintersEmptyState();
    }

    private void saveConfiguredPrinter(String printerName, String macAddress, String paperSize, String setupMethod) {
        String printerLabel = buildPrinterLabel(printerName, macAddress, paperSize, setupMethod);
        int existingIndex = findConfiguredPrinterIndex(macAddress);
        if (existingIndex >= 0) {
            configuredPrinterLabels.set(existingIndex, printerLabel);
        } else {
            configuredPrinterLabels.add(printerLabel);
        }
        getPrinterPreferences().edit()
                .putString(userScopedKey(KEY_CONFIGURED_PRINTERS), joinConfiguredPrinters())
                .apply();
        configuredPrinterAdapter.notifyDataSetChanged();
        updateConfiguredPrintersEmptyState();
        saveConfiguredPrinterToDatabase(printerName, macAddress, paperSize, setupMethod);
    }

    private String buildPrinterLabel(String printerName, String macAddress, String paperSize, String setupMethod) {
        return printerName + "\n" + macAddress + "\nMetodo: " + setupMethod + " | Papel: " + paperSize;
    }

    private int findConfiguredPrinterIndex(String macAddress) {
        for (int index = 0; index < configuredPrinterLabels.size(); index++) {
            if (macAddress.equals(getPrinterMacAddress(configuredPrinterLabels.get(index)))) {
                return index;
            }
        }
        return -1;
    }

    private String getPrinterMacAddress(String printerLabel) {
        String[] lines = printerLabel.split("\n", -1);
        return lines.length > 1 ? lines[1].trim() : "";
    }

    private void fetchConfiguredPrintersFromDatabase() {
        int userId = getCurrentUserId();
        if (userId <= 0) {
            return;
        }
        printerExecutor.execute(() -> {
            try {
                ArrayList<JSONObject> printers = requestConfiguredPrinters(userId);
                runOnUiThread(() -> applyRemoteConfiguredPrinters(printers));
            } catch (IOException | JSONException exception) {
                runOnUiThread(() -> statusText.setText(
                        "No se pudieron cargar impresoras de la base de datos: " + exception.getMessage()));
            }
        });
    }

    private ArrayList<JSONObject> requestConfiguredPrinters(int userId) throws IOException, JSONException {
        HttpURLConnection connection = (HttpURLConnection) new URL(PRINTER_API_URL + "?user_id=" + userId).openConnection();
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

        ArrayList<JSONObject> printers = new ArrayList<>();
        JSONArray printerArray = response.optJSONArray("printers");
        if (printerArray != null) {
            for (int index = 0; index < printerArray.length(); index++) {
                printers.add(printerArray.getJSONObject(index));
            }
        }
        return printers;
    }

    private void applyRemoteConfiguredPrinters(ArrayList<JSONObject> printers) {
        if (printers.isEmpty()) {
            return;
        }
        configuredPrinterLabels.clear();
        for (JSONObject printer : printers) {
            configuredPrinterLabels.add(buildPrinterLabel(
                    printer.optString("printer_name", "Impresora sin nombre"),
                    printer.optString("mac_address", ""),
                    printer.optString("paper_size", DEFAULT_PAPER_SIZE),
                    printer.optString("setup_method", METHOD_BLUETOOTH)));
        }
        getPrinterPreferences().edit()
                .putString(userScopedKey(KEY_CONFIGURED_PRINTERS), joinConfiguredPrinters())
                .apply();
        configuredPrinterAdapter.notifyDataSetChanged();
        updateConfiguredPrintersEmptyState();
        statusText.setText("Impresoras cargadas de la base de datos.");
    }

    private void saveConfiguredPrinterToDatabase(String printerName, String macAddress, String paperSize, String setupMethod) {
        int userId = getCurrentUserId();
        if (userId <= 0) {
            statusText.setText("Impresora guardada localmente. Inicia sesion nuevamente para sincronizar con la base de datos.");
            return;
        }
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("user_id", userId);
            requestBody.put("printer_name", printerName);
            requestBody.put("mac_address", macAddress);
            requestBody.put("paper_size", paperSize);
            requestBody.put("setup_method", setupMethod);
        } catch (JSONException exception) {
            throw new IllegalStateException(exception);
        }
        printerExecutor.execute(() -> {
            try {
                JSONObject response = postConfiguredPrinter(requestBody);
                runOnUiThread(() -> handlePrinterSaveResponse(response));
            } catch (IOException | JSONException exception) {
                runOnUiThread(() -> statusText.setText(
                        "Impresora guardada localmente. No se pudo guardar en la base de datos: " + exception.getMessage()));
            }
        });
    }

    private JSONObject postConfiguredPrinter(JSONObject requestBody) throws IOException, JSONException {
        HttpURLConnection connection = (HttpURLConnection) new URL(PRINTER_API_URL).openConnection();
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

    private void handlePrinterSaveResponse(JSONObject response) {
        if (response.optBoolean("success", false)) {
            statusText.setText("Impresora guardada en la base de datos.");
            return;
        }
        statusText.setText(response.optString("message", "No se pudo guardar la impresora en la base de datos."));
    }

    private int getCurrentUserId() {
        return getSharedPreferences(LoginActivity.AUTH_PREFS, MODE_PRIVATE)
                .getInt(LoginActivity.KEY_USER_ID, 0);
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

    private String joinConfiguredPrinters() {
        StringBuilder builder = new StringBuilder();
        for (String printer : configuredPrinterLabels) {
            if (builder.length() > 0) {
                builder.append(PRINTER_SEPARATOR);
            }
            builder.append(printer);
        }
        return builder.toString();
    }

    private void updateConfiguredPrintersEmptyState() {
        int emptyStateVisibility = configuredPrinterLabels.isEmpty() ? View.VISIBLE : View.GONE;
        emptyConfiguredPrintersText.setVisibility(emptyStateVisibility);
        emptyPrinterIcon.setVisibility(emptyStateVisibility);
    }

    private int getNavigationBarHeight() {
        int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    private int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    private SharedPreferences getPrinterPreferences() {
        return getSharedPreferences(PRINTER_PREFS, MODE_PRIVATE);
    }

    private String userScopedKey(String key) {
        String userName = getSharedPreferences(LoginActivity.AUTH_PREFS, MODE_PRIVATE)
                .getString(LoginActivity.KEY_USER_NAME, "Administrador");
        if (userName == null || userName.trim().isEmpty()) {
            userName = "Administrador";
        }
        return userName + "_" + key;
    }

    private void registerDiscoveryReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(discoveryReceiver, filter);
        discoveryReceiverRegistered = true;
    }

    private void requestBluetoothPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ArrayList<String> missingPermissions = new ArrayList<>();
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (!missingPermissions.isEmpty()) {
                requestPermissions(missingPermissions.toArray(new String[0]), REQUEST_BLUETOOTH_PERMISSIONS);
            }
        } else if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_BLUETOOTH_PERMISSIONS);
        }
    }

    private boolean hasBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void startDiscovery() {
        if (!hasBluetoothPermission()) {
            requestBluetoothPermissionsIfNeeded();
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BLUETOOTH);
            return;
        }
        loadPairedDevices();
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        boolean started = bluetoothAdapter.startDiscovery();
        if (!started) {
            statusText.setText("No se pudo iniciar la búsqueda. Verifica que Bluetooth esté activo.");
        }
    }

    private void loadPairedDevices() {
        if (bluetoothAdapter == null || !hasBluetoothPermission()) {
            return;
        }
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : bondedDevices) {
            addDevice(device);
        }
        if (deviceLabels.isEmpty()) {
            statusText.setText("No hay impresoras emparejadas. Presiona Buscar para detectar dispositivos cercanos.");
        }
    }

    private void addDevice(BluetoothDevice device) {
        if (device == null || !hasBluetoothPermission()) {
            return;
        }
        String address = device.getAddress();
        if (address == null || devices.containsKey(address)) {
            return;
        }
        devices.put(address, device);
        String name = device.getName() == null ? "Dispositivo sin nombre" : device.getName();
        deviceLabels.add(name + "\n" + address);
        listAdapter.notifyDataSetChanged();
    }

    private void showPaperSizeDialog(String printerName, String macAddress, String setupMethod) {
        String[] paperSizes = new String[]{"58mm", "80mm"};
        new AlertDialog.Builder(this)
                .setTitle("Tamaño de papel")
                .setItems(paperSizes, (dialogInterface, which) -> saveConfiguredPrinter(
                        printerName,
                        macAddress,
                        paperSizes[which],
                        setupMethod))
                .show();
    }

    private void showNetworkPrinterDialog() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(36, 18, 36, 0);

        EditText printerNameInput = new EditText(this);
        printerNameInput.setHint("Nombre de la impresora");
        content.addView(printerNameInput);

        EditText macAddressInput = new EditText(this);
        macAddressInput.setHint("MAC address o direccion IP");
        content.addView(macAddressInput);

        EditText paperSizeInput = new EditText(this);
        paperSizeInput.setHint("Tamaño de papel (58mm o 80mm)");
        paperSizeInput.setText(DEFAULT_PAPER_SIZE);
        content.addView(paperSizeInput);

        String[] methods = new String[]{METHOD_WIFI, METHOD_ETHERNET};
        final String[] selectedMethod = new String[]{METHOD_WIFI};

        TextView methodText = new TextView(this);
        methodText.setText("Metodo de alta: " + selectedMethod[0]);
        methodText.setTextColor(COLOR_TEXT);
        methodText.setTextSize(15);
        methodText.setPadding(0, 12, 0, 0);
        methodText.setOnClickListener(view -> new AlertDialog.Builder(this)
                .setTitle("Metodo de alta")
                .setItems(methods, (dialogInterface, which) -> {
                    selectedMethod[0] = methods[which];
                    methodText.setText("Metodo de alta: " + selectedMethod[0]);
                })
                .show());
        content.addView(methodText);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Agregar impresora Wifi/Ethernet")
                .setView(content)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String printerName = printerNameInput.getText().toString().trim();
            String macAddress = macAddressInput.getText().toString().trim();
            String paperSize = paperSizeInput.getText().toString().trim();
            if (printerName.isEmpty()) {
                printerNameInput.setError("Ingresa el nombre de la impresora");
                return;
            }
            if (macAddress.isEmpty()) {
                macAddressInput.setError("Ingresa la MAC address o direccion IP");
                return;
            }
            String normalizedPaperSize = paperSize.isEmpty() ? DEFAULT_PAPER_SIZE : paperSize;
            saveConfiguredPrinter(printerName, macAddress, normalizedPaperSize, selectedMethod[0]);
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void selectPrinter(AdapterView<?> parent, View view, int position, long id) {
        String label = deviceLabels.get(position);
        String address = label.substring(label.lastIndexOf('\n') + 1);
        selectedDevice = devices.get(address);
        printButton.setEnabled(selectedDevice != null);
        if (selectedDevice != null) {
            String printerName = label.substring(0, label.lastIndexOf('\n')).trim();
            showPaperSizeDialog(printerName, address, METHOD_BLUETOOTH);
        }
        statusText.setText("Impresora seleccionada: " + label.replace('\n', ' '));
    }

    private void printTestTicket() {
        if (selectedDevice == null) {
            toast("Selecciona una impresora primero.");
            return;
        }
        if (!hasBluetoothPermission()) {
            requestBluetoothPermissionsIfNeeded();
            return;
        }
        setBusy(true, "Conectando con la impresora seleccionada...");
        printerExecutor.execute(() -> {
            try {
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
                sendTestPrint(selectedDevice);
                runOnUiThread(() -> {
                    setBusy(false, "Impresión de prueba enviada correctamente.");
                    toast("Prueba enviada");
                });
            } catch (IOException exception) {
                runOnUiThread(() -> {
                    setBusy(false, "Error al imprimir: " + exception.getMessage());
                    toast("No se pudo imprimir");
                });
            }
        });
    }

    private void sendTestPrint(BluetoothDevice device) throws IOException {
        try (BluetoothSocket socket = device.createRfcommSocketToServiceRecord(SPP_UUID)) {
            socket.connect();
            try (OutputStream outputStream = socket.getOutputStream()) {
                outputStream.write(buildEscPosTestTicket());
                outputStream.flush();
            }
        }
    }

    private byte[] buildEscPosTestTicket() {
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
        String text = "\n"
                + "      CHAPS RESTAURANT\n"
                + "   PRUEBA IMPRESORA TERMICA\n"
                + "--------------------------------\n"
                + "Bluetooth: OK\n"
                + "Fecha: " + date + "\n"
                + "\n"
                + "Si puedes leer este ticket,\n"
                + "la impresora esta conectada.\n"
                + "\n\n\n";
        byte[] initialize = new byte[]{0x1B, 0x40};
        byte[] alignCenter = new byte[]{0x1B, 0x61, 0x01};
        byte[] cutPaper = new byte[]{0x1D, 0x56, 0x42, 0x00};
        byte[] textBytes = text.getBytes(PRINTER_CHARSET);
        byte[] ticket = new byte[initialize.length + alignCenter.length + textBytes.length + cutPaper.length];
        int index = 0;
        System.arraycopy(initialize, 0, ticket, index, initialize.length);
        index += initialize.length;
        System.arraycopy(alignCenter, 0, ticket, index, alignCenter.length);
        index += alignCenter.length;
        System.arraycopy(textBytes, 0, ticket, index, textBytes.length);
        index += textBytes.length;
        System.arraycopy(cutPaper, 0, ticket, index, cutPaper.length);
        return ticket;
    }

    private void setBusy(boolean busy, String message) {
        progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
        scanButton.setEnabled(!busy);
        printButton.setEnabled(!busy && selectedDevice != null);
        statusText.setText(message);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
