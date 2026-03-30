package com.hrms.auth.controller;

import com.hrms.auth.dto.AccountDTO;
import com.hrms.auth.service.AuthService;
import com.hrms.emp.dto.EmployeeDTO;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;
import java.net.URLEncoder;

@WebServlet("/auth/login.do")
public class LoginServlet extends HttpServlet {
    private AuthService authService = new AuthService();

    // 1. 화면을 보여주는 GET 방식 추가 (필터가 튕겨내면 일로 들어온다)
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // WEB-INF 내부에 있는 실제 로그인 JSP 파일로 포워딩
        request.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(request, response);
    }

    // 2. 로그인을 처리하는 POST 방식
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String user = request.getParameter("username");
        String pass = request.getParameter("password");

        try {
            // [수정] authService.login이 이제 Exception을 던집니다.
            AccountDTO account = authService.login(user, pass);
            // 1. 로그인 성공 시 로직
            if (account != null) {
                HttpSession session = request.getSession();
                
                session.setAttribute("empId", account.getEmpId());
                session.setAttribute("userName", account.getUsername());
                session.setAttribute("userRole", account.getRole());

                // 사원 정보 상세 연동
                com.hrms.emp.dao.EmployeeDAO empDao = new com.hrms.emp.dao.EmployeeDAO();
                com.hrms.emp.dto.EmployeeDTO empInfo = empDao.getEmployeeById(account.getEmpId());
                session.setAttribute("loginUser", empInfo); 

                response.sendRedirect(request.getContextPath() + "/index.jsp");
            }

        } catch (Exception e) {
            String errorCode = e.getMessage(); // "locked" 혹은 "login_fail_1"
            String encodedUser = URLEncoder.encode(user, "UTF-8");
            
            // [중요] 주소창에 찍히는 실제 경로(/auth/login)로 보내야 합니다. 
            // 만약 이동 후에도 흰 창이라면 이 경로가 실제 로그인 폼 주소인지 확인하세요.
            response.sendRedirect(request.getContextPath() + "/auth/login?msg=" + errorCode + "&prevUser=" + encodedUser);
        }
    }
}