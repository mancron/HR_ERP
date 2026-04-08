package com.hrms.common.listener;

import com.hrms.auth.controller.LoginServlet;

import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

/**
 * 세션 만료 / 로그아웃 시 loginUsers 맵에서 해당 사용자 자동 제거
 * - 중복 로그인 차단 정확도 유지
 * - loginUsers 맵 메모리 누수 방지
 */
@WebListener
public class SessionCleanupListener implements HttpSessionListener {

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        // 세션 생성 시점에는 처리 불필요
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        Object empIdObj = session.getAttribute("empId");

        if (empIdObj != null) {
            String sEmpId = String.valueOf(empIdObj);
            // loginUsers 맵에서 해당 사번 제거 → 재로그인 허용
            LoginServlet.getLoginUsers().remove(sEmpId);
        }
    }
}