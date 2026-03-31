package com.hrms.common.listener;

import com.hrms.common.db.DatabaseConnection;
import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

@WebListener
public class AppLifecycleListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // 앱 시작 시점 로직 (현재는 비워둠)
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // 1. HikariCP 커넥션 풀 안전 종료
        DatabaseConnection.shutdown();

        // 2. 현재 웹앱 클래스로더에 등록된 JDBC 드라이버 등록 해제
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver.getClass().getClassLoader() == cl) {
                try {
                    DriverManager.deregisterDriver(driver);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        // 3. MySQL 좀비 스레드 강제 종료 (메모리 누수 원인 제거)
        AbandonedConnectionCleanupThread.checkedShutdown();
    }
}