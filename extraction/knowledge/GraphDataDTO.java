package shu.xai.neo4j.dto;

import java.util.List;
import java.util.Map;

public class GraphDataDTO {
    private List<Map<String, Object>> nodes;
    private List<Map<String, Object>> edges;  // 改为 edges
    
    public GraphDataDTO() {}
    
    public GraphDataDTO(List<Map<String, Object>> nodes, List<Map<String, Object>> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }
    
    // Getters and Setters
    public List<Map<String, Object>> getNodes() { return nodes; }
    public void setNodes(List<Map<String, Object>> nodes) { this.nodes = nodes; }
    
    public List<Map<String, Object>> getEdges() { return edges; }  // 改为 edges
    public void setEdges(List<Map<String, Object>> edges) { this.edges = edges; }  // 改为 edges
}