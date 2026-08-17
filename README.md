<p align="center">
  <h1 align="center">🚇 MetroHub</h1>
  <p align="center"><strong>AI Intelligence Documentation System for Indian Metros</strong></p>
  <p align="center">
    A comprehensive, enterprise-grade document management platform built for Indian metropolitan transit authorities.<br/>
    Features AI-powered text extraction, automated SLA compliance enforcement, role-based access control,<br/>
    multi-channel notifications, risk assessment analytics, and full audit trail capabilities.
  </p>
</p>

---

## Overview

**MetroHub** is an AI-powered documentation management system designed specifically for Indian metropolitan transit authorities (e.g., DMRC, BMRC, CMRL, NMRC). It digitizes the entire document lifecycle — from upload and intelligent classification to acknowledgement tracking, SLA compliance enforcement, and risk-based analytics.

The system addresses critical challenges faced by metro organizations:
- **Paper-based document chaos** → Centralized digital repository with AI classification
- **Lost acknowledgement trails** → Tracked, auditable acknowledgement system
- **SLA compliance gaps** → Automated escalation engine with configurable policies
- **No visibility into compliance risk** → Real-time risk scoring and analytics dashboards
- **Siloed department data** → Role-based views with cross-department oversight for admins

---

## Key Features

| Category | Features |
|----------|----------|
| **Document Management** | Upload (PDF/Word/Images), AI classification (11 types), full-text search, legal hold, archive/delete |
| **Text Extraction** | Apache Tika for PDFs/Word, Tesseract OCR for scanned images, NLP-powered metadata extraction |
| **SLA Compliance** | Configurable per department/priority, automated 4-stage escalation, violation creation |
| **Acknowledgements** | Per-user tracking, department-wide visibility, acknowledgement deadlines |
| **Notifications** | 3-channel delivery (Dashboard, Email, SMS), priority-based routing |
| **Analytics** | Real-time dashboards, risk scoring (0-100), department performance metrics |
| **Reports** | Excel and PDF export, compliance reports, acknowledgement reports, audit logs |
| **Security** | JWT authentication, BCrypt passwords, RBAC with 4 roles, audit trail |
| **Cloud Storage** | AWS S3 integration with local fallback |
| **Legal Hold** | Prevent deletion/modification of documents under legal review |

---

## Technology Stack

### Backend
| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 | Core language |
| Spring Boot | 3.2.2 | Application framework |
| Spring Security | 6.x | Authentication & authorization |
| Spring Data JPA | 3.x | Database ORM |
| MySQL | 8.0+ | Primary database |
| Apache Tika | 2.9.1 | PDF/Word text extraction |
| Tesseract (Tess4J) | 5.8.0 | OCR for scanned images |
| JWT (jjwt) | 0.12.3 | Token-based authentication |
| Apache POI | 5.2.5 | Excel report generation |
| OpenPDF | 1.3.35 | PDF report generation |
| AWS SDK v2 | 2.25.11 | S3 cloud storage |
| Twilio SDK | 10.1.5 | SMS notifications |
| Spring Mail | - | Email notifications |
| Lombok | 1.18.30 | Boilerplate reduction |

### Frontend
| Technology | Version | Purpose |
|------------|---------|---------|
| React | 18.x | UI framework |
| React Router | 6.x | Client-side routing |
| Axios | - | HTTP client |
| React Icons (Fa) | - | Icon library |
| Vanilla CSS | - | Custom styling (no Tailwind) |

---

## Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                        FRONTEND (React 18)                       │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐             │
│  │ Welcome  │ │Dashboard │ │Documents │ │ Upload   │  ...16 pages│
│  │  Page    │ │  Page    │ │  Page    │ │  Page    │             │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘             │
│       │             │            │             │                 │
│  ┌────┴─────────────┴────────────┴─────────────┴──────────────┐  │
│  │              Services Layer (Axios + JWT Interceptor)      │  │
│  │  authService │ documentService │ dashboardService │ ...    │  │
│  └──────────────────────────┬─────────────────────────────────┘  │
└─────────────────────────────┼────────────────────────────────────┘
                              │ REST API (JSON)
