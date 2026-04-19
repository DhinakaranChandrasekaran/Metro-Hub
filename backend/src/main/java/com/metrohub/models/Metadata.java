package com.metrohub.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_metadata")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Metadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false, unique = true)
    private Document document;

    @Column(name = "equipment_id", length = 100)
    private String equipmentId;

    @Column(name = "equipment_name", length = 200)
    private String equipmentName;

    @Column(name = "equipment_location", length = 200)
    private String equipmentLocation;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "vendor_name", length = 200)
    private String vendorName;

    @Column(name = "vendor_code", length = 50)
    private String vendorCode;

    @Column(name = "po_number", length = 100)
    private String poNumber;

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    @Column(name = "invoice_amount")
    private Double invoiceAmount;

    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "document_date")
    private LocalDate documentDate;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "file_number", length = 100)
    private String fileNumber;

    @Column(name = "circular_number", length = 100)
    private String circularNumber;

    @Column(name = "case_number", length = 100)
    private String caseNumber;

    @Column(name = "author_name", length = 100)
    private String authorName;

    @Column(name = "approver_name", length = 100)
    private String approverName;

    @Column(name = "recipient_name", length = 100)
    private String recipientName;

    @Column(name = "subject", length = 500)
    private String subject;

    @Lob
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Lob
    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords;

    @Lob
    @Column(name = "additional_info", columnDefinition = "TEXT")
    private String additionalInfo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
