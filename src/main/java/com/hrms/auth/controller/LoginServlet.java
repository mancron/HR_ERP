package com.hrms.auth.controller;

import com.hrms.auth.dao.AccountDAO;
import com.hrms.auth.dto.AccountDTO;
import com.hrms.auth.service.AuthService;
import com.hrms.emp.dao.EmpDAO;
import com.hrms.emp.dto.EmpDTO;
import com.hrms.org.dao.DeptDAO; // 추가
import com.hrms.org.dao.PosDAO;  // 추가

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;
import java.net.URLEncoder;

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
        // 인코딩 설정 (한글 깨짐 방지)
        request.setCharacterEncoding("UTF-8");
        
        String user = request.getParameter("username");
        String pass = request.getParameter("password");

        try {
            // [수정] 서비스 계층에서 login_attempts 기반으로 검증 수행
            // 5회 이상 실패 시 "account_locked" 예외가 throw 됩니다.
            AccountDTO account = authService.login(user, pass);
            
            if (account != null) {
                HttpSession session = request.getSession();
                
                // 1. 사원 기본 정보 조회
                EmpDAO empDao = new EmpDAO();
                EmpDTO empInfo = empDao.getEmployeeById(account.getEmpId());
                
                if (empInfo != null) {
                    // 2. [데이터 보강] 부서명/직급명 방어 코드 (기존 로직 유지)
                    if (empInfo.getDept_name() == null || empInfo.getDept_name().isEmpty()) {
                        empInfo.setDept_name(new DeptDAO().getDeptNameById(empInfo.getDept_id()));
                    }
                    
                    if (empInfo.getPosition_name() == null || empInfo.getPosition_name().isEmpty()) {
                        empInfo.setPosition_name(new PosDAO().getPositionNameById(empInfo.getPosition_id()));
                    }

                    // 3. 세션 저장
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
            
            // [추가] DB에서 관리자 연락처 동적으로 가져오기
            AccountDAO accountDao = new AccountDAO();
            String adminPhone = accountDao.getAdminContact(); 
            
            String encodedUser = (user != null) ? java.net.URLEncoder.encode(user, "UTF-8") : "";
            String encodedPhone = java.net.URLEncoder.encode(adminPhone, "UTF-8");

            // msg와 함께 adminPhone 파라미터도 추가해서 보냅니다.
            response.sendRedirect(request.getContextPath() + 
                "/auth/login?msg=" + errorCode + 
                "&prevUser=" + encodedUser + 
                "&adminPhone=" + encodedPhone);
        }
    }
}