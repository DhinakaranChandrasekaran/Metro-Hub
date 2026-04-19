package com.metrohub.services;

import com.metrohub.dto.ReportDTOs.*;
import com.metrohub.models.*;
import com.metrohub.repositories.*;
import com.metrohub.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // Enforce read-only transactions
public class AuditReportService {

    private static final Logger log = LoggerFactory.getLogger(AuditReportService.class);

    private final DocumentRepository documentRepository;
    private final DocumentAcknowledgementRepository acknowledgementRepository;
    private final ComplianceViolationRepository violationRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final AlertRepository alertRepository;

    // ============================================
    // COMPLIANCE SUMMARY DASHBOARD
    // ============================================

    

    public ComplianceSummaryDTO getComplianceSummary(
            LocalDate startDate,
            LocalDate endDate,
            Long departmentId,
            String documentType
    ) {
        log.info("📊 Generating compliance summary - startDate={}, endDate={}, deptId={}, docType={}",
                startDate, endDate, departmentId, documentType);

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : LocalDateTime.now().minusMonths(12);
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : LocalDateTime.now();

        // Document metrics
        long totalDocuments = documentRepository.countActiveDocuments();
        long documentsInRange = documentRepository.countDocumentsInDateRange(startDateTime, endDateTime);
        long safetyDocs = documentRepository.countSafetyDocuments();
        
        List<Object[]> docsByType = documentRepository.countDocumentsGroupedByType();
        long circularDocs = 0, policyDocs = 0, otherDocs = 0;
        for (Object[] row : docsByType) {
            if (row[0] != null) {
                String type = row[0].toString();
                long count = ((Number) row[1]).longValue();
                if ("SAFETY_CIRCULAR".equals(type)) circularDocs = count;
                else if ("POLICY".equals(type)) policyDocs = count;
                else otherDocs += count;
            }
        }

        // Acknowledgement metrics
        long totalAcks = acknowledgementRepository.countTotalAcknowledgements();
        long acksInRange = acknowledgementRepository.countAcknowledgementsInDateRange(startDateTime, endDateTime);
        long lateAcks = violationRepository.countLateAcknowledged();
        
        // Calculate pending acknowledgements
        long expectedAcks = acknowledgementRepository.countTotalExpectedAcknowledgements();
        long pendingAcks = Math.max(0, expectedAcks - totalAcks);
        
        double ackRate = expectedAcks > 0 ? (totalAcks * 100.0 / expectedAcks) : 100.0;

        // Violation metrics
        long totalViolations = violationRepository.count();
        long resolvedViolations = violationRepository.countAllResolved();
        long unresolvedViolations = violationRepository.countAllPending();
        long criticalViolations = violationRepository.countCriticalViolations();
        long highViolations = violationRepository.countHighSeverityViolations();
        long mediumViolations = violationRepository.countMediumSeverityViolations();
        long violationsInRange = violationRepository.countViolationsInDateRange(startDateTime, endDateTime);

        // Compliance percentages
        double overallCompliance = totalViolations > 0 ? 
                ((resolvedViolations + (totalDocuments - totalViolations)) * 100.0 / totalDocuments) : 100.0;
        overallCompliance = Math.min(100.0, Math.max(0.0, overallCompliance));

        long unresolvedSafetyViolations = violationRepository.countUnresolvedSafetyViolations();
        double safetyCompliance = safetyDocs > 0 ? 
                ((safetyDocs - unresolvedSafetyViolations) * 100.0 / safetyDocs) : 100.0;

        double nonSafetyCompliance = (totalDocuments - safetyDocs) > 0 ?
                ((totalDocuments - safetyDocs - (unresolvedViolations - unresolvedSafetyViolations)) * 100.0 / (totalDocuments - safetyDocs)) : 100.0;

        // Get department filter info
        String deptName = null;
        if (departmentId != null) {
            deptName = departmentRepository.findById(departmentId)
                    .map(Department::getName)
                    .orElse(null);
        }

        return ComplianceSummaryDTO.builder()
                .totalDocuments(totalDocuments)
                .documentsThisMonth(documentsInRange)
                .documentsThisWeek(documentRepository.countDocumentsInDateRange(
                        LocalDateTime.now().minusWeeks(1), LocalDateTime.now()))
                .safetyDocuments(safetyDocs)
                .circularDocuments(circularDocs)
                .policyDocuments(policyDocs)
                .otherDocuments(otherDocs)
                .totalAcknowledgements(totalAcks)
                .pendingAcknowledgements(pendingAcks)
                .acknowledgementsThisMonth(acksInRange)
                .lateAcknowledgements(lateAcks)
                .acknowledgementRate(Math.round(ackRate * 100.0) / 100.0)
                .totalViolations(totalViolations)
                .resolvedViolations(resolvedViolations)
                .unresolvedViolations(unresolvedViolations)
                .criticalViolations(criticalViolations)
                .highViolations(highViolations)
                .mediumViolations(mediumViolations)
                .violationsThisMonth(violationsInRange)
                .overallCompliancePercentage(Math.round(overallCompliance * 100.0) / 100.0)
                .safetyCompliancePercentage(Math.round(safetyCompliance * 100.0) / 100.0)
                .nonSafetyCompliancePercentage(Math.round(nonSafetyCompliance * 100.0) / 100.0)
                .filterStartDate(startDateTime)
                .filterEndDate(endDateTime)
                .filterDepartmentId(departmentId)
                .filterDepartmentName(deptName)
                .filterDocumentType(documentType)
                .generatedAt(LocalDateTime.now())
                .generatedBy(SecurityUtils.getCurrentUserEmail())
                .reportPeriod(formatDateRange(startDateTime, endDateTime))
                .build();
    }

