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

        // 🔥 승인 대기 목록 조회
        List<LeaveDTO> list = service.getPendingLeaves();

        req.setAttribute("list", list);

        // JSP로 forward
        req.getRequestDispatcher("/WEB-INF/jsp/att/leaveApprove.jsp")
           .forward(req, resp);
    }
}
