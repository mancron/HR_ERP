package com.hrms.att.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import com.hrms.att.service.AttendanceStatusService;

@WebServlet("/att/close")
public class AttendanceCloseServlet extends HttpServlet {

    private AttendanceStatusService service = new AttendanceStatusService();

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int year = Integer.parseInt(request.getParameter("year"));
        int month = Integer.parseInt(request.getParameter("month"));

        int actorId = (int) request.getSession().getAttribute("empId");

        service.closeMonth(year, month, actorId);

        response.getWriter().write("OK");
    }
}
