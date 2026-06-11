package shu.xai.neo4j.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Configuration
public class Neo4jConfig {
    
    @Value("${neo4j.uri:jdbc:neo4j:bolt://localhost:7687/?user=neo4j,password=123456}")
    private String uri;
    
    @Value("${neo4j.username:neo4j}")
    private String username;
    
    @Value("${neo4j.password:123456}")
    private String password;
    
    @Bean
    public Neo4jConnectionManager neo4jConnectionManager() {
        try {
            // 尝试两种可能的驱动类
            try {
                Class.forName("org.neo4j.jdbc.bolt.BoltDriver");
                System.out.println("✅ Neo4j BoltDriver loaded");
            } catch (ClassNotFoundException e1) {
                Class.forName("org.neo4j.jdbc.Driver");
                System.out.println("✅ Neo4j JDBC Driver loaded");
            }
            
            // 测试连接
            testConnection(uri, username, password);
            
            return new Neo4jConnectionManager(uri, username, password);
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Neo4j JDBC Driver not found");
            e.printStackTrace();
            throw new RuntimeException("Neo4j JDBC Driver not found", e);
        }
    }
    
    private void testConnection(String uri, String username, String password) {
        try (Connection conn = DriverManager.getConnection(uri, username, password)) {
            System.out.println("✅ Neo4j connection test successful");
        } catch (SQLException e) {
            System.err.println("❌ Neo4j connection test failed: " + e.getMessage());
            // 不抛出异常，让应用继续启动，但打印警告
        }
    }
    
    public static class Neo4jConnectionManager {
        private final String uri;
        private final String username;
        private final String password;
        
        public Neo4jConnectionManager(String uri, String username, String password) {
            this.uri = uri;
            this.username = username;
            this.password = password;
        }
        
        public Connection getConnection() throws SQLException {
            try {
                return DriverManager.getConnection(uri, username, password);
            } catch (SQLException e) {
                System.err.println("❌ Failed to get Neo4j connection: " + e.getMessage());
                throw e;
            }
        }
    }
}