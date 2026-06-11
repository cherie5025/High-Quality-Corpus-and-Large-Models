package shu.xai.extraction.model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "extraction_sentence")
public class ExtractionSentence {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "literature_id", nullable = false, length = 100)
    private String literatureId;
    
    @Column(name = "record_id", nullable = false, length = 100)
    private String recordId;
    
    @Column(name = "sentence_index", nullable = false)
    private Integer sentenceIndex;
    
    @Column(name = "sentence_text", columnDefinition = "TEXT")
    private String sentenceText;
    
    @Column(name = "entities", columnDefinition = "json")
    private String entities;
    
    @Column(name = "relations", columnDefinition = "json")
    private String relations;
    
    @Column(name = "need_correction")
    private Boolean needCorrection = false;
    
    @Column(name = "corrected_entities", columnDefinition = "json")
    private String correctedEntities;
    
    @Column(name = "corrected_relations", columnDefinition = "json")
    private String correctedRelations;
    
    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    
    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getLiteratureId() {
        return literatureId;
    }
    
    public void setLiteratureId(String literatureId) {
        this.literatureId = literatureId;
    }
    
    public String getRecordId() {
        return recordId;
    }
    
    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }
    
    public Integer getSentenceIndex() {
        return sentenceIndex;
    }
    
    public void setSentenceIndex(Integer sentenceIndex) {
        this.sentenceIndex = sentenceIndex;
    }
    
    public String getSentenceText() {
        return sentenceText;
    }
    
    public void setSentenceText(String sentenceText) {
        this.sentenceText = sentenceText;
    }
    
    public String getEntities() {
        return entities;
    }
    
    public void setEntities(String entities) {
        this.entities = entities;
    }
    
    public String getRelations() {
        return relations;
    }
    
    public void setRelations(String relations) {
        this.relations = relations;
    }
    
    public Boolean getNeedCorrection() {
        return needCorrection;
    }
    
    public void setNeedCorrection(Boolean needCorrection) {
        this.needCorrection = needCorrection;
    }
    
    public String getCorrectedEntities() {
        return correctedEntities;
    }
    
    public void setCorrectedEntities(String correctedEntities) {
        this.correctedEntities = correctedEntities;
    }
    
    public String getCorrectedRelations() {
        return correctedRelations;
    }
    
    public void setCorrectedRelations(String correctedRelations) {
        this.correctedRelations = correctedRelations;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public Date getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}