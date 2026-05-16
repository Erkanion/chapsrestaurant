<?php
require __DIR__ . '/config.php';

header('Content-Type: application/json; charset=utf-8');

try {
    $connection = db_connection();

    if ($_SERVER['REQUEST_METHOD'] === 'GET') {
        $userId = (int) ($_GET['user_id'] ?? 0);
        if ($userId <= 0) {
            http_response_code(400);
            echo json_encode([
                'success' => false,
                'message' => 'Usuario obligatorio',
            ]);
            exit;
        }

        $statement = $connection->prepare(
            'SELECT id, printer_name, mac_address, paper_size, setup_method '
            . 'FROM printers WHERE user_id = ? ORDER BY id ASC'
        );
        $statement->bind_param('i', $userId);
        $statement->execute();
        $result = $statement->get_result();
        $printers = [];
        while ($printer = $result->fetch_assoc()) {
            $printer['id'] = (int) $printer['id'];
            $printers[] = $printer;
        }

        echo json_encode([
            'success' => true,
            'printers' => $printers,
        ]);
        $statement->close();
        $connection->close();
        exit;
    }

    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        $payload = json_decode(file_get_contents('php://input'), true);
        $userId = (int) ($payload['user_id'] ?? 0);
        $printerName = trim($payload['printer_name'] ?? '');
        $macAddress = trim($payload['mac_address'] ?? '');
        $paperSize = trim($payload['paper_size'] ?? '');
        $setupMethod = trim($payload['setup_method'] ?? 'Bluetooth');
        $allowedMethods = ['Bluetooth', 'Ethernet', 'Wifi'];

        if (!in_array($setupMethod, $allowedMethods, true)) {
            $setupMethod = 'Bluetooth';
        }

        if ($userId <= 0 || $printerName === '' || $macAddress === '' || $paperSize === '') {
            http_response_code(400);
            echo json_encode([
                'success' => false,
                'message' => 'Usuario, nombre, MAC address y tamaño de papel son obligatorios',
            ]);
            exit;
        }

        $statement = $connection->prepare(
            'INSERT INTO printers (user_id, printer_name, mac_address, paper_size, setup_method) '
            . 'VALUES (?, ?, ?, ?, ?) '
            . 'ON DUPLICATE KEY UPDATE printer_name = VALUES(printer_name), '
            . 'paper_size = VALUES(paper_size), setup_method = VALUES(setup_method)'
        );
        $statement->bind_param('issss', $userId, $printerName, $macAddress, $paperSize, $setupMethod);
        $statement->execute();
        $statement->close();

        $idStatement = $connection->prepare('SELECT id FROM printers WHERE user_id = ? AND mac_address = ? LIMIT 1');
        $idStatement->bind_param('is', $userId, $macAddress);
        $idStatement->execute();
        $printer = $idStatement->get_result()->fetch_assoc();
        $printerId = $printer ? (int) $printer['id'] : 0;
        $idStatement->close();

        echo json_encode([
            'success' => true,
            'message' => 'Impresora guardada',
            'printer_id' => $printerId,
        ]);
        $connection->close();
        exit;
    }

    http_response_code(405);
    echo json_encode([
        'success' => false,
        'message' => 'Método no permitido',
    ]);
} catch (Throwable $exception) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error interno del servidor',
    ]);
}
