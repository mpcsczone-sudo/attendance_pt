<?php
declare(strict_types=1);
require_once __DIR__ . '/../db.php';
require_once __DIR__ . '/../smtp_mailer.php';

$pdo = get_pdo();
$actor = require_auth($pdo);
$actorId = (int)$actor['id'];
$actorRole = strtoupper((string)$actor['role']);
$receipt = null;
$msg = 'Admin messages loaded.';

// Auto-create table on first call if user imported an earlier schema.sql
$pdo->exec(
    'CREATE TABLE IF NOT EXISTS `admin_messages` (
      `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
      `parent_id` BIGINT UNSIGNED DEFAULT NULL,
      `sender_staff_id` INT UNSIGNED NOT NULL,
      `target_staff_id` INT UNSIGNED DEFAULT NULL,
      `title` VARCHAR(180) NOT NULL DEFAULT \'\',
      `message_body` TEXT NOT NULL,
      `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (`id`),
      KEY `idx_msg_parent` (`parent_id`),
      KEY `idx_msg_target` (`target_staff_id`, `created_at`),
      CONSTRAINT `fk_msg_parent` FOREIGN KEY (`parent_id`) REFERENCES `admin_messages` (`id`) ON DELETE CASCADE,
      CONSTRAINT `fk_msg_sender` FOREIGN KEY (`sender_staff_id`) REFERENCES `staff_users` (`id`) ON DELETE CASCADE,
      CONSTRAINT `fk_msg_target` FOREIGN KEY (`target_staff_id`) REFERENCES `staff_users` (`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci'
);

if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'POST') {
    $body = read_json_body();
    $action = strtolower(trim((string)($body['action'] ?? '')));

    if ($action === 'send_admin_message') {
        if ($actorRole !== 'ADMIN') {
            json_error('Only Admin can publish new Admin notifications.', 403);
        }
        $targetLoginId = strtoupper(trim((string)($body['target_login_id'] ?? '')));
        $title = trim((string)($body['title'] ?? ''));
        $messageBody = trim((string)($body['message_body'] ?? ''));

        if ($title === '' || $messageBody === '') {
            json_error('Please enter both a subject/title and message.', 422);
        }

        $targetStaff = null;
        $targetStaffId = null;
        if ($targetLoginId !== '' && $targetLoginId !== 'ALL') {
            $tStmt = $pdo->prepare('SELECT * FROM staff_users WHERE UPPER(login_id) = ? LIMIT 1');
            $tStmt->execute([$targetLoginId]);
            $targetStaff = $tStmt->fetch();
            if (!$targetStaff) {
                json_error("No staff found with Login ID {$targetLoginId}.", 404);
            }
            $targetStaffId = (int)$targetStaff['id'];
        }

        $ins = $pdo->prepare(
            'INSERT INTO admin_messages (parent_id, sender_staff_id, target_staff_id, title, message_body)
             VALUES (NULL, ?, ?, ?, ?)'
        );
        $ins->execute([$actorId, $targetStaffId, $title, $messageBody]);

        // Dispatch SMTP notification to target or approved staff (capped batch for safety)
        $subject = "[PTTP Admin Notice] {$title}";
        $html = "<h3>Message from College Admin ({$actor['full_name']})</h3>"
              . "<p><strong>Title:</strong> " . htmlspecialchars($title) . "</p>"
              . "<p>" . nl2br(htmlspecialchars($messageBody)) . "</p>"
              . "<p><small>Open the PTTP Staff app → Notifications to reply.</small></p>";

        if ($targetStaff !== null) {
            $receipt = send_smtp_and_log($pdo, (int)$targetStaff['id'], (string)$targetStaff['gmail_id'], 'ADMIN_MESSAGE', $subject, $html);
            $msg = "Notification sent to {$targetStaff['login_id']}.";
        } else {
            $allStmt = $pdo->query("SELECT id, gmail_id FROM staff_users WHERE is_active = 1 AND approval_status = 'APPROVED' AND role <> 'ADMIN' LIMIT 50");
            $recipients = $allStmt->fetchAll();
            foreach ($recipients as $rec) {
                $receipt = send_smtp_and_log($pdo, (int)$rec['id'], (string)$rec['gmail_id'], 'ADMIN_MESSAGE', $subject, $html);
            }
            $msg = 'Notification sent to all approved staff.';
        }
    } elseif ($action === 'reply') {
        $parentId = (int)($body['parent_id'] ?? 0);
        $replyBody = trim((string)($body['message_body'] ?? ''));
        if ($parentId <= 0 || $replyBody === '') {
            json_error('Select a message and write a reply.', 422);
        }

        $pStmt = $pdo->prepare(
            'SELECT m.*, u.gmail_id AS sender_gmail, u.full_name AS sender_name, u.login_id AS sender_login_id
             FROM admin_messages m
             INNER JOIN staff_users u ON u.id = m.sender_staff_id
             WHERE m.id = ? AND m.parent_id IS NULL
             LIMIT 1'
        );
        $pStmt->execute([$parentId]);
        $parent = $pStmt->fetch();
        if (!$parent) {
            json_error('Admin message not found.', 404);
        }

        if ($actorRole !== 'ADMIN' && $parent['target_staff_id'] !== null && (int)$parent['target_staff_id'] !== $actorId) {
            json_error('You do not have access to reply to this message.', 403);
        }

        $ins = $pdo->prepare(
            'INSERT INTO admin_messages (parent_id, sender_staff_id, target_staff_id, title, message_body)
             VALUES (?, ?, ?, \'\', ?)'
        );
        $ins->execute([$parentId, $actorId, $parent['target_staff_id'], $replyBody]);

        // Notify Admin (when user replies) or Target Staff (when Admin replies)
        if ($actorRole !== 'ADMIN' && !empty($parent['sender_gmail'])) {
            $subject = "[Staff Reply] {$actor['login_id']} replied: {$parent['title']}";
            $html = "<h3>Reply from " . htmlspecialchars((string)$actor['full_name']) . " ({$actor['login_id']})</h3>"
                  . "<p><strong>In response to:</strong> " . htmlspecialchars((string)$parent['title']) . "</p>"
                  . "<p>" . nl2br(htmlspecialchars($replyBody)) . "</p>";
            $receipt = send_smtp_and_log($pdo, (int)$parent['sender_staff_id'], (string)$parent['sender_gmail'], 'USER_REPLY', $subject, $html);
        } elseif ($actorRole === 'ADMIN' && $parent['target_staff_id'] !== null) {
            $tStmt = $pdo->prepare('SELECT id, gmail_id FROM staff_users WHERE id = ? LIMIT 1');
            $tStmt->execute([(int)$parent['target_staff_id']]);
            if ($tUser = $tStmt->fetch()) {
                $subject = "[Admin Follow-Up] {$parent['title']}";
                $html = "<h3>Admin Reply</h3><p>" . nl2br(htmlspecialchars($replyBody)) . "</p>";
                $receipt = send_smtp_and_log($pdo, (int)$tUser['id'], (string)$tUser['gmail_id'], 'ADMIN_REPLY', $subject, $html);
            }
        }
        $msg = 'Reply sent.';
    } else {
        json_error('Invalid action for admin_messages.', 400);
    }
}

// Fetch root messages strictly from ADMIN
if ($actorRole === 'ADMIN') {
    $rootStmt = $pdo->query(
        "SELECT m.*,
                s.login_id AS sender_login_id, s.full_name AS sender_name, s.role AS sender_role,
                t.login_id AS target_login_id, t.full_name AS target_name
         FROM admin_messages m
         INNER JOIN staff_users s ON s.id = m.sender_staff_id
         LEFT JOIN staff_users t ON t.id = m.target_staff_id
         WHERE m.parent_id IS NULL AND s.role = 'ADMIN'
         ORDER BY m.id DESC
         LIMIT 50"
    );
} else {
    $rootStmt = $pdo->prepare(
        "SELECT m.*,
                s.login_id AS sender_login_id, s.full_name AS sender_name, s.role AS sender_role,
                t.login_id AS target_login_id, t.full_name AS target_name
         FROM admin_messages m
         INNER JOIN staff_users s ON s.id = m.sender_staff_id
         LEFT JOIN staff_users t ON t.id = m.target_staff_id
         WHERE m.parent_id IS NULL AND s.role = 'ADMIN'
           AND (m.target_staff_id IS NULL OR m.target_staff_id = ?)
         ORDER BY m.id DESC
         LIMIT 50"
    );
    $rootStmt->execute([$actorId]);
}

$roots = $rootStmt->fetchAll();
$rootIds = array_map(static fn(array $r): int => (int)$r['id'], $roots);
$repliesByParent = [];

if (count($rootIds) > 0) {
    $placeholders = implode(',', array_fill(0, count($rootIds), '?'));
    $repSql = "SELECT m.*, s.login_id AS sender_login_id, s.full_name AS sender_name, s.role AS sender_role
               FROM admin_messages m
               INNER JOIN staff_users s ON s.id = m.sender_staff_id
               WHERE m.parent_id IN ({$placeholders})";
    $params = $rootIds;
    if ($actorRole !== 'ADMIN') {
        $repSql .= " AND (m.sender_staff_id = ? OR s.role = 'ADMIN')";
        $params[] = $actorId;
    }
    $repSql .= ' ORDER BY m.id ASC';
    $repStmt = $pdo->prepare($repSql);
    $repStmt->execute($params);
    foreach ($repStmt->fetchAll() as $rep) {
        $pid = (int)$rep['parent_id'];
        $repliesByParent[$pid][] = [
            'id' => (int)$rep['id'],
            'parent_id' => $pid,
            'sender_staff_id' => (int)$rep['sender_staff_id'],
            'sender_login_id' => $rep['sender_login_id'],
            'sender_name' => $rep['sender_name'],
            'sender_role' => $rep['sender_role'],
            'message_body' => $rep['message_body'],
            'created_at' => $rep['created_at'],
        ];
    }
}

$threads = array_map(static function (array $r) use ($repliesByParent): array {
    $id = (int)$r['id'];
    return [
        'id' => $id,
        'sender_staff_id' => (int)$r['sender_staff_id'],
        'sender_name' => $r['sender_name'],
        'sender_login_id' => $r['sender_login_id'],
        'target_staff_id' => $r['target_staff_id'] !== null ? (int)$r['target_staff_id'] : null,
        'target_login_id' => $r['target_login_id'],
        'target_name' => $r['target_name'],
        'title' => $r['title'],
        'message_body' => $r['message_body'],
        'created_at' => $r['created_at'],
        'replies' => $repliesByParent[$id] ?? [],
    ];
}, $roots);

json_success($threads, $msg, $receipt !== null && $receipt['status'] === 'SENT', $receipt);
