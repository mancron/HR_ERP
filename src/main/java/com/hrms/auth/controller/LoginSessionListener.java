package com.hrms.auth.controller;

import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import java.util.Map;

@WebListener
public class LoginSessionListener implements HttpSessionListener {
    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        // [추가] 리스너 호출 여부를 무조건 찍어봄
        System.out.println("========================================");
        System.out.println("[Listener] 세션 만료 감지됨!");
        
        Object empIdObj = se.getSession().getAttribute("empId");
        if (empIdObj != null) {
            String sEmpId = String.valueOf(empIdObj);
            LoginServlet.getLoginUsers().remove(sEmpId);
            System.out.println("[Listener] 맵에서 제거 완료: " + sEmpId);
        }
        System.out.println("========================================");
    }
}