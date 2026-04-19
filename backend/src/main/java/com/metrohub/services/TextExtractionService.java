package com.metrohub.services;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class TextExtractionService {

    private static final Logger log = LoggerFactory.getLogger(TextExtractionService.class);

    // OCR Service for scanned documents
    private final OcrService ocrService;

    // Maximum text length to extract (25000 chars ≈ 5 pages, compressed from 50 pages)
    @Value("${metrohub.extraction.tika.max-text-length:25000}")
    private int maxTextLength;

    // Minimum text length to consider Tika extraction successful
    // If less, we try OCR
    private static final int MIN_TEXT_LENGTH_FOR_SUCCESS = 50;

    // Supported file extensions for text extraction
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "txt", "rtf", // Documents
            "jpg", "jpeg", "png", "tiff", "bmp" // Images (OCR)
    );

    // Extensions that need OCR
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "tiff", "bmp");

    public TextExtractionService(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    

    public ExtractionResult extractText(InputStream inputStream, String fileName) {

        log.info("📄 Starting text extraction for: {}", fileName);

        String extension = getFileExtension(fileName).toLowerCase();

        // ============================================
        // CHECK IF FILE TYPE IS SUPPORTED
        // ============================================
        if (!isSupported(extension)) {
            log.warn("⚠️ Unsupported file type: {}", extension);
            return new ExtractionResult(
                    null,
                    null,
                    false,
                    "Unsupported file type: " + extension);
        }

        try {
            // Read file into byte array (needed for potential retry with OCR)
            byte[] fileBytes = inputStream.readAllBytes();

            // ============================================
            // FOR IMAGES → USE OCR DIRECTLY
            // ============================================
            if (IMAGE_EXTENSIONS.contains(extension)) {
                log.info("🖼️ Image file detected, using OCR: {}", fileName);
                return extractWithOcr(fileBytes, extension);
            }

            // ============================================
            // FOR DOCUMENTS → TRY TIKA FIRST
            // ============================================
            log.info("📑 Document file detected, trying Tika: {}", fileName);
            String tikaText = extractWithTika(new ByteArrayInputStream(fileBytes), extension);

            // ============================================
            // CHECK IF TIKA EXTRACTION WAS SUCCESSFUL
            // ============================================
            if (tikaText != null && tikaText.trim().length() >= MIN_TEXT_LENGTH_FOR_SUCCESS) {
                // Tika worked well! Return the result
                String normalizedText = normalizeText(tikaText);
                log.info("✅ Tika extraction successful: {} characters", normalizedText.length());

                return new ExtractionResult(
                        normalizedText,
                        "TIKA",
                        true,
                        null);
            }

            // ============================================
            // TIKA FAILED → FALLBACK TO OCR
            // ============================================
            // This happens for scanned PDFs
            log.info("🔄 Tika extracted too little text, trying OCR fallback...");

            if (extension.equals("pdf")) {
                return extractWithOcr(fileBytes, extension);
            }

            // For other formats, return what Tika got
            return new ExtractionResult(
                    tikaText != null ? normalizeText(tikaText) : "",
                    "TIKA",
                    tikaText != null && !tikaText.isEmpty(),
                    tikaText == null || tikaText.isEmpty() ? "No text could be extracted" : null);

        } catch (IOException e) {
            log.error("❌ Error reading file: {}", e.getMessage());
            return new ExtractionResult(null, null, false, "Error reading file: " + e.getMessage());
        }
    }

    

    public String extractFromPdf(InputStream inputStream) {
        log.debug("📄 Extracting text from PDF using Tika");

        try {
            // Create a content handler with size limit
            BodyContentHandler handler = new BodyContentHandler(maxTextLength);
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            // Use PDF-specific parser
            PDFParser pdfParser = new PDFParser();
            pdfParser.parse(inputStream, handler, metadata, context);

            return handler.toString();

        } catch (IOException | SAXException | TikaException e) {
            log.error("❌ PDF extraction error: {}", e.getMessage());
            return null;
        }
    }

    

    public String extractFromWord(InputStream inputStream) {
        log.debug("📝 Extracting text from Word document using Tika");

        try {
            BodyContentHandler handler = new BodyContentHandler(maxTextLength);
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            // AutoDetectParser handles both DOC and DOCX
            AutoDetectParser parser = new AutoDetectParser();
            parser.parse(inputStream, handler, metadata, context);

            return handler.toString();

        } catch (IOException | SAXException | TikaException e) {
            log.error("❌ Word extraction error: {}", e.getMessage());
            return null;
        }
    }

    

    public String extractFromImage(InputStream inputStream, String language) {
        log.debug("🖼️ Extracting text from image using OCR");

        try {
            byte[] imageBytes = inputStream.readAllBytes();
            return ocrService.performOcr(imageBytes, language);
        } catch (IOException e) {
            log.error("❌ Image OCR error: {}", e.getMessage());
            return null;
        }
    }

    

    public boolean isSupported(String fileExtension) {
        if (fileExtension == null)
            return false;
        return SUPPORTED_EXTENSIONS.contains(fileExtension.toLowerCase());
    }

    // ============================================================
    // PRIVATE HELPER METHODS
    // ============================================================

    

    private String extractWithTika(InputStream inputStream, String extension) {
        try {
            BodyContentHandler handler = new BodyContentHandler(maxTextLength);
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            // Use AutoDetectParser for automatic format detection
            AutoDetectParser parser = new AutoDetectParser();
            parser.parse(inputStream, handler, metadata, context);

            String extractedText = handler.toString();
            log.debug("📊 Tika extracted {} characters",
                    extractedText != null ? extractedText.length() : 0);

            return extractedText;

        } catch (IOException | SAXException | TikaException e) {
            log.error("❌ Tika extraction failed: {}", e.getMessage());
            return null;
        }
    }

    

    private ExtractionResult extractWithOcr(byte[] fileBytes, String extension) {
        try {
            String ocrText = ocrService.performOcr(fileBytes, null);

            if (ocrText != null && !ocrText.trim().isEmpty()) {
                String normalizedText = normalizeText(ocrText);
                log.info("✅ OCR extraction successful: {} characters", normalizedText.length());

                return new ExtractionResult(
                        normalizedText,
                        "OCR",
                        true,
                        null);
            } else {
                log.warn("⚠️ OCR returned empty text");
                return new ExtractionResult(
                        "",
                        "OCR",
                        false,
                        "OCR could not extract any text from the document");
            }

        } catch (Exception e) {
            log.error("❌ OCR extraction failed: {}", e.getMessage());
            return new ExtractionResult(
                    null,
                    "OCR",
                    false,
                    "OCR extraction failed: " + e.getMessage());
        }
    }

    

    private String normalizeText(String text) {
        if (text == null)
            return "";

        String normalized = text
                // Replace multiple spaces with single space
                .replaceAll("[ \\t]+", " ")
                // Replace multiple newlines with double newline (paragraph break)
                .replaceAll("(\\r?\\n){3,}", "\n\n")
                // Remove leading/trailing whitespace
                .trim();

        // Limit text length
        if (maxTextLength > 0 && normalized.length() > maxTextLength) {
            normalized = normalized.substring(0, maxTextLength);
            log.info("📏 Text truncated to {} characters", maxTextLength);
        }

        // Extract key sentences for efficient reading
        normalized = extractKeyInformation(normalized);

        return normalized;
    }


    private String extractKeyInformation(String text) {
        if (text == null || text.isEmpty())
            return text;

        // Split into paragraphs (2+ newlines = section break)
        String[] sections = text.split("\n\n+");

        StringBuilder keyInfo = new StringBuilder();
        java.util.Set<String> allKeywords = new java.util.LinkedHashSet<>();
        int totalSentences = 0;
        int maxSentencesPerSection = 3; // Extract key sentences from each section
        int maxTotalSentences = 50; // Total sentences to preserve all content

        for (String section : sections) {
            if (section.trim().isEmpty()) continue;

            // Extract sentences from this section
            String[] sentences = section.split("(?<=[.!?])\\s+");
            int sectionSentences = 0;

            for (String sentence : sentences) {
                sentence = sentence.trim();
                if (sentence.length() < 15 || sentence.length() > 500) continue; // Keep reasonable sentences

                // Extract all keywords from sentence
                sentence = highlightKeywords(sentence, allKeywords);

                // Include sentence if: has important keywords OR is essential context
                if (hasImportantKeywords(sentence) || sectionSentences < 2) {
                    keyInfo.append("• ").append(sentence).append("\n");
                    sectionSentences++;
                    totalSentences++;

                    if (sectionSentences >= maxSentencesPerSection || totalSentences >= maxTotalSentences) {
                        break;
                    }
                }
            }

            if (totalSentences >= maxTotalSentences) break;
        }

        // Add ALL extracted keywords summary (not limited)
        if (!allKeywords.isEmpty()) {
            keyInfo.append("\n📌 Key Terms: ").append(String.join(", ", allKeywords));
        }

        return keyInfo.toString().trim();
    }

    private String highlightKeywords(String sentence, java.util.Set<String> foundKeywords) {
        String lower = sentence.toLowerCase();

        // Extended keyword list covering all departments & document types
        String[] keywords = {
            // Financial keywords
            "invoice", "payment", "deadline", "amount", "total", "budget", "expenditure",
            "rupees", "gst", "tax", "vendor", "salary", "purchase order", "billing",
            // Safety keywords
            "safety", "emergency", "fire", "accident", "hazard", "evacuation", "incident",
            "injury", "first aid", "ppe", "protective equipment", "risk assessment", "audit",
            // Maintenance keywords
            "maintenance", "repair", "service", "replacement", "spare parts", "equipment",
            "preventive", "breakdown", "fault", "defect", "inspection",
            // HR keywords
            "employee", "staff", "recruitment", "training", "leave", "attendance",
            "performance", "appraisal", "policy", "benefit", "payroll",
            // Operations keywords
            "procedure", "process", "workflow", "schedule", "timeline", "milestone",
            "requirement", "must", "should", "mandatory", "prohibited",
            // General keywords
            "urgent", "important", "critical", "action", "required", "approve", "submit",
            "complete", "contact", "date", "status", "note", "warning", "attention",
            "alert", "authorized", "signature", "department", "document", "email",
            "phone", "address", "reference", "project", "contract", "agreement"
        };

        for (String keyword : keywords) {
            if (lower.contains(keyword)) {
                foundKeywords.add(keyword.toUpperCase());
                // Highlight keywords
                sentence = sentence.replaceAll("(?i)" + Pattern.quote(keyword), "**" + keyword.toUpperCase() + "**");
            }
        }

        // Also extract custom keywords (words used 2+ times in document = likely important)
        String[] words = sentence.split("[\\s\\p{Punct}]+");
        for (String word : words) {
            if (word.length() > 5 && Character.isUpperCase(word.charAt(0))) {
                foundKeywords.add(word);
            }
        }

        return sentence;
    }


    private boolean hasImportantKeywords(String sentence) {
        String lower = sentence.toLowerCase();
        // Expanded list of important keywords
        String[] importantKeywords = {
            // Financial keywords
            "invoice", "payment", "deadline", "amount", "total", "budget", "expenditure",
            "rupees", "gst", "tax", "vendor", "salary", "purchase order", "billing",
            // Safety keywords
            "safety", "emergency", "fire", "accident", "hazard", "evacuation", "incident",
            "injury", "first aid", "ppe", "protective equipment", "risk assessment", "audit",
            // Maintenance keywords
            "maintenance", "repair", "service", "replacement", "spare parts", "equipment",
            "preventive", "breakdown", "fault", "defect", "inspection",
            // HR keywords
            "employee", "staff", "recruitment", "training", "leave", "attendance",
            "performance", "appraisal", "policy", "benefit", "payroll",
            // Operations keywords
            "procedure", "process", "workflow", "schedule", "timeline", "milestone",
            "requirement", "must", "should", "mandatory", "prohibited",
            // General keywords
            "urgent", "important", "critical", "action", "required", "approve", "submit",
            "complete", "contact", "date", "status", "note", "warning", "attention",
            "alert", "authorized", "signature", "department", "document", "email",
            "phone", "address", "reference", "project", "contract", "agreement"
        };

        for (String keyword : importantKeywords) {
            if (lower.contains(keyword)) return true;
        }
        return false;
    }

    

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    
    public record ExtractionResult(String extractedText, String method, boolean success, String errorMessage) {
    }
}
