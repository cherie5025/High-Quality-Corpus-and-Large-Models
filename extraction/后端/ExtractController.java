package shu.xai.extraction.controller;

import shu.xai.extraction.service.ExtractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/extract")
@CrossOrigin(origins = "*")
public class ExtractController {
    
    @Autowired
    private ExtractService extractService;
    
    /**
     * 保存抽取结果
     */
    @PostMapping("/save")
    public Map<String, Object> save(@RequestBody Map<String, Object> request) {
        try {
            String literatureId = (String) request.get("literatureId");
            String dataset = (String) request.get("dataset");
            List<Map<String, Object>> sentences = (List<Map<String, Object>>) request.get("sentences");
            
            Map<String, Object> result = extractService.saveExtraction(literatureId, dataset, sentences);
            result.put("success", true);
            result.put("message", "保存成功");
            return result;
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "保存失败: " + e.getMessage());
            return error;
        }
    }
    
    /**
     * 保存纠错结果
     */
    @PostMapping("/correction/save")
    public Map<String, Object> saveCorrection(@RequestBody Map<String, Object> request) {
        try {
            String literatureId = (String) request.get("literatureId");
            List<Map<String, Object>> corrections = (List<Map<String, Object>>) request.get("corrections");
            
            Map<String, Object> result = extractService.saveCorrection(literatureId, corrections);
            result.put("success", true);
            result.put("message", "纠错结果保存成功");
            return result;
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "纠错结果保存失败: " + e.getMessage());
            return error;
        }
    }
    
    /**
     * 获取文献的所有抽取记录
     */
    @GetMapping("/literature/{literatureId}")
    public List<Map<String, Object>> getLiteratureRecords(@PathVariable String literatureId) {
        return extractService.getLiteratureExtractions(literatureId);
    }
}