package shu.xai.extraction.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "correction_history")
public class CorrectionHistory {
    
    @Id
    @Column(name = "id", length = 50)
    private String id;
    
    @Column(name = "record_id", nullable = false, length = 50)
    private String recordId;
    
    @Column(name = "sentence_id", nullable = false)
    private Long sentenceId;
    
    @Column(name = "correction_type", nullable = false, length = 20)
    private String correctionType;  // entity, relation
    
    @Column(name = "original_value", columnDefinition = "TEXT")
    private String originalValue;
    
    @Column(name = "corrected_value", columnDefinition = "TEXT")
    private String correctedValue;
    
    @Column(name = "accepted")
    private Boolean accepted = false;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (id == null) {
            id = "CORR_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getRecordId() {
        return recordId;
    }
    
    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }
    
    public Long getSentenceId() {
        return sentenceId;
    }
    
    public void setSentenceId(Long sentenceId) {
        this.sentenceId = sentenceId;
    }
    
    public String getCorrectionType() {
        return correctionType;
    }
    
    public void setCorrectionType(String correctionType) {
        this.correctionType = correctionType;
    }
    
    public String getOriginalValue() {
        return originalValue;
    }
    
    public void setOriginalValue(String originalValue) {
        this.originalValue = originalValue;
    }
    
    public String getCorrectedValue() {
        return correctedValue;
    }
    
    public void setCorrectedValue(String correctedValue) {
        this.correctedValue = correctedValue;
    }
    
    public Boolean getAccepted() {
        return accepted;
    }
    
    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}