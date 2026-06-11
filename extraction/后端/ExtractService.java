package shu.xai.extraction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shu.xai.extraction.model.ExtractionSentence;
import shu.xai.extraction.repository.ExtractSentenceRepository;
import shu.xai.literature_backened.Literature;
import shu.xai.literature_backened.LiteratureRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExtractService {
    
    @Autowired
    private ExtractSentenceRepository sentenceRepository;
    
    @Autowired
    private LiteratureRepository literatureRepository;
    
    private ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 保存抽取结果
     */
    @Transactional
    public Map<String, Object> saveExtraction(String literatureId, String dataset, 
                                               List<Map<String, Object>> sentences) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String recordId = literatureId + "_" + System.currentTimeMillis();
            sentenceRepository.deleteByLiteratureId(literatureId);
            
            Date now = new Date();
            List<ExtractionSentence> entities = new ArrayList<>();
            
            for (Map<String, Object> sent : sentences) {
                ExtractionSentence es = new ExtractionSentence();
                es.setLiteratureId(literatureId);
                es.setRecordId(recordId);
                es.setSentenceIndex((Integer) sent.get("sentence_index"));
                es.setSentenceText((String) sent.get("sentence_text"));
                es.setEntities((String) sent.get("entities"));
                es.setRelations((String) sent.get("relations"));
                es.setNeedCorrection(false);
                es.setCreatedAt(now);
                es.setUpdatedAt(now);
                
                entities.add(es);
            }
            
            sentenceRepository.saveAll(entities);
            
            result.put("count", entities.size());
            result.put("recordId", recordId);
            result.put("literatureId", literatureId);
            result.put("success", true);
            return result;
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("保存失败: " + e.getMessage());
        }
    }
    
    /**
     * 保存纠错结果
     */
    @Transactional
    public Map<String, Object> saveCorrection(String literatureId, List<Map<String, Object>> corrections) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Date now = new Date();
            
            for (Map<String, Object> correction : corrections) {
                Integer sentenceIndex = (Integer) correction.get("sentence_index");
                List<Map<String, Object>> correctedEntities = (List<Map<String, Object>>) correction.get("entities");
                List<Map<String, Object>> correctedRelations = (List<Map<String, Object>>) correction.get("relations");
                
                List<ExtractionSentence> sentences = sentenceRepository
                    .findByLiteratureIdAndSentenceIndex(literatureId, sentenceIndex);
                
                for (ExtractionSentence sentence : sentences) {
                    sentence.setNeedCorrection(true);
                    sentence.setCorrectedEntities(objectMapper.writeValueAsString(correctedEntities));
                    sentence.setCorrectedRelations(objectMapper.writeValueAsString(correctedRelations));
                    sentence.setUpdatedAt(now);
                }
            }
            
            result.put("success", true);
            result.put("literatureId", literatureId);
            result.put("count", corrections.size());
            return result;
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("保存纠错结果失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取文献的所有抽取记录
     */
    public List<Map<String, Object>> getLiteratureExtractions(String literatureId) {
        List<ExtractionSentence> sentences = sentenceRepository.findByLiteratureIdOrderBySentenceIndex(literatureId);
        
        // 从数据库获取文献标题
        String literatureTitle = getLiteratureTitleById(literatureId);
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (ExtractionSentence s : sentences) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("literatureId", s.getLiteratureId());
            map.put("literatureTitle", literatureTitle);
            map.put("recordId", s.getRecordId());
            map.put("sentenceIndex", s.getSentenceIndex());
            map.put("sentenceText", s.getSentenceText());
            
            // 解析JSON字符串
            try {
                if (s.getEntities() != null) {
                    map.put("entities", objectMapper.readValue(s.getEntities(), List.class));
                } else {
                    map.put("entities", new ArrayList<>());
                }
                
                if (s.getRelations() != null) {
                    map.put("relations", objectMapper.readValue(s.getRelations(), List.class));
                } else {
                    map.put("relations", new ArrayList<>());
                }
                
                if (s.getCorrectedEntities() != null) {
                    map.put("correctedEntities", objectMapper.readValue(s.getCorrectedEntities(), List.class));
                }
                
                if (s.getCorrectedRelations() != null) {
                    map.put("correctedRelations", objectMapper.readValue(s.getCorrectedRelations(), List.class));
                }
            } catch (Exception e) {
                map.put("entities", s.getEntities());
                map.put("relations", s.getRelations());
            }
            
            map.put("needCorrection", s.getNeedCorrection());
            map.put("createdAt", s.getCreatedAt());
            map.put("updatedAt", s.getUpdatedAt());
            
            result.add(map);
        }
        
        return result;
    }
    
    /**
     * 根据文献ID获取文献标题
     */
    private String getLiteratureTitleById(String literatureId) {
        if (literatureId == null) {
            return "未知文献";
        }
        
        try {
            Optional<Literature> lit = literatureRepository.findById(literatureId);
            if (lit.isPresent()) {
                String title = lit.get().getTitle();
                if (title != null && !title.isEmpty()) {
                    System.out.println("找到文献标题: " + literatureId + " -> " + title);
                    return title;
                }
            }
        } catch (Exception e) {
            System.out.println("查询文献标题失败: " + literatureId + ", 错误: " + e.getMessage());
        }
        
        System.out.println("未找到文献标题，使用默认: " + literatureId);
        return "文献" + literatureId;
    }
    
    public List<String> getDistinctLiteratureIds() {
        return sentenceRepository.findDistinctLiteratureIds();
    }
}