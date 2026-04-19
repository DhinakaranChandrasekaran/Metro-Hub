package com.metrohub.controllers;

import com.metrohub.dto.ViolationDTOs.*;
import com.metrohub.security.SecurityUtils;
import com.metrohub.services.ComplianceViolationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/violations")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class ViolationController {

    private static final Logger log = LoggerFactory.getLogger(ViolationController.class);

    private final ComplianceViolationService violationService;

    // GET /violations/admin — all violations (SUPER_ADMIN) or dept violations (DEPT_ADMIN)
    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAdminViolations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ViolationDTO> violations;
            if (SecurityUtils.isSuperAdmin()) {
                violations = violationService.getAllViolations(pageable);
            } else {
                Long deptId = SecurityUtils.getCurrentUserDepartmentId();
                violations = violationService.getViolationsByDepartment(deptId, pageable);
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", violations.getContent());
            response.put("totalElements", violations.getTotalElements());
            response.put("totalPages", violations.getTotalPages());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get admin violations: {}", e.getMessage());
            return buildErrorResponse("Failed to retrieve violations", e.getMessage());
        }
    }

    // GET /violations/my — current user's own violations
    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyViolations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ViolationDTO> violations;
            if (SecurityUtils.isDepartmentAdmin()) {
                Long deptId = SecurityUtils.getCurrentUserDepartmentId();
                violations = violationService.getViolationsByDepartment(deptId, pageable);
            } else if (SecurityUtils.isSuperAdmin()) {
                violations = violationService.getAllViolations(pageable);
            } else {
                violations = violationService.getMyViolations(pageable);
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", violations.getContent());
            response.put("totalElements", violations.getTotalElements());
            response.put("totalPages", violations.getTotalPages());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get my violations: {}", e.getMessage());
            return buildErrorResponse("Failed to retrieve violations", e.getMessage());
        }
    }

    // GET /violations/summary — overall summary (SUPER_ADMIN, DEPT_ADMIN)
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<Map<String, Object>> getViolationSummary() {
        try {
            ViolationSummaryDTO summary = violationService.getOverallSummary();
            return ResponseEntity.ok(Map.of("success", true, "data", summary));
        } catch (Exception e) {
            log.error("Failed to get violation summary: {}", e.getMessage());
            return buildErrorResponse("Failed to retrieve summary", e.getMessage());
        }
    }

    // GET /violations/{id} — single violation by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'DEPARTMENT_UPLOAD_ADMIN')")
    public ResponseEntity<Map<String, Object>> getViolationById(@PathVariable Long id) {
        try {
            return violationService.getViolationById(id)
                    .map(dto -> ResponseEntity.ok(Map.<String, Object>of("success", true, "data", dto)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Failed to get violation {}: {}", id, e.getMessage());
            return buildErrorResponse("Violation not found", e.getMessage());
        }
    }

    // POST /violations/{id}/resolve — resolve a violation
    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'DEPARTMENT_UPLOAD_ADMIN')")
    public ResponseEntity<Map<String, Object>> resolveViolation(
            @PathVariable Long id,
            @RequestBody Map<String, String> requestBody) {
        try {
            String remarks = requestBody.get("remarks");
            if (remarks == null || remarks.trim().isEmpty()) {
                return buildErrorResponse("Resolve failed", "Remarks are required");
            }
            ViolationDTO resolved = violationService.resolveViolation(id, remarks.trim());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Violation resolved successfully");
            response.put("data", resolved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to resolve violation {}: {}", id, e.getMessage());
            return buildErrorResponse("Failed to resolve violation", e.getMessage());
        }
    }

    // GET /violations/department/{departmentId} — by department
    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<Map<String, Object>> getViolationsByDepartment(
            @PathVariable Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<ViolationDTO> violations = violationService.getViolationsByDepartment(
                    departmentId, PageRequest.of(page, size));
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", violations.getContent());
            response.put("totalElements", violations.getTotalElements());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get department violations: {}", e.getMessage());
            return buildErrorResponse("Failed to retrieve department violations", e.getMessage());
        }
    }

    // GET /violations/stats/departments — department-wise stats
    @GetMapping("/stats/departments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDepartmentStats() {
        try {
            List<DepartmentViolationStatsDTO> stats = violationService.getDepartmentWiseStats();
            return ResponseEntity.ok(Map.of("success", true, "data", stats));
        } catch (Exception e) {
            log.error("Failed to get department stats: {}", e.getMessage());
            return buildErrorResponse("Failed to retrieve department stats", e.getMessage());
        }
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(String message, String error) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("error", error);
        return ResponseEntity.badRequest().body(response);
    }
}
