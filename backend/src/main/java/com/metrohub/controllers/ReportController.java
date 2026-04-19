package com.metrohub.controllers;

import com.lowagie.text.DocumentException;
import com.metrohub.dto.ReportDTOs.*;
import com.metrohub.dto.ViolationDTOs.*;
import com.metrohub.services.AuditReportService;
import com.metrohub.services.ComplianceSchedulerService;
import com.metrohub.services.ComplianceViolationService;
import com.metrohub.services.ReportExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final AuditReportService auditReportService;
    private final ReportExportService reportExportService;
    private final ComplianceViolationService violationService;
    private final ComplianceSchedulerService schedulerService;

    // ===== COMPLIANCE REPORTS =====

    @GetMapping("/reports/compliance/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<Map<String, Object>> getComplianceSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String documentType) {
        try {
            ComplianceSummaryDTO summary = auditReportService.getComplianceSummary(startDate, endDate, departmentId, documentType);
            return ResponseEntity.ok(buildSuccessResponse("Compliance summary retrieved successfully", summary));
        } catch (Exception e) {
            log.error("Failed to get compliance summary: {}", e.getMessage(), e);
            return buildErrorResponse("Failed to retrieve compliance summary", e.getMessage());
        }
    }

    @GetMapping("/reports/compliance/department")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDepartmentComplianceReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "violations") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        try {
            List<DepartmentComplianceDTO> departments = auditReportService.getDepartmentComplianceReport(startDate, endDate, sortBy, sortDirection);
            Map<String, Object> response = buildSuccessResponse("Department compliance report retrieved successfully", departments);
            response.put("count", departments.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse("Failed to retrieve department report", e.getMessage());
        }
    }

    @GetMapping("/reports/compliance/user")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserDefaulterReport(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String defaulterCategory,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<UserDefaulterDTO> users = auditReportService.getUserDefaulterReport(departmentId, defaulterCategory, pageable);
            Map<String, Object> response = buildSuccessResponse("User defaulter report retrieved successfully", users.getContent());
            response.put("pagination", buildPaginationInfo(users));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse("Failed to retrieve user defaulter report", e.getMessage());
        }
    }

    @GetMapping("/reports/audit/document/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDocumentAuditTrail(@PathVariable Long id) {
        try {
            DocumentAuditTrailDTO auditTrail = auditReportService.getDocumentAuditTrail(id);
            return ResponseEntity.ok(buildSuccessResponse("Document audit trail retrieved successfully", auditTrail));
        } catch (RuntimeException e) {
            return buildErrorResponse("Document not found", "Document with ID " + id + " not found");
        } catch (Exception e) {
            return buildErrorResponse("Failed to retrieve audit trail", e.getMessage());
        }
    }

    @GetMapping("/reports/violations/trends")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<Map<String, Object>> getViolationTrends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            ViolationTrendDTO trends = auditReportService.getViolationTrends(startDate, endDate);
            return ResponseEntity.ok(buildSuccessResponse("Violation trends retrieved successfully", trends));
        } catch (Exception e) {
            return buildErrorResponse("Failed to retrieve violation trends", e.getMessage());
        }
    }

    // ===== PDF EXPORTS =====

    @GetMapping("/reports/export/pdf/compliance-summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<byte[]> exportComplianceSummaryToPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String documentType) throws DocumentException, IOException {
        ComplianceSummaryDTO summary = auditReportService.getComplianceSummary(startDate, endDate, departmentId, documentType);
        byte[] pdfBytes = reportExportService.exportComplianceSummaryToPdf(summary);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=compliance_summary_" + formatDateForFilename() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF).body(pdfBytes);
    }

    @GetMapping("/reports/export/pdf/department-compliance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<byte[]> exportDepartmentReportToPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "violations") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) throws DocumentException, IOException {
        List<DepartmentComplianceDTO> departments = auditReportService.getDepartmentComplianceReport(startDate, endDate, sortBy, sortDirection);
        byte[] pdfBytes = reportExportService.exportDepartmentReportToPdf(departments);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=department_compliance_" + formatDateForFilename() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF).body(pdfBytes);
    }

    @GetMapping("/reports/export/pdf/user-defaulter")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<byte[]> exportUserDefaulterReportToPdf(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String defaulterCategory) throws DocumentException, IOException {
        Page<UserDefaulterDTO> users = auditReportService.getUserDefaulterReport(departmentId, defaulterCategory, PageRequest.of(0, 1000));
        byte[] pdfBytes = reportExportService.exportUserDefaulterReportToPdf(users.getContent());
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=user_defaulter_" + formatDateForFilename() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF).body(pdfBytes);
    }

    @GetMapping("/reports/export/pdf/audit-trail/{documentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<byte[]> exportAuditTrailToPdf(@PathVariable Long documentId) throws DocumentException, IOException {
        DocumentAuditTrailDTO auditTrail = auditReportService.getDocumentAuditTrail(documentId);
        byte[] pdfBytes = reportExportService.exportAuditTrailToPdf(auditTrail);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit_trail_doc_" + documentId + "_" + formatDateForFilename() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF).body(pdfBytes);
    }

    // ===== EXCEL EXPORTS =====

    @GetMapping("/reports/export/excel/compliance-summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<byte[]> exportComplianceSummaryToExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String documentType) throws IOException {
        ComplianceSummaryDTO summary = auditReportService.getComplianceSummary(startDate, endDate, departmentId, documentType);
        byte[] excelBytes = reportExportService.exportComplianceSummaryToExcel(summary);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=compliance_summary_" + formatDateForFilename() + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(excelBytes);
    }

    @GetMapping("/reports/export/excel/department-compliance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<byte[]> exportDepartmentReportToExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "violations") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) throws IOException {
        List<DepartmentComplianceDTO> departments = auditReportService.getDepartmentComplianceReport(startDate, endDate, sortBy, sortDirection);
        byte[] excelBytes = reportExportService.exportDepartmentReportToExcel(departments);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=department_compliance_" + formatDateForFilename() + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(excelBytes);
    }

    @GetMapping("/reports/export/excel/user-defaulter")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<byte[]> exportUserDefaulterReportToExcel(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String defaulterCategory) throws IOException {
        Page<UserDefaulterDTO> users = auditReportService.getUserDefaulterReport(departmentId, defaulterCategory, PageRequest.of(0, 1000));
        byte[] excelBytes = reportExportService.exportUserDefaulterReportToExcel(users.getContent());
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=user_defaulter_" + formatDateForFilename() + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(excelBytes);
    }

    @GetMapping("/reports/export/excel/violation-trends")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<byte[]> exportViolationTrendsToExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws IOException {
        ViolationTrendDTO trends = auditReportService.getViolationTrends(startDate, endDate);
        byte[] excelBytes = reportExportService.exportViolationTrendsToExcel(trends);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=violation_trends_" + formatDateForFilename() + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(excelBytes);
    }

    // ===== VIOLATIONS (NOTE: /violations/my, /violations/my/pending, /violations/my/summary =====
    // NOTE: These endpoints are in ViolationController — do not duplicate here.

    @GetMapping("/violations/department")
    @PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN', 'DEPARTMENT_UPLOAD_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDepartmentViolations(
            @RequestParam Long departmentId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        try {
            Page<ViolationDTO> violations = violationService.getViolationsByDepartment(departmentId, PageRequest.of(page, size));
            Map<String, Object> response = buildSuccessResponse("Department violations retrieved successfully", violations.getContent());
            response.put("pagination", buildPaginationInfo(violations));
            response.put("departmentId", departmentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse("Failed to retrieve department violations", e.getMessage());
        }
    }

    @GetMapping("/violations/department/pending")
    @PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN', 'DEPARTMENT_UPLOAD_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDepartmentPendingViolations(
            @RequestParam Long departmentId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        try {
            Page<ViolationDTO> violations = violationService.getPendingViolationsByDepartment(departmentId, PageRequest.of(page, size));
            Map<String, Object> response = buildSuccessResponse("Pending department violations retrieved successfully", violations.getContent());
            response.put("pagination", buildPaginationInfo(violations));
            response.put("departmentId", departmentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse("Failed to retrieve pending violations", e.getMessage());
        }
    }

    @GetMapping("/violations/department/summary")
    @PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN', 'DEPARTMENT_UPLOAD_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDepartmentSummary(@RequestParam Long departmentId) {
        try {
            return ResponseEntity.ok(buildSuccessResponse("Department violation summary retrieved successfully", violationService.getDepartmentSummary(departmentId)));
        } catch (Exception e) {
            return buildErrorResponse("Failed to retrieve department summary", e.getMessage());
        }
    }


    @GetMapping("/violations/admin/summary")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getOverallSummary() {
        try {
            return ResponseEntity.ok(buildSuccessResponse("Overall violation summary retrieved successfully", violationService.getOverallSummary()));
        } catch (Exception e) {
            return buildErrorResponse("Failed to retrieve overall summary", e.getMessage());
        }
    }

    @GetMapping("/violations/admin/departments")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDepartmentWiseStats() {
        try {
            List<DepartmentViolationStatsDTO> stats = violationService.getDepartmentWiseStats();
            Map<String, Object> response = buildSuccessResponse("Department-wise statistics retrieved successfully", stats);
            response.put("totalDepartments", stats.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse("Failed to retrieve department statistics", e.getMessage());
        }
    }

    @GetMapping("/violations/admin/high-risk")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getHighRiskDepartments(@RequestParam(defaultValue = "5") int limit) {
        try {
            return ResponseEntity.ok(buildSuccessResponse("High-risk departments retrieved successfully", violationService.getHighRiskDepartments(limit)));
        } catch (Exception e) {
            return buildErrorResponse("Failed to retrieve high-risk departments", e.getMessage());
        }
    }

    // NOTE: /violations/{id} and /violations/{id}/resolve are in ViolationController — do not duplicate here.

    @GetMapping("/violations/scheduler/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getSchedulerStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("schedulerRunning", schedulerService.isSchedulerRunning());
        response.put("lastRunTimestamp", schedulerService.getLastRunTimestamp());
        response.put("escalationRules", Map.of(
                "reminder", "24 hours after upload",
                "deptAdminEscalation", "48 hours after upload",
                "superAdminEscalation", "72 hours after upload",
                "violationCreation", "7 days after upload"));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/violations/scheduler/trigger")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerScheduler() {
        try {
            schedulerService.triggerManualCheck();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Compliance check triggered successfully");
            response.put("lastRunTimestamp", schedulerService.getLastRunTimestamp());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse("Failed to trigger compliance check", e.getMessage());
        }
    }

    // ===== HELPERS =====

    private Map<String, Object> buildSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        response.put("timestamp", LocalDateTime.now());
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

    private Map<String, Object> buildPaginationInfo(Page<?> page) {
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("currentPage", page.getNumber());
        pagination.put("totalPages", page.getTotalPages());
        pagination.put("totalElements", page.getTotalElements());
        pagination.put("size", page.getSize());
        pagination.put("hasNext", page.hasNext());
        pagination.put("hasPrevious", page.hasPrevious());
        pagination.put("isFirst", page.isFirst());
        pagination.put("isLast", page.isLast());
        return pagination;
    }

    private String formatDateForFilename() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }
}