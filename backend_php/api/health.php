<?php
declare(strict_types=1);
require_once __DIR__ . '/../db.php';

$mysqlOk = false;
try {
    $pdo = get_pdo();
    $pdo->query('SELECT 1');
    $mysqlOk = true;
} catch (Throwable $e) {
    $mysqlOk = false;
}

$smtpPass = (string)($CONFIG['smtp']['password'] ?? '');
$smtpConfigured = !empty($CONFIG['smtp']['host']) && $smtpPass !== '' && !str_contains($smtpPass, 'YOUR_SMTP');

json_success([
    'status' => $mysqlOk ? 'online' : 'degraded',
    'php_version' => PHP_VERSION,
    'mysql_connected' => $mysqlOk,
    'smtp_configured' => $smtpConfigured,
    'college_domain' => $CONFIG['college_domain'] ?? 'https://pttpshuklacollege.com/',
    'server_time' => date('Y-m-d H:i:s'),
], $mysqlOk ? 'PHP & MySQL connected.' : 'PHP reachable, MySQL connection check failed.');
