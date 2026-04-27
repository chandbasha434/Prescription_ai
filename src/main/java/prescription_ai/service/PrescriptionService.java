package prescription_ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import prescription_ai.entity.Prescription;
import prescription_ai.repository.PrescriptionRepository;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class PrescriptionService {

    @Autowired
    private PrescriptionRepository repo;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // ──────────────────────────────────────────────────────────────────
    //  Main entry point
    // ──────────────────────────────────────────────────────────────────
    public String processFile(MultipartFile file) {
        try {
            byte[] fileBytes = file.getBytes();
            String fileHash  = computeSha256(fileBytes);   // use content hash as cache key

            // ✅ Step 1: DB-first cache check
            Optional<Prescription> existing = repo.findByFileName(fileHash);
            if (existing.isPresent()) {
                return "From DB: " + existing.get().getVideos();
            }

            // ✅ Step 2: call Gemini Vision API
            String mimeType  = resolveMimeType(file);
            String base64Img = Base64.getEncoder().encodeToString(fileBytes);
            List<String> medications = callGeminiVision(base64Img, mimeType);

            if (medications.isEmpty()) {
                return "Image unclear. Please retake.";
            }

            // ✅ Step 3: build YouTube search URLs for each medication
            List<String> videoUrls = buildYouTubeUrls(medications);
            String videosStr = String.join(",", videoUrls);
            String medsStr   = String.join(", ", medications);

            // ✅ Step 4: persist to DB for next-time cache hit
            Prescription p = new Prescription();
            p.setFileName(fileHash);
            p.setExtractedText(medsStr);
            p.setVideos(videosStr);
            repo.save(p);

            return "New Processed: " + videosStr;

        } catch (Exception e) {
            System.err.println("[PrescriptionService] Error: " + e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("RATE_LIMIT")) {
                return "Image unclear. Please retake."; // mapped as error in controller
            }
            return "Image unclear. Please retake.";
        }
    }

    // ──────────────────────────────────────────────────────────────────
    //  Gemini Vision API call
    // ──────────────────────────────────────────────────────────────────
    private List<String> callGeminiVision(String base64Img, String mimeType) throws Exception {

        // Build request JSON using Jackson to safely handle the base64 payload
        ObjectMapper mapper = new ObjectMapper();
        String requestJson = mapper.createObjectNode()
                .set("contents", mapper.createArrayNode().add(
                        mapper.createObjectNode().set("parts", mapper.createArrayNode()
                                // Image part
                                .add(mapper.createObjectNode().set("inlineData",
                                        mapper.createObjectNode()
                                                .put("mimeType", mimeType)
                                                .put("data", base64Img)))
                                // Text prompt part
                                .add(mapper.createObjectNode().put("text",
                                        "You are a medical prescription analyzer. " +
                                        "Look at this prescription image carefully and extract ONLY the medication/drug names written on it. " +
                                        "Return each medication name on a separate line. " +
                                        "Do NOT include dosage, frequency, instructions, or any other text. " +
                                        "Just the medication names, one per line. " +
                                        "If you cannot read any medication names clearly, return the single word: UNCLEAR"
                                ))
                        )
                )).toString();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_URL + geminiApiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("[Gemini] HTTP status: " + response.statusCode());

        if (response.statusCode() == 429) {
            System.err.println("[Gemini] Rate limit hit: " + response.body());
            throw new RuntimeException("RATE_LIMIT");
        }
        if (response.statusCode() != 200) {
            System.err.println("[Gemini] Error response: " + response.body());
            throw new RuntimeException("Gemini API returned " + response.statusCode());
        }

        return parseGeminiMedications(response.body());
    }

    // ──────────────────────────────────────────────────────────────────
    //  Parse medication names from Gemini's JSON response
    // ──────────────────────────────────────────────────────────────────
    private List<String> parseGeminiMedications(String jsonResponse) throws Exception {
        JsonNode root  = objectMapper.readTree(jsonResponse);
        JsonNode parts = root.path("candidates").get(0)
                             .path("content").path("parts");

        StringBuilder rawText = new StringBuilder();
        for (JsonNode part : parts) {
            if (part.has("text")) {
                rawText.append(part.get("text").asText());
            }
        }

        String text = rawText.toString().trim();
        System.out.println("[Gemini] Extracted text: " + text);

        if (text.isEmpty() || text.equalsIgnoreCase("UNCLEAR")) {
            return List.of();
        }

        // Split by newline and clean each medication name
        List<String> medications = new ArrayList<>();
        for (String line : text.split("\\r?\\n")) {
            String med = line.trim()
                             .replaceAll("^[\\-\\*•\\d\\.]+\\s*", "")  // remove list prefixes like "1.", "-", "*"
                             .replaceAll("\\(.*?\\)", "")               // remove parenthetical notes
                             .trim();
            if (!med.isEmpty() && med.length() > 1) {
                medications.add(med);
            }
        }

        System.out.println("[Gemini] Medications found: " + medications);
        return medications;
    }

    // ──────────────────────────────────────────────────────────────────
    //  Build YouTube search URLs for each medication
    // ──────────────────────────────────────────────────────────────────
    private List<String> buildYouTubeUrls(List<String> medications) {
        List<String> urls = new ArrayList<>();
        for (String med : medications) {
            String query = URLEncoder.encode(med + " medication uses side effects guide",
                                             StandardCharsets.UTF_8);
            urls.add("https://www.youtube.com/results?search_query=" + query);
        }
        return urls;
    }

    // ──────────────────────────────────────────────────────────────────
    //  Helpers
    // ──────────────────────────────────────────────────────────────────
    private String computeSha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String resolveMimeType(MultipartFile file) {
        String ct = file.getContentType();
        if (ct != null && ct.startsWith("image/")) return ct;
        String name = file.getOriginalFilename();
        if (name != null) {
            if (name.endsWith(".png"))  return "image/png";
            if (name.endsWith(".webp")) return "image/webp";
            if (name.endsWith(".gif"))  return "image/gif";
        }
        return "image/jpeg"; // safe default
    }
}