┌─────────────────────────────┼────────────────────────────────────┐
│                     BACKEND (Spring Boot 3.2)                    │
│  ┌──────────────────────────┴─────────────────────────────────┐  │
│  │                    Controller Layer (9)                    │  │
│  │  Auth │ Document │ Dashboard │ Alert │ Analytics │ ...     │  │
│  └──────────────────────────┬─────────────────────────────────┘  │
│  ┌──────────────────────────┴─────────────────────────────────┐  │
│  │                     Service Layer (26)                     │  │
│  │  AuthService │ DocumentService │ ComplianceScheduler │ ... │  │
│  └──────────────────────────┬─────────────────────────────────┘  │
│  ┌──────────────────────────┴─────────────────────────────────┐  │
│  │                   Repository Layer (11)                    │  │
│  │               Spring Data JPA + Custom Queries             │  │
│  └──────────────────────────┬─────────────────────────────────┘  │
│  ┌──────────────────────────┴─────────────────────────────────┐  │
│  │                    Security Layer                          │  │
│  │         JWT Filter → UserDetailsService → SecurityConfig   │  │
│  └────────────────────────────────────────────────────────────┘  │
└─────────────────────────────┼────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              │               │               │
        ┌─────┴─────┐  ┌─────┴─────┐  ┌──────┴──────┐
        │  MySQL 8  │  │  AWS S3   │  │ Email/SMS   │
        │ 11 Tables │  │  Storage  │  │ Twilio/SMTP │
        └───────────┘  └───────────┘  └─────────────┘

---

## Role-Based Access Control (RBAC)

MetroHub implements a 4-role RBAC system with granular permissions:

### Role Hierarchy

| Role | Scope | Description |
|------|-------|-------------|
| **SUPER_ADMIN** | Global | Full system access. Views all departments' data. Can remove legal holds, manage all users, access all analytics. |
| **DEPARTMENT_ADMIN** | Department | Manages department users and compliance. Views department-specific analytics, reports, and violations. Cannot upload documents. |
| **DEPARTMENT_UPLOAD_ADMIN** | Department | Uploads documents, sets SLA, applies legal holds. Manages acknowledgements and policies for their department. |
| **DEPARTMENT_USER** | Department | End user. Views documents in their department, acknowledges them, receives notifications. |

### Permission Matrix

| Permission | Super Admin | Dept Admin | Upload Admin | Dept User |
|------------|:-----------:|:----------:|:------------:|:---------:|
| View Dashboard | ✅ (Global) | ✅ (Dept) | ✅ (Dept) | ✅ (Dept) |
| Upload Documents | ❌ | ❌ | ✅ | ❌ |
| View Documents | ✅ (All) | ✅ (Dept) | ✅ (Dept) | ✅ (Dept) |
| Delete Documents | ✅ | ✅ | ❌ | ❌ |
| Acknowledge Documents | ❌ | ❌ | ❌ | ✅ |
| Track Acknowledgements | ✅ | ✅ | ✅ | ✅ |
| Set SLA / Manual Config | ❌ | ❌ | ✅ | ❌ |
| Apply Legal Hold | ❌ | ❌ | ✅ | ❌ |
| Remove Legal Hold | ✅ | ❌ | ❌ | ❌ |
| Manage Policies | ✅ | ✅ | ✅ | ❌ |
| Delete Policies | ✅ | ✅ | ❌ | ❌ |
| View Compliance | ✅ (All) | ✅ (Dept) | ❌ | ❌ |
| Resolve Violations | ✅ | ✅ | ✅ | ❌ |
| View Analytics | ✅ (All) | ✅ (Dept) | ❌ | ❌ |
| View Reports | ✅ (All) | ✅ (Dept) | ❌ | ❌ |
| Manage Users | ✅ (All) | ✅ (Dept) | ❌ | ❌ |
| Trigger Risk Calc | ✅ | ❌ | ❌ | ❌ |
| View Notifications | ✅ | ✅ | ✅ | ✅ |
| Settings | ✅ | ✅ | ✅ | ✅ |

---

## Module-by-Module Breakdown

### 1. Authentication & Authorization

