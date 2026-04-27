package prescription_ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import prescription_ai.dto.MedicationInfo;
import prescription_ai.dto.ServiceResult;
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

    private static final ObjectMapper mapper = new ObjectMapper();

    // ── Main entry point ──────────────────────────────────────────
    public ServiceResult processFile(MultipartFile file) {
        try {
            byte[] bytes   = file.getBytes();
            String hash    = sha256(bytes);

            // Step 1: DB-first cache check
            Optional<Prescription> cached = repo.findByFileName(hash);
            if (cached.isPresent()) {
                List<MedicationInfo> meds  = parseMedsFromDB(cached.get().getExtractedText());
                List<String>         urls  = splitVideos(cached.get().getVideos());
                return ServiceResult.ok(meds, urls, true);
            }

            // Step 2: Call Gemini Vision API
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String mime   = resolveMime(file);
            List<MedicationInfo> meds = callGemini(base64, mime);

            if (meds.isEmpty()) {
                return ServiceResult.error("No medications found. Please upload a clearer prescription image.");
            }

            // Step 3: Build YouTube search URLs
            List<String> urls = buildYouTubeUrls(meds);

            // Step 4: Persist to DB
            Prescription p = new Prescription();
            p.setFileName(hash);
            p.setExtractedText(mapper.writeValueAsString(meds));   // store as JSON
            p.setVideos(String.join(",", urls));
            repo.save(p);

            return ServiceResult.ok(meds, urls, false);

        } catch (Exception e) {
            System.err.println("[Service] Error: " + e.getMessage());
            return ServiceResult.error("Image unclear or AI unavailable. Please try again.");
        }
    }

    // ── Gemini API call ───────────────────────────────────────────
    private List<MedicationInfo> callGemini(String base64, String mime) throws Exception {

        String prompt =
            "Analyze this prescription image carefully. " +
            "For each medication or drug name found, return a JSON array where each element has:\n" +
            "  \"name\": the medication name exactly as written\n" +
            "  \"use\": a short one-line description of what it is used for\n\n" +
            "Example output:\n" +
            "[{\"name\":\"Paracetamol\",\"use\":\"Pain relief and fever reduction\"}," +
            "{\"name\":\"Amoxicillin\",\"use\":\"Antibiotic for bacterial infections\"}]\n\n" +
            "Return ONLY the JSON array. No markdown, no explanation. If no medications found, return: []";

        String body = mapper.createObjectNode()
            .set("contents", mapper.createArrayNode().add(
                mapper.createObjectNode().set("parts", mapper.createArrayNode()
                    .add(mapper.createObjectNode().set("inlineData",
                         mapper.createObjectNode().put("mimeType", mime).put("data", base64)))
                    .add(mapper.createObjectNode().put("text", prompt))
                )))
            .toString();

        HttpClient  client  = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(GEMINI_URL + geminiApiKey))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(Duration.ofSeconds(60))
            .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("[Gemini] HTTP " + resp.statusCode());

        if (resp.statusCode() == 429) throw new RuntimeException("RATE_LIMIT");
        if (resp.statusCode() != 200) {
            System.err.println("[Gemini] Error: " + resp.body());
            throw new RuntimeException("Gemini API " + resp.statusCode());
        }

        return parseGeminiResponse(resp.body());
    }

    // ── Parse Gemini JSON response ────────────────────────────────
    private List<MedicationInfo> parseGeminiResponse(String json) throws Exception {
        JsonNode root = mapper.readTree(json);
        String text = root.path("candidates").get(0)
                          .path("content").path("parts").get(0)
                          .path("text").asText("[]").trim();

        System.out.println("[Gemini] Raw text: " + text);

        // Strip markdown code fences if present
        text = text.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();

        // Find JSON array boundaries
        int start = text.indexOf('[');
        int end   = text.lastIndexOf(']');
        if (start < 0 || end < 0) return List.of();
        text = text.substring(start, end + 1);

        List<MedicationInfo> list = mapper.readValue(text, new TypeReference<>(){});
        System.out.println("[Gemini] Medications: " + list.stream().map(MedicationInfo::getName).toList());
        return list;
    }

    // ── Parse medications stored in DB ────────────────────────────
    private List<MedicationInfo> parseMedsFromDB(String stored) {
        if (stored == null || stored.isBlank()) return List.of();
        try {
            // New format: JSON array
            return mapper.readValue(stored, new TypeReference<>(){});
        } catch (Exception e) {
            // Old format: comma-separated names
            List<MedicationInfo> list = new ArrayList<>();
            for (String name : stored.split(",")) {
                String n = name.trim();
                if (!n.isEmpty()) list.add(new MedicationInfo(n, ""));
            }
            return list;
        }
    }

    // ── Build YouTube search URLs ─────────────────────────────────
    private List<String> buildYouTubeUrls(List<MedicationInfo> meds) {
        List<String> urls = new ArrayList<>();
        for (MedicationInfo m : meds) {
            String q = URLEncoder.encode(m.getName() + " medication uses side effects guide",
                                         StandardCharsets.UTF_8);
            urls.add("https://www.youtube.com/results?search_query=" + q);
        }
        return urls;
    }

    private List<String> splitVideos(String videos) {
        if (videos == null || videos.isBlank()) return List.of();
        List<String> list = new ArrayList<>();
        for (String v : videos.split(",")) { String t = v.trim(); if (!t.isEmpty()) list.add(t); }
        return list;
    }

    // ── Helpers ───────────────────────────────────────────────────
    private String sha256(byte[] data) throws Exception {
        byte[] h = MessageDigest.getInstance("SHA-256").digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : h) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private String resolveMime(MultipartFile f) {
        String ct = f.getContentType();
        if (ct != null && ct.startsWith("image/")) return ct;
        String name = f.getOriginalFilename();
        if (name != null) {
            if (name.endsWith(".png"))  return "image/png";
            if (name.endsWith(".webp")) return "image/webp";
        }
        return "image/jpeg";
    }
}