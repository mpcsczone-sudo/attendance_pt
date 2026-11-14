<?php
declare(strict_types=1);
require_once __DIR__ . '/../db.php';
require_once __DIR__ . '/../smtp_mailer.php';

$pdo = get_pdo();
$staff = require_auth($pdo);
$staffId = (int)$staff['id'];
$receipt = null;
$msg = 'SMTP logs fetched.';

if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'POST') {
    $body = read_json_body();
    if (($body['action'] ?? '') === 'send_test') {
        $subject = "[PTTP SMTP Test] Verified Delivery for {$staff['login_id']}";
        $html = "<h3>SMTP Notification Test Successful</h3>"
              . "<p>Staff Member: <strong>" . htmlspecialchars((string)$staff['full_name']) . "</strong> ({$staff['login_id']})</p>"
              . "<p>Your mapped Gmail <strong>{$staff['gmail_id']}</strong> is receiving real-time SMTP updates from <strong>pttpshuklacollege.com</strong>.</p>";
        $receipt = send_smtp_and_log($pdo, $staffId, (string)$staff['gmail_id'], 'SMTP_TEST', $subject, $html);
        $msg = $receipt['status'] === 'SENT'
            ? "Test SMTP alert sent to {$staff['gmail_id']}."
            : "SMTP test failed: {$receipt['error_message']}";
    }
}

if ($staff['role'] === 'ADMIN') {
    $stmt = $pdo->query('SELECT * FROM smtp_notification_logs ORDER BY id DESC LIMIT 60');
} else {
    $stmt = $pdo->prepare('SELECT * FROM smtp_notification_logs WHERE staff_id = ? ORDER BY id DESC LIMIT 60');
    $stmt->execute([$staffId]);
}

$rows = array_map(static function (array $r): array {
    return [
        'id' => (int)$r['id'],
        'staff_id' => (int)$r['staff_id'],
        'recipient_gmail' => $r['recipient_gmail'],
        'event_type' => $r['event_type'],
        'subject' => $r['subject'],
        'body_preview' => $r['body_preview'],
        'smtp_message_id' => $r['smtp_message_id'],
        'delivery_status' => $r['delivery_status'],
        'error_message' => $r['error_message'],
        'sent_at' => $r['sent_at'],
    ];
}, $stmt->fetchAll());

json_success($rows, $msg, $receipt !== null && $receipt['status'] === 'SENT', $receipt);
