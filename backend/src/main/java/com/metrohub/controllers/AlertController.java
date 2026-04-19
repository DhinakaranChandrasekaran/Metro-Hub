package com.metrohub.controllers;

import com.metrohub.dto.AlertDTO;
import com.metrohub.models.Alert.AlertType;
import com.metrohub.services.AlertService;
import com.metrohub.services.NotificationService;
import com.metrohub.services.ComplianceSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class AlertController {

    private final AlertService alertService;
    private final NotificationService notificationService;
    private final ComplianceSchedulerService complianceSchedulerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<AlertDTO> alerts = alertService.getAllAlerts(PageRequest.of(page, size));
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Alerts retrieved successfully");
            response.put("data", alerts.getContent());
            response.put("pagination", buildPaginationInfo(alerts));
            response.put("unreadCount", alertService.getUnreadCount());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse("Failed to retrieve alerts", e.getMessage());
        }
    }

    @GetMapping("/unread")
    public ResponseEntity<Map<String, Object>> getUnreadAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<AlertDTO> alerts = alertService.getUnreadAlerts(PageRequest.of(page, size));
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Unread alerts retrieved successfully");
            response.put("data", alerts.getContent());
            response.put("pagination", buildPaginationInfo(alerts));
            response.put("totalUnread", alerts.getTotalElements());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse("Failed to retrieve unread alerts", e.getMessage());
        }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount() {
        try {
            Long unreadCount = alertService.getUnreadCount();
            Map<String, Long> countsByType = new HashMap<>();
            for (AlertType type : AlertType.values()) {
                countsByType.put(type.name(), alertService.getUnreadCountByType(type));
            }
            return ResponseEntity.ok(Map.of("success", true, "unreadCount", unreadCount, "countsByType", countsByType));
        } catch (Exception e) {
            return buildErrorResponse("Failed to get unread count", e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getAlertById(@PathVariable Long id) {
        try {
            AlertDTO alert = alertService.getAlertById(id);
            return ResponseEntity.ok(Map.of("success", true, "data", alert));
        } catch (Exception e) {
            return buildErrorResponse("Alert not found", e.getMessage());
        }
    }

    @GetMapping("/document/{documentId}")
    public ResponseEntity<Map<String, Object>> getAlertsForDocument(@PathVariable Long documentId) {
        try {
            List<AlertDTO> alerts = alertService.getAlertsForDocument(documentId);
            return ResponseEntity.ok(Map.of("success", true, "data", alerts, "count", alerts.size(), "documentId", documentId));
        } catch (Exception e) {
            return buildErrorResponse("Failed to retrieve document alerts", e.getMessage());
        }
    }

    @DeleteMapping("/document/{documentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteAlertsForDocument(@PathVariable Long documentId) {
        try {
            alertService.deleteAlertsForDocument(documentId);
            return ResponseEntity.ok(Map.of("success", true, "message", "All alerts deleted for document", "documentId", documentId));
        } catch (Exception e) {
            return buildErrorResponse("Failed to delete document alerts", e.getMessage());
        }
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<Map<String, Object>> getAlertsByType(
            @PathVariable String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            AlertType alertType = AlertType.valueOf(type.toUpperCase());
            Page<AlertDTO> alerts = alertService.getAlertsByType(alertType, PageRequest.of(page, size));
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Alerts retrieved successfully");
            response.put("data", alerts.getContent());
            response.put("pagination", buildPaginationInfo(alerts));
            response.put("alertType", type);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse("Invalid alert type", "Valid types: " + java.util.Arrays.toString(AlertType.values()));
        } catch (Exception e) {
            return buildErrorResponse("Failed to retrieve alerts", e.getMessage());
        }
    }

    @GetMapping("/types")
    public ResponseEntity<Map<String, Object>> getAlertTypes() {
        Map<String, Object> types = new HashMap<>();
        for (AlertType type : AlertType.values()) {
            Map<String, String> typeInfo = new HashMap<>();
            typeInfo.put("name", type.name());
            typeInfo.put("displayName", AlertDTO.getAlertTypeName(type));
            typeInfo.put("icon", AlertDTO.getAlertIcon(type));
            typeInfo.put("colorClass", AlertDTO.getAlertColorClass(type));
            types.put(type.name(), typeInfo);
        }
        return ResponseEntity.ok(Map.of("success", true, "alertTypes", types));
    }

    @PostMapping("/mark-read/{id}")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id) {
        try {
            AlertDTO alert = alertService.markAsRead(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Alert marked as read", "data", alert));
        } catch (Exception e) {
            return buildErrorResponse("Failed to mark alert as read", e.getMessage());
        }
    }

    @PostMapping("/my/mark-read/{id}")
    public ResponseEntity<Map<String, Object>> markMyAlertAsRead(@PathVariable Long id) {
        try {
            Long userId = com.metrohub.security.SecurityUtils.getCurrentUserId();
            if (userId == null) {
                return buildErrorResponse("Unauthorized", "User not authenticated");
            }
            AlertDTO alert = notificationService.markAlertAsReadForCurrentUser(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Notification marked as read", "data", alert));
        } catch (Exception e) {
            return buildErrorResponse("Failed to mark notification as read", e.getMessage());
        }
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        try {
            int count = alertService.markAllAsRead();
            return ResponseEntity.ok(Map.of("success", true, "message", "All alerts marked as read", "markedCount", count));
        } catch (Exception e) {
            return buildErrorResponse("Failed to mark alerts as read", e.getMessage());
        }
    }

    @PostMapping("/mark-multiple-read")
    public ResponseEntity<Map<String, Object>> markMultipleAsRead(@RequestBody Map<String, List<Long>> requestBody) {
        List<Long> alertIds = requestBody.get("alertIds");
        try {
            if (alertIds == null || alertIds.isEmpty()) {
                return buildErrorResponse("No alert IDs provided", "alertIds list is required");
            }
            int count = alertService.markMultipleAsRead(alertIds);
            return ResponseEntity.ok(Map.of("success", true, "message", "Alerts marked as read", "markedCount", count, "requestedCount", alertIds.size()));
        } catch (Exception e) {
            return buildErrorResponse("Failed to mark alerts as read", e.getMessage());
        }
    }

    @PostMapping("/mark-read/document/{documentId}")
    public ResponseEntity<Map<String, Object>> markDocumentAlertsAsRead(@PathVariable Long documentId) {
        try {
            int count = alertService.markAllAsReadForDocument(documentId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Document alerts marked as read", "markedCount", count, "documentId", documentId));
        } catch (Exception e) {
            return buildErrorResponse("Failed to mark document alerts as read", e.getMessage());
        }
    }

    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<AlertDTO> alerts = notificationService.getAlertsForCurrentUser(PageRequest.of(page, size));
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User alerts retrieved successfully");
            response.put("data", alerts.getContent());
            response.put("pagination", buildPaginationInfo(alerts));
            response.put("unreadCount", notificationService.getUnreadCountForCurrentUser());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse("Failed to retrieve user alerts", e.getMessage());
        }
    }

    @GetMapping("/my/unread")
    public ResponseEntity<Map<String, Object>> getMyUnreadAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<AlertDTO> alerts = notificationService.getUnreadAlertsForCurrentUser(PageRequest.of(page, size));
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Unread user alerts retrieved successfully");
            response.put("data", alerts.getContent());
            response.put("pagination", buildPaginationInfo(alerts));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse("Failed to retrieve unread user alerts", e.getMessage());
        }
    }

    @GetMapping("/my/unread-count")
    public ResponseEntity<Map<String, Object>> getMyUnreadCount() {
        try {
            long unreadCount = notificationService.getUnreadCountForCurrentUser();
            return ResponseEntity.ok(Map.of("success", true, "unreadCount", unreadCount));
        } catch (Exception e) {
            return buildErrorResponse("Failed to get unread count", e.getMessage());
        }
    }

    @PostMapping("/my/mark-all-read")
    public ResponseEntity<Map<String, Object>> markAllMyAlertsAsRead() {
        try {
            int count = notificationService.markAllAsReadForCurrentUser();
            return ResponseEntity.ok(Map.of("success", true, "message", "All user alerts marked as read", "markedCount", count));
        } catch (Exception e) {
            return buildErrorResponse("Failed to mark alerts as read", e.getMessage());
        }
    }

    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'DEPARTMENT_UPLOAD_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAlertsByDepartment(
            @PathVariable Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<AlertDTO> alerts = notificationService.getAlertsByDepartment(departmentId, PageRequest.of(page, size));
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Department alerts retrieved successfully");
            response.put("data", alerts.getContent());
            response.put("pagination", buildPaginationInfo(alerts));
            response.put("departmentId", departmentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse("Failed to retrieve department alerts", e.getMessage());
        }
    }

    @PostMapping("/test-notification")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> testNotification(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "DASHBOARD") String channel,
            @RequestParam(defaultValue = "This is a test notification from MetroHub") String message) {
        try {
            Map<String, Object> result = notificationService.sendTestNotification(userId, channel, message);
            return ResponseEntity.ok(Map.of("success", true, "message", "Test notification sent", "result", result));
        } catch (Exception e) {
            return buildErrorResponse("Failed to send test notification", e.getMessage());
        }
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

    private ResponseEntity<Map<String, Object>> buildErrorResponse(String message, String error) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("error", error);
        return ResponseEntity.badRequest().body(response);
    }

    // DEBUG ENDPOINT - MANUALLY TRIGGER SCHEDULER
    @PostMapping("/debug/trigger-scheduler")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerScheduler() {
        try {
            complianceSchedulerService.processComplianceChecks();
            return ResponseEntity.ok(Map.of("success", true, "message", "Scheduler triggered successfully"));
        } catch (Exception e) {
            return buildErrorResponse("Failed to trigger scheduler", e.getMessage());
        }
    }
}
