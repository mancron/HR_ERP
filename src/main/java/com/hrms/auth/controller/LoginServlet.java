package com.hrms.auth.controller;

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
        String user = request.getParameter("username");
        String pass = request.getParameter("password");

        try {
            AccountDTO account = authService.login(user, pass);
            
            if (account != null) {
                HttpSession session = request.getSession();
                
                // 1. 사원 기본 정보 조회
                EmpDAO empDao = new EmpDAO();
                EmpDTO empInfo = empDao.getEmployeeById(account.getEmpId());
                
                if (empInfo != null) {
                    // 2. [데이터 보강] 부서명과 직급명이 없다면 DAO를 통해 채워줌
                    // (EmpDAO의 JOIN 쿼리가 완벽하지 않을 경우를 대비한 방어 코드)
                    if (empInfo.getDept_name() == null || empInfo.getDept_name().isEmpty()) {
                        DeptDAO deptDao = new DeptDAO();
                        empInfo.setDept_name(deptDao.getDeptNameById(empInfo.getDept_id()));
                    }
                    
                    if (empInfo.getPosition_name() == null || empInfo.getPosition_name().isEmpty()) {
                        PosDAO posDao = new PosDAO();
                        empInfo.setPosition_name(posDao.getPositionNameById(empInfo.getPosition_id()));
                    }

                    // 3. 세션 저장 (JSP에서 EL 태그로 바로 사용 가능)
                    session.setAttribute("empId", account.getEmpId());
                    session.setAttribute("userName", account.getUsername());
                    session.setAttribute("userRole", account.getRole());
                    session.setAttribute("loginUser", empInfo); // 모든 정보가 담긴 DTO
                }

                response.sendRedirect(request.getContextPath() + "/index.jsp");
            } else {
                throw new Exception("invalid_user");
            }
            
        } catch (Exception e) {
            String errorCode = e.getMessage();
            String encodedUser = (user != null) ? URLEncoder.encode(user, "UTF-8") : "";
            response.sendRedirect(request.getContextPath() + "/auth/login?msg=" + errorCode + "&prevUser=" + encodedUser);
        }
    }
}