package com.metrohub.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_acknowledgements",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_doc_user_ack",
           columnNames = {"document_id", "user_id"}
       ))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentAcknowledgement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "acknowledged_at", updatable = false)
    private LocalDateTime acknowledgedAt;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(length = 500)
    private String notes;

    public Long getDocumentId() { return document != null ? document.getId() : null; }
    public Long getUserId() { return user != null ? user.getId() : null; }
    public String getDocumentName() { return document != null ? document.getFileName() : null; }
    public String getUserName() { return user != null ? user.getName() : null; }
}
