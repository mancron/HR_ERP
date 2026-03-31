package com.hrms.att.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.List;

import com.hrms.att.dto.LeaveDTO;
import com.hrms.att.service.LeaveService;

@WebServlet("/att/leave/approve")
public class LeaveApprovePageServlet extends HttpServlet {

	private LeaveService service = new LeaveService();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		Integer approverId = (Integer) req.getSession().getAttribute("empId");

		if (approverId == null) {
			resp.sendRedirect(req.getContextPath() + "/login.jsp");
			return;
		}
		
		// 🔥 1. 파라미터 받기
		String dept = req.getParameter("dept");
		String sort = req.getParameter("sort");
		String startDate = req.getParameter("startDate");
		String endDate = req.getParameter("endDate");

		// null 방지 (선택 안했을 경우)
		if (dept == null)
			dept = "";
		if (sort == null)
			sort = "";
		if (startDate == null)
			startDate = "";
		if (endDate == null)
			endDate = "";

		if (!startDate.isEmpty() && !endDate.isEmpty()) {
			if (startDate.compareTo(endDate) > 0) {
				// 🔥 에러 메시지 설정
				req.setAttribute("error", "invalid_date");

				// 🔥 기존 입력값 유지
				req.setAttribute("dept", dept);
				req.setAttribute("sort", sort);
				req.setAttribute("startDate", startDate);
				req.setAttribute("endDate", endDate);

				// 🔥 부서 리스트만 조회
				List<String> deptList = service.getPendingDeptList();
				req.setAttribute("deptList", deptList);

				// 🔥 조회 막고 바로 JSP로
				req.getRequestDispatcher("/WEB-INF/jsp/att/leaveApprove.jsp").forward(req, resp);
				return;
			}
		}

		// 🔥 2. 데이터 조회 (필터 + 정렬 적용)
		List<LeaveDTO> list = service.getPendingLeaves(dept, sort, startDate, endDate, approverId);

		// 🔥 3. 부서 목록 (드롭다운용)
		List<String> deptList = service.getPendingDeptList();

		// 🔥 4. JSP로 전달
		req.setAttribute("list", list);
		req.setAttribute("deptList", deptList);
		req.setAttribute("dept", dept);
		req.setAttribute("sort", sort);
		req.setAttribute("startDate", startDate);
		req.setAttribute("endDate", endDate);

		// JSP로 forward
		req.getRequestDispatcher("/WEB-INF/jsp/att/leaveApprove.jsp").forward(req, resp);
	}
}