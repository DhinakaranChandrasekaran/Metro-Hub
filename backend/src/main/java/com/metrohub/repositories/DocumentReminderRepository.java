package com.metrohub.repositories;

import com.metrohub.models.DocumentReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DocumentReminderRepository extends JpaRepository<DocumentReminder, Long> {

    @Query("SELECT r FROM DocumentReminder r " +
           "WHERE r.isActive = true " +
           "AND r.isSent = false " +
           "AND r.reminderDate <= :now")
    List<DocumentReminder> findPendingReminders(@Param("now") LocalDateTime now);

    List<DocumentReminder> findByDocument_IdOrderByReminderDateAsc(Long documentId);

    @Query("SELECT r FROM DocumentReminder r " +
           "WHERE r.document.id = :documentId " +
           "AND r.isActive = true " +
           "ORDER BY r.reminderDate ASC")
    List<DocumentReminder> findActiveRemindersByDocumentId(@Param("documentId") Long documentId);

    @Query("SELECT r FROM DocumentReminder r " +
           "WHERE r.targetUser.id = :userId " +
           "AND r.isActive = true " +
           "AND r.isSent = false " +
           "ORDER BY r.reminderDate ASC")
    List<DocumentReminder> findPendingRemindersForUser(@Param("userId") Long userId);

    List<DocumentReminder> findByCreatedBy_IdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COUNT(r) FROM DocumentReminder r " +
           "WHERE r.document.id = :documentId " +
           "AND r.isActive = true " +
           "AND r.isSent = false")
    long countPendingReminders(@Param("documentId") Long documentId);

    @Modifying
    @Query("UPDATE DocumentReminder r SET r.isSent = true, r.sentAt = :sentAt, " +
           "r.occurrenceCount = r.occurrenceCount + 1 WHERE r.id = :id")
    void markAsSent(@Param("id") Long id, @Param("sentAt") LocalDateTime sentAt);

    @Modifying
    @Query("UPDATE DocumentReminder r SET r.isActive = false WHERE r.id = :id")
    void deactivate(@Param("id") Long id);

    @Modifying
    @Query("UPDATE DocumentReminder r SET r.isActive = false WHERE r.document.id = :documentId")
    void deactivateAllForDocument(@Param("documentId") Long documentId);

    @Query("SELECT r FROM DocumentReminder r " +
           "WHERE r.isRecurring = true " +
           "AND r.isActive = true " +
           "AND r.isSent = true " +
           "AND (r.maxOccurrences IS NULL OR r.occurrenceCount < r.maxOccurrences)")
    List<DocumentReminder> findRecurringRemindersToReschedule();
}