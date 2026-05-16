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
            'SELECT id, store_name, legal_name, rfc, tax_regime, phone, full_address, email, slogan, logo '
            . 'FROM businesses WHERE user_id = ? ORDER BY id ASC'
        );
        $statement->bind_param('i', $userId);
        $statement->execute();
        $result = $statement->get_result();
        $businesses = [];
        while ($business = $result->fetch_assoc()) {
            $business['id'] = (int) $business['id'];
            $businesses[] = $business;
        }

        echo json_encode([
            'success' => true,
            'businesses' => $businesses,
        ]);
        $statement->close();
        $connection->close();
        exit;
    }

    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        $payload = json_decode(file_get_contents('php://input'), true);
        $businessId = (int) ($payload['id'] ?? 0);
        $userId = (int) ($payload['user_id'] ?? 0);
        $storeName = trim($payload['store_name'] ?? '');
        $legalName = trim($payload['legal_name'] ?? '');
        $rfc = trim($payload['rfc'] ?? '');
        $taxRegime = trim($payload['tax_regime'] ?? '');
        $phone = trim($payload['phone'] ?? '');
        $fullAddress = trim($payload['full_address'] ?? '');
        $email = trim($payload['email'] ?? '');
        $slogan = trim($payload['slogan'] ?? '');
        $logo = trim($payload['logo'] ?? '');

        if ($userId <= 0 || $storeName === '') {
            http_response_code(400);
            echo json_encode([
                'success' => false,
                'message' => 'Usuario y nombre del negocio son obligatorios',
            ]);
            exit;
        }

        if ($businessId > 0) {
            $statement = $connection->prepare(
                'UPDATE businesses SET store_name = ?, legal_name = ?, rfc = ?, tax_regime = ?, phone = ?, '
                . 'full_address = ?, email = ?, slogan = ?, logo = ? WHERE id = ? AND user_id = ?'
            );
            $statement->bind_param(
                'sssssssssii',
                $storeName,
                $legalName,
                $rfc,
                $taxRegime,
                $phone,
                $fullAddress,
                $email,
                $slogan,
                $logo,
                $businessId,
                $userId
            );
            $statement->execute();
            $statement->close();
        } else {
            $statement = $connection->prepare(
                'INSERT INTO businesses (user_id, store_name, legal_name, rfc, tax_regime, phone, full_address, email, slogan, logo) '
                . 'VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
            );
            $statement->bind_param(
                'isssssssss',
                $userId,
                $storeName,
                $legalName,
                $rfc,
                $taxRegime,
                $phone,
                $fullAddress,
                $email,
                $slogan,
                $logo
            );
            $statement->execute();
            $businessId = $connection->insert_id;
            $statement->close();
        }

        echo json_encode([
            'success' => true,
            'message' => 'Negocio guardado',
            'business_id' => (int) $businessId,
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
