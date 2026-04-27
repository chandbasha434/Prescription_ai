package prescription_ai.dto;

import java.util.List;

public class PrescriptionResponse {

    private boolean success;
    private boolean cached;
    private String message;
    private List<String> videos;
    private List<String> medications;  // medication names detected by Gemini

    // ── Success response ──────────────────────────────────────────
    public static PrescriptionResponse ok(List<String> videos, List<String> medications, boolean cached) {
        PrescriptionResponse r = new PrescriptionResponse();
        r.success     = true;
        r.cached      = cached;
        r.videos      = videos;
        r.medications = medications;
        r.message     = cached ? "Served from cache" : "Analyzed by Gemini AI";
        return r;
    }

    // ── Error response ────────────────────────────────────────────
    public static PrescriptionResponse error(String message) {
        PrescriptionResponse r = new PrescriptionResponse();
        r.success     = false;
        r.cached      = false;
        r.message     = message;
        r.videos      = List.of();
        r.medications = List.of();
        return r;
    }

    // ── Getters ───────────────────────────────────────────────────
    public boolean isSuccess()          { return success; }
    public boolean isCached()           { return cached; }
    public String getMessage()          { return message; }
    public List<String> getVideos()     { return videos; }
    public List<String> getMedications(){ return medications; }
}
