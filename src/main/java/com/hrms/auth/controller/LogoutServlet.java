package com.hrms.auth.controller;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;

// 유저님 요청대로 .do 제거
@WebServlet("/auth/logout")
public class LogoutServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 1. 세션 가져오기 (기존 세션이 없으면 null 반환)
        HttpSession session = request.getSession(false);
        
        if (session != null) {
            // 2. 세션 정보 완전히 삭제 (로그아웃의 핵심)
            session.invalidate();
        }
        
        // 3. 로그아웃 후 로그인 페이지로 리다이렉트
        // msg=logout 파라미터를 붙여서 로그인 창에서 알림을 띄우게 합니다.
        response.sendRedirect(request.getContextPath() + "/auth/login?msg=logout");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}