- **Login Flow:** Email + Password → BCrypt verification → JWT access token (24h) + refresh token (7d)
- **Token Storage:** `sessionStorage` (cleared on tab close for security)
- **JWT Filter:** Every API request passes through `JwtAuthenticationFilter` which validates the `Authorization: Bearer <token>` header
- **Logout:** Token is blacklisted server-side via `TokenBlacklistService`
- **Password Security:** BCrypt hashing with salt rounds

---

### 2. Dashboard

- **Super Admin View:** Global statistics across all departments — total documents, pending acknowledgements, active violations, department comparison charts
- **Department View:** Department-specific metrics — recent uploads, acknowledgement rates, SLA compliance percentage
- **Summary Cards:** Total Documents, Pending Acknowledgements, Active Violations, Compliance Rate
- **Auto-refresh:** Dashboard data refreshes automatically

---

### 3. Document Management

- **Document List:** Paginated table with filters (department, type, priority, status)
- **Search:** Full-text search across file names, extracted text, and tags
- **Document Details:** Complete metadata view — file info, classification, extracted text preview, SLA status, acknowledgement status
- **View Original:** Download/view the original uploaded document (PDF viewer)
- **Extracted Text:** Dedicated page showing full extracted text with NLP-identified entities highlighted
- **Delete:** Soft delete with confirmation modal (respects legal hold)
- **Legal Hold:** Prevents deletion/modification when under legal review

---

### 4. Document Upload & Processing

The upload flow is a multi-stage pipeline:

```
User Upload → File Validation → Storage (S3/Local) → Text Extraction (Tika/OCR)
    → AI Classification → NLP Metadata Extraction → SLA Configuration
    → Notification Dispatch → Acknowledgement Tracking Begins
```

1. **File Validation:** Allowed types: PDF, DOCX, JPEG, PNG. Max size: 10MB
2. **Storage:** Uploaded to AWS S3 (primary) with local filesystem fallback
3. **Classification:** AI classifies into one of 11 document types with confidence score
4. **Manual Override:** Upload admin can manually classify and set priority
5. **SLA Configuration:** Manual SLA (hours set by admin) or Auto-SLA (policy rules applied automatically)
6. **Progress Modal:** Real-time upload progress with status indicators

---

### 5. Text Extraction & NLP

- **Apache Tika:** Extracts text from PDF and Word documents natively
- **Tesseract OCR:** For scanned images and image-based PDFs (English language)
- **NLP Processing:** Extracts structured metadata from unstructured text:
  - Equipment IDs and names
  - Vendor names and codes
  - Invoice numbers and amounts
  - Reference numbers and dates
  - People names (author, approver, recipient)
  - AI-generated subject and summary

---

### 6. Acknowledgement System

- **Purpose:** Every `DEPARTMENT_USER` must acknowledge documents uploaded to their department
- **Acknowledge Action:** User clicks "Acknowledge" → timestamp, IP address, and optional notes recorded
- **Tracking Page:** Shows who has acknowledged and who hasn't, with visual progress
- **Unique Constraint:** Each user can acknowledge a document only once
- **Late Acknowledgement:** If acknowledged after SLA deadline, marked as `acknowledged_late` in violations

---

### 7. SLA & Compliance Engine

This is the core automated compliance enforcement engine. It runs on a scheduled basis and processes every active document that has pending acknowledgements.

#### How SLA Works

When a document is uploaded, the system determines its SLA timing:

1. **Manual SLA:** Upload admin explicitly sets SLA hours during upload (e.g., "Acknowledge within 24 hours")
2. **Auto-SLA (Policy Rules):** System looks up matching `policy_rules` based on `department_id` + `priority`:
   - First checks for an exact match (same department + same priority)
   - Falls back to the Global Default policy if no match

#### The 4-Stage Escalation Pipeline

Once SLA is determined, the compliance scheduler runs the following stages sequentially:

```
Document Upload (T=0)
    │
    ├── Stage 1: REMINDER (T + reminder_hours)
    │   └── Send reminder to DEPARTMENT_USER via configured channels
    │
    ├── Stage 2: DEPT ADMIN ESCALATION (T + dept_admin_escalation_hours)
    │   └── Escalate to DEPARTMENT_ADMIN with alert
    │
    ├── Stage 3: SUPER ADMIN ESCALATION (T + super_admin_escalation_hours)
    │   └── Escalate to SUPER_ADMIN with alert
    │
    └── Stage 4: VIOLATION CREATED (T + violation_hours)
        └── Permanent compliance violation record created
```

