package prescription_ai.dto;

import java.util.List;

public class ServiceResult {

    private boolean success;
    private boolean cached;
    private String  errorMessage;
    private List<MedicationInfo> medications;
    private List<String> videoUrls;

    // ── Static factories ──────────────────────────────────────────
    public static ServiceResult ok(List<MedicationInfo> medications, List<String> videoUrls, boolean cached) {
        ServiceResult r = new ServiceResult();
        r.success     = true;
        r.cached      = cached;
        r.medications = medications;
        r.videoUrls   = videoUrls;
        return r;
    }

    public static ServiceResult error(String message) {
        ServiceResult r = new ServiceResult();
        r.success      = false;
        r.errorMessage = message;
        r.medications  = List.of();
        r.videoUrls    = List.of();
        return r;
    }

    // ── Getters ───────────────────────────────────────────────────
    public boolean isSuccess()                  { return success; }
    public boolean isCached()                   { return cached; }
    public String  getErrorMessage()            { return errorMessage; }
    public List<MedicationInfo> getMedications(){ return medications; }
    public List<String> getVideoUrls()          { return videoUrls; }
}
