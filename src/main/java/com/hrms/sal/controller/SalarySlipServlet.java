package com.hrms.sal.controller;

import com.hrms.sal.dto.SalarySlipDTO;
import com.hrms.sal.service.SalarySlipService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@WebServlet("/sal/slip")
public class SalarySlipServlet extends HttpServlet {

    private SalarySlipService service;

    @Override
    public void init() throws ServletException {
        this.service = new SalarySlipService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        // ── 모든 역할 공통: 반드시 본인 emp_id만 사용 ──
        int loginEmpId = (Integer) session.getAttribute("empId");

        // 연도/월 파라미터 (없으면 현재)
        LocalDate now   = LocalDate.now();
        int year  = now.getYear();
        int month = now.getMonthValue();

        try {
            String yearParam  = request.getParameter("year");
            String monthParam = request.getParameter("month");
            if (yearParam  != null && !yearParam.trim().isEmpty())
                year  = Integer.parseInt(yearParam.trim());
            if (monthParam != null && !monthParam.trim().isEmpty())
                month = Integer.parseInt(monthParam.trim());
        } catch (NumberFormatException e) { /* 기본값 유지 */ }

        // 명세서 조회 — 본인 것만
        SalarySlipDTO    slip            = service.getSlip(loginEmpId, year, month);
        List<String[]>   availableMonths = service.getAvailableMonths(loginEmpId);

        request.setAttribute("slip",            slip);
        request.setAttribute("availableMonths", availableMonths);
        request.setAttribute("yearOptions",     service.getYearOptions());
        request.setAttribute("selectedYear",    year);
        request.setAttribute("selectedMonth",   month);
        // isAdmin, empList 제거 — JSP에서도 불필요

        request.getRequestDispatcher("/WEB-INF/jsp/sal/sal_slip.jsp")
               .forward(request, response);
    }
}