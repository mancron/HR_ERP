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
import com.hrms.emp.dto.LeaveDTO;
import com.hrms.emp.service.EmpService;
import com.hrms.emp.service.LeaveService;

@WebServlet("/emp/leave")
public class LeaveServlet extends HttpServlet {

    private EmpService empService = new EmpService();
    private LeaveService leaveService = new LeaveService();

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

        // 본인 여부 확인 (URL 직접 접근 방어)
        EmpDTO empDetail = empService.getEmployeeDetail(empNo);
        if (empDetail == null) {
            response.sendRedirect(request.getContextPath() + "/emp/list");
            return;
        }
        if (loginEmpId != empDetail.getEmp_id()) {
            response.setContentType("text/html; charset=UTF-8");
            java.io.PrintWriter out = response.getWriter();
            out.println("<script>");
            out.println("alert('본인만 신청할 수 있습니다.');");
            out.println("window.top.location.href='" + request.getContextPath() + "/emp/list';");
            out.println("</script>");
            out.flush();
            return;
        }
        
        
        // 부서장 이름 조회 (department.manager_id → emp_name)
        String deptManagerName = leaveService.getDeptManagerName(empDetail.getDept_id());

        // 내일 날짜
        String tomorrow = LocalDate.now().plusDays(1).toString();

        request.setAttribute("empDetail", empDetail);
        request.setAttribute("deptManagerName", deptManagerName);
        request.setAttribute("tomorrow", tomorrow);

        String mode = request.getParameter("mode");
        String idStr = request.getParameter("id");

        if ("edit".equals(mode) && idStr != null) {
            int requestId = Integer.parseInt(idStr);
            LeaveDTO existing = leaveService.getLeaveById(requestId, loginEmpId);
            if (existing == null || !"대기".equals(existing.getStatus())) {
                response.sendRedirect(request.getContextPath() + "/emp/approval");
                return;
            }
            request.setAttribute("existing", existing);
            request.setAttribute("mode", "edit");
        }
        
        request.getRequestDispatcher("/WEB-INF/jsp/emp/leave.jsp").forward(request, response);
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

        // 파라미터 → LeaveDTO
        LeaveDTO dto = new LeaveDTO();
        dto.setEmp_id(Integer.parseInt(request.getParameter("emp_id")));
        dto.setLeave_type(request.getParameter("leave_type"));
        dto.setStart_date(request.getParameter("start_date"));

        // 복직이면 end_date는 null로 처리
        String endDate = request.getParameter("end_date");
        dto.setEnd_date((endDate != null && !endDate.trim().isEmpty()) ? endDate : null);

        dto.setReason(request.getParameter("reason"));
        dto.setStatus("대기"); // 초기 상태 고정

        // 부서장 ID 조회 후 세팅
        String empNo = request.getParameter("emp_no");
        int deptManagerId = leaveService.getDeptManagerId(dto.getEmp_id());
        dto.setDept_manager_id(deptManagerId);

        // 본인 여부 재확인 (POST 직접 요청 방어)
        if (!loginEmpId.equals(dto.getEmp_id())) {
            response.sendRedirect(request.getContextPath() + "/emp/list");
            return;
        }

        //수정모드 처리
        String mode = request.getParameter("mode");
        String idStr = request.getParameter("request_id");

        if ("edit".equals(mode) && idStr != null) {
            dto.setRequest_id(Integer.parseInt(idStr));
            boolean isSuccess = leaveService.updateLeaveRequest(dto);
            response.setContentType("text/html; charset=UTF-8");
            java.io.PrintWriter out = response.getWriter();
            out.println("<script>");
            out.println("alert('" + (isSuccess ? "수정이 완료되었습니다." : "수정 중 오류가 발생했습니다.") + "');");
            out.println("if (window.parent && window.parent !== window) {");
            out.println("    // iframe 안에서 실행된 경우 — iframe src 초기화 후 부모창 새로고침");
            out.println("    window.parent.document.getElementById('approvalModalIframe').src = '';");
            out.println("    window.parent.document.getElementById('approvalDetailModal').classList.remove('active');");
            out.println("    window.parent.location.reload();");
            out.println("} else {");
            out.println("    location.href='" + request.getContextPath() + "/emp/approval';");
            out.println("}");
            out.println("</script>");
            out.flush();
            return;
        }
        
        if (leaveService.hasPendingLeave(dto.getEmp_id())) {
            response.setContentType("text/html; charset=UTF-8");
            java.io.PrintWriter out = response.getWriter();
            out.println("<script>");
            out.println("alert('이미 진행 중인 휴직/복직 신청이 있습니다.');");
            out.println("history.back();");
            out.println("</script>");
            out.flush();
            return;
        }
        boolean isSuccess = leaveService.insertLeaveRequest(dto);

        if (isSuccess) {
            // 신청 완료 후 상세 페이지로 이동
            response.setContentType("text/html; charset=UTF-8");
            java.io.PrintWriter out = response.getWriter();
            out.println("<script>");
            out.println("alert('신청이 완료되었습니다.');");
            out.println("location.href='" + request.getContextPath() + "/emp/detail?emp_no=" + empNo + "';");
            out.println("</script>");
            out.flush();
        } else {
            response.setContentType("text/html; charset=UTF-8");
            java.io.PrintWriter out = response.getWriter();
            out.println("<script>");
            out.println("alert('신청 중 오류가 발생했습니다. 다시 시도해주세요.');");
            out.println("history.back();");
            out.println("</script>");
            out.flush();
        }
    }
    
    
}