package prescription_ai.dto;

import java.util.List;

public class PrescriptionResponse {

    private boolean success;
    private boolean cached;
    private String  message;
    private List<MedicationInfo> medications;
    private List<String> videos;

    // ── Success ───────────────────────────────────────────────────
    public static PrescriptionResponse ok(
            List<MedicationInfo> medications, List<String> videos, boolean cached) {
        PrescriptionResponse r = new PrescriptionResponse();
        r.success     = true;
        r.cached      = cached;
        r.medications = medications;
        r.videos      = videos;
        r.message     = cached ? "Served from cache" : "Analyzed by Gemini AI";
        return r;
    }

    // ── Error ─────────────────────────────────────────────────────
    public static PrescriptionResponse error(String message) {
        PrescriptionResponse r = new PrescriptionResponse();
        r.success     = false;
        r.cached      = false;
        r.message     = message;
        r.medications = List.of();
        r.videos      = List.of();
        return r;
    }

    // ── Getters ───────────────────────────────────────────────────
    public boolean isSuccess()                   { return success; }
    public boolean isCached()                    { return cached; }
    public String  getMessage()                  { return message; }
    public List<MedicationInfo> getMedications() { return medications; }
    public List<String> getVideos()              { return videos; }
}
