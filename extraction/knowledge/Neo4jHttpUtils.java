package shu.xai.neo4j.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class Neo4jHttpUtils {
    
    @Value("${neo4j.http.uri:http://localhost:7474}")
    private String httpUri;
    
    @Value("${neo4j.username:neo4j}")
    private String username;
    
    @Value("${neo4j.password:12345678}")
    private String password;
    
    private CloseableHttpClient httpClient;
    private String authHeader;
    
    @PostConstruct
    public void init() {
        httpClient = HttpClients.createDefault();
        // 基础认证
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        authHeader = "Basic " + encodedAuth;
        System.out.println("✅ Neo4j HTTP 客户端初始化完成");
    }
    
    public boolean testConnection() {
        try {
            String query = "{\"statements\":[{\"statement\":\"RETURN 1\"}]}";
            
            HttpPost httpPost = new HttpPost(httpUri + "/db/neo4j/tx/commit");
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Authorization", authHeader);
            httpPost.setEntity(new StringEntity(query, StandardCharsets.UTF_8));
            
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                
                if (statusCode == 200) {
                    System.out.println("✅ Neo4j HTTP 连接测试成功");
                    return true;
                } else {
                    System.out.println("❌ Neo4j HTTP 连接测试失败，状态码: " + statusCode);
                    System.out.println("响应: " + responseBody);
                    return false;
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Neo4j HTTP 连接测试异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public List<Map<String, Object>> executeQuery(String cypher) {
        return executeQuery(cypher, null);
    }
    
    public List<Map<String, Object>> executeQuery(String cypher, Map<String, Object> params) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try {
            // 构建请求体
            Map<String, Object> statement = new HashMap<>();
            statement.put("statement", cypher);
            if (params != null && !params.isEmpty()) {
                statement.put("parameters", params);
            }
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("statements", Collections.singletonList(statement));
            
            String jsonBody = JSON.toJSONString(requestBody);
            System.out.println("发送查询: " + jsonBody);
            
            HttpPost httpPost = new HttpPost(httpUri + "/db/neo4j/tx/commit");
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Authorization", authHeader);
            httpPost.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));
            
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                
                if (statusCode == 200) {
                    JSONObject jsonResponse = JSON.parseObject(responseBody);
                    JSONArray resultsArray = jsonResponse.getJSONArray("results");
                    
                    if (resultsArray != null && !resultsArray.isEmpty()) {
                        JSONArray data = resultsArray.getJSONObject(0).getJSONArray("data");
                        JSONArray columns = resultsArray.getJSONObject(0).getJSONArray("columns");
                        
                        for (int i = 0; i < data.size(); i++) {
                            JSONArray row = data.getJSONObject(i).getJSONArray("row");
                            Map<String, Object> rowMap = new HashMap<>();
                            
                            for (int j = 0; j < columns.size(); j++) {
                                rowMap.put(columns.getString(j), row.get(j));
                            }
                            results.add(rowMap);
                        }
                    }
                    System.out.println("✅ 查询成功，返回 " + results.size() + " 条结果");
                } else {
                    System.err.println("❌ 查询失败，状态码: " + statusCode);
                    System.err.println("响应: " + responseBody);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ 执行查询异常: " + e.getMessage());
            e.printStackTrace();
        }
        
        return results;
    }
    
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 获取节点总数
            List<Map<String, Object>> nodeResult = executeQuery("MATCH (n) RETURN count(n) as count");
            if (!nodeResult.isEmpty()) {
                stats.put("nodeCount", nodeResult.get(0).get("count"));
            }
            
            // 获取关系总数
            List<Map<String, Object>> relResult = executeQuery("MATCH ()-[r]->() RETURN count(r) as count");
            if (!relResult.isEmpty()) {
                stats.put("relationCount", relResult.get(0).get("count"));
            }
            
            // 获取实体类型分布
            List<Map<String, Object>> typeResult = executeQuery(
                "MATCH (n) RETURN n.type as type, count(n) as count ORDER BY count DESC"
            );
            stats.put("entityTypes", typeResult);
            
        } catch (Exception e) {
            System.err.println("获取统计信息失败: " + e.getMessage());
        }
        
        return stats;
    }
    
    public Map<String, Object> getGraphData(String entityName, int depth) {
        Map<String, Object> result = new HashMap<>();
        
        String query = 
            "MATCH path = (e:Entity {name: $name})-[*1.." + depth + "]-(connected) " +
            "RETURN nodes(path) as nodes, relationships(path) as relationships";
        
        Map<String, Object> params = new HashMap<>();
        params.put("name", entityName);
        
        try {
            List<Map<String, Object>> results = executeQuery(query, params);
            
            Set<Map<String, Object>> nodesSet = new HashSet<>();
            List<Map<String, Object>> edges = new ArrayList<>();
            
            for (Map<String, Object> row : results) {
                Object nodes = row.get("nodes");
                Object relationships = row.get("relationships");
                
                // 这里需要根据实际返回格式处理
                // 简化处理，实际项目中需要更详细的解析
            }
            
            result.put("nodes", new ArrayList<>(nodesSet));
            result.put("edges", edges);
            
        } catch (Exception e) {
            System.err.println("获取图谱数据失败: " + e.getMessage());
        }
        
        return result;
    }
}