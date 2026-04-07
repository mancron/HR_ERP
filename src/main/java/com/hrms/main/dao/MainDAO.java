package com.hrms.main.dao;

import com.hrms.common.db.DatabaseConnection;

import java.sql.*;
import java.util.*;

public class MainDAO {

    // ─────────────────────────────────────────
    // 공통 — 본인 데이터
    // ─────────────────────────────────────────

    /** 이번 연도 잔여 연차 조회 */
    public Map<String, Object> getAnnualLeave(int empId) {
        String sql =
            "SELECT total_days, used_days, remain_days " +
            "FROM annual_leave " +
            "WHERE emp_id = ? AND leave_year = YEAR(CURDATE())";
        return querySingleRow(sql, empId);
    }

    /** 이번달 급여 조회 */
    public Map<String, Object> getMonthlySalary(int empId) {
        String sql =
            "SELECT net_salary, gross_salary, status, salary_year, salary_month " +
            "FROM salary " +
            "WHERE emp_id = ? " +
            "  AND salary_year  = YEAR(CURDATE()) " +
            "  AND salary_month = MONTH(CURDATE())";
        return querySingleRow(sql, empId);
    }

    /** 이번달 근무시간 + 초과근무 합계 */
    public Map<String, Object> getMonthWorkHours(int empId) {
        String sql =
            "SELECT COALESCE(SUM(work_hours), 0)    AS monthWorkHours, " +
            "       COALESCE(SUM(overtime_hours), 0) AS monthOvertimeHours " +
            "FROM attendance " +
            "WHERE emp_id = ? " +
            "  AND DATE_FORMAT(work_date, '%Y-%m') = DATE_FORMAT(CURDATE(), '%Y-%m')";
        return querySingleRow(sql, empId);
    }

    /** 내 신청 현황 — 최근 3건 (휴가 + 초과근무 합산) */
    public List<Map<String, Object>> getRecentRequests(int empId) {
        String sql =
            "SELECT '휴가' AS type, leave_type AS subType, " +
            "       DATE_FORMAT(start_date,'%m/%d') AS startDt, " +
            "       DATE_FORMAT(end_date,'%m/%d')   AS endDt, " +
            "       status, created_at " +
            "FROM leave_request WHERE emp_id = ? " +
            "UNION ALL " +
            "SELECT '초과근무', NULL, " +
            "       DATE_FORMAT(ot_date,'%m/%d'), NULL, " +
            "       status, created_at " +
            "FROM overtime_request WHERE emp_id = ? " +
            "ORDER BY created_at DESC LIMIT 3";
        return queryList(sql, empId, empId);
    }

    /** 최근 알림 3건 */
    public List<Map<String, Object>> getRecentNotifications(int empId) {
        String sql =
            "SELECT noti_type, message, CAST(is_read AS UNSIGNED) AS is_read, created_at " +
            "FROM notification " +
            "WHERE emp_id = ? " +
            "ORDER BY created_at DESC LIMIT 3";
        return queryList(sql, empId);
    }

    // ─────────────────────────────────────────
    // HR담당자 / CEO — 전사 결재대기
    // ─────────────────────────────────────────

    /** 전사 휴가 결재 대기 목록 */
    public List<Map<String, Object>> getPendingLeaves() {
        String sql =
            "SELECT lr.leave_id, e.emp_name, d.dept_name, " +
            "       lr.leave_type, " +
            "       DATE_FORMAT(lr.start_date,'%m/%d') AS startDt, " +
            "       DATE_FORMAT(lr.end_date,'%m/%d')   AS endDt, " +
            "       lr.days " +
            "FROM leave_request lr " +
            "JOIN employee   e ON lr.emp_id = e.emp_id " +
            "JOIN department d ON e.dept_id = d.dept_id " +
            "WHERE lr.status = '대기' " +
            "ORDER BY lr.created_at ASC";
        return queryList(sql);
    }

    /** 전사 초과근무 결재 대기 목록 */
    public List<Map<String, Object>> getPendingOts() {
        String sql =
            "SELECT ot.ot_id, e.emp_name, d.dept_name, " +
            "       DATE_FORMAT(ot.ot_date,'%m/%d') AS otDt, " +
            "       ot.ot_hours " +
            "FROM overtime_request ot " +
            "JOIN employee   e ON ot.emp_id = e.emp_id " +
            "JOIN department d ON e.dept_id = d.dept_id " +
            "WHERE ot.status = '대기' " +
            "ORDER BY ot.created_at ASC";
        return queryList(sql);
    }

    // ─────────────────────────────────────────
    // 부서장 — 팀 결재대기 (본인 dept_id 기준)
    // ─────────────────────────────────────────

    /** 팀 휴가 결재 대기 목록 — 부서장 본인의 부서 소속 팀원만 */
    public List<Map<String, Object>> getPendingLeavesByDept(int managerEmpId) {
        String sql =
            "SELECT lr.leave_id, e.emp_name, d.dept_name, " +
            "       lr.leave_type, " +
            "       DATE_FORMAT(lr.start_date,'%m/%d') AS startDt, " +
            "       DATE_FORMAT(lr.end_date,'%m/%d')   AS endDt, " +
            "       lr.days " +
            "FROM leave_request lr " +
            "JOIN employee   e ON lr.emp_id = e.emp_id " +
            "JOIN department d ON e.dept_id = d.dept_id " +
            "WHERE lr.status = '대기' " +
            "  AND e.dept_id = (SELECT dept_id FROM employee WHERE emp_id = ?) " +
            "ORDER BY lr.created_at ASC";
        return queryList(sql, managerEmpId);
    }

