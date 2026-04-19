package com.metrohub.services;

import com.metrohub.exception.ResourceNotFoundException;
import com.metrohub.dto.PolicyDTOs.PolicyRuleDTO;
import com.metrohub.dto.PolicyDTOs.PolicyRuleRequestDTO;
import com.metrohub.models.Department;
import com.metrohub.models.Document;
import com.metrohub.models.PolicyRule;
import com.metrohub.models.User;
import com.metrohub.repositories.DepartmentRepository;
import com.metrohub.repositories.PolicyRuleRepository;
import com.metrohub.security.SecurityUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyService {

    private static final Logger log = LoggerFactory.getLogger(PolicyService.class);

    private final PolicyRuleRepository policyRuleRepository;
    private final DepartmentRepository departmentRepository;

    // Default SLA values (fallback)
    private static final int DEFAULT_REMINDER_HOURS = 24;
    private static final int DEFAULT_DEPT_ADMIN_ESCALATION_HOURS = 48;
    private static final int DEFAULT_SUPER_ADMIN_ESCALATION_HOURS = 72;
    private static final int DEFAULT_VIOLATION_HOURS = 168;

    

    @PostConstruct
    @Transactional
    public void init() {
        ensureDefaultPolicyExists();
    }

    // ============================================
    // CRUD OPERATIONS
    // ============================================

    @Transactional
    public PolicyRuleDTO createPolicy(PolicyRuleRequestDTO requestDTO) {
        log.info("Creating new policy rule: {}", requestDTO.getName());

        // ⭐ Only allow creating Global Default policy (no department/priority based)
        if (!Boolean.TRUE.equals(requestDTO.getIsDefault()) ||
            requestDTO.getDepartmentId() != null ||
            requestDTO.getPriority() != null) {
            throw new IllegalArgumentException(
                "❌ Priority/Department-based policies are deprecated. Only Global Default policy is allowed.");
        }

        PolicyRule policyRule = new PolicyRule();
        mapRequestToEntity(requestDTO, policyRule);

        // Set created by
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null) {
            policyRule.setCreatedBy(currentUser);
        }

        // If setting as default, remove default from others
        if (Boolean.TRUE.equals(requestDTO.getIsDefault())) {
            clearDefaultFlag();
        }

        PolicyRule saved = policyRuleRepository.save(policyRule);
        log.info("Created policy rule ID={}, name={}", saved.getId(), saved.getName());

        return toDTO(saved);
    }

    @Transactional
    public PolicyRuleDTO updatePolicy(Long id, PolicyRuleRequestDTO requestDTO) {
        log.info("Updating policy rule ID={}", id);

        Optional<PolicyRule> policyRuleOpt = policyRuleRepository.findById(id);
        if (policyRuleOpt.isEmpty()) {
            throw new ResourceNotFoundException("Policy rule not found: " + id);
        }
        PolicyRule policyRule = policyRuleOpt.get();

        // Check for duplicate (if changing department/priority)
        if (!isSameScope(policyRule, requestDTO)) {
            if (policyRuleRepository.existsByDepartmentIdAndPriority(
                    requestDTO.getDepartmentId(), requestDTO.getPriority())) {
                throw new IllegalArgumentException(
                    "Policy already exists for this department/priority combination");
            }
        }

        mapRequestToEntity(requestDTO, policyRule);

        // Set updated by
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null) {
            policyRule.setUpdatedBy(currentUser);
        }

        // If setting as default, remove default from others
        if (Boolean.TRUE.equals(requestDTO.getIsDefault()) && 
            !Boolean.TRUE.equals(policyRule.getIsDefault())) {
            clearDefaultFlag();
        }

        PolicyRule saved = policyRuleRepository.save(policyRule);
        log.info("Updated policy rule ID={}, name={}", saved.getId(), saved.getName());

        return toDTO(saved);
    }

        public PolicyRuleDTO getPolicyById(Long id) {
        Optional<PolicyRule> policyRuleOpt = policyRuleRepository.findById(id);
        if (policyRuleOpt.isEmpty()) {
            throw new ResourceNotFoundException("Policy rule not found: " + id);
        }
        PolicyRule policyRule = policyRuleOpt.get();
        return toDTO(policyRule);
    }

        public List<PolicyRuleDTO> getAllPolicies() {
        return policyRuleRepository.findAllByOrderByDepartmentAscPriorityAsc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

        public List<PolicyRuleDTO> getActivePolicies() {
        return policyRuleRepository.findByIsActiveTrueOrderByDepartmentAscPriorityAsc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deletePolicy(Long id) {
        log.info("Soft deleting policy rule ID={}", id);

        Optional<PolicyRule> policyRuleOpt = policyRuleRepository.findById(id);
        if (policyRuleOpt.isEmpty()) {
            throw new ResourceNotFoundException("Policy rule not found: " + id);
        }
        PolicyRule policyRule = policyRuleOpt.get();

        // Cannot delete default policy
        if (Boolean.TRUE.equals(policyRule.getIsDefault())) {
            throw new IllegalArgumentException("Cannot delete the default policy");
        }

        policyRule.setIsActive(false);
        policyRuleRepository.save(policyRule);

        log.info("Deactivated policy rule ID={}", id);
    }

    @Transactional
    public void hardDeletePolicy(Long id) {
        log.info("Hard deleting policy rule ID={}", id);

        Optional<PolicyRule> policyRuleOpt = policyRuleRepository.findById(id);
        if (policyRuleOpt.isEmpty()) {
            throw new ResourceNotFoundException("Policy rule not found: " + id);
        }
        PolicyRule policyRule = policyRuleOpt.get();

        policyRuleRepository.delete(policyRule);

        log.info("Permanently deleted policy rule ID={}", id);
    }

    @Transactional
    public PolicyRuleDTO togglePolicy(Long id) {
        log.info("Toggling policy rule ID={}", id);

        Optional<PolicyRule> policyRuleOpt = policyRuleRepository.findById(id);
        if (policyRuleOpt.isEmpty()) {
            throw new ResourceNotFoundException("Policy rule not found: " + id);
        }
        PolicyRule policyRule = policyRuleOpt.get();

        if (Boolean.TRUE.equals(policyRule.getIsDefault())) {
            throw new IllegalArgumentException("Cannot toggle the default policy");
        }

        policyRule.setIsActive(!Boolean.TRUE.equals(policyRule.getIsActive()));
        policyRule = policyRuleRepository.save(policyRule);

        log.info("Toggled policy rule ID={} to active={}", id, policyRule.getIsActive());
        return toDTO(policyRule);
    }

    // ============================================
    // POLICY LOOKUP
    // ============================================

        public PolicyRule findApplicablePolicy(Document document) {
        // ⭐ ALL documents use GLOBAL DEFAULT policy (no priority/department matching)
        log.debug("Finding policy for document ID={} - using Global Default", document.getId());
        return getDefaultPolicy();
    }

        public Optional<PolicyRule> findPolicy(Long departmentId, Document.Priority priority) {
        // ⭐ ALL lookups return GLOBAL DEFAULT policy
        return policyRuleRepository.findGlobalDefaultPolicy();
    }

        public PolicyRule getDefaultPolicy() {
        return policyRuleRepository.findGlobalDefaultPolicy()
                .orElseGet(this::logAndCreateFallbackPolicy);
    }

    // ============================================
    // SLA CONFIGURATION (Manual SLA takes priority)
    // ============================================

        public int getReminderHours(Document document) {
        // Check for manual SLA first
        if (Boolean.TRUE.equals(document.getIsSlaManual()) && document.getSlaReminderHours() != null) {
            log.debug("Using manual SLA reminder hours: {} for doc {}", document.getSlaReminderHours(), document.getId());
            return document.getSlaReminderHours();
        }
        
        // Fall back to policy
        PolicyRule policy = findApplicablePolicy(document);
        return policy.getReminderHours() != null ? 
                policy.getReminderHours() : DEFAULT_REMINDER_HOURS;
    }

        public int getDeptAdminEscalationHours(Document document) {
        // Check for manual SLA first
        if (Boolean.TRUE.equals(document.getIsSlaManual()) && document.getSlaDeptAdminEscalationHours() != null) {
            log.debug("Using manual SLA dept admin escalation hours: {} for doc {}", 
                    document.getSlaDeptAdminEscalationHours(), document.getId());
            return document.getSlaDeptAdminEscalationHours();
        }
        
        // Fall back to policy
        PolicyRule policy = findApplicablePolicy(document);
        return policy.getDeptAdminEscalationHours() != null ? 
                policy.getDeptAdminEscalationHours() : DEFAULT_DEPT_ADMIN_ESCALATION_HOURS;
    }

        public int getSuperAdminEscalationHours(Document document) {
        // Check for manual SLA first
        if (Boolean.TRUE.equals(document.getIsSlaManual()) && document.getSlaSuperAdminEscalationHours() != null) {
            log.debug("Using manual SLA super admin escalation hours: {} for doc {}", 
                    document.getSlaSuperAdminEscalationHours(), document.getId());
            return document.getSlaSuperAdminEscalationHours();
        }
        
        // Fall back to policy
        PolicyRule policy = findApplicablePolicy(document);
        return policy.getSuperAdminEscalationHours() != null ? 
                policy.getSuperAdminEscalationHours() : DEFAULT_SUPER_ADMIN_ESCALATION_HOURS;
    }

        public int getViolationHours(Document document) {
        // Check for manual SLA first
        if (Boolean.TRUE.equals(document.getIsSlaManual()) && document.getSlaViolationHours() != null) {
            log.debug("Using manual SLA violation hours: {} for doc {}", 
                    document.getSlaViolationHours(), document.getId());
            return document.getSlaViolationHours();
        }
        
        // Fall back to policy
        PolicyRule policy = findApplicablePolicy(document);
        return policy.getViolationHours() != null ? 
                policy.getViolationHours() : DEFAULT_VIOLATION_HOURS;
    }

    // ============================================
    // NOTIFICATION SETTINGS (Manual SLA takes priority)
    // ============================================

        public boolean isEmailEnabled(Document document) {
        // Check for manual SLA first
        if (Boolean.TRUE.equals(document.getIsSlaManual()) && document.getSlaEmailEnabled() != null) {
            log.debug("Using manual SLA email setting: {} for doc {}", document.getSlaEmailEnabled(), document.getId());
            return document.getSlaEmailEnabled();
        }
        // Fall back to policy
        PolicyRule policy = findApplicablePolicy(document);
        return Boolean.TRUE.equals(policy.getEmailEnabled());
    }

        public boolean isSmsEnabled(Document document) {
        // Check for manual SLA first
        if (Boolean.TRUE.equals(document.getIsSlaManual()) && document.getSlaSmsEnabled() != null) {
            log.debug("Using manual SLA SMS setting: {} for doc {}", document.getSlaSmsEnabled(), document.getId());
            return document.getSlaSmsEnabled();
        }
        // Fall back to policy
        PolicyRule policy = findApplicablePolicy(document);
        return Boolean.TRUE.equals(policy.getSmsEnabled());
    }

        public boolean isDashboardEnabled(Document document) {
        // Check for manual SLA first
        if (Boolean.TRUE.equals(document.getIsSlaManual()) && document.getSlaDashboardEnabled() != null) {
            log.debug("Using manual SLA dashboard setting: {} for doc {}", document.getSlaDashboardEnabled(), document.getId());
            return document.getSlaDashboardEnabled();
        }
        // Fall back to policy
        PolicyRule policy = findApplicablePolicy(document);
        return Boolean.TRUE.equals(policy.getDashboardEnabled());
    }

    // ============================================
    // UTILITIES
    // ============================================

        public PolicyRuleDTO toDTO(PolicyRule policyRule) {
        return PolicyRuleDTO.builder()
                .id(policyRule.getId())
                .name(policyRule.getName())
                .description(policyRule.getDescription())
                .departmentId(policyRule.getDepartmentId())
                .departmentName(policyRule.getDepartmentName())
                .priority(policyRule.getPriority())
                .priorityName(policyRule.getPriorityName())
                .scopeDescription(policyRule.getScopeDescription())
                .reminderHours(policyRule.getReminderHours())
                .deptAdminEscalationHours(policyRule.getDeptAdminEscalationHours())
                .superAdminEscalationHours(policyRule.getSuperAdminEscalationHours())
                .violationHours(policyRule.getViolationHours())
                .emailEnabled(policyRule.getEmailEnabled())
                .smsEnabled(policyRule.getSmsEnabled())
                .dashboardEnabled(policyRule.getDashboardEnabled())
                .isActive(policyRule.getIsActive())
                .isDefault(policyRule.getIsDefault())
                .deptAdminEscalationEnabled(policyRule.isDeptAdminEscalationEnabled())
                .superAdminEscalationEnabled(policyRule.isSuperAdminEscalationEnabled())
                .violationEnabled(policyRule.isViolationEnabled())
                .createdById(policyRule.getCreatedBy() != null ? policyRule.getCreatedBy().getId() : null)
                .createdByName(policyRule.getCreatedByName())
                .updatedById(policyRule.getUpdatedBy() != null ? policyRule.getUpdatedBy().getId() : null)
                .updatedByName(policyRule.getUpdatedByName())
                .createdAt(policyRule.getCreatedAt())
                .updatedAt(policyRule.getUpdatedAt())
                .build();
    }

        public boolean policyExists(Long departmentId, Document.Priority priority) {
        return policyRuleRepository.existsByDepartmentIdAndPriority(departmentId, priority);
    }

    @Transactional
    public void ensureDefaultPolicyExists() {
        if (!policyRuleRepository.existsByIsDefaultTrue()) {
            log.info("No default policy found, creating global default policy");
            
            PolicyRule defaultPolicy = PolicyRule.builder()
                    .name("Global Default")
                    .description("Default compliance policy for all departments and priorities")
                    .department(null)
                    .priority(null)
                    .reminderHours(DEFAULT_REMINDER_HOURS)
                    .deptAdminEscalationHours(DEFAULT_DEPT_ADMIN_ESCALATION_HOURS)
                    .superAdminEscalationHours(DEFAULT_SUPER_ADMIN_ESCALATION_HOURS)
                    .violationHours(DEFAULT_VIOLATION_HOURS)
                    .emailEnabled(true)
                    .smsEnabled(true)
                    .dashboardEnabled(true)
                    .isActive(true)
                    .isDefault(true)
                    .build();

            policyRuleRepository.save(defaultPolicy);
            log.info("Created global default policy");
        }
    }

    // ============================================
    // PRIVATE HELPERS
    // ============================================

    private void mapRequestToEntity(PolicyRuleRequestDTO requestDTO, PolicyRule entity) {
        entity.setName(requestDTO.getName());
        entity.setDescription(requestDTO.getDescription());

        // Set department
        if (requestDTO.getDepartmentId() != null) {
            Optional<Department> deptOpt = departmentRepository.findById(requestDTO.getDepartmentId());
            if (deptOpt.isEmpty()) {
                throw new ResourceNotFoundException(
                        "Department not found: " + requestDTO.getDepartmentId());
            }
            Department dept = deptOpt.get();
            entity.setDepartment(dept);
        } else {
            entity.setDepartment(null);
        }

        entity.setPriority(requestDTO.getPriority());
        entity.setReminderHours(requestDTO.getReminderHours());
        entity.setDeptAdminEscalationHours(requestDTO.getDeptAdminEscalationHours());
        entity.setSuperAdminEscalationHours(requestDTO.getSuperAdminEscalationHours());
        entity.setViolationHours(requestDTO.getViolationHours());
        entity.setEmailEnabled(requestDTO.getEmailEnabled());
        entity.setSmsEnabled(requestDTO.getSmsEnabled());
        entity.setDashboardEnabled(requestDTO.getDashboardEnabled());
        entity.setIsActive(requestDTO.getIsActive());
        entity.setIsDefault(requestDTO.getIsDefault());
    }

    private boolean isSameScope(PolicyRule existing, PolicyRuleRequestDTO request) {
        Long existingDeptId = existing.getDepartmentId();
        Long requestDeptId = request.getDepartmentId();

        boolean sameDept = (existingDeptId == null && requestDeptId == null) ||
                (existingDeptId != null && existingDeptId.equals(requestDeptId));

        boolean samePriority = (existing.getPriority() == null && request.getPriority() == null) ||
                (existing.getPriority() != null && existing.getPriority().equals(request.getPriority()));

        return sameDept && samePriority;
    }

    private void clearDefaultFlag() {
        policyRuleRepository.findByIsDefaultTrue().ifPresent(this::removeDefaultFlag);
    }

    private void removeDefaultFlag(PolicyRule policy) {
        policy.setIsDefault(false);
        policyRuleRepository.save(policy);
        log.info("Cleared default flag from policy ID={}", policy.getId());
    }

    private PolicyRule logAndCreateFallbackPolicy() {
        log.warn("No default policy found, creating fallback");
        return createFallbackPolicy();
    }

    @Transactional
    protected PolicyRule createFallbackPolicy() {
        PolicyRule fallback = PolicyRule.builder()
                .name("System Fallback")
                .description("Auto-generated fallback policy")
                .department(null)
                .priority(null)
                .reminderHours(DEFAULT_REMINDER_HOURS)
                .deptAdminEscalationHours(DEFAULT_DEPT_ADMIN_ESCALATION_HOURS)
                .superAdminEscalationHours(DEFAULT_SUPER_ADMIN_ESCALATION_HOURS)
                .violationHours(DEFAULT_VIOLATION_HOURS)
                .emailEnabled(true)
                .smsEnabled(true)
                .dashboardEnabled(true)
                .isActive(true)
                .isDefault(true)
                .build();

        return policyRuleRepository.save(fallback);
    }
}
