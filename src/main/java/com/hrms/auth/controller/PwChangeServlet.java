package com.hrms.auth.controller;

import com.hrms.auth.service.AuthService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;
import java.util.Map;

@WebServlet("/auth/pw-change")
public class PwChangeServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String error = request.getParameter("error");
        Map<String, String> vd = authService.getPwChangeViewData(error);
        
        request.setAttribute("vd", vd);
        request.setAttribute("viewPage", "/WEB-INF/jsp/auth/pw-change.jsp");
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        String userId = (session != null) ? (String) session.getAttribute("userName") : null; 

        if (userId == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }

        String currentPw = request.getParameter("currentPw");
        String newPw = request.getParameter("newPw");
        String confirmPw = request.getParameter("confirmPw");

        // 1. 현재 비밀번호와 새 비밀번호가 동일한지 체크 (1차 차단)
        if (currentPw != null && currentPw.equals(newPw)) {
            response.sendRedirect(request.getContextPath() + "/auth/pw-change?error=same_password");
            return;
        }

        // 2. 서버 측 복합 유효성 검사 (보안 강화)
        boolean isValidPattern = newPw != null && newPw.length() >= 8 
                                && newPw.matches(".*[a-zA-Z].*") 
                                && newPw.matches(".*[0-9].*") 
                                && newPw.matches(".*[~!@#$%^&*()_+|<>?:{}].*");

        if (!isValidPattern) {
            response.sendRedirect(request.getContextPath() + "/auth/pw-change?error=weak_password");
            return;
        }

        // 3. 새 비밀번호와 확인용 비밀번호 일치 여부 체크
        if (!newPw.equals(confirmPw)) {
            response.sendRedirect(request.getContextPath() + "/auth/pw-change?error=mismatch");
            return;
        }

        // 4. 서비스 호출 (BCrypt를 통한 현재 비번 검증 및 최종 변경)
        boolean isSuccess = authService.changePassword(userId, currentPw, newPw);

        if (isSuccess) {
            session.invalidate();
            response.sendRedirect(request.getContextPath() + "/auth/login?msg=pw_success");
        } else {
            response.sendRedirect(request.getContextPath() + "/auth/pw-change?error=fail");
        }
    }
}