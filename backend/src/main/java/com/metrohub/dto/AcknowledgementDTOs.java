package com.metrohub.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

public class AcknowledgementDTOs {

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AcknowledgementDTO {
        private Long id;
        private Long documentId;
        private String documentName;
        private String documentType;
        private String priority;
        private Long userId;
        private String userName;
        private String userEmail;
        private String employeeId;
        private String departmentName;
        private LocalDateTime acknowledgedAt;
        private String ipAddress;
        private String notes;
        private boolean acknowledged;
        private String timeAgo;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AcknowledgementRequestDTO {
        @NotNull(message = "Document ID is required")
        private Long documentId;

        @Size(max = 500, message = "Notes cannot exceed 500 characters")
        private String notes;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AcknowledgementResponseDTO {
        private Long id;
        private Long documentId;
        private String documentName;
        private Long userId;
        private String userName;
        private String userEmail;
        private Long departmentId;
        private String departmentName;
        private LocalDateTime acknowledgedAt;
        private String notes;
        private String ipAddress;
        private boolean acknowledged;

        public static AcknowledgementResponseDTO success(Long documentId, String documentName,
                Long userId, String userName, LocalDateTime acknowledgedAt) {
            return AcknowledgementResponseDTO.builder()
                    .documentId(documentId)
                    .documentName(documentName)
                    .userId(userId)
                    .userName(userName)
                    .acknowledgedAt(acknowledgedAt)
                    .acknowledged(true)
                    .build();
        }
    }
}
