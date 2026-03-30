package com.hrms.auth.controller;

import com.hrms.auth.dto.AccountDTO;
import com.hrms.auth.service.AuthService;
import com.hrms.emp.dao.EmpDAO;
import com.hrms.emp.dto.EmpDTO;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;
import java.net.URLEncoder;

@WebServlet("/auth/login.do")
public class LoginServlet extends HttpServlet {
    private AuthService authService = new AuthService();

    // 이 서블릿의 doGet은 사용하지 않거나, 혹시 모르니 LoginViewServlet으로 리다이렉트 처리
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/auth/login");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String user = request.getParameter("username");
        String pass = request.getParameter("password");

        try {
            AccountDTO account = authService.login(user, pass);
            if (account != null) {
                HttpSession session = request.getSession();
                session.setAttribute("empId", account.getEmpId());
                session.setAttribute("userName", account.getUsername());
                session.setAttribute("userRole", account.getRole());

                EmpDAO empDao = new EmpDAO();
                EmpDTO empInfo = empDao.getEmployeeById(account.getEmpId());
                session.setAttribute("loginUser", empInfo); 

                response.sendRedirect(request.getContextPath() + "/index.jsp");
            }
        } catch (Exception e) {
            String errorCode = e.getMessage(); // "login_fail_1" 또는 "locked"
            String encodedUser = URLEncoder.encode(user != null ? user : "", "UTF-8");
            
            // [중요] LoginViewServlet(/auth/login)으로 msg 파라미터를 담아 보냄
            response.sendRedirect(request.getContextPath() + "/auth/login?msg=" + errorCode + "&prevUser=" + encodedUser);
        }
    }
}