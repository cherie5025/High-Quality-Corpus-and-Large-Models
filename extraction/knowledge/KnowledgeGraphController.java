package shu.xai.neo4j.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import shu.xai.neo4j.utils.Result;
import shu.xai.neo4j.utils.Neo4jHttpUtils;
import shu.xai.extraction.service.ExtractService;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/knowledge-graph")
@CrossOrigin(origins = "*")
public class KnowledgeGraphController {
    
    @Resource
    private Neo4jHttpUtils neo4jHttpUtils;
    
    @Autowired
    private ExtractService extractService;
    
    @GetMapping("/test")
    public Result<Map<String, Object>> testConnection() {
        Map<String, Object> result = new HashMap<>();
        boolean connected = neo4jHttpUtils.testConnection();
        result.put("connected", connected);
        result.put("message", connected ? "✅ Neo4j连接成功" : "❌ Neo4j连接失败");
        return Result.success(result);
    }
    
    @GetMapping("/literature/list")
    public Result<List<Map<String, Object>>> getLiteratureList() {
        try {
            List<String> literatureIds = extractService.getDistinctLiteratureIds();
            
            List<Map<String, Object>> list = new ArrayList<>();
            for (String litId : literatureIds) {
                Map<String, Object> item = new HashMap<>();
                item.put("literatureId", litId);
                item.put("title", "文献" + litId);
                item.put("year", "未知");
                list.add(item);
            }
            
            if (list.isEmpty()) {
                Map<String, Object> testPaper = new HashMap<>();
                testPaper.put("literatureId", "62");
                testPaper.put("title", "Aqueous electrolyte-based batteries");
                testPaper.put("year", "2024");
                list.add(testPaper);
            }
            
            return Result.success(list);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取文献列表失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/literature/{literatureId}/graph")
    public Result<Map<String, Object>> getLiteratureGraph(@PathVariable String literatureId) {
        System.out.println("\n========== 开始获取图谱数据 ==========");
        System.out.println("文献ID: " + literatureId);
        
        try {
            Map<String, Object> graphData = new HashMap<>();
            List<Map<String, Object>> nodes = new ArrayList<>();
            List<Map<String, Object>> edges = new ArrayList<>();
            Set<String> nodeIds = new HashSet<>();
            
            Map<String, List<Map<String, Object>>> nodeSentencesMap = new HashMap<>();
            Map<String, List<Map<String, Object>>> edgeSentencesMap = new HashMap<>();
            
            List<Map<String, Object>> sentences = extractService.getLiteratureExtractions(literatureId);
            
            System.out.println("\n--- 获取到的句子数量: " + (sentences != null ? sentences.size() : 0) + " ---");
            
            if (sentences != null && !sentences.isEmpty()) {
                int totalEntitiesCount = 0;
                int totalRelationsCount = 0;
                int useCorrectedEntityCount = 0;
                int useCorrectedRelationCount = 0;
                
                for (Map<String, Object> sentence : sentences) {
                    String sentenceText = (String) sentence.get("sentenceText");
                    String literatureIdFromSentence = (String) sentence.get("literatureId");
                    String literatureTitle = (String) sentence.get("literatureTitle");
                    
                    if (literatureIdFromSentence == null) {
                        literatureIdFromSentence = literatureId;
                    }
                    
                    if (sentenceText == null || sentenceText.isEmpty()) {
                        continue;
                    }
                    
                    // 优先使用纠错后的实体
                    Object correctedEntitiesObj = sentence.get("correctedEntities");
                    List<Map<String, Object>> entities = new ArrayList<>();
                    boolean usedCorrectedEntities = false;
                    
                    if (correctedEntitiesObj instanceof List && !((List) correctedEntitiesObj).isEmpty()) {
                        entities = (List<Map<String, Object>>) correctedEntitiesObj;
                        usedCorrectedEntities = true;
                        useCorrectedEntityCount++;
                    } else {
                        Object entitiesObj = sentence.get("entities");
                        if (entitiesObj instanceof List) {
                            entities = (List<Map<String, Object>>) entitiesObj;
                        }
                    }
                    
                    // 优先使用纠错后的关系
                    Object correctedRelationsObj = sentence.get("correctedRelations");
                    List<Map<String, Object>> relations = new ArrayList<>();
                    boolean usedCorrectedRelations = false;
                    
                    if (correctedRelationsObj instanceof List && !((List) correctedRelationsObj).isEmpty()) {
                        relations = (List<Map<String, Object>>) correctedRelationsObj;
                        usedCorrectedRelations = true;
                        useCorrectedRelationCount++;
                    } else {
                        Object relationsObj = sentence.get("relations");
                        if (relationsObj instanceof List) {
                            relations = (List<Map<String, Object>>) relationsObj;
                        }
                    }
                    
                    totalEntitiesCount += entities.size();
                    totalRelationsCount += relations.size();
                    
                    // 处理实体
                    for (Map<String, Object> entity : entities) {
                        String entityName = (String) entity.get("text");
                        String entityType = (String) entity.get("type");
                        
                        if (entityName != null && !entityName.isEmpty()) {
                            Map<String, Object> sentenceInfo = new HashMap<>();
                            sentenceInfo.put("sentenceText", sentenceText);
                            sentenceInfo.put("literatureId", literatureIdFromSentence);
                            sentenceInfo.put("literatureTitle", literatureTitle);
                            
                            if (!nodeSentencesMap.containsKey(entityName)) {
                                nodeSentencesMap.put(entityName, new ArrayList<>());
                            }
                            nodeSentencesMap.get(entityName).add(sentenceInfo);
                            
                            if (!nodeIds.contains(entityName)) {
                                nodeIds.add(entityName);
                                Map<String, Object> node = new HashMap<>();
                                node.put("id", entityName);
                                node.put("name", entityName);
                                node.put("label", entityName);
                                node.put("type", entityType != null ? entityType : "unknown");
                                node.put("degree", 0);
                                nodes.add(node);
                            }
                        }
                    }
                    
                    // 处理关系
                    for (Map<String, Object> rel : relations) {
                        String subject = (String) rel.get("subject");
                        String predicate = (String) rel.get("predicate");
                        String object = (String) rel.get("object");
                        
                        if (subject != null && !subject.isEmpty() && 
                            object != null && !object.isEmpty() && 
                            predicate != null && !predicate.isEmpty()) {
                            
                            if (!nodeIds.contains(subject)) {
                                nodeIds.add(subject);
                                Map<String, Object> node = new HashMap<>();
                                node.put("id", subject);
                                node.put("name", subject);
                                node.put("label", subject);
                                node.put("type", "unknown");
                                node.put("degree", 0);
                                nodes.add(node);
                            }
                            
                            if (!nodeIds.contains(object)) {
                                nodeIds.add(object);
                                Map<String, Object> node = new HashMap<>();
                                node.put("id", object);
                                node.put("name", object);
                                node.put("label", object);
                                node.put("type", "unknown");
                                node.put("degree", 0);
                                nodes.add(node);
                            }
                            
                            String edgeKey = subject + "_" + predicate + "_" + object;
                            
                            Map<String, Object> sentenceInfo = new HashMap<>();
                            sentenceInfo.put("sentenceText", sentenceText);
                            sentenceInfo.put("literatureId", literatureIdFromSentence);
                            sentenceInfo.put("literatureTitle", literatureTitle);
                            
                            if (!edgeSentencesMap.containsKey(edgeKey)) {
                                edgeSentencesMap.put(edgeKey, new ArrayList<>());
                            }
                            edgeSentencesMap.get(edgeKey).add(sentenceInfo);
                            
                            Map<String, Object> edge = new HashMap<>();
                            edge.put("source", subject);
                            edge.put("target", object);
                            edge.put("relation", predicate);
                            edge.put("label", predicate);
                            edges.add(edge);
                        }
                    }
                }
                
                System.out.println("\n=== 数据处理统计 ===");
                System.out.println("使用纠错实体的句子数: " + useCorrectedEntityCount + "/" + sentences.size());
                System.out.println("使用纠错关系的句子数: " + useCorrectedRelationCount + "/" + sentences.size());
                System.out.println("总实体数: " + totalEntitiesCount);
                System.out.println("总关系数: " + totalRelationsCount);
                System.out.println("唯一节点数: " + nodeIds.size());
            }
            
            // 为节点添加句子列表
            for (Map<String, Object> node : nodes) {
                String nodeName = (String) node.get("name");
                if (nodeSentencesMap.containsKey(nodeName)) {
                    node.put("sentences", nodeSentencesMap.get(nodeName));
                } else {
                    node.put("sentences", new ArrayList<>());
                }
            }
            
            // 为边添加句子列表，并去重
            List<Map<String, Object>> uniqueEdges = new ArrayList<>();
            Map<String, Map<String, Object>> edgeMap = new HashMap<>();
            
            for (Map<String, Object> edge : edges) {
                String source = (String) edge.get("source");
                String target = (String) edge.get("target");
                String relation = (String) edge.get("relation");
                String edgeKey = source + "_" + relation + "_" + target;
                
                if (edgeMap.containsKey(edgeKey)) {
                    Map<String, Object> existingEdge = edgeMap.get(edgeKey);
                    List<Map<String, Object>> existingSentences = (List<Map<String, Object>>) existingEdge.get("sentences");
                    List<Map<String, Object>> newSentences = edgeSentencesMap.get(edgeKey);
                    if (newSentences != null) {
                        for (Map<String, Object> s : newSentences) {
                            boolean exists = false;
                            for (Map<String, Object> es : existingSentences) {
                                if (es.get("sentenceText").equals(s.get("sentenceText"))) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                existingSentences.add(s);
                            }
                        }
                    }
                } else {
                    List<Map<String, Object>> sentencesList = edgeSentencesMap.get(edgeKey);
                    if (sentencesList == null) {
                        sentencesList = new ArrayList<>();
                    }
                    edge.put("sentences", sentencesList);
                    edgeMap.put(edgeKey, edge);
                    uniqueEdges.add(edge);
                }
            }
            
            // 计算节点度数
            for (Map<String, Object> edge : uniqueEdges) {
                String source = (String) edge.get("source");
                String target = (String) edge.get("target");
                
                for (Map<String, Object> node : nodes) {
                    String nodeName = (String) node.get("name");
                    if (nodeName.equals(source) || nodeName.equals(target)) {
                        int degree = (Integer) node.get("degree");
                        node.put("degree", degree + 1);
                    }
                }
            }
            
            graphData.put("nodes", nodes);
            graphData.put("edges", uniqueEdges);
            
            System.out.println("\n=== 最终返回数据 ===");
            System.out.println("节点数量: " + nodes.size());
            System.out.println("边数量: " + uniqueEdges.size());
            System.out.println("========== 图谱数据获取完成 ==========\n");
            
            return Result.success(graphData);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("获取图谱数据失败: " + e.getMessage());
            return Result.error("获取图谱数据失败: " + e.getMessage());
        }
    }
    
    // ==================== 获取全部图谱数据接口 ====================
    @GetMapping("/all-graph")
    public Result<Map<String, Object>> getAllGraph() {
        System.out.println("\n========== 开始获取全部图谱数据 ==========");
        
        try {
            Map<String, Object> graphData = new HashMap<>();
            List<Map<String, Object>> nodes = new ArrayList<>();
            List<Map<String, Object>> edges = new ArrayList<>();
            Set<String> nodeIds = new HashSet<>();
            
            Map<String, List<Map<String, Object>>> nodeSentencesMap = new HashMap<>();
            Map<String, List<Map<String, Object>>> edgeSentencesMap = new HashMap<>();
            
            // 获取所有文献ID
            List<String> literatureIds = extractService.getDistinctLiteratureIds();
            System.out.println("文献数量: " + literatureIds.size());
            
            List<Map<String, Object>> allSentences = new ArrayList<>();
            
            // 遍历所有文献，获取所有句子数据
            for (String litId : literatureIds) {
                List<Map<String, Object>> litSentences = extractService.getLiteratureExtractions(litId);
                if (litSentences != null && !litSentences.isEmpty()) {
                    for (Map<String, Object> sentence : litSentences) {
                        sentence.put("literatureId", litId);
                        allSentences.add(sentence);
                    }
                }
            }
            
            System.out.println("获取到的句子总数: " + allSentences.size());
            
            if (allSentences != null && !allSentences.isEmpty()) {
                int totalEntitiesCount = 0;
                int totalRelationsCount = 0;
                int useCorrectedEntityCount = 0;
                int useCorrectedRelationCount = 0;
                
                for (Map<String, Object> sentence : allSentences) {
                    String sentenceText = (String) sentence.get("sentenceText");
                    String literatureId = (String) sentence.get("literatureId");
                    String literatureTitle = (String) sentence.get("literatureTitle");
                    
                    if (sentenceText == null || sentenceText.isEmpty()) {
                        continue;
                    }
                    
                    // 优先使用纠错后的实体
                    Object correctedEntitiesObj = sentence.get("correctedEntities");
                    List<Map<String, Object>> entities = new ArrayList<>();
                    boolean usedCorrectedEntities = false;
                    
                    if (correctedEntitiesObj instanceof List && !((List) correctedEntitiesObj).isEmpty()) {
                        entities = (List<Map<String, Object>>) correctedEntitiesObj;
                        usedCorrectedEntities = true;
                        useCorrectedEntityCount++;
                    } else {
                        Object entitiesObj = sentence.get("entities");
                        if (entitiesObj instanceof List) {
                            entities = (List<Map<String, Object>>) entitiesObj;
                        }
                    }
                    
                    // 优先使用纠错后的关系
                    Object correctedRelationsObj = sentence.get("correctedRelations");
                    List<Map<String, Object>> relations = new ArrayList<>();
                    boolean usedCorrectedRelations = false;
                    
                    if (correctedRelationsObj instanceof List && !((List) correctedRelationsObj).isEmpty()) {
                        relations = (List<Map<String, Object>>) correctedRelationsObj;
                        usedCorrectedRelations = true;
                        useCorrectedRelationCount++;
                    } else {
                        Object relationsObj = sentence.get("relations");
                        if (relationsObj instanceof List) {
                            relations = (List<Map<String, Object>>) relationsObj;
                        }
                    }
                    
                    totalEntitiesCount += entities.size();
                    totalRelationsCount += relations.size();
                    
                    // 处理实体
                    for (Map<String, Object> entity : entities) {
                        String entityName = (String) entity.get("text");
                        String entityType = (String) entity.get("type");
                        
                        if (entityName != null && !entityName.isEmpty()) {
                            Map<String, Object> sentenceInfo = new HashMap<>();
                            sentenceInfo.put("sentenceText", sentenceText);
                            sentenceInfo.put("literatureId", literatureId);
                            sentenceInfo.put("literatureTitle", literatureTitle);
                            
                            if (!nodeSentencesMap.containsKey(entityName)) {
                                nodeSentencesMap.put(entityName, new ArrayList<>());
                            }
                            nodeSentencesMap.get(entityName).add(sentenceInfo);
                            
                            if (!nodeIds.contains(entityName)) {
                                nodeIds.add(entityName);
                                Map<String, Object> node = new HashMap<>();
                                node.put("id", entityName);
                                node.put("name", entityName);
                                node.put("label", entityName);
                                node.put("type", entityType != null ? entityType : "unknown");
                                node.put("degree", 0);
                                nodes.add(node);
                            }
                        }
                    }
                    
                    // 处理关系
                    for (Map<String, Object> rel : relations) {
                        String subject = (String) rel.get("subject");
                        String predicate = (String) rel.get("predicate");
                        String object = (String) rel.get("object");
                        
                        if (subject != null && !subject.isEmpty() && 
                            object != null && !object.isEmpty() && 
                            predicate != null && !predicate.isEmpty()) {
                            
                            if (!nodeIds.contains(subject)) {
                                nodeIds.add(subject);
                                Map<String, Object> node = new HashMap<>();
                                node.put("id", subject);
                                node.put("name", subject);
                                node.put("label", subject);
                                node.put("type", "unknown");
                                node.put("degree", 0);
                                nodes.add(node);
                            }
                            
                            if (!nodeIds.contains(object)) {
                                nodeIds.add(object);
                                Map<String, Object> node = new HashMap<>();
                                node.put("id", object);
                                node.put("name", object);
                                node.put("label", object);
                                node.put("type", "unknown");
                                node.put("degree", 0);
                                nodes.add(node);
                            }
                            
                            String edgeKey = subject + "_" + predicate + "_" + object;
                            
                            Map<String, Object> sentenceInfo = new HashMap<>();
                            sentenceInfo.put("sentenceText", sentenceText);
                            sentenceInfo.put("literatureId", literatureId);
                            sentenceInfo.put("literatureTitle", literatureTitle);
                            
                            if (!edgeSentencesMap.containsKey(edgeKey)) {
                                edgeSentencesMap.put(edgeKey, new ArrayList<>());
                            }
                            edgeSentencesMap.get(edgeKey).add(sentenceInfo);
                            
                            Map<String, Object> edge = new HashMap<>();
                            edge.put("source", subject);
                            edge.put("target", object);
                            edge.put("relation", predicate);
                            edge.put("label", predicate);
                            edges.add(edge);
                        }
                    }
                }
                
                System.out.println("\n=== 数据处理统计 ===");
                System.out.println("使用纠错实体的句子数: " + useCorrectedEntityCount + "/" + allSentences.size());
                System.out.println("使用纠错关系的句子数: " + useCorrectedRelationCount + "/" + allSentences.size());
                System.out.println("总实体数: " + totalEntitiesCount);
                System.out.println("总关系数: " + totalRelationsCount);
                System.out.println("唯一节点数: " + nodeIds.size());
            }
            
            // 为节点添加句子列表
            for (Map<String, Object> node : nodes) {
                String nodeName = (String) node.get("name");
                if (nodeSentencesMap.containsKey(nodeName)) {
                    node.put("sentences", nodeSentencesMap.get(nodeName));
                } else {
                    node.put("sentences", new ArrayList<>());
                }
            }
            
            // 为边添加句子列表，并去重
            List<Map<String, Object>> uniqueEdges = new ArrayList<>();
            Map<String, Map<String, Object>> edgeMap = new HashMap<>();
            
            for (Map<String, Object> edge : edges) {
                String source = (String) edge.get("source");
                String target = (String) edge.get("target");
                String relation = (String) edge.get("relation");
                String edgeKey = source + "_" + relation + "_" + target;
                
                if (edgeMap.containsKey(edgeKey)) {
                    Map<String, Object> existingEdge = edgeMap.get(edgeKey);
                    List<Map<String, Object>> existingSentences = (List<Map<String, Object>>) existingEdge.get("sentences");
                    List<Map<String, Object>> newSentences = edgeSentencesMap.get(edgeKey);
                    if (newSentences != null) {
                        for (Map<String, Object> s : newSentences) {
                            boolean exists = false;
                            for (Map<String, Object> es : existingSentences) {
                                if (es.get("sentenceText").equals(s.get("sentenceText")) && 
                                    es.get("literatureId").equals(s.get("literatureId"))) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                existingSentences.add(s);
                            }
                        }
                    }
                } else {
                    List<Map<String, Object>> sentencesList = edgeSentencesMap.get(edgeKey);
                    if (sentencesList == null) {
                        sentencesList = new ArrayList<>();
                    }
                    edge.put("sentences", sentencesList);
                    edgeMap.put(edgeKey, edge);
                    uniqueEdges.add(edge);
                }
            }
            
            // 计算节点度数
            for (Map<String, Object> edge : uniqueEdges) {
                String source = (String) edge.get("source");
                String target = (String) edge.get("target");
                
                for (Map<String, Object> node : nodes) {
                    String nodeName = (String) node.get("name");
                    if (nodeName.equals(source) || nodeName.equals(target)) {
                        int degree = (Integer) node.get("degree");
                        node.put("degree", degree + 1);
                    }
                }
            }
            
            graphData.put("nodes", nodes);
            graphData.put("edges", uniqueEdges);
            
            System.out.println("\n=== 最终返回数据 ===");
            System.out.println("节点数量: " + nodes.size());
            System.out.println("边数量: " + uniqueEdges.size());
            System.out.println("========== 全部图谱数据获取完成 ==========\n");
            
            return Result.success(graphData);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("获取全部图谱数据失败: " + e.getMessage());
            return Result.error("获取全部图谱数据失败: " + e.getMessage());
        }
    }
    
    // ==================== 节点搜索接口 ====================
    @GetMapping("/nodes/search")
    public Result<List<Map<String, Object>>> searchNodes(
            @RequestParam String literatureId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "50") Integer limit) {
        System.out.println("\n========== 节点搜索 ==========");
        System.out.println("文献ID: " + literatureId);
        System.out.println("关键词: " + keyword);
        System.out.println("限制数量: " + limit);
        
        try {
            List<Map<String, Object>> sentences = extractService.getLiteratureExtractions(literatureId);
            
            if (sentences == null || sentences.isEmpty()) {
                System.out.println("未找到该文献的数据");
                return Result.success(new ArrayList<>());
            }
            
            Map<String, Map<String, Object>> nodeMap = new HashMap<>();
            Map<String, Set<Map<String, Object>>> nodeSentencesMap = new HashMap<>();
            
            for (Map<String, Object> sentence : sentences) {
                String sentenceText = (String) sentence.get("sentenceText");
                String literatureIdFromSentence = (String) sentence.get("literatureId");
                String literatureTitle = (String) sentence.get("literatureTitle");
                
                if (literatureIdFromSentence == null) {
                    literatureIdFromSentence = literatureId;
                }
                
                if (sentenceText == null || sentenceText.isEmpty()) {
                    continue;
                }
                
                Object correctedEntitiesObj = sentence.get("correctedEntities");
                List<Map<String, Object>> entities = new ArrayList<>();
                
                if (correctedEntitiesObj instanceof List && !((List) correctedEntitiesObj).isEmpty()) {
                    entities = (List<Map<String, Object>>) correctedEntitiesObj;
                } else {
                    Object entitiesObj = sentence.get("entities");
                    if (entitiesObj instanceof List) {
                        entities = (List<Map<String, Object>>) entitiesObj;
                    }
                }
                
                for (Map<String, Object> entity : entities) {
                    String entityName = (String) entity.get("text");
                    String entityType = (String) entity.get("type");
                    
                    if (entityName == null || entityName.isEmpty()) {
                        continue;
                    }
                    
                    if (entityName.toLowerCase().contains(keyword.toLowerCase())) {
                        if (!nodeMap.containsKey(entityName)) {
                            Map<String, Object> node = new HashMap<>();
                            node.put("id", entityName);
                            node.put("name", entityName);
                            node.put("type", entityType != null ? entityType : "unknown");
                            node.put("degree", 0);
                            nodeMap.put(entityName, node);
                            nodeSentencesMap.put(entityName, new HashSet<>());
                        }
                        
                        Map<String, Object> sentenceInfo = new HashMap<>();
                        sentenceInfo.put("sentenceText", sentenceText);
                        sentenceInfo.put("literatureId", literatureIdFromSentence);
                        sentenceInfo.put("literatureTitle", literatureTitle);
                        nodeSentencesMap.get(entityName).add(sentenceInfo);
                    }
                }
            }
            
            List<Map<String, Object>> resultNodes = new ArrayList<>(nodeMap.values());
            
            for (Map<String, Object> node : resultNodes) {
                String nodeName = (String) node.get("name");
                Set<Map<String, Object>> sentencesSet = nodeSentencesMap.get(nodeName);
                if (sentencesSet != null && !sentencesSet.isEmpty()) {
                    node.put("sentences", new ArrayList<>(sentencesSet));
                } else {
                    node.put("sentences", new ArrayList<>());
                }
            }
            
            resultNodes.sort(Comparator.comparing(n -> (String) n.get("name")));
            
            if (resultNodes.size() > limit) {
                resultNodes = resultNodes.subList(0, limit);
            }
            
            System.out.println("找到节点数: " + resultNodes.size());
            System.out.println("========== 节点搜索完成 ==========\n");
            
            return Result.success(resultNodes);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("节点搜索失败: " + e.getMessage());
            return Result.error("节点搜索失败: " + e.getMessage());
        }
    }
    
    // ==================== 关系搜索接口 ====================
    @GetMapping("/relations/search")
    public Result<List<Map<String, Object>>> searchRelations(
            @RequestParam String literatureId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "50") Integer limit) {
        System.out.println("\n========== 关系搜索 ==========");
        System.out.println("文献ID: " + literatureId);
        System.out.println("关键词: " + keyword);
        System.out.println("限制数量: " + limit);
        
        try {
            List<Map<String, Object>> sentences = extractService.getLiteratureExtractions(literatureId);
            
            if (sentences == null || sentences.isEmpty()) {
                System.out.println("未找到该文献的数据");
                return Result.success(new ArrayList<>());
            }
            
            Map<String, Map<String, Object>> relationMap = new HashMap<>();
            
            for (Map<String, Object> sentence : sentences) {
                String sentenceText = (String) sentence.get("sentenceText");
                String literatureIdFromSentence = (String) sentence.get("literatureId");
                String literatureTitle = (String) sentence.get("literatureTitle");
                
                if (literatureIdFromSentence == null) {
                    literatureIdFromSentence = literatureId;
                }
                
                if (sentenceText == null || sentenceText.isEmpty()) {
                    continue;
                }
                
                Object correctedRelationsObj = sentence.get("correctedRelations");
                List<Map<String, Object>> relations = new ArrayList<>();
                
                if (correctedRelationsObj instanceof List && !((List) correctedRelationsObj).isEmpty()) {
                    relations = (List<Map<String, Object>>) correctedRelationsObj;
                } else {
                    Object relationsObj = sentence.get("relations");
                    if (relationsObj instanceof List) {
                        relations = (List<Map<String, Object>>) relationsObj;
                    }
                }
                
                for (Map<String, Object> rel : relations) {
                    String subject = (String) rel.get("subject");
                    String predicate = (String) rel.get("predicate");
                    String object = (String) rel.get("object");
                    
                    if (subject == null || subject.isEmpty() || 
                        predicate == null || predicate.isEmpty() || 
                        object == null || object.isEmpty()) {
                        continue;
                    }
                    
                    boolean matchPredicate = predicate.toLowerCase().contains(keyword.toLowerCase());
                    boolean matchSubject = subject.toLowerCase().contains(keyword.toLowerCase());
                    boolean matchObject = object.toLowerCase().contains(keyword.toLowerCase());
                    
                    if (matchPredicate || matchSubject || matchObject) {
                        String relationKey = subject + "_" + predicate + "_" + object;
                        
                        if (!relationMap.containsKey(relationKey)) {
                            Map<String, Object> relation = new HashMap<>();
                            relation.put("source", subject);
                            relation.put("sourceName", subject);
                            relation.put("target", object);
                            relation.put("targetName", object);
                            relation.put("relation", predicate);
                            relation.put("sentences", new ArrayList<Map<String, Object>>());
                            relationMap.put(relationKey, relation);
                        }
                        
                        Map<String, Object> relation = relationMap.get(relationKey);
                        List<Map<String, Object>> sentencesList = (List<Map<String, Object>>) relation.get("sentences");
                        
                        Map<String, Object> sentenceInfo = new HashMap<>();
                        sentenceInfo.put("sentenceText", sentenceText);
                        sentenceInfo.put("literatureId", literatureIdFromSentence);
                        sentenceInfo.put("literatureTitle", literatureTitle);
                        
                        boolean exists = false;
                        for (Map<String, Object> s : sentencesList) {
                            if (s.get("sentenceText").equals(sentenceText)) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            sentencesList.add(sentenceInfo);
                        }
                    }
                }
            }
            
            List<Map<String, Object>> resultRelations = new ArrayList<>(relationMap.values());
            resultRelations.sort(Comparator.comparing(r -> (String) r.get("relation")));
            
            if (resultRelations.size() > limit) {
                resultRelations = resultRelations.subList(0, limit);
            }
            
            System.out.println("找到关系数: " + resultRelations.size());
            System.out.println("========== 关系搜索完成 ==========\n");
            
            return Result.success(resultRelations);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("关系搜索失败: " + e.getMessage());
            return Result.error("关系搜索失败: " + e.getMessage());
        }
    }
}