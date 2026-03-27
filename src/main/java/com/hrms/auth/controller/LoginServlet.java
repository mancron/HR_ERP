package com.hrms.auth.controller;

import com.hrms.auth.dto.AccountDTO;
import com.hrms.auth.service.AuthService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;
import java.net.URLEncoder; // 인코딩용 추가

@WebServlet("/auth/login.do")
public class LoginServlet extends HttpServlet {
    private AuthService authService = new AuthService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String user = request.getParameter("username");
        String pass = request.getParameter("password");

        AccountDTO account = authService.login(user, pass);

        if (account != null) {
            HttpSession session = request.getSession();
            session.setAttribute("userName", account.getUsername());
            session.setAttribute("userRole", account.getRole());

            response.sendRedirect(request.getContextPath() + "/index.jsp");
        } else {
            // 실패 시 입력했던 username을 파라미터로 전달 (한글/특수문자 대비 인코딩)
            String encodedUser = URLEncoder.encode(user, "UTF-8");
            response.sendRedirect(request.getContextPath() + "/auth/login?error=login_fail&prevUser=" + encodedUser);
        }
    }
}