<?php
declare(strict_types=1);
require_once __DIR__ . '/../db.php';
require_once __DIR__ . '/../smtp_mailer.php';

$pdo = get_pdo();
$actor = require_auth($pdo);

if (!in_array($actor['role'], ['ADMIN', 'HOD'], true)) {
    json_error('Only Admin or HOD can approve staff registrations.', 403);
}

$receipt = null;
$msg = 'Pending registrations fetched.';

if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'POST') {
    $body = read_json_body();
    $action = strtolower(trim((string)($body['action'] ?? '')));
    $targetStaffId = (int)($body['staff_id'] ?? 0);
    $note = trim((string)($body['note'] ?? ''));

    $tStmt = $pdo->prepare('SELECT * FROM staff_users WHERE id = ? LIMIT 1');
    $tStmt->execute([$targetStaffId]);
    $target = $tStmt->fetch();
    if (!$target) {
        json_error('Staff registration not found.', 404);
    }

    if ($action === 'approve') {
        $upd = $pdo->prepare(
            "UPDATE staff_users
             SET approval_status = 'APPROVED', is_active = 1, gmail_verified = 1,
                 approved_by = ?, approved_at = NOW(), rejection_reason = NULL
             WHERE id = ?"
        );
        $upd->execute([(int)$actor['id'], $targetStaffId]);

        $subject = "Approved: PTTP College Staff Account ({$target['login_id']})";
        $html = "<h3>Registration Approved by Admin</h3>"
              . "<p>Dear <strong>" . htmlspecialchars((string)$target['full_name']) . "</strong>,</p>"
              . "<p>Your Login ID <strong>{$target['login_id']}</strong> mapped to <strong>{$target['gmail_id']}</strong> has been <strong>APPROVED</strong>.</p>"
              . "<p>You can now log in to the PTTP Staff Attendance app using your Login ID or Gmail.</p>";

        $receipt = send_smtp_and_log($pdo, $targetStaffId, (string)$target['gmail_id'], 'REGISTRATION_APPROVED', $subject, $html);
        $msg = "Approved {$target['login_id']} and sent SMTP notification to {$target['gmail_id']}.";
    } elseif ($action === 'reject') {
        $upd = $pdo->prepare(
            "UPDATE staff_users
             SET approval_status = 'REJECTED', is_active = 0,
                 approved_by = ?, approved_at = NOW(), rejection_reason = ?
             WHERE id = ?"
        );
        $upd->execute([(int)$actor['id'], $note, $targetStaffId]);

        $subject = "Registration Update: PTTP College Staff Portal ({$target['login_id']})";
        $html = "<h3>Registration Not Approved</h3>"
              . "<p>Dear <strong>" . htmlspecialchars((string)$target['full_name']) . "</strong>,</p>"
              . "<p>Your staff registration for Login ID <strong>{$target['login_id']}</strong> was rejected by Admin.</p>"
              . ($note !== '' ? "<p><strong>Note:</strong> " . htmlspecialchars($note) . "</p>" : '');

        $receipt = send_smtp_and_log($pdo, $targetStaffId, (string)$target['gmail_id'], 'REGISTRATION_REJECTED', $subject, $html);
        $msg = "Rejected {$target['login_id']} and notified {$target['gmail_id']} via SMTP.";
    } else {
        json_error('Invalid action. Use approve or reject.', 400);
    }
}

if ($actor['role'] === 'ADMIN') {
    $listStmt = $pdo->query(
        "SELECT * FROM staff_users WHERE approval_status = 'PENDING' ORDER BY created_at DESC LIMIT 100"
    );
} else {
    $listStmt = $pdo->prepare(
        "SELECT * FROM staff_users WHERE approval_status = 'PENDING' AND department = ? ORDER BY created_at DESC LIMIT 100"
    );
    $listStmt->execute([$actor['department']]);
}

$pending = array_map('format_staff_profile', $listStmt->fetchAll());
json_success($pending, $msg, $receipt !== null && $receipt['status'] === 'SENT', $receipt);
