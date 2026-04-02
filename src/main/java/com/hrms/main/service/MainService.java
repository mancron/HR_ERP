package com.hrms.main.service;

import java.util.Map;

import com.hrms.att.dto.AttendanceDTO;
import com.hrms.att.service.AttendanceService;
import com.hrms.main.dao.MainDAO;
import com.hrms.main.dto.MainDashboardDTO;
import com.hrms.sys.service.NotificationService;

public class MainService {

    private final MainDAO           mainDAO             = new MainDAO();
    private final AttendanceService attendanceService   = new AttendanceService();
    private final NotificationService notificationService = new NotificationService();

    /** 일반 사용자 대시보드 데이터 */
    public MainDashboardDTO getUserDashboard(int empId) {
        MainDashboardDTO dto = new MainDashboardDTO();

        // 연차
        Map<String, Object> al = mainDAO.getAnnualLeave(empId);
        dto.setRemainDays(toDouble(al.get("remain_days")));
        dto.setUsedDays(toDouble(al.get("used_days")));
        dto.setTotalDays(toDouble(al.get("total_days")));

        // 급여
        Map<String, Object> sal = mainDAO.getMonthlySalary(empId);
        dto.setNetSalary(toInt(sal.get("net_salary")));
        dto.setSalaryStatus((String) sal.get("status"));
        dto.setSalaryYear(toInt(sal.get("salary_year")));
        dto.setSalaryMonth(toInt(sal.get("salary_month")));

        // 근무시간
        Map<String, Object> wh = mainDAO.getMonthWorkHours(empId);
        dto.setMonthWorkHours(toDouble(wh.get("monthWorkHours")));
        dto.setMonthOvertimeHours(toDouble(wh.get("monthOvertimeHours")));

        // 신청 현황 + 알림
        dto.setRecentRequests(mainDAO.getRecentRequests(empId));
        dto.setRecentNotifications(mainDAO.getRecentNotifications(empId));

        return dto;
    }

    /** HR담당자 대시보드 데이터 */
    public MainDashboardDTO getHrDashboard(int empId) {
        MainDashboardDTO dto = getUserDashboard(empId); // 공통 데이터 포함

        dto.setPendingLeaves(mainDAO.getPendingLeaves());
        dto.setPendingLeaveCount(dto.getPendingLeaves().size());
        dto.setPendingOts(mainDAO.getPendingOts());
        dto.setPendingOtCount(dto.getPendingOts().size());
        dto.setDeptAttendance(mainDAO.getDeptAttendanceToday());

        Map<String, Object> salStatus = mainDAO.getSalaryProcessStatus();
        dto.setSalaryDoneCount(toInt(salStatus.get("doneCount")));
        dto.setSalaryTotalCount(toInt(salStatus.get("totalCount")));

        return dto;
    }

    /** 관리자 대시보드 데이터 */
    public MainDashboardDTO getAdminDashboard(int empId) {
        MainDashboardDTO dto = getHrDashboard(empId); // HR 데이터 포함

        dto.setLockedAccountCount(mainDAO.getLockedAccountCount());
        dto.setIncompleteEvalCount(mainDAO.getIncompleteEvalCount());
        dto.setRecentAuditLogs(mainDAO.getRecentAuditLogs());

        return dto;
    }

    /** 오늘 출퇴근 조회 (AttendanceService 재사용) */
    public AttendanceDTO getTodayAttendance(int empId) {
        return attendanceService.getTodayAttendance(empId);
    }

    // ── 타입 변환 유틸 ──
    private double toDouble(Object obj) {
        if (obj == null) return 0.0;
        return Double.parseDouble(obj.toString());
    }

    private int toInt(Object obj) {
        if (obj == null) return 0;
        return Integer.parseInt(obj.toString());
    }
}