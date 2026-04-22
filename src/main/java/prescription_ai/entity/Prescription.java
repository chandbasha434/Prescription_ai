package prescription_ai.entity;

import jakarta.persistence.*;

@Entity
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    @Column(length = 5000)
    private String extractedText;

    @Column(length = 5000)
    private String videos;

    public Long getId() { return id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

    public String getVideos() { return videos; }
    public void setVideos(String videos) { this.videos = videos; }
}