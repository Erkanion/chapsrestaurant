<?php
require __DIR__ . '/config.php';

header('Content-Type: application/json; charset=utf-8');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode([
        'success' => false,
        'message' => 'Método no permitido',
    ]);
    exit;
}

$payload = json_decode(file_get_contents('php://input'), true);
$user = trim($payload['user'] ?? '');
$password = $payload['password'] ?? '';

if ($user === '' || $password === '') {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'message' => 'Usuario y contraseña son obligatorios',
    ]);
    exit;
}

try {
    $pdo = db_connection();
    $statement = $pdo->prepare(
        'SELECT id, name, password_hash FROM users '
        . 'WHERE is_active = 1 AND (email = :user OR username = :user) LIMIT 1'
    );
    $statement->execute(['user' => $user]);
    $account = $statement->fetch();

    if (!$account || !password_verify($password, $account['password_hash'])) {
        http_response_code(401);
        echo json_encode([
            'success' => false,
            'message' => 'Credenciales incorrectas',
        ]);
        exit;
    }

    echo json_encode([
        'success' => true,
        'message' => 'Acceso concedido',
        'user_id' => (int) $account['id'],
        'name' => $account['name'],
    ]);
} catch (Throwable $exception) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error interno del servidor',
    ]);
}