#### Notification Channels Per Stage

Each stage can use different channels based on policy configuration:
- **Dashboard:** In-app notification bell (always enabled)
- **Email:** SMTP email via Gmail (configurable)
- **SMS:** Twilio SMS (configurable, with mock mode for development)

---

### 8. Notifications & Alerts

- **Alert Types:** 11 different alert types (HIGH_PRIORITY_UPLOAD, DEADLINE_APPROACHING, ESCALATION_DEPT_ADMIN, etc.)
- **Priority-Based Routing:**
  - HIGH priority → Dashboard + Email + SMS
  - MEDIUM priority → Dashboard + Email
  - LOW priority → Dashboard only
- **Notification Page:** Filterable list with read/unread status, notification type badges
- **Bell Icon:** Real-time unread count in the system header
- **Batch Processing:** Notifications processed in batches of 50 every 5 minutes

---

### 9. Policy Management

- **SLA Policy Rules:** Admin-configurable rules that define escalation timelines
- **Rule Matching:** Each rule targets a specific `department + priority` combination
- **Global Default:** One rule with `department_id = NULL, priority = NULL` serves as fallback
- **Policy Fields:**
  - `reminder_hours` — When to send first reminder
  - `dept_admin_escalation_hours` — When to escalate to department admin
  - `super_admin_escalation_hours` — When to escalate to super admin
  - `violation_hours` — When to create a violation record
- **Notification Toggles:** Enable/disable Email, SMS, Dashboard per rule
- **Legal Hold Management:** Apply legal hold to prevent document deletion during investigations

---

### 10. Compliance Violations

- **Auto-Created:** by the compliance scheduler when `violation_hours` is exceeded
- **Violation Record:** Includes document ID, user ID, department, days delayed, which policy rule was applied
- **Resolution:** Admins can resolve violations with remarks
- **Late Acknowledgement:** If a user acknowledges after violation, the violation is updated with `acknowledged_late = true`
- **Escalation Tracking:** Each violation tracks whether reminder, dept admin escalation, and super admin escalation were sent

---

### 11. Analytics & Risk Assessment

- **Super Admin View:** Cross-department analytics with aggregated metrics
- **Department Admin View:** Department-specific performance analytics
- **Risk Score Calculation (0-100):**
  - Late acknowledgements: +5 points each
  - Active violations: +15 points each
  - Pending violations: +10 points each
  - Dept admin escalations: +8 points each
  - Super admin escalations: +12 points each
  - Legal holds: +10 points each
  - Safety violations: +20 points each
  - Repeat offenses: +15 points each
- **Risk Levels:** LOW (0-25), MEDIUM (26-50), HIGH (51-75), CRITICAL (76-100)
- **Snapshots:** Risk scores are saved as immutable snapshots for audit trail
- **Auto-refresh:** Analytics data refreshes automatically

---

### 12. Reports & Export

- **Report Types:**
  - Document Inventory Report
  - Acknowledgement Status Report
  - Compliance Violation Report
  - Department Performance Report
  - Audit Trail Report
- **Export Formats:** Excel (.xlsx) and PDF
- **Filters:** Date range, department, priority, status
- **Role-Based:** Super Admin sees all data; Department Admin sees only their department

---

### 13. User Management

- **Create User:** Add new users with role, department, employee ID, phone number
- **Edit User:** Update name, email, role, department, phone, active status
- **Delete User:** Soft deactivation (sets `is_active = false`)
- **Department Filtering:** Super Admin sees all users; Department Admin sees only their department's users
- **Password:** Auto-assigned using BCrypt hashing

---

### 14. Settings

- **Profile Tab:** View and update current user's profile information
- **Security Tab:** Change password
- **About Tab:** System information and version details

---

## SLA Workflow — All Scenarios

### Scenario 1: Happy Path (User Acknowledges on Time)

```
T=0h    Upload Admin uploads a MEDIUM priority document to Maintenance dept
        → System finds policy: Global Default (reminder=24h, escalation=48h/72h, violation=168h)
        → Notifications sent to all DEPARTMENT_USER in Maintenance

T=12h   Maint Technician 1 opens and acknowledges the document ✅
        → Acknowledgement recorded with timestamp
        → No further escalation needed for this user
```

