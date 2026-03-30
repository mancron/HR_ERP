package com.hrms.auth.controller;

import com.hrms.auth.dto.AccountDTO;
import com.hrms.auth.service.AuthService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;
import java.net.URLEncoder;

@WebServlet("/auth/login.do")
public class LoginServlet extends HttpServlet {
    private AuthService authService = new AuthService();

    // 1. 화면을 보여주는 GET 방식 추가 (필터가 튕겨내면 일로 들어온다)
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // WEB-INF 내부에 있는 실제 로그인 JSP 파일로 포워딩
        request.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(request, response);
    }

    // 2. 로그인을 처리하는 POST 방식
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String user = request.getParameter("username");
        String pass = request.getParameter("password");

        AccountDTO account = authService.login(user, pass);

        if (account != null) {
            HttpSession session = request.getSession();
            // 수정 요청한 세션 키값 적용
            session.setAttribute("userName", account.getUsername());
            session.setAttribute("userRole", account.getRole());

            response.sendRedirect(request.getContextPath() + "/main"); // 메인 경로 확인 필요
        } else {
            // 실패 시 경로를 서블릿 매핑 주소(.do)와 일치시킴
            String encodedUser = (user != null) ? URLEncoder.encode(user, "UTF-8") : "";
            response.sendRedirect(request.getContextPath() + "/auth/login.do?error=login_fail&prevUser=" + encodedUser);
        }
    }
}