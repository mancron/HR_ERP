package com.hrms.main.dto;

import java.util.List;
import java.util.Map;

/**
 * 대시보드 화면에 필요한 모든 데이터를 담는 DTO
 * role별로 필요한 필드만 채워서 JSP로 전달
 */
public class MainDashboardDTO {

    // ── 공통 (전체 role) ──────────────────────────────────────────
    private double remainDays;          // 잔여 연차
    private double usedDays;            // 사용 연차
    private double totalDays;           // 부여 연차

    private int    netSalary;           // 이번달 실수령액
    private String salaryStatus;        // 대기/완료
    private int    salaryMonth;         // 급여 월
    private int    salaryYear;          // 급여 연도

    private double monthWorkHours;      // 이번달 근무시간 합계
    private double monthOvertimeHours;  // 이번달 초과근무 합계

    private List<Map<String, Object>> recentRequests;       // 내 신청 현황 (최근 3건)
    private List<Map<String, Object>> recentNotifications;  // 최근 알림 (3건)

    // ── HR담당자 / 부서장 ──────────────────────────────────────────
    private int pendingLeaveCount;                          // 휴가 결재 대기 수
    private int pendingOtCount;                             // 초과근무 결재 대기 수
    private List<Map<String, Object>> pendingLeaves;        // 휴가 대기 목록
    private List<Map<String, Object>> pendingOts;           // 초과근무 대기 목록

    private List<Map<String, Object>> deptAttendance;       // 부서별 오늘 근태 현황

    private int salaryDoneCount;        // 이번달 급여 지급 완료 수
    private int salaryTotalCount;       // 이번달 급여 전체 대상 수

    // ── CEO 전용 ──────────────────────────────────────────────────
    private int totalEmpCount;          // 전사 재직 인원 수

    // ── 관리자 전용 ───────────────────────────────────────────────
    private int lockedAccountCount;     // 잠금 계정 수
    private int incompleteEvalCount;    // 평가 미완료 수

    private List<Map<String, Object>> recentAuditLogs;      // 최근 감사로그 5건

    // getter / setter
    public double getRemainDays() { return remainDays; }
    public void setRemainDays(double remainDays) { this.remainDays = remainDays; }

    public double getUsedDays() { return usedDays; }
    public void setUsedDays(double usedDays) { this.usedDays = usedDays; }

    public double getTotalDays() { return totalDays; }
    public void setTotalDays(double totalDays) { this.totalDays = totalDays; }

    public int getNetSalary() { return netSalary; }
    public void setNetSalary(int netSalary) { this.netSalary = netSalary; }

    public String getSalaryStatus() { return salaryStatus; }
    public void setSalaryStatus(String salaryStatus) { this.salaryStatus = salaryStatus; }

    public int getSalaryMonth() { return salaryMonth; }
    public void setSalaryMonth(int salaryMonth) { this.salaryMonth = salaryMonth; }

    public int getSalaryYear() { return salaryYear; }
    public void setSalaryYear(int salaryYear) { this.salaryYear = salaryYear; }

    public double getMonthWorkHours() { return monthWorkHours; }
    public void setMonthWorkHours(double monthWorkHours) { this.monthWorkHours = monthWorkHours; }

    public double getMonthOvertimeHours() { return monthOvertimeHours; }
    public void setMonthOvertimeHours(double monthOvertimeHours) { this.monthOvertimeHours = monthOvertimeHours; }

    public List<Map<String, Object>> getRecentRequests() { return recentRequests; }
    public void setRecentRequests(List<Map<String, Object>> recentRequests) { this.recentRequests = recentRequests; }

    public List<Map<String, Object>> getRecentNotifications() { return recentNotifications; }
    public void setRecentNotifications(List<Map<String, Object>> recentNotifications) { this.recentNotifications = recentNotifications; }

    public int getPendingLeaveCount() { return pendingLeaveCount; }
    public void setPendingLeaveCount(int pendingLeaveCount) { this.pendingLeaveCount = pendingLeaveCount; }

    public int getPendingOtCount() { return pendingOtCount; }
    public void setPendingOtCount(int pendingOtCount) { this.pendingOtCount = pendingOtCount; }

    public List<Map<String, Object>> getPendingLeaves() { return pendingLeaves; }
    public void setPendingLeaves(List<Map<String, Object>> pendingLeaves) { this.pendingLeaves = pendingLeaves; }

    public List<Map<String, Object>> getPendingOts() { return pendingOts; }
    public void setPendingOts(List<Map<String, Object>> pendingOts) { this.pendingOts = pendingOts; }

    public List<Map<String, Object>> getDeptAttendance() { return deptAttendance; }
    public void setDeptAttendance(List<Map<String, Object>> deptAttendance) { this.deptAttendance = deptAttendance; }

    public int getSalaryDoneCount() { return salaryDoneCount; }
    public void setSalaryDoneCount(int salaryDoneCount) { this.salaryDoneCount = salaryDoneCount; }

    public int getSalaryTotalCount() { return salaryTotalCount; }
    public void setSalaryTotalCount(int salaryTotalCount) { this.salaryTotalCount = salaryTotalCount; }

    public int getTotalEmpCount() { return totalEmpCount; }
    public void setTotalEmpCount(int totalEmpCount) { this.totalEmpCount = totalEmpCount; }

    public int getLockedAccountCount() { return lockedAccountCount; }
    public void setLockedAccountCount(int lockedAccountCount) { this.lockedAccountCount = lockedAccountCount; }

    public int getIncompleteEvalCount() { return incompleteEvalCount; }
    public void setIncompleteEvalCount(int incompleteEvalCount) { this.incompleteEvalCount = incompleteEvalCount; }

    public List<Map<String, Object>> getRecentAuditLogs() { return recentAuditLogs; }
    public void setRecentAuditLogs(List<Map<String, Object>> recentAuditLogs) { this.recentAuditLogs = recentAuditLogs; }
}