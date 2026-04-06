package com.hrms.att.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.List;

import com.hrms.att.dto.OvertimeDTO;
import com.hrms.att.service.OvertimeService;

@WebServlet("/att/overtime/approve")
public class OvertimeApprovePageServlet extends HttpServlet {

    private OvertimeService service = new OvertimeService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

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

        // null 방지
        if (dept == null) dept = "";
        if (sort == null) sort = "";
        if (startDate == null) startDate = "";
        if (endDate == null) endDate = "";

        // 🔥 날짜 역전 방지 (휴가랑 동일)
        if (!startDate.isEmpty() && !endDate.isEmpty()) {
            if (startDate.compareTo(endDate) > 0) {

                req.setAttribute("error", "invalid_date");

                req.setAttribute("dept", dept);
                req.setAttribute("sort", sort);
                req.setAttribute("startDate", startDate);
                req.setAttribute("endDate", endDate);

                // 부서 리스트만 조회
                List<String> deptList = service.getPendingDeptList();
                req.setAttribute("deptList", deptList);

                req.getRequestDispatcher("/WEB-INF/jsp/att/overtimeApprove.jsp")
                        .forward(req, resp);
                return;
            }
        }

        // 🔥 2. 데이터 조회
        List<OvertimeDTO> list =
                service.getPendingOvertimes(dept, sort, startDate, endDate, approverId);

        // 🔥 3. 부서 목록
        List<String> deptList = service.getPendingDeptList();

        // 🔥 4. 전달
        req.setAttribute("list", list);
        req.setAttribute("deptList", deptList);
        req.setAttribute("dept", dept);
        req.setAttribute("sort", sort);
        req.setAttribute("startDate", startDate);
        req.setAttribute("endDate", endDate);

        req.getRequestDispatcher("/WEB-INF/jsp/att/overtimeApprove.jsp")
                .forward(req, resp);
    }
}