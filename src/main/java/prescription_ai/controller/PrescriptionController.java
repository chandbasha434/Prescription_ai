package prescription_ai.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import prescription_ai.dto.PrescriptionResponse;
import prescription_ai.service.PrescriptionService;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/prescription")
@CrossOrigin(origins = "*")
public class PrescriptionController {

    @Autowired
    private PrescriptionService service;

    // ── Health check ─────────────────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    // ── Upload & analyze ─────────────────────────────────────────
    @PostMapping("/upload")
    public ResponseEntity<PrescriptionResponse> uploadPrescription(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(PrescriptionResponse.error("No file uploaded. Please select an image."));
        }

        String result = service.processFile(file);

        if (result.equals("Image unclear. Please retake.")) {
            return ResponseEntity.badRequest()
                    .body(PrescriptionResponse.error(result));
        }

        // Detect source: cache hit vs fresh AI call
        boolean cached    = result.startsWith("From DB:");
        String videosPart = result.replaceFirst("^(New Processed:|From DB:)\\s*", "").trim();

        // Parse comma-separated video URLs
        List<String> videoList = Arrays.stream(videosPart.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        // Derive medication names from YouTube URLs
        //   URL format: ...search_query=MedicationName+medication+uses+...
        List<String> medications = videoList.stream()
                .map(url -> {
                    try {
                        String q = url.substring(url.indexOf("search_query=") + 13);
                        // First token before the first '+' is the medication name
                        return java.net.URLDecoder.decode(q.split("\\+")[0], "UTF-8");
                    } catch (Exception e) {
                        return "";
                    }
                })
                .filter(s -> !s.isEmpty())
                .toList();

        return ResponseEntity.ok(PrescriptionResponse.ok(videoList, medications, cached));
    }
}