    // ============================================
    // DEPARTMENT-WISE COMPLIANCE REPORT
    // ============================================

    

    public List<DepartmentComplianceDTO> getDepartmentComplianceReport(
            LocalDate startDate,
            LocalDate endDate,
            String sortBy,
            String sortDirection
    ) {
        log.info("📊 Generating department compliance report");

        List<Department> departments = departmentRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        List<DepartmentComplianceDTO> reports = new ArrayList<>();

        @SuppressWarnings("unused") // Available for date-filtered queries
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : LocalDateTime.now().minusMonths(12);
        @SuppressWarnings("unused") // Available for date-filtered queries
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : LocalDateTime.now();
        LocalDateTime lastMonthStart = LocalDateTime.now().minusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0);
        LocalDateTime lastMonthEnd = LocalDateTime.now().withDayOfMonth(1).minusDays(1).withHour(23).withMinute(59);

        for (Department dept : departments) {
            Long deptId = dept.getId();

            // User metrics
            long totalUsers = userRepository.countByDepartmentId(deptId);
            List<User> activeUsers = userRepository.findByDepartmentIdAndActive(deptId);
            long usersWithViolations = violationRepository.countPendingByDepartmentId(deptId);
            
            // Find repeat defaulters (3+ violations in 6 months)
            LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
            List<Object[]> repeatDefaulterData = violationRepository.findRepeatDefaulters(sixMonthsAgo, 3);
            long repeatDefaulters = countRepeatDefaultersForDepartment(repeatDefaulterData, deptId);

            // Document metrics
            long docsReceived = documentRepository.countByDepartmentId(deptId);
            long docsAcked = acknowledgementRepository.countByDepartmentId(deptId);

            // Acknowledgement metrics
            long pendingAcks = Math.max(0, (docsReceived * totalUsers) - docsAcked);
            long lateAcks = violationRepository.countLateAcknowledgedByDepartmentId(deptId);
            double ackRate = (docsReceived * totalUsers) > 0 ? 
                    (docsAcked * 100.0 / (docsReceived * totalUsers)) : 100.0;

            // Violation metrics
            long totalViolations = violationRepository.countByDepartment_Id(deptId);
            long resolvedViolations = violationRepository.countResolvedByDepartmentId(deptId);
            long unresolvedViolations = violationRepository.countPendingByDepartmentId(deptId);
            long criticalViolations = violationRepository.countCriticalByDepartmentId(deptId);
            long highViolations = violationRepository.countHighByDepartmentId(deptId);
            long mediumViolations = violationRepository.countMediumByDepartmentId(deptId);

            // Trend data
            long violationsLastMonth = violationRepository.countViolationsByDepartmentInDateRange(
                    deptId, lastMonthStart, lastMonthEnd);
            long violationsThisMonth = violationRepository.countViolationsByDepartmentInDateRange(
                    deptId, LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0), LocalDateTime.now());
            double violationTrend = violationsLastMonth > 0 ? 
                    ((violationsThisMonth - violationsLastMonth) * 100.0 / violationsLastMonth) : 0.0;

            // Compliance score (weighted calculation)
            double complianceScore = calculateComplianceScore(
                    ackRate, unresolvedViolations, totalViolations, criticalViolations);
            String riskLevel = calculateRiskLevel(complianceScore);

