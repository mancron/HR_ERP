package com.hrms.auth.controller;

import com.hrms.auth.service.AuthService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;
import java.util.Map;

@WebServlet("/auth/login") // 브라우저에서 /auth/login 으로 접속할 때 실행됨
public class LoginViewServlet extends HttpServlet {
    private AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 1. URL 파라미터에서 msg(에러코드) 가져오기
        String msg = request.getParameter("msg");
        
        // 2. AuthService를 통해 뷰에 필요한 데이터(실패횟수, 연락처 등) 생성
        // 이vd 변수가 JSP의 ${vd.xxx} 와 연결됩니다.
        Map<String, String> vd = authService.getLoginViewData(msg);
        
        // 3. request 객체에 데이터 담기
        request.setAttribute("vd", vd);
        
        // 4. 깨끗하게 만든 login.jsp로 이동
        request.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(request, response);
    }
}