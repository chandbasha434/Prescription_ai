package prescription_ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import prescription_ai.entity.Prescription;
import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    Optional<Prescription> findByFileName(String fileName);
    List<Prescription> findAllByOrderByCreatedAtDesc();
}