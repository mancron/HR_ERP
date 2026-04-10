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

        int year = Integer.parseInt(request.getParameter("year"));
        int month = Integer.parseInt(request.getParameter("month"));

        int actorId = (int) request.getSession().getAttribute("empId");

        try {
            // 🔥 1️⃣ 근태 마감 (조건 검사 포함)
            service.closeMonth(year, month, actorId);

            // 🔥 2️⃣ 급여 재계산 (마감 성공 후 실행)
            service.recalculateAfterClose(year, month, actorId);

            response.getWriter().write("OK");

        } catch (IllegalStateException e) {
            // 👉 마감 조건 실패 (결근 후보, 미퇴근 등)
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(e.getMessage());

        } catch (RuntimeException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("마감 처리 중 오류 발생");
        }
    }
}