<?php
declare(strict_types=1);
require_once __DIR__ . '/../db.php';

$pdo = get_pdo();
$staff = require_auth($pdo);
$actorId = (int)$staff['id'];
$requestedStaffId = isset($_GET['target_staff_id']) && is_numeric($_GET['target_staff_id']) ? (int)$_GET['target_staff_id'] : 0;
$requestedLoginId = strtoupper(trim((string)($_GET['target_login_id'] ?? '')));

$targetStaff = $staff;
$staffId = $actorId;
if (($requestedStaffId > 0 && $requestedStaffId !== $actorId) || ($requestedLoginId !== '' && strtoupper((string)$staff['login_id']) !== $requestedLoginId)) {
    if (strtoupper((string)$staff['role']) !== 'ADMIN') {
        json_error('Only Admin can view attendance for other staff members.', 403);
    }
    if ($requestedStaffId > 0) {
        $tStmt = $pdo->prepare('SELECT * FROM staff_users WHERE id = ? LIMIT 1');
        $tStmt->execute([$requestedStaffId]);
        $found = $tStmt->fetch();
    } else {
        $found = false;
    }
    if (!$found && $requestedLoginId !== '') {
        $tStmt = $pdo->prepare('SELECT * FROM staff_users WHERE UPPER(login_id) = ? LIMIT 1');
        $tStmt->execute([$requestedLoginId]);
        $found = $tStmt->fetch();
    }
    if (!$found) {
        json_error('Selected staff user not found.', 404);
    }
    $targetStaff = $found;
    $staffId = (int)$found['id'];
}

$month = preg_match('/^\d{4}-\d{2}$/', (string)($_GET['month'] ?? '')) ? (string)$_GET['month'] : date('Y-m');
$today = date('Y-m-d');

$formatRow = static function (array $r): array {
    $lat = array_key_exists('latitude', $r) && $r['latitude'] !== null ? (float)$r['latitude'] : null;
    $lng = array_key_exists('longitude', $r) && $r['longitude'] !== null ? (float)$r['longitude'] : null;
    $geoAddr = array_key_exists('geo_address', $r) ? $r['geo_address'] : null;
    $faceOk = array_key_exists('face_verified', $r) ? (bool)$r['face_verified'] : false;
    return [
        'id' => (int)$r['id'],
        'staff_id' => (int)$r['staff_id'],
        'login_id' => $r['login_id'],
        'full_name' => $r['full_name'],
        'department' => $r['department'],
        'gmail_id' => $r['gmail_id'],
        'attendance_date' => $r['attendance_date'],
        'check_in_time' => $r['check_in_time'],
        'check_out_time' => $r['check_out_time'],
        'status' => $r['status'],
        'latitude' => $lat,
        'longitude' => $lng,
        'geo_address' => $geoAddr,
        'face_verified' => $faceOk,
        'remarks' => $r['remarks'] ?? '',
        'check_in_source' => $r['check_in_source'] ?? 'ANDROID_APP',
    ];
};

$todayStmt = $pdo->prepare(
    'SELECT a.*, u.login_id, u.full_name, u.department, u.gmail_id
     FROM attendance_records a
     INNER JOIN staff_users u ON u.id = a.staff_id
     WHERE a.staff_id = ? AND a.attendance_date = ?
     LIMIT 1'
);
$todayStmt->execute([$actorId, $today]);
$todayRow = $todayStmt->fetch();

$monthStart = $month . '-01';
$monthEnd = date('Y-m-d', strtotime($monthStart . ' +1 month'));
$histStmt = $pdo->prepare(
    'SELECT a.*, u.login_id, u.full_name, u.department, u.gmail_id
     FROM attendance_records a
     INNER JOIN staff_users u ON u.id = a.staff_id
     WHERE a.staff_id = ? AND a.attendance_date >= ? AND a.attendance_date < ?
     ORDER BY a.attendance_date DESC
     LIMIT 60'
);
$histStmt->execute([$staffId, $monthStart, $monthEnd]);
$rows = $histStmt->fetchAll();

// Authoritative SQL aggregate for Monthly Summary
$aggStmt = $pdo->prepare(
    "SELECT
        COUNT(*) AS total_days,
        SUM(CASE WHEN UPPER(status) = 'PRESENT' THEN 1 ELSE 0 END) AS present_days,
        SUM(CASE WHEN UPPER(status) = 'LATE' THEN 1 ELSE 0 END) AS late_days,
        SUM(CASE WHEN UPPER(status) = 'ON_DUTY' THEN 1 ELSE 0 END) AS on_duty_days,
        SUM(CASE WHEN UPPER(status) = 'HALF_DAY' THEN 1 ELSE 0 END) AS half_days,
        SUM(CASE WHEN UPPER(status) = 'ABSENT' THEN 1 ELSE 0 END) AS absent_days
     FROM attendance_records
     WHERE staff_id = ? AND attendance_date >= ? AND attendance_date < ?"
);
$aggStmt->execute([$staffId, $monthStart, $monthEnd]);
$agg = $aggStmt->fetch() ?: [];

$total = (int)($agg['total_days'] ?? 0);
$present = (int)($agg['present_days'] ?? 0);
$late = (int)($agg['late_days'] ?? 0);
$onDuty = (int)($agg['on_duty_days'] ?? 0);
$half = (int)($agg['half_days'] ?? 0);
$absent = (int)($agg['absent_days'] ?? 0);
$effective = $present + $late + $onDuty + ($half * 0.5);
$pct = $total > 0 ? round(($effective / $total) * 100.0, 1) : 0.0;

$deptRegister = [];
if (in_array($staff['role'], ['HOD', 'ADMIN'], true)) {
    if ($staff['role'] === 'ADMIN') {
        $regStmt = $pdo->prepare(
            'SELECT a.*, u.login_id, u.full_name, u.department, u.gmail_id
             FROM attendance_records a
             INNER JOIN staff_users u ON u.id = a.staff_id
             WHERE a.attendance_date = ?
             ORDER BY a.check_in_time ASC'
        );
        $regStmt->execute([$today]);
    } else {
        $regStmt = $pdo->prepare(
            'SELECT a.*, u.login_id, u.full_name, u.department, u.gmail_id
             FROM attendance_records a
             INNER JOIN staff_users u ON u.id = a.staff_id
             WHERE a.attendance_date = ? AND u.department = ?
             ORDER BY a.check_in_time ASC'
        );
        $regStmt->execute([$today, $staff['department']]);
    }
    $deptRegister = array_map($formatRow, $regStmt->fetchAll());
}

json_success([
    'today' => $todayRow ? $formatRow($todayRow) : null,
    'staff' => format_staff_profile($staff),
    'target_staff' => format_staff_profile($targetStaff),
    'summary' => [
        'month' => $month,
        'total_days' => $total,
        'present_days' => $present,
        'late_days' => $late,
        'on_duty_days' => $onDuty,
        'half_days' => $half,
        'absent_days' => $absent,
        'attendance_percentage' => $pct,
    ],
    'records' => array_map($formatRow, $rows),
    'department_register' => $deptRegister,
]);
