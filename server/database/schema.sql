CREATE DATABASE IF NOT EXISTS chapsrestaurant
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE chapsrestaurant;

CREATE TABLE IF NOT EXISTS users (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Crea usuarios desde PHP para guardar contraseñas seguras con password_hash().
-- Ejemplo:
-- INSERT INTO users (name, email, username, password_hash)
-- VALUES ('Administrador', 'admin@chapsrestaurant.com', 'admin', '$2y$10$REEMPLAZA_ESTE_HASH');


CREATE TABLE IF NOT EXISTS businesses (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL,
    store_name VARCHAR(160) NOT NULL,
    legal_name VARCHAR(180) NOT NULL,
    rfc VARCHAR(20) NOT NULL,
    tax_regime VARCHAR(120) NOT NULL,
    phone VARCHAR(40) NOT NULL,
    full_address TEXT NOT NULL,
    email VARCHAR(180) NOT NULL,
    slogan VARCHAR(180) NOT NULL,
    logo VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_business_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS printers (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL,
    printer_name VARCHAR(160) NOT NULL,
    mac_address VARCHAR(80) NOT NULL,
    paper_size VARCHAR(40) NOT NULL,
    setup_method ENUM('Bluetooth', 'Ethernet', 'Wifi') NOT NULL DEFAULT 'Bluetooth',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_printer_user_mac (user_id, mac_address),
    CONSTRAINT fk_printer_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
