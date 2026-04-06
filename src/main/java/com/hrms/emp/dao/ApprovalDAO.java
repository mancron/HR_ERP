package com.hrms.emp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.hrms.emp.dto.LeaveDTO;
import com.hrms.emp.dto.ResignDTO;

public class ApprovalDAO {

    // 공통 SELECT 절
    private static final String LEAVE_SELECT =
        "SELECT r.*, e.emp_name, e.emp_no, d.dept_name, p.position_name, " +
        "(d.dept_name = '인사팀') AS is_hr_dept, " +
        "(SELECT COUNT(*) FROM account a WHERE a.emp_id = e.emp_id AND a.role = '최종승인자') > 0 AS is_req_president " +
        "FROM leave_of_absence_request r " +
        "JOIN employee e     ON r.emp_id      = e.emp_id " +
        "JOIN department d   ON e.dept_id      = d.dept_id " +
        "JOIN job_position p ON e.position_id  = p.position_id ";

    private static final String RESIGN_SELECT =
        "SELECT r.*, e.emp_name, e.emp_no, d.dept_name, p.position_name, " +
        "(d.dept_name = '인사팀') AS is_hr_dept, " +
        "(SELECT COUNT(*) FROM account a WHERE a.emp_id = e.emp_id AND a.role = '최종승인자') > 0 AS is_req_president " +
        "FROM resign_request r " +
        "JOIN employee e     ON r.emp_id      = e.emp_id " +
        "JOIN department d   ON e.dept_id      = d.dept_id " +
        "JOIN job_position p ON e.position_id  = p.position_id ";

    // 미완료 상태 조건
    private static final String IN_PROGRESS = "AND r.status NOT IN ('최종승인', '반려') ";

    // 완료 상태 조건
    private static final String DONE = "AND r.status IN ('최종승인', '반려') ";

    // ────────────────────────────────────────────────
    // 부서장 여부 확인
    // ────────────────────────────────────────────────
    public boolean isDeptManager(Connection con, int empId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM department WHERE manager_id = ? AND is_active = 1";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, empId);
            rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }

    // ────────────────────────────────────────────────
    // 결재 현황 — 미완료 (대기/부서장승인/HR담당자승인)
    // ────────────────────────────────────────────────

