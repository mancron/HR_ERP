package com.hrms.auth.controller;

import com.hrms.auth.dao.AccountDAO;
import com.hrms.auth.dto.AccountDTO;
import com.hrms.auth.service.AuthService;
import com.hrms.emp.dao.EmpDAO;
import com.hrms.emp.dto.EmpDTO;
import com.hrms.org.dao.DeptDAO;
import com.hrms.org.dao.PosDAO;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;
import java.net.URLEncoder;

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
            // AuthService에서 retired_user 체크를 포함한 검증 수행
            AccountDTO account = authService.login(user, pass);
            
            if (account != null) {
                HttpSession session = request.getSession();
                
                EmpDAO empDao = new EmpDAO();
                EmpDTO empInfo = empDao.getEmployeeById(account.getEmpId());
                
                if (empInfo != null) {
                    if (empInfo.getDept_name() == null || empInfo.getDept_name().isEmpty()) {
                        empInfo.setDept_name(new DeptDAO().getDeptNameById(empInfo.getDept_id()));
                    }
                    
                    if (empInfo.getPosition_name() == null || empInfo.getPosition_name().isEmpty()) {
                        empInfo.setPosition_name(new PosDAO().getPositionNameById(empInfo.getPosition_id()));
                    }

                    session.setAttribute("empId", account.getEmpId());
                    session.setAttribute("userName", account.getUsername());
                    session.setAttribute("userRole", account.getRole());
                    session.setAttribute("loginUser", empInfo);
                }

                response.sendRedirect(request.getContextPath() + "/main");
            } else {
                throw new Exception("invalid_user");
            }
            
        } catch (Exception e) {
            // "retired_user", "account_locked", "login_fail_N" 등의 에러 코드를 그대로 msg 파라미터로 전송
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