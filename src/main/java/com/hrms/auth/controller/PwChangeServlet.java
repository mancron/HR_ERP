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
        // 1. URL 파라미터에서 에러 코드 확인
        String error = request.getParameter("error");
        
        // 2. [스크립틀릿 제거를 위한 핵심] 서비스로부터 뷰 데이터를 받아옴
        Map<String, String> vd = authService.getPwChangeViewData(error);
        
        // 3. JSP에서 사용할 수 있도록 속성(Attribute)에 저장
        request.setAttribute("vd", vd);
        
        // 4. 중앙 영역에 끼워 넣을 JSP 경로 설정
        request.setAttribute("viewPage", "/WEB-INF/jsp/auth/pw-change.jsp");
        
        // 5. 레이아웃 페이지(index.jsp)로 포워드
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        // 로그인 시 세션에 저장한 ID 키값 확인 (userName 사용 중)
        String userId = (session != null) ? (String) session.getAttribute("userName") : null; 

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

        // 2. 서비스 호출 (비밀번호 변경 로직 수행)
        boolean isSuccess = authService.changePassword(userId, currentPw, newPw);

        if (isSuccess) {
            // 성공 시 세션을 만료시키고 로그인 페이지로 이동 (성공 메시지 포함)
            session.invalidate();
            response.sendRedirect(request.getContextPath() + "/auth/login?msg=pw_success");
        } else {
            // 실패 시 에러 코드를 들고 다시 변경 페이지로 리다이렉트
            response.sendRedirect(request.getContextPath() + "/auth/pw-change?error=fail");
        }
    }
}