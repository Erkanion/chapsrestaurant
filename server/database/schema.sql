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
