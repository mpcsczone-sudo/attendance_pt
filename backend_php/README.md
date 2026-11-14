# Pt. T. P. Shukla College — PHP / MySQL / SMTP Backend

Deploy the files inside `backend_php/` to `https://pttpshuklacollege.com/api/` (for example `public_html/api/` in cPanel).

## 1. MySQL Setup
1. Create a MySQL database (e.g. `pttp_staff_attendance`) and database user in cPanel.
2. Import `schema.sql` using phpMyAdmin or MySQL CLI.

## 2. Configure PHP & SMTP
1. Copy `config.example.php` to `config.php`.
2. Fill in your MySQL host, database name, username, and password.
3. Fill in your SMTP credentials (for Gmail SMTP, enable 2FA and generate a 16-character Google App Password, or use your college domain SMTP from cPanel Email Accounts).

## 3. Directory Layout on `pttpshuklacollege.com`
Upload the files so `https://pttpshuklacollege.com/api/health.php` is reachable:
- `public_html/api/config.php`
- `public_html/api/db.php`
- `public_html/api/smtp_mailer.php`
- `public_html/api/health.php`
- `public_html/api/auth_login.php`
- `public_html/api/auth_register.php`
- `public_html/api/auth_gmail_otp.php`
- `public_html/api/map_gmail.php`
- `public_html/api/attendance_mark.php`
- `public_html/api/attendance_list.php`
- `public_html/api/leave_manage.php`
- `public_html/api/staff_directory.php`
- `public_html/api/smtp_logs.php`

## 4. Verify
Open `https://pttpshuklacollege.com/api/health.php` or tap **Ping API** inside the **PTTP Staff** Android app.
