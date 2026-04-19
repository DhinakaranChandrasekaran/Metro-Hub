package com.metrohub.services;

import com.metrohub.dto.AlertDTO;
import com.metrohub.models.Alert;
import com.metrohub.models.Alert.AlertType;
import com.metrohub.models.Alert.NotificationChannel;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import com.metrohub.models.Department;
import com.metrohub.models.Document;
import com.metrohub.models.User;
import com.metrohub.repositories.AlertRepository;
import com.metrohub.repositories.UserRepository;
import com.metrohub.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import com.metrohub.util.EmailTemplateBuilder;

import jakarta.mail.internet.MimeMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${metrohub.notifications.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${metrohub.notifications.email.from:noreply@metrohub.in}")
    private String emailFrom;

    @Value("${metrohub.notifications.sms.enabled:true}")
    private boolean smsEnabled;

    @Value("${metrohub.notifications.twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${metrohub.notifications.twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${metrohub.notifications.sms.from-number:}")
    private String twilioPhoneNumber;

    @Value("${metrohub.notifications.sms.mock-enabled:true}")
    private boolean smsMockEnabled;

    @Transactional
    public void sendNotification(User user, Document document, AlertType alertType, String message) {
        log.info("📢 Sending notification to user: {} - Type: {}", user.getEmail(), alertType);

        // 1. Create dashboard alert (always)
        Alert alert = createDashboardAlert(user, document, alertType, message);
        Long alertId = alert != null ? alert.getId() : null;

        // 2. Send email (async if enabled)
        if (emailEnabled && user.getEmail() != null) {
            sendEmailAsync(user, document, alertType, message, alertId);
        }

        // 3. Send SMS (async if enabled)
        if (smsEnabled && user.getPhoneNumber() != null) {
            sendSmsAsync(user, document, alertType, message, alertId);
        }
    }

    @Transactional
    public void notifyDepartmentOnUpload(Document document) {
        log.info("📢 Notifying department of new document: {}", document.getFileName());

        Department department = document.getDepartment();
        if (department == null) {
            log.warn("Document has no department, skipping notifications");
            return;
        }

        String message = String.format(
                "📄 New document uploaded: '%s' - Please review and acknowledge",
                document.getFileName());

        AlertType alertType = document.getPriority() == Document.Priority.HIGH ? AlertType.HIGH_PRIORITY_UPLOAD
                : AlertType.NEW_DOCUMENT_UPLOADED;

        // Get all department admins and upload admins to notify
        List<User> recipients = userRepository.findNotificationRecipientsForDepartment(department.getId());

        // Create and send individual notifications to each admin
        for (User recipient : recipients) {
            try {
                // Send notification via all channels (Dashboard + Email + SMS)
                sendNotification(recipient, document, alertType, message);
                log.info("✉️ Upload notification sent to {} (Dashboard + Email + SMS)", recipient.getEmail());
            } catch (Exception e) {
                log.error("Failed to send upload notification to {}: {}", recipient.getEmail(), e.getMessage());
            }
        }

        // Also create department-level alert for dashboard
        Alert alert = Alert.builder()
                .document(document)
                .department(department)
                .alertType(alertType)
                .notificationChannel(NotificationChannel.DASHBOARD)
                .message(message)
                .isRead(false)
                .emailSent(false)
                .smsSent(false)
                .slaReminderHours(document.getSlaReminderHours())
                .slaDeptAdminEscalationHours(document.getSlaDeptAdminEscalationHours())
                .slaSuperAdminEscalationHours(document.getSlaSuperAdminEscalationHours())
                .slaViolationHours(document.getSlaViolationHours())
                .isManualSla(document.getIsSlaManual())
                .build();

        Alert savedAlert = alertRepository.save(alert);
        log.info("✅ Single department notification created for {} - Alert ID: {}", department.getName(), savedAlert.getId());
    }

    @Transactional
    public void sendHighPriorityAlert(Document document) {
        log.info("🔴 Sending high priority alert for document: {}", document.getFileName());

        Department department = document.getDepartment();
        if (department == null) {
            log.warn("Document has no department, skipping high priority alert");
            return;
        }

        List<User> recipients = userRepository.findNotificationRecipientsForDepartment(department.getId());

        String message = String.format(
                "🔴 HIGH PRIORITY: Document '%s' requires immediate attention!",
                document.getFileName());

        for (User recipient : recipients) {
            try {
                sendNotification(recipient, document, AlertType.HIGH_PRIORITY_UPLOAD, message);
            } catch (Exception e) {
                log.error("Failed to send high priority alert to {}: {}", recipient.getEmail(), e.getMessage());
            }
        }
    }

    @Transactional
    public void sendAcknowledgementRequired(User user, Document document) {
        String message = String.format(
                "📝 Acknowledgement required for document: '%s'",
                document.getFileName());
        sendNotification(user, document, AlertType.ACKNOWLEDGEMENT_REQUIRED, message);
    }

    @Transactional
    public void sendDeadlineApproaching(Document document, long daysRemaining) {
        log.info("⏰ Sending deadline approaching alert for document: {}", document.getFileName());

        Department department = document.getDepartment();
        if (department == null)
            return;

        List<User> recipients = userRepository.findNotificationRecipientsForDepartment(department.getId());

        String message = String.format(
                "⏰ Deadline approaching for '%s' - %d day%s remaining",
                document.getFileName(),
                daysRemaining,
                daysRemaining == 1 ? "" : "s");

        for (User recipient : recipients) {
            try {
                sendNotification(recipient, document, AlertType.DEADLINE_APPROACHING, message);
            } catch (Exception e) {
                log.error("Failed to send deadline alert to {}: {}", recipient.getEmail(), e.getMessage());
            }
        }
    }

    @Transactional
    public void sendDeadlineOverdue(Document document) {
        log.info("❌ Sending deadline overdue alert for document: {}", document.getFileName());

        Department department = document.getDepartment();
        if (department == null)
            return;

        List<User> recipients = userRepository.findNotificationRecipientsForDepartment(department.getId());

        String message = String.format(
                "❌ OVERDUE: Document '%s' deadline has passed - Immediate action required",
                document.getFileName());

        for (User recipient : recipients) {
            try {
                sendNotification(recipient, document, AlertType.DEADLINE_OVERDUE, message);
            } catch (Exception e) {
                log.error("Failed to send overdue alert to {}: {}", recipient.getEmail(), e.getMessage());
            }
        }
    }

        public boolean sendEmail(String to, String subject, String body) {
        if (!emailEnabled) {
            log.debug("Email disabled, skipping email to: {}", to);
            return false;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(emailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // true = HTML content

            mailSender.send(mimeMessage);
            log.info("✉️ HTML email sent to: {}", to);
            return true;
        } catch (Exception e) {
            log.error("❌ Failed to send email to {}: {}", to, e.getMessage());
            return false;
        }
    }

        public boolean sendSms(String phoneNumber, String message) {
        if (!smsEnabled) {
            log.debug("SMS disabled, skipping SMS to: {}", phoneNumber);
            return false;
        }

        // Check if real Twilio credentials are configured
        if (twilioAccountSid.isEmpty() || twilioAuthToken.isEmpty()) {
            // Use mock SMS for testing/development
            if (smsMockEnabled) {
                log.info("📱 [MOCK SMS] To: {} | Message: {}", phoneNumber, message);
                return true;
            }
            log.warn("Twilio credentials not configured and mock SMS disabled");
            return false;
        }

        try {
            // Initialize Twilio
            Twilio.init(twilioAccountSid, twilioAuthToken);

            // Send SMS via Twilio
            Message twilioMessage = Message.creator(
                    new PhoneNumber(phoneNumber),
                    new PhoneNumber(twilioPhoneNumber),
                    message).create();

            log.info("📱 SMS sent to: {} - SID: {}", phoneNumber, twilioMessage.getSid());
            return true;
        } catch (Exception e) {
            log.error("❌ Failed to send SMS to {}: {}", phoneNumber, e.getMessage());
            return false;
        }
    }

    @Transactional
    public Alert createDashboardAlert(User user, Document document, AlertType alertType, String message) {
        // Check for duplicate alert in last 24 hours
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        if (alertRepository.existsByUserIdAndDocumentIdAndAlertTypeSince(
                user.getId(), document.getId(), alertType, since)) {
            log.debug("Alert already exists for user {} and document {}, skipping",
                    user.getId(), document.getId());
            return null;
        }

        Alert alert = Alert.builder()
                .targetUser(user)
                .document(document)
                .department(document.getDepartment())
                .alertType(alertType)
                .notificationChannel(NotificationChannel.DASHBOARD)
                .message(message)
                .isRead(false)
                .emailSent(false)
                .smsSent(false)
                // Include SLA timings from document if available
                .slaReminderHours(document.getSlaReminderHours())
                .slaDeptAdminEscalationHours(document.getSlaDeptAdminEscalationHours())
                .slaSuperAdminEscalationHours(document.getSlaSuperAdminEscalationHours())
                .slaViolationHours(document.getSlaViolationHours())
                .isManualSla(document.getIsSlaManual())
                .build();

        Alert savedAlert = alertRepository.save(alert);
        log.debug("📋 Dashboard alert created: {}", savedAlert.getId());

        return savedAlert;
    }

    @Async
    protected void sendEmailAsync(User user, Document document, AlertType alertType, String message, Long alertId) {
        try {
            String subject = buildEmailSubject(alertType, document);
            String body = buildEmailBody(user, document, alertType, message);

            if (sendEmail(user.getEmail(), subject, body)) {
                alertRepository.markEmailSent(alertId);
            }
        } catch (Exception e) {
            log.error("Async email failed: {}", e.getMessage());
        }
    }

    @Async
    protected void sendSmsAsync(User user, Document document, AlertType alertType, String message, Long alertId) {
        try {
            String smsMessage = buildSmsMessage(user, document, alertType, message);
            if (sendSms(user.getPhoneNumber(), smsMessage)) {
                alertRepository.markSmsSent(alertId);
            }
        } catch (Exception e) {
            log.error("Async SMS failed: {}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    public void processPendingEmails() {
        if (!emailEnabled)
            return;

        List<Alert> pendingAlerts = alertRepository.findPendingEmailAlerts();
        log.debug("Processing {} pending email notifications", pendingAlerts.size());

        for (Alert alert : pendingAlerts) {
            try {
                User user = alert.getTargetUser();
                Document document = alert.getDocument();

                String subject = buildEmailSubject(alert.getAlertType(), document);
                String body = buildEmailBody(user, document, alert.getAlertType(), alert.getMessage());

                if (sendEmail(user.getEmail(), subject, body)) {
                    alertRepository.markEmailSent(alert.getId());
                }
            } catch (Exception e) {
                log.error("Failed to process pending email for alert {}: {}", alert.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    public void processPendingSms() {
        if (!smsEnabled)
            return;

        List<Alert> pendingAlerts = alertRepository.findPendingSmsAlerts();
        log.debug("Processing {} pending SMS notifications", pendingAlerts.size());

        for (Alert alert : pendingAlerts) {
            try {
                User user = alert.getTargetUser();
                Document document = alert.getDocument();
                String smsMessage = buildSmsMessage(user, document, alert.getAlertType(), alert.getMessage());

                if (sendSms(user.getPhoneNumber(), smsMessage)) {
                    alertRepository.markSmsSent(alert.getId());
                }
            } catch (Exception e) {
                log.error("Failed to process pending SMS for alert {}: {}", alert.getId(), e.getMessage());
            }
        }
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private String buildEmailSubject(AlertType alertType, Document document) {
        String prefix = switch (alertType) {
            case HIGH_PRIORITY_UPLOAD -> "🔴 HIGH PRIORITY";
            case DEADLINE_OVERDUE -> "❌ OVERDUE";
            case DEADLINE_APPROACHING -> "⏰ DEADLINE";
            case DEADLINE_TODAY -> "📅 DUE TODAY";
            case NEW_DOCUMENT_UPLOADED -> "📄 NEW DOCUMENT";
            case ACKNOWLEDGEMENT_REQUIRED -> "📝 ACTION REQUIRED";
            default -> "📢 NOTIFICATION";
        };

        return String.format("[MetroHub] %s: %s", prefix,
                document != null ? document.getFileName() : "System Alert");
    }

    private String buildEmailBody(User user, Document document, AlertType alertType, String message) {
        String fileName = document != null ? document.getFileName() : "System Alert";
        String department = document != null && document.getDepartment() != null
                ? document.getDepartment().getName()
                : "N/A";
        String priority = document != null && document.getPriority() != null
                ? document.getPriority().name()
                : "MEDIUM";
        String docType = document != null && document.getDocumentType() != null
                ? document.getDocumentType().name()
                : "Document";

        String bodyHtml = switch (alertType) {
            case NEW_DOCUMENT_UPLOADED, HIGH_PRIORITY_UPLOAD ->
                EmailTemplateBuilder.newDocumentBody(fileName, department, priority, docType);
            case DEADLINE_APPROACHING, DEADLINE_TODAY, DEADLINE_OVERDUE ->
                EmailTemplateBuilder.deadlineBody(fileName, department, 24);
            default ->
                EmailTemplateBuilder.newDocumentBody(fileName, department, priority, docType);
        };

        return EmailTemplateBuilder.buildEmail(
                user.getName(), message, bodyHtml, alertType.name());
    }

    private String buildSmsMessage(User user, Document document, AlertType alertType, String message) {
        String prefix = switch (alertType) {
            case HIGH_PRIORITY_UPLOAD -> "[HIGH PRIORITY]";
            case DEADLINE_OVERDUE -> "[OVERDUE]";
            case DEADLINE_APPROACHING -> "[DEADLINE]";
            case DEADLINE_TODAY -> "[DUE TODAY]";
            case NEW_DOCUMENT_UPLOADED -> "[NEW DOCUMENT]";
            case ACKNOWLEDGEMENT_REQUIRED -> "[ACTION REQUIRED]";
            default -> "[NOTIFICATION]";
        };

        String fileName = document != null ? document.getFileName() : "N/A";
        String department = document != null && document.getDepartment() != null
                ? document.getDepartment().getName() : "N/A";
        String priority = document != null && document.getPriority() != null
                ? document.getPriority().name() : "MEDIUM";
        String docType = document != null && document.getDocumentType() != null
                ? document.getDocumentType().name() : "Document";

        String sms = String.format(
                "MetroHub %s\nDoc: %s\nDept: %s\nType: %s\nPriority: %s\nDear %s, please login to MetroHub to review and take action.\nMetro Rail Authority | Helpline: 1800-XXX-XXXX",
                prefix, fileName, department, docType, priority, user.getName());

        return sms;
    }

    // ============================================================
    // PHASE 6: USER-SPECIFIC ALERT METHODS
    // ============================================================

        public Page<AlertDTO> getAlertsForCurrentUser(Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long departmentId = SecurityUtils.getCurrentDepartmentId();

        Page<Alert> alerts;

        // SUPER_ADMIN sees all alerts, others see their targeted alerts or department
        // alerts
        if (SecurityUtils.hasRole("SUPER_ADMIN")) {
            alerts = alertRepository.findAllByOrderByCreatedAtDesc(pageable);
        } else if (userId != null) {
            // Get alerts targeted at user or their department
            alerts = alertRepository.findByTargetUserIdOrDepartmentIdOrderByCreatedAtDesc(
                    userId, departmentId, pageable);
        } else {
            alerts = Page.empty(pageable);
        }

        return alerts.map(AlertDTO::fromEntity);
    }

        public Page<AlertDTO> getUnreadAlertsForCurrentUser(Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long departmentId = SecurityUtils.getCurrentDepartmentId();

        Page<Alert> alerts;

        if (SecurityUtils.hasRole("SUPER_ADMIN")) {
            alerts = alertRepository.findByIsReadFalseOrderByCreatedAtDesc(pageable);
        } else if (userId != null) {
            alerts = alertRepository.findUnreadByUserOrDepartment(userId, departmentId, pageable);
        } else {
            alerts = Page.empty(pageable);
        }

        return alerts.map(AlertDTO::fromEntity);
    }

        public long getUnreadCountForCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        Long departmentId = SecurityUtils.getCurrentDepartmentId();

        if (SecurityUtils.hasRole("SUPER_ADMIN")) {
            return alertRepository.countAllUnread();
        } else if (userId != null) {
            return alertRepository.countUnreadByUserOrDepartment(userId, departmentId);
        }

        return 0L;
    }

        public Page<AlertDTO> getAlertsByDepartment(Long departmentId, Pageable pageable) {
        Page<Alert> alerts = alertRepository.findByDepartmentIdOrderByCreatedAtDesc(departmentId, pageable);
        return alerts.map(AlertDTO::fromEntity);
    }

    @Transactional
    public int markAllAsReadForCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        Long departmentId = SecurityUtils.getCurrentDepartmentId();

        if (userId == null) {
            return 0;
        }

        // ALL users (including SUPER_ADMIN) mark only THEIR OWN alerts as read
        // SUPER_ADMIN does NOT mark other users' alerts
        return alertRepository.markAsReadByUserOrDepartment(userId, departmentId);
    }

    @Transactional
    public AlertDTO markAlertAsReadForCurrentUser(Long alertId) {
        Long userId = com.metrohub.security.SecurityUtils.getCurrentUserId();
        Long departmentId = com.metrohub.security.SecurityUtils.getCurrentDepartmentId();

        Optional<Alert> alertOpt = alertRepository.findById(alertId);
        if (alertOpt.isEmpty()) {
            throw new IllegalArgumentException("Alert not found: " + alertId);
        }

        Alert alert = alertOpt.get();

        // Check if alert belongs to current user or department
        Long alertUserId = alert.getTargetUser() != null ? alert.getTargetUser().getId() : null;
        Long alertDeptId = alert.getDepartment() != null ? alert.getDepartment().getId() : null;

        boolean isOwnAlert = (alertUserId != null && alertUserId.equals(userId)) ||
                             (alertDeptId != null && alertDeptId.equals(departmentId)) ||
                             com.metrohub.security.SecurityUtils.hasRole("SUPER_ADMIN");

        if (!isOwnAlert) {
            throw new org.springframework.security.access.AccessDeniedException("You can only mark your own alerts as read");
        }

        // Mark ONLY THIS USER'S alert as read (independent status per user)
        alert.setIsRead(true);
        Alert savedAlert = alertRepository.save(alert);
        log.info("✓ User {} marked their own alert {} as read (independent per user)", userId, alertId);

        // Return the updated alert
        return AlertDTO.fromEntity(savedAlert);
    }

        public long getPendingEmailCount() {
        return alertRepository.countPendingEmailAlerts();
    }

        public long getPendingSmsCount() {
        return alertRepository.countPendingSmsAlerts();
    }

        public Map<String, Object> sendTestNotification(Long userId, String channel, String message) {
        log.info("🧪 Sending test notification - userId: {}, channel: {}", userId, channel);

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        User user = userOpt.get();

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("userName", user.getName());
        result.put("email", user.getEmail());
        result.put("phoneNumber", user.getPhoneNumber());

        String channelUpper = channel.toUpperCase();

        // Dashboard notification
        if (channelUpper.equals("DASHBOARD") || channelUpper.equals("ALL")) {
            try {
                Alert alert = Alert.builder()
                        .targetUser(user)
                        .department(user.getDepartmentEntity())
                        .alertType(AlertType.NEW_DOCUMENT_UPLOADED)
                        .notificationChannel(NotificationChannel.DASHBOARD)
                        .message("🧪 TEST: " + message)
                        .isRead(false)
                        .build();
                Alert savedAlert = alertRepository.save(alert);
                result.put("dashboard", Map.of("success", true, "alertId", savedAlert.getId()));
                log.info("✅ Test dashboard alert created for user: {}", user.getEmail());
            } catch (Exception e) {
                log.error("❌ Failed to create dashboard alert: {}", e.getMessage(), e);
                result.put("dashboard",
                        Map.of("success", false, "error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
            }
        }

        // Email notification (non-transactional, catches its own exceptions)
        if (channelUpper.equals("EMAIL") || channelUpper.equals("ALL")) {
            if (user.getEmail() != null) {
                try {
                    String subject = "🧪 MetroHub Test Notification";
                    String body = String.format(
                            "Hello %s,\n\n" +
                                    "This is a test notification from MetroHub.\n\n" +
                                    "Message: %s\n\n" +
                                    "If you received this email, your email notifications are working correctly.\n\n" +
                                    "---\nMetroHub Document Management System",
                            user.getName(), message);

                    boolean emailSent = sendEmail(user.getEmail(), subject, body);
                    result.put("email", Map.of(
                            "success", emailSent,
                            "enabled", emailEnabled,
                            "to", user.getEmail()));
                } catch (Exception e) {
                    log.error("❌ Email error: {}", e.getMessage());
                    result.put("email", Map.of("success", false, "enabled", emailEnabled, "error",
                            e.getMessage() != null ? e.getMessage() : "Email failed"));
                }
            } else {
                result.put("email", Map.of("success", false, "error", "User has no email"));
            }
        }

        // SMS notification (non-transactional, catches its own exceptions)
        if (channelUpper.equals("SMS") || channelUpper.equals("ALL")) {
            if (user.getPhoneNumber() != null) {
                try {
                    String smsMessage = String.format(
                            "MetroHub TEST: %s - If you received this, SMS notifications are working.",
                            message);

                    boolean smsSent = sendSms(user.getPhoneNumber(), smsMessage);
                    result.put("sms", Map.of(
                            "success", smsSent,
                            "enabled", smsEnabled,
                            "to", user.getPhoneNumber()));
                } catch (Exception e) {
                    log.error("❌ SMS error: {}", e.getMessage());
                    result.put("sms", Map.of("success", false, "enabled", smsEnabled, "error",
                            e.getMessage() != null ? e.getMessage() : "SMS failed"));
                }
            } else {
                result.put("sms", Map.of("success", false, "error", "User has no phone number"));
            }
        }

        log.info("🧪 Test notification result: {}", result);
        return result;
    }
}
