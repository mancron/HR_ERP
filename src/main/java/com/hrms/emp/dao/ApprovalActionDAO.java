package com.hrms.emp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.hrms.emp.dto.HistoryDTO;

public class ApprovalActionDAO {

    // 부서장 여부
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

    // 신청자가 인사팀 소속인지 확인
    // department 테이블에서 인사팀 dept_id를 찾아 비교
    public boolean isHrDept(Connection con, String type, int requestId) throws SQLException {
        String table = "leave".equals(type) ? "leave_of_absence_request" : "resign_request";
        String sql = "SELECT COUNT(*) FROM " + table + " r " +
                     "JOIN employee e ON r.emp_id = e.emp_id " +
                     "JOIN department d ON e.dept_id = d.dept_id " +
                     "WHERE r.request_id = ? AND d.dept_name = '인사팀'";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, requestId);
            rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }

    // 현재 status 조회
    public String getStatus(Connection con, String type, int requestId) throws SQLException {
        String table = "leave".equals(type) ? "leave_of_absence_request" : "resign_request";
        String sql = "SELECT status FROM " + table + " WHERE request_id = ?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, requestId);
            rs = pstmt.executeQuery();
            return rs.next() ? rs.getString("status") : null;
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }

    // 신청자 emp_id 조회
    public int getEmpId(Connection con, String type, int requestId) throws SQLException {
        String table = "leave".equals(type) ? "leave_of_absence_request" : "resign_request";
        String sql = "SELECT emp_id FROM " + table + " WHERE request_id = ?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, requestId);
            rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt("emp_id") : 0;
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }

    // dept_manager_id 조회
    public int getDeptManagerId(Connection con, String type, int requestId) throws SQLException {
        String table = "leave".equals(type) ? "leave_of_absence_request" : "resign_request";
        String sql = "SELECT dept_manager_id FROM " + table + " WHERE request_id = ?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, requestId);
            rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt("dept_manager_id") : 0;
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }

    // 부서장 승인: 대기 → 부서장승인
    public int approveDeptManager(Connection con, String type,
                                   int requestId, int loginEmpId) throws SQLException {
        String table = "leave".equals(type) ? "leave_of_absence_request" : "resign_request";
        String sql = "UPDATE " + table + " SET status='부서장승인', " +
                     "dept_manager_id=?, dept_approved_at=NOW() " +
                     "WHERE request_id=? AND status='대기'";
        PreparedStatement pstmt = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, loginEmpId);
            pstmt.setInt(2, requestId);
            return pstmt.executeUpdate();
        } finally {
            if (pstmt != null) pstmt.close();
        }
    }

    // HR담당자 승인: 부서장승인 → HR담당자승인
    public int approveHrManager(Connection con, String type,
                                 int requestId, int loginEmpId) throws SQLException {
        String table = "leave".equals(type) ? "leave_of_absence_request" : "resign_request";
        String sql = "UPDATE " + table + " SET status='HR담당자승인', " +
                     "hr_manager_id=?, hr_approved_at=NOW() " +
                     "WHERE request_id=? AND status='부서장승인'";
        PreparedStatement pstmt = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, loginEmpId);
            pstmt.setInt(2, requestId);
            return pstmt.executeUpdate();
        } finally {
            if (pstmt != null) pstmt.close();
        }
    }

    // 관리자 최종 승인
    // 일반 부서: HR담당자승인 → 최종승인
    // 인사팀:   부서장승인   → 최종승인
    public int approvePresident(Connection con, String type,
                             int requestId, int loginEmpId) throws SQLException {
        String table = "leave".equals(type) ? "leave_of_absence_request" : "resign_request";
        String sql = "UPDATE " + table + " SET status='최종승인', " +
                     "president_id=?, president_approved_at=NOW() " +
                     "WHERE request_id=? AND status IN ('HR담당자승인', '부서장승인')";
        PreparedStatement pstmt = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, loginEmpId);
            pstmt.setInt(2, requestId);
            return pstmt.executeUpdate();
        } finally {
            if (pstmt != null) pstmt.close();
        }
    }

    // 반려
    public int reject(Connection con, String type,
                      int requestId, String rejectReason) throws SQLException {
        String table = "leave".equals(type) ? "leave_of_absence_request" : "resign_request";
        String sql = "UPDATE " + table + " SET status='반려', reject_reason=? " +
                     "WHERE request_id=?";
        PreparedStatement pstmt = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, rejectReason);
            pstmt.setInt(2, requestId);
            return pstmt.executeUpdate();
        } finally {
            if (pstmt != null) pstmt.close();
        }
    }

    // 휴직 유형 조회
    public String getLeaveType(Connection con, int requestId) throws SQLException {
        String sql = "SELECT leave_type FROM leave_of_absence_request WHERE request_id=?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, requestId);
            rs = pstmt.executeQuery();
            return rs.next() ? rs.getString("leave_type") : null;
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }

    // 희망 퇴직일 조회
    public String getResignDate(Connection con, int requestId) throws SQLException {
        String sql = "SELECT resign_date FROM resign_request WHERE request_id=?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, requestId);
            rs = pstmt.executeQuery();
            return rs.next() ? rs.getString("resign_date") : null;
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }

    // 인사승인 완료 후 employee status 변경
    public int updateEmployeeStatus(Connection con, int empId,
                                     String status) throws SQLException {
        String sql = "UPDATE employee SET status=? WHERE emp_id=?";
        PreparedStatement pstmt = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, status);
            pstmt.setInt(2, empId);
            return pstmt.executeUpdate();
        } finally {
            if (pstmt != null) pstmt.close();
        }
    }

    // 퇴직 처리
    public int updateEmployeeResign(Connection con, int empId,
                                     String resignDate) throws SQLException {
        String sql = "UPDATE employee SET status='퇴직', resign_date=? WHERE emp_id=?";
        PreparedStatement pstmt = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, resignDate);
            pstmt.setInt(2, empId);
            return pstmt.executeUpdate();
        } finally {
            if (pstmt != null) pstmt.close();
        }
    }
    
    // 신청자가 관리자인지 확인
    public boolean isPresident(Connection con, String type, int requestId) throws SQLException {
        String table = "leave".equals(type) ? "leave_of_absence_request" : "resign_request";
        String sql = "SELECT COUNT(*) FROM " + table + " r " +
                     "JOIN employee e ON r.emp_id = e.emp_id " +
                     "JOIN account a  ON e.emp_id = a.emp_id " +
                     "WHERE r.request_id = ? AND a.role = '최종승인자'";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, requestId);
            rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }

    // 부서장 자가승인용 — 신청자 본인이 해당 부서 부서장인지 확인
    public boolean isSelfDeptManager(Connection con, String type,
                                      int requestId, int loginEmpId) throws SQLException {
        String table = "leave".equals(type) ? "leave_of_absence_request" : "resign_request";
        String sql = "SELECT COUNT(*) FROM " + table + " r " +
                     "JOIN employee e   ON r.emp_id  = e.emp_id " +
                     "JOIN department d ON e.dept_id = d.dept_id " +
                     "WHERE r.request_id = ? " +
                     "AND r.emp_id = ? " +          // 신청자 본인
                     "AND d.manager_id = ?";         // 본인이 부서장
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, requestId);
            pstmt.setInt(2, loginEmpId);
            pstmt.setInt(3, loginEmpId);
            rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }
    
    // 관리자 신청용 메서드
    public int approveHrManagerForPresident(Connection con, String type,
                                         int requestId, int loginEmpId) throws SQLException {
        String table = "leave".equals(type) ? "leave_of_absence_request" : "resign_request";
        String sql = "UPDATE " + table + " SET status='최종승인', " +
                     "hr_manager_id=?, hr_approved_at=NOW() " +
                     "WHERE request_id=? AND status='대기'"; // ← 대기에서 바로 확정
        PreparedStatement pstmt = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, loginEmpId);
            pstmt.setInt(2, requestId);
            return pstmt.executeUpdate();
        } finally {
            if (pstmt != null) pstmt.close();
        }
    }
    
    //휴직,복직,퇴직이 최종 승인됐을 때 인사발령 이력에 입력
    public int insertPersonnelHistory(Connection con, HistoryDTO dto) throws SQLException {
        String sql = "INSERT INTO personnel_history " +
                     "(emp_id, change_type, change_date, from_dept_id, to_dept_id, " +
                     "from_position_id, from_role, to_position_id, to_role, reason, approved_by) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement pstmt = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, dto.getEmp_id());
            pstmt.setString(2, dto.getChange_type());
            pstmt.setObject(3, dto.getChange_date());
            pstmt.setObject(4, dto.getFrom_dept_id() > 0 ? dto.getFrom_dept_id() : null, java.sql.Types.INTEGER);
            pstmt.setObject(5, dto.getTo_dept_id() > 0 ? dto.getTo_dept_id() : null, java.sql.Types.INTEGER);
            pstmt.setObject(6, dto.getFrom_position_id() > 0 ? dto.getFrom_position_id() : null, java.sql.Types.INTEGER);
            pstmt.setObject(7, dto.getFrom_role(), java.sql.Types.VARCHAR);
            pstmt.setObject(8, dto.getTo_position_id() > 0 ? dto.getTo_position_id() : null, java.sql.Types.INTEGER);
            pstmt.setObject(9, dto.getTo_role(), java.sql.Types.VARCHAR);
            pstmt.setString(10, dto.getReason());
            pstmt.setObject(11, dto.getApproved_by() > 0 ? dto.getApproved_by() : null, java.sql.Types.INTEGER);
            return pstmt.executeUpdate();
        } finally {
            if (pstmt != null) pstmt.close();
        }
    }
    
    // 직원의 현재 dept_id, status 조회
    public int[] getEmpDeptAndStatus(Connection con, int empId) throws SQLException {
        String sql = "SELECT dept_id FROM employee WHERE emp_id = ?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, empId);
            rs = pstmt.executeQuery();
            if (rs.next()) return new int[]{ rs.getInt("dept_id") };
            return new int[]{ 0 };
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }
    
 // 휴직/복직 신청 사유 조회
    public String getLeaveReason(Connection con, int requestId) throws SQLException {
        String sql = "SELECT reason FROM leave_of_absence_request WHERE request_id = ?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, requestId);
            rs = pstmt.executeQuery();
            return rs.next() ? rs.getString("reason") : null;
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }

    // 퇴직 신청 사유 조회
    public String getResignReason(Connection con, int requestId) throws SQLException {
        String sql = "SELECT reason FROM resign_request WHERE request_id = ?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, requestId);
            rs = pstmt.executeQuery();
            return rs.next() ? rs.getString("reason") : null;
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }
    
    // 휴직,복직,퇴직 때 신청했던 발령 날짜 세팅
    public String getLeaveStartDate(Connection con, int requestId) throws SQLException {
        String sql = "SELECT start_date FROM leave_of_absence_request WHERE request_id = ?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, requestId);
            rs = pstmt.executeQuery();
            return rs.next() ? rs.getString("start_date") : null;
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }
    
    // 직원의 현재 position_id, 직책 조회
    public int[] getEmpPositionAndRole(Connection con, int empId) throws SQLException {
        String sql = "SELECT e.position_id, " +
                     "(SELECT COUNT(*) FROM department WHERE manager_id = e.emp_id AND is_active = 1) AS is_manager " +
                     "FROM employee e WHERE e.emp_id = ?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, empId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return new int[]{
                    rs.getInt("position_id"),
                    rs.getInt("is_manager")  // 0이면 일반, 1이면 부서장
                };
            }
            return new int[]{ 0, 0 };
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }
    
}