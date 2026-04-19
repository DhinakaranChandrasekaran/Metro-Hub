package com.metrohub.services;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    // Path to Tesseract data files (language models)
    @Value("${metrohub.extraction.ocr.tessdata-path:}")
    private String tessdataPath;

    // Default OCR language (English + Hindi for Indian documents)
    @Value("${metrohub.extraction.ocr.language:eng}")
    private String defaultLanguage;

    // Whether OCR is enabled in configuration
    @Value("${metrohub.extraction.ocr.enabled:true}")
    private boolean ocrEnabled;

    // DPI for PDF to image conversion (higher = better quality but slower)
    private static final int PDF_RENDER_DPI = 300;

    // Maximum pages to OCR in a single PDF (prevents timeout)
    private static final int MAX_PDF_PAGES = 50;

    

        public String performOcr(byte[] fileBytes, String language) {
        
        log.info("🔍 Starting OCR processing...");
        
        if (!ocrEnabled) {
            log.warn("⚠️ OCR is disabled in configuration");
            return "";
        }

        try {
            // Create Tesseract instance
            Tesseract tesseract = createTesseractInstance(language);
            
            // Convert bytes to BufferedImage
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(fileBytes));
            
            if (image == null) {
                // Might be a PDF - try PDF processing
                log.info("📄 Could not read as image, trying PDF processing...");
                return performOcrOnPdf(fileBytes, language);
            }
            
            // Perform OCR
            String result = tesseract.doOCR(image);
            log.info("✅ OCR completed: {} characters extracted", 
                    result != null ? result.length() : 0);
            
            return result;
            
        } catch (IOException e) {
            log.error("❌ Error reading image for OCR: {}", e.getMessage());
            return "";
        } catch (TesseractException e) {
            log.error("❌ Tesseract OCR error: {}", e.getMessage());
            return "";
        } catch (Exception e) {
            log.error("❌ Unexpected OCR error: {}", e.getMessage());
            return "";
        }
    }

    

        public String performOcrOnPdf(byte[] pdfBytes, String language) {
        
        log.info("📄 Starting OCR on PDF document...");
        
        if (!ocrEnabled) {
            log.warn("⚠️ OCR is disabled in configuration");
            return "";
        }

        List<String> pageTexts = new ArrayList<>();
        
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            
            int totalPages = document.getNumberOfPages();
            int pagesToProcess = Math.min(totalPages, MAX_PDF_PAGES);
            
            log.info("📊 PDF has {} pages, will OCR {} pages", totalPages, pagesToProcess);
            
            PDFRenderer renderer = new PDFRenderer(document);
            Tesseract tesseract = createTesseractInstance(language);
            
            // Process each page
            for (int pageNum = 0; pageNum < pagesToProcess; pageNum++) {
                try {
                    log.debug("🔄 Processing page {} of {}", pageNum + 1, pagesToProcess);
                    
                    // Render page as image
                    BufferedImage pageImage = renderer.renderImageWithDPI(
                            pageNum, 
                            PDF_RENDER_DPI, 
                            ImageType.RGB
                    );
                    
                    // Perform OCR on the page image
                    String pageText = tesseract.doOCR(pageImage);
                    
                    if (pageText != null && !pageText.trim().isEmpty()) {
                        pageTexts.add("--- Page " + (pageNum + 1) + " ---\n" + pageText);
                    }
                    
                } catch (Exception e) {
                    log.warn("⚠️ Error processing page {}: {}", pageNum + 1, e.getMessage());
                }
            }
            
            if (totalPages > MAX_PDF_PAGES) {
                pageTexts.add("\n[Note: Only first " + MAX_PDF_PAGES + 
                        " of " + totalPages + " pages were processed]");
            }
            
            String combinedText = String.join("\n\n", pageTexts);
            log.info("✅ PDF OCR completed: {} pages, {} characters", 
                    pagesToProcess, combinedText.length());
            
            return combinedText;
            
        } catch (IOException e) {
            log.error("❌ Error loading PDF for OCR: {}", e.getMessage());
            return "";
        }
    }

    

        public boolean isOcrAvailable() {
        if (!ocrEnabled) {
            return false;
        }
        
        try {
            createTesseractInstance(null);
            // Try to create instance - if this works, Tesseract is available
            return true;
        } catch (Exception e) {
            log.warn("⚠️ Tesseract OCR is not available: {}", e.getMessage());
            return false;
        }
    }

    

        public String[] getSupportedLanguages() {
        return new String[] {
            "eng",      // English
            "hin",      // Hindi
            "eng+hin",  // English + Hindi combined
            "mar",      // Marathi
            "tam",      // Tamil
            "tel",      // Telugu
            "kan",      // Kannada
            "guj",      // Gujarati
            "ben"       // Bengali
        };
    }

    // ============================================================
    // PRIVATE HELPER METHODS
    // ============================================================

    

    private Tesseract createTesseractInstance(String language) {
        Tesseract tesseract = new Tesseract();
        
        // Set tessdata path if configured
        if (tessdataPath != null && !tessdataPath.isEmpty()) {
            File tessdataDir = new File(tessdataPath);
            if (tessdataDir.exists()) {
                tesseract.setDatapath(tessdataPath);
                log.debug("📁 Using tessdata path: {}", tessdataPath);
            }
        } else {
            // Try default paths based on OS
            String defaultPath = detectTessdataPath();
            if (defaultPath != null) {
                tesseract.setDatapath(defaultPath);
                log.debug("📁 Using detected tessdata path: {}", defaultPath);
            }
        }
        
        // Set language
        String ocrLanguage = (language != null && !language.isEmpty()) 
                ? language 
                : defaultLanguage;
        tesseract.setLanguage(ocrLanguage);
        log.debug("🌐 OCR language set to: {}", ocrLanguage);
        
        // Configure for better accuracy
        tesseract.setPageSegMode(1);  // Automatic page segmentation with OSD
        tesseract.setOcrEngineMode(1); // LSTM neural network mode (more accurate)
        
        return tesseract;
    }

    

    private String detectTessdataPath() {
        // Common tessdata locations
        String[] possiblePaths = {
            // Windows (Tesseract installer)
            "C:\\Program Files\\Tesseract-OCR\\tessdata",
            "C:\\Program Files (x86)\\Tesseract-OCR\\tessdata",
            // Linux (apt install)
            "/usr/share/tesseract-ocr/4.00/tessdata",
            "/usr/share/tesseract-ocr/5/tessdata",
            "/usr/share/tessdata",
            // macOS (Homebrew)
            "/usr/local/share/tessdata",
            "/opt/homebrew/share/tessdata"
        };
        
        for (String path : possiblePaths) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                // Check if eng.traineddata exists
                File engData = new File(dir, "eng.traineddata");
                if (engData.exists()) {
                    return path;
                }
            }
        }
        
        // Check TESSDATA_PREFIX environment variable
        String envPath = System.getenv("TESSDATA_PREFIX");
        if (envPath != null && new File(envPath).exists()) {
            return envPath;
        }
        
        log.warn("⚠️ Could not detect tessdata path. OCR may not work correctly.");
        return null;
    }
}
