package shu.xai.extraction.repository;

import shu.xai.extraction.model.CorrectionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CorrectionHistoryRepository extends JpaRepository<CorrectionHistory, String> {
    
    List<CorrectionHistory> findByRecordIdOrderByCreatedAtDesc(String recordId);
    
    List<CorrectionHistory> findBySentenceIdOrderByCreatedAtDesc(Long sentenceId);
    
    List<CorrectionHistory> findByRecordIdAndCorrectionType(String recordId, String correctionType);
}