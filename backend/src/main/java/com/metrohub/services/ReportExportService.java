package com.metrohub.services;

// OpenPDF imports for PDF generation
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

// Apache POI imports for Excel generation
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.metrohub.dto.ReportDTOs.*;
import com.metrohub.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportExportService {

    private static final Logger log = LoggerFactory.getLogger(ReportExportService.class);

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    @SuppressWarnings("unused") // Available for future date formatting
    private static final DateTimeFormatter DATE_ONLY_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    // ============================================
    // PDF EXPORT METHODS
    // ============================================

    

    public byte[] exportComplianceSummaryToPdf(ComplianceSummaryDTO summary) throws DocumentException, IOException {
        log.info("📄 Exporting compliance summary to PDF");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, baos);

        document.open();

        // Header
        addPdfHeader(document, "COMPLIANCE SUMMARY REPORT");
        addPdfSubHeader(document, summary.getReportPeriod());

        // Summary Section
        document.add(createSectionTitle("OVERVIEW"));
        
        PdfPTable overviewTable = new PdfPTable(2);
        overviewTable.setWidthPercentage(100);
        overviewTable.setSpacingBefore(10f);
        overviewTable.setSpacingAfter(10f);
        
        addTableRow(overviewTable, "Total Documents", String.valueOf(summary.getTotalDocuments()));
        addTableRow(overviewTable, "Total Acknowledgements", String.valueOf(summary.getTotalAcknowledgements()));
        addTableRow(overviewTable, "Pending Acknowledgements", String.valueOf(summary.getPendingAcknowledgements()));
        addTableRow(overviewTable, "Acknowledgement Rate", summary.getAcknowledgementRate() + "%");
        
        document.add(overviewTable);

        // Violation Section
        document.add(createSectionTitle("VIOLATION SUMMARY"));
        
        PdfPTable violationTable = new PdfPTable(2);
        violationTable.setWidthPercentage(100);
        violationTable.setSpacingBefore(10f);
        violationTable.setSpacingAfter(10f);
        
        addTableRow(violationTable, "Total Violations", String.valueOf(summary.getTotalViolations()));
        addTableRow(violationTable, "Resolved Violations", String.valueOf(summary.getResolvedViolations()));
        addTableRow(violationTable, "Unresolved Violations", String.valueOf(summary.getUnresolvedViolations()));
        addTableRow(violationTable, "Critical Violations", String.valueOf(summary.getCriticalViolations()));
        addTableRow(violationTable, "High Violations", String.valueOf(summary.getHighViolations()));
        addTableRow(violationTable, "Medium Violations", String.valueOf(summary.getMediumViolations()));
        
        document.add(violationTable);

        // Compliance Section
        document.add(createSectionTitle("COMPLIANCE METRICS"));
        
        PdfPTable complianceTable = new PdfPTable(2);
        complianceTable.setWidthPercentage(100);
        complianceTable.setSpacingBefore(10f);
        complianceTable.setSpacingAfter(10f);
        
        addTableRow(complianceTable, "Overall Compliance", summary.getOverallCompliancePercentage() + "%");
        addTableRow(complianceTable, "Safety Compliance", summary.getSafetyCompliancePercentage() + "%");
        addTableRow(complianceTable, "Non-Safety Compliance", summary.getNonSafetyCompliancePercentage() + "%");
        
        document.add(complianceTable);

        // Footer
        addPdfFooter(document, summary.getGeneratedBy());

        document.close();
        return baos.toByteArray();
    }

    

    public byte[] exportDepartmentReportToPdf(List<DepartmentComplianceDTO> departments) 
            throws DocumentException, IOException {
        log.info("📄 Exporting department compliance report to PDF");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate()); // Landscape
        PdfWriter.getInstance(document, baos);

        document.open();

        addPdfHeader(document, "DEPARTMENT COMPLIANCE REPORT");
        addPdfSubHeader(document, "Generated on " + LocalDateTime.now().format(DATE_FORMAT));

        // Department Table
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setSpacingBefore(15f);
        table.setWidths(new float[]{2f, 1f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f});

        // Headers
        addTableHeader(table, "Department");
        addTableHeader(table, "Users");
        addTableHeader(table, "Docs Received");
        addTableHeader(table, "Ack Rate");
        addTableHeader(table, "Violations");
        addTableHeader(table, "Resolved");
        addTableHeader(table, "Compliance");
        addTableHeader(table, "Risk");

        // Data rows
        for (DepartmentComplianceDTO dept : departments) {
            table.addCell(createCell(dept.getDepartmentName()));
            table.addCell(createCell(String.valueOf(dept.getTotalUsers())));
            table.addCell(createCell(String.valueOf(dept.getDocumentsReceived())));
            table.addCell(createCell(dept.getAcknowledgementRate() + "%"));
            table.addCell(createCell(String.valueOf(dept.getTotalViolations())));
            table.addCell(createCell(String.valueOf(dept.getResolvedViolations())));
            table.addCell(createCell(dept.getComplianceScore() + "%"));
            table.addCell(createRiskCell(dept.getRiskLevel()));
        }

        document.add(table);
        addPdfFooter(document, SecurityUtils.getCurrentUserEmail());

        document.close();
        return baos.toByteArray();
    }

    

    public byte[] exportUserDefaulterReportToPdf(List<UserDefaulterDTO> users) 
            throws DocumentException, IOException {
        log.info("📄 Exporting user defaulter report to PDF");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, baos);

        document.open();

        addPdfHeader(document, "USER DEFAULTER REPORT");
        addPdfSubHeader(document, "Generated on " + LocalDateTime.now().format(DATE_FORMAT));

        PdfPTable table = new PdfPTable(9);
        table.setWidthPercentage(100);
        table.setSpacingBefore(15f);
        table.setWidths(new float[]{2f, 1.5f, 2f, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f, 1.5f});

        // Headers
        addTableHeader(table, "Name");
        addTableHeader(table, "Employee ID");
        addTableHeader(table, "Department");
        addTableHeader(table, "Assigned");
        addTableHeader(table, "Acknowledged");
        addTableHeader(table, "Late");
        addTableHeader(table, "Violations");
        addTableHeader(table, "Unresolved");
        addTableHeader(table, "Category");

        // Data rows
        for (UserDefaulterDTO user : users) {
            table.addCell(createCell(user.getUserName()));
            table.addCell(createCell(user.getEmployeeId()));
            table.addCell(createCell(user.getDepartmentName()));
            table.addCell(createCell(String.valueOf(user.getTotalDocumentsAssigned())));
            table.addCell(createCell(String.valueOf(user.getDocumentsAcknowledged())));
            table.addCell(createCell(String.valueOf(user.getLateAcknowledgements())));
            table.addCell(createCell(String.valueOf(user.getTotalViolations())));
            table.addCell(createCell(String.valueOf(user.getUnresolvedViolations())));
            table.addCell(createCategoryCell(user.getDefaulterCategory()));
        }

        document.add(table);
        addPdfFooter(document, SecurityUtils.getCurrentUserEmail());

        document.close();
        return baos.toByteArray();
    }

    

    public byte[] exportAuditTrailToPdf(DocumentAuditTrailDTO auditTrail) 
            throws DocumentException, IOException {
        log.info("📄 Exporting document audit trail to PDF - docId={}", auditTrail.getDocumentId());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, baos);

        document.open();

        addPdfHeader(document, "DOCUMENT AUDIT TRAIL");
        addPdfSubHeader(document, auditTrail.getFileName());

        // Document Info Section
        document.add(createSectionTitle("DOCUMENT INFORMATION"));
        
        PdfPTable docTable = new PdfPTable(2);
        docTable.setWidthPercentage(100);
        docTable.setSpacingBefore(10f);
        docTable.setSpacingAfter(10f);
        
        addTableRow(docTable, "Document ID", String.valueOf(auditTrail.getDocumentId()));
        addTableRow(docTable, "File Name", auditTrail.getFileName());
        addTableRow(docTable, "Document Type", auditTrail.getDocumentType());
        addTableRow(docTable, "Priority", auditTrail.getPriority());
        addTableRow(docTable, "Status", auditTrail.getStatus());
        addTableRow(docTable, "Uploaded By", auditTrail.getUploadedByName() + 
                " (" + auditTrail.getUploadedByEmployeeId() + ")");
        addTableRow(docTable, "Upload Date", auditTrail.getUploadDate().format(DATE_FORMAT));
        addTableRow(docTable, "Target Department", auditTrail.getTargetDepartmentName());
        
        document.add(docTable);

        // Acknowledgement Summary
        document.add(createSectionTitle("ACKNOWLEDGEMENT SUMMARY"));
        
        PdfPTable ackSummary = new PdfPTable(2);
        ackSummary.setWidthPercentage(100);
        ackSummary.setSpacingBefore(10f);
        ackSummary.setSpacingAfter(10f);
        
        addTableRow(ackSummary, "Total Users in Department", String.valueOf(auditTrail.getTotalUsersInDepartment()));
        addTableRow(ackSummary, "Users Acknowledged", String.valueOf(auditTrail.getAcknowledgedCount()));
        addTableRow(ackSummary, "Users Pending", String.valueOf(auditTrail.getPendingCount()));
        addTableRow(ackSummary, "Acknowledgement Rate", auditTrail.getAcknowledgementRate() + "%");
        
        document.add(ackSummary);

        // Acknowledged Users Table
        if (auditTrail.getAcknowledgedUsers() != null && !auditTrail.getAcknowledgedUsers().isEmpty()) {
            document.add(createSectionTitle("ACKNOWLEDGED USERS"));
            
            PdfPTable ackTable = new PdfPTable(5);
            ackTable.setWidthPercentage(100);
            ackTable.setSpacingBefore(10f);
            
            addTableHeader(ackTable, "Name");
            addTableHeader(ackTable, "Employee ID");
            addTableHeader(ackTable, "Acknowledged At");
            addTableHeader(ackTable, "Days Taken");
            addTableHeader(ackTable, "Was Late");
            
            for (DocumentAuditTrailDTO.AcknowledgementRecord record : auditTrail.getAcknowledgedUsers()) {
                ackTable.addCell(createCell(record.getUserName()));
                ackTable.addCell(createCell(record.getEmployeeId()));
                ackTable.addCell(createCell(record.getAcknowledgedAt().format(DATE_FORMAT)));
                ackTable.addCell(createCell(String.valueOf(record.getDaysToAcknowledge())));
                ackTable.addCell(createCell(record.getWasLate() ? "Yes" : "No"));
            }
            
            document.add(ackTable);
        }

        // Violation Summary
        document.add(createSectionTitle("VIOLATION SUMMARY"));
        
        PdfPTable violSummary = new PdfPTable(2);
        violSummary.setWidthPercentage(100);
        violSummary.setSpacingBefore(10f);
        violSummary.setSpacingAfter(10f);
        
        addTableRow(violSummary, "Total Violations", String.valueOf(auditTrail.getTotalViolations()));
        addTableRow(violSummary, "Resolved", String.valueOf(auditTrail.getResolvedViolations()));
        addTableRow(violSummary, "Unresolved", String.valueOf(auditTrail.getUnresolvedViolations()));
        addTableRow(violSummary, "Reminders Sent", String.valueOf(auditTrail.getRemindersSent()));
        addTableRow(violSummary, "Dept Admin Escalations", String.valueOf(auditTrail.getDeptAdminEscalations()));
        addTableRow(violSummary, "Super Admin Escalations", String.valueOf(auditTrail.getSuperAdminEscalations()));
        
        document.add(violSummary);

        addPdfFooter(document, auditTrail.getReportGeneratedBy());

        document.close();
        return baos.toByteArray();
    }

    // ============================================
    // EXCEL EXPORT METHODS
    // ============================================

    

    public byte[] exportComplianceSummaryToExcel(ComplianceSummaryDTO summary) throws IOException {
        log.info("📊 Exporting compliance summary to Excel");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Compliance Summary");
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            int rowNum = 0;
            
            // Title
            org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("COMPLIANCE SUMMARY REPORT");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
            
            // Period
            org.apache.poi.ss.usermodel.Row periodRow = sheet.createRow(rowNum++);
            periodRow.createCell(0).setCellValue("Report Period: " + summary.getReportPeriod());
            
            rowNum++; // Empty row
            
            // Document Metrics
            org.apache.poi.ss.usermodel.Row docHeader = sheet.createRow(rowNum++);
            docHeader.createCell(0).setCellValue("DOCUMENT METRICS");
            docHeader.getCell(0).setCellStyle(headerStyle);
            
            createDataRow(sheet, rowNum++, "Total Documents", summary.getTotalDocuments(), dataStyle);
            createDataRow(sheet, rowNum++, "Safety Documents", summary.getSafetyDocuments(), dataStyle);
            createDataRow(sheet, rowNum++, "Circular Documents", summary.getCircularDocuments(), dataStyle);
            createDataRow(sheet, rowNum++, "Policy Documents", summary.getPolicyDocuments(), dataStyle);
            
            rowNum++;
            
            // Acknowledgement Metrics
            org.apache.poi.ss.usermodel.Row ackHeader = sheet.createRow(rowNum++);
            ackHeader.createCell(0).setCellValue("ACKNOWLEDGEMENT METRICS");
            ackHeader.getCell(0).setCellStyle(headerStyle);
            
            createDataRow(sheet, rowNum++, "Total Acknowledgements", summary.getTotalAcknowledgements(), dataStyle);
            createDataRow(sheet, rowNum++, "Pending Acknowledgements", summary.getPendingAcknowledgements(), dataStyle);
            createDataRow(sheet, rowNum++, "Late Acknowledgements", summary.getLateAcknowledgements(), dataStyle);
            createDataRow(sheet, rowNum++, "Acknowledgement Rate (%)", summary.getAcknowledgementRate(), dataStyle);
            
            rowNum++;
            
            // Violation Metrics
            org.apache.poi.ss.usermodel.Row violHeader = sheet.createRow(rowNum++);
            violHeader.createCell(0).setCellValue("VIOLATION METRICS");
            violHeader.getCell(0).setCellStyle(headerStyle);
            
            createDataRow(sheet, rowNum++, "Total Violations", summary.getTotalViolations(), dataStyle);
            createDataRow(sheet, rowNum++, "Resolved Violations", summary.getResolvedViolations(), dataStyle);
            createDataRow(sheet, rowNum++, "Unresolved Violations", summary.getUnresolvedViolations(), dataStyle);
            createDataRow(sheet, rowNum++, "Critical Violations", summary.getCriticalViolations(), dataStyle);
            createDataRow(sheet, rowNum++, "High Violations", summary.getHighViolations(), dataStyle);
            createDataRow(sheet, rowNum++, "Medium Violations", summary.getMediumViolations(), dataStyle);
            
            rowNum++;
            
            // Compliance Metrics
            org.apache.poi.ss.usermodel.Row compHeader = sheet.createRow(rowNum++);
            compHeader.createCell(0).setCellValue("COMPLIANCE METRICS");
            compHeader.getCell(0).setCellStyle(headerStyle);
            
            createDataRow(sheet, rowNum++, "Overall Compliance (%)", summary.getOverallCompliancePercentage(), dataStyle);
            createDataRow(sheet, rowNum++, "Safety Compliance (%)", summary.getSafetyCompliancePercentage(), dataStyle);
            createDataRow(sheet, rowNum++, "Non-Safety Compliance (%)", summary.getNonSafetyCompliancePercentage(), dataStyle);
            
            // Auto-size columns
            for (int i = 0; i < 4; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    

    public byte[] exportDepartmentReportToExcel(List<DepartmentComplianceDTO> departments) throws IOException {
        log.info("📊 Exporting department report to Excel");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Department Compliance");
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            @SuppressWarnings("unused")
            CellStyle dataStyle = createDataStyle(workbook); // Available for future use
            CellStyle riskHighStyle = createRiskStyle(workbook, IndexedColors.RED);
            CellStyle riskMediumStyle = createRiskStyle(workbook, IndexedColors.ORANGE);
            CellStyle riskLowStyle = createRiskStyle(workbook, IndexedColors.GREEN);
            
            int rowNum = 0;
            
            // Title
            org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("DEPARTMENT COMPLIANCE REPORT");
            titleCell.setCellStyle(titleStyle);
            
            rowNum++; // Empty row
            
            // Headers
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {"Rank", "Department", "Code", "Total Users", "Docs Received", 
                    "Ack Rate (%)", "Total Violations", "Resolved", "Unresolved", 
                    "Critical", "Compliance Score (%)", "Risk Level"};
            
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Data rows
            for (DepartmentComplianceDTO dept : departments) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(dept.getRiskRank() != null ? dept.getRiskRank() : 0);
                row.createCell(1).setCellValue(dept.getDepartmentName());
                row.createCell(2).setCellValue(dept.getDepartmentCode());
                row.createCell(3).setCellValue(dept.getTotalUsers());
                row.createCell(4).setCellValue(dept.getDocumentsReceived());
                row.createCell(5).setCellValue(dept.getAcknowledgementRate());
                row.createCell(6).setCellValue(dept.getTotalViolations());
                row.createCell(7).setCellValue(dept.getResolvedViolations());
                row.createCell(8).setCellValue(dept.getUnresolvedViolations());
                row.createCell(9).setCellValue(dept.getCriticalViolations());
                row.createCell(10).setCellValue(dept.getComplianceScore());
                
                org.apache.poi.ss.usermodel.Cell riskCell = row.createCell(11);
                riskCell.setCellValue(dept.getRiskLevel());
                switch (dept.getRiskLevel()) {
                    case "CRITICAL":
                    case "HIGH":
                        riskCell.setCellStyle(riskHighStyle);
                        break;
                    case "MEDIUM":
                        riskCell.setCellStyle(riskMediumStyle);
                        break;
                    default:
                        riskCell.setCellStyle(riskLowStyle);
                }
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    

    public byte[] exportUserDefaulterReportToExcel(List<UserDefaulterDTO> users) throws IOException {
        log.info("📊 Exporting user defaulter report to Excel");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("User Defaulters");
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            
            int rowNum = 0;
            
            // Title
            org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(rowNum++);
            titleRow.createCell(0).setCellValue("USER DEFAULTER REPORT");
            titleRow.getCell(0).setCellStyle(titleStyle);
            
            rowNum++;
            
            // Headers
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {"Rank", "Name", "Employee ID", "Email", "Department", 
                    "Role", "Docs Assigned", "Acknowledged", "Pending", "Late Acks",
                    "Total Violations", "Resolved", "Unresolved", "Category"};
            
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Data rows
            for (UserDefaulterDTO user : users) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(user.getDefaulterRank() != null ? user.getDefaulterRank() : 0);
                row.createCell(1).setCellValue(user.getUserName());
                row.createCell(2).setCellValue(user.getEmployeeId());
                row.createCell(3).setCellValue(user.getUserEmail());
                row.createCell(4).setCellValue(user.getDepartmentName());
                row.createCell(5).setCellValue(user.getUserRole());
                row.createCell(6).setCellValue(user.getTotalDocumentsAssigned());
                row.createCell(7).setCellValue(user.getDocumentsAcknowledged());
                row.createCell(8).setCellValue(user.getDocumentsPending());
                row.createCell(9).setCellValue(user.getLateAcknowledgements());
                row.createCell(10).setCellValue(user.getTotalViolations());
                row.createCell(11).setCellValue(user.getResolvedViolations());
                row.createCell(12).setCellValue(user.getUnresolvedViolations());
                row.createCell(13).setCellValue(user.getDefaulterCategory());
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    

    public byte[] exportViolationTrendsToExcel(ViolationTrendDTO trends) throws IOException {
        log.info("📊 Exporting violation trends to Excel");

        try (Workbook workbook = new XSSFWorkbook()) {
            // Summary Sheet
            Sheet summarySheet = workbook.createSheet("Summary");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            int rowNum = 0;
            org.apache.poi.ss.usermodel.Row titleRow = summarySheet.createRow(rowNum++);
            titleRow.createCell(0).setCellValue("VIOLATION TREND ANALYSIS");
            titleRow.getCell(0).setCellStyle(titleStyle);
            
            rowNum++;
            
            createDataRow(summarySheet, rowNum++, "Total Violations (All Time)", trends.getTotalViolationsAllTime(), dataStyle);
            createDataRow(summarySheet, rowNum++, "Violations This Year", trends.getViolationsThisYear(), dataStyle);
            createDataRow(summarySheet, rowNum++, "Violations This Month", trends.getViolationsThisMonth(), dataStyle);
            createDataRow(summarySheet, rowNum++, "Violations Last Month", trends.getViolationsLastMonth(), dataStyle);
            createDataRow(summarySheet, rowNum++, "Month-over-Month Change (%)", trends.getMonthOverMonthChange(), dataStyle);
            
            rowNum++;
            
            createDataRow(summarySheet, rowNum++, "Safety Violations", trends.getSafetyViolations(), dataStyle);
            createDataRow(summarySheet, rowNum++, "Non-Safety Violations", trends.getNonSafetyViolations(), dataStyle);
            createDataRow(summarySheet, rowNum++, "Average Delay (Days)", trends.getAverageAcknowledgementDelayDays(), dataStyle);
            createDataRow(summarySheet, rowNum++, "Max Delay (Days)", trends.getMaxAcknowledgementDelayDays(), dataStyle);
            createDataRow(summarySheet, rowNum++, "Resolution Rate (%)", trends.getResolutionRate(), dataStyle);
            
            for (int i = 0; i < 4; i++) {
                summarySheet.autoSizeColumn(i);
            }
            
            // Monthly Trends Sheet
            if (trends.getMonthlyTrends() != null && !trends.getMonthlyTrends().isEmpty()) {
                Sheet trendsSheet = workbook.createSheet("Monthly Trends");
                rowNum = 0;
                
                org.apache.poi.ss.usermodel.Row header = trendsSheet.createRow(rowNum++);
                String[] trendHeaders = {"Month", "New Violations", "Resolved", "Cumulative", "Compliance Rate (%)"};
                for (int i = 0; i < trendHeaders.length; i++) {
                    org.apache.poi.ss.usermodel.Cell cell = header.createCell(i);
                    cell.setCellValue(trendHeaders[i]);
                    cell.setCellStyle(headerStyle);
                }
                
                for (ViolationTrendDTO.MonthlyTrend trend : trends.getMonthlyTrends()) {
                    org.apache.poi.ss.usermodel.Row row = trendsSheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(trend.getMonthName());
                    row.createCell(1).setCellValue(trend.getNewViolations());
                    row.createCell(2).setCellValue(trend.getResolvedViolations());
                    row.createCell(3).setCellValue(trend.getCumulativeViolations());
                    row.createCell(4).setCellValue(trend.getComplianceRate());
                }
                
                for (int i = 0; i < trendHeaders.length; i++) {
                    trendsSheet.autoSizeColumn(i);
                }
            }
            
            // Department Risk Sheet
            if (trends.getDepartmentRiskRanking() != null && !trends.getDepartmentRiskRanking().isEmpty()) {
                Sheet riskSheet = workbook.createSheet("Department Risk");
                rowNum = 0;
                
                org.apache.poi.ss.usermodel.Row header = riskSheet.createRow(rowNum++);
                String[] riskHeaders = {"Rank", "Department", "Code", "Total Violations", 
                        "Unresolved", "Compliance Rate (%)", "Risk Level"};
                for (int i = 0; i < riskHeaders.length; i++) {
                    org.apache.poi.ss.usermodel.Cell cell = header.createCell(i);
                    cell.setCellValue(riskHeaders[i]);
                    cell.setCellStyle(headerStyle);
                }
                
                for (ViolationTrendDTO.DepartmentRisk risk : trends.getDepartmentRiskRanking()) {
                    org.apache.poi.ss.usermodel.Row row = riskSheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(risk.getRank());
                    row.createCell(1).setCellValue(risk.getDepartmentName());
                    row.createCell(2).setCellValue(risk.getDepartmentCode());
                    row.createCell(3).setCellValue(risk.getTotalViolations());
                    row.createCell(4).setCellValue(risk.getUnresolvedViolations());
                    row.createCell(5).setCellValue(risk.getComplianceRate());
                    row.createCell(6).setCellValue(risk.getRiskLevel());
                }
                
                for (int i = 0; i < riskHeaders.length; i++) {
                    riskSheet.autoSizeColumn(i);
                }
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    // ============================================
    // PDF HELPER METHODS
    // ============================================

    private void addPdfHeader(Document document, String title) throws DocumentException {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
        Paragraph header = new Paragraph(title, headerFont);
        header.setAlignment(Element.ALIGN_CENTER);
        header.setSpacingAfter(5f);
        document.add(header);
    }

    private void addPdfSubHeader(Document document, String subtitle) throws DocumentException {
        Font subHeaderFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.GRAY);
        Paragraph subHeader = new Paragraph(subtitle, subHeaderFont);
        subHeader.setAlignment(Element.ALIGN_CENTER);
        subHeader.setSpacingAfter(20f);
        document.add(subHeader);
    }

    private Paragraph createSectionTitle(String title) {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(33, 37, 41));
        Paragraph section = new Paragraph(title, sectionFont);
        section.setSpacingBefore(15f);
        section.setSpacingAfter(5f);
        return section;
    }

    private void addTableHeader(PdfPTable table, String text) {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, headerFont));
        cell.setBackgroundColor(new Color(52, 58, 64));
        cell.setPadding(8f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addTableRow(PdfPTable table, String label, String value) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.DARK_GRAY);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
        
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setPadding(6f);
        labelCell.setBackgroundColor(new Color(248, 249, 250));
        table.addCell(labelCell);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setPadding(6f);
        table.addCell(valueCell);
    }

    private PdfPCell createCell(String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "-", font));
        cell.setPadding(5f);
        return cell;
    }

    private PdfPCell createRiskCell(String riskLevel) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(riskLevel, font));
        cell.setPadding(5f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        
        switch (riskLevel) {
            case "CRITICAL":
                cell.setBackgroundColor(new Color(220, 53, 69));
                break;
            case "HIGH":
                cell.setBackgroundColor(new Color(253, 126, 20));
                break;
            case "MEDIUM":
                cell.setBackgroundColor(new Color(255, 193, 7));
                font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.DARK_GRAY);
                cell = new PdfPCell(new Phrase(riskLevel, font));
                cell.setPadding(5f);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(new Color(255, 193, 7));
                break;
            default:
                cell.setBackgroundColor(new Color(40, 167, 69));
        }
        
        return cell;
    }

    private PdfPCell createCategoryCell(String category) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        PdfPCell cell = new PdfPCell(new Phrase(category, font));
        cell.setPadding(5f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        
        switch (category) {
            case "CHRONIC":
                cell.setBackgroundColor(new Color(220, 53, 69));
                font.setColor(Color.WHITE);
                break;
            case "REPEAT":
                cell.setBackgroundColor(new Color(253, 126, 20));
                font.setColor(Color.WHITE);
                break;
            case "OCCASIONAL":
                cell.setBackgroundColor(new Color(255, 193, 7));
                break;
            default:
                cell.setBackgroundColor(new Color(40, 167, 69));
                font.setColor(Color.WHITE);
        }
        
        return cell;
    }

    private void addPdfFooter(Document document, String generatedBy) throws DocumentException {
        document.add(Chunk.NEWLINE);
        
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
        Paragraph footer = new Paragraph();
        footer.add(new Chunk("Generated on: " + LocalDateTime.now().format(DATE_FORMAT), footerFont));
        footer.add(Chunk.NEWLINE);
        footer.add(new Chunk("Generated by: " + (generatedBy != null ? generatedBy : "System"), footerFont));
        footer.add(Chunk.NEWLINE);
        footer.add(new Chunk("MetroHub Document Management System - CONFIDENTIAL", footerFont));
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(30f);
        
        document.add(footer);
    }

    // ============================================
    // EXCEL HELPER METHODS
    // ============================================

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createRiskStyle(Workbook workbook, IndexedColors color) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private void createDataRow(Sheet sheet, int rowNum, String label, Object value, CellStyle style) {
        org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum);
        org.apache.poi.ss.usermodel.Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        
        org.apache.poi.ss.usermodel.Cell valueCell = row.createCell(1);
        if (value instanceof Number) {
            valueCell.setCellValue(((Number) value).doubleValue());
        } else {
            valueCell.setCellValue(String.valueOf(value));
        }
        valueCell.setCellStyle(style);
    }
}
