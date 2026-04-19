-- ============================================================
-- METROHUB - COMPLETE DATABASE SCHEMA
-- AI Intelligence Documentation System for Indian Metros
-- ============================================================
-- Database   : MySQL 8.0+
-- Charset    : utf8mb4 (full Unicode support)
-- Engine     : InnoDB (ACID-compliant, FK support)
-- ============================================================
-- This file contains:
--   1. Database creation
--   2. All 11 table definitions
--   3. Default data inserts (departments, users, policies)
--   4. Cleanup / truncate operations (commented, run manually)
-- ============================================================

-- ============================================================
-- DATABASE CREATION
-- ============================================================

CREATE DATABASE IF NOT EXISTS metrohub_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE metrohub_db;

-- ============================================================
-- TABLE 1: DEPARTMENTS
-- ============================================================
-- Stores department/division information for metro organization.
-- Pre-populated with 10 standard metro departments.
-- ============================================================

CREATE TABLE IF NOT EXISTS departments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,
    code            VARCHAR(20)  NOT NULL UNIQUE,
    description     VARCHAR(500),
    head_name       VARCHAR(100),
    contact_email   VARCHAR(150),
    is_active       BOOLEAN DEFAULT TRUE,
    display_order   INT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_dept_code   (code),
    INDEX idx_dept_active (is_active)
) ENGINE=InnoDB;

-- ============================================================
-- TABLE 2: USERS
-- ============================================================
-- User accounts with JWT authentication and RBAC.
-- Four roles: SUPER_ADMIN, DEPARTMENT_ADMIN,
--             DEPARTMENT_UPLOAD_ADMIN, DEPARTMENT_USER
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    email           VARCHAR(150) NOT NULL UNIQUE,
    phone_number    VARCHAR(20),
    password        VARCHAR(255) NOT NULL,
    employee_id     VARCHAR(50) UNIQUE,
    department      VARCHAR(100),
    department_id   BIGINT,
    role            ENUM(
                        'SUPER_ADMIN',
                        'DEPARTMENT_ADMIN',
                        'DEPARTMENT_UPLOAD_ADMIN',
                        'DEPARTMENT_USER'
                    ) DEFAULT 'DEPARTMENT_USER',
    is_active       BOOLEAN DEFAULT TRUE,
    last_login      TIMESTAMP NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_user_email (email),
    INDEX idx_user_role  (role),
    INDEX idx_user_dept  (department_id),

    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ============================================================
-- TABLE 3: DOCUMENTS
-- ============================================================
-- Core table storing uploaded document information.
-- Supports classification, text extraction, legal hold,
-- priority-based SLA, and full-text search.
-- ============================================================

