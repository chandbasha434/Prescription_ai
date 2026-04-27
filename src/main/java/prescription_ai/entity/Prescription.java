package prescription_ai.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "prescription", indexes = {
    @Index(name = "idx_prescription_file_name", columnList = "file_name")
})
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    @Column(length = 5000)
    private String extractedText;   // JSON array of MedicationInfo

    @Column(length = 5000)
    private String videos;          // comma-separated YouTube URLs

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Getters & Setters ─────────────────────────────────────────
    public Long   getId()           { return id; }

    public String getFileName()     { return fileName; }
    public void   setFileName(String v) { this.fileName = v; }

    public String getExtractedText()    { return extractedText; }
    public void   setExtractedText(String v) { this.extractedText = v; }

    public String getVideos()       { return videos; }
    public void   setVideos(String v) { this.videos = v; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}