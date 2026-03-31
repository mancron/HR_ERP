package com.hrms.emp.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import com.hrms.emp.dto.EmpDTO;
import com.hrms.emp.dto.TransferDTO;
import com.hrms.emp.service.EmpService;
import com.hrms.org.dto.DeptDTO;
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
        List<DeptDTO> deptList = transferService.getDeptList();
        List<PositionDTO> positionList = transferService.getPositionList();
        
        request.setAttribute("empDetail", empDetail);
        request.setAttribute("deptList", deptList);
        request.setAttribute("positionList", positionList);
        request.setAttribute("tomorrow", tomorrow.toString()); // "2026-04-01" 형식
        
        // 발령 페이지로 이동
        request.getRequestDispatcher("/WEB-INF/jsp/emp/transfer.jsp").forward(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
    	
    	
        // 1. 폼 데이터 받기
        String empNo = request.getParameter("emp_no");
        String transferType = request.getParameter("transfer_type");
        String targetDeptId = request.getParameter("target_dept");      // JSP의 value가 ID라고 가정
        String targetPosId = request.getParameter("target_position");  // JSP의 value가 ID라고 가정
        String transferDate = request.getParameter("transfer_date");
        String reason = request.getParameter("reason");
        
        // 이전 정보 (hidden 혹은 조회 결과에서 가져옴)
        String prevDeptId = request.getParameter("prev_dept_id");
        String prevPosId = request.getParameter("prev_position_id");

        // 2. DTO 객체 생성 및 데이터 담기
        TransferDTO dto = new TransferDTO();
        dto.setEmp_no(empNo);
        dto.setTransfer_type(transferType);
        dto.setTransfer_date(transferDate);
        dto.setReason(reason);
        dto.setTarget_dept_id(Integer.parseInt(targetDeptId));
        dto.setTarget_position_id(Integer.parseInt(targetPosId));
        dto.setPrev_dept_id(Integer.parseInt(prevDeptId));
        dto.setPrev_position_id(Integer.parseInt(prevPosId));

        // 3. TransferService를 통해 트랜잭션 실행
        boolean isSuccess = transferService.executeTransfer(dto);

        if(isSuccess) {
            // 성공 시 해당 사원의 상세 페이지로 이동
            response.sendRedirect(request.getContextPath() + "/emp/detail?emp_no=" + empNo);
        } else {
            // 실패 시 메시지와 함께 뒤로 가기 등 처리
            response.sendRedirect(request.getContextPath() + "/emp/transfer?emp_no=" + empNo + "&error=1");
        }
    }
}
