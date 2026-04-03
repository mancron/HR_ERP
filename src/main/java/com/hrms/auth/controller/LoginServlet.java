package com.hrms.auth.controller;

import com.hrms.auth.dao.AccountDAO;
import com.hrms.auth.dto.AccountDTO;
import com.hrms.auth.dto.LoginResultDTO;
import com.hrms.auth.service.AuthService;
import com.hrms.emp.dto.EmpDTO;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;

@WebServlet("/auth/login.do")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/auth/login");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        
        String user = request.getParameter("username");
        String pass = request.getParameter("password");

        try {
            // 1. AuthService에서 조립된 최종 결과 객체 수신
            LoginResultDTO result = authService.login(user, pass);
            
            if (result != null && result.getAccount() != null) {
                HttpSession session = request.getSession();
                
                AccountDTO account = result.getAccount();
                EmpDTO empInfo = result.getEmpInfo();
                
                // 2. 세션에 기본 정보 세팅
                session.setAttribute("empId", account.getEmpId());
                session.setAttribute("userName", account.getUsername());
                session.setAttribute("userRole", account.getRole());
                
                if (empInfo != null) {
                    session.setAttribute("loginUser", empInfo);
                }

                // 3. [핵심] 필터와 JSP UI에서 사용할 부서장 권한 세팅
                session.setAttribute("isManager", result.isManager());

                // 로그인 성공 시 메인 화면 이동
                response.sendRedirect(request.getContextPath() + "/main");
            } else {
                throw new Exception("invalid_user");
            }
            
        } catch (Exception e) {
            // 실패 처리 로직 (기존과 동일)
            String errorCode = e.getMessage();
            
            AccountDAO accountDao = new AccountDAO();
            String adminPhone = accountDao.getAdminContact(); 
            
            String encodedUser = (user != null) ? java.net.URLEncoder.encode(user, "UTF-8") : "";
            String encodedPhone = java.net.URLEncoder.encode(adminPhone, "UTF-8");

            response.sendRedirect(request.getContextPath() + 
                "/auth/login?msg=" + errorCode + 
                "&prevUser=" + encodedUser + 
                "&adminPhone=" + encodedPhone);
        }
    }
}