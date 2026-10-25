-- Digital Complaint Priority System Database Schema (Safe Initialization)

CREATE DATABASE IF NOT EXISTS campus_complaints CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus_complaints;

-- Users Table (Safe creation without dropping existing data)
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role ENUM('STUDENT', 'ADMIN') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Complaints Table (Safe creation without dropping existing data)
CREATE TABLE IF NOT EXISTS complaints (
    id VARCHAR(36) PRIMARY KEY,
    student_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    location VARCHAR(100) NOT NULL,
    priority ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL,
    status ENUM('PENDING', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED') DEFAULT 'PENDING',
    assigned_staff VARCHAR(100) DEFAULT NULL,
    submission_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Sequence counter for human-readable complaint IDs (CMP-<number>).
-- Holds the LAST number issued. The next complaint gets last_value + 1.
CREATE TABLE IF NOT EXISTS complaint_sequence (
    id TINYINT PRIMARY KEY,
    `last_value` INT NOT NULL
) ENGINE=InnoDB;

-- Indexes for performance and sorting
-- MySQL-compatible, repeatable index creation (MySQL 8.0 does not support
-- CREATE INDEX IF NOT EXISTS). Each index is created only when absent.
SET @idx_sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'complaints' AND index_name = 'idx_complaints_student') = 0,
    'CREATE INDEX idx_complaints_student ON complaints(student_id)', 'SELECT 1');
PREPARE idx_stmt FROM @idx_sql; EXECUTE idx_stmt; DEALLOCATE PREPARE idx_stmt;
SET @idx_sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'complaints' AND index_name = 'idx_complaints_priority') = 0,
    'CREATE INDEX idx_complaints_priority ON complaints(priority)', 'SELECT 1');
PREPARE idx_stmt FROM @idx_sql; EXECUTE idx_stmt; DEALLOCATE PREPARE idx_stmt;
SET @idx_sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'complaints' AND index_name = 'idx_complaints_status') = 0,
    'CREATE INDEX idx_complaints_status ON complaints(status)', 'SELECT 1');
PREPARE idx_stmt FROM @idx_sql; EXECUTE idx_stmt; DEALLOCATE PREPARE idx_stmt;

-- Insert Sample Users if not already present (Passwords hashed using BCrypt)
-- Demo password for all three sample accounts: password
-- Change these demo credentials before any real deployment
INSERT IGNORE INTO users (id, username, password_hash, full_name, email, role) VALUES 
(1, 'admin', '$2a$10$2TxK9TjH9ff.NMBXAlLCCeCrEt9PJH4Y7oPO1gl3sGBjPbNg2oBfC', 'System Administrator', 'admin@campus.edu', 'ADMIN'),
(2, 'student1', '$2a$10$2TxK9TjH9ff.NMBXAlLCCeCrEt9PJH4Y7oPO1gl3sGBjPbNg2oBfC', 'Alice Smith', 'alice@campus.edu', 'STUDENT'),
(3, 'student2', '$2a$10$2TxK9TjH9ff.NMBXAlLCCeCrEt9PJH4Y7oPO1gl3sGBjPbNg2oBfC', 'Bob Jones', 'bob@campus.edu', 'STUDENT');

-- Refresh demo-account hashes on repeat setup so earlier placeholder hashes do not persist.
UPDATE users SET password_hash = '$2a$10$2TxK9TjH9ff.NMBXAlLCCeCrEt9PJH4Y7oPO1gl3sGBjPbNg2oBfC' WHERE username IN ('admin', 'student1', 'student2');

-- Insert Sample Complaints if not already present
INSERT IGNORE INTO complaints (id, student_id, title, category, description, location, priority, status, assigned_staff) VALUES 
('CMP-1001', 2, 'Exposed electrical wiring near podium', 'Electrical', 'Exposed live electrical wires hanging from the ceiling fan switch in room 302.', 'Academic Block 3, Room 302', 'CRITICAL', 'ASSIGNED', 'Mr. David (Electrician)'),
('CMP-1002', 2, 'Major water pipe leakage', 'Water', 'Water pipe bursting and flooding the main corridor near the cafeteria.', 'Student Center, Ground Floor', 'HIGH', 'IN_PROGRESS', 'Mr. Robert (Plumbing Lead)'),
('CMP-1003', 3, 'Broken ceiling fan making noise', 'Classroom', 'Ceiling fan is rattling loudly and wobbling dangerously during lectures.', 'Science Block, Lab 101', 'MEDIUM', 'PENDING', NULL),
('CMP-1004', 3, 'Corridor light bulb fused', 'Cleaning and Maintenance', 'Tube light in the second-floor hallway is burnt out, causing poor visibility.', 'Hostel Block A, 2nd Floor', 'LOW', 'RESOLVED', 'Maintenance Staff Team B');

-- ---------------------------------------------------------------------------
-- Sequential complaint ID counter initialization.
-- Legacy/non-CMP identifiers are migrated by ComplaintDAO under a database lock
-- and transaction, so this script never blindly renumbers existing complaint rows.
-- ---------------------------------------------------------------------------
INSERT INTO complaint_sequence (id, `last_value`)
SELECT 1, GREATEST(1004, COALESCE(MAX(CAST(SUBSTRING(id, 5) AS UNSIGNED)), 0))
FROM complaints
WHERE id REGEXP '^CMP-[0-9]+$'
ON DUPLICATE KEY UPDATE `last_value` = GREATEST(`last_value`, VALUES(`last_value`));
