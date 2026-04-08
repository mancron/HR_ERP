package com.hrms.att.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDate;

import com.hrms.att.service.AttendanceStatusService;

@WebServlet("/att/fix")
public class AttendanceFixServlet extends HttpServlet {

    private AttendanceStatusService service = new AttendanceStatusService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    	
    	HttpSession session = request.getSession();
    	int actorId = (int) session.getAttribute("empId");
    	
    	String empIdStr = request.getParameter("empId");

    	if (empIdStr == null || empIdStr.isEmpty()) {
    	    throw new RuntimeException("empId 누락");
    	}

    	int empId = Integer.parseInt(empIdStr);
    	
        String action = request.getParameter("actionType");

        String[] dates = request.getParameterValues("dates");

        if (dates == null) return;

        for (String d : dates) {

            LocalDate date = LocalDate.parse(d);

            switch (action) {

                case "ABSENT":
                    service.markAbsent(empId, date, actorId);
                    break;

                case "CHECKIN_FIX":
                    service.updateCheckIn(
                        empId,
                        date,
                        java.sql.Time.valueOf("09:00:00"),
                        "출근 보정",
                        actorId
                    );
                    break;

                case "CHECKOUT_FIX":
                    service.updateCheckout(
                        empId,
                        date,
                        java.sql.Time.valueOf("18:00:00"),
                        "퇴근 보정",
                        actorId
                    );
                    break;
            }
        }
    }
}
