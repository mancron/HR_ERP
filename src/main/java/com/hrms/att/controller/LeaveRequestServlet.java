package com.hrms.att.controller;

import com.hrms.att.dto.AnnualLeaveDTO;
import com.hrms.att.dto.LeaveDTO;
import com.hrms.att.service.LeaveService;
import com.hrms.emp.dto.EmployeeDTO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.Date;
import java.util.List;

@WebServlet("/att/leave/req")
public class LeaveRequestServlet extends HttpServlet {

	private LeaveService leaveService = new LeaveService();

	// GET → 신청 페이지 이동
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		HttpSession session = request.getSession();
		EmployeeDTO loginUser = (EmployeeDTO) session.getAttribute("loginUser");
		int empId = (int) session.getAttribute("empId");

		if (loginUser == null) {
			response.sendRedirect(request.getContextPath() + "/auth/login.do");
			return;
		}

		// 🔥 연차 정보 조회
		AnnualLeaveDTO annual = leaveService.getAnnualLeave(empId);
		List<LeaveDTO> list = leaveService.getLeaveList(empId);
		
		request.setAttribute("annual", annual);
		request.setAttribute("list", list);

		request.getRequestDispatcher("/WEB-INF/jsp/att/leaveRequest.jsp").forward(request, response);
	}

	// POST → 휴가 신청 처리
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		request.setCharacterEncoding("UTF-8");

		HttpSession session = request.getSession();
		EmployeeDTO loginUser = (EmployeeDTO) session.getAttribute("loginUser");

		if (loginUser == null) {
			response.sendRedirect(request.getContextPath() + "/auth/login.do");
			return;
		}

		int empId = loginUser.getEmpId();

		try {
			// 1. 파라미터
			String leaveType = request.getParameter("leave_type");
			String halfType = request.getParameter("half_type");
			String reason = request.getParameter("reason");

			Date startDate = Date.valueOf(request.getParameter("start_date"));
			Date endDate = Date.valueOf(request.getParameter("end_date"));

			double days = leaveService.calculateDays(startDate.toLocalDate(), endDate.toLocalDate(), leaveType);

			// 2. DTO
			LeaveDTO dto = new LeaveDTO();
			dto.setEmpId(empId);
			dto.setLeaveType(leaveType);
			dto.setHalfType(halfType);
			dto.setStartDate(startDate);
			dto.setEndDate(endDate);
			dto.setDays(days);
			dto.setReason(reason);

			// 3. Service 호출
			String result = leaveService.applyLeave(dto);

			// 4. 결과 처리
			if ("success".equals(result)) {
				response.sendRedirect(request.getContextPath() + "/att/leave/req?msg=success");
			} else {
				response.sendRedirect(request.getContextPath() + "/att/leave/req?error=" + result);
			}

		} catch (Exception e) {
			e.printStackTrace();
			response.sendRedirect(request.getContextPath() + "/att/leave/req?error=exception");
		}
	}
}