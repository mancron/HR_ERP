package com.hrms.att.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;

import com.hrms.att.service.AttendanceStatusService;

@WebServlet("/att/fix")
public class AttendanceFixServlet extends HttpServlet {

    private AttendanceStatusService service = new AttendanceStatusService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {

        int empId = Integer.parseInt(request.getParameter("empId"));
        String action = request.getParameter("actionType");

        String[] dates = request.getParameterValues("dates");

        if (dates == null) return;

        for (String d : dates) {

            LocalDate date = LocalDate.parse(d);

            switch (action) {

                case "ABSENT":
                    service.markAbsent(empId, date);
                    break;

                case "CHECKOUT":
                    service.updateCheckout(
                            empId,
                            date,
                            java.sql.Time.valueOf("18:00:00"),
                            "관리자 보정"
                    );
                    break;

                case "NORMAL":
                    service.updateAttendance(
                            empId,
                            date,
                            null,
                            null,
                            "정상",
                            "관리자 보정"
                    );
                    break;
            }
        }
    }
}
