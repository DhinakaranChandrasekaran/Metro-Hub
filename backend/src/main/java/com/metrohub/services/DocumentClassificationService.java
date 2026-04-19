package com.metrohub.services;

import com.metrohub.models.Department;
import com.metrohub.models.Document.DocumentType;
import com.metrohub.models.Document.Priority;
import com.metrohub.repositories.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DocumentClassificationService {

    private static final Logger log = LoggerFactory.getLogger(DocumentClassificationService.class);

    private final DepartmentRepository departmentRepository;

    // ============================================================
    // KEYWORD DEFINITIONS FOR CLASSIFICATION
    // ============================================================

    

    private static final Map<String, List<String>> DEPARTMENT_KEYWORDS = Map.of(

            // Safety Department
            "SAFETY", Arrays.asList(
                    "safety", "emergency", "fire", "accident", "hazard", "risk",
                    "evacuation", "drill", "incident", "injury", "first aid",
                    "protective equipment", "ppe", "safety circular", "warning"),

            // Finance Department
            "FINANCE", Arrays.asList(
                    "invoice", "amount", "payment", "gst", "tax", "budget",
                    "expenditure", "cost", "bill", "receipt", "financial",
                    "rupees", "rs.", "₹", "vendor payment", "salary", "wages"),

            // Maintenance Department
            "MAINT", Arrays.asList(
                    "maintenance", "job card", "repair", "equipment", "breakdown",
                    "escalator", "elevator", "lift", "hvac", "electrical",
                    "plumbing", "amc", "preventive", "corrective", "spare parts"),

            // Legal Department
            "LEGAL", Arrays.asList(
                    "contract", "agreement", "legal", "law", "court", "notice",
                    "compliance", "regulation", "tender", "bid", "rti",
                    "arbitration", "dispute", "litigation", "nda"),

            // HR Department
            "HR", Arrays.asList(
                    "employee", "staff", "leave", "hr", "human resources",
                    "recruitment", "training", "appraisal", "attendance",
                    "resignation", "appointment", "promotion", "transfer"),

            // Operations Department
            "OPS", Arrays.asList(
                    "train", "operations", "schedule", "timetable", "passenger",
                    "platform", "station", "signal", "track", "rolling stock",
                    "departure", "arrival", "service", "metro line"),

            // Engineering Department
            "ENGG", Arrays.asList(
                    "engineering", "design", "construction", "civil", "structural",
                    "drawing", "specification", "inspection", "quality", "testing"),

            // Admin/General Department
            "ADMIN", Arrays.asList(
                    "circular", "notice", "memo", "meeting", "minutes",
                    "announcement", "policy", "procedure", "guideline", "sop"));

    

    private static final Map<DocumentType, List<String>> DOCUMENT_TYPE_KEYWORDS = Map.of(

            DocumentType.JOB_CARD, Arrays.asList(
                    "job card", "work order", "maintenance task", "repair order"),

            DocumentType.INVOICE, Arrays.asList(
                    "invoice", "tax invoice", "proforma", "bill", "payment due"),

            DocumentType.POLICY, Arrays.asList(
                    "policy", "guideline", "procedure", "sop", "standard operating"),

            DocumentType.SAFETY_CIRCULAR, Arrays.asList(
                    "safety circular", "safety notice", "safety alert", "emergency procedure"),

            DocumentType.LEGAL_NOTICE, Arrays.asList(
                    "legal notice", "court order", "summon", "legal document"),

            DocumentType.CONTRACT, Arrays.asList(
                    "contract", "agreement", "mou", "memorandum of understanding"),

            DocumentType.MANUAL, Arrays.asList(
                    "manual", "handbook", "user guide", "operation manual"),

            DocumentType.REPORT, Arrays.asList(
                    "report", "analysis", "summary", "quarterly", "annual report"),

            DocumentType.MEMO, Arrays.asList(
                    "memo", "memorandum", "internal communication", "office order"),

            DocumentType.CERTIFICATE, Arrays.asList(
                    "certificate", "certification", "clearance", "approval"));

    

    private static final List<String> HIGH_PRIORITY_KEYWORDS = Arrays.asList(
            "urgent", "emergency", "immediate", "critical", "safety",
            "accident", "injury", "fire", "legal notice", "court",
            "deadline", "compliance", "violation", "mandatory", "asap");

    

    private static final List<String> LOW_PRIORITY_KEYWORDS = Arrays.asList(
            "information", "fyi", "for your information", "general",
            "circular", "newsletter", "update", "routine");

    // ============================================================
    // MAIN CLASSIFICATION METHOD
    // ============================================================

    

    public ClassificationResult classifyDocument(String extractedText, String fileName) {

        log.info("🔍 Starting document classification...");

        // Combine text and filename for analysis
        String combinedText = "";
        if (extractedText != null) {
            combinedText = extractedText.toLowerCase();
        }
        if (fileName != null) {
            combinedText += " " + fileName.toLowerCase();
        }

        if (combinedText.trim().isEmpty()) {
            log.warn("⚠️ No text available for classification");
            return new ClassificationResult(
                    DocumentType.OTHER,
                    null,
                    null,
                    Priority.MEDIUM,
                    0.0,
                    new String[] {});
        }

        // Detect components
        String detectedDepartment = detectDepartment(combinedText);
        DocumentType detectedType = detectDocumentType(combinedText);
        Priority detectedPriority = determinePriority(combinedText, detectedType);

        // Find department ID
        Long departmentId = null;
        if (detectedDepartment != null) {
            departmentId = findDepartmentIdByCode(detectedDepartment);
        }

        // Calculate confidence and get matched keywords
        MatchResult matchResult = calculateConfidence(combinedText);

        log.info("✅ Classification complete - Type: {}, Dept: {}, Priority: {}, Confidence: {:.2f}",
                detectedType, detectedDepartment, detectedPriority, matchResult.confidence);

        return new ClassificationResult(
                detectedType != null ? detectedType : DocumentType.OTHER,
                detectedDepartment,
                departmentId,
                detectedPriority,
                matchResult.confidence,
                matchResult.matchedKeywords.toArray(new String[0]));
    }

    // ============================================================
    // DEPARTMENT DETECTION
    // ============================================================

    

    public String detectDepartment(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        String normalizedText = text.toLowerCase();
        Map<String, Integer> departmentScores = new HashMap<>();

        // Count keyword matches for each department
        for (Map.Entry<String, List<String>> entry : DEPARTMENT_KEYWORDS.entrySet()) {
            String deptCode = entry.getKey();
            List<String> keywords = entry.getValue();

            int score = 0;
            for (String keyword : keywords) {
                if (normalizedText.contains(keyword)) {
                    // Longer keywords get higher weight
                    score += keyword.length() > 5 ? 2 : 1;
                }
            }

            if (score > 0) {
                departmentScores.put(deptCode, score);
            }
        }

        // Find department with highest score
        if (departmentScores.isEmpty()) {
            return null;
        }

        String bestMatch = departmentScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        log.debug("📊 Department detection - Best match: {} with score {}",
                bestMatch, departmentScores.get(bestMatch));

        return bestMatch;
    }

    // ============================================================
    // DOCUMENT TYPE DETECTION
    // ============================================================

    

    public DocumentType detectDocumentType(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        String normalizedText = text.toLowerCase();
        Map<DocumentType, Integer> typeScores = new HashMap<>();

        // Check each document type's keywords
        for (Map.Entry<DocumentType, List<String>> entry : DOCUMENT_TYPE_KEYWORDS.entrySet()) {
            DocumentType docType = entry.getKey();
            List<String> keywords = entry.getValue();

            int score = 0;
            for (String keyword : keywords) {
                if (normalizedText.contains(keyword)) {
                    score += keyword.split(" ").length; // Multi-word matches score higher
                }
            }

            if (score > 0) {
                typeScores.put(docType, score);
            }
        }

        // Find type with highest score
        if (typeScores.isEmpty()) {
            return DocumentType.OTHER;
        }

        DocumentType bestMatch = typeScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(DocumentType.OTHER);

        log.debug("📄 Document type detection - Best match: {}", bestMatch);

        return bestMatch;
    }

    // ============================================================
    // PRIORITY DETECTION
    // ============================================================

    

    public Priority determinePriority(String text, DocumentType documentType) {

        // Safety and legal documents are always high priority
        if (documentType == DocumentType.SAFETY_CIRCULAR ||
                documentType == DocumentType.LEGAL_NOTICE) {
            return Priority.HIGH;
        }

        if (text == null || text.isEmpty()) {
            return Priority.MEDIUM;
        }

        String normalizedText = text.toLowerCase();

        // Check for high priority keywords
        for (String keyword : HIGH_PRIORITY_KEYWORDS) {
            if (normalizedText.contains(keyword)) {
                log.debug("🔴 High priority keyword found: {}", keyword);
                return Priority.HIGH;
            }
        }

        // Check for low priority keywords
        for (String keyword : LOW_PRIORITY_KEYWORDS) {
            if (normalizedText.contains(keyword)) {
                // Only low priority if no high priority keywords
                log.debug("🟢 Low priority keyword found: {}", keyword);
                return Priority.LOW;
            }
        }

        // Default to medium
        return Priority.MEDIUM;
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    

    private Long findDepartmentIdByCode(String code) {
        try {
            Optional<Department> dept = departmentRepository.findByCode(code);
            return dept.map(Department::getId).orElse(null);
        } catch (Exception e) {
            log.warn("⚠️ Could not find department: {}", code);
            return null;
        }
    }

    

    private MatchResult calculateConfidence(String text) {
        List<String> matchedKeywords = new ArrayList<>();
        int totalMatches = 0;

        // Check department keywords
        for (List<String> keywords : DEPARTMENT_KEYWORDS.values()) {
            for (String keyword : keywords) {
                if (text.contains(keyword)) {
                    matchedKeywords.add(keyword);
                    totalMatches++;
                    if (matchedKeywords.size() >= 10)
                        break; // Limit stored keywords
                }
            }
        }

        // Check document type keywords
        for (List<String> keywords : DOCUMENT_TYPE_KEYWORDS.values()) {
            for (String keyword : keywords) {
                if (text.contains(keyword) && !matchedKeywords.contains(keyword)) {
                    matchedKeywords.add(keyword);
                    totalMatches++;
                    if (matchedKeywords.size() >= 10)
                        break;
                }
            }
        }

        // Calculate confidence (0.0 to 1.0)
        // More matches = higher confidence, capped at 1.0
        double confidence = Math.min(1.0, totalMatches * 0.15);

        // Boost confidence if multiple different categories matched
        if (matchedKeywords.size() >= 3) {
            confidence = Math.min(1.0, confidence + 0.1);
        }

        return new MatchResult(confidence, matchedKeywords);
    }

    

    private record MatchResult(double confidence, List<String> matchedKeywords) {
    }

    
    public record ClassificationResult(
            DocumentType documentType, String suggestedDepartment, Long suggestedDepartmentId,
            Priority priority, double confidence, String[] detectedKeywords) {
    }
}
