package com.hrms.emp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.hrms.emp.dto.HistoryDTO;
import com.hrms.emp.dto.TransferDTO;
import com.hrms.org.dto.DeptDTO;

public class TransferDAO {

	/**
	 * 1. 사원 테이블의 부서/직급 정보 수정 (UPDATE employee) 설계서 흐름도: [사원정보 수정] 단계
	 */
	public int updateEmployeePosition(Connection con, String empNo, int toDeptId, int toPositionId)
			throws SQLException {
		String sql = "UPDATE employee SET dept_id=?, position_id=? WHERE emp_no=?";
		PreparedStatement pstmt = null;
		try {
			pstmt = con.prepareStatement(sql);
			pstmt.setInt(1, toDeptId);
			pstmt.setInt(2, toPositionId);
			pstmt.setString(3, empNo);
			return pstmt.executeUpdate();
		} finally {
			if (pstmt != null)
				pstmt.close();
		}
	}

	/**
	 * 2. 발령 이력 테이블에 내역 삽입 (INSERT INTO personnel_history) 설계서 흐름도: [발력이력 등록] 단계
	 */
	public int insertPersonnelHistory(Connection con, HistoryDTO dto) throws SQLException {
	    String sql = "INSERT INTO personnel_history " +
	                 "(emp_id, change_type, change_date, from_dept_id, to_dept_id, " +
	                 "from_position_id, to_position_id, reason, approved_by) " +
	                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
	    PreparedStatement pstmt = null;
	    try {
	        pstmt = con.prepareStatement(sql);
	        pstmt.setInt(1, dto.getEmp_id());
	        pstmt.setString(2, dto.getChange_type());
	        pstmt.setObject(3, dto.getChange_date()); // LocalDateTime → DATE
	        pstmt.setInt(4, dto.getFrom_dept_id());
	        pstmt.setInt(5, dto.getTo_dept_id());
	        pstmt.setInt(6, dto.getFrom_position_id());
	        pstmt.setInt(7, dto.getTo_position_id());
	        pstmt.setString(8, dto.getReason());
	        pstmt.setInt(9, dto.getApproved_by());
	        return pstmt.executeUpdate();
	    } finally {
	        if (pstmt != null) pstmt.close();
	    }
	}

	public List<DeptDTO> getDeptList(Connection con) throws SQLException {
		List<DeptDTO> list = new ArrayList<>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String sql = "SELECT dept_id, dept_name FROM department ORDER BY dept_name ASC";
		try {
			pstmt = con.prepareStatement(sql);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				DeptDTO dto = new DeptDTO();
				dto.setDept_id(rs.getInt("dept_id"));
				dto.setDept_name(rs.getString("dept_name"));
				list.add(dto);
			}
		} finally {
			if (rs != null)
				rs.close();
			if (pstmt != null)
				pstmt.close();
		}
		return list;
	}

	public List<PositionDTO> getPositionList(Connection con) throws SQLException {
		List<PositionDTO> list = new ArrayList<>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String sql = "SELECT position_id, position_name FROM job_position ORDER BY position_id ASC";
		try {
			pstmt = con.prepareStatement(sql);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				PositionDTO dto = new PositionDTO();
				dto.setPosition_id(rs.getInt("position_id"));
				dto.setPosition_name(rs.getString("position_name"));
				list.add(dto);
			}
		} finally {
			if (rs != null)
				rs.close();
			if (pstmt != null)
				pstmt.close();
		}
		return list;
	}
}