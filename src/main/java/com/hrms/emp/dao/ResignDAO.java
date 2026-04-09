package com.hrms.emp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.hrms.emp.dto.ResignDTO;

public class ResignDAO {

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
            if (rs.next()) {
                return rs.getString("emp_name");
            }
            return "미지정"; // 부서장이 없는 경우
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }
	
    // 퇴직 신청
    public int insertResignRequest(Connection con, ResignDTO dto) throws SQLException {
        String sql = "INSERT INTO resign_request " +
                     "(emp_id, resign_date, reason, status, dept_manager_id) " +
                     "VALUES (?, ?, ?, '대기', ?)";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setInt(1, dto.getEmp_id());
            pstmt.setString(2, dto.getResign_date());
            pstmt.setString(3, dto.getReason());
            if (dto.getDept_manager_id() > 0) {
                pstmt.setInt(4, dto.getDept_manager_id());
            } else {
                pstmt.setNull(4, java.sql.Types.INTEGER);
            }
            pstmt.executeUpdate();
            rs = pstmt.getGeneratedKeys(); // ← 추가
            return rs.next() ? rs.getInt(1) : 0; // ← request_id 반환
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }
    
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
    
    // 대기 중인 퇴직 신청이 있는지 확인
    public boolean hasPendingResign(Connection con, int empId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM resign_request " +
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
    
    // 대기 중인 퇴직 신청 철회
    public int withdrawResign(Connection con, int requestId, int empId) throws SQLException {
        String sql = "DELETE FROM resign_request " +
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
}
