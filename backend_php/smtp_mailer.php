<?php
declare(strict_types=1);

/**
 * Pure-PHP TLS/SSL SMTP Client + MySQL Audit Logger.
 * Works on standard cPanel PHP 8+ without requiring external Composer packages.
 */
function send_smtp_and_log(
    PDO $pdo,
    int $staffId,
    string $recipientGmail,
    string $eventType,
    string $subject,
    string $htmlBody,
    ?string $ccEmail = null
): array {
    global $CONFIG;
    $smtp = $CONFIG['smtp'] ?? [];
    $host = trim((string)($smtp['host'] ?? ''));
    $port = (int)($smtp['port'] ?? 587);
    $enc = strtolower((string)($smtp['encryption'] ?? 'tls'));
    $username = (string)($smtp['username'] ?? '');
    $password = (string)($smtp['password'] ?? '');
    $fromEmail = (string)($smtp['from_email'] ?? $username);
    $fromName = (string)($smtp['from_name'] ?? 'PTTP College Attendance');

    $messageId = sprintf('<%s.%d@pttpshuklacollege.com>', bin2hex(random_bytes(8)), time());
    $plainPreview = trim(preg_replace('/\s+/', ' ', strip_tags($htmlBody)) ?? '');
    $plainPreview = mb_substr($plainPreview, 0, 280);

    $status = 'FAILED';
    $errorMessage = null;

    try {
        if ($host === '' || $username === '' || str_contains($password, 'YOUR_SMTP')) {
            throw new RuntimeException('SMTP credentials not yet configured in config.php.');
        }

        $remote = ($enc === 'ssl' ? 'ssl://' : 'tcp://') . $host . ':' . $port;
        $socket = @stream_socket_client($remote, $errno, $errstr, 15, STREAM_CLIENT_CONNECT);
        if (!$socket) {
            throw new RuntimeException("SMTP connect failed: {$errstr} ({$errno})");
        }
        stream_set_timeout($socket, 15);

        $expect = static function ($sock, array $codes): string {
            $resp = '';
            while (($line = fgets($sock, 515)) !== false) {
                $resp .= $line;
                if (isset($line[3]) && $line[3] === ' ') {
                    break;
                }
            }
            $code = (int)substr($resp, 0, 3);
            if (!in_array($code, $codes, true)) {
                throw new RuntimeException('SMTP unexpected reply: ' . trim($resp));
            }
            return $resp;
        };

        $cmd = static function ($sock, string $line) use ($expect): void {
            fwrite($sock, $line . "\r\n");
        };

        $expect($socket, [220]);
        $cmd($socket, 'EHLO pttpshuklacollege.com');
        $expect($socket, [250]);

        if ($enc === 'tls') {
            $cmd($socket, 'STARTTLS');
            $expect($socket, [220]);
            if (!stream_socket_enable_crypto($socket, true, STREAM_CRYPTO_METHOD_TLS_CLIENT)) {
                throw new RuntimeException('Unable to negotiate STARTTLS encryption.');
            }
            $cmd($socket, 'EHLO pttpshuklacollege.com');
            $expect($socket, [250]);
        }

        $cmd($socket, 'AUTH LOGIN');
        $expect($socket, [334]);
        $cmd($socket, base64_encode($username));
        $expect($socket, [334]);
        $cmd($socket, base64_encode($password));
        $expect($socket, [235]);

        $cmd($socket, "MAIL FROM:<{$fromEmail}>");
        $expect($socket, [250]);
        $cmd($socket, "RCPT TO:<{$recipientGmail}>");
        $expect($socket, [250, 251]);

        if ($ccEmail !== null && filter_var($ccEmail, FILTER_VALIDATE_EMAIL) && strcasecmp($ccEmail, $recipientGmail) !== 0) {
            $cmd($socket, "RCPT TO:<{$ccEmail}>");
            $expect($socket, [250, 251]);
        }

        $cmd($socket, 'DATA');
        $expect($socket, [354]);

        $headers = [
            "Date: " . date('r'),
            "From: =?UTF-8?B?" . base64_encode($fromName) . "?= <{$fromEmail}>",
            "To: <{$recipientGmail}>",
        ];
        if ($ccEmail !== null && filter_var($ccEmail, FILTER_VALIDATE_EMAIL)) {
            $headers[] = "Cc: <{$ccEmail}>";
        }
        $headers[] = "Subject: =?UTF-8?B?" . base64_encode($subject) . "?=";
        $headers[] = "Message-ID: {$messageId}";
        $headers[] = "MIME-Version: 1.0";
        $headers[] = "Content-Type: text/html; charset=UTF-8";
        $headers[] = "Content-Transfer-Encoding: base64";

        $safeBody = chunk_split(base64_encode($htmlBody));
        $payload = implode("\r\n", $headers) . "\r\n\r\n" . $safeBody . "\r\n.";
        fwrite($socket, $payload . "\r\n");
        $expect($socket, [250]);

        $cmd($socket, 'QUIT');
        fclose($socket);
        $status = 'SENT';
    } catch (Throwable $e) {
        $status = 'FAILED';
        $errorMessage = mb_substr($e->getMessage(), 0, 250);
    }

    $stmt = $pdo->prepare(
        'INSERT INTO smtp_notification_logs
         (staff_id, recipient_gmail, event_type, subject, body_preview, smtp_message_id, delivery_status, error_message)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?)'
    );
    $stmt->execute([
        $staffId,
        $recipientGmail,
        $eventType,
        $subject,
        $plainPreview,
        $messageId,
        $status,
        $errorMessage
    ]);

    return [
        'recipient_gmail' => $recipientGmail,
        'message_id' => $messageId,
        'status' => $status,
        'subject' => $subject,
        'sent_at' => date('Y-m-d H:i:s'),
        'error_message' => $errorMessage,
    ];
}
