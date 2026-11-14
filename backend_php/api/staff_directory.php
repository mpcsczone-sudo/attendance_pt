<?php
declare(strict_types=1);
require_once __DIR__ . '/../db.php';

$pdo = get_pdo();
$staff = require_auth($pdo);

if (!in_array($staff['role'], ['HOD', 'ADMIN'], true)) {
    json_error('Staff directory is restricted to HOD and Admin.', 403);
}

if ($staff['role'] === 'ADMIN') {
    $stmt = $pdo->query('SELECT * FROM staff_users WHERE is_active = 1 ORDER BY login_id ASC LIMIT 200');
} else {
    $stmt = $pdo->prepare('SELECT * FROM staff_users WHERE department = ? AND is_active = 1 ORDER BY login_id ASC LIMIT 200');
    $stmt->execute([$staff['department']]);
}

$list = array_map('format_staff_profile', $stmt->fetchAll());
json_success($list);
