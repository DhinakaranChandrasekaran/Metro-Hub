package com.metrohub.controllers;

import com.metrohub.dto.AnalyticsDTOs.*;
import com.metrohub.services.AnalyticsService;
import com.metrohub.services.RiskCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final RiskCalculationService riskCalculationService;

    @GetMapping("/risk/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<Map<String, Object>> getOrganizationRiskSummary() {
        try {
            RiskSummaryDTO summary = analyticsService.getOrganizationRiskSummary();
            return ResponseEntity.ok(buildResponse("Organization risk summary retrieved successfully", summary));
        } catch (Exception e) {
            return buildErrorResponse("Failed to retrieve risk summary", e.getMessage());
        }
    }

    @GetMapping("/risk/departments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllDepartmentRisks() {
        try {
            List<DepartmentRiskDTO> risks;

            // Super Admin sees all departments, Dept Admin sees only their own
            if (com.metrohub.security.SecurityUtils.isSuperAdmin()) {
                risks = analyticsService.getAllDepartmentRisks();
            } else {
                // Dept Admin - get only their department
                Long deptId = com.metrohub.security.SecurityUtils.getCurrentUserDepartmentId();
                if (deptId != null) {
                    risks = analyticsService.getDepartmentRisksForDepartment(deptId);
                } else {
                    risks = new java.util.ArrayList<>();
                }
            }

            Map<String, Object> response = buildResponse("Department risks retrieved successfully", risks);
            response.put("count", risks.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse("Failed to retrieve department risks", e.getMessage());
        }
    }

    @GetMapping("/risk/department/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDepartmentRisk(@PathVariable Long id) {
        try {
            DepartmentRiskDTO risk = riskCalculationService.calculateDepartmentRisk(id);
            return ResponseEntity.ok(buildResponse("Department risk retrieved successfully", risk));
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Department not found");
            response.put("error", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return buildErrorResponse("Failed to retrieve department risk", e.getMessage());
        }
    }

    @GetMapping("/risk/users")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllUserRisks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            List<UserRiskDTO> risks = analyticsService.getAllUserRisks(page, size);
            Map<String, Object> response = buildResponse("User risks retrieved successfully", risks);
            response.put("count", risks.size());
            response.put("page", page);
            response.put("size", size);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse("Failed to retrieve user risks", e.getMessage());
        }
    }

    @GetMapping("/risk/user/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserRisk(@PathVariable Long id) {
        try {
            UserRiskDTO risk = riskCalculationService.calculateUserRisk(id);
            return ResponseEntity.ok(buildResponse("User risk retrieved successfully", risk));
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "User not found");
            response.put("error", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return buildErrorResponse("Failed to retrieve user risk", e.getMessage());
        }
    }

    @GetMapping("/risk/top-defaulters")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<Map<String, Object>> getTopDefaulters(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<TopDefaulterDTO> defaulters = analyticsService.getTopDefaulters(Math.min(limit, 50));
            Map<String, Object> response = buildResponse("Top defaulters retrieved successfully", defaulters);
            response.put("count", defaulters.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse("Failed to retrieve top defaulters", e.getMessage());
        }
    }

    @GetMapping("/risk/heatmap")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDepartmentRiskHeatmap() {
        try {
            RiskHeatmapDTO heatmap = analyticsService.getDepartmentRiskHeatmap();
            return ResponseEntity.ok(buildResponse("Risk heatmap retrieved successfully", heatmap));
        } catch (Exception e) {
            return buildErrorResponse("Failed to retrieve risk heatmap", e.getMessage());
        }
    }

    @GetMapping("/risk/trends")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<Map<String, Object>> getRiskTrends(@RequestParam(defaultValue = "12_MONTHS") String period) {
        try {
            if (!List.of("7_DAYS", "30_DAYS", "90_DAYS", "12_MONTHS").contains(period)) {
                period = "12_MONTHS";
            }
            RiskTrendDTO trends = analyticsService.getViolationTrends(period);
            Map<String, Object> response = buildResponse("Risk trends retrieved successfully", trends);
            response.put("period", period);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse("Failed to retrieve risk trends", e.getMessage());
        }
    }

    @PostMapping("/risk/calculate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerRiskCalculation() {
        try {
            var deptSnapshots = riskCalculationService.calculateAndSaveAllDepartmentRiskScores();
            var userSnapshots = riskCalculationService.calculateAndSaveAllUserRiskScores();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Risk scores calculated and saved successfully");
            response.put("departmentSnapshotsCreated", deptSnapshots.size());
            response.put("userSnapshotsCreated", userSnapshots.size());
            response.put("calculatedAt", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse("Failed to calculate risk scores", e.getMessage());
        }
    }

    @GetMapping("/risk/quick-stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<Map<String, Object>> getQuickStats() {
        try {
            RiskSummaryDTO summary = analyticsService.getOrganizationRiskSummary();
            Map<String, Object> stats = new HashMap<>();
            stats.put("organizationRiskScore", summary.getOrganizationRiskScore());
            stats.put("organizationRiskLevel", summary.getOrganizationRiskLevel());
            stats.put("criticalDepartments", summary.getCriticalRiskDepartments());
            stats.put("highRiskDepartments", summary.getHighRiskDepartments());
            stats.put("pendingViolations", summary.getPendingViolations());
            stats.put("chronicDefaulters", summary.getChronicDefaultersCount());
            stats.put("complianceRate", summary.getOverallComplianceRate());
            stats.put("trendDirection", summary.getTrendDirection());
            stats.put("activeWarnings", summary.getActiveWarnings().size());
            return ResponseEntity.ok(buildResponse("Quick stats retrieved successfully", stats));
        } catch (Exception e) {
            return buildErrorResponse("Failed to retrieve quick stats", e.getMessage());
        }
    }

    private Map<String, Object> buildResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        response.put("generatedAt", LocalDateTime.now());
        return response;
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(String message, String error) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("error", error);
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.internalServerError().body(response);
    }
}
