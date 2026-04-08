package com.hrms.auth.controller;

import com.hrms.auth.service.AuthService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;
import java.util.Map;

@WebServlet("/auth/login")
public class LoginViewServlet extends HttpServlet {
    private AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
        // [추가] 이미 로그인된 세션이 있는지 확인 (우회 방지)
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("empId") != null) {
            // 이미 로그인 상태라면 로그인 폼 대신 메인 페이지로 보냄
            response.sendRedirect(request.getContextPath() + "/main");
            return;
        }

        // 1. URL 파라미터에서 msg(에러코드) 가져오기
        String msg = request.getParameter("msg");
        
        // 2. AuthService를 통해 데이터 생성
        Map<String, String> vd = authService.getLoginViewData(msg);
        
        // 3. 데이터 담기
        request.setAttribute("vd", vd);
        
        // 4. JSP로 포워드
        request.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(request, response);
    }
}