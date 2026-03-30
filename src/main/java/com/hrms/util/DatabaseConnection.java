package com.hrms.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {
    private static HikariDataSource dataSource;

    static {
        try {
            HikariConfig config = new HikariConfig();
            
            // 필수 설정 (기존에 쓰던 mydb 정보 적용)
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setJdbcUrl("jdbc:mysql://localhost:3306/hr_erp?characterEncoding=UTF-8&serverTimezone=UTC");
            config.setUsername("root");
            config.setPassword("1234");

            // 성능 최적화 옵션 (시니어 권장 설정)
            config.setMaximumPoolSize(10);         // 최대 커넥션 개수
            config.setConnectionTimeout(30000);    // 연결 대기 시간 (30초)
            config.setIdleTimeout(600000);         // 유휴 커넥션 유지 시간 (10분)
            config.setMaxLifetime(1800000);        // 커넥션 최대 수명 (30분)
            
            // 캐시 설정 (MySQL 성능 향상)
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("HikariCP 설정 오류 발생");
        }
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private DatabaseConnection() {} // 인스턴스화 방지
    
    
}