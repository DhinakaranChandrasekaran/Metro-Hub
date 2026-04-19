package com.metrohub.services;

import com.metrohub.dto.NlpDTOs.NlpAnalysisResultDTO;
import com.metrohub.models.Department;
import com.metrohub.models.Document;
import com.metrohub.models.Document.DocumentType;
import com.metrohub.models.Document.Priority;
import com.metrohub.models.Metadata;
import com.metrohub.repositories.DepartmentRepository;
import com.metrohub.repositories.DocumentRepository;
import com.metrohub.repositories.MetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentNlpService {

    private static final Logger log = LoggerFactory.getLogger(DocumentNlpService.class);

    private final DocumentRepository documentRepository;
    private final DepartmentRepository departmentRepository;
    private final MetadataRepository metadataRepository;

    // ============================================================
    // KEYWORD DEFINITIONS FOR DEPARTMENT CLASSIFICATION
    // ============================================================
    // Format: Department Code → Keywords with weights
    // Weight: Higher weight = more specific indicator
    // ============================================================

    private static final Map<String, Map<String, Integer>> DEPARTMENT_KEYWORD_WEIGHTS = new LinkedHashMap<>() {
        {

            // Safety & Quality Department
            put("SAFE", new LinkedHashMap<>() {
                {
                    put("safety", 5);
                    put("emergency", 5);
                    put("fire", 4);
                    put("accident", 5);
                    put("hazard", 4);
                    put("evacuation", 5);
                    put("drill", 3);
                    put("incident", 4);
                    put("injury", 4);
                    put("first aid", 4);
                    put("ppe", 4);
                    put("protective equipment", 4);
                    put("safety circular", 6);
                    put("warning", 2);
                    put("risk assessment", 5);
                    put("safety audit", 5);
                    put("occupational health", 4);
                    put("safety violation", 5);
                    put("near miss", 4);
                }
            });

            // Finance & Accounts Department
            put("FIN", new LinkedHashMap<>() {
                {
                    put("invoice", 5);
                    put("tax invoice", 6);
                    put("gst", 4);
                    put("payment", 3);
                    put("amount", 2);
                    put("budget", 4);
                    put("expenditure", 4);
                    put("rupees", 3);
                    put("rs.", 3);
                    put("₹", 3);
                    put("inr", 3);
                    put("vendor payment", 5);
                    put("salary", 3);
                    put("financial", 4);
                    put("accounts", 3);
                    put("purchase order", 4);
                    put("billing", 4);
                    put("tax", 3);
                    put("debit", 3);
                    put("credit", 3);
                }
            });

            // Maintenance Department
            put("MAINT", new LinkedHashMap<>() {
                {
                    put("maintenance", 5);
                    put("job card", 6);
                    put("work order", 5);
                    put("repair", 4);
                    put("equipment", 3);
                    put("breakdown", 5);
                    put("escalator", 4);
                    put("elevator", 4);
                    put("lift", 3);
                    put("hvac", 4);
                    put("electrical", 3);
                    put("plumbing", 3);
                    put("amc", 4);
                    put("preventive maintenance", 6);
                    put("corrective", 4);
                    put("spare parts", 4);
                    put("asset", 3);
                    put("fault", 4);
                    put("servicing", 3);
                }
            });

            // Legal & Compliance Department
            put("LEGAL", new LinkedHashMap<>() {
                {
                    put("contract", 5);
                    put("agreement", 5);
                    put("legal", 4);
                    put("law", 3);
                    put("court", 5);
                    put("legal notice", 6);
                    put("compliance", 4);
                    put("regulation", 4);
                    put("tender", 4);
                    put("bid", 3);
                    put("rti", 4);
                    put("arbitration", 5);
                    put("dispute", 4);
                    put("litigation", 5);
                    put("nda", 4);
                    put("non-disclosure", 5);
                    put("terms and conditions", 4);
                    put("clause", 3);
                    put("statutory", 4);
                }
            });

            // Human Resources Department
            put("HR", new LinkedHashMap<>() {
                {
                    put("employee", 4);
                    put("staff", 3);
                    put("leave", 4);
                    put("hr", 3);
                    put("human resources", 5);
                    put("recruitment", 5);
                    put("training", 4);
                    put("appraisal", 5);
                    put("attendance", 4);
                    put("resignation", 5);
                    put("appointment", 5);
                    put("promotion", 4);
                    put("transfer", 3);
                    put("joining", 4);
                    put("offer letter", 5);
                    put("experience letter", 5);
                    put("deputation", 4);
                    put("disciplinary", 5);
                }
            });

            // Operations Department
            put("OPS", new LinkedHashMap<>() {
                {
                    put("train", 3);
                    put("operations", 4);
                    put("schedule", 3);
                    put("timetable", 5);
                    put("passenger", 4);
                    put("platform", 3);
                    put("station", 3);
                    put("signal", 4);
                    put("signaling", 5);
                    put("track", 3);
                    put("rolling stock", 5);
                    put("departure", 3);
                    put("arrival", 3);
                    put("metro line", 5);
                    put("service disruption", 5);
                    put("frequency", 3);
                    put("headway", 5);
                    put("occ", 4); // Operations Control Center
                }
            });

            // Engineering Department
            put("ENG", new LinkedHashMap<>() {
                {
                    put("engineering", 4);
                    put("design", 3);
                    put("construction", 4);
                    put("civil", 4);
                    put("structural", 4);
                    put("drawing", 3);
                    put("specification", 4);
                    put("inspection", 4);
                    put("quality control", 5);
                    put("testing", 3);
                    put("project", 3);
                    put("execution", 3);
                    put("commissioning", 5);
                    put("validation", 3);
                }
            });

            // Procurement Department
            put("PROC", new LinkedHashMap<>() {
                {
                    put("procurement", 5);
                    put("purchase", 4);
                    put("vendor", 4);
                    put("supplier", 4);
                    put("quotation", 5);
                    put("rfq", 5);
                    put("rfp", 5);
                    put("tender", 4);
                    put("bid evaluation", 5);
                    put("rate contract", 5);
                    put("indent", 4);
                    put("delivery", 3);
                    put("consignment", 4);
                }
            });

            // IT & Systems Department
            put("IT", new LinkedHashMap<>() {
                {
                    put("software", 4);
                    put("system", 3);
                    put("application", 3);
                    put("database", 4);
                    put("network", 4);
                    put("server", 4);
                    put("it", 3);
                    put("cyber security", 5);
                    put("backup", 4);
                    put("technical support", 4);
                    put("erp", 4);
                    put("portal", 3);
                    put("password", 3);
                    put("access control", 4);
                }
            });

            // Administration Department
            put("ADMIN", new LinkedHashMap<>() {
                {
                    put("circular", 4);
                    put("notice", 3);
                    put("memo", 4);
                    put("meeting", 3);
                    put("minutes", 4);
                    put("announcement", 3);
                    put("policy", 4);
                    put("procedure", 3);
                    put("guideline", 4);
                    put("sop", 4);
                    put("office order", 5);
                    put("general", 2);
                    put("administration", 4);
                }
            });
        }
    };

    // ============================================================
    // DOCUMENT TYPE KEYWORDS
    // ============================================================

    private static final Map<DocumentType, Map<String, Integer>> DOCUMENT_TYPE_KEYWORDS = new LinkedHashMap<>() {
        {

            put(DocumentType.JOB_CARD, Map.of(
                    "job card", 10, "work order", 8, "maintenance task", 8,
                    "repair order", 8, "service request", 6, "equipment id", 5));

            put(DocumentType.INVOICE, Map.of(
                    "tax invoice", 10, "invoice", 8, "invoice no", 9,
                    "bill", 5, "payment due", 7, "gstin", 8, "igst", 7, "cgst", 7));

            put(DocumentType.POLICY, Map.of(
                    "policy", 8, "guideline", 6, "procedure", 5, "sop", 7,
                    "standard operating", 8, "effective from", 6, "applicable to", 5));

            put(DocumentType.SAFETY_CIRCULAR, Map.of(
                    "safety circular", 10, "safety notice", 9, "safety alert", 9,
                    "emergency procedure", 8, "safety advisory", 8, "safety bulletin", 8));

            put(DocumentType.LEGAL_NOTICE, Map.of(
                    "legal notice", 10, "court order", 9, "summon", 8,
                    "show cause notice", 9, "legal document", 7, "advocate", 6));

            put(DocumentType.CONTRACT, Map.of(
                    "contract", 8, "agreement", 7, "mou", 8,
                    "memorandum of understanding", 9, "terms and conditions", 7,
                    "parties", 4, "whereas", 5, "witnesseth", 7));

            put(DocumentType.MANUAL, Map.of(
                    "manual", 8, "handbook", 7, "user guide", 8,
                    "operation manual", 9, "reference guide", 7, "instructions", 5));

            put(DocumentType.REPORT, Map.of(
                    "report", 6, "analysis", 5, "summary", 4, "findings", 6,
                    "quarterly report", 8, "annual report", 8, "inspection report", 8));

            put(DocumentType.MEMO, Map.of(
                    "memo", 8, "memorandum", 8, "internal communication", 7,
                    "office order", 8, "office memo", 9, "inter-office", 7));

            put(DocumentType.CERTIFICATE, Map.of(
                    "certificate", 8, "certification", 7, "clearance", 6,
                    "approval", 5, "certified", 6, "hereby certify", 9));
        }
    };

    // ============================================================
    // PRIORITY KEYWORDS
    // ============================================================

    private static final Map<Priority, List<String>> PRIORITY_KEYWORDS = Map.of(
            Priority.HIGH, Arrays.asList(
                    "urgent", "emergency", "immediate", "critical", "safety",
                    "accident", "injury", "fire", "legal notice", "court",
                    "deadline", "compliance", "violation", "mandatory", "asap",
                    "top priority", "life threatening", "hazardous", "alert",
                    "show cause", "final notice", "legal action"),
            Priority.MEDIUM, Arrays.asList(
                    "important", "attention required", "action required", "review",
                    "approval needed", "pending", "follow up", "standard",
                    "routine maintenance", "quarterly", "monthly"),
            Priority.LOW, Arrays.asList(
                    "information", "fyi", "for your information", "general",
                    "circular", "newsletter", "update", "routine", "reference",
                    "awareness", "informative", "no action required"));

    // ============================================================
    // DEADLINE DETECTION PATTERNS
    // ============================================================

    private static final List<Pattern> DEADLINE_PATTERNS = Arrays.asList(
            // "within X days/weeks/months"
            Pattern.compile("within\\s+(\\d+)\\s+(day|days|week|weeks|month|months)", Pattern.CASE_INSENSITIVE),
            // "before DD/MM/YYYY" or "before DD-MM-YYYY"
            Pattern.compile("before\\s+(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})", Pattern.CASE_INSENSITIVE),
            // "by DD/MM/YYYY"
            Pattern.compile("by\\s+(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})", Pattern.CASE_INSENSITIVE),
            // "deadline: DD/MM/YYYY"
            Pattern.compile("deadline[:\\s]+(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})", Pattern.CASE_INSENSITIVE),
            // "due date: DD/MM/YYYY"
            Pattern.compile("due\\s+date[:\\s]+(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})", Pattern.CASE_INSENSITIVE),
            // "effective from DD/MM/YYYY"
            Pattern.compile("effective\\s+from\\s+(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})", Pattern.CASE_INSENSITIVE),
            // "last date: DD/MM/YYYY"
            Pattern.compile("last\\s+date[:\\s]+(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})", Pattern.CASE_INSENSITIVE),
            // "complete by DD/MM/YYYY"
            Pattern.compile("complete\\s+by\\s+(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})", Pattern.CASE_INSENSITIVE),
            // "not later than DD/MM/YYYY"
            Pattern.compile("not\\s+later\\s+than\\s+(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})", Pattern.CASE_INSENSITIVE),
            // Date formats: DD Month YYYY
            Pattern.compile(
                    "(\\d{1,2}\\s+(?:January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{4})",
                    Pattern.CASE_INSENSITIVE));

    // ============================================================
    // MAIN ANALYSIS METHOD
    // ============================================================

    

    @Transactional
    public NlpAnalysisResultDTO analyzeDocument(Long documentId) {
        return analyzeDocument(documentId, false);
    }

    

    @Transactional
    public NlpAnalysisResultDTO analyzeDocument(Long documentId, boolean force) {

        log.info("═══════════════════════════════════════════════════════════");
        log.info("🔬 Starting NLP classification for document ID: {} (force={})", documentId, force);
        log.info("═══════════════════════════════════════════════════════════");

        // Load document
        Optional<Document> docOpt = documentRepository.findById(documentId);
        if (docOpt.isEmpty()) {
            throw new RuntimeException("Document not found with ID: " + documentId);
        }
        Document document = docOpt.get();

        // Check if text is extracted
        if (!Boolean.TRUE.equals(document.getIsTextExtracted()) ||
                document.getExtractedText() == null ||
                document.getExtractedText().trim().isEmpty()) {

            log.warn("⚠️ Document {} has no extracted text. NLP analysis skipped.", documentId);
            return NlpAnalysisResultDTO.builder()
                    .documentId(documentId)
                    .success(false)
                    .errorMessage("No extracted text available for NLP analysis")
                    .processingDate(LocalDateTime.now())
                    .build();
        }

        String extractedText = document.getExtractedText();
        String fileName = document.getFileName();

        // Perform NLP analysis
        NlpAnalysisResultDTO result = analyzeText(extractedText, fileName);
        result.setDocumentId(documentId);

        // Update document in database (force overrides manual classification check)
        updateDocumentWithNlpResults(document, result, force);

        log.info("✅ Phase 4 classification completed for document ID: {}", documentId);
        log.info("   📁 Department: {}", result.getDepartmentName());
        log.info("   📄 Type: {}", result.getDocumentType());
        log.info("   ⚡ Priority: {}", result.getPriority());
        log.info("   📊 Confidence: {:.2f}", result.getClassificationConfidence());
        log.info("═══════════════════════════════════════════════════════════");

        return result;
    }

    

    public NlpAnalysisResultDTO analyzeText(String text, String fileName) {

        log.info("📝 Analyzing text content ({} characters)", text != null ? text.length() : 0);

        if (text == null || text.trim().isEmpty()) {
            return NlpAnalysisResultDTO.builder()
                    .success(false)
                    .errorMessage("Empty text provided")
                    .processingDate(LocalDateTime.now())
                    .build();
        }

        // Combine text and filename for analysis
        String combinedText = text.toLowerCase();
        if (fileName != null) {
            combinedText += " " + fileName.toLowerCase();
        }

        // 1. Classify department
        DepartmentClassificationResult deptResult = classifyDepartment(combinedText);
        log.info("🏢 Detected department: {} (Score: {}, Confidence: {:.2f})",
                deptResult.departmentName(), deptResult.matchScore(), deptResult.confidence());

        // 2. Detect document type
        DocumentTypeResult typeResult = detectDocumentType(combinedText, fileName);
        log.info("📄 Detected type: {} (Score: {}, Confidence: {:.2f})",
                typeResult.documentType(), typeResult.matchScore(), typeResult.confidence());

        // 3. Determine priority
        PriorityResult priorityResult = determinePriority(combinedText, typeResult.documentType());
        log.info("⚡ Detected priority: {} (Reason: {})",
                priorityResult.priority(), priorityResult.reason());

        // 4. Generate summary
        String summary = generateSummary(text, 3);
        log.info("📝 Summary generated successfully ({} characters)", summary != null ? summary.length() : 0);

        // 5. Detect deadline
        DeadlineResult deadlineResult = detectDeadline(text);
        if (deadlineResult.deadlineFound()) {
            log.info("📅 Deadline detected: {} (Type: {})",
                    deadlineResult.deadlineText(), deadlineResult.deadlineType());
        }

        // 6. Calculate overall confidence
        double overallConfidence = calculateOverallConfidence(deptResult, typeResult, priorityResult);

        // Collect all matched keywords
        Set<String> allKeywords = new LinkedHashSet<>();
        if (deptResult.matchedKeywords() != null) {
            allKeywords.addAll(Arrays.asList(deptResult.matchedKeywords()));
        }
        if (typeResult.matchedKeywords() != null) {
            allKeywords.addAll(Arrays.asList(typeResult.matchedKeywords()));
        }
        if (priorityResult.triggerKeywords() != null) {
            allKeywords.addAll(Arrays.asList(priorityResult.triggerKeywords()));
        }

        return NlpAnalysisResultDTO.builder()
                .success(true)
                .processingDate(LocalDateTime.now())
                // Department info
                .departmentCode(deptResult.departmentCode())
                .departmentName(deptResult.departmentName())
                .departmentId(deptResult.departmentId())
                .departmentScore(deptResult.matchScore())
                .departmentConfidence(deptResult.confidence())
                // Document type info
                .documentType(typeResult.documentType())
                .documentTypeScore(typeResult.matchScore())
                .documentTypeConfidence(typeResult.confidence())
                // Priority info
                .priority(priorityResult.priority())
                .priorityReason(priorityResult.reason())
                // Summary
                .summary(summary)
                // Deadline
                .deadlineFound(deadlineResult.deadlineFound())
                .deadlineText(deadlineResult.deadlineText())
                .deadlineType(deadlineResult.deadlineType())
                .deadlineDate(deadlineResult.normalizedDate())
                // Overall
                .classificationConfidence(overallConfidence)
                .matchedKeywords(allKeywords.toArray(new String[0]))
                .isManuallyClassified(false)
                .build();
    }

    // ============================================================
    // DEPARTMENT CLASSIFICATION
    // ============================================================

    public DepartmentClassificationResult classifyDepartment(String text) {

        if (text == null || text.isEmpty()) {
            return new DepartmentClassificationResult(null, null, null, 0, 0.0, new String[] {});
        }

        String normalizedText = text.toLowerCase();
        Map<String, Integer> departmentScores = new HashMap<>();
        Map<String, List<String>> departmentMatchedKeywords = new HashMap<>();

        // Calculate scores for each department
        for (Map.Entry<String, Map<String, Integer>> deptEntry : DEPARTMENT_KEYWORD_WEIGHTS.entrySet()) {
            String deptCode = deptEntry.getKey();
            Map<String, Integer> keywords = deptEntry.getValue();

            int totalScore = 0;
            List<String> matched = new ArrayList<>();

            for (Map.Entry<String, Integer> kwEntry : keywords.entrySet()) {
                String keyword = kwEntry.getKey();
                int weight = kwEntry.getValue();

                // Count occurrences (limited to avoid over-counting)
                int count = countOccurrences(normalizedText, keyword);
                if (count > 0) {
                    totalScore += weight * Math.min(count, 3); // Cap at 3 occurrences
                    matched.add(keyword);
                }
            }

            if (totalScore > 0) {
                departmentScores.put(deptCode, totalScore);
                departmentMatchedKeywords.put(deptCode, matched);
            }
        }

        // Find best match
        if (departmentScores.isEmpty()) {
            return new DepartmentClassificationResult(null, null, null, 0, 0.0, new String[] {});
        }

        String bestDept = departmentScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        int bestScore = departmentScores.get(bestDept);
        List<String> matchedKeywords = departmentMatchedKeywords.get(bestDept);

        // Calculate confidence
        int totalPossibleScore = DEPARTMENT_KEYWORD_WEIGHTS.get(bestDept).values().stream()
                .mapToInt(Integer::intValue).sum();
        double confidence = Math.min(1.0, (double) bestScore / (totalPossibleScore * 0.3));

        // Get department details from database
        String deptName = getDepartmentName(bestDept);
        Long deptId = getDepartmentId(bestDept);

        return new DepartmentClassificationResult(
                bestDept,
                deptName,
                deptId,
                bestScore,
                confidence,
                matchedKeywords.toArray(new String[0]));
    }

    // ============================================================
    // DOCUMENT TYPE DETECTION
    // ============================================================

    public DocumentTypeResult detectDocumentType(String text, String fileName) {

        if (text == null || text.isEmpty()) {
            return new DocumentTypeResult(DocumentType.OTHER, 0, 0.0, new String[] {});
        }

        String normalizedText = text.toLowerCase();
        if (fileName != null) {
            normalizedText += " " + fileName.toLowerCase();
        }

        Map<DocumentType, Integer> typeScores = new HashMap<>();
        Map<DocumentType, List<String>> typeMatchedKeywords = new HashMap<>();

        // Calculate scores for each document type
        for (Map.Entry<DocumentType, Map<String, Integer>> typeEntry : DOCUMENT_TYPE_KEYWORDS.entrySet()) {
            DocumentType docType = typeEntry.getKey();
            Map<String, Integer> keywords = typeEntry.getValue();

            int totalScore = 0;
            List<String> matched = new ArrayList<>();

            for (Map.Entry<String, Integer> kwEntry : keywords.entrySet()) {
                String keyword = kwEntry.getKey();
                int weight = kwEntry.getValue();

                int count = countOccurrences(normalizedText, keyword);
                if (count > 0) {
                    totalScore += weight * Math.min(count, 2);
                    matched.add(keyword);
                }
            }

            if (totalScore > 0) {
                typeScores.put(docType, totalScore);
                typeMatchedKeywords.put(docType, matched);
            }
        }

        // Find best match
        if (typeScores.isEmpty()) {
            return new DocumentTypeResult(DocumentType.OTHER, 0, 0.0, new String[] {});
        }

        DocumentType bestType = typeScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(DocumentType.OTHER);

        int bestScore = typeScores.get(bestType);
        List<String> matchedKeywords = typeMatchedKeywords.get(bestType);

        // Calculate confidence
        int totalPossibleScore = DOCUMENT_TYPE_KEYWORDS.get(bestType).values().stream()
                .mapToInt(Integer::intValue).sum();
        double confidence = Math.min(1.0, (double) bestScore / (totalPossibleScore * 0.25));

        return new DocumentTypeResult(
                bestType,
                bestScore,
                confidence,
                matchedKeywords.toArray(new String[0]));
    }

    // ============================================================
    // PRIORITY DETERMINATION
    // ============================================================

    public PriorityResult determinePriority(String text, DocumentType documentType) {

        // Safety circulars and legal notices are always HIGH priority
        if (documentType == DocumentType.SAFETY_CIRCULAR ||
                documentType == DocumentType.LEGAL_NOTICE) {
            return new PriorityResult(Priority.HIGH,
                    "Document type (" + documentType + ") requires high priority",
                    new String[] { documentType.toString() });
        }

        if (text == null || text.isEmpty()) {
            return new PriorityResult(Priority.MEDIUM, "Default priority", new String[] {});
        }

        String normalizedText = text.toLowerCase();

        // Check for HIGH priority keywords first
        List<String> highMatches = new ArrayList<>();
        for (String keyword : PRIORITY_KEYWORDS.get(Priority.HIGH)) {
            if (normalizedText.contains(keyword)) {
                highMatches.add(keyword);
            }
        }
        if (!highMatches.isEmpty()) {
            return new PriorityResult(Priority.HIGH,
                    "High priority keywords detected: " + String.join(", ", highMatches),
                    highMatches.toArray(new String[0]));
        }

        // Check for LOW priority keywords
        List<String> lowMatches = new ArrayList<>();
        for (String keyword : PRIORITY_KEYWORDS.get(Priority.LOW)) {
            if (normalizedText.contains(keyword)) {
                lowMatches.add(keyword);
            }
        }
        if (!lowMatches.isEmpty()) {
            return new PriorityResult(Priority.LOW,
                    "Informational keywords detected: " + String.join(", ", lowMatches),
                    lowMatches.toArray(new String[0]));
        }

        // Check for MEDIUM priority indicators
        List<String> mediumMatches = new ArrayList<>();
        for (String keyword : PRIORITY_KEYWORDS.get(Priority.MEDIUM)) {
            if (normalizedText.contains(keyword)) {
                mediumMatches.add(keyword);
            }
        }
        if (!mediumMatches.isEmpty()) {
            return new PriorityResult(Priority.MEDIUM,
                    "Standard priority keywords detected: " + String.join(", ", mediumMatches),
                    mediumMatches.toArray(new String[0]));
        }

        return new PriorityResult(Priority.MEDIUM, "Default priority (no specific indicators)", new String[] {});
    }

    // ============================================================
    // SUMMARY GENERATION (Extractive)
    // ============================================================

    public String generateSummary(String text, int maxSentences) {

        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        // Clean and normalize text
        String cleanText = text
                .replaceAll("\\s+", " ") // Normalize whitespace
                .replaceAll("[\\r\\n]+", ". ") // Replace newlines with periods
                .trim();

        // Split into sentences
        String[] sentences = cleanText.split("(?<=[.!?])\\s+");

        if (sentences.length == 0) {
            // If no sentence boundaries, take first 200 characters
            return cleanText.length() > 200 ? cleanText.substring(0, 200) + "..." : cleanText;
        }

        // Score sentences based on keyword presence and position
        Map<String, Double> sentenceScores = new LinkedHashMap<>();

        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i].trim();

            // Skip very short or very long sentences
            if (sentence.length() < 20 || sentence.length() > 500) {
                continue;
            }

            double score = 0.0;
            String lowerSentence = sentence.toLowerCase();

            // Position bonus: first sentences are usually important
            if (i < 3) {
                score += (3 - i) * 2.0;
            }

            // Keyword presence bonus
            for (List<String> keywords : PRIORITY_KEYWORDS.values()) {
                for (String keyword : keywords) {
                    if (lowerSentence.contains(keyword)) {
                        score += 1.5;
                    }
                }
            }

            // Department keyword bonus (document is about something specific)
            for (Map<String, Integer> keywords : DEPARTMENT_KEYWORD_WEIGHTS.values()) {
                for (String keyword : keywords.keySet()) {
                    if (lowerSentence.contains(keyword)) {
                        score += 1.0;
                    }
                }
            }

            // Penalize sentences that look like headers or signatures
            if (sentence.matches("^[A-Z\\s]+$") ||
                    sentence.contains("Regards") ||
                    sentence.contains("Sincerely")) {
                score -= 5.0;
            }

            sentenceScores.put(sentence, score);
        }

        // Select top sentences maintaining original order
        List<String> selectedSentences = sentenceScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(maxSentences)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Reorder by original position
        List<String> orderedSummary = new ArrayList<>();
        for (String sentence : sentences) {
            if (selectedSentences.contains(sentence.trim())) {
                orderedSummary.add(sentence.trim());
            }
            if (orderedSummary.size() >= maxSentences) {
                break;
            }
        }

        String summary = String.join(" ", orderedSummary);

        // Limit length
        if (summary.length() > 500) {
            summary = summary.substring(0, 497) + "...";
        }

        return summary;
    }

    // ============================================================
    // DEADLINE DETECTION
    // ============================================================

    public DeadlineResult detectDeadline(String text) {

        if (text == null || text.isEmpty()) {
            return new DeadlineResult(false, null, null, null, -1);
        }

        for (Pattern pattern : DEADLINE_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String matchedText = matcher.group(0);
                String dateOrPeriod = matcher.group(1);

                String deadlineType = determineDeadlineType(matchedText);
                String normalizedDate = tryNormalizeDate(dateOrPeriod);
                int daysFromNow = calculateDaysFromNow(dateOrPeriod);

                return new DeadlineResult(
                        true,
                        matchedText,
                        normalizedDate,
                        deadlineType,
                        daysFromNow);
            }
        }

        return new DeadlineResult(false, null, null, null, -1);
    }

    // ============================================================
    // ASYNC PROCESSING
    // ============================================================

    @Async("nlpProcessingExecutor")
    public void processDocumentAsync(Long documentId) {

        log.info("🚀 Starting async NLP processing for document ID: {}", documentId);

        try {
            // Small delay to ensure text extraction is complete
            Thread.sleep(1000);
            analyzeDocument(documentId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ NLP processing interrupted for document ID: {}", documentId);
        } catch (Exception e) {
            log.error("❌ NLP processing failed for document ID: {}: {}", documentId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public boolean isNlpProcessed(Long documentId) {
        return documentRepository.findById(documentId)
                .map(this::hasClassificationConfidence)
                .orElse(false);
    }

    @Transactional
    public NlpAnalysisResultDTO reanalyzeDocument(Long documentId) {
        log.info("🔄 Re-analyzing document ID: {}", documentId);
        return analyzeDocument(documentId);
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    

    private void updateDocumentWithNlpResults(Document document, NlpAnalysisResultDTO result, boolean force) {

        // Only update if not manually classified (unless force is true)
        if (force || !Boolean.TRUE.equals(document.getIsManuallyClassified())) {

            // 🔒 DO NOT change department during NLP processing
            // Department is set at upload time and should NEVER be overwritten
            // This prevents breaking acknowledgement workflows
            // if (result.getDepartmentId() != null) {
            //     departmentRepository.findById(result.getDepartmentId())
            //             .ifPresent(document::setDepartment);
            // }

            // 🔒 DO NOT override document type if user provided one at upload
            // Only update if it's empty/null (auto NLP classification)
            if (result.getDocumentType() != null && (document.getDocumentType() == null || document.getDocumentType() == DocumentType.OTHER)) {
                document.setDocumentType(result.getDocumentType());
                log.info("📝 Set document type from NLP: {}", result.getDocumentType());
            } else if (document.getDocumentType() != null) {
                log.info("✅ Keeping user-provided document type: {}", document.getDocumentType());
            }

            // 🔒 DO NOT override priority if user provided one at upload
            // Only update if it's empty/null (auto NLP classification)
            if (result.getPriority() != null && document.getPriority() == null) {
                document.setPriority(result.getPriority());
                log.info("🔴 Set priority from NLP: {}", result.getPriority());
            } else if (document.getPriority() != null) {
                log.info("✅ Keeping user-provided priority: {}", document.getPriority());
            }

            // Set classification confidence
            document.setClassificationConfidence(result.getClassificationConfidence());

            // Build description with summary and deadline
            StringBuilder description = new StringBuilder();
            if (result.getSummary() != null && !result.getSummary().isEmpty()) {
                description.append(result.getSummary());
            }
            if (result.getDeadlineFound() && result.getDeadlineText() != null) {
                if (description.length() > 0) {
                    description.append(" | ");
                }
                description.append("DEADLINE: ").append(result.getDeadlineText());
            }
            if (description.length() > 0) {
                // Only set auto-generated description if user didn't provide one during upload
                if (document.getDescription() == null || document.getDescription().isBlank()) {
                    String desc = description.toString();
                    if (desc.length() > 500) {
                        desc = desc.substring(0, 497) + "...";
                    }
                    document.setDescription(desc);
                } else {
                    log.info("⏭️ Keeping user-provided description for document {}", document.getId());
                }
            }

            // Mark as automatically classified
            document.setIsManuallyClassified(false);

            // Save document
            documentRepository.save(document);
            log.info("💾 Document {} updated with NLP classification results", document.getId());

            // ============================================
            // UPDATE METADATA TABLE WITH EXTRACTED INFO
            // ============================================
            updateMetadataWithNlpResults(document, result);
        } else {
            log.info("⚠️ Document {} is manually classified, skipping NLP updates", document.getId());
        }
    }

    

    private String truncate(String value, int maxLength) {
        if (value == null)
            return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    

    private void updateMetadataWithNlpResults(Document document, NlpAnalysisResultDTO result) {
        try {
            // Find existing metadata for this document
            Metadata metadata = metadataRepository.findByDocument_Id(document.getId())
                    .orElse(null);

            if (metadata == null) {
                log.warn("⚠️ No metadata record found for document ID: {}. Creating new one.", document.getId());
                metadata = new Metadata();
                metadata.setDocument(document);
            }

            // Set summary from NLP analysis (TEXT field - no limit but truncate to 10000
            // for safety)
            if (result.getSummary() != null && !result.getSummary().isEmpty()) {
                metadata.setSummary(truncate(result.getSummary(), 10000));
            }

            // Set keywords from NLP analysis (TEXT field)
            if (result.getMatchedKeywords() != null && result.getMatchedKeywords().length > 0) {
                metadata.setKeywords(truncate(String.join(", ", result.getMatchedKeywords()), 10000));
            }

            // Set subject based on document type and department (VARCHAR 500)
            StringBuilder subject = new StringBuilder();
            if (result.getDocumentType() != null) {
                subject.append(result.getDocumentType().toString().replace("_", " "));
            }
            if (result.getDepartmentName() != null) {
                if (subject.length() > 0)
                    subject.append(" - ");
                subject.append(result.getDepartmentName());
            }
            if (subject.length() > 0) {
                metadata.setSubject(truncate(subject.toString(), 500));
            }

            // Set deadline if detected
            if (result.getDeadlineFound() && result.getDeadlineDate() != null) {
                try {
                    // Try to parse the normalized date
                    LocalDate deadline = LocalDate.parse(result.getDeadlineDate(),
                            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    metadata.setDeadline(deadline);
                } catch (Exception e) {
                    log.debug("Could not parse deadline date: {}", result.getDeadlineDate());
                }
            }

            // Set author/approver from text if available
            extractPeopleFromText(document.getExtractedText(), metadata);

            // Set reference numbers if available in text
            extractReferenceNumbers(document.getExtractedText(), metadata);

            // Set document date if available
            extractDocumentDate(document.getExtractedText(), metadata);

            // Save metadata
            metadataRepository.save(metadata);
            log.info("📋 Metadata updated for document ID: {} with NLP results", document.getId());

        } catch (Exception e) {
            log.error("❌ Failed to update metadata for document ID: {}: {}", document.getId(), e.getMessage());
        }
    }

    

    private void extractPeopleFromText(String text, Metadata metadata) {
        if (text == null || text.isEmpty())
            return;

        // Patterns for extracting names
        Pattern authorPattern = Pattern.compile(
                "(?:From|Author|Prepared by|Submitted by|Written by)[:\\s]+([A-Z][a-zA-Z]+(?:\\s+[A-Z][a-zA-Z]+)*)",
                Pattern.CASE_INSENSITIVE);
        Pattern approverPattern = Pattern.compile(
                "(?:Approved by|Authorized by|Sanctioned by|Signed)[:\\s]+([A-Z][a-zA-Z]+(?:\\s+[A-Z][a-zA-Z]+)*)",
                Pattern.CASE_INSENSITIVE);
        Pattern recipientPattern = Pattern.compile(
                "(?:To|Attention|Attn|Dear)[:\\s]+([A-Z][a-zA-Z]+(?:\\s+[A-Z][a-zA-Z]+)*)",
                Pattern.CASE_INSENSITIVE);

        // Extract author (VARCHAR 100)
        Matcher authorMatcher = authorPattern.matcher(text);
        if (authorMatcher.find() && metadata.getAuthorName() == null) {
            metadata.setAuthorName(truncate(authorMatcher.group(1).trim(), 100));
        }

        // Extract approver (VARCHAR 100)
        Matcher approverMatcher = approverPattern.matcher(text);
        if (approverMatcher.find() && metadata.getApproverName() == null) {
            metadata.setApproverName(truncate(approverMatcher.group(1).trim(), 100));
        }

        // Extract recipient (VARCHAR 100)
        Matcher recipientMatcher = recipientPattern.matcher(text);
        if (recipientMatcher.find() && metadata.getRecipientName() == null) {
            metadata.setRecipientName(truncate(recipientMatcher.group(1).trim(), 100));
        }
    }

    

    private void extractReferenceNumbers(String text, Metadata metadata) {
        if (text == null || text.isEmpty())
            return;

        // Reference number pattern
        Pattern refPattern = Pattern.compile(
                "(?:Ref(?:erence)?(?:\\s+No)?|File No|Circular No|Invoice No|PO No)[.:\\s]+([A-Z0-9/-]+)",
                Pattern.CASE_INSENSITIVE);

        // Invoice number pattern
        Pattern invoicePattern = Pattern.compile(
                "(?:Invoice(?:\\s+No)?|Bill(?:\\s+No)?)[.:\\s]+([A-Z0-9/-]+)",
                Pattern.CASE_INSENSITIVE);

        // PO number pattern
        Pattern poPattern = Pattern.compile(
                "(?:PO(?:\\s+No)?|Purchase Order(?:\\s+No)?)[.:\\s]+([A-Z0-9/-]+)",
                Pattern.CASE_INSENSITIVE);

        // Extract reference number (VARCHAR 100)
        Matcher refMatcher = refPattern.matcher(text);
        if (refMatcher.find() && metadata.getReferenceNumber() == null) {
            metadata.setReferenceNumber(truncate(refMatcher.group(1).trim(), 100));
        }

        // Extract invoice number (VARCHAR 100)
        Matcher invoiceMatcher = invoicePattern.matcher(text);
        if (invoiceMatcher.find() && metadata.getInvoiceNumber() == null) {
            metadata.setInvoiceNumber(truncate(invoiceMatcher.group(1).trim(), 100));
        }

        // Extract PO number (VARCHAR 100)
        Matcher poMatcher = poPattern.matcher(text);
        if (poMatcher.find() && metadata.getPoNumber() == null) {
            metadata.setPoNumber(truncate(poMatcher.group(1).trim(), 100));
        }

        // Extract amount if invoice
        Pattern amountPattern = Pattern.compile(
                "(?:Total|Amount|Rs|₹|INR)[.:\\s]+([0-9,]+\\.?\\d*)",
                Pattern.CASE_INSENSITIVE);
        Matcher amountMatcher = amountPattern.matcher(text);
        if (amountMatcher.find() && metadata.getInvoiceAmount() == null) {
            try {
                String amountStr = amountMatcher.group(1).replace(",", "");
                metadata.setInvoiceAmount(Double.parseDouble(amountStr));
                metadata.setCurrency("INR");
            } catch (NumberFormatException e) {
                // Ignore invalid amounts
            }
        }
    }

    

    private void extractDocumentDate(String text, Metadata metadata) {
        if (text == null || text.isEmpty())
            return;

        // Date pattern: DD/MM/YYYY or DD-MM-YYYY
        Pattern datePattern = Pattern.compile(
                "(?:Date|Dated)[:\\s]+([0-3]?\\d[/-][0-1]?\\d[/-]\\d{2,4})",
                Pattern.CASE_INSENSITIVE);

        Matcher dateMatcher = datePattern.matcher(text);
        if (dateMatcher.find() && metadata.getDocumentDate() == null) {
            String dateStr = dateMatcher.group(1);
            try {
                // Try different date formats
                List<java.time.format.DateTimeFormatter> formatters = Arrays.asList(
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                        java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                        java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy"),
                        java.time.format.DateTimeFormatter.ofPattern("d-M-yyyy"),
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy"),
                        java.time.format.DateTimeFormatter.ofPattern("dd-MM-yy"));

                for (java.time.format.DateTimeFormatter formatter : formatters) {
                    try {
                        LocalDate date = LocalDate.parse(dateStr, formatter);
                        metadata.setDocumentDate(date);
                        break;
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception e) {
                log.debug("Could not parse document date: {}", dateStr);
            }
        }
    }

    

    private int countOccurrences(String text, String keyword) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(keyword, index)) != -1) {
            count++;
            index += keyword.length();
        }
        return count;
    }

    

    private String getDepartmentName(String code) {
        try {
            return departmentRepository.findByCode(code)
                    .map(Department::getName)
                    .orElse(code);
        } catch (Exception e) {
            return code;
        }
    }

    

    private Long getDepartmentId(String code) {
        try {
            return departmentRepository.findByCode(code)
                    .map(Department::getId)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    

    private double calculateOverallConfidence(
            DepartmentClassificationResult dept,
            DocumentTypeResult type,
            PriorityResult priority) {

        double deptWeight = 0.4;
        double typeWeight = 0.4;
        double priorityWeight = 0.2;

        double deptConfidence = dept.confidence();
        double typeConfidence = type.confidence();
        double priorityConfidence = priority.triggerKeywords().length > 0 ? 0.8 : 0.5;

        return Math.min(1.0,
                (deptConfidence * deptWeight) +
                        (typeConfidence * typeWeight) +
                        (priorityConfidence * priorityWeight));
    }

    

    private String determineDeadlineType(String matchedText) {
        String lower = matchedText.toLowerCase();
        if (lower.contains("within")) {
            return "RELATIVE";
        } else if (lower.contains("effective from")) {
            return "EFFECTIVE_FROM";
        } else {
            return "ABSOLUTE";
        }
    }

    

    private String tryNormalizeDate(String dateStr) {
        if (dateStr == null)
            return null;

        // Handle relative dates
        if (dateStr.matches("\\d+")) {
            // "within X days" - calculate future date
            try {
                int days = Integer.parseInt(dateStr);
                return LocalDate.now().plusDays(days)
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (NumberFormatException e) {
                return dateStr;
            }
        }

        // Return as-is for absolute dates
        return dateStr;
    }

    

    private int calculateDaysFromNow(String dateStr) {
        if (dateStr == null)
            return -1;

        // Handle relative dates like "7" (days)
        if (dateStr.matches("\\d+")) {
            return Integer.parseInt(dateStr);
        }

        // Try to parse absolute date
        try {
            // Try common Indian date formats
            List<DateTimeFormatter> formatters = Arrays.asList(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                    DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                    DateTimeFormatter.ofPattern("dd/MM/yy"),
                    DateTimeFormatter.ofPattern("dd-MM-yy"),
                    DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH));

            for (DateTimeFormatter formatter : formatters) {
                try {
                    LocalDate date = LocalDate.parse(dateStr, formatter);
                    return (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            log.debug("Could not parse date: {}", dateStr);
        }

        return -1;
    }

    // Inner Result Records (previously in interface)
    public record DepartmentClassificationResult(String departmentCode, String departmentName, Long departmentId,
            int matchScore, double confidence, String[] matchedKeywords) {
    }

    public record DocumentTypeResult(DocumentType documentType, int matchScore, double confidence,
            String[] matchedKeywords) {
    }

    public record PriorityResult(Priority priority, String reason, String[] triggerKeywords) {
    }

    public record DeadlineResult(boolean deadlineFound, String deadlineText, String normalizedDate, String deadlineType,
            int daysFromNow) {
    }

    private boolean hasClassificationConfidence(Document doc) {
        return doc.getClassificationConfidence() != null && doc.getClassificationConfidence() > 0;
    }
}
