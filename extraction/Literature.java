package shu.xai.literature_backened;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "literature", schema = "literature_db")  // 指定数据库和表
public class Literature {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;
    
    @Column(name = "title", nullable = false)
    private String title;
    
     @Column(name = "authors", columnDefinition = "TEXT")  // 加上这行
    private String authors;
    
    @Column(name = "abstract", columnDefinition = "TEXT")  // 加上这行
    private String abstractText;
    
    @Column(name = "full_text", columnDefinition = "LONGTEXT")
    private String fullText;
    
    @Column(name = "filename")
    private String filename;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    public String getFullText() {
        return fullText;
    }

    public void setFullText(String fullText) {
        this.fullText = fullText;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}