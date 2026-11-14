<?php
declare(strict_types=1);
require_once __DIR__ . '/../db.php';
require_once __DIR__ . '/../smtp_mailer.php';

$body = read_json_body();
$loginId = strtoupper(trim((string)($body['login_id'] ?? '')));
$gmailId = strtolower(trim((string)($body['gmail_id'] ?? '')));
$fullName = trim((string)($body['full_name'] ?? ''));
$department = trim((string)($body['department'] ?? ''));
$designation = trim((string)($body['designation'] ?? 'Faculty'));
$password = (string)($body['password'] ?? '');
$role = strtoupper(trim((string)($body['role'] ?? 'STAFF')));

if ($loginId === '' || !filter_var($gmailId, FILTER_VALIDATE_EMAIL) || $fullName === '' || $department === '' || strlen($password) < 6) {
    json_error('Provide valid Login ID, Gmail ID, full name, department, and password (min 6 chars).', 422);
}
if (!in_array($role, ['STAFF', 'HOD'], true)) {
    $role = 'STAFF';
}

$pdo = get_pdo();
$dup = $pdo->prepare('SELECT id FROM staff_users WHERE UPPER(login_id) = ? OR LOWER(gmail_id) = ? LIMIT 1');
$dup->execute([$loginId, $gmailId]);
if ($dup->fetch()) {
    json_error('Login ID or Gmail ID is already registered.', 409);
}

$hash = password_hash($password, PASSWORD_DEFAULT);
// Save with PENDING approval_status and is_active = 0 until Admin approves
$ins = $pdo->prepare(
    "INSERT INTO staff_users
     (login_id, gmail_id, gmail_verified, full_name, department, designation, role, approval_status, password_hash, is_active)
     VALUES (?, ?, 0, ?, ?, ?, ?, 'PENDING', ?, 0)"
);
$ins->execute([$loginId, $gmailId, $fullName, $department, $designation, $role, $hash]);
$staffId = (int)$pdo->lastInsertId();

$stmt = $pdo->prepare('SELECT * FROM staff_users WHERE id = ?');
$stmt->execute([$staffId]);
$staff = $stmt->fetch();

// 1. Send confirmation email to Staff's mapped Gmail
$subject = "PTTP College Registration Received — Pending Admin Approval ({$loginId})";
$html = "<h3>Registration Submitted for Admin Approval</h3>"
      . "<p>Dear <strong>" . htmlspecialchars($fullName) . "</strong>,</p>"
      . "<p>Your College Login ID <strong>{$loginId}</strong> has been mapped to <strong>{$gmailId}</strong>.</p>"
      . "<p>Your account is currently <strong>PENDING ADMIN APPROVAL</strong>. You will receive an SMTP confirmation on this Gmail as soon as the College Admin approves your registration, after which you can log in.</p>";

$ccAdmin = $CONFIG['smtp']['hod_cc_email'] ?? null;
$receipt = send_smtp_and_log($pdo, $staffId, $gmailId, 'REGISTRATION_PENDING', $subject, $html, $ccAdmin);

json_success(
    format_staff_profile($staff),
    'Registration submitted. Login will be enabled once Admin approves your Gmail mapping.',
    $receipt['status'] === 'SENT',
    $receipt,
    201
);
