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
		String dept = request.getParameter("dept");
		int positionId = 0;
		String status = "재직";

		int page = 1;
		int size = 10;

		String pageParam = request.getParameter("page");
		if (pageParam != null && !pageParam.trim().isEmpty()) {
		    page = Integer.parseInt(pageParam.trim());
		}

		int offset = (page - 1) * size;
		
		List<Map<String, Object>> list = summaryService.getSummaryList(keyword, dept, positionId, status, year,
				month, offset, size);
		int totalCount = summaryService.getSummaryCount(keyword, dept, positionId, status);

		int totalPage = (int) Math.ceil((double) totalCount / size);
		
		List<String> deptList = summaryService.getDeptList();
		request.setAttribute("deptList", deptList);
		request.setAttribute("list", list);
		request.setAttribute("year", year);
		request.setAttribute("month", month);
		request.setAttribute("currentPage", page);
		request.setAttribute("totalPage", totalPage);
		
		boolean isClosed = statusService.isClosed(year, month);
		boolean hasUnfinished = statusService.existsUnfinishedCheckoutAll(year, month);
		boolean hasAbsent = statusService.existsAbsentCandidateAll(year, month);
		request.setAttribute("isClosed", isClosed);
		request.setAttribute("hasUnfinished", hasUnfinished);
		request.setAttribute("hasAbsent", hasAbsent);
		
		request.getRequestDispatcher("/WEB-INF/jsp/att/attendanceStatus.jsp").forward(request, response);
	}
}