    /** 팀 초과근무 결재 대기 목록 — 부서장 본인의 부서 소속 팀원만 */
    public List<Map<String, Object>> getPendingOtsByDept(int managerEmpId) {
        String sql =
            "SELECT ot.ot_id, e.emp_name, d.dept_name, " +
            "       DATE_FORMAT(ot.ot_date,'%m/%d') AS otDt, " +
            "       ot.ot_hours " +
            "FROM overtime_request ot " +
            "JOIN employee   e ON ot.emp_id = e.emp_id " +
            "JOIN department d ON e.dept_id = d.dept_id " +
            "WHERE ot.status = '대기' " +
            "  AND e.dept_id = (SELECT dept_id FROM employee WHERE emp_id = ?) " +
            "ORDER BY ot.created_at ASC";
        return queryList(sql, managerEmpId);
    }

    // ─────────────────────────────────────────
    // HR담당자 전용
    // ─────────────────────────────────────────

    /** 부서별 오늘 근태 현황 — /att/status 화면에서 사용 */
    public List<Map<String, Object>> getDeptAttendanceToday() {
        String sql =
            "SELECT d.dept_name, " +
            "       COUNT(e.emp_id) AS totalEmp, " +
            "       SUM(CASE WHEN a.status IN ('출근','지각') THEN 1 ELSE 0 END) AS attendCount, " +
            "       SUM(CASE WHEN a.status = '지각'           THEN 1 ELSE 0 END) AS lateCount, " +
            "       SUM(CASE WHEN a.emp_id IS NULL            THEN 1 ELSE 0 END) AS absentCount " +
            "FROM employee e " +
            "JOIN department d ON e.dept_id = d.dept_id " +
            "LEFT JOIN attendance a ON e.emp_id = a.emp_id AND a.work_date = CURDATE() " +
            "WHERE e.status = '재직' AND d.is_active = 1 " +
            "GROUP BY d.dept_id, d.dept_name " +
            "ORDER BY d.sort_order ASC";
        return queryList(sql);
    }

    /** 이번달 급여 처리 현황 */
    public Map<String, Object> getSalaryProcessStatus() {
        String sql =
            "SELECT COUNT(*) AS totalCount, " +
            "       SUM(CASE WHEN status = '완료' THEN 1 ELSE 0 END) AS doneCount " +
            "FROM salary " +
            "WHERE salary_year = YEAR(CURDATE()) AND salary_month = MONTH(CURDATE())";
        return querySingleRow(sql);
    }

    // ─────────────────────────────────────────
    // CEO 전용
    // ─────────────────────────────────────────

    /** 전사 재직 인원 수 */
    public int getTotalEmpCount() {
        String sql = "SELECT COUNT(*) FROM employee WHERE status = '재직'";
        return queryCount(sql);
    }

    // ─────────────────────────────────────────
    // 관리자 전용
    // ─────────────────────────────────────────

    /** 잠금 계정 수 */
    public int getLockedAccountCount() {
        String sql =
            "SELECT COUNT(*) FROM account WHERE login_attempts >= 5 AND is_active = 1";
        return queryCount(sql);
    }

    /** 평가 미완료 수 (이번 연도) */
    public int getIncompleteEvalCount() {
        String sql =
            "SELECT COUNT(*) FROM evaluation " +
            "WHERE eval_year = YEAR(CURDATE()) AND eval_status = '작성중'";
        return queryCount(sql);
    }

    /** 최근 감사로그 5건 */
    public List<Map<String, Object>> getRecentAuditLogs() {
        String sql =
            "SELECT al.target_table, al.action, al.column_name, " +
            "       al.old_value, al.new_value, " +
            "       DATE_FORMAT(al.created_at, '%m/%d %H:%i') AS logTime, " +
            "       COALESCE(e.emp_name, '시스템') AS actorName " +
            "FROM audit_log al " +
            "LEFT JOIN employee e ON al.actor_id = e.emp_id " +
            "ORDER BY al.created_at DESC LIMIT 5";
        return queryList(sql);
    }

    // ─────────────────────────────────────────
    // Private 공통 쿼리 유틸
    // ─────────────────────────────────────────

    private Map<String, Object> querySingleRow(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn  = DatabaseConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) pstmt.setObject(i + 1, params[i]);
            rs = pstmt.executeQuery();
            if (rs.next()) return rowToMap(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(rs, pstmt, conn);
        }
        return new HashMap<>();
    }

    private List<Map<String, Object>> queryList(String sql, Object... params) {
        List<Map<String, Object>> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn  = DatabaseConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) pstmt.setObject(i + 1, params[i]);
            rs = pstmt.executeQuery();
            while (rs.next()) list.add(rowToMap(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(rs, pstmt, conn);
        }
        return list;
    }

    private int queryCount(String sql) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn  = DatabaseConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs    = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(rs, pstmt, conn);
        }
        return 0;
    }

    private Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        ResultSetMetaData meta = rs.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            row.put(meta.getColumnLabel(i), rs.getObject(i));
        }
        return row;
    }

    private void close(ResultSet rs, PreparedStatement pstmt, Connection conn) {
        if (rs    != null) try { rs.close();    } catch (SQLException ignored) {}
        if (pstmt != null) try { pstmt.close(); } catch (SQLException ignored) {}
        if (conn  != null) try { conn.close();  } catch (SQLException ignored) {}
    }
}