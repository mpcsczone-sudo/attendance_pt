<?php
declare(strict_types=1);
require_once __DIR__ . '/../db.php';

$pdo = get_pdo();
$staff = require_auth($pdo);
$staffId = (int)$staff['id'];
$month = preg_match('/^\d{4}-\d{2}$/', (string)($_GET['month'] ?? '')) ? (string)$_GET['month'] : date('Y-m');
$today = date('Y-m-d');

$formatRow = static function (array $r): array {
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
        'remarks' => $r['remarks'],
        'check_in_source' => $r['check_in_source'],
    ];
};

$todayStmt = $pdo->prepare(
    'SELECT a.*, u.login_id, u.full_name, u.department, u.gmail_id
     FROM attendance_records a
     INNER JOIN staff_users u ON u.id = a.staff_id
     WHERE a.staff_id = ? AND a.attendance_date = ?
     LIMIT 1'
);
$todayStmt->execute([$staffId, $today]);
$todayRow = $todayStmt->fetch();

$histStmt = $pdo->prepare(
    'SELECT a.*, u.login_id, u.full_name, u.department, u.gmail_id
     FROM attendance_records a
     INNER JOIN staff_users u ON u.id = a.staff_id
     WHERE a.staff_id = ? AND DATE_FORMAT(a.attendance_date, "%Y-%m") = ?
     ORDER BY a.attendance_date DESC
     LIMIT 60'
);
$histStmt->execute([$staffId, $month]);
$rows = $histStmt->fetchAll();

$present = 0;
$late = 0;
$onDuty = 0;
$half = 0;
$absent = 0;
foreach ($rows as $r) {
    switch ($r['status']) {
        case 'PRESENT': $present++; break;
        case 'LATE': $late++; break;
        case 'ON_DUTY': $onDuty++; break;
        case 'HALF_DAY': $half++; break;
        case 'ABSENT': $absent++; break;
    }
}
$total = count($rows);
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
