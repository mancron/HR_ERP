package com.hrms.sal.controller;

import com.hrms.sal.service.SalaryStatusService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

@WebServlet("/sal/status")
public class SalaryStatusServlet extends HttpServlet {

    private SalaryStatusService service;

    @Override
    public void init() throws ServletException {
        this.service = new SalaryStatusService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 연도/월 파라미터 (없으면 현재 연도/월)
        LocalDate now = LocalDate.now();
        int year  = now.getYear();
        int month = now.getMonthValue();
        int deptId = 0;

        try {
            String yearParam  = request.getParameter("year");
            String monthParam = request.getParameter("month");
            String deptParam  = request.getParameter("deptId");

            if (yearParam  != null && !yearParam.trim().isEmpty())
                year  = Integer.parseInt(yearParam.trim());
            if (monthParam != null && !monthParam.trim().isEmpty())
                month = Integer.parseInt(monthParam.trim());
            if (deptParam  != null && !deptParam.trim().isEmpty())
                deptId = Integer.parseInt(deptParam.trim());

        } catch (NumberFormatException e) {
            // 잘못된 파라미터는 기본값 유지
        }

        Map<String, Object> data = service.getSalaryStatus(year, month, deptId);

        request.setAttribute("salaryList",  data.get("salaryList"));
        request.setAttribute("deptList",    data.get("deptList"));
        request.setAttribute("totalGross",  data.get("totalGross"));
        request.setAttribute("totalNet",    data.get("totalNet"));
        request.setAttribute("avgNet",      data.get("avgNet"));
        request.setAttribute("empCount",    data.get("empCount"));
        request.setAttribute("yearOptions", service.getYearOptions());
        request.setAttribute("selectedYear",  year);
        request.setAttribute("selectedMonth", month);
        request.setAttribute("selectedDeptId", deptId);

        request.getRequestDispatcher("/WEB-INF/jsp/sal/sal_status.jsp")
               .forward(request, response);
    }
}