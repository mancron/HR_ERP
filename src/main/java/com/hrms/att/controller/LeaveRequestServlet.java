package com.hrms.att.controller;

import com.hrms.att.dto.AnnualLeaveDTO;
import com.hrms.att.dto.LeaveDTO;
import com.hrms.att.dto.RequestDTO;
import com.hrms.att.service.LeaveService;
import com.hrms.emp.dto.EmpDTO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@WebServlet("/att/leave/req")
public class LeaveRequestServlet extends HttpServlet {

	private LeaveService leaveService = new LeaveService();

	// GET → 신청 페이지 이동
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		int empId = getLoginEmpId(request, response);
		if (empId == -1)
			return;

		String monthParam = request.getParameter("month");

		LocalDate now = LocalDate.now();
		int year = now.getYear();
		int month = now.getMonthValue();

		if (monthParam != null && !monthParam.isEmpty()) {
			String[] parts = monthParam.split("-");
			year = Integer.parseInt(parts[0]);
			month = Integer.parseInt(parts[1]);
		}

		// 🔥 추가
		String selectedMonth = year + "-" + String.format("%02d", month);

		// 연차 정보 조회
		AnnualLeaveDTO annual = leaveService.getAnnualLeave(empId);

		// 리스트 조회
		List<RequestDTO> list = leaveService.getLeaveListByMonth(empId, year, month);

		request.setAttribute("annual", annual);
		request.setAttribute("list", list);
		request.setAttribute("month", selectedMonth); // 🔥 수정됨

		request.getRequestDispatcher("/WEB-INF/jsp/att/leaveRequest.jsp").forward(request, response);
	}

	// POST → 휴가 신청 처리
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		request.setCharacterEncoding("UTF-8");

		int empId = getLoginEmpId(request, response);
		if (empId == -1)
			return;

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

			// 성공
			if ("success".equals(result)) {
				request.setAttribute("msg", "휴가 신청이 완료되었습니다.");
				doGet(request, response);
				return;
			}

			// 실패
			String errorMsg;

			switch (result) {
			case "empty_reason":
				errorMsg = "사유를 입력하세요.";
				break;
			case "invalid_date":
				errorMsg = "날짜가 올바르지 않습니다.";
				break;
			case "not_enough":
				errorMsg = "연차가 부족합니다.";
				break;
			case "overlap":
				errorMsg = "이미 신청된 기간입니다.";
				break;
			case "invalid_half":
			    errorMsg = "반차는 하루만 선택 가능합니다.";
			    break;
			default:
				errorMsg = "처리 중 오류가 발생했습니다.";
			}

			request.setAttribute("errorMsg", errorMsg);

			// 🔥 입력값 유지 (UX)
			request.setAttribute("formData", dto);

			// 👉 핵심: doGet 호출
			doGet(request, response);

		} catch (Exception e) {
			e.printStackTrace();

			request.setAttribute("errorMsg", "서버 오류가 발생했습니다.");
			request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
		}
	}

	// 로그인 검증
	private int getLoginEmpId(HttpServletRequest request, HttpServletResponse response) throws IOException {
		HttpSession session = request.getSession();
		EmpDTO loginUser = (EmpDTO) session.getAttribute("loginUser");

		if (loginUser == null) {
			response.sendRedirect(request.getContextPath() + "/auth/login.do");
			return -1;
		}

		return loginUser.getEmp_id();
	}
}