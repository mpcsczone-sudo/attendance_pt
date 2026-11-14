<?php
declare(strict_types=1);
require_once __DIR__ . '/../db.php';
require_once __DIR__ . '/../smtp_mailer.php';

$pdo = get_pdo();
$staff = require_auth($pdo);
$staffId = (int)$staff['id'];
$receipt = null;
$msg = 'Leave requests loaded.';

if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'POST') {
    $body = read_json_body();
    $action = strtolower(trim((string)($body['action'] ?? 'submit')));

    if ($action === 'submit') {
        $leaveType = strtoupper(trim((string)($body['leave_type'] ?? 'CASUAL')));
        $startDate = trim((string)($body['start_date'] ?? ''));
        $endDate = trim((string)($body['end_date'] ?? ''));
        $reason = trim((string)($body['reason'] ?? ''));

        if (!in_array($leaveType, ['CASUAL', 'EARNED', 'MEDICAL', 'ON_DUTY', 'REGULARIZATION'], true)) {
            $leaveType = 'CASUAL';
        }
        if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $startDate) || !preg_match('/^\d{4}-\d{2}-\d{2}$/', $endDate) || $reason === '') {
            json_error('Provide valid YYYY-MM-DD dates and reason.', 422);
        }

        $ins = $pdo->prepare(
            "INSERT INTO leave_requests (staff_id, leave_type, start_date, end_date, reason, status)
             VALUES (?, ?, ?, ?, ?, 'PENDING')"
        );
        $ins->execute([$staffId, $leaveType, $startDate, $endDate, $reason]);

        $subject = "[PTTP Leave Request] {$staff['login_id']} ({$leaveType}) {$startDate} to {$endDate}";
        $html = "<h3>Leave / Regularization Submitted</h3>"
              . "<p><strong>Staff:</strong> " . htmlspecialchars((string)$staff['full_name']) . " ({$staff['login_id']})</p>"
              . "<p><strong>Mapped Gmail:</strong> {$staff['gmail_id']}</p>"
              . "<p><strong>Type:</strong> {$leaveType} | <strong>Dates:</strong> {$startDate} to {$endDate}</p>"
              . "<p><strong>Reason:</strong> " . htmlspecialchars($reason) . "</p>";

        $cc = $CONFIG['smtp']['hod_cc_email'] ?? null;
        $receipt = send_smtp_and_log($pdo, $staffId, (string)$staff['gmail_id'], 'LEAVE_SUBMITTED', $subject, $html, $cc);
        $msg = 'Request submitted and SMTP alert sent.';
    } elseif ($action === 'approve' || $action === 'reject') {
        if (!in_array($staff['role'], ['HOD', 'ADMIN'], true)) {
            json_error('Only HOD or Admin can review requests.', 403);
        }
        $requestId = (int)($body['request_id'] ?? 0);
        $reviewNote = trim((string)($body['review_note'] ?? ''));
        $newStatus = $action === 'approve' ? 'APPROVED' : 'REJECTED';

        $q = $pdo->prepare(
            'SELECT l.*, u.login_id, u.full_name, u.gmail_id
             FROM leave_requests l
             INNER JOIN staff_users u ON u.id = l.staff_id
             WHERE l.id = ? LIMIT 1'
        );
        $q->execute([$requestId]);
        $target = $q->fetch();
        if (!$target) {
            json_error('Leave request not found.', 404);
        }

        $upd = $pdo->prepare(
            'UPDATE leave_requests SET status = ?, reviewed_by = ?, review_note = ? WHERE id = ?'
        );
        $upd->execute([$newStatus, $staffId, $reviewNote, $requestId]);

        $subject = "[PTTP Leave {$newStatus}] {$target['leave_type']} ({$target['start_date']} to {$target['end_date']})";
        $html = "<h3>Leave Request {$newStatus}</h3>"
              . "<p>Dear <strong>" . htmlspecialchars((string)$target['full_name']) . "</strong> ({$target['login_id']}),</p>"
              . "<p>Your <strong>{$target['leave_type']}</strong> request for {$target['start_date']} to {$target['end_date']} has been <strong>{$newStatus}</strong> by {$staff['full_name']}.</p>"
              . ($reviewNote !== '' ? "<p><strong>Remark:</strong> " . htmlspecialchars($reviewNote) . "</p>" : '');

        $receipt = send_smtp_and_log($pdo, (int)$target['staff_id'], (string)$target['gmail_id'], 'LEAVE_' . $newStatus, $subject, $html);
        $msg = "Request {$newStatus} and SMTP notification sent to {$target['gmail_id']}.";
    } elseif ($action === 'cancel') {
        $requestId = (int)($body['request_id'] ?? 0);
        $q = $pdo->prepare('SELECT * FROM leave_requests WHERE id = ? AND staff_id = ? LIMIT 1');
        $q->execute([$requestId, $staffId]);
        $target = $q->fetch();
        if (!$target) {
            json_error('Leave request not found.', 404);
        }
        if (strtoupper((string)$target['status']) !== 'PENDING') {
            json_error("Cannot cancel: request is already {$target['status']}.", 409);
        }

        $del = $pdo->prepare("DELETE FROM leave_requests WHERE id = ? AND staff_id = ? AND status = 'PENDING'");
        $del->execute([$requestId, $staffId]);

        $subject = "[PTTP Leave Cancelled] {$staff['login_id']} ({$target['leave_type']})";
        $html = "<h3>Pending Leave Request Cancelled</h3>"
              . "<p><strong>Staff:</strong> " . htmlspecialchars((string)$staff['full_name']) . " ({$staff['login_id']})</p>"
              . "<p>Your pending <strong>{$target['leave_type']}</strong> request ({$target['start_date']} to {$target['end_date']}) was cancelled before approval.</p>";
        $receipt = send_smtp_and_log($pdo, $staffId, (string)$staff['gmail_id'], 'LEAVE_CANCELLED', $subject, $html);
        $msg = 'Pending leave request cancelled.';
    } else {
        json_error('Unsupported leave action.', 400);
    }
}

