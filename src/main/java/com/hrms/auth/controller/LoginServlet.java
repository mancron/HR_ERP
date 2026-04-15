package com.hrms.auth.controller;

import com.hrms.auth.dao.AccountDAO;
import com.hrms.auth.dto.AccountDTO;
import com.hrms.auth.dto.LoginResultDTO;
import com.hrms.auth.service.AuthService;
import com.hrms.emp.dto.EmpDTO;
import com.hrms.emp.service.PendingApplyService;

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
        
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("empId") != null) {
            response.sendRedirect(request.getContextPath() + "/main");
            return;
        }

        String user = request.getParameter("username");
        String pass = request.getParameter("password");
        session = request.getSession();

        try {
            LoginResultDTO result = authService.login(user, pass);
            
            if (result != null && result.getAccount() != null) {
                AccountDTO account = result.getAccount();
                EmpDTO empInfo = result.getEmpInfo();
                String sEmpId = String.valueOf(account.getEmpId());

                if (loginUsers.containsKey(sEmpId)) {
                    loginUsers.remove(sEmpId);
                }

                session.setAttribute("empId", account.getEmpId());
                session.setAttribute("userName", account.getUsername());
                session.setAttribute("userRole", account.getRole());
                session.setAttribute("isManager", result.isManager());
                
                if (empInfo != null) {
                    session.setAttribute("loginUser", empInfo);
                    loginUsers.put(sEmpId, session.getId());
                    new PendingApplyService().processPending();
                }
                
                response.sendRedirect(request.getContextPath() + "/main");
            } else {
                throw new Exception("invalid_auth");
            }
            
        } catch (Exception e) {
            String errorCode = e.getMessage();
            if (errorCode == null) errorCode = "invalid_auth";
            
            boolean isKnownError = errorCode.equals("already_logged_in") || 
                                 errorCode.equals("account_locked") || 
                                 errorCode.equals("retired_user") || 
                                 errorCode.equals("invalid_user") || 
                                 errorCode.startsWith("login_fail");

            if (!isKnownError) errorCode = "invalid_auth";
            
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