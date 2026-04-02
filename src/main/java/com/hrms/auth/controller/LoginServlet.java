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
                response.sendRedirect(request.getContextPath() + "/index.jsp");
            } else {
                throw new Exception("invalid_user");
            }
            
        } catch (Exception e) {
            String errorCode = e.getMessage();
            
            AccountDAO accountDao = new AccountDAO();
            String adminPhone = accountDao.getAdminContact(); 
            
            // [수정] URL 파라미터 대신 세션을 사용하여 데이터 전달
            HttpSession session = request.getSession();
            session.setAttribute("loginMsg", errorCode);
            session.setAttribute("prevUser", user);
            session.setAttribute("adminPhone", adminPhone);

            // 주소창에는 파라미터가 남지 않음
            response.sendRedirect(request.getContextPath() + "/auth/login");
        }
    }
}