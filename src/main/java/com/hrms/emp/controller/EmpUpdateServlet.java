package com.hrms.emp.controller;

import java.io.IOException;

import com.hrms.emp.dto.EmpDTO;
import com.hrms.emp.service.EmpDetailService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;


@WebServlet("/emp/update")
public class EmpUpdateServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private EmpDetailService empDetailService = new EmpDetailService();
    
    
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

    	System.out.println("=== EmpUpdateServlet doPost 진입 ===");
        System.out.println("emp_no: " + request.getParameter("emp_no"));
        System.out.println("emp_id: " + request.getParameter("emp_id"));
    	
        request.setCharacterEncoding("UTF-8");

        // 세션 체크
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userRole") == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }
        String userRole = (String) session.getAttribute("userRole");

        // 파라미터 → DTO
        EmpDTO dto = new EmpDTO();
        dto.setEmp_id(Integer.parseInt(request.getParameter("emp_id")));
        dto.setEmp_no(request.getParameter("emp_no"));
        dto.setEmp_name(request.getParameter("emp_name"));
        dto.setGender(request.getParameter("gender"));
        dto.setBirth_date(request.getParameter("birth_date"));
        dto.setPhone(request.getParameter("phone"));
        dto.setEmail(request.getParameter("email"));
        dto.setEmergency_contact(request.getParameter("emergency_contact"));
        dto.setBank_account(request.getParameter("bank_account"));
        dto.setAddress(request.getParameter("address"));
        dto.setEmp_type(request.getParameter("emp_type"));

        // base_salary는 BigDecimal — null/빈값 방어
        String salaryParam = request.getParameter("base_salary");
        if (salaryParam != null && !salaryParam.trim().isEmpty()) {
            dto.setBase_salary(Integer.parseInt(salaryParam.trim()));
        }

        int result = empDetailService.updateEmployee(dto, userRole);

        // 저장 후 상세 페이지로 리다이렉트
        String redirectUrl = request.getContextPath()
                           + "/emp/detail?emp_no=" + dto.getEmp_no();
        if (result > 0) {
            response.sendRedirect(redirectUrl);
        } else {
            response.sendRedirect(redirectUrl + "&error=update_fail");
        }
    }
}