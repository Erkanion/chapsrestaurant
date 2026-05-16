<?php
const DB_HOST = '127.0.0.1';
const DB_NAME = 'chapsrestaurant';
const DB_USER = 'chaps_user';
const DB_PASSWORD = 'cambia_esta_contrasena';
const DB_CHARSET = 'utf8mb4';

function db_connection(): PDO
{
    $dsn = 'mysql:host=' . DB_HOST . ';dbname=' . DB_NAME . ';charset=' . DB_CHARSET;
    return new PDO($dsn, DB_USER, DB_PASSWORD, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES => false,
    ]);
}
