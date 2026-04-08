package com.hrms.emp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.hrms.emp.dto.LeaveDTO;

public class LeaveDAO {

    // department.manager_id로 부서장 이름 조회
    public String getDeptManagerName(Connection con, int deptId) throws SQLException {
        String sql = "SELECT e.emp_name " +
                     "FROM department d " +
                     "JOIN employee e ON d.manager_id = e.emp_id " +
                     "WHERE d.dept_id = ?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, deptId);
            rs = pstmt.executeQuery();
            return rs.next() ? rs.getString("emp_name") : "미지정";
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }
    
    // 부서장 emp_id 조회 (신청자 emp_id로 소속 부서 찾아서 부서장 ID 반환)
    public int getDeptManagerId(Connection con, int empId) throws SQLException {
        String sql = "SELECT d.manager_id " +
                     "FROM employee e " +
                     "JOIN department d ON e.dept_id = d.dept_id " +
                     "WHERE e.emp_id = ?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, empId);
            rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt("manager_id") : 0;
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }
    
    // 휴직/복직 신청 INSERT
    public int insertLeaveRequest(Connection con, LeaveDTO dto) throws SQLException {
        String sql = "INSERT INTO leave_of_absence_request " +
                     "(emp_id, leave_type, start_date, end_date, reason, " +
                     "status, dept_manager_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement pstmt = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, dto.getEmp_id());
            pstmt.setString(2, dto.getLeave_type());
            pstmt.setString(3, dto.getStart_date());
            // end_date는 복직 시 null 가능
            if (dto.getEnd_date() != null) {
                pstmt.setString(4, dto.getEnd_date());
            } else {
                pstmt.setNull(4, java.sql.Types.DATE);
            }
            pstmt.setString(5, dto.getReason());
            pstmt.setString(6, dto.getStatus());
            // dept_manager_id가 0이면 null로 처리
            if (dto.getDept_manager_id() > 0) {
                pstmt.setInt(7, dto.getDept_manager_id());
            } else {
                pstmt.setNull(7, java.sql.Types.INTEGER);
            }
            return pstmt.executeUpdate();
        } finally {
            if (pstmt != null) pstmt.close();
        }
    }
    
    // 대기 중인 본인의 휴직,복직 신청 철회
    public int withdrawLeave(Connection con, int requestId, int empId) throws SQLException {
        String sql = "DELETE FROM leave_of_absence_request " +
                     "WHERE request_id = ? AND emp_id = ? AND status = '대기'";
        PreparedStatement pstmt = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, requestId);
            pstmt.setInt(2, empId);
            return pstmt.executeUpdate();
        } finally {
            if (pstmt != null) pstmt.close();
        }
    }
    
    public LeaveDTO getLeaveById(Connection con, int requestId, int empId) throws SQLException {
        String sql = "SELECT * FROM leave_of_absence_request " +
                     "WHERE request_id = ? AND emp_id = ? AND status = '대기'";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, requestId);
            pstmt.setInt(2, empId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                LeaveDTO dto = new LeaveDTO();
                dto.setRequest_id(rs.getInt("request_id"));
                dto.setEmp_id(rs.getInt("emp_id"));
                dto.setLeave_type(rs.getString("leave_type"));
                dto.setStart_date(rs.getString("start_date"));
                dto.setEnd_date(rs.getString("end_date"));
                dto.setReason(rs.getString("reason"));
                dto.setStatus(rs.getString("status"));
                return dto;
            }
            return null;
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }

    // 대기 중인 휴직/복직요청 변경(수정)
    public int updateLeaveRequest(Connection con, LeaveDTO dto) throws SQLException {
        String sql = "UPDATE leave_of_absence_request " +
                     "SET leave_type=?, start_date=?, end_date=?, reason=? " +
                     "WHERE request_id=? AND emp_id=? AND status='대기'";
        PreparedStatement pstmt = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, dto.getLeave_type());
            pstmt.setString(2, dto.getStart_date());
            if (dto.getEnd_date() != null) pstmt.setString(3, dto.getEnd_date());
            else pstmt.setNull(3, java.sql.Types.DATE);
            pstmt.setString(4, dto.getReason());
            pstmt.setInt(5, dto.getRequest_id());
            pstmt.setInt(6, dto.getEmp_id());
            return pstmt.executeUpdate();
        } finally {
            if (pstmt != null) pstmt.close();
        }
    }
    
    // 대기 중인 휴직/복직 신청이 있는지 확인
    public boolean hasPendingLeave(Connection con, int empId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM leave_of_absence_request " +
                     "WHERE emp_id = ? AND status NOT IN ('최종승인', '반려')";
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
    
    // 직원 현재 상태 조회
    public String getEmpStatus(Connection con, int empId) throws SQLException {
        String sql = "SELECT status FROM employee WHERE emp_id = ?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, empId);
            rs = pstmt.executeQuery();
            return rs.next() ? rs.getString("status") : null;
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }
}