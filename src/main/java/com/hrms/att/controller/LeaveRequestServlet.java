package com.hrms.att.controller;

import com.hrms.att.dao.LeaveDAO;
import com.hrms.att.dto.AnnualLeaveDTO;
import com.hrms.att.dto.LeaveDTO;
import com.hrms.auth.dto.AccountDTO;
import com.hrms.emp.dto.EmployeeDTO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@WebServlet("/att/leave/req")
public class LeaveRequestServlet extends HttpServlet {

    private LeaveDAO leaveDAO = new LeaveDAO();

    // GET → 신청 페이지 이동
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();
        EmployeeDTO loginUser = (EmployeeDTO) session.getAttribute("loginUser");
        int empId = (int) session.getAttribute("empId");

        if (loginUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login.do");
            return;
        }

        // 🔥 연차 정보 조회
        AnnualLeaveDTO annual = leaveDAO.getAnnualLeave(empId);

        request.setAttribute("annual", annual);

        request.getRequestDispatcher("/WEB-INF/jsp/att/leaveRequest.jsp").forward(request, response);
    }

    // POST → 휴가 신청 처리
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession();
        AccountDTO loginUser = (AccountDTO) session.getAttribute("loginUser");

        if (loginUser == null) {
            response.sendRedirect("/auth/login.do");
            return;
        }

        int empId = loginUser.getEmpId();

        try {
            // =========================
            // 1. 파라미터 받기
            // =========================
            String leaveType = request.getParameter("leave_type");
            String halfType = request.getParameter("half_type");
            String reason = request.getParameter("reason");

            Date startDate = Date.valueOf(request.getParameter("start_date"));
            Date endDate = Date.valueOf(request.getParameter("end_date"));

            // (임시) 승인자 - 나중에 조직도 붙이면 자동 지정
            int approverId = Integer.parseInt(request.getParameter("approver_id"));

            // =========================
            // 2. days 계산
            // =========================
            double days = calculateDays(startDate.toLocalDate(), endDate.toLocalDate(), leaveType);

            // =========================
            // 3. 잔여 연차 체크
            // =========================
            double remainDays = leaveDAO.getRemainDays(empId);

            if (days > remainDays) {
                request.setAttribute("errorMsg", "잔여 연차가 부족합니다. (잔여: " + remainDays + "일)");
                request.getRequestDispatcher("/WEB-INF/jsp/att/leaveRequest.jsp").forward(request, response);
                return;
            }

            // =========================
            // 4. 기간 중복 체크
            // =========================
            if (leaveDAO.isOverlapping(empId, startDate, endDate)) {
                request.setAttribute("errorMsg", "해당 기간에 이미 신청된 휴가가 있습니다.");
                request.getRequestDispatcher("/WEB-INF/jsp/att/leaveRequest.jsp").forward(request, response);
                return;
            }

            // =========================
            // 5. DTO 세팅
            // =========================
            LeaveDTO dto = new LeaveDTO();
            dto.setEmpId(empId);
            dto.setLeaveType(leaveType);
            dto.setHalfType(halfType);
            dto.setStartDate(startDate);
            dto.setEndDate(endDate);
            dto.setDays(days);
            dto.setReason(reason);
            dto.setApproverId(approverId);

            // =========================
            // 6. INSERT
            // =========================
            boolean result = leaveDAO.insertLeave(dto);

            if (result) {
                // TODO: 알림 테이블 INSERT (LEAVE_PENDING)
                response.sendRedirect("/leave/list.do");
            } else {
                request.setAttribute("errorMsg", "휴가 신청 실패");
                request.getRequestDispatcher("/WEB-INF/jsp/att/leaveRequest.jsp").forward(request, response);
            }

        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("errorMsg", "서버 오류 발생");
            request.getRequestDispatcher("/WEB-INF/jsp/att/leaveRequest.jsp").forward(request, response);
        }
    }

    // =========================================================
    // 📌 휴가 일수 계산 (간단 버전)
    // =========================================================
    private double calculateDays(LocalDate start, LocalDate end, String leaveType) {

        long daysBetween = ChronoUnit.DAYS.between(start, end) + 1;

        // 반차 처리
        if ("반차".equals(leaveType)) {
            return 0.5;
        }

        return daysBetween;
    }
}