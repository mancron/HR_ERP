package com.hrms.emp.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import com.hrms.emp.dto.EmpDTO;
import com.hrms.emp.dto.HistoryDTO;
import com.hrms.emp.service.EmpService;
import com.hrms.emp.service.TransferService;


@WebServlet("/emp/transfer")
public class TransferServlet extends HttpServlet {
    
    // static 호출 대신 객체를 생성해서 사용합니다.
    private EmpService empService = new EmpService();
    private TransferService transferService = new TransferService();

    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String empNo = request.getParameter("emp_no");
        if (empNo == null || empNo.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/emp/list");
            return;
        }

        // 객체 변수명(empService)으로 호출하여 상세 정보를 가져옵니다.
        EmpDTO empDetail = empService.getEmployeeDetail(empNo);
        if (empDetail == null) {
            response.sendRedirect(request.getContextPath() + "/emp/list");
            return;
        }
        
        // 부서/직급 목록 조회 (TransferService에 추가할 메서드)
        List<EmpDTO> deptList = transferService.getDeptList();
        List<EmpDTO> positionList = transferService.getPositionList();
        String tomorrow = LocalDate.now().plusDays(1).toString();

        // 직책 추가
        boolean isCurrentManager = transferService.isDeptManager(empDetail.getEmp_id());
        
        request.setAttribute("isCurrentManager", isCurrentManager);
        request.setAttribute("empDetail", empDetail);
        request.setAttribute("deptList", deptList);
        request.setAttribute("positionList", positionList);
        request.setAttribute("tomorrow", tomorrow);
        
        // 발령 페이지로 이동
        request.getRequestDispatcher("/WEB-INF/jsp/emp/transfer.jsp").forward(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
    	request.setCharacterEncoding("UTF-8");
    	
    	HttpSession session = request.getSession(false);
    	if (session == null || session.getAttribute("empId") == null) {
    	    response.sendRedirect(request.getContextPath() + "/auth/login");
    	    return;
    	}
        Integer approvedBy = (Integer) session.getAttribute("empId");
    	
        String empNo = request.getParameter("emp_no");
        String targetRole = request.getParameter("target_role");
        
        HistoryDTO dto = new HistoryDTO();
        dto.setEmp_id(Integer.parseInt(request.getParameter("emp_id")));
        dto.setChange_type(request.getParameter("transfer_type"));
        dto.setFrom_dept_id(Integer.parseInt(request.getParameter("prev_dept_id")));
        dto.setTo_dept_id(Integer.parseInt(request.getParameter("target_dept")));
        dto.setFrom_position_id(Integer.parseInt(request.getParameter("prev_position_id")));
        dto.setTo_position_id(Integer.parseInt(request.getParameter("target_position")));
        dto.setReason(request.getParameter("reason"));
        dto.setApproved_by(approvedBy != null ? approvedBy : 0);
        dto.setChange_date(LocalDate.parse(request.getParameter("transfer_date")).atStartOfDay());
        

        // 3. TransferService를 통해 트랜잭션 실행
        boolean isSuccess = transferService.executeTransfer(empNo, dto, targetRole);

        if(isSuccess) {
            // 성공 시 해당 사원의 상세 페이지로 이동
            //response.sendRedirect(request.getContextPath() + "/emp/detail?emp_no=" + empNo); //직원 상세 창으로 돌아가기
        	//발령 처리를 하자마자 창꺼지고 새로고침
            response.setContentType("text/html; charset=UTF-8");
            java.io.PrintWriter out = response.getWriter();
            out.println("<script>");
            out.println("window.top.location.reload();"); // 부모 창(목록) 새로고침
            out.println("</script>");
            out.flush();
        } else {
            // 실패 시 메시지와 함께 뒤로 가기 등 처리
            response.sendRedirect(request.getContextPath() + "/emp/transfer?emp_no=" + empNo + "&error=1");
        }
    }
}
