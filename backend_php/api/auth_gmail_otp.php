<?php
declare(strict_types=1);
require_once __DIR__ . '/../db.php';
require_once __DIR__ . '/../smtp_mailer.php';

$body = read_json_body();
$action = (string)($body['action'] ?? '');
$identifier = trim((string)($body['identifier'] ?? ''));
$otp = trim((string)($body['otp'] ?? ''));

if ($identifier === '') {
    json_error('Enter your College Login ID or mapped Gmail ID.', 422);
}

$pdo = get_pdo();
$stmt = $pdo->prepare(
    'SELECT * FROM staff_users
     WHERE (UPPER(login_id) = UPPER(?) OR LOWER(gmail_id) = LOWER(?))
     LIMIT 1'
);
$stmt->execute([$identifier, $identifier]);
$staff = $stmt->fetch();

if (!$staff) {
    json_error('No staff registration found for that Login ID or Gmail.', 404);
}

$approval = strtoupper((string)($staff['approval_status'] ?? 'PENDING'));
if ($approval !== 'APPROVED' || (int)$staff['is_active'] !== 1) {
    json_error('Your Gmail registration is pending Admin approval. Login opens after Admin approval.', 403);
}

$staffId = (int)$staff['id'];
$gmail = (string)$staff['gmail_id'];

if ($action === 'request_otp') {
    $code = str_pad((string)random_int(100000, 999999), 6, '0', STR_PAD_LEFT);
    $otpHash = password_hash($code, PASSWORD_DEFAULT);
    $expiresAt = date('Y-m-d H:i:s', time() + 600);

    $ins = $pdo->prepare(
        "INSERT INTO otp_verifications (staff_id, gmail_id, otp_hash, purpose, expires_at)
         VALUES (?, ?, ?, 'LOGIN', ?)"
    );
    $ins->execute([$staffId, $gmail, $otpHash, $expiresAt]);

    $subject = "PTTP College Staff Login OTP: {$code}";
    $html = "<h3>One-Time Sign-In Verification</h3>"
          . "<p>Staff Login ID: <strong>{$staff['login_id']}</strong></p>"
          . "<p>Your 6-digit verification code is:</p>"
          . "<h2 style='letter-spacing:4px;color:#1D4ED8;'>{$code}</h2>"
          . "<p>Valid for 10 minutes.</p>";

    $receipt = send_smtp_and_log($pdo, $staffId, $gmail, 'LOGIN_OTP', $subject, $html);

    $parts = explode('@', $gmail);
    $masked = substr($parts[0], 0, 2) . '***@' . ($parts[1] ?? 'gmail.com');

    json_success([
        'masked_gmail' => $masked,
        'login_id' => $staff['login_id'],
        'expires_in_seconds' => 600,
    ], $receipt['status'] === 'SENT' ? "OTP dispatched via SMTP to {$masked}." : "OTP generated; check SMTP logs: {$receipt['error_message']}", $receipt['status'] === 'SENT', $receipt);
}

if ($action === 'verify_otp') {
    if ($otp === '') {
        json_error('Enter the 6-digit OTP code.', 422);
    }
    $q = $pdo->prepare(
        "SELECT * FROM otp_verifications
         WHERE staff_id = ? AND purpose = 'LOGIN' AND consumed_at IS NULL AND expires_at > NOW()
         ORDER BY id DESC LIMIT 1"
    );
    $q->execute([$staffId]);
    $row = $q->fetch();

    if (!$row || !password_verify($otp, (string)$row['otp_hash'])) {
        json_error('Invalid or expired OTP code.', 401);
    }

    $pdo->prepare('UPDATE otp_verifications SET consumed_at = NOW() WHERE id = ?')->execute([(int)$row['id']]);
    $pdo->prepare('UPDATE staff_users SET gmail_verified = 1 WHERE id = ?')->execute([$staffId]);
    $staff['gmail_verified'] = 1;

    $session = issue_auth_session($pdo, $staff);
    json_success($session, 'Gmail OTP verified and signed in.');
}

json_error('Unsupported OTP action.', 400);
