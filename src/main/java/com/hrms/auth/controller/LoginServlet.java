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
    
    // [유지] 중복 로그인 체크를 위한 메모리 맵
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
            // [팀장님 변경안 반영] LoginResultDTO로 결과 수신
            LoginResultDTO result = authService.login(user, pass);
            
            if (result != null && result.getAccount() != null) {
                AccountDTO account = result.getAccount();
                EmpDTO empInfo = result.getEmpInfo();
                String sEmpId = String.valueOf(account.getEmpId());

                // [우리 기능 유지] 중복 로그인 검증
                if (loginUsers.containsKey(sEmpId)) {
                    throw new Exception("already_logged_in");
                }

                // [팀장님 변경안 반영] 세션 정보 세팅
                session.setAttribute("empId", account.getEmpId());
                session.setAttribute("userName", account.getUsername());
                session.setAttribute("userRole", account.getRole());
                
                // [팀장님 변경안 반영] 부서장 권한 세팅 (isManager)
                session.setAttribute("isManager", result.isManager());
                
                if (empInfo != null) {
                    session.setAttribute("loginUser", empInfo);
                    
                    // [우리 기능 유지] 로그인 성공 시 중복 로그인 체크 맵에 등록
                    loginUsers.put(sEmpId, session.getId());
                }
                
                response.sendRedirect(request.getContextPath() + "/main");
            } else {
                throw new Exception("invalid_auth");
            }
            
        } catch (Exception e) {
            String errorCode = e.getMessage();
            if (errorCode == null) errorCode = "invalid_auth";
            
            // [우리 기능 유지] 퇴사자, 미존재 계정, 잠금 등 상세 에러 분기 유지
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