            DepartmentComplianceDTO dto = DepartmentComplianceDTO.builder()
                    .departmentId(deptId)
                    .departmentName(dept.getName())
                    .departmentCode(dept.getCode())
                    .departmentHead(dept.getHeadName())
                    .departmentEmail(dept.getContactEmail())
                    .totalUsers(totalUsers)
                    .activeUsers((long) activeUsers.size())
                    .usersWithViolations(usersWithViolations)
                    .repeatDefaulters(repeatDefaulters)
                    .documentsReceived(docsReceived)
                    .documentsAcknowledged(docsAcked)
                    .documentsPending(Math.max(0, docsReceived - docsAcked))
                    .totalAcknowledgementsRequired(docsReceived * totalUsers)
                    .totalAcknowledgementsDone(docsAcked)
                    .pendingAcknowledgements(pendingAcks)
                    .lateAcknowledgements(lateAcks)
                    .acknowledgementRate(Math.round(ackRate * 100.0) / 100.0)
                    .totalViolations(totalViolations)
                    .resolvedViolations(resolvedViolations)
                    .unresolvedViolations(unresolvedViolations)
                    .criticalViolations(criticalViolations)
                    .highViolations(highViolations)
                    .mediumViolations(mediumViolations)
                    .complianceScore(Math.round(complianceScore * 100.0) / 100.0)
                    .riskLevel(riskLevel)
                    .violationsLastMonth(violationsLastMonth)
                    .violationsThisMonth(violationsThisMonth)
                    .violationTrend(Math.round(violationTrend * 100.0) / 100.0)
                    .dataAsOf(LocalDateTime.now())
                    .build();

