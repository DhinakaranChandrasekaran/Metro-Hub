package com.metrohub.repositories;

import com.metrohub.models.AuditLog;
import com.metrohub.models.AuditLog.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    

    Page<AuditLog> findByUser_Id(Long userId, Pageable pageable);

    

    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);

    

    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);

    

    Page<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    

    List<AuditLog> findTop100ByOrderByTimestampDesc();
}
