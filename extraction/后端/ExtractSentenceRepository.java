package shu.xai.extraction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import shu.xai.extraction.model.ExtractionSentence;

import java.util.List;
import java.util.Map;

public interface ExtractSentenceRepository extends JpaRepository<ExtractionSentence, Long> {
    
    // 根据文献ID查询，按句子索引排序
    List<ExtractionSentence> findByLiteratureIdOrderBySentenceIndex(String literatureId);
    
    // 根据文献ID和句子索引查询
    List<ExtractionSentence> findByLiteratureIdAndSentenceIndex(String literatureId, Integer sentenceIndex);
    
    // 查询需要纠错的句子
    List<ExtractionSentence> findByLiteratureIdAndNeedCorrectionTrue(String literatureId);
    
    // 删除文献的所有抽取记录
    @Modifying
    @Transactional
    @Query("DELETE FROM ExtractionSentence e WHERE e.literatureId = :literatureId")
    void deleteByLiteratureId(@Param("literatureId") String literatureId);

    @Query("SELECT DISTINCT e.literatureId FROM ExtractionSentence e")
    List<String> findDistinctLiteratureIds();
    
    // 新增：关联查询获取文献标题
    @Query("SELECT " +
           "e.literatureId as literatureId, " +
           "e.recordId as recordId, " +
           "e.sentenceIndex as sentenceIndex, " +
           "e.sentenceText as sentenceText, " +
           "e.entities as entities, " +
           "e.relations as relations, " +
           "e.correctedEntities as correctedEntities, " +
           "e.correctedRelations as correctedRelations, " +
           "e.needCorrection as needCorrection, " +
           "e.createdAt as createdAt, " +
           "e.updatedAt as updatedAt, " +
           "l.title as literatureTitle " +
           "FROM ExtractionSentence e " +
           "LEFT JOIN Literature l ON e.literatureId = l.id " +
           "WHERE e.literatureId = :literatureId " +
           "ORDER BY e.sentenceIndex")
    List<Map<String, Object>> findExtractionsWithTitle(@Param("literatureId") String literatureId);
}