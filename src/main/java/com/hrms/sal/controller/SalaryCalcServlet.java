package com.hrms.sal.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import com.hrms.sal.dto.SalaryCalcDTO;
import com.hrms.sal.service.SalaryCalcService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/sal/calc")
public class SalaryCalcServlet extends HttpServlet {

    private SalaryCalcService service;

    @Override
    public void init() throws ServletException {
        this.service = new SalaryCalcService();
    }

    // ── GET: 급여 목록 조회 ──
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        // Flash 메시지 처리
        String successMsg = (String) session.getAttribute("successMsg");
        String errorMsg   = (String) session.getAttribute("errorMsg");
        if (successMsg != null) { request.setAttribute("successMsg", successMsg); session.removeAttribute("successMsg"); }
        if (errorMsg   != null) { request.setAttribute("errorMsg",   errorMsg);   session.removeAttribute("errorMsg");   }

        LocalDate now   = LocalDate.now();
        int year  = now.getYear();
        int month = now.getMonthValue();

        try {
            String yearParam  = request.getParameter("year");
            String monthParam = request.getParameter("month");
            if (yearParam  != null && !yearParam.trim().isEmpty())  year  = Integer.parseInt(yearParam.trim());
            if (monthParam != null && !monthParam.trim().isEmpty())  month = Integer.parseInt(monthParam.trim());
        } catch (NumberFormatException e) { /* 기본값 유지 */ }

        // 대기 건 존재 여부 (전체 지급 버튼 활성화 판단)
        List<SalaryCalcDTO> salaryList = service.getSalaryList(year, month);
        boolean hasPending     = salaryList.stream().anyMatch(s -> "대기".equals(s.getStatus()));

        // ── 추가: 근태 마감 여부 조회 ──
        boolean isClosed = service.isAttendanceClosed(year, month);

        request.setAttribute("salaryList",    salaryList);
        request.setAttribute("hasPending",    hasPending);
        request.setAttribute("isClosed",      isClosed);   // ← 추가
        request.setAttribute("yearOptions",   service.getYearOptions());
        request.setAttribute("selectedYear",  year);
        request.setAttribute("selectedMonth", month);

        request.getRequestDispatcher("/WEB-INF/jsp/sal/sal_calc.jsp")
               .forward(request, response);
    }

    // ── POST: 계산 / 재계산 / 지급 ──
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        int actorEmpId = (Integer) session.getAttribute("empId");

        String action = request.getParameter("action");
        int year  = LocalDate.now().getYear();
        int month = LocalDate.now().getMonthValue();
        try {
            year  = Integer.parseInt(request.getParameter("year").trim());
            month = Integer.parseInt(request.getParameter("month").trim());
        } catch (Exception e) { /* 기본값 유지 */ }

        try {
            switch (action == null ? "" : action) {
                case "calculate":
                    service.calculate(year, month);
                    session.setAttribute("successMsg", year + "년 " + month + "월 급여 계산이 완료되었습니다.");
                    break;
                case "recalculate":
                    service.recalculate(year, month);
                    session.setAttribute("successMsg", year + "년 " + month + "월 재계산이 완료되었습니다. (완료 건 제외)");
                    break;
                case "payOne":
                    int salaryId = Integer.parseInt(request.getParameter("salaryId").trim());
                    service.payOne(salaryId, actorEmpId, year, month); // ← year, month 추가
                    session.setAttribute("successMsg", "지급 처리가 완료되었습니다.");
                    break;
                case "payAll":
                    service.payAll(year, month, actorEmpId);
                    session.setAttribute("successMsg", year + "년 " + month + "월 전체 지급 처리가 완료되었습니다.");
                    break;
                default:
                    session.setAttribute("errorMsg", "잘못된 요청입니다.");
            }
        } catch (RuntimeException e) {
            session.setAttribute("errorMsg", e.getMessage());
        }

        response.sendRedirect(request.getContextPath() + "/sal/calc?year=" + year + "&month=" + month);
    }
}