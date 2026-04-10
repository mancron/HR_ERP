package com.hrms.att.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

import com.hrms.att.service.AttendanceCloseService;

@WebServlet("/att/close")
public class AttendanceCloseServlet extends HttpServlet {

    private AttendanceCloseService service;

    @Override
    public void init() throws ServletException {
        this.service = new AttendanceCloseService();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        int actorId = (Integer) session.getAttribute("empId");

        int year  = 0;
        int month = 0;
        try {
            year  = Integer.parseInt(request.getParameter("year").trim());
            month = Integer.parseInt(request.getParameter("month").trim());
        } catch (Exception e) {
            session.setAttribute("errorMsg", "잘못된 요청입니다.");
            response.sendRedirect(request.getContextPath() + "/sal/calc");
            return;
        }

        try {
            service.closeAndRecalculate(year, month, actorId);
            session.setAttribute("successMsg",
                year + "년 " + month + "월 근태 마감 및 급여 자동 재계산이 완료되었습니다.");
        } catch (RuntimeException e) {
            session.setAttribute("errorMsg", e.getMessage());
        }

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            response.sendRedirect(referer);
        } else {
            response.sendRedirect(
                request.getContextPath() + "/att/status?year=" + year + "&month=" + month);
        }
    }
}