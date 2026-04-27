package prescription_ai.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import prescription_ai.dto.*;
import prescription_ai.entity.Prescription;
import prescription_ai.repository.PrescriptionRepository;
import prescription_ai.service.PrescriptionService;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/prescription")
@CrossOrigin(origins = "*")
public class PrescriptionController {

    @Autowired private PrescriptionService    service;
    @Autowired private PrescriptionRepository repo;

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // ── Health ────────────────────────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<String> health() { return ResponseEntity.ok("OK"); }

    // ── Upload & analyze ──────────────────────────────────────────
    @PostMapping("/upload")
    public ResponseEntity<PrescriptionResponse> upload(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty())
            return ResponseEntity.badRequest()
                    .body(PrescriptionResponse.error("No file uploaded. Please select an image."));

        ServiceResult result = service.processFile(file);

        if (!result.isSuccess())
            return ResponseEntity.badRequest()
                    .body(PrescriptionResponse.error(result.getErrorMessage()));

        return ResponseEntity.ok(
                PrescriptionResponse.ok(result.getMedications(), result.getVideoUrls(), result.isCached()));
    }

    // ── History ───────────────────────────────────────────────────
    @GetMapping("/history")
    public ResponseEntity<List<HistoryItem>> history() {
        List<Prescription> all = repo.findAllByOrderByCreatedAtDesc();
        List<HistoryItem> items = new ArrayList<>();

        for (Prescription p : all) {
            List<MedicationInfo> meds = parseMeds(p.getExtractedText());
            int videoCount = p.getVideos() == null ? 0
                    : (int) java.util.Arrays.stream(p.getVideos().split(","))
                                            .filter(s -> !s.isBlank()).count();
            String date = p.getCreatedAt() != null ? p.getCreatedAt().format(FMT) : "Unknown";
            items.add(new HistoryItem(p.getId(), meds, videoCount, date));
        }
        return ResponseEntity.ok(items);
    }

    private List<MedicationInfo> parseMeds(String stored) {
        if (stored == null || stored.isBlank()) return List.of();
        try {
            return mapper.readValue(stored, new TypeReference<>(){});
        } catch (Exception e) {
            List<MedicationInfo> list = new ArrayList<>();
            for (String n : stored.split(",")) {
                String t = n.trim(); if (!t.isEmpty()) list.add(new MedicationInfo(t, ""));
            }
            return list;
        }
    }
}