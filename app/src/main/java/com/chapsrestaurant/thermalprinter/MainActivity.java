package com.chapsrestaurant.thermalprinter;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
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
    private static final int COLOR_BLACK = 0xFF101010;
    private static final int COLOR_ORANGE = 0xFFFF7A00;
    private static final int COLOR_RED = 0xFFD62828;
    private static final int COLOR_WOOD = 0xFF8B5A2B;
    private static final int COLOR_DARK_GREEN = 0xFF0B3D2E;
    private static final int COLOR_CREAM = 0xFFFFF3E0;

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

    private LinearLayout menuContainer;
    private TextView welcomeText;
    private boolean menuVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isLoggedIn()) {
            openLoginScreen();
            return;
        }
        configureSystemBars();
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

    private void configureSystemBars() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(COLOR_BLACK);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getDecorView().setSystemUiVisibility(0);
        }
    }

    private void buildInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 18 + getStatusBarHeight(), 20, 20);
        root.setBackgroundColor(COLOR_DARK_GREEN);

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(14, 10, 14, 10);
        topBar.setBackgroundColor(COLOR_BLACK);
        root.addView(topBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        Button hamburgerButton = new Button(this);
        hamburgerButton.setText("☰");
        hamburgerButton.setTextSize(24);
        hamburgerButton.setTextColor(COLOR_CREAM);
        hamburgerButton.setBackgroundColor(COLOR_ORANGE);
        hamburgerButton.setAllCaps(false);
        hamburgerButton.setOnClickListener(view -> toggleMenu());
        topBar.addView(hamburgerButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        View spacer = new View(this);
        topBar.addView(spacer, new LinearLayout.LayoutParams(
                0,
                1,
                1));

        TextView userText = new TextView(this);
        userText.setText(getCurrentUserName());
        userText.setTextColor(COLOR_CREAM);
        userText.setTextSize(15);
        userText.setTypeface(Typeface.DEFAULT_BOLD);
        userText.setPadding(0, 0, 12, 0);
        topBar.addView(userText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        Button logoutButton = new Button(this);
        logoutButton.setText("⎋");
        logoutButton.setTextSize(20);
        logoutButton.setTextColor(COLOR_CREAM);
        logoutButton.setBackgroundColor(COLOR_RED);
        logoutButton.setAllCaps(false);
        logoutButton.setOnClickListener(view -> logout());
        topBar.addView(logoutButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        welcomeText = new TextView(this);
        welcomeText.setText("Bienvenido");
        welcomeText.setTextColor(COLOR_CREAM);
        welcomeText.setTextSize(26);
        welcomeText.setTypeface(Typeface.DEFAULT_BOLD);
        welcomeText.setGravity(Gravity.CENTER_HORIZONTAL);
        welcomeText.setPadding(0, 24, 0, 18);
        root.addView(welcomeText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        menuContainer = new LinearLayout(this);
        menuContainer.setOrientation(LinearLayout.VERTICAL);
        menuContainer.setVisibility(View.GONE);
        menuContainer.setBackgroundColor(COLOR_WOOD);
        menuContainer.setPadding(12, 12, 12, 12);
        root.addView(menuContainer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1));

        TextView menuTitle = new TextView(this);
        menuTitle.setText("POS Restaurante");
        menuTitle.setTextColor(COLOR_CREAM);
        menuTitle.setTextSize(24);
        menuTitle.setTypeface(Typeface.DEFAULT_BOLD);
        menuTitle.setGravity(Gravity.CENTER_HORIZONTAL);
        menuTitle.setPadding(0, 8, 0, 14);
        menuContainer.addView(menuTitle, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        ArrayAdapter<String> menuAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                new ArrayList<>(MENU_OPTIONS)) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView itemView = (TextView) super.getView(position, convertView, parent);
                itemView.setTextColor(COLOR_CREAM);
                itemView.setTextSize(18);
                itemView.setTypeface(Typeface.DEFAULT_BOLD);
                itemView.setBackgroundColor(position % 2 == 0 ? COLOR_BLACK : COLOR_DARK_GREEN);
                itemView.setPadding(20, 18, 20, 18);
                return itemView;
            }
        };
        ListView menuList = new ListView(this);
        menuList.setAdapter(menuAdapter);
        menuList.setDividerHeight(4);
        menuList.setBackgroundColor(COLOR_WOOD);
        menuList.setOnItemClickListener(this::handleMenuSelection);
        menuContainer.addView(menuList, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1));

        setContentView(root);
    }

    private int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    private String getCurrentUserName() {
        String userName = getSharedPreferences(LoginActivity.AUTH_PREFS, MODE_PRIVATE)
                .getString(LoginActivity.KEY_USER_NAME, "Administrador");
        if (userName == null || userName.trim().isEmpty()) {
            return "Administrador";
        }
        return userName;
    }

    private void toggleMenu() {
        menuVisible = !menuVisible;
        menuContainer.setVisibility(menuVisible ? View.VISIBLE : View.GONE);
    }

    private void handleMenuSelection(AdapterView<?> parent, View view, int position, long id) {
        String option = MENU_OPTIONS.get(position);
        menuVisible = false;
        menuContainer.setVisibility(View.GONE);
        if ("Configuracion de Impresora".equals(option)) {
            startActivity(new Intent(this, PrinterConfigActivity.class));
            return;
        }
        welcomeText.setText(option);
        Toast.makeText(this, option, Toast.LENGTH_SHORT).show();
    }
}
