package com.chapsrestaurant.thermalprinter;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
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

public class MainActivity extends Activity {
    private static final int REQUEST_ENABLE_BLUETOOTH = 100;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 101;
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final Charset PRINTER_CHARSET = Charset.forName("windows-1252");

    private final Map<String, BluetoothDevice> devices = new LinkedHashMap<>();
    private final ArrayList<String> deviceLabels = new ArrayList<>();
    private final ExecutorService printerExecutor = Executors.newSingleThreadExecutor();

    private ArrayAdapter<String> listAdapter;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice selectedDevice;
    private Button scanButton;
    private Button printButton;
    private ProgressBar progressBar;
    private TextView statusText;

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
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager == null ? null : bluetoothManager.getAdapter();
        buildInterface();
        registerDiscoveryReceiver();

        if (bluetoothAdapter == null) {
            scanButton.setEnabled(false);
            printButton.setEnabled(false);
            statusText.setText("Este dispositivo no tiene Bluetooth disponible.");
            return;
        }

        requestBluetoothPermissionsIfNeeded();
        loadPairedDevices();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothAdapter != null && hasBluetoothPermission() && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        unregisterReceiver(discoveryReceiver);
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

    private void buildInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 36, 32, 24);
        root.setBackgroundColor(0xFFF7F2EF);

        TextView title = new TextView(this);
        title.setText("Impresora térmica Bluetooth");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(0xFF3E2723);
        root.addView(title);

        TextView description = new TextView(this);
        description.setText("Busca impresoras térmicas Bluetooth, selecciona una y envía una impresión ESC/POS de prueba.");
        description.setTextSize(16);
        description.setTextColor(0xFF5D4037);
        description.setPadding(0, 10, 0, 20);
        root.addView(description);

        scanButton = new Button(this);
        scanButton.setText("Buscar impresoras Bluetooth");
        scanButton.setAllCaps(false);
        scanButton.setOnClickListener(view -> startDiscovery());
        root.addView(scanButton, new LinearLayout.LayoutParams(
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
        statusText.setText("Activa Bluetooth y presiona Buscar impresoras Bluetooth.");
        statusText.setTextColor(0xFF4E342E);
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
    }

    private void registerDiscoveryReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(discoveryReceiver, filter);
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

    private void selectPrinter(AdapterView<?> parent, View view, int position, long id) {
        String label = deviceLabels.get(position);
        String address = label.substring(label.lastIndexOf('\n') + 1);
        selectedDevice = devices.get(address);
        printButton.setEnabled(selectedDevice != null);
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