            reports.add(dto);
        }

        // Sort reports
        sortDepartmentReports(reports, sortBy, sortDirection);

        // Assign risk ranks
        for (int i = 0; i < reports.size(); i++) {
            reports.get(i).setRiskRank(i + 1);
        }

        return reports;
    }

    // ============================================
    // USER-WISE DEFAULTER REPORT
    // ============================================

    

    public Page<UserDefaulterDTO> getUserDefaulterReport(
            Long departmentId,
            String defaulterCategory,
            Pageable pageable
    ) {
        log.info("📊 Generating user defaulter report - deptId={}, category={}", departmentId, defaulterCategory);

        List<User> users;
        if (departmentId != null) {
            users = userRepository.findByDepartmentIdAndActive(departmentId);
        } else {
            users = userRepository.findAll().stream()
                    .filter(u -> u.getIsActive() != null && u.getIsActive())
                    .collect(Collectors.toList());
        }

        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        List<UserDefaulterDTO> defaulters = new ArrayList<>();

        for (User user : users) {
            Long userId = user.getId();
            Long userDeptId = user.getDepartmentEntity() != null ? user.getDepartmentEntity().getId() : null;

            // Skip if user has no department
            if (userDeptId == null) continue;

            // Document metrics
            long totalAssigned = acknowledgementRepository.countTotalDocumentsAssignedToUser(userDeptId);
            long acknowledged = acknowledgementRepository.countDocumentsAcknowledgedByUserInDepartment(userId, userDeptId);
            long pending = totalAssigned - acknowledged;
            long lateAcks = violationRepository.countLateAcknowledgedByUser(userId);
            double onTimeRate = totalAssigned > 0 ? ((acknowledged - lateAcks) * 100.0 / totalAssigned) : 100.0;

            // Violation metrics
            long totalViolations = violationRepository.countByUser_Id(userId);
            long unresolvedViolations = violationRepository.countPendingByUserId(userId);
            long resolvedViolations = totalViolations - unresolvedViolations;

            // Historical data (last 6 months)
            long violations6Months = violationRepository.countUserViolationsInDateRange(userId, sixMonthsAgo, LocalDateTime.now());
            
            // Determine defaulter category
            String category = "NONE";
            if (violations6Months >= 5) category = "CHRONIC";
            else if (violations6Months >= 3) category = "REPEAT";
            else if (violations6Months >= 1) category = "OCCASIONAL";

            // Filter by category if specified
            if (defaulterCategory != null && !defaulterCategory.isEmpty() && !category.equals(defaulterCategory)) {
                continue;
            }

            // Get monthly violation history
            List<UserDefaulterDTO.MonthlyViolationCount> violationHistory = getMonthlyViolationHistory(userId, sixMonthsAgo);

            // Get delay statistics
            Double avgDelay = violationRepository.getAverageDaysDelayedByDepartment(userDeptId);
            Integer maxDelay = violationRepository.getMaxDaysDelayed();

            // Get last activity dates
            LocalDateTime lastAckDate = acknowledgementRepository.getLastAcknowledgementDateByUser(userId);
            List<ComplianceViolation> userViolations = violationRepository.findByUser_IdOrderByCreatedAtDesc(userId);
            LocalDateTime lastViolationDate = userViolations.isEmpty() ? null : userViolations.get(0).getCreatedAt();

            // Count severity levels
            long critical = 0, high = 0, medium = 0;
            for (ComplianceViolation v : userViolations) {
                if (!v.getResolved()) {
                    int days = v.getDaysDelayed() != null ? v.getDaysDelayed() : 0;
                    if (days >= 14) critical++;
                    else if (days >= 10) high++;
                    else if (days >= 7) medium++;
                }
            }

            UserDefaulterDTO dto = UserDefaulterDTO.builder()
                    .userId(userId)
                    .userName(user.getName())
                    .userEmail(user.getEmail())
                    .employeeId(user.getEmployeeId())
                    .userRole(user.getRole().name())
                    .isActive(user.getIsActive())
                    .departmentId(userDeptId)
                    .departmentName(user.getDepartmentEntity().getName())
                    .departmentCode(user.getDepartmentEntity().getCode())
                    .totalDocumentsAssigned(totalAssigned)
                    .documentsAcknowledged(acknowledged)
                    .documentsPending(pending)
                    .lateAcknowledgements(lateAcks)
                    .onTimeRate(Math.round(onTimeRate * 100.0) / 100.0)
                    .totalViolations(totalViolations)
                    .resolvedViolations(resolvedViolations)
                    .unresolvedViolations(unresolvedViolations)
                    .criticalViolations(critical)
                    .highViolations(high)
                    .mediumViolations(medium)
                    .violationsLast6Months(violations6Months)
                    .violationHistory(violationHistory)
                    .averageDelayDays(avgDelay != null ? avgDelay.longValue() : 0L)
                    .maxDelayDays(maxDelay != null ? maxDelay.longValue() : 0L)
                    .isRepeatDefaulter(violations6Months >= 3)
                    .isChronicDefaulter(violations6Months >= 5)
                    .defaulterCategory(category)
                    .lastAcknowledgementDate(lastAckDate)
                    .lastViolationDate(lastViolationDate)
                    .lastLoginDate(user.getLastLogin())
                    .build();

            defaulters.add(dto);
        }

        // Sort by violations (descending)
        defaulters.sort(Comparator.comparing(UserDefaulterDTO::getTotalViolations).reversed());

        // Assign ranks
        for (int i = 0; i < defaulters.size(); i++) {
            defaulters.get(i).setDefaulterRank(i + 1);
        }

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), defaulters.size());
        List<UserDefaulterDTO> pagedList = start < defaulters.size() ? 
                defaulters.subList(start, end) : Collections.emptyList();

        return new PageImpl<>(pagedList, pageable, defaulters.size());
    }

    // ============================================
    // DOCUMENT AUDIT TRAIL
    // ============================================

    

    public DocumentAuditTrailDTO getDocumentAuditTrail(Long documentId) {
        log.info("📊 Generating document audit trail - docId={}", documentId);

        Optional<Document> docOpt = documentRepository.findById(documentId);
        if (docOpt.isEmpty()) {
            throw new RuntimeException("Document not found: " + documentId);
        }
        Document document = docOpt.get();

        // Get acknowledgements
        List<DocumentAcknowledgement> acknowledgements = 
                acknowledgementRepository.findAllByDocumentIdOrderByAcknowledgedAt(documentId);

        // Get violations
        List<ComplianceViolation> violations = violationRepository.findAllByDocumentId(documentId);

        // Get alerts/notifications
        List<Alert> alerts = alertRepository.findAllByDocumentIdOrderByCreatedAt(documentId);

        // Get users who should acknowledge
        Long deptId = document.getDepartment() != null ? document.getDepartment().getId() : null;
        List<User> deptUsers = deptId != null ? 
                userRepository.findByDepartmentIdAndActive(deptId) : Collections.emptyList();

        // Filter to acknowledging roles only
        deptUsers = deptUsers.stream()
                .filter(this::isAcknowledgingRole)
                .collect(Collectors.toList());

        // Build acknowledged users list
        Set<Long> acknowledgedUserIds = acknowledgements.stream()
                .map(DocumentAcknowledgement::getUserId)
                .collect(Collectors.toSet());

        List<DocumentAuditTrailDTO.AcknowledgementRecord> ackRecords = new ArrayList<>();
        for (DocumentAcknowledgement ack : acknowledgements) {
            User user = ack.getUser();
            long daysToAck = ChronoUnit.DAYS.between(document.getUploadDate(), ack.getAcknowledgedAt());
            boolean wasLate = daysToAck > 7;

            ackRecords.add(DocumentAuditTrailDTO.AcknowledgementRecord.builder()
                    .userId(user.getId())
                    .userName(user.getName())
                    .userEmail(user.getEmail())
                    .employeeId(user.getEmployeeId())
                    .acknowledgedAt(ack.getAcknowledgedAt())
                    .ipAddress(ack.getIpAddress())
                    .wasLate(wasLate)
                    .daysToAcknowledge((int) daysToAck)
                    .build());
        }

        // Build pending users list
        List<DocumentAuditTrailDTO.PendingUserRecord> pendingRecords = new ArrayList<>();
        for (User user : deptUsers) {
            if (!acknowledgedUserIds.contains(user.getId())) {
                long daysPending = ChronoUnit.DAYS.between(document.getUploadDate(), LocalDateTime.now());
                boolean hasViolation = hasViolationForUser(violations, user.getId());
                boolean reminderSent = hasReminderForUser(alerts, user.getId());

                pendingRecords.add(DocumentAuditTrailDTO.PendingUserRecord.builder()
                        .userId(user.getId())
                        .userName(user.getName())
                        .userEmail(user.getEmail())
                        .employeeId(user.getEmployeeId())
                        .daysPending((int) daysPending)
                        .hasViolation(hasViolation)
                        .reminderSent(reminderSent)
                        .build());
            }
        }

        // Build notification records
        List<DocumentAuditTrailDTO.NotificationRecord> notificationRecords = new ArrayList<>();
        for (Alert alert : alerts) {
            if (alert.getAlertType() == Alert.AlertType.NEW_DOCUMENT_UPLOADED ||
                alert.getAlertType() == Alert.AlertType.ACKNOWLEDGEMENT_REQUIRED) {
                notificationRecords.add(DocumentAuditTrailDTO.NotificationRecord.builder()
                        .userId(alert.getTargetUserId())
                        .userName(alert.getTargetUser() != null ? alert.getTargetUser().getName() : null)
                        .userEmail(alert.getTargetUser() != null ? alert.getTargetUser().getEmail() : null)
                        .notificationChannel(alert.getNotificationChannel().name())
                        .sentAt(alert.getCreatedAt())
                        .delivered(true)
                        .build());
            }
        }

        // Build escalation records
        List<DocumentAuditTrailDTO.EscalationRecord> escalationRecords = new ArrayList<>();
        for (Alert alert : alerts) {
            if (alert.getAlertType() == Alert.AlertType.COMPLIANCE_REMINDER ||
                alert.getAlertType() == Alert.AlertType.ESCALATION_DEPT_ADMIN ||
                alert.getAlertType() == Alert.AlertType.ESCALATION_SUPER_ADMIN) {
                escalationRecords.add(DocumentAuditTrailDTO.EscalationRecord.builder()
                        .escalationType(alert.getAlertType().name())
                        .targetUserId(alert.getTargetUserId())
                        .targetUserName(alert.getTargetUser() != null ? alert.getTargetUser().getName() : null)
                        .escalatedAt(alert.getCreatedAt())
                        .message(alert.getMessage())
                        .build());
            }
        }

        // Build violation records
        List<DocumentAuditTrailDTO.ViolationRecord> violationRecords = new ArrayList<>();
        for (ComplianceViolation v : violations) {
            violationRecords.add(DocumentAuditTrailDTO.ViolationRecord.builder()
                    .violationId(v.getId())
                    .userId(v.getUserId())
                    .userName(v.getUserName())
                    .userEmail(v.getUserEmail())
                    .violationDate(v.getViolationDate())
                    .daysDelayed(v.getDaysDelayed())
                    .severity(v.getDaysDelayed() >= 14 ? "CRITICAL" : 
                             v.getDaysDelayed() >= 10 ? "HIGH" : "MEDIUM")
                    .resolved(v.getResolved())
                    .resolvedDate(v.getResolvedDate())
                    .resolvedByName(v.getResolvedByName())
                    .remarks(v.getRemarks())
                    .build());
        }

        // Count escalation types
        long reminders = alertRepository.countRemindersSentForDocument(documentId);
        long deptAdminEscalations = alertRepository.countDeptAdminEscalationsForDocument(documentId);
        long superAdminEscalations = alertRepository.countSuperAdminEscalationsForDocument(documentId);

        // Calculate acknowledgement rate
        double ackRate = deptUsers.size() > 0 ? 
                (acknowledgements.size() * 100.0 / deptUsers.size()) : 100.0;

        return DocumentAuditTrailDTO.builder()
                .documentId(document.getId())
                .fileName(document.getFileName())
                .documentType(document.getDocumentType() != null ? document.getDocumentType().name() : null)
                .priority(document.getPriority() != null ? document.getPriority().name() : null)
                .status(document.getStatus() != null ? document.getStatus().name() : null)
                .fileSizeBytes(document.getFileSize())
                .fileExtension(document.getFileExtension())
                .description(document.getDescription())
                .uploadedById(document.getUploadedBy().getId())
                .uploadedByName(document.getUploadedBy().getName())
                .uploadedByEmail(document.getUploadedBy().getEmail())
                .uploadedByEmployeeId(document.getUploadedBy().getEmployeeId())
                .uploadDate(document.getUploadDate())
                .targetDepartmentId(deptId)
                .targetDepartmentName(document.getDepartment() != null ? document.getDepartment().getName() : null)
                .targetDepartmentCode(document.getDepartment() != null ? document.getDepartment().getCode() : null)
                .totalUsersNotified(notificationRecords.size())
                .totalUsersInDepartment(deptUsers.size())
                .notificationHistory(notificationRecords)
                .acknowledgedCount(acknowledgements.size())
                .pendingCount(pendingRecords.size())
                .acknowledgementRate(Math.round(ackRate * 100.0) / 100.0)
                .acknowledgedUsers(ackRecords)
                .pendingUsers(pendingRecords)
                .escalationHistory(escalationRecords)
                .remindersSent((int) reminders)
                .deptAdminEscalations((int) deptAdminEscalations)
                .superAdminEscalations((int) superAdminEscalations)
                .totalViolations(violations.size())
                .resolvedViolations((int) violations.stream().filter(ComplianceViolation::getResolved).count())
                .unresolvedViolations((int) violations.stream().filter(this::isUnresolved).count())
                .violationDetails(violationRecords)
                .reportGeneratedAt(LocalDateTime.now())
                .reportGeneratedBy(SecurityUtils.getCurrentUserEmail())
                .build();
    }

    // ============================================
    // VIOLATION TREND & RISK ANALYSIS
    // ============================================

    

    public ViolationTrendDTO getViolationTrends(LocalDate startDate, LocalDate endDate) {
        log.info("📊 Generating violation trend analysis");

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : LocalDateTime.now().minusMonths(12);
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : LocalDateTime.now();

        // Overall metrics
        long totalViolations = violationRepository.count();
        long violationsThisYear = violationRepository.countViolationsSince(
                LocalDateTime.now().withMonth(1).withDayOfMonth(1).withHour(0).withMinute(0));
        long violationsThisMonth = violationRepository.countViolationsSince(
                LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0));
        long violationsLastMonth = violationRepository.countViolationsInDateRange(
                LocalDateTime.now().minusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0),
                LocalDateTime.now().withDayOfMonth(1).minusDays(1).withHour(23).withMinute(59));
        
        double monthChange = violationsLastMonth > 0 ? 
                ((violationsThisMonth - violationsLastMonth) * 100.0 / violationsLastMonth) : 0.0;

        // Monthly trends (last 12 months)
        List<Object[]> monthlyData = violationRepository.getMonthlyViolationCounts(
                LocalDateTime.now().minusMonths(12));
        List<Object[]> monthlyResolved = violationRepository.getMonthlyResolvedCounts(
                LocalDateTime.now().minusMonths(12));
        
        Map<String, Long> resolvedMap = monthlyResolved.stream()
                .collect(Collectors.toMap(
                        this::toMonthKey,
                        this::toViolationCount
                ));

        long cumulative = 0;
        List<ViolationTrendDTO.MonthlyTrend> monthlyTrends = new ArrayList<>();
        for (Object[] row : monthlyData) {
            String month = (String) row[0];
            long newViolations = ((Number) row[1]).longValue();
            long resolved = resolvedMap.getOrDefault(month, 0L);
            cumulative += newViolations - resolved;

            monthlyTrends.add(ViolationTrendDTO.MonthlyTrend.builder()
                    .month(month)
                    .monthName(formatMonth(month))
                    .newViolations(newViolations)
                    .resolvedViolations(resolved)
                    .cumulativeViolations(Math.max(0, cumulative))
                    .complianceRate(newViolations > 0 ? 
                            (resolved * 100.0 / newViolations) : 100.0)
                    .build());
        }

        // Department risk ranking
        List<DepartmentComplianceDTO> deptReports = getDepartmentComplianceReport(
                startDate, endDate, "complianceScore", "asc");
        List<ViolationTrendDTO.DepartmentRisk> deptRisks = new ArrayList<>();
        int rank = 1;
        for (DepartmentComplianceDTO dept : deptReports) {
            deptRisks.add(ViolationTrendDTO.DepartmentRisk.builder()
                    .rank(rank++)
                    .departmentId(dept.getDepartmentId())
                    .departmentName(dept.getDepartmentName())
                    .departmentCode(dept.getDepartmentCode())
                    .totalViolations(dept.getTotalViolations())
                    .unresolvedViolations(dept.getUnresolvedViolations())
                    .complianceRate(dept.getComplianceScore())
                    .riskLevel(dept.getRiskLevel())
                    .build());
        }

        // Safety vs Non-Safety ratio
        long safetyViolations = violationRepository.countSafetyViolations();
        long nonSafetyViolations = totalViolations - safetyViolations;
        double ratio = nonSafetyViolations > 0 ? (double) safetyViolations / nonSafetyViolations : 0.0;

        // Delay metrics
        Double avgDelay = violationRepository.getAverageDaysDelayed();
        Integer maxDelay = violationRepository.getMaxDaysDelayed();

        // Delay distribution
        List<Object[]> delayData = violationRepository.getDelayDistribution();
        long totalUnresolved = violationRepository.countAllPending();
        List<ViolationTrendDTO.DelayDistribution> delayDistribution = new ArrayList<>();
        for (Object[] row : delayData) {
            String range = (String) row[0];
            long count = ((Number) row[1]).longValue();
            delayDistribution.add(ViolationTrendDTO.DelayDistribution.builder()
                    .delayRange(range)
                    .count(count)
                    .percentage(totalUnresolved > 0 ? (count * 100.0 / totalUnresolved) : 0.0)
                    .build());
        }

        // Chronic defaulters
        List<Object[]> defaulterData = violationRepository.findDefaulters(3L, PageRequest.of(0, 10));
        List<ViolationTrendDTO.ChronicDefaulter> chronicDefaulters = new ArrayList<>();
        int defaulterRank = 1;
        for (Object[] row : defaulterData) {
            Long userId = ((Number) row[0]).longValue();
            String userName = (String) row[1];
            String employeeId = (String) row[2];
            String deptName = (String) row[3];
            long violCount = ((Number) row[4]).longValue();
            long unresolvedCount = violationRepository.countPendingByUserId(userId);
            String category = violCount >= 5 ? "CHRONIC" : "REPEAT";

            chronicDefaulters.add(ViolationTrendDTO.ChronicDefaulter.builder()
                    .rank(defaulterRank++)
                    .userId(userId)
                    .userName(userName)
                    .employeeId(employeeId)
                    .departmentName(deptName)
                    .totalViolations(violCount)
                    .unresolvedViolations(unresolvedCount)
                    .defaulterCategory(category)
                    .build());
        }

        // Resolution metrics
        long resolved = violationRepository.countAllResolved();
        double resolutionRate = totalViolations > 0 ? (resolved * 100.0 / totalViolations) : 100.0;

        // Escalation metrics
        long reminders = violationRepository.countRemindersSent();
        long deptEscalations = violationRepository.countDeptAdminEscalations();
        long superEscalations = violationRepository.countSuperAdminEscalations();
        double escalationRate = totalViolations > 0 ? 
                ((deptEscalations + superEscalations) * 100.0 / totalViolations) : 0.0;

        return ViolationTrendDTO.builder()
                .totalViolationsAllTime(totalViolations)
                .violationsThisYear(violationsThisYear)
                .violationsThisMonth(violationsThisMonth)
                .violationsLastMonth(violationsLastMonth)
                .monthOverMonthChange(Math.round(monthChange * 100.0) / 100.0)
                .monthlyTrends(monthlyTrends)
                .departmentRiskRanking(deptRisks)
                .safetyViolations(safetyViolations)
                .nonSafetyViolations(nonSafetyViolations)
                .safetyToNonSafetyRatio(Math.round(ratio * 100.0) / 100.0)
                .averageAcknowledgementDelayDays(avgDelay != null ? Math.round(avgDelay * 100.0) / 100.0 : 0.0)
                .medianAcknowledgementDelayDays(avgDelay != null ? avgDelay : 0.0)  // Approximation
                .maxAcknowledgementDelayDays(maxDelay != null ? maxDelay : 0)
                .delayDistribution(delayDistribution)
                .totalDefaulters((long) defaulterData.size())
                .repeatDefaulters(chronicDefaulters.stream()
                        .filter(this::isRepeatCategory)
                        .count())
                .chronicDefaulters(chronicDefaulters.stream()
                        .filter(this::isChronicCategory)
                        .count())
                .topChronicDefaulters(chronicDefaulters)
                .averageResolutionTimeDays(0.0)  // Would need additional tracking
                .resolvedThisMonth(violationRepository.countResolvedInDateRange(
                        LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0),
                        LocalDateTime.now()))
                .pendingResolution(totalUnresolved)
                .resolutionRate(Math.round(resolutionRate * 100.0) / 100.0)
                .totalReminders(reminders)
                .deptAdminEscalations(deptEscalations)
                .superAdminEscalations(superEscalations)
                .escalationRate(Math.round(escalationRate * 100.0) / 100.0)
                .analysisStartDate(startDateTime)
                .analysisEndDate(endDateTime)
                .generatedAt(LocalDateTime.now())
                .generatedBy(SecurityUtils.getCurrentUserEmail())
                .build();
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private String formatDateRange(LocalDateTime start, LocalDateTime end) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        return start.format(formatter) + " - " + end.format(formatter);
    }

    private String formatMonth(String yearMonth) {
        try {
            String[] parts = yearMonth.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            return java.time.Month.of(month).name().substring(0, 3) + " " + year;
        } catch (Exception e) {
            return yearMonth;
        }
    }

    private double calculateComplianceScore(double ackRate, long unresolvedViolations, 
                                           long totalViolations, long criticalViolations) {
        // Weighted scoring:
        // - Acknowledgement rate: 50%
        // - Violation resolution rate: 30%
        // - No critical violations bonus: 20%
        double ackScore = ackRate * 0.5;
        double resolutionRate = totalViolations > 0 ? 
                ((totalViolations - unresolvedViolations) * 100.0 / totalViolations) : 100.0;
        double resolutionScore = resolutionRate * 0.3;
        double criticalBonus = criticalViolations == 0 ? 20.0 : 
                (criticalViolations <= 2 ? 10.0 : 0.0);
        
        return Math.min(100.0, ackScore + resolutionScore + criticalBonus);
    }

    private String calculateRiskLevel(double complianceScore) {
        if (complianceScore >= 90) return "LOW";
        if (complianceScore >= 70) return "MEDIUM";
        if (complianceScore >= 50) return "HIGH";
        return "CRITICAL";
    }

    private void sortDepartmentReports(List<DepartmentComplianceDTO> reports, 
                                       String sortBy, String sortDirection) {
        Comparator<DepartmentComplianceDTO> comparator;
        
        switch (sortBy != null ? sortBy.toLowerCase() : "violations") {
            case "compliancescore":
            case "compliance":
                comparator = Comparator.comparing(DepartmentComplianceDTO::getComplianceScore);
                break;
            case "acknowledgementrate":
            case "ackrate":
                comparator = Comparator.comparing(DepartmentComplianceDTO::getAcknowledgementRate);
                break;
            case "name":
                comparator = Comparator.comparing(DepartmentComplianceDTO::getDepartmentName);
                break;
            default:
                comparator = Comparator.comparing(DepartmentComplianceDTO::getTotalViolations);
        }

        if ("desc".equalsIgnoreCase(sortDirection)) {
            comparator = comparator.reversed();
        }

        reports.sort(comparator);
    }

    private List<UserDefaulterDTO.MonthlyViolationCount> getMonthlyViolationHistory(
            Long userId, LocalDateTime since) {
        List<UserDefaulterDTO.MonthlyViolationCount> history = new ArrayList<>();
        LocalDateTime current = since;

        while (current.isBefore(LocalDateTime.now())) {
            LocalDateTime monthEnd = current.plusMonths(1).minusDays(1);
            long count = violationRepository.countUserViolationsInDateRange(userId, current, monthEnd);

            history.add(UserDefaulterDTO.MonthlyViolationCount.builder()
                    .month(current.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                    .violationCount(count)
                    .lateAckCount(0L)  // Would need additional tracking
                    .build());

            current = current.plusMonths(1);
        }

        return history;
    }

    private long countRepeatDefaultersForDepartment(List<Object[]> data, Long deptId) {
        long count = 0;
        for (Object[] row : data) {
            Long userId = ((Number) row[0]).longValue();
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getDepartmentEntity() != null &&
                    user.getDepartmentEntity().getId().equals(deptId)) {
                count++;
            }
        }
        return count;
    }

    private boolean isAcknowledgingRole(User u) {
        return u.getRole() == User.UserRole.DEPARTMENT_UPLOAD_ADMIN ||
               u.getRole() == User.UserRole.DEPARTMENT_USER;
    }

    private boolean hasViolationForUser(List<ComplianceViolation> violations, Long userId) {
        for (ComplianceViolation v : violations) {
            if (v.getUserId().equals(userId)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasReminderForUser(List<Alert> alerts, Long userId) {
        for (Alert a : alerts) {
            if (a.getTargetUserId() != null &&
                    a.getTargetUserId().equals(userId) &&
                    a.getAlertType() == Alert.AlertType.COMPLIANCE_REMINDER) {
                return true;
            }
        }
        return false;
    }

    private boolean isUnresolved(ComplianceViolation v) {
        return !v.getResolved();
    }

    private String toMonthKey(Object[] row) {
        return (String) row[0];
    }

    private Long toViolationCount(Object[] row) {
        return ((Number) row[1]).longValue();
    }

    private boolean isRepeatCategory(ViolationTrendDTO.ChronicDefaulter d) {
        return "REPEAT".equals(d.getDefaulterCategory());
    }

    private boolean isChronicCategory(ViolationTrendDTO.ChronicDefaulter d) {
        return "CHRONIC".equals(d.getDefaulterCategory());
    }
}
