<?php
declare(strict_types=1);
require_once __DIR__ . '/../db.php';
require_once __DIR__ . '/../smtp_mailer.php';

$actor = require_auth($pdo = get_pdo());
$body = read_json_body();
$action = (string)($body['action'] ?? '');
$loginId = strtoupper(trim((string)($body['login_id'] ?? $actor['login_id'])));
$gmailId = strtolower(trim((string)($body['gmail_id'] ?? '')));
$otp = trim((string)($body['otp'] ?? ''));

if (!filter_var($gmailId, FILTER_VALIDATE_EMAIL)) {
    json_error('Enter a valid Gmail address.', 422);
}

if ($action === 'initiate_link') {
    $dup = $pdo->prepare('SELECT id FROM staff_users WHERE LOWER(gmail_id) = ? AND id <> ? LIMIT 1');
    $dup->execute([$gmailId, (int)$actor['id']]);
    if ($dup->fetch()) {
        json_error('This Gmail is already mapped to another staff Login ID.', 409);
    }

    $code = str_pad((string)random_int(100000, 999999), 6, '0', STR_PAD_LEFT);
    $otpHash = password_hash($code, PASSWORD_DEFAULT);
    $expiresAt = date('Y-m-d H:i:s', time() + 600);

    $pdo->prepare(
        "INSERT INTO otp_verifications (staff_id, gmail_id, otp_hash, purpose, expires_at)
         VALUES (?, ?, ?, 'GMAIL_LINK', ?)"
    )->execute([(int)$actor['id'], $gmailId, $otpHash, $expiresAt]);

    $subject = "Verify Login ID {$actor['login_id']} ↔ Gmail Mapping";
    $html = "<h3>Verify Your Mapped Gmail ID</h3>"
          . "<p>Staff Member: <strong>" . htmlspecialchars((string)$actor['full_name']) . "</strong> ({$actor['login_id']})</p>"
          . "<p>Enter this 6-digit code in the PTTP Staff app to confirm mapping to <strong>{$gmailId}</strong>:</p>"
          . "<h2 style='letter-spacing:4px;color:#13294B;'>{$code}</h2>";

    $receipt = send_smtp_and_log($pdo, (int)$actor['id'], $gmailId, 'GMAIL_VERIFICATION', $subject, $html);

    json_success([
        'masked_gmail' => $gmailId,
        'login_id' => $actor['login_id'],
        'expires_in_seconds' => 600,
    ], 'Verification OTP sent via SMTP.', $receipt['status'] === 'SENT', $receipt);
}

if ($action === 'confirm_link') {
    $q = $pdo->prepare(
        "SELECT * FROM otp_verifications
         WHERE staff_id = ? AND LOWER(gmail_id) = ? AND purpose = 'GMAIL_LINK'
           AND consumed_at IS NULL AND expires_at > NOW()
         ORDER BY id DESC LIMIT 1"
    );
    $q->execute([(int)$actor['id'], $gmailId]);
    $row = $q->fetch();

    if (!$row || !password_verify($otp, (string)$row['otp_hash'])) {
        json_error('Invalid or expired verification code.', 400);
    }

    $pdo->prepare('UPDATE otp_verifications SET consumed_at = NOW() WHERE id = ?')->execute([(int)$row['id']]);
    $pdo->prepare('UPDATE staff_users SET gmail_id = ?, gmail_verified = 1 WHERE id = ?')
        ->execute([$gmailId, (int)$actor['id']]);

    $u = $pdo->prepare('SELECT * FROM staff_users WHERE id = ?');
    $u->execute([(int)$actor['id']]);
    $updated = $u->fetch();

    $receipt = send_smtp_and_log(
        $pdo,
        (int)$actor['id'],
        $gmailId,
        'GMAIL_MAPPED',
        "Confirmed: {$updated['login_id']} Mapped to {$gmailId}",
        "<p>Your Login ID <strong>{$updated['login_id']}</strong> is now verified and linked to <strong>{$gmailId}</strong>.</p>"
    );

    json_success(format_staff_profile($updated), 'Gmail mapping verified.', $receipt['status'] === 'SENT', $receipt);
}

if ($action === 'admin_map') {
    if (!in_array($actor['role'], ['HOD', 'ADMIN'], true)) {
        json_error('Only HOD or Admin can map other staff accounts.', 403);
    }
    $targetStmt = $pdo->prepare('SELECT * FROM staff_users WHERE UPPER(login_id) = ? LIMIT 1');
    $targetStmt->execute([$loginId]);
    $target = $targetStmt->fetch();
    if (!$target) {
        json_error("No staff account found with Login ID {$loginId}.", 404);
    }

    $pdo->prepare('UPDATE staff_users SET gmail_id = ?, gmail_verified = 1 WHERE id = ?')
        ->execute([$gmailId, (int)$target['id']]);

    $u = $pdo->prepare('SELECT * FROM staff_users WHERE id = ?');
    $u->execute([(int)$target['id']]);
    $updated = $u->fetch();

    $receipt = send_smtp_and_log(
        $pdo,
        (int)$updated['id'],
        $gmailId,
        'ADMIN_GMAIL_MAP',
        "Admin Mapped {$updated['login_id']} to Your Gmail",
        "<p>Pt. T. P. Shukla College Admin has mapped Login ID <strong>{$updated['login_id']}</strong> to <strong>{$gmailId}</strong> for attendance notifications.</p>"
    );

    json_success(format_staff_profile($updated), 'Staff Login ID mapped to Gmail.', $receipt['status'] === 'SENT', $receipt);
}

json_error('Unsupported mapping action.', 400);
