package com.hrms.emp.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDate;

import com.hrms.emp.dto.EmpDTO;
import com.hrms.emp.dto.ResignDTO;
import com.hrms.emp.service.EmpService;
import com.hrms.emp.service.LeaveService; // 부서장 조회는 LeaveService 재사용
import com.hrms.emp.service.ResignService;

@WebServlet("/emp/resign")
public class ResignServlet extends HttpServlet {

    private EmpService empService = new EmpService();
    private LeaveService leaveService = new LeaveService();
    private ResignService resignService = new ResignService();

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 세션 체크
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("empId") == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }
        Integer loginEmpId = (Integer) session.getAttribute("empId");

        // 파라미터 확인
        String empNo = request.getParameter("emp_no");
        if (empNo == null || empNo.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/emp/list");
            return;
        }

        // 직원 정보 조회
        EmpDTO empDetail = empService.getEmployeeDetail(empNo);
        if (empDetail == null) {
            response.sendRedirect(request.getContextPath() + "/emp/list");
            return;
        }

        // 본인 여부 확인 (URL 직접 접근 방어)
        if (!loginEmpId.equals(empDetail.getEmp_id())) {
            response.setContentType("text/html; charset=UTF-8");
            java.io.PrintWriter out = response.getWriter();
            out.println("<script>");
            out.println("alert('본인만 신청할 수 있습니다.');");
            out.println("window.top.location.href='" + request.getContextPath() + "/emp/list';");
            out.println("</script>");
            out.flush();
            return;
        }

        // 이미 퇴직한 직원 방어
        if ("퇴직".equals(empDetail.getStatus())) {
            response.setContentType("text/html; charset=UTF-8");
            java.io.PrintWriter out = response.getWriter();
            out.println("<script>");
            out.println("alert('이미 퇴직 처리된 직원입니다.');");
            out.println("history.back();");
            out.println("</script>");
            out.flush();
            return;
        }

        // 부서장 이름 조회 (LeaveService 재사용)
        String deptManagerName = leaveService.getDeptManagerName(empDetail.getDept_id());

        // 내일 날짜
        String tomorrow = LocalDate.now().plusDays(1).toString();

        request.setAttribute("empDetail", empDetail);
        request.setAttribute("deptManagerName", deptManagerName);
        request.setAttribute("tomorrow", tomorrow);

        request.getRequestDispatcher("/WEB-INF/jsp/emp/resign.jsp").forward(request, response);
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
        Integer loginEmpId = (Integer) session.getAttribute("empId");

        // 파라미터 수신
        String empIdStr    = request.getParameter("emp_id");
        String resignDate  = request.getParameter("resign_date");
        String reason      = request.getParameter("reason");

        // 본인 여부 확인
        int empId = Integer.parseInt(empIdStr);
        if (!loginEmpId.equals(empId)) {
            response.setContentType("text/html; charset=UTF-8");
            java.io.PrintWriter out = response.getWriter();
            out.println("<script>");
            out.println("alert('본인만 신청할 수 있습니다.');");
            out.println("history.back();");
            out.println("</script>");
            out.flush();
            return;
        }

        // ResignDTO 조립
        ResignDTO dto = new ResignDTO();
        dto.setEmp_id(empId);
        dto.setResign_date(resignDate);
        dto.setReason(reason);
        
        // 부서장 ID 조회 후 세팅
        int deptManagerId = resignService.getDeptManagerId(empId);
        dto.setDept_manager_id(deptManagerId);

        if (resignService.hasPendingResign(empId)) {
            response.setContentType("text/html; charset=UTF-8");
            java.io.PrintWriter out = response.getWriter();
            out.println("<script>");
            out.println("alert('이미 진행 중인 퇴직 신청이 있습니다.');");
            out.println("history.back();");
            out.println("</script>");
            out.flush();
            return;
        }
        //등록 처리
        boolean isSuccess = resignService.submitResign(dto);

        response.setContentType("text/html; charset=UTF-8");
        java.io.PrintWriter out = response.getWriter();
        out.println("<script>");
        if (isSuccess) {
            out.println("alert('퇴직 신청이 완료되었습니다.');");
            out.println("window.top.location.href='" + request.getContextPath() + "/emp/approval';");
        } else {
            out.println("alert('퇴직 신청 중 오류가 발생했습니다.');");
            out.println("history.back();");
        }
        out.println("</script>");
        out.flush();
    }
}