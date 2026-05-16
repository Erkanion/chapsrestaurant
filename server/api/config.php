<?php
const DB_HOST = 'localhost';
const DB_NAME = 'chapsrestaurant';
const DB_USER = 'root';
const DB_PASSWORD = '';
const DB_CHARSET = 'utf8mb4';

function db_connection(): mysqli
{
    mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

    $connection = new mysqli(DB_HOST, DB_USER, DB_PASSWORD, DB_NAME);
    $connection->set_charset(DB_CHARSET);

    return $connection;
}
