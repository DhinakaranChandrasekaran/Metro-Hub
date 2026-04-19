package com.metrohub.controllers;

import com.metrohub.dto.PolicyDTOs.*;
import com.metrohub.models.Document;
import com.metrohub.services.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/policies")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class PolicyController {

    private static final Logger log = LoggerFactory.getLogger(PolicyController.class);

    private final PolicyService policyService;

    @PostMapping
    @PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN', 'DEPARTMENT_UPLOAD_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> createPolicy(@Valid @RequestBody PolicyRuleRequestDTO requestDTO) {
        log.info("Creating policy: {}", requestDTO.getName());
        try {
            PolicyRuleDTO created = policyService.createPolicy(requestDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(buildResponse("Policy rule created successfully", created));
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Policy creation failed", e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create policy", e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN', 'DEPARTMENT_UPLOAD_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> updatePolicy(@PathVariable Long id, @Valid @RequestBody PolicyRuleRequestDTO requestDTO) {
        try {
            PolicyRuleDTO updated = policyService.updatePolicy(id, requestDTO);
            return ResponseEntity.ok(buildResponse("Policy rule updated successfully", updated));
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Policy update failed", e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update policy", e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DEPARTMENT_USER', 'DEPARTMENT_UPLOAD_ADMIN', 'DEPARTMENT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllPolicies() {
        try {
            List<PolicyRuleDTO> policies = policyService.getAllPolicies();
            Map<String, Object> response = buildResponse("Policies retrieved successfully", policies);
            response.put("count", policies.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve policies", e.getMessage());
        }
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('DEPARTMENT_USER', 'DEPARTMENT_UPLOAD_ADMIN', 'DEPARTMENT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getActivePolicies() {
        try {
            List<PolicyRuleDTO> policies = policyService.getActivePolicies();
            Map<String, Object> response = buildResponse("Active policies retrieved successfully", policies);
            response.put("count", policies.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve active policies", e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DEPARTMENT_USER', 'DEPARTMENT_UPLOAD_ADMIN', 'DEPARTMENT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getPolicyById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(buildResponse("Policy retrieved successfully", policyService.getPolicyById(id)));
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, "Policy not found", e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> deletePolicy(@PathVariable Long id) {
        try {
            policyService.deletePolicy(id);
            Map<String, Object> response = buildResponse("Policy rule deactivated successfully", null);
            response.put("policyId", id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Cannot delete policy", e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete policy", e.getMessage());
        }
    }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPARTMENT_ADMIN')")
    public ResponseEntity<Map<String, Object>> hardDeletePolicy(@PathVariable Long id) {
        try {
            policyService.hardDeletePolicy(id);
            Map<String, Object> response = buildResponse("Policy rule permanently deleted", null);
            response.put("policyId", id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Cannot delete policy", e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete policy", e.getMessage());
        }
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN', 'DEPARTMENT_UPLOAD_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> togglePolicy(@PathVariable Long id) {
        try {
            PolicyRuleDTO toggled = policyService.togglePolicy(id);
            return ResponseEntity.ok(buildResponse("Policy status toggled", toggled));
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Toggle failed", e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to toggle policy", e.getMessage());
        }
    }

    @GetMapping("/lookup")
    @PreAuthorize("hasAnyRole('DEPARTMENT_USER', 'DEPARTMENT_UPLOAD_ADMIN', 'DEPARTMENT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> lookupPolicy(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Document.Priority priority) {
        try {
            PolicyRuleDTO policy = policyService.findPolicy(departmentId, priority)
                    .map(policyService::toDTO)
                    .orElse(policyService.toDTO(policyService.getDefaultPolicy()));
            Map<String, Object> response = buildResponse("Applicable policy found", policy);
            response.put("searchCriteria", Map.of(
                    "departmentId", departmentId != null ? departmentId : "all",
                    "priority", priority != null ? priority.name() : "all"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to lookup policy", e.getMessage());
        }
    }

    @GetMapping("/default")
    @PreAuthorize("hasAnyRole('DEPARTMENT_USER', 'DEPARTMENT_UPLOAD_ADMIN', 'DEPARTMENT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDefaultPolicy() {
        try {
            return ResponseEntity.ok(buildResponse("Default policy retrieved successfully", policyService.toDTO(policyService.getDefaultPolicy())));
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve default policy", e.getMessage());
        }
    }

    private Map<String, Object> buildResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        if (data != null) response.put("data", data);
        return response;
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message, String details) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("error", details);
        return ResponseEntity.status(status).body(response);
    }
}
