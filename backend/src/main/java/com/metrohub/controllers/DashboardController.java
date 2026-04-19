package com.metrohub.controllers;

import com.metrohub.dto.DashboardDTOs.*;
import com.metrohub.dto.DocumentDTOs.DocumentCardDTO;
import com.metrohub.dto.UserDTOs.UserDTO;
import com.metrohub.models.User;
import com.metrohub.security.SecurityUtils;
import com.metrohub.services.AcknowledgementService;
import com.metrohub.services.DashboardService;
import com.metrohub.services.NotificationService;
import com.metrohub.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;
    private final NotificationService notificationService;
    private final AcknowledgementService acknowledgementService;
    private final UserService userService;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        try {
            DashboardSummaryDTO summary = dashboardService.getDashboardSummary();
            return ResponseEntity.ok(buildResponse("Dashboard summary retrieved successfully", summary));
        } catch (Exception e) {
            log.error("Failed to get dashboard summary: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(buildError("Failed to retrieve dashboard summary", e.getMessage()));
        }
    }

    @GetMapping("/documents")
    public ResponseEntity<Map<String, Object>> getDocumentCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "uploadDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            Sort sort = Sort.by("asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC, mapSortField(sortBy));
            Page<DocumentCardDTO> documents = dashboardService.getDocumentCards(PageRequest.of(page, size, sort));
            Map<String, Object> response = buildResponse("Documents retrieved successfully", documents.getContent());
            response.put("pagination", buildPaginationInfo(documents));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(buildError("Failed to retrieve documents", e.getMessage()));
        }
    }

    @GetMapping("/pending-actions")
    public ResponseEntity<Map<String, Object>> getPendingActions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<PendingActionDTO> pendingActions = dashboardService.getPendingActions(PageRequest.of(page, size));
            Map<String, Object> response = buildResponse("Pending actions retrieved successfully", pendingActions.getContent());
            response.put("pagination", buildPaginationInfo(pendingActions));
            response.put("totalPendingActions", pendingActions.getTotalElements());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(buildError("Failed to retrieve pending actions", e.getMessage()));
        }
    }

    @GetMapping("/deadlines")
    public ResponseEntity<Map<String, Object>> getDeadlineTracking() {
        try {
            DeadlineTrackingDTO deadlines = dashboardService.getDeadlineTracking();
            return ResponseEntity.ok(buildResponse("Deadline tracking data retrieved successfully", deadlines));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(buildError("Failed to retrieve deadline tracking", e.getMessage()));
        }
    }

    @GetMapping("/recent")
    public ResponseEntity<Map<String, Object>> getRecentDocuments(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<DocumentCardDTO> recentDocs = dashboardService.getRecentDocuments(days, limit);
            Map<String, Object> response = buildResponse("Recent documents retrieved successfully", recentDocs);
            response.put("count", recentDocs.size());
            response.put("daysBack", days);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(buildError("Failed to retrieve recent documents", e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getQuickStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("todayUploads", dashboardService.getTodayUploadCount());
            stats.put("highPriorityCount", dashboardService.getHighPriorityCount());
            stats.put("documentsWithDeadlines", dashboardService.getDocumentsWithDeadlinesCount());
            stats.put("pendingActionsCount", dashboardService.getPendingActionsCount());
            stats.put("overdueCount", dashboardService.getOverdueCount());
            stats.put("dueSoon3Days", dashboardService.getDueSoonCount(3));
            stats.put("dueSoon7Days", dashboardService.getDueSoonCount(7));
            return ResponseEntity.ok(buildResponse("Statistics retrieved successfully", stats));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(buildError("Failed to retrieve statistics", e.getMessage()));
        }
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAdminDashboard() {
        try {
            Map<String, Object> adminData = new HashMap<>();
            adminData.put("summary", dashboardService.getDashboardSummary());
            adminData.put("totalUsers", userService.countActiveUsers());
            adminData.put("departmentAdmins", userService.getAllDepartmentAdmins().size());
            adminData.put("pendingEmailNotifications", notificationService.getPendingEmailCount());
            adminData.put("pendingSmsNotifications", notificationService.getPendingSmsCount());
            adminData.put("systemHealth", Map.of("status", "healthy", "notificationsEnabled", true));
            Map<String, Object> response = buildResponse("Admin dashboard retrieved successfully", adminData);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(buildError("Failed to retrieve admin dashboard", e.getMessage()));
        }
    }

    @GetMapping({"/department", "/department/{departmentId}"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'DEPARTMENT_UPLOAD_ADMIN', 'DEPARTMENT_USER')")
    public ResponseEntity<Map<String, Object>> getDepartmentDashboard(@PathVariable(required = false) Long departmentId) {
        try {
            Long effectiveDeptId = departmentId;
            if (effectiveDeptId == null) {
                Long currentUserId = SecurityUtils.getCurrentUserId();
                UserDTO currentUser = userService.getUserById(currentUserId);
                effectiveDeptId = currentUser.getDepartmentId();
            }

            if (effectiveDeptId == null) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("message", "Department ID required");
                return ResponseEntity.badRequest().body(err);
            }

            Map<String, Object> deptData = new HashMap<>();
            deptData.put("departmentId", effectiveDeptId);
            deptData.put("totalDepartmentUsers", userService.countUsersByDepartment(effectiveDeptId));
            deptData.put("uploadAdmin", userService.getUploadAdminForDepartment(effectiveDeptId));
            deptData.put("departmentDocuments", dashboardService.getDashboardSummary());
            deptData.put("acknowledgementStats", Map.of("pendingAcknowledgements", acknowledgementService.getPendingAcknowledgementCount()));
            deptData.put("unreadAlerts", notificationService.getUnreadCountForCurrentUser());

            return ResponseEntity.ok(buildResponse("Department dashboard retrieved successfully", deptData));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(buildError("Failed to retrieve department dashboard", e.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyDashboard() {
        try {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            UserDTO currentUser = userService.getUserById(currentUserId);

            Map<String, Object> myData = new HashMap<>();
            myData.put("user", currentUser);
            myData.put("unreadAlerts", notificationService.getUnreadCountForCurrentUser());
            myData.put("pendingAcknowledgements", acknowledgementService.getPendingAcknowledgementCount());
            myData.put("documentsPendingAcknowledgement", acknowledgementService.getDocumentsPendingAcknowledgement());

            Map<String, Boolean> permissions = new HashMap<>();
            permissions.put("canUpload", currentUser.getRole() != null &&
                    (currentUser.getRole() == User.UserRole.SUPER_ADMIN || currentUser.getRole() == User.UserRole.DEPARTMENT_UPLOAD_ADMIN));
            permissions.put("canManageUsers", currentUser.getRole() != null &&
                    (currentUser.getRole() == User.UserRole.SUPER_ADMIN || currentUser.getRole() == User.UserRole.DEPARTMENT_ADMIN));
            permissions.put("canViewAllDocuments", currentUser.getRole() != null && currentUser.getRole() == User.UserRole.SUPER_ADMIN);
            myData.put("permissions", permissions);

            return ResponseEntity.ok(buildResponse("Personal dashboard retrieved successfully", myData));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(buildError("Failed to retrieve personal dashboard", e.getMessage()));
        }
    }

    private String mapSortField(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "uploaddate", "upload_date" -> "uploadDate";
            case "priority" -> "priority";
            case "filename", "file_name" -> "fileName";
            case "documenttype", "document_type" -> "documentType";
            default -> "uploadDate";
        };
    }

    private Map<String, Object> buildResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        response.put("timestamp", LocalDateTime.now());
        return response;
    }

    private Map<String, Object> buildError(String message, String error) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("error", error);
        return response;
    }

    private Map<String, Object> buildPaginationInfo(Page<?> page) {
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("currentPage", page.getNumber());
        pagination.put("totalPages", page.getTotalPages());
        pagination.put("totalElements", page.getTotalElements());
        pagination.put("pageSize", page.getSize());
        pagination.put("isFirst", page.isFirst());
        pagination.put("isLast", page.isLast());
        pagination.put("hasNext", page.hasNext());
        pagination.put("hasPrevious", page.hasPrevious());
        return pagination;
    }
}
