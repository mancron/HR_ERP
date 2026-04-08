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
        
        //동일 브라우저 내 계정 전환 방지
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("empId") != null) {
            // 이미 세션 정보가 있다면 메인으로 돌려보냄
            response.sendRedirect(request.getContextPath() + "/main");
            return;
        }

        String user = request.getParameter("username");
        String pass = request.getParameter("password");
        session = request.getSession(); // 새로운 세션 획득

        try {
            LoginResultDTO result = authService.login(user, pass);
            
            if (result != null && result.getAccount() != null) {
                AccountDTO account = result.getAccount();
                EmpDTO empInfo = result.getEmpInfo();
                String sEmpId = String.valueOf(account.getEmpId());

                // [중복 로그인 원칙 적용]
                if (loginUsers.containsKey(sEmpId)) {
                    String existingId = loginUsers.get(sEmpId);
                    // 맵에 있는 세션ID가 현재 요청의 세션ID와 다를 때만(타 기기/브라우저) 차단
                    if (!existingId.equals(session.getId())) {
                        throw new Exception("already_logged_in");
                    }
                }

                // 세션 설정
                session.setAttribute("empId", account.getEmpId());
                session.setAttribute("userName", account.getUsername());
                session.setAttribute("userRole", account.getRole());
                session.setAttribute("isManager", result.isManager());
                
                if (empInfo != null) {
                    session.setAttribute("loginUser", empInfo);
                    // 맵에 현재 사번과 세션ID 등록
                    loginUsers.put(sEmpId, session.getId());
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