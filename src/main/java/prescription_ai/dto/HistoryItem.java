package prescription_ai.dto;

import java.util.List;

public class HistoryItem {
    private Long   id;
    private List<MedicationInfo> medications;
    private int    videoCount;
    private String analyzedAt;

    public HistoryItem(Long id, List<MedicationInfo> medications, int videoCount, String analyzedAt) {
        this.id          = id;
        this.medications = medications;
        this.videoCount  = videoCount;
        this.analyzedAt  = analyzedAt;
    }

    public Long   getId()                        { return id; }
    public List<MedicationInfo> getMedications() { return medications; }
    public int    getVideoCount()                { return videoCount; }
    public String getAnalyzedAt()                { return analyzedAt; }
}
