package com.hrms.common.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {
    private static HikariDataSource dataSource;
    private static HikariDataSource readOnlyDataSource; // AI 조회용 읽기 전용 풀

    static {
        try {
            // .env 로드
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing() 
                    .load();

            String dbUrl              = dotenv.get("DB_URL");
            String dbUser             = dotenv.get("DB_USER");
            String dbPassword         = dotenv.get("DB_PASSWORD");
            
            // 읽기 전용 계정 정보 로드
            String dbReadOnlyUser     = dotenv.get("DB_READONLY_USER");
            String dbReadOnlyPassword = dotenv.get("DB_READONLY_PASSWORD");

            // ─── 1. 메인 데이터소스 (읽기/쓰기) ───
            HikariConfig config = new HikariConfig();
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setJdbcUrl(dbUrl);
            config.setUsername(dbUser);
            config.setPassword(dbPassword);

            config.setMaximumPoolSize(10);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);

            config.addDataSourceProperty("cachePrepStmts",       "true");
            config.addDataSourceProperty("prepStmtCacheSize",    "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit","2048");
            
            // 다중 쿼리 실행 방지 (SQL Injection 방어)
            config.addDataSourceProperty("allowMultiQueries", "false"); 

            dataSource = new HikariDataSource(config);

            // ─── 2. AI 전용 데이터소스 (읽기 전용) ───
            if (dbReadOnlyUser != null && !dbReadOnlyUser.trim().isEmpty()) {
                HikariConfig roConfig = new HikariConfig();
                roConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
                roConfig.setJdbcUrl(dbUrl);
                roConfig.setUsername(dbReadOnlyUser);
                roConfig.setPassword(dbReadOnlyPassword);

                // AI 전용이므로 풀 사이즈를 작게 할당
                roConfig.setMaximumPoolSize(5);
                roConfig.setConnectionTimeout(30000);
                
                // 핵심: JDBC 레벨에서 읽기 전용 속성 강제
                roConfig.setReadOnly(true); 
                
                // 핵심: 다중 쿼리 원천 차단 (DROP, DELETE 등 방지)
                roConfig.addDataSourceProperty("allowMultiQueries", "false");

                readOnlyDataSource = new HikariDataSource(roConfig);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("HikariCP 설정 오류 발생");
        }
    }

    // 기존 메인 커넥션 (CUD 작업 및 일반 조회용)
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // AI Text-to-SQL 전용 커넥션 (SELECT만 가능)
    public static Connection getReadOnlyConnection() throws SQLException {
        if (readOnlyDataSource == null) {
            throw new SQLException("읽기 전용 데이터소스가 설정되지 않았습니다. .env 파일을 확인하세요.");
        }
        return readOnlyDataSource.getConnection();
    }

    private DatabaseConnection() {}
    
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        if (readOnlyDataSource != null && !readOnlyDataSource.isClosed()) {
            readOnlyDataSource.close();
        }
    }
}