$formatLeave = static function (array $r): array {
    return [
        'id' => (int)$r['id'],
        'staff_id' => (int)$r['staff_id'],
        'login_id' => $r['login_id'],
        'full_name' => $r['full_name'],
        'gmail_id' => $r['gmail_id'],
        'department' => $r['department'],
        'leave_type' => $r['leave_type'],
        'start_date' => $r['start_date'],
        'end_date' => $r['end_date'],
        'reason' => $r['reason'],
        'status' => $r['status'],
        'review_note' => $r['review_note'],
        'created_at' => $r['created_at'],
    ];
};

$myStmt = $pdo->prepare(
    'SELECT l.*, u.login_id, u.full_name, u.gmail_id, u.department
     FROM leave_requests l
     INNER JOIN staff_users u ON u.id = l.staff_id
     WHERE l.staff_id = ?
     ORDER BY l.id DESC LIMIT 40'
);
$myStmt->execute([$staffId]);
$myRequests = array_map($formatLeave, $myStmt->fetchAll());

$pendingApprovals = [];
if (in_array($staff['role'], ['HOD', 'ADMIN'], true)) {
    if ($staff['role'] === 'ADMIN') {
        $pStmt = $pdo->query(
            "SELECT l.*, u.login_id, u.full_name, u.gmail_id, u.department
             FROM leave_requests l
             INNER JOIN staff_users u ON u.id = l.staff_id
             WHERE l.status = 'PENDING'
             ORDER BY l.id DESC LIMIT 50"
        );
    } else {
        $pStmt = $pdo->prepare(
            "SELECT l.*, u.login_id, u.full_name, u.gmail_id, u.department
             FROM leave_requests l
             INNER JOIN staff_users u ON u.id = l.staff_id
             WHERE l.status = 'PENDING' AND u.department = ?
             ORDER BY l.id DESC LIMIT 50"
        );
        $pStmt->execute([$staff['department']]);
    }
    $pendingApprovals = array_map($formatLeave, $pStmt->fetchAll());
}

json_success([
    'my_requests' => $myRequests,
    'pending_approvals' => $pendingApprovals,
], $msg, $receipt !== null && $receipt['status'] === 'SENT', $receipt);
