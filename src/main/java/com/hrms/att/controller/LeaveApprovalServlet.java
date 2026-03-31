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

        Integer approverId = (Integer) req.getSession().getAttribute("empId");

        if (approverId == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }

        try {
            // 🔥 핵심 변경
            boolean result = service.approveLeave(leaveId, approverId, status, reason);

            if (!result) {
                throw new Exception("승인/반려 처리 실패");
            }

        } catch (Exception e) {
            e.printStackTrace();

            req.setAttribute("errorMsg", e.getMessage());
            req.getRequestDispatcher("/error.jsp").forward(req, resp);
            return;
        }

        // 성공 시 목록 이동
        resp.sendRedirect(req.getContextPath() + "/att/leave/approve");
    }
}