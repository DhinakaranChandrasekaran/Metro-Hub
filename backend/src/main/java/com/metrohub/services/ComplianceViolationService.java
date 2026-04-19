package com.metrohub.services;

import com.metrohub.exception.ResourceNotFoundException;
import com.metrohub.exception.UnauthorizedException;
import com.metrohub.dto.ViolationDTOs.DepartmentViolationStatsDTO;
import com.metrohub.dto.ViolationDTOs.ViolationDTO;
import com.metrohub.dto.ViolationDTOs.ViolationSummaryDTO;
import com.metrohub.models.ComplianceViolation;
import com.metrohub.models.ComplianceViolation.ViolationType;
import com.metrohub.models.Department;
import com.metrohub.models.Document;
import com.metrohub.models.PolicyRule;
import com.metrohub.models.User;
import com.metrohub.repositories.ComplianceViolationRepository;
import com.metrohub.repositories.DepartmentRepository;
import com.metrohub.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ComplianceViolationService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceViolationService.class);

    private final ComplianceViolationRepository violationRepository;
    private final DepartmentRepository departmentRepository;

    // ============================================
    // VIOLATION CREATION
    // ============================================

    @Transactional
    public ComplianceViolation createViolation(Document document, User user, int daysDelayed) {
        // Delegate to the policy-aware version with null policy (legacy behavior)
        return createViolationWithPolicy(document, user, daysDelayed, null, 168);
    }

    @Transactional
    public ComplianceViolation createViolationWithPolicy(Document document, User user, 
            int daysDelayed, PolicyRule policyRule, int slaHours) {
        log.info("🚨 Creating compliance violation for document {} and user {} (Policy: {}, SLA: {}h)", 
                document.getId(), user.getEmail(), 
                policyRule != null ? policyRule.getName() : "Default",
                slaHours);

        // Check if violation already exists
        if (violationExists(document.getId(), user.getId())) {
            log.warn("Violation already exists for document {} and user {}", 
                    document.getId(), user.getId());
            return violationRepository.findByDocument_IdAndUser_Id(document.getId(), user.getId())
                    .orElse(null);
        }

        Department department = document.getDepartment();
        if (department == null) {
            department = user.getDepartmentEntity();
        }

        ComplianceViolation violation = ComplianceViolation.builder()
                .document(document)
                .user(user)
                .department(department)
                .violationType(ViolationType.ACK_DELAY)
                .violationDate(LocalDateTime.now())
                .daysDelayed(daysDelayed)
                .policyRule(policyRule)
                .slaHoursApplied(slaHours)
                .resolved(false)
                .acknowledgedLate(false)
                .reminderSent(false)
                .deptAdminEscalated(false)
                .superAdminEscalated(false)
                .build();

        ComplianceViolation saved = violationRepository.save(violation);
        log.info("✅ Compliance violation created: ID={}, Document={}, User={}, DaysDelayed={}, Policy={}, SLA={}h", 
                saved.getId(), document.getFileName(), user.getEmail(), daysDelayed,
                policyRule != null ? policyRule.getName() : "Default", slaHours);

        return saved;
    }

        @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public boolean violationExists(Long documentId, Long userId) {
        try {
            return violationRepository.existsByDocument_IdAndUser_Id(documentId, userId);
        } catch (Exception e) {
            log.warn("Error checking if violation exists: {}", e.getMessage());
            // If query fails, assume violation doesn't exist to allow creation
            return false;
        }
    }

    // ============================================
    // VIOLATION RETRIEVAL
    // ============================================

    @Transactional(readOnly = true)
    public Optional<ViolationDTO> getViolationById(Long id) {
        return violationRepository.findById(id).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ViolationDTO> getMyViolations(Pageable pageable) {
        User currentUser = SecurityUtils.getCurrentUser();
        return violationRepository.findByUser_Id(currentUser.getId(), pageable)
                .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ViolationDTO> getViolationsByDepartment(Long departmentId, Pageable pageable) {
        // Verify access
        User currentUser = SecurityUtils.getCurrentUser();
        if (!currentUser.hasGlobalAccess() && 
            !departmentId.equals(currentUser.getDepartmentEntity().getId())) {
            throw new UnauthorizedException("You can only view violations for your own department");
        }

        return violationRepository.findByDepartment_Id(departmentId, pageable)
                .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ViolationDTO> getAllViolations(Pageable pageable) {
        return violationRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ViolationDTO> getPendingViolationsByDepartment(Long departmentId, Pageable pageable) {
        return violationRepository.findPendingViolationsByDepartment(departmentId, pageable)
                .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ViolationDTO> getMyPendingViolations(Pageable pageable) {
        User currentUser = SecurityUtils.getCurrentUser();
        return violationRepository.findPendingViolationsByUser(currentUser.getId(), pageable)
                .map(this::toDTO);
    }

    // ============================================
    // LATE ACKNOWLEDGEMENT
    // ============================================

    @Transactional
    public void markLateAcknowledgement(Long documentId, Long userId) {
        violationRepository.findByDocument_IdAndUser_Id(documentId, userId)
                .ifPresent(this::processLateAcknowledgement);
    }

    private void processLateAcknowledgement(ComplianceViolation violation) {
        if (!violation.getAcknowledgedLate()) {
            violation.markLateAcknowledgement();
            violationRepository.save(violation);
            log.info("📝 Marked late acknowledgement for violation ID={}", violation.getId());
        }
    }

    // ============================================
    // VIOLATION RESOLUTION
    // ============================================

    @Transactional
    public ViolationDTO resolveViolation(Long violationId, String remarks) {
        User currentUser = SecurityUtils.getCurrentUser();
        
        // Only admins can resolve
        if (!currentUser.canManageUsers() && !currentUser.hasGlobalAccess()) {
            throw new UnauthorizedException("Only administrators can resolve violations");
        }

        Optional<ComplianceViolation> violationOpt = violationRepository.findById(violationId);
        if (violationOpt.isEmpty()) {
            throw new ResourceNotFoundException("Violation", "id", violationId);
        }
        ComplianceViolation violation = violationOpt.get();

        // Check department access for department admins
        if (!currentUser.hasGlobalAccess() && 
            !violation.getDepartment().getId().equals(currentUser.getDepartmentEntity().getId())) {
            throw new UnauthorizedException("You can only resolve violations in your own department");
        }

        if (violation.getResolved()) {
            log.warn("Violation {} is already resolved", violationId);
            return toDTO(violation);
        }

        violation.resolve(currentUser, remarks);
        ComplianceViolation saved = violationRepository.save(violation);
        
        log.info("✅ Violation {} resolved by {} with remarks: {}", 
                violationId, currentUser.getEmail(), remarks);

        return toDTO(saved);
    }

    // ============================================
    // STATISTICS & REPORTS
    // ============================================

    @Transactional(readOnly = true)
    public ViolationSummaryDTO getMySummary() {
        User currentUser = SecurityUtils.getCurrentUser();
        Long userId = currentUser.getId();

        return ViolationSummaryDTO.builder()
                .totalViolations(violationRepository.countByUser_Id(userId))
                .pendingViolations(violationRepository.countPendingByUserId(userId))
                .resolvedViolations(violationRepository.countByUser_Id(userId) - 
                                   violationRepository.countPendingByUserId(userId))
                .userId(userId)
                .userName(currentUser.getName())
                .build();
    }

    @Transactional(readOnly = true)
    public ViolationSummaryDTO getDepartmentSummary(Long departmentId) {
        Optional<Department> deptOpt = departmentRepository.findById(departmentId);
        if (deptOpt.isEmpty()) {
            throw new ResourceNotFoundException("Department", "id", departmentId);
        }
        Department department = deptOpt.get();

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        return ViolationSummaryDTO.builder()
                .totalViolations(violationRepository.countByDepartment_Id(departmentId))
                .pendingViolations(violationRepository.countPendingByDepartmentId(departmentId))
                .resolvedViolations(violationRepository.countResolvedByDepartmentId(departmentId))
                .lateAcknowledgedViolations(violationRepository.countLateAcknowledgedByDepartmentId(departmentId))
                .criticalCount(violationRepository.countCriticalByDepartmentId(departmentId))
                .highCount(violationRepository.countHighByDepartmentId(departmentId))
                .mediumCount(violationRepository.countMediumByDepartmentId(departmentId))
                .departmentId(departmentId)
                .departmentName(department.getName())
                .violationsLast7Days(violationRepository.countViolationsSinceByDepartment(departmentId, sevenDaysAgo))
                .violationsLast30Days(violationRepository.countViolationsSinceByDepartment(departmentId, thirtyDaysAgo))
                .build();
    }

    @Transactional(readOnly = true)
    public ViolationSummaryDTO getOverallSummary() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        return ViolationSummaryDTO.builder()
                .totalViolations(violationRepository.count())
                .pendingViolations(violationRepository.countAllPending())
                .resolvedViolations(violationRepository.countAllResolved())
                .criticalCount(violationRepository.countCriticalViolations())
                .highCount(violationRepository.countHighSeverityViolations())
                .mediumCount(violationRepository.countMediumSeverityViolations())
                .remindersSent(violationRepository.countRemindersSent())
                .deptAdminEscalations(violationRepository.countDeptAdminEscalations())
                .superAdminEscalations(violationRepository.countSuperAdminEscalations())
                .violationsLast7Days(violationRepository.countViolationsSince(sevenDaysAgo))
                .violationsLast30Days(violationRepository.countViolationsSince(thirtyDaysAgo))
                .build();
    }

    @Transactional(readOnly = true)
    public List<DepartmentViolationStatsDTO> getDepartmentWiseStats() {
        List<DepartmentViolationStatsDTO> stats = new ArrayList<>();
        
        List<Department> departments = departmentRepository.findAll();
        for (Department dept : departments) {
            DepartmentViolationStatsDTO deptStats = DepartmentViolationStatsDTO.builder()
                    .departmentId(dept.getId())
                    .departmentName(dept.getName())
                    .departmentCode(dept.getCode())
                    .totalViolations(violationRepository.countByDepartment_Id(dept.getId()))
                    .pendingViolations(violationRepository.countPendingByDepartmentId(dept.getId()))
                    .resolvedViolations(violationRepository.countResolvedByDepartmentId(dept.getId()))
                    .lateAcknowledged(violationRepository.countLateAcknowledgedByDepartmentId(dept.getId()))
                    .criticalCount(violationRepository.countCriticalByDepartmentId(dept.getId()))
                    .highCount(violationRepository.countHighByDepartmentId(dept.getId()))
                    .mediumCount(violationRepository.countMediumByDepartmentId(dept.getId()))
                    .build();
            stats.add(deptStats);
        }

        return stats;
    }

    @Transactional(readOnly = true)
    public List<DepartmentViolationStatsDTO> getHighRiskDepartments(int limit) {
        List<Object[]> results = violationRepository.findHighRiskDepartments(PageRequest.of(0, limit));
        List<DepartmentViolationStatsDTO> highRisk = new ArrayList<>();

        for (Object[] row : results) {
            Long deptId = (Long) row[0];
            String deptName = (String) row[1];
            Long violationCount = (Long) row[2];

            DepartmentViolationStatsDTO dto = DepartmentViolationStatsDTO.builder()
                    .departmentId(deptId)
                    .departmentName(deptName)
                    .pendingViolations(violationCount)
                    .criticalCount(violationRepository.countCriticalByDepartmentId(deptId))
                    .highCount(violationRepository.countHighByDepartmentId(deptId))
                    .mediumCount(violationRepository.countMediumByDepartmentId(deptId))
                    .build();
            highRisk.add(dto);
        }

        return highRisk;
    }

    // ============================================
    // ESCALATION TRACKING
    // ============================================

    @Transactional
    public void markReminderSent(Long violationId) {
        violationRepository.markReminderSent(violationId, LocalDateTime.now());
        log.debug("Marked reminder sent for violation {}", violationId);
    }

    @Transactional
    public void markDeptAdminEscalated(Long violationId) {
        violationRepository.markDeptAdminEscalated(violationId, LocalDateTime.now());
        log.debug("Marked dept admin escalation for violation {}", violationId);
    }

    @Transactional
    public void markSuperAdminEscalated(Long violationId) {
        violationRepository.markSuperAdminEscalated(violationId, LocalDateTime.now());
        log.debug("Marked super admin escalation for violation {}", violationId);
    }

    // ============================================
    // CONVERSION
    // ============================================

        public ViolationDTO toDTO(ComplianceViolation violation) {
        if (violation == null) return null;

        Document doc = violation.getDocument();
        User user = violation.getUser();
        Department dept = violation.getDepartment();

        return ViolationDTO.builder()
                .id(violation.getId())
                // Document info
                .documentId(doc != null ? doc.getId() : null)
                .documentName(doc != null ? doc.getFileName() : null)
                .documentType(doc != null && doc.getDocumentType() != null ? 
                             doc.getDocumentType().name() : null)
                .documentPriority(doc != null && doc.getPriority() != null ? 
                                 doc.getPriority().name() : null)
                .documentUploadDate(doc != null ? doc.getUploadDate() : null)
                // User info
                .userId(user != null ? user.getId() : null)
                .userName(user != null ? user.getName() : null)
                .userEmail(user != null ? user.getEmail() : null)
                .userEmployeeId(user != null ? user.getEmployeeId() : null)
                // Department info
                .departmentId(dept != null ? dept.getId() : null)
                .departmentName(dept != null ? dept.getName() : null)
                .departmentCode(dept != null ? dept.getCode() : null)
                // Violation details
                .violationType(violation.getViolationType())
                .violationDate(violation.getViolationDate())
                .daysDelayed(violation.getDaysDelayed())
                // Policy information (Phase 9)
                .policyRuleId(violation.getPolicyRuleId())
                .policyRuleName(violation.getPolicyRuleName())
                .slaHoursApplied(violation.getSlaHoursApplied())
                // Resolution
                .resolved(violation.getResolved())
                .resolvedById(violation.getResolvedBy() != null ? violation.getResolvedBy().getId() : null)
                .resolvedByName(violation.getResolvedByName())
                .resolvedDate(violation.getResolvedDate())
                .remarks(violation.getRemarks())
                // Late acknowledgement
                .acknowledgedLate(violation.getAcknowledgedLate())
                .lateAcknowledgementDate(violation.getLateAcknowledgementDate())
                // Escalation status
                .reminderSent(violation.getReminderSent())
                .reminderSentAt(violation.getReminderSentAt())
                .deptAdminEscalated(violation.getDeptAdminEscalated())
                .deptAdminEscalatedAt(violation.getDeptAdminEscalatedAt())
                .superAdminEscalated(violation.getSuperAdminEscalated())
                .superAdminEscalatedAt(violation.getSuperAdminEscalatedAt())
                // Timestamps
                .createdAt(violation.getCreatedAt())
                .build();
    }
}
