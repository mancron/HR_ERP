package com.hrms.auth.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

/**
 * 로그아웃 서블릿.
 * 세션 완전 파기 + 쿠키 무효화 + PRG 패턴으로 로그인 리다이렉트.
 */
@WebServlet("/auth/logout")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 1. 세션 무효화 (null 체크 필수)
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate(); // 세션 내 모든 attribute 제거 + 세션 파기
        }

        // 2. JSESSIONID 쿠키 즉시 만료 처리
        Cookie cookie = new Cookie("JSESSIONID", "");
        cookie.setMaxAge(0);
        cookie.setPath(req.getContextPath().isEmpty() ? "/" : req.getContextPath());
        cookie.setHttpOnly(true);  // JS 접근 차단
        cookie.setSecure(false);   // HTTPS 환경이면 true로 변경
        resp.addCookie(cookie);

        // 3. PRG 패턴: 로그인 페이지로 리다이렉트
        resp.sendRedirect(req.getContextPath() + "/auth/login");
    }
}