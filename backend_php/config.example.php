<?php
// Copy this file to config.php and fill in your cPanel MySQL & SMTP credentials.
// NEVER commit config.php with live passwords to public repositories.

return [
    'college_name' => 'Pt. T. P. Shukla College',
    'college_domain' => 'https://pttpshuklacollege.com/',
    'timezone' => 'Asia/Kolkata',
    'late_cutoff_time' => '09:45:00',

    'db' => [
        'host' => 'localhost',
        'port' => '3306',
        'name' => 'pttp_staff_attendance',
        'user' => 'YOUR_MYSQL_USERNAME',
        'pass' => 'YOUR_MYSQL_PASSWORD',
        'charset' => 'utf8mb4',
    ],

    // Configure either Gmail SMTP (smtp.gmail.com + App Password) or College Domain SMTP
    'smtp' => [
        'host' => 'smtp.gmail.com',
        'port' => 587, // 587 for STARTTLS, 465 for SSL
        'encryption' => 'tls', // 'tls' or 'ssl'
        'username' => 'notifications@pttpshuklacollege.com',
        'password' => 'YOUR_SMTP_OR_GMAIL_APP_PASSWORD',
        'from_email' => 'notifications@pttpshuklacollege.com',
        'from_name' => 'PTTP Shukla College Staff Portal',
        'hod_cc_email' => 'principal@pttpshuklacollege.com',
    ],

    'token_ttl_days' => 30,
];
