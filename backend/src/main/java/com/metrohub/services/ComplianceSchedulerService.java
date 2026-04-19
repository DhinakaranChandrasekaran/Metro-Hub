package com.metrohub.services;
import com.metrohub.models.*;
import com.metrohub.models.Alert.AlertType;
import com.metrohub.models.User.UserRole;
import com.metrohub.repositories.*;
import com.metrohub.util.EmailTemplateBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class ComplianceSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceSchedulerService.class);

    private final DocumentRepository documentRepository;
    private final DocumentAcknowledgementRepository acknowledgementRepository;
    private final UserRepository userRepository;
    private final AlertRepository alertRepository;
    private final ComplianceViolationService violationService;
    private final NotificationService notificationService;
    private final PolicyService policyService;

    // Scheduler state
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private volatile String lastRunTimestamp = "Never";

    // Track processed items to avoid duplicates in single run
    private final Set<String> processedReminders = new HashSet<>();
    private final Set<String> processedDeptEscalations = new HashSet<>();
    private final Set<String> processedSuperEscalations = new HashSet<>();
    private final Set<String> processedViolations = new HashSet<>();

    // ============================================
    // MAIN SCHEDULED JOB - Runs Every 5 Minutes
    // ============================================

    @Scheduled(cron = "0 */5 * * * *")  // Every 5 minutes for faster escalation response
    public void processComplianceChecks() {
        if (isRunning.compareAndSet(false, true)) {
            try {
                log.info("⏰ ============================================");
                log.info("⏰ COMPLIANCE SCHEDULER - Starting 5-minute check");
                log.info("⏰ Timestamp: {}", LocalDateTime.now());
                log.info("⏰ ============================================");

                // Clear tracking sets
                clearProcessedSets();

                // Process in order (Auto-SLA FIRST, then escalations)
                try {
                    log.info("🔄 Step 1: Applying auto-SLAs...");
                    applyAutoSlas();
                    log.info("✅ Step 1 complete");
                } catch (Exception e) {
                    log.error("❌ Step 1 failed: {}", e.getMessage());
                }

                try {
                    log.info("🔄 Step 2: Processing violations...");
                    processViolationCreation();
                    log.info("✅ Step 2 complete");
                } catch (Exception e) {
                    log.error("❌ Step 2 failed: {}", e.getMessage());
                }

                try {
                    log.info("🔄 Step 3: Processing super-admin escalations...");
                    processSuperAdminEscalations();
                    log.info("✅ Step 3 complete");
                } catch (Exception e) {
                    log.error("❌ Step 3 failed: {}", e.getMessage());
                }

                try {
                    log.info("🔄 Step 4: Processing dept-admin escalations...");
                    processDeptAdminEscalations();
                    log.info("✅ Step 4 complete");
                } catch (Exception e) {
                    log.error("❌ Step 4 failed: {}", e.getMessage());
                }

                try {
                    log.info("🔄 Step 5: Processing reminders...");
                    processReminders();
                    log.info("✅ Step 5 complete");
                } catch (Exception e) {
                    log.error("❌ Step 5 failed: {}", e.getMessage());
                }

                lastRunTimestamp = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                log.info("✅ COMPLIANCE SCHEDULER - Completed successfully");
                log.info("✅ ============================================");

            } catch (Exception e) {
                log.error("❌ COMPLIANCE SCHEDULER - Error during execution: {}", e.getMessage(), e);
            } finally {
                isRunning.set(false);
            }
        } else {
            log.warn("⚠️ Compliance scheduler is already running, skipping this cycle");
        }
    }

    // ============================================
    // AUTO-SLA APPLICATION (at T+30 minutes)
    // ============================================

    @Transactional
    public void applyAutoSlas() {
        log.info("🔄 Applying auto-SLAs to documents past 30-minute grace period...");

        // Use native query to find documents uploaded 30+ minutes ago WITHOUT any SLA configured
        LocalDateTime gracePeriodThreshold = LocalDateTime.now().minusMinutes(30);
        List<Document> documentsNeedingAutoSla = documentRepository.findDocumentsNeedingAutoSla(gracePeriodThreshold);

        int autoSlasApplied = 0;
        for (Document doc : documentsNeedingAutoSla) {
            try {
                // ⭐ AUTO-SLA ALWAYS uses GLOBAL DEFAULT policy (24, 48, 72, 168)
                // NOT priority/department-based policies
                PolicyRule policy = policyService.getDefaultPolicy();

                // Apply auto-SLA from policy
                doc.setSlaReminderHours(policy.getReminderHours() != null ? policy.getReminderHours() : 24);
                doc.setSlaDeptAdminEscalationHours(policy.getDeptAdminEscalationHours() != null ? policy.getDeptAdminEscalationHours() : 48);
                doc.setSlaSuperAdminEscalationHours(policy.getSuperAdminEscalationHours() != null ? policy.getSuperAdminEscalationHours() : 72);
                doc.setSlaViolationHours(policy.getViolationHours() != null ? policy.getViolationHours() : 168);

                doc.setSlaEmailEnabled(policy.getEmailEnabled() != null ? policy.getEmailEnabled() : true);
                doc.setSlaSmsEnabled(policy.getSmsEnabled() != null ? policy.getSmsEnabled() : true);
                doc.setSlaDashboardEnabled(true);

                doc.setSlaConfiguredAt(LocalDateTime.now());
                doc.setIsSlaManual(false); // Mark as auto-SLA (important for notification channels)

                documentRepository.save(doc);

                log.info("✅ Auto-SLA applied to document ID={} - Policy: {} ({}h, {}h, {}h, {}h)",
                        doc.getId(), policy.getName(),
                        doc.getSlaReminderHours(),
                        doc.getSlaDeptAdminEscalationHours(),
                        doc.getSlaSuperAdminEscalationHours(),
                        doc.getSlaViolationHours());
                autoSlasApplied++;
            } catch (Exception e) {
                log.error("Failed to apply auto-SLA to document ID={}: {}", doc.getId(), e.getMessage());
            }
        }

        log.info("✅ Auto-SLAs applied: {} documents", autoSlasApplied);
    }

    // ============================================
    // REMINDERS (Policy-Driven Timing)
    // ============================================

    @Transactional
    public void processReminders() {
        log.info("📢 Processing reminders (policy-driven)...");

        // Get all active documents that might need reminders (check all recent docs, not just 1+ hour old)
        List<Document> allDocs = documentRepository.findDocumentsNeedingComplianceCheck(
                LocalDateTime.now().minusMinutes(5)); // Check documents from last 5 minutes + earlier

        int remindersSent = 0;
        for (Document doc : allDocs) {
            // Get policy-driven timing for this document
            int reminderHours = policyService.getReminderHours(doc);

            // Skip if reminder is disabled (0 hours)
            if (reminderHours <= 0) continue;

            LocalDateTime threshold = doc.getUploadDate().plusHours(reminderHours);
            if (LocalDateTime.now().isBefore(threshold)) continue;

            List<User> unacknowledgedUsers = acknowledgementRepository.findUsersNotAcknowledged(doc.getId());

            for (User user : unacknowledgedUsers) {
                // SAFETY CHECK 1: Ensure user is DEPARTMENT_USER (not admin)
                if (user.getRole() != UserRole.DEPARTMENT_USER) {
                    log.warn("⚠️ Non-DEPARTMENT_USER in findUsersNotAcknowledged! Role={}, Email={}. Skipping.", user.getRole(), user.getEmail());
                    continue;
                }

                // SAFETY CHECK 2: Double-verify user hasn't acknowledged (transaction race condition)
                if (acknowledgementRepository.existsByDocument_IdAndUser_Id(doc.getId(), user.getId())) {
                    log.debug("⏭️ User {} has already acknowledged doc {} - skipping reminder", user.getEmail(), doc.getId());
                    continue;
                }

                String key = doc.getId() + "_" + user.getId();

                // Skip if already processed in this run
                if (processedReminders.contains(key)) continue;

                // Skip if violation already exists (no more reminders after violation)
                if (violationService.violationExists(doc.getId(), user.getId())) {
                    log.debug("Skipping reminder - violation exists for doc {} user {}", doc.getId(), user.getId());
                    continue;
                }

                // Skip if reminder already sent in last 24 hours
                if (wasReminderSentRecently(doc.getId(), user.getId())) {
                    continue;
                }

                sendReminder(user, doc, reminderHours);
                processedReminders.add(key);
                remindersSent++;
            }
        }

        log.info("📢 Reminders sent: {}", remindersSent);
    }

    private void sendReminder(User user, Document doc, int reminderHours) {
        PolicyRule policy = policyService.findApplicablePolicy(doc);

        // Check if document has manual SLA
        String slaType = Boolean.TRUE.equals(doc.getIsSlaManual()) ? "Manual SLA" : "Policy: " + policy.getName();

        String message = String.format(
            "⏰ REMINDER: Document '%s' requires your acknowledgement. " +
            "%s (SLA: %dh). Please acknowledge to avoid escalation.",
            doc.getFileName(),
            slaType,
            reminderHours
        );

        try {
            // Send via NotificationService with all enabled channels (Dashboard + Email + SMS)
            notificationService.sendNotification(user, doc, AlertType.ACKNOWLEDGEMENT_REQUIRED, message);
            log.info("✅ Reminder sent to {} for document {} (Dashboard + Email + SMS)", user.getEmail(), doc.getFileName());
        } catch (Exception e) {
            log.error("Failed to send reminder to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    // ============================================
    // DEPARTMENT ADMIN ESCALATION (Policy-Driven)
    // ============================================

    @Transactional
    public void processDeptAdminEscalations() {
        log.info("📤 Processing department admin escalations (policy-driven)...");

        // Get all active documents
        List<Document> allDocs = documentRepository.findDocumentsNeedingComplianceCheck(
                LocalDateTime.now().minusMinutes(5));

        int escalationsSent = 0;
        for (Document doc : allDocs) {
            // Get policy-driven timing for this document
            int escalationHours = policyService.getDeptAdminEscalationHours(doc);

            // Skip if escalation is disabled (0 hours)
            if (escalationHours <= 0) {
                log.debug("Dept admin escalation disabled for doc {} (policy)", doc.getId());
                continue;
            }

            LocalDateTime threshold = doc.getUploadDate().plusHours(escalationHours);
            if (LocalDateTime.now().isBefore(threshold)) continue;

            List<User> unacknowledgedUsers = acknowledgementRepository.findUsersNotAcknowledged(doc.getId());

            for (User user : unacknowledgedUsers) {
                String key = doc.getId() + "_" + user.getId();

                // Skip if already processed
                if (processedDeptEscalations.contains(key)) continue;

                // Skip if violation already exists
                if (violationService.violationExists(doc.getId(), user.getId())) continue;

                // Skip if already escalated to dept admin
                if (wasAlreadyEscalatedToDeptAdmin(doc.getId(), user.getId())) continue;

                sendDeptAdminEscalation(doc, user, escalationHours);
                processedDeptEscalations.add(key);
                escalationsSent++;
            }
        }

        log.info("📤 Department admin escalations sent: {}", escalationsSent);
    }

    private void sendDeptAdminEscalation(Document doc, User delinquentUser, int escalationHours) {
        Department dept = doc.getDepartment();
        if (dept == null) return;

        PolicyRule policy = policyService.findApplicablePolicy(doc);

        // Find department admins
        List<User> deptAdmins = userRepository.findByDepartmentIdAndRole(
                dept.getId(), UserRole.DEPARTMENT_ADMIN);
        List<User> uploadAdmins = userRepository.findByDepartmentIdAndRole(
                dept.getId(), UserRole.DEPARTMENT_UPLOAD_ADMIN);

        String message = String.format(
            "⚠️ ESCALATION L1: User '%s' has not acknowledged document '%s' for %d+ hours. " +
            "Policy: %s. Immediate attention required for compliance.",
            delinquentUser.getName(), doc.getFileName(), escalationHours, policy.getName()
        );

        // USER GETS REMINDER at escalation time (Dashboard + Email + SMS)
        String reminderMessage = String.format(
            "📢 REMINDER: Document '%s' - Department '%s' - %d+ hours pending. " +
            "Please acknowledge immediately to avoid further escalations.",
            doc.getFileName(),
            doc.getDepartment() != null ? doc.getDepartment().getName() : "Unknown",
            escalationHours
        );
        try {
            notificationService.sendNotification(delinquentUser, doc, AlertType.ACKNOWLEDGEMENT_REQUIRED, reminderMessage);
            log.info("✅ Reminder sent to USER {} at escalation (Dashboard + Email + SMS)", delinquentUser.getEmail());
        } catch (Exception e) {
            log.error("Failed to send reminder to user {} at escalation: {}", delinquentUser.getEmail(), e.getMessage());
        }

        // Notify department admins with channels based on SLA type
        for (User admin : deptAdmins) {
            if (!admin.getId().equals(delinquentUser.getId())) {
                try {
                    // ALWAYS create dashboard alert
                    notificationService.createDashboardAlert(admin, doc, AlertType.ESCALATION_DEPT_ADMIN, message);

                    // BOTH MANUAL & AUTO-SLA: Send Email (in addition to Dashboard)
                    // NO SMS for escalations (SMS only for user reminders)
                    if (admin.getEmail() != null) {
                        String subject = "⚠️ ESCALATION L1 - " + doc.getFileName();
                        String escalationBody = EmailTemplateBuilder.escalationBody(doc.getFileName(), doc.getDepartment().getName(), 1);
                        String emailContent = EmailTemplateBuilder.buildEmail(admin.getName(), message, escalationBody, "ESCALATION_DEPT_ADMIN");
                        notificationService.sendEmail(admin.getEmail(), subject, emailContent);
                    }
                    log.info("📊📧 Dept admin escalation sent to {} - Dashboard + Email (NO SMS)", admin.getEmail());
                } catch (Exception e) {
                    log.error("Failed to send escalation to {}: {}", admin.getEmail(), e.getMessage());
                }
            }
        }

        // Notify upload admins with same channel logic
        for (User admin : uploadAdmins) {
            if (!admin.getId().equals(delinquentUser.getId())) {
                try {
                    // ALWAYS create dashboard alert
                    notificationService.createDashboardAlert(admin, doc, AlertType.ESCALATION_DEPT_ADMIN, message);

                    // BOTH MANUAL & AUTO-SLA: Send Email (in addition to Dashboard)
                    // NO SMS for escalations (SMS only for user reminders)
                    if (admin.getEmail() != null) {
                        String subject = "⚠️ ESCALATION L1 - " + doc.getFileName();
                        notificationService.sendEmail(admin.getEmail(), subject, message);
                    }
                    log.info("📊📧 Upload admin escalation sent to {} - Dashboard + Email (NO SMS)", admin.getEmail());
                } catch (Exception e) {
                    log.error("Failed to send escalation to {}: {}", admin.getEmail(), e.getMessage());
                }
            }
        }
    }

    // ============================================
    // L2 ESCALATION - Dept Admin + Upload Admin (Policy-Driven)
    // ============================================

    @Transactional
    public void processSuperAdminEscalations() {
        log.info("🔴 Processing L2 escalations to Dept Admin + Upload Admin ONLY (policy-driven)...");

        // Get all active documents
        List<Document> allDocs = documentRepository.findDocumentsNeedingComplianceCheck(
                LocalDateTime.now().minusMinutes(5));

        int escalationsSent = 0;
        for (Document doc : allDocs) {
            // Get policy-driven timing for this document
            int escalationHours = policyService.getSuperAdminEscalationHours(doc);

            // Skip if escalation is disabled (0 hours)
            if (escalationHours <= 0) {
                log.debug("L2 escalation disabled for doc {} (policy)", doc.getId());
                continue;
            }

            LocalDateTime threshold = doc.getUploadDate().plusHours(escalationHours);
            if (LocalDateTime.now().isBefore(threshold)) continue;

            List<User> unacknowledgedUsers = acknowledgementRepository.findUsersNotAcknowledged(doc.getId());

            // Skip if all users acknowledged
            if (unacknowledgedUsers.isEmpty()) {
                log.debug("⏭️ All users acknowledged for doc {} - skipping L2 escalation", doc.getId());
                continue;
            }

            for (User user : unacknowledgedUsers) {
                String key = doc.getId() + "_" + user.getId();

                // Skip if already processed
                if (processedSuperEscalations.contains(key)) continue;

                // Skip if violation already exists
                if (violationService.violationExists(doc.getId(), user.getId())) continue;

                // Skip if already escalated to dept admin / upload admin
                if (wasAlreadyEscalatedToDeptAdmin(doc.getId(), user.getId())) {
                    log.debug("⏭️ L2 escalation already sent for doc {} user {}", doc.getId(), user.getId());
                    continue;
                }

                sendL2Escalation(doc, user, escalationHours);
                processedSuperEscalations.add(key);
                escalationsSent++;
            }
        }

        log.info("🔴 L2 escalations sent to Dept Admin + Upload Admin ONLY: {}", escalationsSent);
    }

    private void sendL2Escalation(Document doc, User delinquentUser, int escalationHours) {
        Department dept = doc.getDepartment();
        if (dept == null) return;

        PolicyRule policy = policyService.findApplicablePolicy(doc);

        int violationHours = policyService.getViolationHours(doc);
        int hoursUntilViolation = violationHours > 0 ? violationHours - escalationHours : 0;

        // USER GETS REMINDER at L2 escalation time (Dashboard + Email + SMS)
        String reminderMessage = String.format(
            "📢 REMINDER: Document '%s' - Department '%s' - %d+ hours pending. " +
            "Please acknowledge immediately to avoid violation.",
            doc.getFileName(),
            dept.getName(),
            escalationHours
        );
        try {
            notificationService.sendNotification(delinquentUser, doc, AlertType.ACKNOWLEDGEMENT_REQUIRED, reminderMessage);
            log.info("✅ Reminder sent to USER {} at L2 escalation (Dashboard + Email + SMS)", delinquentUser.getEmail());
        } catch (Exception e) {
            log.error("Failed to send reminder to user {} at L2 escalation: {}", delinquentUser.getEmail(), e.getMessage());
        }

        // L2 ESCALATION to Department Admins (Dashboard + Email, NO SMS)
        String deptAdminMessage = String.format(
            "⚠️ ESCALATION L2: User '%s' has not acknowledged document '%s' for %d+ hours. " +
            "Policy: %s. Immediate attention required for compliance.",
            delinquentUser.getName(), doc.getFileName(), escalationHours, policy.getName()
        );

        List<User> deptAdmins = userRepository.findByDepartmentIdAndRole(
                dept.getId(), UserRole.DEPARTMENT_ADMIN);
        for (User admin : deptAdmins) {
            if (!admin.getId().equals(delinquentUser.getId())) {
                try {
                    notificationService.createDashboardAlert(admin, doc, AlertType.ESCALATION_DEPT_ADMIN, deptAdminMessage);
                    if (admin.getEmail() != null) {
                        String subject = "⚠️ ESCALATION L2 - " + doc.getFileName();
                        String escalationBody = EmailTemplateBuilder.escalationBody(doc.getFileName(), dept.getName(), 2);
                        String emailContent = EmailTemplateBuilder.buildEmail(admin.getName(), deptAdminMessage, escalationBody, "ESCALATION_DEPT_ADMIN");
                        notificationService.sendEmail(admin.getEmail(), subject, emailContent);
                    }
                    log.info("📊📧 L2 Dept admin escalation sent to {} - Dashboard + Email (NO SMS)", admin.getEmail());
                } catch (Exception e) {
                    log.error("Failed to send L2 escalation to dept admin {}: {}", admin.getEmail(), e.getMessage());
                }
            }
        }

        // L2 ESCALATION to Upload Admins (Dashboard + Email, NO SMS) - NO SUPER ADMIN HERE
        String uploadAdminMessage = String.format(
            "⚠️ ESCALATION L2: User '%s' (%s) has not acknowledged document '%s' for %d+ hours. " +
            "Department: %s. Policy: %s. %s",
            delinquentUser.getName(),
            delinquentUser.getEmail(),
            doc.getFileName(),
            escalationHours,
            dept.getName(),
            policy.getName(),
            violationHours > 0
                ? String.format("Violation will be created in %d hours if unresolved.", hoursUntilViolation)
                : "Violations disabled for this policy."
        );

        List<User> uploadAdmins = userRepository.findByDepartmentIdAndRole(
                dept.getId(), UserRole.DEPARTMENT_UPLOAD_ADMIN);
        for (User admin : uploadAdmins) {
            if (!admin.getId().equals(delinquentUser.getId())) {
                try {
                    notificationService.createDashboardAlert(admin, doc, AlertType.ESCALATION_DEPT_ADMIN, uploadAdminMessage);
                    if (admin.getEmail() != null) {
                        String subject = "⚠️ ESCALATION L2 - " + doc.getFileName();
                        String escalationBody = EmailTemplateBuilder.escalationBody(doc.getFileName(), dept.getName(), 2);
                        String emailContent = EmailTemplateBuilder.buildEmail(admin.getName(), uploadAdminMessage, escalationBody, "ESCALATION_DEPT_ADMIN");
                        notificationService.sendEmail(admin.getEmail(), subject, emailContent);
                    }
                    log.info("📊📧 L2 Upload admin escalation sent to {} - Dashboard + Email (NO SMS)", admin.getEmail());
                } catch (Exception e) {
                    log.error("Failed to send L2 escalation to upload admin {}: {}", admin.getEmail(), e.getMessage());
                }
            }
        }
    }

    // ============================================
    // VIOLATION CREATION (Policy-Driven)
    // ============================================

    @Transactional
    public void processViolationCreation() {
        log.info("🚨 Processing violation creation (policy-driven)...");

        // Get all active documents
        List<Document> allDocs = documentRepository.findDocumentsNeedingComplianceCheck(
                LocalDateTime.now().minusMinutes(5));

        int violationsCreated = 0;
        for (Document doc : allDocs) {
            // Get policy-driven timing for this document
            int violationHours = policyService.getViolationHours(doc);

            // Skip if violations are disabled (0 hours)
            if (violationHours <= 0) {
                log.debug("Violation creation disabled for doc {} (policy)", doc.getId());
                continue;
            }

            LocalDateTime threshold = doc.getUploadDate().plusHours(violationHours);
            if (LocalDateTime.now().isBefore(threshold)) continue;

            List<User> unacknowledgedUsers = acknowledgementRepository.findUsersNotAcknowledged(doc.getId());

            for (User user : unacknowledgedUsers) {
                String key = doc.getId() + "_" + user.getId();

                // Skip if already processed
                if (processedViolations.contains(key)) continue;

                // Skip if violation already exists
                if (violationService.violationExists(doc.getId(), user.getId())) {
                    log.debug("Violation already exists for doc {} user {}", doc.getId(), user.getId());
                    continue;
                }

                createViolation(doc, user, violationHours);
                processedViolations.add(key);
                violationsCreated++;
            }
        }

        log.info("🚨 Violations created: {}", violationsCreated);
    }

    private void createViolation(Document doc, User user, int violationHours) {
        long hoursDelay = ChronoUnit.HOURS.between(doc.getUploadDate(), LocalDateTime.now());
        int daysDelayed = (int) (hoursDelay / 24);

        PolicyRule policy = policyService.findApplicablePolicy(doc);

        try {
            ComplianceViolation violation = violationService.createViolationWithPolicy(
                    doc, user, daysDelayed, policy, violationHours);
            log.info("Created violation ID={} for user={} on document={} (Policy: {}, SLA: {}h)",
                    violation.getId(), user.getEmail(), doc.getFileName(),
                    policy.getName(), violationHours);

            // ❌ USER DOES NOT GET VIOLATION NOTIFICATION (only Super Admin gets it)

            // Notify super admins ONLY (Dashboard + Email, NO SMS)
            // Include list of all unacknowledged users
            List<User> unacknowledgedUsers = acknowledgementRepository.findUsersNotAcknowledged(doc.getId());
            String unacknowledgedList = unacknowledgedUsers.isEmpty() ? "None" :
                    unacknowledgedUsers.stream()
                            .map(u -> u.getName() + " (" + u.getEmail() + ")")
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("Unknown");

            List<User> superAdmins = userRepository.findAllSuperAdmins();
            String adminMessage = String.format(
                "🚨 VIOLATION CREATED: User '%s' (%s) - Document '%s' - Department '%s' - " +
                "%d days overdue - Policy: %s (SLA: %dh)\n\n" +
                "📋 All Unacknowledged Users for this document:\n%s",
                user.getName(), user.getEmail(), doc.getFileName(),
                doc.getDepartment() != null ? doc.getDepartment().getName() : "Unknown",
                daysDelayed, policy.getName(), violationHours,
                unacknowledgedList
            );

            for (User admin : superAdmins) {
                // Dashboard alert
                notificationService.createDashboardAlert(admin, doc, AlertType.COMPLIANCE_VIOLATION_CREATED, adminMessage);
                // Email alert (NO SMS for violations)
                if (admin.getEmail() != null) {
                    String subject = "🚨 VIOLATION CREATED - " + doc.getFileName();
                    String emailContent = EmailTemplateBuilder.buildEmail(admin.getName(), adminMessage, "New violation created", "VIOLATION");
                    notificationService.sendEmail(admin.getEmail(), subject, emailContent);
                }
            }

            log.info("🚨 Violation created: User={}, Document={}, DaysDelayed={}, Policy={}, UnacknowledgedCount={}",
                    user.getEmail(), doc.getFileName(), daysDelayed, policy.getName(), unacknowledgedUsers.size());

        } catch (Exception e) {
            log.error("Failed to create violation for user {} doc {}: {}",
                    user.getEmail(), doc.getId(), e.getMessage());
        }
    }



    // ============================================
    // HELPER METHODS
    // ============================================

    private boolean wasReminderSentRecently(Long documentId, Long userId) {
        // Check if reminder was sent in last 10 minutes (stricter than 24h to prevent spam)
        LocalDateTime recentThreshold = LocalDateTime.now().minusMinutes(10);
        boolean sentRecently = alertRepository.existsByUserIdAndDocumentIdAndAlertTypeSince(
                userId, documentId, AlertType.ACKNOWLEDGEMENT_REQUIRED, recentThreshold);
        if (sentRecently) {
            log.debug("⏭️ Reminder already sent to user {} for doc {} in last 10 minutes", userId, documentId);
            return true;
        }
        return false;
    }

    private boolean wasAlreadyEscalatedToDeptAdmin(Long documentId, Long userId) {
        // Check if escalation was sent in last 10 minutes (prevent duplicate escalations)
        LocalDateTime recentThreshold = LocalDateTime.now().minusMinutes(10);
        boolean escalatedRecently = alertRepository.existsByUserIdAndDocumentIdAndAlertTypeSince(
                userId, documentId, AlertType.ESCALATION_DEPT_ADMIN, recentThreshold);
        if (escalatedRecently) {
            log.debug("⏭️ Dept admin escalation already sent for doc {} user {} in last 10 minutes", documentId, userId);
            return true;
        }
        return false;
    }

    @SuppressWarnings("unused")
    private boolean wasAlreadyEscalatedToSuperAdmin(Long documentId, Long userId) {
        // Check if escalation was sent in last 10 minutes (prevent duplicate escalations)
        LocalDateTime recentThreshold = LocalDateTime.now().minusMinutes(10);
        boolean escalatedRecently = alertRepository.existsByUserIdAndDocumentIdAndAlertTypeSince(
                userId, documentId, AlertType.ESCALATION_SUPER_ADMIN, recentThreshold);
        if (escalatedRecently) {
            log.debug("⏭️ Super admin escalation already sent for doc {} user {} in last 10 minutes", documentId, userId);
            return true;
        }
        return false;
    }

    private void clearProcessedSets() {
        processedReminders.clear();
        processedDeptEscalations.clear();
        processedSuperEscalations.clear();
        processedViolations.clear();
    }

    // Status methods

        public boolean isSchedulerRunning() {
        return isRunning.get();
    }

        public String getLastRunTimestamp() {
        return lastRunTimestamp;
    }

    @Transactional
    public void triggerManualCheck() {
        log.info("🔧 Manual compliance check triggered");
        processComplianceChecks();
    }
}
