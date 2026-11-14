-- Pt. T. P. Shukla College Staff Attendance, Geo-Face Check-In & Time Mapping Schema
-- Compatible with MySQL 8.0+ / MariaDB 10.4+

CREATE DATABASE IF NOT EXISTS `pttp_staff_attendance`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `pttp_staff_attendance`;

CREATE TABLE IF NOT EXISTS `staff_users` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `login_id` VARCHAR(40) NOT NULL,
  `gmail_id` VARCHAR(190) NOT NULL,
  `gmail_verified` TINYINT(1) NOT NULL DEFAULT 0,
  `full_name` VARCHAR(120) NOT NULL,
  `department` VARCHAR(100) NOT NULL,
  `designation` VARCHAR(100) NOT NULL DEFAULT 'Faculty',
  `role` ENUM('STAFF', 'HOD', 'ADMIN') NOT NULL DEFAULT 'STAFF',
  `shift_start_time` TIME NOT NULL DEFAULT '09:30:00',
  `shift_end_time` TIME NOT NULL DEFAULT '16:30:00',
  `approval_status` ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
  `approved_by` INT UNSIGNED DEFAULT NULL,
  `approved_at` DATETIME DEFAULT NULL,
  `rejection_reason` VARCHAR(255) DEFAULT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `is_active` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_staff_login_id` (`login_id`),
  UNIQUE KEY `uk_staff_gmail_id` (`gmail_id`),
  KEY `idx_staff_department` (`department`),
  KEY `idx_staff_approval` (`approval_status`, `is_active`),
  CONSTRAINT `fk_staff_approver` FOREIGN KEY (`approved_by`) REFERENCES `staff_users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `api_tokens` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `staff_id` INT UNSIGNED NOT NULL,
  `token_hash` CHAR(64) NOT NULL,
  `expires_at` DATETIME NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token_hash` (`token_hash`),
  KEY `idx_token_staff` (`staff_id`),
  CONSTRAINT `fk_tokens_staff` FOREIGN KEY (`staff_id`) REFERENCES `staff_users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `otp_verifications` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `staff_id` INT UNSIGNED NOT NULL,
  `gmail_id` VARCHAR(190) NOT NULL,
  `otp_hash` VARCHAR(255) NOT NULL,
  `purpose` ENUM('LOGIN', 'GMAIL_LINK') NOT NULL DEFAULT 'LOGIN',
  `expires_at` DATETIME NOT NULL,
  `consumed_at` DATETIME DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_otp_staff_purpose` (`staff_id`, `purpose`, `expires_at`),
  CONSTRAINT `fk_otp_staff` FOREIGN KEY (`staff_id`) REFERENCES `staff_users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `attendance_records` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `staff_id` INT UNSIGNED NOT NULL,
  `attendance_date` DATE NOT NULL,
  `check_in_time` TIME DEFAULT NULL,
  `check_out_time` TIME DEFAULT NULL,
  `status` ENUM('PRESENT', 'LATE', 'ON_DUTY', 'HALF_DAY', 'ABSENT') NOT NULL DEFAULT 'PRESENT',
  `latitude` DECIMAL(10,7) DEFAULT NULL,
  `longitude` DECIMAL(10,7) DEFAULT NULL,
  `geo_accuracy_m` FLOAT DEFAULT NULL,
  `geo_address` VARCHAR(255) DEFAULT NULL,
  `face_verified` TINYINT(1) NOT NULL DEFAULT 0,
  `face_confidence` FLOAT DEFAULT NULL,
  `remarks` VARCHAR(255) NOT NULL DEFAULT '',
  `device_info` VARCHAR(160) NOT NULL DEFAULT '',
  `check_in_source` VARCHAR(40) NOT NULL DEFAULT 'ANDROID_APP',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_staff_date` (`staff_id`, `attendance_date`),
  KEY `idx_att_date` (`attendance_date`),
  CONSTRAINT `fk_att_staff` FOREIGN KEY (`staff_id`) REFERENCES `staff_users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `leave_requests` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `staff_id` INT UNSIGNED NOT NULL,
  `leave_type` ENUM('CASUAL', 'EARNED', 'MEDICAL', 'ON_DUTY', 'REGULARIZATION') NOT NULL DEFAULT 'CASUAL',
  `start_date` DATE NOT NULL,
  `end_date` DATE NOT NULL,
  `reason` TEXT NOT NULL,
  `status` ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
  `reviewed_by` INT UNSIGNED DEFAULT NULL,
  `review_note` VARCHAR(255) DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_leave_staff_status` (`staff_id`, `status`),
  CONSTRAINT `fk_leave_staff` FOREIGN KEY (`staff_id`) REFERENCES `staff_users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_leave_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `staff_users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `smtp_notification_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `staff_id` INT UNSIGNED NOT NULL,
  `recipient_gmail` VARCHAR(190) NOT NULL,
  `event_type` VARCHAR(50) NOT NULL,
  `subject` VARCHAR(255) NOT NULL,
  `body_preview` VARCHAR(300) NOT NULL DEFAULT '',
  `smtp_message_id` VARCHAR(160) NOT NULL DEFAULT '',
  `delivery_status` ENUM('SENT', 'FAILED') NOT NULL DEFAULT 'SENT',
  `error_message` VARCHAR(255) DEFAULT NULL,
  `sent_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_smtp_staff_sent` (`staff_id`, `sent_at`),
  CONSTRAINT `fk_smtp_staff` FOREIGN KEY (`staff_id`) REFERENCES `staff_users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `admin_messages` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `parent_id` BIGINT UNSIGNED DEFAULT NULL,
  `sender_staff_id` INT UNSIGNED NOT NULL,
  `target_staff_id` INT UNSIGNED DEFAULT NULL,
  `title` VARCHAR(180) NOT NULL DEFAULT '',
  `message_body` TEXT NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_msg_parent` (`parent_id`),
  KEY `idx_msg_target` (`target_staff_id`, `created_at`),
  CONSTRAINT `fk_msg_parent` FOREIGN KEY (`parent_id`) REFERENCES `admin_messages` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_msg_sender` FOREIGN KEY (`sender_staff_id`) REFERENCES `staff_users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_msg_target` FOREIGN KEY (`target_staff_id`) REFERENCES `staff_users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed initial Principal / Admin account
INSERT IGNORE INTO `staff_users`
  (`login_id`, `gmail_id`, `gmail_verified`, `full_name`, `department`, `designation`, `role`, `shift_start_time`, `shift_end_time`, `approval_status`, `approved_at`, `password_hash`, `is_active`)
VALUES
  ('PTTP-ADMIN-001', 'mpcsczone@gmail.com', 1, 'College Principal / Admin', 'Administration', 'Principal', 'ADMIN', '09:30:00', '16:30:00', 'APPROVED', NOW(), '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 1);
