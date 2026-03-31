package com.hrms.common.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {
    private static HikariDataSource dataSource;

    static {
        try {
            // .env 로드 (못 찾아도 예외 안 던짐)
        	Dotenv dotenv = Dotenv.configure()
        	        .ignoreIfMissing() 
        	        .load();

        	String dbUrl      = dotenv.get("DB_URL");
        	String dbUser     = dotenv.get("DB_USER");
        	String dbPassword = dotenv.get("DB_PASSWORD");
            HikariConfig config = new HikariConfig();
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setJdbcUrl(dbUrl);
            config.setUsername(dbUser);
            config.setPassword(dbPassword);

            config.setMaximumPoolSize(10);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);

            config.addDataSourceProperty("cachePrepStmts",          "true");
            config.addDataSourceProperty("prepStmtCacheSize",        "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit",    "2048");

            dataSource = new HikariDataSource(config);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("HikariCP 설정 오류 발생");
        }
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private DatabaseConnection() {}
}