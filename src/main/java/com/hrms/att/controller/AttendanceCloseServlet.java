package com.hrms.att.controller;

import java.io.IOException;

import com.hrms.att.service.AttendanceCloseService;
import com.hrms.att.service.AttendanceStatusService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/att/close")
public class AttendanceCloseServlet extends HttpServlet {

    private AttendanceCloseService service;
    private AttendanceStatusService sservice;

    @Override
    public void init() throws ServletException {
        this.service = new AttendanceCloseService();
        this.sservice = new AttendanceStatusService();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=UTF-8");

        int year = Integer.parseInt(request.getParameter("year"));
        int month = Integer.parseInt(request.getParameter("month"));
        int actorId = (int) request.getSession().getAttribute("empId");

        HttpSession session = request.getSession();
        // Referer로 원래 페이지 판별
        String referer = request.getHeader("Referer");
        boolean fromSalary = referer != null && referer.contains("/sal/calc");

        try {
            // 1. 조건 검사만
            sservice.validateClose(year, month);

            // 2. 마감 + 급여 재계산
            service.closeAndRecalculate(year, month, actorId);

            if (fromSalary) {
                session.setAttribute("successMsg",
                        year + "년 " + month + "월 근태 마감 및 급여 재계산이 완료되었습니다.");
                response.sendRedirect(request.getContextPath()
                        + "/sal/calc?year=" + year + "&month=" + month);
            } else {
                response.getWriter().write("OK");
            }

        } catch (IllegalStateException e) {
            if (fromSalary) {
                session.setAttribute("errorMsg", e.getMessage());
                response.sendRedirect(request.getContextPath()
                        + "/sal/calc?year=" + year + "&month=" + month);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(e.getMessage());
            }

        } catch (RuntimeException e) {
            if (fromSalary) {
                session.setAttribute("errorMsg", "마감 처리 중 오류가 발생했습니다.");
                response.sendRedirect(request.getContextPath()
                        + "/sal/calc?year=" + year + "&month=" + month);
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("마감 처리 중 오류 발생");
            }
        }
    }
}