package com.metrohub.dto;

import com.metrohub.models.Document.DocumentType;
import com.metrohub.models.Document.Priority;
import lombok.*;

import java.time.LocalDateTime;

public class NlpDTOs {

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class NlpAnalysisRequestDTO {
        private String text;
        private String fileName;

        @Builder.Default
        private Integer maxSummarySentences = 3;
        @Builder.Default
        private Boolean includeKeywords = true;
        @Builder.Default
        private Boolean detectDeadlines = true;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class NlpAnalysisResultDTO {
        private Long documentId;
        private Boolean success;
        private String errorMessage;
        private LocalDateTime processingDate;
        private String departmentCode;
        private String departmentName;
        private Long departmentId;
        private Integer departmentScore;
        private Double departmentConfidence;
        private DocumentType documentType;
        private Integer documentTypeScore;
        private Double documentTypeConfidence;
        private Priority priority;
        private String priorityReason;
        private String summary;
        private Boolean deadlineFound;
        private String deadlineText;
        private String deadlineType;
        private String deadlineDate;
        private Integer daysUntilDeadline;
        private Double classificationConfidence;
        private String[] matchedKeywords;
        private Integer totalKeywordsMatched;
        private Boolean isManuallyClassified;
        private String fileName;
        private String extractionMethod;
        private Integer textLength;

        public String getConfidencePercentage() {
            if (classificationConfidence == null) return "0%";
            return String.format("%.1f%%", classificationConfidence * 100);
        }

        public boolean isHighConfidence() {
            return classificationConfidence != null && classificationConfidence >= 0.7;
        }

        public String getDeadlineUrgency() {
            if (!Boolean.TRUE.equals(deadlineFound) || daysUntilDeadline == null || daysUntilDeadline < 0) return "NONE";
            if (daysUntilDeadline <= 3) return "CRITICAL";
            if (daysUntilDeadline <= 7) return "URGENT";
            if (daysUntilDeadline <= 14) return "SOON";
            return "NORMAL";
        }
    }
}
