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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@WebServlet("/auth/login.do")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private AuthService authService = new AuthService();
    private static final Map<String, String> loginUsers = new ConcurrentHashMap<>();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/auth/login");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        
        String user = request.getParameter("username");
        String pass = request.getParameter("password");
        HttpSession session = request.getSession();

        try {
            AccountDTO account = authService.login(user, pass);
            
            if (account != null) {
                String sEmpId = String.valueOf(account.getEmpId());

                if (loginUsers.containsKey(sEmpId)) {
                    throw new Exception("already_logged_in");
                }

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

                    loginUsers.put(sEmpId, session.getId());
                }
                response.sendRedirect(request.getContextPath() + "/main");
            } else {
                throw new Exception("invalid_auth");
            }
            
        } catch (Exception e) {
            String errorCode = e.getMessage();
            
            if (errorCode == null) errorCode = "invalid_auth";
            
            // [체크!] 퇴사자(retired_user)와 미존재계정(invalid_user)을 허용 리스트에 유지
            boolean isKnownError = errorCode.equals("already_logged_in") || 
                                 errorCode.equals("account_locked") || 
                                 errorCode.equals("retired_user") || 
                                 errorCode.equals("invalid_user") || 
                                 errorCode.startsWith("login_fail");

            if (!isKnownError) {
                errorCode = "invalid_auth";
            }
            
            AccountDAO accountDao = new AccountDAO();
            session.setAttribute("loginErrorMsg", errorCode);
            session.setAttribute("prevUser", user);
            session.setAttribute("adminPhone", accountDao.getAdminContact());

            response.sendRedirect(request.getContextPath() + "/auth/login");
        }
    }

    public static Map<String, String> getLoginUsers() {
        return loginUsers;
    }
}