    // 부서장용 휴직/복직 목록 (미완료)
    public List<LeaveDTO> getLeaveListForDeptManager(Connection con, int empId,
            String status, String keyword, String deptName, String leaveType)
            throws SQLException {
        StringBuilder sql = new StringBuilder(LEAVE_SELECT)
            .append("WHERE (r.dept_manager_id = ? OR r.emp_id = ?) ")
            .append(IN_PROGRESS);
        if (!"all".equals(status))                     sql.append("AND r.status = ? ");
        if (keyword   != null && !keyword.isEmpty())   sql.append("AND e.emp_name LIKE ? ");
        if (deptName  != null && !deptName.isEmpty())  sql.append("AND d.dept_name = ? ");
        if (leaveType != null && !leaveType.isEmpty()) sql.append("AND r.leave_type = ? ");
        sql.append("ORDER BY r.start_date ASC");

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<LeaveDTO> list = new ArrayList<>();
        try {
            pstmt = con.prepareStatement(sql.toString());
            int idx = 1;
            pstmt.setInt(idx++, empId);
            pstmt.setInt(idx++, empId);
            if (!"all".equals(status))                     pstmt.setString(idx++, status);
            if (keyword   != null && !keyword.isEmpty())   pstmt.setString(idx++, "%" + keyword + "%");
            if (deptName  != null && !deptName.isEmpty())  pstmt.setString(idx++, deptName);
            if (leaveType != null && !leaveType.isEmpty()) pstmt.setString(idx++, leaveType);
            rs = pstmt.executeQuery();
            while (rs.next()) list.add(mapLeaveRow(rs));
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
        return list;
    }

    // 부서장용 퇴직 목록 (미완료)
    public List<ResignDTO> getResignListForDeptManager(Connection con, int empId,
            String status, String keyword, String deptName)
            throws SQLException {
        StringBuilder sql = new StringBuilder(RESIGN_SELECT)
            .append("WHERE (r.dept_manager_id = ? OR r.emp_id = ?) ")
            .append(IN_PROGRESS);
        if (!"all".equals(status))                    sql.append("AND r.status = ? ");
        if (keyword  != null && !keyword.isEmpty())   sql.append("AND e.emp_name LIKE ? ");
        if (deptName != null && !deptName.isEmpty())  sql.append("AND d.dept_name = ? ");
        sql.append("ORDER BY r.resign_date ASC");

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<ResignDTO> list = new ArrayList<>();
        try {
            pstmt = con.prepareStatement(sql.toString());
            int idx = 1;
            pstmt.setInt(idx++, empId);
            pstmt.setInt(idx++, empId);
            if (!"all".equals(status))                    pstmt.setString(idx++, status);
            if (keyword  != null && !keyword.isEmpty())   pstmt.setString(idx++, "%" + keyword + "%");
            if (deptName != null && !deptName.isEmpty())  pstmt.setString(idx++, deptName);
            rs = pstmt.executeQuery();
            while (rs.next()) list.add(mapResignRow(rs));
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
        return list;
    }

    // HR담당자/관리자용 휴직/복직 목록 (미완료)
    public List<LeaveDTO> getLeaveListForHr(Connection con, String status,
            String keyword, String deptName, String leaveType) throws SQLException {
        StringBuilder sql = new StringBuilder(LEAVE_SELECT)
            .append("WHERE 1=1 ")
            .append(IN_PROGRESS);
        if (!"all".equals(status))                     sql.append("AND r.status = ? ");
        if (keyword   != null && !keyword.isEmpty())   sql.append("AND e.emp_name LIKE ? ");
        if (deptName  != null && !deptName.isEmpty())  sql.append("AND d.dept_name = ? ");
        if (leaveType != null && !leaveType.isEmpty()) sql.append("AND r.leave_type = ? ");
        sql.append("ORDER BY r.start_date ASC");

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<LeaveDTO> list = new ArrayList<>();
        try {
            pstmt = con.prepareStatement(sql.toString());
            int idx = 1;
            if (!"all".equals(status))                     pstmt.setString(idx++, status);
            if (keyword   != null && !keyword.isEmpty())   pstmt.setString(idx++, "%" + keyword + "%");
            if (deptName  != null && !deptName.isEmpty())  pstmt.setString(idx++, deptName);
            if (leaveType != null && !leaveType.isEmpty()) pstmt.setString(idx++, leaveType);
            rs = pstmt.executeQuery();
            while (rs.next()) list.add(mapLeaveRow(rs));
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
        return list;
    }

    // HR담당자/관리자용 퇴직 목록 (미완료)
    public List<ResignDTO> getResignListForHr(Connection con, String status,
            String keyword, String deptName) throws SQLException {
        StringBuilder sql = new StringBuilder(RESIGN_SELECT)
            .append("WHERE 1=1 ")
            .append(IN_PROGRESS);
        if (!"all".equals(status))                    sql.append("AND r.status = ? ");
        if (keyword  != null && !keyword.isEmpty())   sql.append("AND e.emp_name LIKE ? ");
        if (deptName != null && !deptName.isEmpty())  sql.append("AND d.dept_name = ? ");
        sql.append("ORDER BY r.resign_date ASC");

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<ResignDTO> list = new ArrayList<>();
        try {
            pstmt = con.prepareStatement(sql.toString());
            int idx = 1;
            if (!"all".equals(status))                    pstmt.setString(idx++, status);
            if (keyword  != null && !keyword.isEmpty())   pstmt.setString(idx++, "%" + keyword + "%");
            if (deptName != null && !deptName.isEmpty())  pstmt.setString(idx++, deptName);
            rs = pstmt.executeQuery();
            while (rs.next()) list.add(mapResignRow(rs));
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
        return list;
    }

    // ────────────────────────────────────────────────
    // 결재 처리 결과 — 완료 (관리자승인/반려) + 검색 + 페이징
    // ────────────────────────────────────────────────

    // HR담당자/관리자용 휴직/복직 처리 결과 건수
    public int getLeaveDoneCount(Connection con, String status,
            String keyword, String deptName, String leaveType) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) " +
            "FROM leave_of_absence_request r " +
            "JOIN employee e     ON r.emp_id      = e.emp_id " +
            "JOIN department d   ON e.dept_id      = d.dept_id " +
            "JOIN job_position p ON e.position_id  = p.position_id " +
            "WHERE 1=1 ").append(DONE);
        if (!"all".equals(status))                     sql.append("AND r.status = ? ");
        if (keyword   != null && !keyword.isEmpty())   sql.append("AND e.emp_name LIKE ? ");
        if (deptName  != null && !deptName.isEmpty())  sql.append("AND d.dept_name = ? ");
        if (leaveType != null && !leaveType.isEmpty()) sql.append("AND r.leave_type = ? ");

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql.toString());
            int idx = 1;
            if (!"all".equals(status))                     pstmt.setString(idx++, status);
            if (keyword   != null && !keyword.isEmpty())   pstmt.setString(idx++, "%" + keyword + "%");
            if (deptName  != null && !deptName.isEmpty())  pstmt.setString(idx++, deptName);
            if (leaveType != null && !leaveType.isEmpty()) pstmt.setString(idx++, leaveType);
            rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }

