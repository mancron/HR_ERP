package com.hrms.att.controller;

import com.google.gson.Gson;
import com.hrms.att.dto.AttIssueDTO;
import com.hrms.att.service.AttendanceIssueService;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@WebServlet("/att/issues")
public class AttendanceIssueServlet extends HttpServlet {

    private AttendanceIssueService service = new AttendanceIssueService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int empId = Integer.parseInt(request.getParameter("empId"));

        String monthParam = request.getParameter("month");

        int year;
        int month;

        if (monthParam != null && !monthParam.isEmpty()) {
            String[] parts = monthParam.split("-");
            year = Integer.parseInt(parts[0]);
            month = Integer.parseInt(parts[1]);
        } else {
            LocalDate now = LocalDate.now();
            year = now.getYear();
            month = now.getMonthValue();
        }

        List<AttIssueDTO> list = service.getIssues(empId, year, month);

        response.setContentType("application/json;charset=UTF-8");

        new Gson().toJson(list, response.getWriter());
    }
}