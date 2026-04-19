package com.metrohub.services;

import com.metrohub.dto.DocumentDTOs.DocumentReminderDTO;
import com.metrohub.dto.DocumentDTOs.DocumentReminderRequestDTO;
import com.metrohub.models.Alert;
import com.metrohub.models.Document;
import com.metrohub.models.DocumentReminder;
import com.metrohub.models.User;
import com.metrohub.repositories.DocumentAcknowledgementRepository;
import com.metrohub.repositories.DocumentReminderRepository;
import com.metrohub.repositories.DocumentRepository;
import com.metrohub.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentReminderService {

    private static final Logger log = LoggerFactory.getLogger(DocumentReminderService.class);

    private final DocumentReminderRepository reminderRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentAcknowledgementRepository acknowledgementRepository;
    private final NotificationService notificationService;

    // ============================================
    // CREATE REMINDER
    // ============================================

    @Transactional
    public DocumentReminderDTO createReminder(DocumentReminderRequestDTO request) {
        log.info("📅 Creating reminder for document ID: {}", request.getDocumentId());

        // Get document
        Optional<Document> docOpt = documentRepository.findById(request.getDocumentId());
        if (docOpt.isEmpty()) {
            throw new RuntimeException("Document not found with ID: " + request.getDocumentId());
        }
        Document document = docOpt.get();

        // Get target user if specified
        User targetUser = null;
        if (request.getTargetUserId() != null) {
            Optional<User> userOpt = userRepository.findById(request.getTargetUserId());
            if (userOpt.isEmpty()) {
                throw new RuntimeException("User not found with ID: " + request.getTargetUserId());
            }
            targetUser = userOpt.get();
        }

        // Get current user
        User currentUser = getCurrentUser();

        // Validate recurring settings
        if (Boolean.TRUE.equals(request.getIsRecurring()) && request.getRecurrenceHours() == null) {
            throw new IllegalArgumentException("Recurrence hours required for recurring reminders");
        }

        // Build reminder
        DocumentReminder reminder = DocumentReminder.builder()
                .document(document)
                .targetUser(targetUser)
                .reminderDate(request.getReminderDate())
                .message(request.getMessage())
                .reminderType(request.getReminderType())
                .isRecurring(request.getIsRecurring())
                .recurrenceHours(request.getRecurrenceHours())
                .maxOccurrences(request.getMaxOccurrences())
                .createdBy(currentUser)
                .build();

        DocumentReminder saved = reminderRepository.save(reminder);
        log.info("✅ Reminder created with ID: {} for document: {}", saved.getId(), document.getFileName());

        return toDTO(saved);
    }

    // ============================================
    // GET REMINDERS
    // ============================================

    @Transactional(readOnly = true)
    public List<DocumentReminderDTO> getRemindersForDocument(Long documentId) {
        return reminderRepository.findByDocument_IdOrderByReminderDateAsc(documentId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentReminderDTO> getActiveRemindersForDocument(Long documentId) {
        return reminderRepository.findActiveRemindersByDocumentId(documentId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DocumentReminderDTO getReminderById(Long id) {
        Optional<DocumentReminder> reminderOpt = reminderRepository.findById(id);
        if (reminderOpt.isEmpty()) {
            throw new RuntimeException("Reminder not found with ID: " + id);
        }
        DocumentReminder reminder = reminderOpt.get();
        return toDTO(reminder);
    }

    // ============================================
    // CANCEL REMINDERS
    // ============================================

    @Transactional
    public void cancelReminder(Long id) {
        log.info("🚫 Cancelling reminder ID: {}", id);
        reminderRepository.deactivate(id);
    }

    @Transactional
    public void cancelAllRemindersForDocument(Long documentId) {
        log.info("🚫 Cancelling all reminders for document ID: {}", documentId);
        reminderRepository.deactivateAllForDocument(documentId);
    }

    // ============================================
    // PROCESS REMINDERS (SCHEDULER)
    // ============================================

    @Scheduled(fixedRate = 60000)  // Run every minute
    @Transactional
    public int processPendingReminders() {
        List<DocumentReminder> pendingReminders = reminderRepository.findPendingReminders(LocalDateTime.now());
        
        if (pendingReminders.isEmpty()) {
            return 0;
        }

        log.info("⏰ Processing {} pending reminders", pendingReminders.size());
        int sentCount = 0;

        for (DocumentReminder reminder : pendingReminders) {
            try {
                sendReminder(reminder);
                
                // Mark as sent
                reminderRepository.markAsSent(reminder.getId(), LocalDateTime.now());
                sentCount++;

                // Handle recurring reminders
                if (Boolean.TRUE.equals(reminder.getIsRecurring())) {
                    rescheduleRecurringReminder(reminder);
                }
            } catch (Exception e) {
                log.error("❌ Failed to send reminder {}: {}", reminder.getId(), e.getMessage());
            }
        }

        log.info("✅ Sent {} reminders", sentCount);
        return sentCount;
    }

    // ============================================
    // SEND IMMEDIATE REMINDER
    // ============================================

    @Transactional
    public int sendImmediateReminder(Long documentId, String customMessage) {
        log.info("📧 Sending immediate reminder for document ID: {}", documentId);

        Optional<Document> docOpt = documentRepository.findById(documentId);
        if (docOpt.isEmpty()) {
            throw new RuntimeException("Document not found with ID: " + documentId);
        }
        Document document = docOpt.get();

        // Get users who haven't acknowledged
        List<User> pendingUsers = acknowledgementRepository.findUsersNotAcknowledged(documentId);

        if (pendingUsers.isEmpty()) {
            log.info("✅ No pending users for document ID: {}", documentId);
            return 0;
        }

        String message = customMessage != null ? customMessage :
                String.format("⏰ REMINDER: Please acknowledge document '%s'", document.getFileName());

        int sentCount = 0;
        for (User user : pendingUsers) {
            try {
                notificationService.sendNotification(user, document, 
                        Alert.AlertType.ACKNOWLEDGEMENT_REQUIRED, message);
                sentCount++;
                log.info("📧 Reminder sent to: {}", user.getEmail());
            } catch (Exception e) {
                log.warn("⚠️ Failed to send reminder to {}: {}", user.getEmail(), e.getMessage());
            }
        }

        return sentCount;
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private void sendReminder(DocumentReminder reminder) {
        Document document = reminder.getDocument();
        String message = reminder.getMessage() != null ? reminder.getMessage() :
                String.format("⏰ REMINDER: Please acknowledge document '%s'", document.getFileName());

        if (reminder.getTargetUser() != null) {
            // Send to specific user
            notificationService.sendNotification(reminder.getTargetUser(), document,
                    Alert.AlertType.ACKNOWLEDGEMENT_REQUIRED, message);
            log.info("📧 Reminder sent to: {}", reminder.getTargetUser().getEmail());
        } else {
            // Send to all pending users
            List<User> pendingUsers = acknowledgementRepository.findUsersNotAcknowledged(document.getId());
            for (User user : pendingUsers) {
                try {
                    notificationService.sendNotification(user, document,
                            Alert.AlertType.ACKNOWLEDGEMENT_REQUIRED, message);
                    log.info("📧 Reminder sent to: {}", user.getEmail());
                } catch (Exception e) {
                    log.warn("⚠️ Failed to send reminder to {}: {}", user.getEmail(), e.getMessage());
                }
            }
        }
    }

    private void rescheduleRecurringReminder(DocumentReminder reminder) {
        // Check if max occurrences reached
        if (reminder.getMaxOccurrences() != null && 
            reminder.getOccurrenceCount() >= reminder.getMaxOccurrences()) {
            log.info("🛑 Recurring reminder {} reached max occurrences", reminder.getId());
            reminderRepository.deactivate(reminder.getId());
            return;
        }

        // Create next occurrence
        DocumentReminder nextReminder = DocumentReminder.builder()
                .document(reminder.getDocument())
                .targetUser(reminder.getTargetUser())
                .reminderDate(reminder.getReminderDate().plusHours(reminder.getRecurrenceHours()))
                .message(reminder.getMessage())
                .reminderType(reminder.getReminderType())
                .isRecurring(true)
                .recurrenceHours(reminder.getRecurrenceHours())
                .maxOccurrences(reminder.getMaxOccurrences())
                .occurrenceCount(reminder.getOccurrenceCount() + 1)
                .createdBy(reminder.getCreatedBy())
                .build();

        reminderRepository.save(nextReminder);
        log.info("🔄 Rescheduled recurring reminder for: {}", nextReminder.getReminderDate());
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            return userRepository.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }

    private DocumentReminderDTO toDTO(DocumentReminder reminder) {
        return DocumentReminderDTO.builder()
                .id(reminder.getId())
                .documentId(reminder.getDocument().getId())
                .documentName(reminder.getDocument().getFileName())
                .targetUserId(reminder.getTargetUser() != null ? reminder.getTargetUser().getId() : null)
                .targetUserEmail(reminder.getTargetUser() != null ? reminder.getTargetUser().getEmail() : null)
                .reminderDate(reminder.getReminderDate())
                .message(reminder.getMessage())
                .reminderType(reminder.getReminderType())
                .isSent(reminder.getIsSent())
                .sentAt(reminder.getSentAt())
                .isRecurring(reminder.getIsRecurring())
                .recurrenceHours(reminder.getRecurrenceHours())
                .maxOccurrences(reminder.getMaxOccurrences())
                .occurrenceCount(reminder.getOccurrenceCount())
                .createdByEmail(reminder.getCreatedBy() != null ? reminder.getCreatedBy().getEmail() : null)
                .createdAt(reminder.getCreatedAt())
                .isActive(reminder.getIsActive())
                .build();
    }
}
