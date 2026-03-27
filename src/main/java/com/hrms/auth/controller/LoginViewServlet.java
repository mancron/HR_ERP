package com.hrms.auth.controller;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;

@WebServlet("/auth/login")
public class LoginViewServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // [중요] 질문자님이 말씀하신 실제 파일 위치로 경로 수정
        RequestDispatcher rd = request.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp");
        rd.forward(request, response);
    }
}