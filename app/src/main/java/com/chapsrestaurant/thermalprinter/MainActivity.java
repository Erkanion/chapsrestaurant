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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity {
    private static final int COLOR_BACKGROUND = 0xFFFAFAFA;
    private static final int COLOR_SURFACE = 0xFFFFFFFF;
    private static final int COLOR_TEXT = 0xFF212121;
    private static final int COLOR_MUTED = 0xFF6B7280;
    private static final int COLOR_ACCENT = 0xFF2F6F5E;

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
    private LinearLayout homeContainer;
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
            window.setStatusBarColor(COLOR_BACKGROUND);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    private void buildInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 18 + getStatusBarHeight(), 20, 20);
        root.setBackgroundColor(COLOR_BACKGROUND);

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, 8, 0, 8);
        topBar.setBackgroundColor(COLOR_SURFACE);
        root.addView(topBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        Button hamburgerButton = new Button(this);
        hamburgerButton.setText("☰");
        hamburgerButton.setTextSize(22);
        hamburgerButton.setTextColor(COLOR_ACCENT);
        hamburgerButton.setBackgroundColor(COLOR_SURFACE);
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

        homeContainer = new LinearLayout(this);
        homeContainer.setOrientation(LinearLayout.VERTICAL);
        homeContainer.setGravity(Gravity.CENTER);
        homeContainer.setBackgroundColor(COLOR_BACKGROUND);
        root.addView(homeContainer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1));

        welcomeText = new TextView(this);
        welcomeText.setText("Bienvenido");
        welcomeText.setTextColor(COLOR_TEXT);
        welcomeText.setTextSize(24);
        welcomeText.setTypeface(Typeface.DEFAULT_BOLD);
        welcomeText.setGravity(Gravity.CENTER);
        welcomeText.setPadding(0, 0, 0, dpToPx(18));
        homeContainer.addView(welcomeText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        ImageView logoImage = new ImageView(this);
        logoImage.setImageResource(R.drawable.chapsrestaurant);
        logoImage.setContentDescription("Chaps Restaurant");
        logoImage.setAdjustViewBounds(true);
        logoImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(320));
        logoParams.gravity = Gravity.CENTER;
        homeContainer.addView(logoImage, logoParams);

        menuContainer = new LinearLayout(this);
        menuContainer.setOrientation(LinearLayout.VERTICAL);
        menuContainer.setVisibility(View.GONE);
        menuContainer.setBackgroundColor(COLOR_SURFACE);
        menuContainer.setPadding(0, 12, 0, 0);
        root.addView(menuContainer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1));

        TextView menuTitle = new TextView(this);
        menuTitle.setText("POS Restaurante");
        menuTitle.setTextColor(COLOR_TEXT);
        menuTitle.setTextSize(22);
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
                itemView.setTextColor(COLOR_TEXT);
                itemView.setTextSize(16);
                itemView.setTypeface(Typeface.DEFAULT);
                itemView.setBackgroundColor(COLOR_SURFACE);
                itemView.setPadding(18, 16, 18, 16);
                return itemView;
            }
        };
        ListView menuList = new ListView(this);
        menuList.setAdapter(menuAdapter);
        menuList.setDividerHeight(1);
        menuList.setBackgroundColor(COLOR_SURFACE);
        menuList.setOnItemClickListener(this::handleMenuSelection);
        menuContainer.addView(menuList, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1));

        setContentView(root);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
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
        homeContainer.setVisibility(menuVisible ? View.GONE : View.VISIBLE);
    }

    private void handleMenuSelection(AdapterView<?> parent, View view, int position, long id) {
        String option = MENU_OPTIONS.get(position);
        menuVisible = false;
        menuContainer.setVisibility(View.GONE);
        homeContainer.setVisibility(View.VISIBLE);
        if ("Configuracion de Negocio".equals(option)) {
            startActivity(new Intent(this, BusinessConfigActivity.class));
            return;
        }
        if ("Configuracion de Impresora".equals(option)) {
            startActivity(new Intent(this, PrinterConfigActivity.class));
            return;
        }
        welcomeText.setText(option);
        Toast.makeText(this, option, Toast.LENGTH_SHORT).show();
    }
}
