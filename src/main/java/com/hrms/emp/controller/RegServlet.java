package com.hrms.emp.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

import com.hrms.emp.dto.EmpDTO;
import com.hrms.emp.service.RegService;
import com.hrms.emp.service.TransferService;

@WebServlet("/emp/reg")
public class RegServlet extends HttpServlet {

    private RegService regService = new RegService();
    private TransferService transferService = new TransferService(); // 부서/직급 목록 재사용

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 세션 체크
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("empId") == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }
        String userRole = (String) session.getAttribute("userRole");

        //username 중복확인 AJAX
        String action = request.getParameter("action");
        if ("checkUsername".equals(action)) {
            String username = request.getParameter("username");
            boolean exists = regService.isUsernameExist(username);
            response.setContentType("application/json; charset=UTF-8");
            java.io.PrintWriter out = response.getWriter();
            out.print("{\"exists\":" + exists + "}");
            out.flush();
            return;
        }
        
        //이메일 중복확인 AJAX
        if ("checkEmail".equals(action)) {
            String email = request.getParameter("email");
            boolean exists = regService.isEmailExist(email);
            response.setContentType("application/json; charset=UTF-8");
            java.io.PrintWriter out = response.getWriter();
            out.print("{\"exists\":" + exists + "}");
            out.flush();
            return;
        }
        
        // 최종승인자/HR담당자만 접근 가능
        if (!"최종승인자".equals(userRole) && !"HR담당자".equals(userRole)) {
            response.setContentType("text/html; charset=UTF-8");
            java.io.PrintWriter out = response.getWriter();
            out.println("<script>");
            out.println("alert('접근 권한이 없습니다.');");
            out.println("history.back();");
            out.println("</script>");
            out.flush();
            return;
        }

        // 부서/직급 목록 조회
        List<EmpDTO> deptList     = transferService.getDeptList();
        List<EmpDTO> positionList = transferService.getPositionList();
        String nextEmpNo = regService.getNextEmpNo();
        request.setAttribute("nextEmpNo", nextEmpNo);

        request.setAttribute("deptList",     deptList);
        request.setAttribute("positionList", positionList);

        request.getRequestDispatcher("/WEB-INF/jsp/emp/reg.jsp")
               .forward(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        // 세션 체크
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("empId") == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }
        String userRole = (String) session.getAttribute("userRole");
        if (!"관리자".equals(userRole) && !"HR담당자".equals(userRole)) {
            response.sendRedirect(request.getContextPath() + "/emp/list");
            return;
        }

        // 파라미터 수신
        String username         = request.getParameter("username");
        String empName          = request.getParameter("emp_name");
        String deptId           = request.getParameter("dept_id");
        String positionId       = request.getParameter("position_id");
        String hireDate         = request.getParameter("hire_date");
        String empType          = request.getParameter("emp_type");
        String baseSalary       = request.getParameter("base_salary");
        String birthDate        = request.getParameter("birth_date");
        String gender           = request.getParameter("gender");
        String address          = request.getParameter("address");
        String emergencyContact = request.getParameter("emergency_contact");
        String bankAccount      = request.getParameter("bank_account");
        String email            = request.getParameter("email");
        String phone            = request.getParameter("phone");

        // username 중복 확인
        if (regService.isUsernameExist(username)) {
            response.setContentType("text/html; charset=UTF-8");
            java.io.PrintWriter out = response.getWriter();
            out.println("<script>");
            out.println("alert('이미 사용 중인 아이디입니다.');");
            out.println("history.back();");
            out.println("</script>");
            out.flush();
            return;
        }

        // EmpDTO 조립
        EmpDTO dto = new EmpDTO();
        dto.setEmp_name(empName);
        dto.setDept_id(Integer.parseInt(deptId));
        dto.setPosition_id(Integer.parseInt(positionId));
        dto.setHire_date(hireDate);
        dto.setEmp_type(empType);
        dto.setBase_salary(Integer.parseInt(baseSalary));
        dto.setBirth_date(birthDate);
        dto.setGender(gender);
        dto.setAddress(address);
        dto.setEmergency_contact(emergencyContact);
        dto.setBank_account(bankAccount);
        dto.setEmail(email);
        dto.setPhone(phone);

        // 등록 처리
        String tempPw = regService.registerEmployee(dto, username);

        if (tempPw != null) {
            response.setContentType("text/html; charset=UTF-8");
            java.io.PrintWriter out = response.getWriter();
            out.println("<script>");
            out.println("alert('직원 등록이 완료되었습니다.\\n임시 비밀번호: " + tempPw + "');");
            out.println("location.href='" + request.getContextPath() + "/emp/list';");
            out.println("</script>");
            out.flush();
        } else {
            response.setContentType("text/html; charset=UTF-8");
            java.io.PrintWriter out = response.getWriter();
            out.println("<script>");
            out.println("alert('직원 등록 중 오류가 발생했습니다.');");
            out.println("history.back();");
            out.println("</script>");
            out.flush();
        }
    }
}