package prescription_ai.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import prescription_ai.entity.Prescription;
import prescription_ai.repository.PrescriptionRepository;

import java.util.Optional;

@Service
public class PrescriptionService {

    @Autowired
    private PrescriptionRepository repo;

    public String processFile(MultipartFile file) {

        try {
            // ✅ Step 1: get file name
            String fileName = file.getOriginalFilename();

            // ✅ Step 2: DB check first (IMPORTANT)
            Optional<Prescription> existing = repo.findByFileName(fileName);

            if (existing.isPresent()) {
                return "From DB: " + existing.get().getVideos();
            }

            // ✅ Step 3: simulate AI failure
            if (fileName != null && fileName.contains("error")) {
                throw new RuntimeException("AI failed to process image");
            }

            // ✅ Step 4: mock AI result
            String extractedText = "knee pain detected";
            String videos = "https://youtube.com/knee-exercise";

            // ✅ Step 5: save to DB
            Prescription p = new Prescription();
            p.setFileName(fileName);
            p.setExtractedText(extractedText);
            p.setVideos(videos);

            repo.save(p);

            return "New Processed: " + videos;

        } catch (Exception e) {
            // ✅ Step 6: graceful error handling
            return "Image unclear. Please retake.";
        }
    }
}