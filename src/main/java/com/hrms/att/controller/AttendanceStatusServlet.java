package com.hrms.att.controller;

import com.hrms.att.dto.AttendanceSummaryDTO;
import com.hrms.att.service.AttendanceStatusService;
import com.hrms.att.service.AttendanceSummaryService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@WebServlet("/att/status")
public class AttendanceStatusServlet extends HttpServlet {

	private AttendanceSummaryService summaryService = new AttendanceSummaryService();
	private AttendanceStatusService statusService = new AttendanceStatusService();

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String monthParam = request.getParameter("month");
		
		LocalDate now = LocalDate.now();

		int year = now.getYear();
		int month = now.getMonthValue();

		if (monthParam != null && !monthParam.isEmpty()) {
			String[] parts = monthParam.split("-");
			year = Integer.parseInt(parts[0]);
			month = Integer.parseInt(parts[1]);
		}

		// 👉 EmpDAO 필터 구조 맞춤
		String keyword = null;
		int deptId = 0;
		int positionId = 0;
		String status = "재직";

		List<Map<String, Object>> list = summaryService.getSummaryList(keyword, deptId, positionId, status, year,
				month);
		List<String> deptList = summaryService.getDeptList();
		request.setAttribute("deptList", deptList);
		request.setAttribute("list", list);
		request.setAttribute("year", year);
		request.setAttribute("month", month);
		
		boolean isClosed = statusService.isClosed(year, month);
		request.setAttribute("isClosed", isClosed);
		
		request.getRequestDispatcher("/WEB-INF/jsp/att/attendanceStatus.jsp").forward(request, response);
	}
}