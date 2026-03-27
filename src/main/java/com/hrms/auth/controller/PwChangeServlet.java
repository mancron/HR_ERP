package com.hrms.auth.controller;

import com.hrms.auth.service.AuthService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;

// 1. 브라우저에서 접속할 주소: http://localhost:8081/hr_erp/auth/pw-change
@WebServlet("/auth/pw-change") 
public class PwChangeServlet extends HttpServlet {
    private AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 1. 중앙 회색 영역에 끼워 넣을 실제 JSP 파일의 경로를 속성(Attribute)에 담습니다.
        // 이 경로는 나중에 index.jsp에서 <jsp:include> 할 때 사용됩니다.
        request.setAttribute("viewPage", "/WEB-INF/jsp/auth/pw-change.jsp");
        
        // 2. 브라우저 주소창은 /auth/pw-change를 유지하면서, 
        // 화면 전체 레이아웃을 담당하는 /index.jsp로 제어권을 넘깁니다(Forward).
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        // 로그인 서블릿에서 저장한 이름과 정확히 일치해야 합니다. (userName 혹은 userId)
        String userId = (String) session.getAttribute("userName"); 

        if (userId == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }

        String currentPw = request.getParameter("currentPw");
        String newPw = request.getParameter("newPw");
        String confirmPw = request.getParameter("confirmPw");

        // 1. 새 비밀번호 확인 일치 검사
        if (newPw == null || !newPw.equals(confirmPw)) {
            response.sendRedirect(request.getContextPath() + "/auth/pw-change?error=mismatch");
            return;
        }

        // 2. 서비스 호출
        boolean isSuccess = authService.changePassword(userId, currentPw, newPw);

        if (isSuccess) {
            // 성공 시 세션을 날리고 로그인 페이지로 보내면서 성공 메시지 전달
            session.invalidate();
            response.sendRedirect(request.getContextPath() + "/auth/login?msg=pw_success");
        } else {
            // 실패 시 (현재 비밀번호 틀림 등) 다시 변경 페이지로 에러 코드 전달
            response.sendRedirect(request.getContextPath() + "/auth/pw-change?error=fail");
        }
    }
}