**Result:** No violation. Clean compliance record.

---

### Scenario 2: Late Acknowledgement (After Reminder, Before Violation)

```
T=0h    Document uploaded (MEDIUM priority, Maintenance dept)
T=24h   ⚠️ Stage 1 REMINDER sent to pending users (Dashboard + Email)
T=36h   Maint Technician 1 acknowledges ✅ (late, but before violation)
T=48h   ⚠️ Stage 2 DEPT ADMIN ESCALATION sent (but Technician 1 already acknowledged)
```

**Result:** No violation created. Technician 1 acknowledged late but within the violation window.

---

### Scenario 3: Full Escalation → Violation Created

```
T=0h    Document uploaded (MEDIUM priority, Safety dept)
T=24h   ⚠️ Stage 1 REMINDER sent to Safety Inspector (Dashboard + Email + SMS)
T=48h   🔶 Stage 2 DEPT ADMIN ESCALATION → Safety Manager notified
T=72h   🔴 Stage 3 SUPER ADMIN ESCALATION → System Admin notified
T=168h  ❌ Stage 4 VIOLATION CREATED → Permanent compliance violation record
        → Safety Inspector now has a violation on their record
        → Department risk score increases
```

**Result:** Compliance violation permanently recorded. Requires admin resolution.

---

### Scenario 4: Safety HIGH Priority (Fastest Escalation)

```
T=0h    SAFETY CIRCULAR uploaded with HIGH priority to Safety dept
        → System finds policy: "Safety HIGH Priority" (reminder=6h, escalation=12h/24h, violation=48h)

T=6h    ⚠️ REMINDER sent (Dashboard + Email + SMS — all 3 channels enabled)
T=12h   🔶 DEPT ADMIN ESCALATION to Safety Manager
T=24h   🔴 SUPER ADMIN ESCALATION to System Admin
T=48h   ❌ VIOLATION CREATED (only 2 days vs 7 days for default!)
```

**Result:** Safety documents get fastest enforcement. Critical for safety compliance.

---

### Scenario 5: Manual SLA Override

```
T=0h    Upload Admin uploads document and manually sets SLA = 12 hours
        → Manual SLA takes precedence over any policy rule
        → System uses 12h for all escalation calculations

T=12h   If not acknowledged → escalation begins based on 12h baseline
```

**Result:** Manual SLA gives upload admins fine-grained control for urgent documents.

---

### Scenario 6: HR LOW Priority (Relaxed — No Violations)

```
T=0h    HR document uploaded with LOW priority
        → System finds policy: "HR LOW Priority" (reminder=48h, escalation=0h, violation=0h)

T=48h   ⚠️ REMINDER sent (Dashboard only — email/SMS disabled)
        → No further escalation (escalation_hours = 0)
        → No violation creation (violation_hours = 0)
```

**Result:** Low priority HR documents only get a reminder, never escalate or create violations.

---

### Scenario 7: Legal Hold Prevents Deletion

```
T=0h    Document uploaded and processing begins
T=24h   Legal Officer applies Legal Hold with reason: "Under investigation"
        → document.legal_hold = true
        → Document cannot be deleted or modified by anyone
T=72h   Department Admin tries to delete → BLOCKED ❌ "Document under legal hold"
T=168h  Violation may still be created (legal hold doesn't stop SLA)
T=720h  Super Admin removes legal hold → Document can now be deleted
```

**Result:** Legal hold protects document integrity during investigations.

---

### Scenario 8: Late Acknowledgement After Violation

```
T=0h    Document uploaded
T=168h  ❌ Violation created for Safety Inspector (168h = 7 days)
T=200h  Safety Inspector finally acknowledges the document
        → Violation updated: acknowledged_late = true, late_acknowledgement_date recorded
        → Violation remains on record (acknowledged_late does NOT resolve it)
        → Violation must still be resolved by an admin
```

**Result:** Late acknowledgement is recorded but doesn't auto-resolve the violation.

---

<p align="center">
  <strong>Built with ❤️ for Indian Metro Systems</strong><br/>
  <em>MetroHub — Digitizing Metro Document Intelligence</em>
</p>
