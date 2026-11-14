<?php
declare(strict_types=1);
require_once __DIR__ . '/../db.php';

$body = read_json_body();
$identifier = trim((string)($body['identifier'] ?? ''));
$password = (string)($body['password'] ?? '');

if ($identifier === '' || $password === '') {
    json_error('Enter both Login ID (or mapped Gmail) and password.', 422);
}

$pdo = get_pdo();
$stmt = $pdo->prepare(
    'SELECT * FROM staff_users
     WHERE (UPPER(login_id) = UPPER(?) OR LOWER(gmail_id) = LOWER(?))
     LIMIT 1'
);
$stmt->execute([$identifier, $identifier]);
$staff = $stmt->fetch();

if (!$staff || !password_verify($password, (string)$staff['password_hash'])) {
    json_error('Invalid Login ID / Gmail ID or password.', 401);
}

$approval = strtoupper((string)($staff['approval_status'] ?? 'PENDING'));
if ($approval === 'PENDING') {
    json_error('Your registration is awaiting Admin approval. You will receive an SMTP email on your mapped Gmail once approved.', 403);
}
if ($approval === 'REJECTED') {
    $reason = trim((string)($staff['rejection_reason'] ?? ''));
    json_error('Your registration was rejected by Admin.' . ($reason !== '' ? " Reason: {$reason}" : ''), 403);
}
if ((int)$staff['is_active'] !== 1) {
    json_error('Your account is currently inactive. Contact College Admin.', 403);
}

$session = issue_auth_session($pdo, $staff);
json_success($session, 'Authenticated successfully.');
