package com.hrms.att.controller;

import com.hrms.att.service.LeaveService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebServlet("/leave/updateStatus")
public class LeaveApprovalServlet extends HttpServlet {

    private LeaveService service = new LeaveService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");

        int leaveId = Integer.parseInt(req.getParameter("leaveId"));
        String status = req.getParameter("status");
        String reason = req.getParameter("reason");

        // 🔥 로그인 관리자 ID
        Integer approverId = (Integer) req.getSession().getAttribute("empId");

        if (approverId == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }

        service.updateLeaveStatus(leaveId, approverId, status, reason);

        // 다시 목록으로
        resp.sendRedirect(req.getContextPath() + "/att/leave/approve");
    }
}