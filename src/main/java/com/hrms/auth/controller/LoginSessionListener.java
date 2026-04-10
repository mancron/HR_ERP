package com.hrms.auth.controller;

import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import java.util.Map;

@WebListener
public class LoginSessionListener implements HttpSessionListener {

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        String currentSessionId = session.getId();
        Object empIdObj = session.getAttribute("empId");

        if (empIdObj != null) {
            String sEmpId = String.valueOf(empIdObj);
            
            // 1. 현재 로그인 유저 맵을 가져옴
            Map<String, String> loginUsers = LoginServlet.getLoginUsers();
            
            // 2. 맵에 저장된 세션 ID와 지금 만료되는 세션 ID가 일치하는지 확인
            String validSessionId = loginUsers.get(sEmpId);
            
            System.out.println("========================================");
            System.out.println("[Listener] 세션 만료 감지: " + sEmpId);
            System.out.println("[Listener] 만료되는 ID: " + currentSessionId);
            System.out.println("[Listener] 맵에 등록된 ID: " + validSessionId);

            if (currentSessionId.equals(validSessionId)) {
                // 일치할 때만 맵에서 제거 (정상 로그아웃 또는 타임아웃)
                loginUsers.remove(sEmpId);
                System.out.println("[Listener] 결과: 최신 세션이므로 맵에서 제거 완료");
            } else {
                // 일치하지 않는다면 이미 다른 곳에서 로그인하여 맵이 갱신된 상태임
                System.out.println("[Listener] 결과: 이전 세션이므로 맵을 건드리지 않음 (유지)");
            }
            System.out.println("========================================");
        }
    }
}