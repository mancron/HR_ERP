package com.hrms.main.service;

import java.util.Map;

import com.hrms.att.dto.AttendanceDTO;
import com.hrms.att.service.AttendanceService;
import com.hrms.main.dao.MainDAO;
import com.hrms.main.dto.MainDashboardDTO;

public class MainService {

    private final MainDAO           mainDAO           = new MainDAO();
    private final AttendanceService attendanceService = new AttendanceService();

    // ────────────────────────────────────────────────────────────────
    // 공통 — 본인 급여·연차·근무시간 (전 역할 공통 사용)
    // ────────────────────────────────────────────────────────────────
    private void fillPersonalData(MainDashboardDTO dto, int empId) {
        Map<String, Object> al = mainDAO.getAnnualLeave(empId);
        dto.setRemainDays(toDouble(al.get("remain_days")));
        dto.setUsedDays(toDouble(al.get("used_days")));
        dto.setTotalDays(toDouble(al.get("total_days")));

        Map<String, Object> sal = mainDAO.getMonthlySalary(empId);
        dto.setNetSalary(toInt(sal.get("net_salary")));
        dto.setSalaryStatus((String) sal.get("status"));
        dto.setSalaryYear(toInt(sal.get("salary_year")));
        dto.setSalaryMonth(toInt(sal.get("salary_month")));

        Map<String, Object> wh = mainDAO.getMonthWorkHours(empId);
        dto.setMonthWorkHours(toDouble(wh.get("monthWorkHours")));
        dto.setMonthOvertimeHours(toDouble(wh.get("monthOvertimeHours")));
    }

    // ────────────────────────────────────────────────────────────────
    // 일반직원 대시보드
    // ────────────────────────────────────────────────────────────────
    public MainDashboardDTO getUserDashboard(int empId) {
        MainDashboardDTO dto = new MainDashboardDTO();
        fillPersonalData(dto, empId);
        dto.setRecentRequests(mainDAO.getRecentRequests(empId));
        dto.setRecentNotifications(mainDAO.getRecentNotifications(empId));
        return dto;
    }

    // ────────────────────────────────────────────────────────────────
    // 부서장 대시보드 — 팀 결재대기 + 본인 데이터
    // ────────────────────────────────────────────────────────────────
    public MainDashboardDTO getManagerDashboard(int empId) {
        MainDashboardDTO dto = new MainDashboardDTO();
        fillPersonalData(dto, empId);

        // 팀 결재대기 (deptId 기준으로 필터링은 DAO에서 처리)
        dto.setPendingLeaves(mainDAO.getPendingLeavesByDept(empId));
        dto.setPendingLeaveCount(dto.getPendingLeaves().size());
        dto.setPendingOts(mainDAO.getPendingOtsByDept(empId));
        dto.setPendingOtCount(dto.getPendingOts().size());

        // 본인 신청 현황
        dto.setRecentRequests(mainDAO.getRecentRequests(empId));

        return dto;
    }

    // ────────────────────────────────────────────────────────────────
    // HR담당자 대시보드 — 전사 결재대기 + 급여처리 + 본인 데이터
    // 부서별 근태는 /att/status 전용 화면에서 처리
    // ────────────────────────────────────────────────────────────────
    public MainDashboardDTO getHrDashboard(int empId) {
        MainDashboardDTO dto = new MainDashboardDTO();
        fillPersonalData(dto, empId);

        dto.setPendingLeaves(mainDAO.getPendingLeaves());
        dto.setPendingLeaveCount(dto.getPendingLeaves().size());
        dto.setPendingOts(mainDAO.getPendingOts());
        dto.setPendingOtCount(dto.getPendingOts().size());

        Map<String, Object> salStatus = mainDAO.getSalaryProcessStatus();
        dto.setSalaryDoneCount(toInt(salStatus.get("doneCount")));
        dto.setSalaryTotalCount(toInt(salStatus.get("totalCount")));

        return dto;
    }

    // ────────────────────────────────────────────────────────────────
    // CEO 대시보드 — 전사 조회 전용 (쓰기 없음) + 본인 데이터
    // ────────────────────────────────────────────────────────────────
    public MainDashboardDTO getCeoDashboard(int empId) {
        MainDashboardDTO dto = new MainDashboardDTO();
        fillPersonalData(dto, empId);

        dto.setTotalEmpCount(mainDAO.getTotalEmpCount());
        dto.setPendingLeaves(mainDAO.getPendingLeaves());
        dto.setPendingLeaveCount(dto.getPendingLeaves().size());
        dto.setIncompleteEvalCount(mainDAO.getIncompleteEvalCount());

        return dto;
    }

    // ────────────────────────────────────────────────────────────────
    // 관리자 대시보드 — 시스템 지표 전용 + 본인 데이터
    // 결재대기·급여처리는 HR 업무이므로 제외
    // ────────────────────────────────────────────────────────────────
    public MainDashboardDTO getAdminDashboard(int empId) {
        MainDashboardDTO dto = new MainDashboardDTO();
        fillPersonalData(dto, empId);

        dto.setLockedAccountCount(mainDAO.getLockedAccountCount());
        dto.setIncompleteEvalCount(mainDAO.getIncompleteEvalCount());
        dto.setRecentAuditLogs(mainDAO.getRecentAuditLogs());

        return dto;
    }

    // ────────────────────────────────────────────────────────────────
    // 출퇴근 조회 (전 역할 공통)
    // ────────────────────────────────────────────────────────────────
    public AttendanceDTO getTodayAttendance(int empId) {
        return attendanceService.getTodayAttendance(empId);
    }

    // ── 타입 변환 유틸 ──────────────────────────────────────────────
    private double toDouble(Object obj) {
        if (obj == null) return 0.0;
        return Double.parseDouble(obj.toString());
    }

    private int toInt(Object obj) {
        if (obj == null) return 0;
        return Integer.parseInt(obj.toString());
    }
}