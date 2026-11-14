<?php
declare(strict_types=1);

$configPath = __DIR__ . '/config.php';
if (!file_exists($configPath)) {
    http_response_code(500);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode([
        'success' => false,
        'message' => 'Server configuration missing: copy config.example.php to config.php on pttpshuklacollege.com.'
    ]);
    exit;
}

$CONFIG = require $configPath;
date_default_timezone_set($CONFIG['timezone'] ?? 'Asia/Kolkata');

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: Content-Type, Authorization');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');

if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'OPTIONS') {
    http_response_code(204);
    exit;
}

function get_pdo(): PDO {
    static $pdo = null;
    global $CONFIG;
    if ($pdo instanceof PDO) {
        return $pdo;
    }
    $db = $CONFIG['db'];
    $dsn = sprintf(
        'mysql:host=%s;port=%s;dbname=%s;charset=%s',
        $db['host'],
        $db['port'],
        $db['name'],
        $db['charset'] ?? 'utf8mb4'
    );
    $pdo = new PDO($dsn, $db['user'], $db['pass'], [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES => false,
    ]);
    return $pdo;
}

function read_json_body(): array {
    $raw = file_get_contents('php://input');
    if ($raw === false || trim($raw) === '') {
        return [];
    }
    $decoded = json_decode($raw, true);
    return is_array($decoded) ? $decoded : [];
}

function json_success($data, string $message = 'OK', bool $smtpDispatched = false, ?array $smtpReceipt = null, int $code = 200): void {
    http_response_code($code);
    $payload = [
        'success' => true,
        'message' => $message,
        'data' => $data,
        'smtp_dispatched' => $smtpDispatched,
    ];
    if ($smtpReceipt !== null) {
        $payload['smtp_receipt'] = $smtpReceipt;
    }
    echo json_encode($payload, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
    exit;
}

function json_error(string $message, int $code = 400): void {
    http_response_code($code);
    echo json_encode([
        'success' => false,
        'message' => $message,
        'data' => null,
        'smtp_dispatched' => false,
    ], JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
    exit;
}

function format_staff_profile(array $row): array {
    return [
        'id' => (int)$row['id'],
        'login_id' => $row['login_id'],
        'gmail_id' => $row['gmail_id'],
        'gmail_verified' => (bool)$row['gmail_verified'],
        'full_name' => $row['full_name'],
        'department' => $row['department'],
        'designation' => $row['designation'],
        'role' => $row['role'],
        'approval_status' => $row['approval_status'] ?? 'APPROVED',
    ];
}

function issue_auth_session(PDO $pdo, array $staffRow): array {
    global $CONFIG;
    $rawToken = bin2hex(random_bytes(32));
    $tokenHash = hash('sha256', $rawToken);
    $ttlDays = (int)($CONFIG['token_ttl_days'] ?? 30);
    $expiresAt = date('Y-m-d H:i:s', strtotime("+{$ttlDays} days"));

    $stmt = $pdo->prepare('INSERT INTO api_tokens (staff_id, token_hash, expires_at) VALUES (?, ?, ?)');
    $stmt->execute([(int)$staffRow['id'], $tokenHash, $expiresAt]);

    return [
        'token' => $rawToken,
        'expires_at' => $expiresAt,
        'staff' => format_staff_profile($staffRow),
    ];
}

function require_auth(PDO $pdo): array {
    $authHeader = $_SERVER['HTTP_AUTHORIZATION'] ?? '';
    if (!preg_match('/Bearer\s+([a-f0-9]{64})/i', $authHeader, $matches)) {
        json_error('Authentication required. Please sign in again.', 401);
    }
    $tokenHash = hash('sha256', $matches[1]);
    $stmt = $pdo->prepare(
        'SELECT u.* FROM api_tokens t
         INNER JOIN staff_users u ON u.id = t.staff_id
         WHERE t.token_hash = ? AND t.expires_at > NOW()
           AND u.is_active = 1 AND u.approval_status = "APPROVED"
         LIMIT 1'
    );
    $stmt->execute([$tokenHash]);
    $user = $stmt->fetch();
    if (!$user) {
        json_error('Session expired or account pending admin approval.', 401);
    }
    return $user;
}
