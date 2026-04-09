package com.hrms.emp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.hrms.emp.dto.EmpDTO;
import com.hrms.emp.dto.HistoryDTO;
public class TransferDAO {

	/**
	 * 1. 사원 테이블의 부서/직급 정보 수정 (UPDATE employee) 설계서 흐름도: [사원정보 수정] 단계
	 */
	public int updateEmployeePosition(Connection con, String empNo,
            int toDeptId, int toPositionId) throws SQLException {
		String sql = "UPDATE employee SET dept_id=?, position_id=? WHERE emp_no=?";
		PreparedStatement pstmt = null;
		try {
			pstmt = con.prepareStatement(sql);
			pstmt.setInt(1, toDeptId);
			pstmt.setInt(2, toPositionId);
			pstmt.setString(3, empNo);
			return pstmt.executeUpdate();
		} finally {
			if (pstmt != null) pstmt.close();
		}
	}

	/**
	 * 2. 발령 이력 테이블에 내역 삽입 (INSERT INTO personnel_history) 설계서 흐름도: [발력이력 등록] 단계
	 */
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
        	pstmt.setInt(4, dto.getFrom_dept_id());
        	pstmt.setInt(5, dto.getTo_dept_id());
        	pstmt.setInt(6, dto.getFrom_position_id());
        	pstmt.setString(7, dto.getFrom_role());
        	pstmt.setInt(8, dto.getTo_position_id());
        	pstmt.setString(9, dto.getTo_role());
        	pstmt.setString(10, dto.getReason());
        	pstmt.setInt(11, dto.getApproved_by());
            return pstmt.executeUpdate();
        } finally {
            if (pstmt != null) pstmt.close();
        }
    }

	//부서 목록 가져오기
	public List<EmpDTO> getDeptList(Connection con) throws SQLException {
        List<EmpDTO> list = new ArrayList<>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String sql = "SELECT dept_id, dept_name FROM department ORDER BY dept_name ASC";
        try {
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                EmpDTO dto = new EmpDTO();
                dto.setDept_id(rs.getInt("dept_id"));
                dto.setDept_name(rs.getString("dept_name"));
                list.add(dto);
            }
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
        return list;
    }

	//직급 목록 가져오기
	public List<EmpDTO> getPositionList(Connection con) throws SQLException {
        List<EmpDTO> list = new ArrayList<>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String sql = "SELECT position_id, position_name, base_salary FROM job_position ORDER BY position_level DESC";
        try {
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                EmpDTO dto = new EmpDTO();
                dto.setPosition_id(rs.getInt("position_id"));
                dto.setPosition_name(rs.getString("position_name"));
                dto.setBase_salary(rs.getInt("base_salary")); // ← 추가
                list.add(dto);
            }
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
        return list;
    }
	
	
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
	
	// 발령 직책을 DB에 반영
	public int updateDeptManager(Connection con, int deptId, int empId) throws SQLException {
	    String sql = "UPDATE department SET manager_id = ? WHERE dept_id = ?";
	    PreparedStatement pstmt = null;
	    try {
	        pstmt = con.prepareStatement(sql);
	        pstmt.setInt(1, empId);
	        pstmt.setInt(2, deptId);
	        return pstmt.executeUpdate();
	    } finally {
	        if (pstmt != null) pstmt.close();
	    }
	}
	
	// 기존 부서장 해제 — 발령 대상자가 기존 부서장이었을 때 manager_id를 NULL로 초기화
	public int clearDeptManager(Connection con, int deptId) throws SQLException {
	    String sql = "UPDATE department SET manager_id = NULL WHERE dept_id = ?";
	    PreparedStatement pstmt = null;
	    try {
	        pstmt = con.prepareStatement(sql);
	        pstmt.setInt(1, deptId);
	        return pstmt.executeUpdate();
	    } finally {
	        if (pstmt != null) pstmt.close();
	    }
	}
	
	// 해당 부서의 현재 부서장 emp_id 조회 (없으면 0 반환)
	public int getCurrentDeptManagerId(Connection con, int deptId) throws SQLException {
	    String sql = "SELECT manager_id FROM department WHERE dept_id = ?";
	    PreparedStatement pstmt = null;
	    ResultSet rs = null;
	    try {
	        pstmt = con.prepareStatement(sql);
	        pstmt.setInt(1, deptId);
	        rs = pstmt.executeQuery();
	        if (rs.next()) {
	            int managerId = rs.getInt("manager_id");
	            return rs.wasNull() ? 0 : managerId;
	        }
	        return 0;
	    } finally {
	        if (rs != null) rs.close();
	        if (pstmt != null) pstmt.close();
	    }
	}

	// 기존 부서장의 현재 부서/직급/직책 정보 조회 (이력 기록용)
	public HistoryDTO getEmpInfoForHistory(Connection con, int empId) throws SQLException {
	    String sql = "SELECT e.dept_id, e.position_id, " +
	                 "(SELECT COUNT(*) FROM department WHERE manager_id = e.emp_id AND is_active = 1) AS is_manager " +
	                 "FROM employee e WHERE e.emp_id = ?";
	    PreparedStatement pstmt = null;
	    ResultSet rs = null;
	    try {
	        pstmt = con.prepareStatement(sql);
	        pstmt.setInt(1, empId);
	        rs = pstmt.executeQuery();
	        if (rs.next()) {
	            HistoryDTO dto = new HistoryDTO();
	            dto.setEmp_id(empId);
	            dto.setFrom_dept_id(rs.getInt("dept_id"));
	            dto.setTo_dept_id(rs.getInt("dept_id")); // 부서 이동 없음
	            dto.setFrom_position_id(rs.getInt("position_id"));
	            dto.setTo_position_id(rs.getInt("position_id")); // 직급 변동 없음
	            dto.setFrom_role("부서장");
	            dto.setTo_role("일반"); // 부서장 → 일반으로 강등
	            return dto;
	        }
	        return null;
	    } finally {
	        if (rs != null) rs.close();
	        if (pstmt != null) pstmt.close();
	    }
	}
	
	// 새 부서장 배정 시 해당 부서의 대기 중인 신청들의 dept_manager_id 업데이트
	public void updatePendingRequestsManager(Connection con, int deptId, int newManagerId) throws SQLException {
	    // 휴직/복직 신청 업데이트
	    String leaveSql = "UPDATE leave_of_absence_request l " +
	                      "JOIN employee e ON l.emp_id = e.emp_id " +
	                      "SET l.dept_manager_id = ? " +
	                      "WHERE e.dept_id = ? AND l.status = '대기'";
	    PreparedStatement pstmt = null;
	    try {
	        pstmt = con.prepareStatement(leaveSql);
	        pstmt.setInt(1, newManagerId);
	        pstmt.setInt(2, deptId);
	        pstmt.executeUpdate();
	    } finally {
	        if (pstmt != null) pstmt.close();
	    }

	    // 퇴직 신청 업데이트
	    String resignSql = "UPDATE resign_request r " +
	                       "JOIN employee e ON r.emp_id = e.emp_id " +
	                       "SET r.dept_manager_id = ? " +
	                       "WHERE e.dept_id = ? AND r.status = '대기'";
	    try {
	        pstmt = con.prepareStatement(resignSql);
	        pstmt.setInt(1, newManagerId);
	        pstmt.setInt(2, deptId);
	        pstmt.executeUpdate();
	    } finally {
	        if (pstmt != null) pstmt.close();
	    }
	}
}