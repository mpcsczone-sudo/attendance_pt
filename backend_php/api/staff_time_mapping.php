<?php
declare(strict_types=1);
require_once __DIR__ . '/../db.php';
require_once __DIR__ . '/../smtp_mailer.php';

$pdo = get_pdo();
$actor = require_auth($pdo);

$body = read_json_body();
$targetLoginId = strtoupper(trim((string)($body['login_id'] ?? $actor['login_id'])));
$startTime = trim((string)($body['shift_start_time'] ?? ''));
$endTime = trim((string)($body['shift_end_time'] ?? ''));

if (!preg_match('/^\d{2}:\d{2}(:\d{2})?$/', $startTime) || !preg_match('/^\d{2}:\d{2}(:\d{2})?$/', $endTime)) {
    json_error('Provide valid shift times in HH:MM format (e.g. 09:30 and 16:30).', 422);
}

if (strlen($startTime) === 5) $startTime .= ':00';
if (strlen($endTime) === 5) $endTime .= ':00';

// Staff can update own time mapping only if Admin/HOD allows, or Admin/HOD can map any staff
if (strcasecmp($targetLoginId, (string)$actor['login_id']) !== 0 && !in_array($actor['role'], ['ADMIN', 'HOD'], true)) {
    json_error('Only Admin or HOD can assign shift timings for other staff.', 403);
}

$tStmt = $pdo->prepare('SELECT * FROM staff_users WHERE UPPER(login_id) = ? LIMIT 1');
$tStmt->execute([$targetLoginId]);
$target = $tStmt->fetch();
if (!$target) {
    json_error("No staff found with Login ID {$targetLoginId}.", 404);
}

$upd = $pdo->prepare('UPDATE staff_users SET shift_start_time = ?, shift_end_time = ? WHERE id = ?');
$upd->execute([$startTime, $endTime, (int)$target['id']]);

$u = $pdo->prepare('SELECT * FROM staff_users WHERE id = ?');
$u->execute([(int)$target['id']]);
$updated = $u->fetch();

$sLabel = substr($startTime, 0, 5);
$eLabel = substr($endTime, 0, 5);
$subject = "[PTTP Shift Time Mapping] {$updated['login_id']}: {$sLabel} – {$eLabel}";
$html = "<h3>Staff Shift Time Mapping Updated</h3>"
      . "<p>Dear <strong>" . htmlspecialchars((string)$updated['full_name']) . "</strong> ({$updated['login_id']}),</p>"
      . "<p>Your attendance shift schedule is now mapped to <strong>{$sLabel} – {$eLabel}</strong>.</p>"
      . "<p>The app will notify you 10 minutes before <strong>{$sLabel}</strong> if you have not checked in yet.</p>";

$receipt = send_smtp_and_log($pdo, (int)$updated['id'], (string)$updated['gmail_id'], 'SHIFT_TIME_MAPPED', $subject, $html);

json_success(
    format_staff_profile($updated),
    "Shift timing updated to {$sLabel} – {$eLabel} for {$updated['login_id']}.",
    $receipt['status'] === 'SENT',
    $receipt
);