    // HR담당자/관리자용 휴직/복직 처리 결과 목록
    public List<LeaveDTO> getLeaveDoneList(Connection con, String status,
            String keyword, String deptName, String leaveType,
            int offset, int pageSize) throws SQLException {
        StringBuilder sql = new StringBuilder(LEAVE_SELECT)
            .append("WHERE 1=1 ").append(DONE);
        if (!"all".equals(status))                     sql.append("AND r.status = ? ");
        if (keyword   != null && !keyword.isEmpty())   sql.append("AND e.emp_name LIKE ? ");
        if (deptName  != null && !deptName.isEmpty())  sql.append("AND d.dept_name = ? ");
        if (leaveType != null && !leaveType.isEmpty()) sql.append("AND r.leave_type = ? ");
        sql.append("ORDER BY r.created_at DESC LIMIT ? OFFSET ?");

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<LeaveDTO> list = new ArrayList<>();
        try {
            pstmt = con.prepareStatement(sql.toString());
            int idx = 1;
            if (!"all".equals(status))                     pstmt.setString(idx++, status);
            if (keyword   != null && !keyword.isEmpty())   pstmt.setString(idx++, "%" + keyword + "%");
            if (deptName  != null && !deptName.isEmpty())  pstmt.setString(idx++, deptName);
            if (leaveType != null && !leaveType.isEmpty()) pstmt.setString(idx++, leaveType);
            pstmt.setInt(idx++, pageSize);
            pstmt.setInt(idx,   offset);
            rs = pstmt.executeQuery();
            while (rs.next()) list.add(mapLeaveRow(rs));
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
        return list;
    }

    // HR담당자/관리자용 퇴직 처리 결과 건수
    public int getResignDoneCount(Connection con, String status,
            String keyword, String deptName) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) " +
            "FROM resign_request r " +
            "JOIN employee e     ON r.emp_id      = e.emp_id " +
            "JOIN department d   ON e.dept_id      = d.dept_id " +
            "JOIN job_position p ON e.position_id  = p.position_id " +
            "WHERE 1=1 ").append(DONE);
        if (!"all".equals(status))                    sql.append("AND r.status = ? ");
        if (keyword  != null && !keyword.isEmpty())   sql.append("AND e.emp_name LIKE ? ");
        if (deptName != null && !deptName.isEmpty())  sql.append("AND d.dept_name = ? ");

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql.toString());
            int idx = 1;
            if (!"all".equals(status))                    pstmt.setString(idx++, status);
            if (keyword  != null && !keyword.isEmpty())   pstmt.setString(idx++, "%" + keyword + "%");
            if (deptName != null && !deptName.isEmpty())  pstmt.setString(idx++, deptName);
            rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }

    // HR담당자/관리자용 퇴직 처리 결과 목록
    public List<ResignDTO> getResignDoneList(Connection con, String status,
            String keyword, String deptName,
            int offset, int pageSize) throws SQLException {
        StringBuilder sql = new StringBuilder(RESIGN_SELECT)
            .append("WHERE 1=1 ").append(DONE);
        if (!"all".equals(status))                    sql.append("AND r.status = ? ");
        if (keyword  != null && !keyword.isEmpty())   sql.append("AND e.emp_name LIKE ? ");
        if (deptName != null && !deptName.isEmpty())  sql.append("AND d.dept_name = ? ");
        sql.append("ORDER BY r.created_at DESC LIMIT ? OFFSET ?");

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<ResignDTO> list = new ArrayList<>();
        try {
            pstmt = con.prepareStatement(sql.toString());
            int idx = 1;
            if (!"all".equals(status))                    pstmt.setString(idx++, status);
            if (keyword  != null && !keyword.isEmpty())   pstmt.setString(idx++, "%" + keyword + "%");
            if (deptName != null && !deptName.isEmpty())  pstmt.setString(idx++, deptName);
            pstmt.setInt(idx++, pageSize);
            pstmt.setInt(idx,   offset);
            rs = pstmt.executeQuery();
            while (rs.next()) list.add(mapResignRow(rs));
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
        return list;
    }

    // 부서장용 휴직/복직 처리 결과 건수
    public int getLeaveDoneCountForDeptManager(Connection con, int empId,
            String status, String keyword, String deptName, String leaveType) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) " +
            "FROM leave_of_absence_request r " +
            "JOIN employee e     ON r.emp_id      = e.emp_id " +
            "JOIN department d   ON e.dept_id      = d.dept_id " +
            "JOIN job_position p ON e.position_id  = p.position_id " +
            "WHERE (r.dept_manager_id = ? OR r.emp_id = ?) ").append(DONE);
        if (!"all".equals(status))                     sql.append("AND r.status = ? ");
        if (keyword   != null && !keyword.isEmpty())   sql.append("AND e.emp_name LIKE ? ");
        if (deptName  != null && !deptName.isEmpty())  sql.append("AND d.dept_name = ? ");
        if (leaveType != null && !leaveType.isEmpty()) sql.append("AND r.leave_type = ? ");

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql.toString());
            int idx = 1;
            pstmt.setInt(idx++, empId);
            pstmt.setInt(idx++, empId);
            if (!"all".equals(status))                     pstmt.setString(idx++, status);
            if (keyword   != null && !keyword.isEmpty())   pstmt.setString(idx++, "%" + keyword + "%");
            if (deptName  != null && !deptName.isEmpty())  pstmt.setString(idx++, deptName);
            if (leaveType != null && !leaveType.isEmpty()) pstmt.setString(idx++, leaveType);
            rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }

    // 부서장용 휴직/복직 처리 결과 목록
    public List<LeaveDTO> getLeaveDoneListForDeptManager(Connection con, int empId,
            String status, String keyword, String deptName, String leaveType,
            int offset, int pageSize) throws SQLException {
        StringBuilder sql = new StringBuilder(LEAVE_SELECT)
            .append("WHERE (r.dept_manager_id = ? OR r.emp_id = ?) ")
            .append(DONE);
        if (!"all".equals(status))                     sql.append("AND r.status = ? ");
        if (keyword   != null && !keyword.isEmpty())   sql.append("AND e.emp_name LIKE ? ");
        if (deptName  != null && !deptName.isEmpty())  sql.append("AND d.dept_name = ? ");
        if (leaveType != null && !leaveType.isEmpty()) sql.append("AND r.leave_type = ? ");
        sql.append("ORDER BY r.created_at DESC LIMIT ? OFFSET ?");

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<LeaveDTO> list = new ArrayList<>();
        try {
            pstmt = con.prepareStatement(sql.toString());
            int idx = 1;
            pstmt.setInt(idx++, empId);
            pstmt.setInt(idx++, empId);
            if (!"all".equals(status))                     pstmt.setString(idx++, status);
            if (keyword   != null && !keyword.isEmpty())   pstmt.setString(idx++, "%" + keyword + "%");
            if (deptName  != null && !deptName.isEmpty())  pstmt.setString(idx++, deptName);
            if (leaveType != null && !leaveType.isEmpty()) pstmt.setString(idx++, leaveType);
            pstmt.setInt(idx++, pageSize);
            pstmt.setInt(idx,   offset);
            rs = pstmt.executeQuery();
            while (rs.next()) list.add(mapLeaveRow(rs));
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
        return list;
    }

    // 부서장용 퇴직 처리 결과 건수
    public int getResignDoneCountForDeptManager(Connection con, int empId,
            String status, String keyword, String deptName) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) " +
            "FROM resign_request r " +
            "JOIN employee e     ON r.emp_id      = e.emp_id " +
            "JOIN department d   ON e.dept_id      = d.dept_id " +
            "JOIN job_position p ON e.position_id  = p.position_id " +
            "WHERE (r.dept_manager_id = ? OR r.emp_id = ?) ").append(DONE);
        if (!"all".equals(status))                    sql.append("AND r.status = ? ");
        if (keyword  != null && !keyword.isEmpty())   sql.append("AND e.emp_name LIKE ? ");
        if (deptName != null && !deptName.isEmpty())  sql.append("AND d.dept_name = ? ");

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql.toString());
            int idx = 1;
            pstmt.setInt(idx++, empId);
            pstmt.setInt(idx++, empId);
            if (!"all".equals(status))                    pstmt.setString(idx++, status);
            if (keyword  != null && !keyword.isEmpty())   pstmt.setString(idx++, "%" + keyword + "%");
            if (deptName != null && !deptName.isEmpty())  pstmt.setString(idx++, deptName);
            rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }

    // 부서장용 퇴직 처리 결과 목록
    public List<ResignDTO> getResignDoneListForDeptManager(Connection con, int empId,
            String status, String keyword, String deptName,
            int offset, int pageSize) throws SQLException {
        StringBuilder sql = new StringBuilder(RESIGN_SELECT)
            .append("WHERE (r.dept_manager_id = ? OR r.emp_id = ?) ")
            .append(DONE);
        if (!"all".equals(status))                    sql.append("AND r.status = ? ");
        if (keyword  != null && !keyword.isEmpty())   sql.append("AND e.emp_name LIKE ? ");
        if (deptName != null && !deptName.isEmpty())  sql.append("AND d.dept_name = ? ");
        sql.append("ORDER BY r.created_at DESC LIMIT ? OFFSET ?");

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<ResignDTO> list = new ArrayList<>();
        try {
            pstmt = con.prepareStatement(sql.toString());
            int idx = 1;
            pstmt.setInt(idx++, empId);
            pstmt.setInt(idx++, empId);
            if (!"all".equals(status))                    pstmt.setString(idx++, status);
            if (keyword  != null && !keyword.isEmpty())   pstmt.setString(idx++, "%" + keyword + "%");
            if (deptName != null && !deptName.isEmpty())  pstmt.setString(idx++, deptName);
            pstmt.setInt(idx++, pageSize);
            pstmt.setInt(idx,   offset);
            rs = pstmt.executeQuery();
            while (rs.next()) list.add(mapResignRow(rs));
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
        return list;
    }

    // ────────────────────────────────────────────────
    // 내 신청 현황 — 미완료
    // ────────────────────────────────────────────────

    // 내 휴직/복직 신청 목록 (미완료)
    public List<LeaveDTO> getMyLeaveList(Connection con, int empId,
            String leaveType, String status) throws SQLException {
        StringBuilder sql = new StringBuilder(LEAVE_SELECT)
            .append("WHERE r.emp_id = ? ")
            .append(IN_PROGRESS);
        if (leaveType != null && !leaveType.isEmpty()) sql.append("AND r.leave_type = ? ");
        if (!"all".equals(status))                     sql.append("AND r.status = ? ");
        sql.append("ORDER BY r.created_at DESC");

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<LeaveDTO> list = new ArrayList<>();
        try {
            pstmt = con.prepareStatement(sql.toString());
            int idx = 1;
            pstmt.setInt(idx++, empId);
            if (leaveType != null && !leaveType.isEmpty()) pstmt.setString(idx++, leaveType);
            if (!"all".equals(status))                     pstmt.setString(idx++, status);
            rs = pstmt.executeQuery();
            while (rs.next()) list.add(mapLeaveRow(rs));
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
        return list;
    }

    // 내 퇴직 신청 목록 (미완료)
    public List<ResignDTO> getMyResignList(Connection con, int empId,
            String status) throws SQLException {
        StringBuilder sql = new StringBuilder(RESIGN_SELECT)
            .append("WHERE r.emp_id = ? ")
            .append(IN_PROGRESS);
        if (!"all".equals(status)) sql.append("AND r.status = ? ");
        sql.append("ORDER BY r.created_at DESC");

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<ResignDTO> list = new ArrayList<>();
        try {
            pstmt = con.prepareStatement(sql.toString());
            int idx = 1;
            pstmt.setInt(idx++, empId);
            if (!"all".equals(status)) pstmt.setString(idx++, status);
            rs = pstmt.executeQuery();
            while (rs.next()) list.add(mapResignRow(rs));
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
        return list;
    }

    // ────────────────────────────────────────────────
    // 내 신청 처리 결과 — 완료 + 검색 + 페이징
    // ────────────────────────────────────────────────

    // 내 휴직/복직 처리 결과 건수
    public int getMyLeaveDoneCount(Connection con, int empId,
            String leaveType, String status) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) " +
            "FROM leave_of_absence_request r " +
            "JOIN employee e     ON r.emp_id      = e.emp_id " +
            "JOIN department d   ON e.dept_id      = d.dept_id " +
            "JOIN job_position p ON e.position_id  = p.position_id " +
            "WHERE r.emp_id = ? ").append(DONE);
        if (leaveType != null && !leaveType.isEmpty()) sql.append("AND r.leave_type = ? ");
        if (!"all".equals(status))                     sql.append("AND r.status = ? ");

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql.toString());
            int idx = 1;
            pstmt.setInt(idx++, empId);
            if (leaveType != null && !leaveType.isEmpty()) pstmt.setString(idx++, leaveType);
            if (!"all".equals(status))                     pstmt.setString(idx++, status);
            rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }

    // 내 휴직/복직 처리 결과 목록
    public List<LeaveDTO> getMyLeaveDoneList(Connection con, int empId,
            String leaveType, String status,
            int offset, int pageSize) throws SQLException {
        StringBuilder sql = new StringBuilder(LEAVE_SELECT)
            .append("WHERE r.emp_id = ? ").append(DONE);
        if (leaveType != null && !leaveType.isEmpty()) sql.append("AND r.leave_type = ? ");
        if (!"all".equals(status))                     sql.append("AND r.status = ? ");
        sql.append("ORDER BY r.created_at DESC LIMIT ? OFFSET ?");

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<LeaveDTO> list = new ArrayList<>();
        try {
            pstmt = con.prepareStatement(sql.toString());
            int idx = 1;
            pstmt.setInt(idx++, empId);
            if (leaveType != null && !leaveType.isEmpty()) pstmt.setString(idx++, leaveType);
            if (!"all".equals(status))                     pstmt.setString(idx++, status);
            pstmt.setInt(idx++, pageSize);
            pstmt.setInt(idx,   offset);
            rs = pstmt.executeQuery();
            while (rs.next()) list.add(mapLeaveRow(rs));
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
        return list;
    }

    // 내 퇴직 처리 결과 건수
    public int getMyResignDoneCount(Connection con, int empId,
            String status) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) " +
            "FROM resign_request r " +
            "JOIN employee e     ON r.emp_id      = e.emp_id " +
            "JOIN department d   ON e.dept_id      = d.dept_id " +
            "JOIN job_position p ON e.position_id  = p.position_id " +
            "WHERE r.emp_id = ? ").append(DONE);
        if (!"all".equals(status)) sql.append("AND r.status = ? ");

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql.toString());
            int idx = 1;
            pstmt.setInt(idx++, empId);
            if (!"all".equals(status)) pstmt.setString(idx++, status);
            rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }

    // 내 퇴직 처리 결과 목록
    public List<ResignDTO> getMyResignDoneList(Connection con, int empId,
            String status, int offset, int pageSize) throws SQLException {
        StringBuilder sql = new StringBuilder(RESIGN_SELECT)
            .append("WHERE r.emp_id = ? ").append(DONE);
        if (!"all".equals(status)) sql.append("AND r.status = ? ");
        sql.append("ORDER BY r.created_at DESC LIMIT ? OFFSET ?");

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<ResignDTO> list = new ArrayList<>();
        try {
            pstmt = con.prepareStatement(sql.toString());
            int idx = 1;
            pstmt.setInt(idx++, empId);
            if (!"all".equals(status)) pstmt.setString(idx++, status);
            pstmt.setInt(idx++, pageSize);
            pstmt.setInt(idx,   offset);
            rs = pstmt.executeQuery();
            while (rs.next()) list.add(mapResignRow(rs));
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
        return list;
    }

    // ────────────────────────────────────────────────
    // 매핑
    // ────────────────────────────────────────────────

    private LeaveDTO mapLeaveRow(ResultSet rs) throws SQLException {
        LeaveDTO dto = new LeaveDTO();
        dto.setRequest_id(rs.getInt("request_id"));
        dto.setEmp_id(rs.getInt("emp_id"));
        dto.setLeave_type(rs.getString("leave_type"));
        dto.setStart_date(rs.getString("start_date"));
        dto.setEnd_date(rs.getString("end_date"));
        dto.setReason(rs.getString("reason"));
        dto.setStatus(rs.getString("status"));
        dto.setDept_manager_id(rs.getInt("dept_manager_id"));
        dto.setHr_manager_id(rs.getInt("hr_manager_id"));
        dto.setReject_reason(rs.getString("reject_reason"));
        dto.setEmp_name(rs.getString("emp_name"));
        dto.setEmp_no(rs.getString("emp_no"));
        dto.setDept_name(rs.getString("dept_name"));
        dto.setPosition_name(rs.getString("position_name"));
        dto.setHrDept(rs.getBoolean("is_hr_dept"));
        dto.setReqIsPresident(rs.getBoolean("is_req_president"));
        dto.setCreated_at(rs.getTimestamp("created_at") != null
            ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        dto.setDept_approved_at(rs.getTimestamp("dept_approved_at") != null
            ? rs.getTimestamp("dept_approved_at").toLocalDateTime() : null);
        dto.setHr_approved_at(rs.getTimestamp("hr_approved_at") != null
            ? rs.getTimestamp("hr_approved_at").toLocalDateTime() : null);
        dto.setPresident_approved_at(rs.getTimestamp("president_approved_at") != null
        	    ? rs.getTimestamp("president_approved_at").toLocalDateTime() : null);
        return dto;
    }

    private ResignDTO mapResignRow(ResultSet rs) throws SQLException {
        ResignDTO dto = new ResignDTO();
        dto.setRequest_id(rs.getInt("request_id"));
        dto.setEmp_id(rs.getInt("emp_id"));
        dto.setResign_date(rs.getString("resign_date"));
        dto.setReason(rs.getString("reason"));
        dto.setStatus(rs.getString("status"));
        dto.setDept_manager_id(rs.getInt("dept_manager_id"));
        dto.setHr_manager_id(rs.getInt("hr_manager_id"));
        dto.setReject_reason(rs.getString("reject_reason"));
        dto.setEmp_name(rs.getString("emp_name"));
        dto.setEmp_no(rs.getString("emp_no"));
        dto.setDept_name(rs.getString("dept_name"));
        dto.setPosition_name(rs.getString("position_name"));
        dto.setHrDept(rs.getBoolean("is_hr_dept"));
        dto.setReqIsPresident(rs.getBoolean("is_req_president"));
        dto.setCreated_at(rs.getTimestamp("created_at") != null
            ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        dto.setDept_approved_at(rs.getTimestamp("dept_approved_at") != null
            ? rs.getTimestamp("dept_approved_at").toLocalDateTime() : null);
        dto.setHr_approved_at(rs.getTimestamp("hr_approved_at") != null
            ? rs.getTimestamp("hr_approved_at").toLocalDateTime() : null);
        dto.setPresident_approved_at(rs.getTimestamp("president_approved_at") != null
        	    ? rs.getTimestamp("president_approved_at").toLocalDateTime() : null);
        return dto;
    }

    // ────────────────────────────────────────────────
    // 상세 조회
    // ────────────────────────────────────────────────

    public LeaveDTO getLeaveDetail(Connection con, int requestId) throws SQLException {
        String sql = "SELECT r.*, " +
                     "e.emp_name, e.emp_no, d.dept_name, p.position_name, " +
                     "(d.dept_name = '인사팀') AS is_hr_dept, " +
                     "(SELECT COUNT(*) FROM account a WHERE a.emp_id = e.emp_id AND a.role = '최종승인자') > 0 AS is_req_president, " +
                     "dm.emp_name AS dept_manager_name, " +
                     "hm.emp_name AS hr_manager_name, " +
                     "pres.emp_name AS president_name " +
                     "FROM leave_of_absence_request r " +
                     "JOIN employee e       ON r.emp_id         = e.emp_id " +
                     "JOIN department d     ON e.dept_id         = d.dept_id " +
                     "JOIN job_position p   ON e.position_id     = p.position_id " +
                     "LEFT JOIN employee dm ON r.dept_manager_id = dm.emp_id " +
                     "LEFT JOIN employee hm ON r.hr_manager_id   = hm.emp_id " +
                     "LEFT JOIN employee pres ON r.president_id       = pres.emp_id " +
                     "WHERE r.request_id = ?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, requestId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                LeaveDTO dto = mapLeaveRow(rs);
                dto.setDept_manager_name(rs.getString("dept_manager_name"));
                dto.setHr_manager_name(rs.getString("hr_manager_name"));
                dto.setPresident_name(rs.getString("president_name"));
                return dto;
            }
            return null;
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }

    public ResignDTO getResignDetail(Connection con, int requestId) throws SQLException {
        String sql = "SELECT r.*, " +
                     "e.emp_name, e.emp_no, d.dept_name, p.position_name, " +
                     "(d.dept_name = '인사팀') AS is_hr_dept, " +
                     "(SELECT COUNT(*) FROM account a WHERE a.emp_id = e.emp_id AND a.role = '최종승인자') > 0 AS is_req_president, " +
                     "dm.emp_name AS dept_manager_name, " +
                     "hm.emp_name AS hr_manager_name, " +
                     "pres.emp_name AS president_name " +
                     "FROM resign_request r " +
                     "JOIN employee e       ON r.emp_id         = e.emp_id " +
                     "JOIN department d     ON e.dept_id         = d.dept_id " +
                     "JOIN job_position p   ON e.position_id     = p.position_id " +
                     "LEFT JOIN employee dm ON r.dept_manager_id = dm.emp_id " +
                     "LEFT JOIN employee hm ON r.hr_manager_id   = hm.emp_id " +
                     "LEFT JOIN employee pres ON r.president_id       = pres.emp_id " +
                     "WHERE r.request_id = ?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, requestId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                ResignDTO dto = mapResignRow(rs);
                dto.setDept_manager_name(rs.getString("dept_manager_name"));
                dto.setHr_manager_name(rs.getString("hr_manager_name"));
                dto.setPresident_name(rs.getString("president_name"));
                return dto;
            }
            return null;
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }
}