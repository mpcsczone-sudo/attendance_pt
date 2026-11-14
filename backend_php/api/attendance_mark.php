<?php
declare(strict_types=1);
require_once __DIR__ . '/../db.php';
require_once __DIR__ . '/../smtp_mailer.php';

$pdo = get_pdo();
$staff = require_auth($pdo);
$body = read_json_body();

$action = strtoupper(trim((string)($body['action'] ?? 'CHECK_IN')));
$remarks = trim((string)($body['remarks'] ?? ''));
$deviceInfo = trim((string)($body['device_info'] ?? 'Android App'));
$today = date('Y-m-d');
$nowTime = date('H:i:s');
$staffId = (int)$staff['id'];

$existingStmt = $pdo->prepare('SELECT * FROM attendance_records WHERE staff_id = ? AND attendance_date = ? LIMIT 1');
$existingStmt->execute([$staffId, $today]);
$existing = $existingStmt->fetch();

if ($action === 'CHECK_IN' || $action === 'ON_DUTY') {
    if ($existing && !empty($existing['check_in_time'])) {
        json_error("Attendance already checked in today at {$existing['check_in_time']}.", 409);
    }

    $cutoff = (string)($CONFIG['late_cutoff_time'] ?? '09:45:00');
    $status = $action === 'ON_DUTY' ? 'ON_DUTY' : ($nowTime > $cutoff ? 'LATE' : 'PRESENT');

    $ins = $pdo->prepare(
        'INSERT INTO attendance_records
         (staff_id, attendance_date, check_in_time, status, remarks, device_info, check_in_source)
         VALUES (?, ?, ?, ?, ?, ?, "ANDROID_APP")'
    );
    $ins->execute([$staffId, $today, $nowTime, $status, $remarks, $deviceInfo]);
} elseif ($action === 'CHECK_OUT') {
    if (!$existing || empty($existing['check_in_time'])) {
        json_error('Please check in before checking out.', 400);
    }
    if (!empty($existing['check_out_time'])) {
        json_error("Already checked out today at {$existing['check_out_time']}.", 409);
    }
    $mergedRemarks = $existing['remarks'];
    if ($remarks !== '') {
        $mergedRemarks = $mergedRemarks !== '' ? "{$mergedRemarks} | Out: {$remarks}" : $remarks;
    }
    $upd = $pdo->prepare(
        'UPDATE attendance_records SET check_out_time = ?, remarks = ? WHERE id = ?'
    );
    $upd->execute([$nowTime, $mergedRemarks, (int)$existing['id']]);
} else {
    json_error('Invalid attendance action.', 400);
}

$fetchStmt = $pdo->prepare(
    'SELECT a.*, u.login_id, u.full_name, u.department, u.gmail_id
     FROM attendance_records a
     INNER JOIN staff_users u ON u.id = a.staff_id
     WHERE a.staff_id = ? AND a.attendance_date = ?
     LIMIT 1'
);
$fetchStmt->execute([$staffId, $today]);
$record = $fetchStmt->fetch();

$eventLabel = $action === 'CHECK_OUT' ? 'CHECK_OUT' : 'CHECK_IN';
$subject = "[PTTP Attendance] {$staff['login_id']} {$eventLabel} ({$record['status']}) — {$today}";
$html = "<h3>Pt. T. P. Shukla College — Attendance Notification</h3>"
      . "<p><strong>Staff:</strong> " . htmlspecialchars((string)$staff['full_name']) . " ({$staff['login_id']})</p>"
      . "<p><strong>Department:</strong> " . htmlspecialchars((string)$staff['department']) . "</p>"
      . "<p><strong>Date:</strong> {$today}</p>"
      . "<p><strong>Status:</strong> {$record['status']}</p>"
      . "<p><strong>Check-In:</strong> " . ($record['check_in_time'] ?? '--') . " | <strong>Check-Out:</strong> " . ($record['check_out_time'] ?? '--') . "</p>"
      . ($remarks !== '' ? "<p><strong>Remarks:</strong> " . htmlspecialchars($remarks) . "</p>" : '');

$cc = $CONFIG['smtp']['hod_cc_email'] ?? null;
$receipt = send_smtp_and_log($pdo, $staffId, (string)$staff['gmail_id'], $eventLabel, $subject, $html, $cc);

json_success(
    [
        'id' => (int)$record['id'],
        'staff_id' => (int)$record['staff_id'],
        'login_id' => $record['login_id'],
        'full_name' => $record['full_name'],
        'department' => $record['department'],
        'gmail_id' => $record['gmail_id'],
        'attendance_date' => $record['attendance_date'],
        'check_in_time' => $record['check_in_time'],
        'check_out_time' => $record['check_out_time'],
        'status' => $record['status'],
        'remarks' => $record['remarks'],
        'check_in_source' => $record['check_in_source'],
    ],
    $receipt['status'] === 'SENT'
        ? "{$eventLabel} recorded & SMTP email sent to {$staff['gmail_id']}."
        : "{$eventLabel} recorded in MySQL (SMTP pending: {$receipt['error_message']}).",
    $receipt['status'] === 'SENT',
    $receipt
);
