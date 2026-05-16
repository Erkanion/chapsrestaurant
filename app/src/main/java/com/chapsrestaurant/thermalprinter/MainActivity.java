package com.chapsrestaurant.thermalprinter;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity {
    private static final List<String> MENU_OPTIONS = Arrays.asList(
            "Dashboard",
            "Configuracion de Negocio",
            "Configuracion de Impresora",
            "Menu",
            "Informes y Estadisticas",
            "Corte de Caja",
            "Categorias",
            "Productos",
            "Ingredientes",
            "Nueva Comanda",
            "Mesa y Ordenes",
            "Ordenes",
            "Cocina",
            "Gestion de Meseros",
            "Gestion de Usuarios",
            "Gestios de Mesas"
    );

    private TextView statusText;
    private ListView menuList;
    private boolean menuVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isLoggedIn()) {
            openLoginScreen();
            return;
        }
        buildInterface();
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

    private void buildInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 28, 24, 24);
        root.setBackgroundColor(0xFFF7F2EF);

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, 0, 0, 12);
        root.addView(topBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        Button hamburgerButton = new Button(this);
        hamburgerButton.setText("☰");
        hamburgerButton.setTextSize(24);
        hamburgerButton.setAllCaps(false);
        hamburgerButton.setOnClickListener(view -> toggleMenu());
        topBar.addView(hamburgerButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("POS Restaurante");
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(0xFF3E2723);
        title.setPadding(16, 0, 0, 0);
        topBar.addView(title, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1));

        Button logoutButton = new Button(this);
        logoutButton.setText("⎋");
        logoutButton.setTextSize(20);
        logoutButton.setAllCaps(false);
        logoutButton.setOnClickListener(view -> logout());
        topBar.addView(logoutButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView userText = new TextView(this);
        userText.setText(getCurrentUserName());
        userText.setTextColor(0xFF3E2723);
        userText.setTextSize(15);
        userText.setTypeface(Typeface.DEFAULT_BOLD);
        userText.setPadding(10, 0, 0, 0);
        topBar.addView(userText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView subtitle = new TextView(this);
        subtitle.setText("Toca ☰ para abrir o cerrar el menú principal");
        subtitle.setTextSize(16);
        subtitle.setTextColor(0xFF5D4037);
        subtitle.setPadding(0, 0, 0, 16);
        root.addView(subtitle);

        statusText = new TextView(this);
        statusText.setText("Menu principal listo.");
        statusText.setTextColor(0xFF4E342E);
        statusText.setTextSize(15);
        statusText.setGravity(Gravity.CENTER_HORIZONTAL);
        statusText.setPadding(0, 12, 0, 12);
        root.addView(statusText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        ArrayAdapter<String> menuAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                new ArrayList<>(MENU_OPTIONS));
        menuList = new ListView(this);
        menuList.setAdapter(menuAdapter);
        menuList.setDividerHeight(1);
        menuList.setOnItemClickListener(this::handleMenuSelection);
        menuList.setVisibility(View.GONE);
        root.addView(menuList, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1));

        setContentView(root);
    }

    private String getCurrentUserName() {
        String userName = getSharedPreferences(LoginActivity.AUTH_PREFS, MODE_PRIVATE)
                .getString(LoginActivity.KEY_USER_NAME, "Usuario");
        if (userName == null || userName.trim().isEmpty()) {
            return "Usuario";
        }
        return userName;
    }

    private void toggleMenu() {
        menuVisible = !menuVisible;
        menuList.setVisibility(menuVisible ? View.VISIBLE : View.GONE);
        statusText.setText(menuVisible ? "Menu principal abierto." : "Menu principal cerrado.");
    }

    private void handleMenuSelection(AdapterView<?> parent, View view, int position, long id) {
        String option = MENU_OPTIONS.get(position);
        if ("Configuracion de Impresora".equals(option)) {
            menuVisible = false;
            menuList.setVisibility(View.GONE);
            startActivity(new Intent(this, PrinterConfigActivity.class));
            return;
        }
        menuVisible = false;
        menuList.setVisibility(View.GONE);
        statusText.setText(option + " seleccionado. Módulo pendiente de implementar.");
        Toast.makeText(this, option, Toast.LENGTH_SHORT).show();
    }
}
