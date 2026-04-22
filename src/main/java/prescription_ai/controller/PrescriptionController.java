package prescription_ai.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import prescription_ai.service.PrescriptionService;

@RestController
@RequestMapping("/api")
public class PrescriptionController {

    @Autowired
    private PrescriptionService service;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadPrescription(@RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No file uploaded");
        }

        String result = service.processFile(file);

        if (result.equals("Image unclear. Please retake.")) {
            return ResponseEntity.badRequest().body(result);
        }

        return ResponseEntity.ok(result);
    }
}