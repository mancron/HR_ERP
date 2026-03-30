package com.hrms.att.controller;

import com.hrms.att.dto.AttendanceDTO;
import com.hrms.att.dto.AttendanceSummaryDTO;
import com.hrms.att.service.AttendanceService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.List;

@WebServlet("/att/record")
public class AttendanceServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private AttendanceService service = new AttendanceService();

    // 화면 요청
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int empId = 1; // 테스트용

        // 오늘 데이터
        AttendanceDTO dto = service.getTodayAttendance(empId);

        // 공휴일
        boolean isHoliday = service.isHoliday();

        // 월 선택 (없으면 현재 월)
        String yearMonth = request.getParameter("month");
        if (yearMonth == null || yearMonth.isEmpty()) {
            yearMonth = java.time.LocalDate.now().toString().substring(0, 7);
        }

        // 월별 리스트
        List<AttendanceDTO> list = service.getMonthlyWithAbsent(empId, yearMonth);
        
     // 👉 통계 생성
        AttendanceSummaryDTO summary = service.getMonthlySummary(list);

        request.setAttribute("list", list);
        request.setAttribute("summary", summary);
        request.setAttribute("attendance", dto);
        request.setAttribute("isHoliday", isHoliday);
        request.setAttribute("month", yearMonth);

        request.getRequestDispatcher("/WEB-INF/jsp/att/attendance.jsp").forward(request, response);
    }

    // 출근 / 퇴근 처리
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int empId = 1; // ⚠️ 테스트용
        String action = request.getParameter("action");

        if ("checkin".equals(action)) {
            service.checkIn(empId);

        } else if ("checkout".equals(action)) {
            service.checkOut(empId);
        }

        // 처리 후 다시 화면으로 이동
        response.sendRedirect(request.getContextPath() + "/att/record");
    }
}