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
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 🔥 1. 파라미터 받기
        String dept = req.getParameter("dept");
        String sort = req.getParameter("sort");

        // null 방지 (선택 안했을 경우)
        if (dept == null) dept = "";
        if (sort == null) sort = "";

        // 🔥 2. 데이터 조회 (필터 + 정렬 적용)
        List<LeaveDTO> list = service.getPendingLeaves(dept, sort);

        // 🔥 3. 부서 목록 (드롭다운용)
        List<String> deptList = service.getPendingDeptList();

        // 🔥 4. JSP로 전달
        req.setAttribute("list", list);
        req.setAttribute("deptList", deptList);
        req.setAttribute("dept", dept);
        req.setAttribute("sort", sort);

        // JSP로 forward
        req.getRequestDispatcher("/WEB-INF/jsp/att/leaveApprove.jsp")
           .forward(req, resp);
    }
}