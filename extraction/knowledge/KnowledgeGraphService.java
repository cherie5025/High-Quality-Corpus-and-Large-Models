package shu.xai.neo4j.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import shu.xai.neo4j.utils.Neo4jHttpUtils;
import shu.xai.neo4j.dto.GraphDataDTO;
import shu.xai.extraction.service.ExtractService;

import javax.annotation.Resource;
import java.util.*;

@Service
public class KnowledgeGraphService {
    
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeGraphService.class);
    
    @Resource
    private Neo4jHttpUtils neo4jHttpUtils;
    
    @Autowired
    private ExtractService extractService;
    
    /**
     * 获取有抽取结果的文献列表
     */
    public List<Map<String, Object>> getLiteratureList() {
        List<Map<String, Object>> result = new ArrayList<>();
        
        try {
            List<String> literatureIds = extractService.getDistinctLiteratureIds();
            
            if (literatureIds != null && !literatureIds.isEmpty()) {
                for (String litId : literatureIds) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("literatureId", litId);
                    item.put("title", "文献" + litId);
                    item.put("year", "未知");
                    result.add(item);
                }
                logger.info("获取到 {} 篇文献", result.size());
            } else {
                logger.warn("没有找到文献数据，使用测试数据");
                Map<String, Object> testPaper = new HashMap<>();
                testPaper.put("literatureId", "62");
                testPaper.put("title", "Aqueous electrolyte-based batteries");
                testPaper.put("year", "2024");
                result.add(testPaper);
            }
            
        } catch (Exception e) {
            logger.error("获取文献列表失败: {}", e.getMessage());
            Map<String, Object> testPaper = new HashMap<>();
            testPaper.put("literatureId", "62");
            testPaper.put("title", "Aqueous electrolyte-based batteries");
            testPaper.put("year", "2024");
            result.add(testPaper);
        }
        
        return result;
    }
    
    /**
     * 获取文献的图谱数据
     */
    public Map<String, Object> getLiteratureGraph(String literatureId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        Set<String> nodeIds = new HashSet<>();
        
        // 存储节点对应的句子信息（改为存储Map对象）
        Map<String, List<Map<String, Object>>> nodeSentencesMap = new HashMap<>();
        Map<String, List<Map<String, Object>>> edgeSentencesMap = new HashMap<>();
        
        try {
            List<Map<String, Object>> sentences = extractService.getLiteratureExtractions(literatureId);
            
            logger.info("获取到 {} 条句子数据", sentences != null ? sentences.size() : 0);
            
            if (sentences != null && !sentences.isEmpty()) {
                for (Map<String, Object> sentence : sentences) {
                    String sentenceText = (String) sentence.get("sentenceText");
                    if (sentenceText == null || sentenceText.isEmpty()) {
                        continue;
                    }
                    
                    // 获取文献ID和标题
                    String litId = (String) sentence.get("literatureId");
                    String litTitle = (String) sentence.get("literatureTitle");
                    
                    // 如果没有标题，尝试从其他地方获取
                    if (litTitle == null || litTitle.isEmpty()) {
                        litTitle = (String) sentence.get("paperTitle");
                    }
                    if (litTitle == null || litTitle.isEmpty()) {
                        litTitle = "文献" + litId;
                    }
                    if (litId == null || litId.isEmpty()) {
                        litId = literatureId;
                    }
                    
                    // 构建句子信息对象
                    Map<String, Object> sentenceInfo = new HashMap<>();
                    sentenceInfo.put("sentenceText", sentenceText);
                    sentenceInfo.put("literatureId", litId);
                    sentenceInfo.put("literatureTitle", litTitle);
                    
                    // 获取实体
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
                    
                    // 获取关系
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
                    
                    // 处理实体（节点）
                    for (Map<String, Object> entity : entities) {
                        String entityName = (String) entity.get("text");
                        String entityType = (String) entity.get("type");
                        
                        if (entityName != null && !entityName.isEmpty()) {
                            // 记录节点对应的句子信息
                            if (!nodeSentencesMap.containsKey(entityName)) {
                                nodeSentencesMap.put(entityName, new ArrayList<Map<String, Object>>());
                            }
                            nodeSentencesMap.get(entityName).add(sentenceInfo);
                            
                            // 添加节点（去重）
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
                    
                    // 处理关系（边）
                    for (Map<String, Object> rel : relations) {
                        String subject = (String) rel.get("subject");
                        String predicate = (String) rel.get("predicate");
                        String object = (String) rel.get("object");
                        
                        if (subject != null && !subject.isEmpty() && 
                            object != null && !object.isEmpty() && 
                            predicate != null && !predicate.isEmpty()) {
                            
                            // 添加头实体节点（如果不存在）
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
                            
                            // 添加尾实体节点（如果不存在）
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
                            
                            // 记录边对应的句子信息
                            String edgeKey = subject + "_" + predicate + "_" + object;
                            if (!edgeSentencesMap.containsKey(edgeKey)) {
                                edgeSentencesMap.put(edgeKey, new ArrayList<Map<String, Object>>());
                            }
                            edgeSentencesMap.get(edgeKey).add(sentenceInfo);
                            
                            // 添加边
                            Map<String, Object> edge = new HashMap<>();
                            edge.put("source", subject);
                            edge.put("target", object);
                            edge.put("relation", predicate);
                            edge.put("label", predicate);
                            edges.add(edge);
                        }
                    }
                }
            }
            
            // 为节点添加句子列表
            for (Map<String, Object> node : nodes) {
                String nodeName = (String) node.get("name");
                List<Map<String, Object>> sentencesList = nodeSentencesMap.get(nodeName);
                if (sentencesList != null && !sentencesList.isEmpty()) {
                    // 去重（按sentenceText去重）
                    List<Map<String, Object>> uniqueSentences = new ArrayList<>();
                    Set<String> textSet = new HashSet<>();
                    for (Map<String, Object> s : sentencesList) {
                        String text = (String) s.get("sentenceText");
                        if (!textSet.contains(text)) {
                            textSet.add(text);
                            uniqueSentences.add(s);
                        }
                    }
                    node.put("sentences", uniqueSentences);
                } else {
                    node.put("sentences", new ArrayList<Map<String, Object>>());
                }
            }
            
            // 边去重并添加句子列表
            List<Map<String, Object>> uniqueEdges = new ArrayList<>();
            Map<String, Map<String, Object>> edgeMap = new HashMap<>();
            
            for (Map<String, Object> edge : edges) {
                String source = (String) edge.get("source");
                String target = (String) edge.get("target");
                String relation = (String) edge.get("relation");
                String edgeKey = source + "_" + relation + "_" + target;
                
                if (edgeMap.containsKey(edgeKey)) {
                    // 已存在，合并句子
                    Map<String, Object> existingEdge = edgeMap.get(edgeKey);
                    List<Map<String, Object>> existingSentences = (List<Map<String, Object>>) existingEdge.get("sentences");
                    List<Map<String, Object>> newSentences = edgeSentencesMap.get(edgeKey);
                    if (newSentences != null) {
                        // 去重合并
                        Set<String> textSet = new HashSet<>();
                        for (Map<String, Object> s : existingSentences) {
                            textSet.add((String) s.get("sentenceText"));
                        }
                        for (Map<String, Object> s : newSentences) {
                            String text = (String) s.get("sentenceText");
                            if (!textSet.contains(text)) {
                                textSet.add(text);
                                existingSentences.add(s);
                            }
                        }
                    }
                } else {
                    List<Map<String, Object>> sentencesList = edgeSentencesMap.get(edgeKey);
                    if (sentencesList == null) {
                        sentencesList = new ArrayList<Map<String, Object>>();
                    } else {
                        // 去重
                        List<Map<String, Object>> uniqueList = new ArrayList<>();
                        Set<String> textSet = new HashSet<>();
                        for (Map<String, Object> s : sentencesList) {
                            String text = (String) s.get("sentenceText");
                            if (!textSet.contains(text)) {
                                textSet.add(text);
                                uniqueList.add(s);
                            }
                        }
                        sentencesList = uniqueList;
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
            
            result.put("nodes", nodes);
            result.put("edges", uniqueEdges);
            
            logger.info("图谱数据构建完成 - 节点: {}, 边: {}", nodes.size(), uniqueEdges.size());
            
        } catch (Exception e) {
            logger.error("获取文献图谱失败: {}", e.getMessage(), e);
            result.put("nodes", new ArrayList<>());
            result.put("edges", new ArrayList<>());
        }
        
        return result;
    }
    
    /**
     * 搜索节点
     */
    public List<Map<String, Object>> searchNodes(String literatureId, String keyword, Integer limit) {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Map<String, Object>> nodeMap = new HashMap<>();
        Map<String, List<Map<String, Object>>> nodeSentencesMap = new HashMap<>();
        
        try {
            List<Map<String, Object>> sentences = extractService.getLiteratureExtractions(literatureId);
            
            if (sentences == null || sentences.isEmpty()) {
                logger.info("文献 {} 没有数据", literatureId);
                return result;
            }
            
            String lowerKeyword = keyword.toLowerCase();
            
            for (Map<String, Object> sentence : sentences) {
                String sentenceText = (String) sentence.get("sentenceText");
                if (sentenceText == null || sentenceText.isEmpty()) {
                    continue;
                }
                
                // 获取文献信息
                String litId = (String) sentence.get("literatureId");
                String litTitle = (String) sentence.get("literatureTitle");
                if (litTitle == null || litTitle.isEmpty()) {
                    litTitle = "文献" + litId;
                }
                
                Map<String, Object> sentenceInfo = new HashMap<>();
                sentenceInfo.put("sentenceText", sentenceText);
                sentenceInfo.put("literatureId", litId);
                sentenceInfo.put("literatureTitle", litTitle);
                
                // 获取实体
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
                
                // 处理实体
                for (Map<String, Object> entity : entities) {
                    String entityName = (String) entity.get("text");
                    String entityType = (String) entity.get("type");
                    
                    if (entityName != null && !entityName.isEmpty() && 
                        entityName.toLowerCase().contains(lowerKeyword)) {
                        
                        if (!nodeMap.containsKey(entityName)) {
                            Map<String, Object> node = new HashMap<>();
                            node.put("id", entityName);
                            node.put("name", entityName);
                            node.put("type", entityType != null ? entityType : "unknown");
                            node.put("degree", 0);
                            nodeMap.put(entityName, node);
                            nodeSentencesMap.put(entityName, new ArrayList<Map<String, Object>>());
                        }
                        nodeSentencesMap.get(entityName).add(sentenceInfo);
                    }
                }
            }
            
            // 计算度数
            for (Map<String, Object> sentence : sentences) {
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
                    String object = (String) rel.get("object");
                    
                    for (Map<String, Object> node : nodeMap.values()) {
                        String nodeName = (String) node.get("name");
                        if (nodeName.equals(subject) || nodeName.equals(object)) {
                            int degree = (Integer) node.get("degree");
                            node.put("degree", degree + 1);
                        }
                    }
                }
            }
            
            // 添加句子列表
            for (Map<String, Object> node : nodeMap.values()) {
                String nodeName = (String) node.get("name");
                List<Map<String, Object>> sentencesList = nodeSentencesMap.get(nodeName);
                if (sentencesList != null && !sentencesList.isEmpty()) {
                    // 去重
                    List<Map<String, Object>> uniqueSentences = new ArrayList<>();
                    Set<String> textSet = new HashSet<>();
                    for (Map<String, Object> s : sentencesList) {
                        String text = (String) s.get("sentenceText");
                        if (!textSet.contains(text)) {
                            textSet.add(text);
                            uniqueSentences.add(s);
                        }
                    }
                    node.put("sentences", uniqueSentences);
                } else {
                    node.put("sentences", new ArrayList<Map<String, Object>>());
                }
            }
            
            // 转换为列表并排序
            result = new ArrayList<>(nodeMap.values());
            result.sort(new Comparator<Map<String, Object>>() {
                public int compare(Map<String, Object> a, Map<String, Object> b) {
                    String nameA = (String) a.get("name");
                    String nameB = (String) b.get("name");
                    return nameA.compareTo(nameB);
                }
            });
            
            // 限制数量
            if (result.size() > limit) {
                result = result.subList(0, limit);
            }
            
            logger.info("节点搜索完成 - 关键词: {}, 结果数: {}", keyword, result.size());
            
        } catch (Exception e) {
            logger.error("节点搜索失败: {}", e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * 搜索关系
     */
    public List<Map<String, Object>> searchRelations(String literatureId, String keyword, Integer limit) {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Map<String, Object>> relationMap = new HashMap<>();
        
        try {
            List<Map<String, Object>> sentences = extractService.getLiteratureExtractions(literatureId);
            
            if (sentences == null || sentences.isEmpty()) {
                logger.info("文献 {} 没有数据", literatureId);
                return result;
            }
            
            String lowerKeyword = keyword.toLowerCase();
            
            for (Map<String, Object> sentence : sentences) {
                String sentenceText = (String) sentence.get("sentenceText");
                if (sentenceText == null || sentenceText.isEmpty()) {
                    continue;
                }
                
                // 获取文献信息
                String litId = (String) sentence.get("literatureId");
                String litTitle = (String) sentence.get("literatureTitle");
                if (litTitle == null || litTitle.isEmpty()) {
                    litTitle = "文献" + litId;
                }
                
                Map<String, Object> sentenceInfo = new HashMap<>();
                sentenceInfo.put("sentenceText", sentenceText);
                sentenceInfo.put("literatureId", litId);
                sentenceInfo.put("literatureTitle", litTitle);
                
                // 获取关系
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
                    
                    // 关键词匹配
                    boolean matchPredicate = predicate.toLowerCase().contains(lowerKeyword);
                    boolean matchSubject = subject.toLowerCase().contains(lowerKeyword);
                    boolean matchObject = object.toLowerCase().contains(lowerKeyword);
                    
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
                        
                        // 去重添加
                        boolean exists = false;
                        for (Map<String, Object> s : sentencesList) {
                            if (sentenceText.equals(s.get("sentenceText"))) {
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
            
            // 转换为列表并排序
            result = new ArrayList<>(relationMap.values());
            result.sort(new Comparator<Map<String, Object>>() {
                public int compare(Map<String, Object> a, Map<String, Object> b) {
                    String relA = (String) a.get("relation");
                    String relB = (String) b.get("relation");
                    return relA.compareTo(relB);
                }
            });
            
            // 限制数量
            if (result.size() > limit) {
                result = result.subList(0, limit);
            }
            
            logger.info("关系搜索完成 - 关键词: {}, 结果数: {}", keyword, result.size());
            
        } catch (Exception e) {
            logger.error("关系搜索失败: {}", e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * 获取全局图谱（从Neo4j）
     */
    public GraphDataDTO getGlobalGraph(Integer limit) {
        if (limit == null || limit <= 0) limit = 100;
        String cypher = "MATCH (n)-[r]->(m) RETURN n, r, m LIMIT " + limit;
        List<Map<String, Object>> results = neo4jHttpUtils.executeQuery(cypher);
        return parseGraphResults(results);
    }
    
    /**
     * 获取节点图谱（从Neo4j）
     */
    public GraphDataDTO getNodeGraph(String nodeName) {
        String cypher = "MATCH (n {name: '" + nodeName + "'})-[r*0..1]-(m) RETURN n, r, m";
        List<Map<String, Object>> results = neo4jHttpUtils.executeQuery(cypher);
        return parseGraphResults(results);
    }
    
    private GraphDataDTO parseGraphResults(List<Map<String, Object>> results) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> links = new ArrayList<>();
        Set<String> nodeIds = new HashSet<>();
        
        // TODO: 根据实际返回结构解析
        
        return new GraphDataDTO(nodes, links);
    }
}