CREATE TABLE IF NOT EXISTS documents (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- File Information
    file_name                   VARCHAR(255) NOT NULL,
    stored_file_name            VARCHAR(255) NOT NULL,
    file_path                   VARCHAR(500) NOT NULL,
    file_type                   VARCHAR(100),
    file_size                   BIGINT,
    file_extension              VARCHAR(10),

    -- Classification
    document_type               ENUM(
                                    'JOB_CARD','INVOICE','POLICY',
                                    'SAFETY_CIRCULAR','LEGAL_NOTICE','CONTRACT',
                                    'MANUAL','REPORT','MEMO','CERTIFICATE','OTHER'
                                ),
    priority                    ENUM('HIGH','MEDIUM','LOW') DEFAULT 'MEDIUM',
    department_id               BIGINT,
    classification_confidence   DECIMAL(5,4),
    is_manually_classified      BOOLEAN DEFAULT FALSE,

    -- Text Extraction
    extracted_text              LONGTEXT,
    extraction_method           VARCHAR(50),
    is_text_extracted           BOOLEAN DEFAULT FALSE,
    ocr_language                VARCHAR(20),

    -- Upload Info
    uploaded_by                 BIGINT NOT NULL,
    upload_date                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Status & Metadata
    status                      ENUM('ACTIVE','ARCHIVED','PENDING_REVIEW','DELETED') DEFAULT 'ACTIVE',
    is_archived                 BOOLEAN DEFAULT FALSE,
    description                 VARCHAR(500),
    tags                        VARCHAR(500),

    -- Legal Hold (Phase 9)
    legal_hold                  BOOLEAN DEFAULT FALSE,
    legal_hold_reason           VARCHAR(500),
    legal_hold_by               BIGINT,
    legal_hold_date             TIMESTAMP NULL,

    -- Indexes
    INDEX idx_doc_type        (document_type),
    INDEX idx_doc_priority    (priority),
    INDEX idx_doc_dept        (department_id),
    INDEX idx_doc_status      (status),
    INDEX idx_doc_upload_date (upload_date),
    INDEX idx_doc_uploaded_by (uploaded_by),
    INDEX idx_doc_legal_hold  (legal_hold),
    FULLTEXT INDEX idx_doc_fulltext (file_name, extracted_text, tags),

    FOREIGN KEY (department_id) REFERENCES departments(id)  ON DELETE SET NULL,
    FOREIGN KEY (uploaded_by)   REFERENCES users(id)        ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ============================================================
-- TABLE 4: DOCUMENT_METADATA
-- ============================================================
-- Extended metadata (1:1 with documents).
-- Equipment info, vendor details, dates, references, AI summary.
-- ============================================================

CREATE TABLE IF NOT EXISTS document_metadata (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id         BIGINT NOT NULL UNIQUE,

    -- Equipment Related
    equipment_id        VARCHAR(100),
    equipment_name      VARCHAR(200),
    equipment_location  VARCHAR(200),
    serial_number       VARCHAR(100),

    -- Vendor Related
    vendor_name         VARCHAR(200),
    vendor_code         VARCHAR(50),
    po_number           VARCHAR(100),
    invoice_number      VARCHAR(100),
    invoice_amount      DECIMAL(15,2),
    currency            VARCHAR(10) DEFAULT 'INR',

    -- Date Fields
    document_date       DATE,
    effective_date      DATE,
    expiry_date         DATE,
    deadline            DATE,

    -- Reference Numbers
    reference_number    VARCHAR(100),
    file_number         VARCHAR(100),
    circular_number     VARCHAR(100),
    case_number         VARCHAR(100),

    -- People
    author_name         VARCHAR(100),
    approver_name       VARCHAR(100),
    recipient_name      VARCHAR(100),

    -- AI-Generated
    subject             VARCHAR(500),
    summary             TEXT,
    keywords            TEXT,
    additional_info     JSON,

    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_meta_equipment (equipment_id),
    INDEX idx_meta_vendor    (vendor_name),
    INDEX idx_meta_deadline  (deadline),

    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- TABLE 5: AUDIT_LOGS
-- ============================================================
-- Immutable action log for security and compliance auditing.
-- Tracks every user action across the system.
-- ============================================================

CREATE TABLE IF NOT EXISTS audit_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT,
    user_email      VARCHAR(150),

    action          ENUM(
                        'LOGIN','LOGOUT','LOGIN_FAILED','PASSWORD_CHANGED',
                        'DOCUMENT_UPLOADED','DOCUMENT_VIEWED','DOCUMENT_DOWNLOADED',
                        'DOCUMENT_DELETED','DOCUMENT_UPDATED','DOCUMENT_ARCHIVED',
                        'SEARCH_PERFORMED',
                        'USER_CREATED','USER_UPDATED','USER_DELETED','USER_ROLE_CHANGED',
                        'SYSTEM_ERROR','CONFIGURATION_CHANGED'
                    ) NOT NULL,

    entity_type     VARCHAR(50),
    entity_id       BIGINT,
    entity_name     VARCHAR(255),

    details         JSON,
    ip_address      VARCHAR(50),
    user_agent      VARCHAR(500),

    status          ENUM('SUCCESS','FAILURE','WARNING') DEFAULT 'SUCCESS',
    error_message   VARCHAR(500),

    timestamp       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_audit_user      (user_id),
    INDEX idx_audit_action    (action),
    INDEX idx_audit_timestamp (timestamp),
    INDEX idx_audit_entity    (entity_type, entity_id),

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ============================================================
-- TABLE 6: ALERTS
-- ============================================================
-- System notifications / alerts for documents.
-- Supports multi-channel delivery: Dashboard, Email, SMS.
-- Used by the compliance scheduler for escalation alerts.
-- ============================================================

CREATE TABLE IF NOT EXISTS alerts (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id             BIGINT,
    user_id                 BIGINT,
    department_id           BIGINT,

    alert_type              ENUM(
                                'HIGH_PRIORITY_UPLOAD',
                                'DEADLINE_APPROACHING',
                                'DEADLINE_OVERDUE',
                                'DOCUMENT_PENDING_REVIEW',
                                'DEADLINE_TODAY',
                                'NEW_DOCUMENT_UPLOADED',
                                'ACKNOWLEDGEMENT_REQUIRED',
                                'COMPLIANCE_REMINDER',
                                'ESCALATION_DEPT_ADMIN',
                                'ESCALATION_SUPER_ADMIN',
                                'COMPLIANCE_VIOLATION_CREATED'
                            ) NOT NULL,

    notification_channel    ENUM('DASHBOARD','EMAIL','SMS') DEFAULT 'DASHBOARD',
    message                 TEXT,
    is_read                 BOOLEAN DEFAULT FALSE,
    email_sent              BOOLEAN DEFAULT FALSE,
    sms_sent                BOOLEAN DEFAULT FALSE,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_alert_document   (document_id),
    INDEX idx_alert_user       (user_id),
    INDEX idx_alert_department (department_id),
    INDEX idx_alert_type       (alert_type),
    INDEX idx_alert_channel    (notification_channel),
    INDEX idx_alert_read       (is_read),
    INDEX idx_alert_created    (created_at),
    INDEX idx_alert_unread     (is_read, created_at),

    FOREIGN KEY (document_id)   REFERENCES documents(id)   ON DELETE CASCADE,
    FOREIGN KEY (user_id)       REFERENCES users(id)       ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ============================================================
-- TABLE 7: DOCUMENT_ACKNOWLEDGEMENTS
-- ============================================================
-- Tracks which users have acknowledged which documents.
-- Each user can acknowledge a document only once (UNIQUE key).
-- ============================================================

CREATE TABLE IF NOT EXISTS document_acknowledgements (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id     BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    acknowledged_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address      VARCHAR(50),
    notes           VARCHAR(500),

    UNIQUE KEY uk_doc_user_ack (document_id, user_id),

    INDEX idx_ack_document  (document_id),
    INDEX idx_ack_user      (user_id),
    INDEX idx_ack_timestamp (acknowledged_at),

    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)     REFERENCES users(id)     ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- TABLE 8: COMPLIANCE_VIOLATIONS
-- ============================================================
-- Permanent records of compliance failures.
-- Created when a user fails to acknowledge a document
-- within the SLA-defined violation_hours.
-- Tracks escalation steps and late acknowledgements.
-- ============================================================

CREATE TABLE IF NOT EXISTS compliance_violations (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id                 BIGINT NOT NULL,
    user_id                     BIGINT NOT NULL,
    department_id               BIGINT NOT NULL,

    -- Violation Details
    violation_type              ENUM('ACK_DELAY') DEFAULT 'ACK_DELAY',
    violation_date              TIMESTAMP NOT NULL,
    days_delayed                INT NOT NULL,

    -- Resolution
    resolved                    BOOLEAN DEFAULT FALSE,
    resolved_by                 BIGINT,
    resolved_date               TIMESTAMP NULL,
    remarks                     VARCHAR(1000),

    -- Late Acknowledgement
    acknowledged_late           BOOLEAN DEFAULT FALSE,
    late_acknowledgement_date   TIMESTAMP NULL,

    -- Escalation Tracking
    reminder_sent               BOOLEAN DEFAULT FALSE,
    reminder_sent_at            TIMESTAMP NULL,
    dept_admin_escalated        BOOLEAN DEFAULT FALSE,
    dept_admin_escalated_at     TIMESTAMP NULL,
    super_admin_escalated       BOOLEAN DEFAULT FALSE,
    super_admin_escalated_at    TIMESTAMP NULL,

    -- Policy Reference (Phase 9)
    policy_rule_id              BIGINT,
    sla_hours_applied           INT,

    -- Audit
    created_at                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_violation_doc_user (document_id, user_id),

    INDEX idx_violation_document   (document_id),
    INDEX idx_violation_user       (user_id),
    INDEX idx_violation_department (department_id),
    INDEX idx_violation_resolved   (resolved),
    INDEX idx_violation_date       (violation_date),
    INDEX idx_violation_type       (violation_type),
    INDEX idx_violation_created    (created_at),
    INDEX idx_violation_policy     (policy_rule_id),

    FOREIGN KEY (document_id)   REFERENCES documents(id)   ON DELETE CASCADE,
    FOREIGN KEY (user_id)       REFERENCES users(id)       ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE,
    FOREIGN KEY (resolved_by)   REFERENCES users(id)       ON DELETE SET NULL
) ENGINE=InnoDB;

-- ============================================================
-- TABLE 9: POLICY_RULES
-- ============================================================
-- Admin-configurable SLA rules for compliance enforcement.
-- Each rule defines escalation timelines per department/priority.
-- Only one rule can be default; rules matched by dept + priority.
-- ============================================================

CREATE TABLE IF NOT EXISTS policy_rules (
    id                              BIGINT AUTO_INCREMENT PRIMARY KEY,
    department_id                   BIGINT,
    priority                        ENUM('HIGH','MEDIUM','LOW'),

    -- SLA Timings (in hours)
    reminder_hours                  INT NOT NULL DEFAULT 24,
    dept_admin_escalation_hours     INT NOT NULL DEFAULT 48,
    super_admin_escalation_hours    INT NOT NULL DEFAULT 72,
    violation_hours                 INT NOT NULL DEFAULT 168,

    -- Notification Channels
    email_enabled                   BOOLEAN DEFAULT TRUE,
    sms_enabled                     BOOLEAN DEFAULT FALSE,
    dashboard_enabled               BOOLEAN DEFAULT TRUE,

    -- Rule Config
    name                            VARCHAR(100) NOT NULL,
    description                     VARCHAR(500),
    is_active                       BOOLEAN DEFAULT TRUE,
    is_default                      BOOLEAN DEFAULT FALSE,

    -- Audit
    created_by                      BIGINT,
    updated_by                      BIGINT,
    created_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_policy_dept_priority (department_id, priority),

    INDEX idx_policy_department (department_id),
    INDEX idx_policy_priority   (priority),
    INDEX idx_policy_active     (is_active),
    INDEX idx_policy_default    (is_default),

    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by)    REFERENCES users(id)       ON DELETE SET NULL,
    FOREIGN KEY (updated_by)    REFERENCES users(id)       ON DELETE SET NULL
) ENGINE=InnoDB;

-- ============================================================
-- TABLE 10: RISK_SCORE_SNAPSHOTS
-- ============================================================
-- Historical risk score snapshots for audit trail.
-- Immutable once created. Scores range 0-100 with levels.
-- ============================================================

CREATE TABLE IF NOT EXISTS risk_score_snapshots (
    id                              BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- Entity Reference
    entity_type                     ENUM('USER','DEPARTMENT') NOT NULL,
    entity_id                       BIGINT NOT NULL,
    entity_name                     VARCHAR(200),

    -- Risk Score
    risk_score                      INT NOT NULL,
    risk_level                      ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL,

    -- Risk Factors
    late_acknowledgement_count      INT DEFAULT 0,
    violation_count                 INT DEFAULT 0,
    pending_violation_count         INT DEFAULT 0,
    dept_admin_escalation_count     INT DEFAULT 0,
    super_admin_escalation_count    INT DEFAULT 0,
    legal_hold_count                INT DEFAULT 0,
    safety_violation_count          INT DEFAULT 0,
    repeat_offense_count            INT DEFAULT 0,

    -- Metadata
    calculation_period_start        TIMESTAMP NULL,
    calculation_period_end          TIMESTAMP NULL,
    created_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    calculation_notes               VARCHAR(500),

    INDEX idx_risk_entity      (entity_type, entity_id),
    INDEX idx_risk_level       (risk_level),
    INDEX idx_risk_score       (risk_score),
    INDEX idx_risk_created     (created_at),
    INDEX idx_risk_entity_date (entity_type, entity_id, created_at)
) ENGINE=InnoDB;

-- ============================================================
-- TABLE 11: DOCUMENT_REMINDERS
-- ============================================================
-- Custom reminders for specific documents.
-- Supports one-time and recurring reminders.
-- ============================================================

CREATE TABLE IF NOT EXISTS document_reminders (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id             BIGINT NOT NULL,
    target_user_id          BIGINT,
    reminder_date           TIMESTAMP NOT NULL,
    message                 VARCHAR(500),
    reminder_type           ENUM('ACKNOWLEDGEMENT','DEADLINE','REVIEW','CUSTOM') DEFAULT 'ACKNOWLEDGEMENT',
    is_sent                 BOOLEAN DEFAULT FALSE,
    sent_at                 TIMESTAMP NULL,
    is_recurring            BOOLEAN DEFAULT FALSE,
    recurrence_hours        INT,
    max_occurrences         INT,
    occurrence_count        INT DEFAULT 0,
    created_by              BIGINT,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active               BOOLEAN DEFAULT TRUE,

    INDEX idx_reminder_document (document_id),
    INDEX idx_reminder_user     (target_user_id),
    INDEX idx_reminder_date     (reminder_date),
    INDEX idx_reminder_pending  (is_active, is_sent, reminder_date),

    FOREIGN KEY (document_id)    REFERENCES documents(id) ON DELETE CASCADE,
    FOREIGN KEY (target_user_id) REFERENCES users(id)     ON DELETE CASCADE,
    FOREIGN KEY (created_by)     REFERENCES users(id)     ON DELETE SET NULL
) ENGINE=InnoDB;


-- ============================================================
-- ============================================================
--                    DEFAULT DATA INSERTS
-- ============================================================
-- ============================================================


-- ============================================================
-- DEFAULT DEPARTMENTS (10 departments)
-- ============================================================

INSERT INTO departments (name, code, description, display_order) VALUES
    ('Maintenance',        'MAINT', 'Escalator, elevator, and equipment maintenance',  1),
    ('Safety & Quality',   'SAFE',  'Safety compliance and quality assurance',          2),
    ('Human Resources',    'HR',    'Employee management and HR policies',              3),
    ('Finance & Accounts', 'FIN',   'Financial management and accounting',              4),
    ('Legal & Compliance', 'LEGAL', 'Legal affairs and regulatory compliance',          5),
    ('Engineering',        'ENG',   'Civil, electrical, and systems engineering',       6),
    ('Operations',         'OPS',   'Daily metro operations and scheduling',            7),
    ('IT & Systems',       'IT',    'Information technology and software systems',      8),
    ('Procurement',        'PROC',  'Vendor management and purchasing',                 9),
    ('Administration',     'ADMIN', 'General administration and support',              10)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ============================================================
-- DEFAULT USERS
-- ============================================================

-- Super Admin (Password: Admin@123)
INSERT INTO users (name, email, password, employee_id, role, is_active) VALUES
    ('System Admin', 'admin@metrohub.in',
     '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqKzGb/1Z9TjMPjIYA8nExBBqbQ2e',
     'ADMIN-001', 'SUPER_ADMIN', TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Department Upload Admins (Password: Upload@123)
INSERT INTO users (name, email, phone_number, password, employee_id, role, department_id, is_active) VALUES
    ('Maintenance Supervisor', 'maint.admin@metrohub.in',   '+919876543210',
     '$2a$10$eKlsJYU/dVhYo1vk7tFnBeO5V2qNrJUq8J8wvfQvQ.H8hYCq3UKCO',
     'MAINT-001', 'DEPARTMENT_UPLOAD_ADMIN', 1, TRUE),
    ('Safety Officer',         'safety.admin@metrohub.in',  '+919876543211',
     '$2a$10$eKlsJYU/dVhYo1vk7tFnBeO5V2qNrJUq8J8wvfQvQ.H8hYCq3UKCO',
     'SAFE-001',  'DEPARTMENT_UPLOAD_ADMIN', 2, TRUE),
    ('HR Executive',           'hr.admin@metrohub.in',      '+919876543212',
     '$2a$10$eKlsJYU/dVhYo1vk7tFnBeO5V2qNrJUq8J8wvfQvQ.H8hYCq3UKCO',
     'HR-001',    'DEPARTMENT_UPLOAD_ADMIN', 3, TRUE),
    ('Accounts Officer',       'finance.admin@metrohub.in', '+919876543213',
     '$2a$10$eKlsJYU/dVhYo1vk7tFnBeO5V2qNrJUq8J8wvfQvQ.H8hYCq3UKCO',
     'FIN-001',   'DEPARTMENT_UPLOAD_ADMIN', 4, TRUE),
    ('Legal Officer',          'legal.admin@metrohub.in',   '+919876543214',
     '$2a$10$eKlsJYU/dVhYo1vk7tFnBeO5V2qNrJUq8J8wvfQvQ.H8hYCq3UKCO',
     'LEGAL-001', 'DEPARTMENT_UPLOAD_ADMIN', 5, TRUE),
    ('Operations Manager',     'ops.admin@metrohub.in',     '+919876543215',
     '$2a$10$eKlsJYU/dVhYo1vk7tFnBeO5V2qNrJUq8J8wvfQvQ.H8hYCq3UKCO',
     'OPS-001',   'DEPARTMENT_UPLOAD_ADMIN', 7, TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Department Admins (Password: Manager@123)
INSERT INTO users (name, email, phone_number, password, employee_id, role, department_id, is_active) VALUES
    ('Maintenance Manager', 'maint.manager@metrohub.in',  '+919876543220',
     '$2a$10$3JxWmT7K2QvF.Q5j8dFzUOvWz1YLN4Xq1BML7uKQ.t6xKzV1XVHOK',
     'MAINT-MGR-001', 'DEPARTMENT_ADMIN', 1, TRUE),
    ('Safety Manager',      'safety.manager@metrohub.in', '+919876543221',
     '$2a$10$3JxWmT7K2QvF.Q5j8dFzUOvWz1YLN4Xq1BML7uKQ.t6xKzV1XVHOK',
     'SAFE-MGR-001',  'DEPARTMENT_ADMIN', 2, TRUE),
    ('HR Manager',          'hr.manager@metrohub.in',     '+919876543222',
     '$2a$10$3JxWmT7K2QvF.Q5j8dFzUOvWz1YLN4Xq1BML7uKQ.t6xKzV1XVHOK',
     'HR-MGR-001',    'DEPARTMENT_ADMIN', 3, TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Department Users (Password: User@123)
INSERT INTO users (name, email, phone_number, password, employee_id, role, department_id, is_active) VALUES
    ('Maint Technician 1',    'maint.tech1@metrohub.in',      '+919876543230',
     '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
     'MAINT-TECH-001', 'DEPARTMENT_USER', 1, TRUE),
    ('Maint Technician 2',    'maint.tech2@metrohub.in',      '+919876543231',
     '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
     'MAINT-TECH-002', 'DEPARTMENT_USER', 1, TRUE),
    ('Safety Inspector',      'safety.inspector@metrohub.in', '+919876543232',
     '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
     'SAFE-INSP-001',  'DEPARTMENT_USER', 2, TRUE),
    ('Shift In-charge',       'safety.shift@metrohub.in',     '+919876543233',
     '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
     'SAFE-SHIFT-001', 'DEPARTMENT_USER', 2, TRUE),
    ('HR Assistant',          'hr.assistant@metrohub.in',     '+919876543234',
     '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
     'HR-ASST-001',    'DEPARTMENT_USER', 3, TRUE),
    ('Finance Auditor',       'finance.auditor@metrohub.in',  '+919876543235',
     '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
     'FIN-AUD-001',    'DEPARTMENT_USER', 4, TRUE),
    ('Operations Controller', 'ops.controller@metrohub.in',   '+919876543236',
     '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
     'OPS-CTRL-001',   'DEPARTMENT_USER', 7, TRUE),
    ('Station Manager',       'ops.station@metrohub.in',      '+919876543237',
     '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
     'OPS-STN-001',    'DEPARTMENT_USER', 7, TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ============================================================
-- DEFAULT POLICY RULES (SLA Configuration)
-- ============================================================

-- Global Default Policy (applies when no dept/priority match)
INSERT INTO policy_rules (name, description, department_id, priority,
    reminder_hours, dept_admin_escalation_hours, super_admin_escalation_hours,
    violation_hours, email_enabled, sms_enabled, dashboard_enabled,
    is_active, is_default) VALUES
    ('Global Default',
     'Default compliance policy for all departments',
     NULL, NULL, 24, 48, 72, 168, TRUE, FALSE, TRUE, TRUE, TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Safety Department - HIGH Priority (fastest escalation)
INSERT INTO policy_rules (name, description, department_id, priority,
    reminder_hours, dept_admin_escalation_hours, super_admin_escalation_hours,
    violation_hours, email_enabled, sms_enabled, dashboard_enabled,
    is_active, is_default) VALUES
    ('Safety HIGH Priority',
     'Urgent safety documents - fastest escalation',
     2, 'HIGH', 6, 12, 24, 48, TRUE, TRUE, TRUE, TRUE, FALSE)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Safety Department - MEDIUM Priority
INSERT INTO policy_rules (name, description, department_id, priority,
    reminder_hours, dept_admin_escalation_hours, super_admin_escalation_hours,
    violation_hours, email_enabled, sms_enabled, dashboard_enabled,
    is_active, is_default) VALUES
    ('Safety MEDIUM Priority',
     'Standard safety documents',
     2, 'MEDIUM', 24, 48, 72, 168, TRUE, TRUE, TRUE, TRUE, FALSE)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Operations Department - HIGH Priority
INSERT INTO policy_rules (name, description, department_id, priority,
    reminder_hours, dept_admin_escalation_hours, super_admin_escalation_hours,
    violation_hours, email_enabled, sms_enabled, dashboard_enabled,
    is_active, is_default) VALUES
    ('Operations HIGH Priority',
     'Critical operations documents',
     7, 'HIGH', 12, 24, 48, 96, TRUE, TRUE, TRUE, TRUE, FALSE)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- HR Department - LOW Priority (relaxed)
INSERT INTO policy_rules (name, description, department_id, priority,
    reminder_hours, dept_admin_escalation_hours, super_admin_escalation_hours,
    violation_hours, email_enabled, sms_enabled, dashboard_enabled,
    is_active, is_default) VALUES
    ('HR LOW Priority',
     'Low priority HR documents - relaxed timeline',
     3, 'LOW', 48, 0, 0, 0, FALSE, FALSE, TRUE, TRUE, FALSE)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Finance Department - MEDIUM Priority
INSERT INTO policy_rules (name, description, department_id, priority,
    reminder_hours, dept_admin_escalation_hours, super_admin_escalation_hours,
    violation_hours, email_enabled, sms_enabled, dashboard_enabled,
    is_active, is_default) VALUES
    ('Finance MEDIUM Priority',
     'Standard finance documents',
     4, 'MEDIUM', 24, 48, 72, 168, TRUE, FALSE, TRUE, TRUE, FALSE)
ON DUPLICATE KEY UPDATE name = VALUES(name);


-- ============================================================
-- ============================================================
--           CLEANUP / TRUNCATE / DELETE OPERATIONS
-- ============================================================
-- ============================================================
-- WARNING: These operations DELETE ALL DATA from the tables.
-- Uncomment and run ONLY when you need to reset the database.
-- Execute in this exact order to respect foreign key constraints.
-- ============================================================

-- TRUNCATE ALL TABLES (order respects FK dependencies)
-- SET FOREIGN_KEY_CHECKS = 0;
-- TRUNCATE TABLE risk_score_snapshots;
-- TRUNCATE TABLE document_reminders;
-- TRUNCATE TABLE compliance_violations;
-- TRUNCATE TABLE document_acknowledgements;
-- TRUNCATE TABLE alerts;
-- TRUNCATE TABLE audit_logs;
-- TRUNCATE TABLE document_metadata;
-- TRUNCATE TABLE policy_rules;
-- TRUNCATE TABLE documents;
-- TRUNCATE TABLE users;
-- TRUNCATE TABLE departments;
-- SET FOREIGN_KEY_CHECKS = 1;

-- DELETE ALL DATA (alternative - resets auto-increment)
-- SET FOREIGN_KEY_CHECKS = 0;
-- DELETE FROM risk_score_snapshots;
-- DELETE FROM document_reminders;
-- DELETE FROM compliance_violations;
-- DELETE FROM document_acknowledgements;
-- DELETE FROM alerts;
-- DELETE FROM audit_logs;
-- DELETE FROM document_metadata;
-- DELETE FROM policy_rules;
-- DELETE FROM documents;
-- DELETE FROM users;
-- DELETE FROM departments;
-- SET FOREIGN_KEY_CHECKS = 1;

-- DROP ALL TABLES (complete reset)
-- SET FOREIGN_KEY_CHECKS = 0;
-- DROP TABLE IF EXISTS risk_score_snapshots;
-- DROP TABLE IF EXISTS document_reminders;
-- DROP TABLE IF EXISTS compliance_violations;
-- DROP TABLE IF EXISTS document_acknowledgements;
-- DROP TABLE IF EXISTS alerts;
-- DROP TABLE IF EXISTS audit_logs;
-- DROP TABLE IF EXISTS document_metadata;
-- DROP TABLE IF EXISTS policy_rules;
-- DROP TABLE IF EXISTS documents;
-- DROP TABLE IF EXISTS users;
-- DROP TABLE IF EXISTS departments;
-- SET FOREIGN_KEY_CHECKS = 1;

-- DROP DATABASE (nuclear option)
-- DROP DATABASE IF EXISTS metrohub_db;

-- ============================================================
-- END OF SCHEMA
-- ============================================================
