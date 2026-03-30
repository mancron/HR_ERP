package com.hrms.emp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Vector;

import com.hrms.emp.dto.EmpDTO;
import com.hrms.org.dto.DeptDTO;
import com.hrms.util.DatabaseConnection;

public class EmpDAO {
	
	public EmpDAO() {}
	
	//직원 목록 카드
	public Vector<EmpDTO> getEmpList() {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String sql = null;
        Vector<EmpDTO> vlist = new Vector<EmpDTO>();
        
        try {
            con = DatabaseConnection.getConnection(); 
            sql = "SELECT e.*, d.dept_name, p.position_name " +
            	      "FROM employee e " +
            	      "LEFT JOIN department d ON e.dept_id = d.dept_id " +
            	      "LEFT JOIN job_position p ON e.position_id = p.position_id " +
            	      "ORDER BY e.emp_no ASC";
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
            	EmpDTO dto = new EmpDTO();
            	dto.setEmp_id(rs.getInt("emp_id"));
            	dto.setEmp_name(rs.getString("emp_name"));
                dto.setEmp_no(rs.getString("emp_no"));
                dto.setDept_id(rs.getInt("dept_id"));
                dto.setPosition_id(rs.getInt("position_id"));
                dto.setHire_date(rs.getString("hire_date"));
                dto.setResign_date(rs.getString("resign_date"));
                dto.setEmp_type(rs.getString("emp_type"));
                dto.setStatus(rs.getString("status"));
                dto.setBase_salary(rs.getInt("base_salary"));
                dto.setBirth_date(rs.getString("birth_date"));
                dto.setGender(rs.getString("gender"));
                dto.setAddress(rs.getString("address"));
                dto.setEmergency_contact(rs.getString("emergency_contact"));
                dto.setBank_account(rs.getString("bank_account"));
                dto.setEmail(rs.getString("email"));
                dto.setPhone(rs.getString("phone"));
                dto.setCreated_at(rs.getString("created_at"));
                dto.setDept_name(rs.getString("dept_name"));
                dto.setPosition_name(rs.getString("position_name"));
                vlist.addElement(dto);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (con != null) con.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return vlist;
	}

	/**
	 * 필터 조건으로 직원 목록 조회
	 * @param keyword  - 이름 또는 사번 검색어 (null 또는 "" 이면 전체)
	 * @param deptId   - 부서 ID (0 또는 "all" 이면 전체)
	 * @param positionId - 직급 ID (0 또는 "all" 이면 전체)
	 * @param status   - 재직 상태 ("work"/"leave"/"resign", "all" 이면 전체)
	 */
	public Vector<EmpDTO> searchEmpList(String keyword, int deptId, int positionId, String status) {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Vector<EmpDTO> vlist = new Vector<EmpDTO>();
        
        try {
            con = DatabaseConnection.getConnection();
 
            // 동적 WHERE 절 구성
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT e.*, d.dept_name, p.position_name ");
            sql.append("FROM employee e ");
            sql.append("LEFT JOIN department d ON e.dept_id = d.dept_id ");
            sql.append("LEFT JOIN job_position p ON e.position_id = p.position_id ");
            sql.append("WHERE 1=1 ");
 
            // 이름 또는 사번 검색
            if (keyword != null && !keyword.trim().isEmpty()) {
                sql.append("AND (e.emp_name LIKE ? OR e.emp_no LIKE ?) ");
            }
            // 부서 필터
            if (deptId > 0) {
                sql.append("AND e.dept_id = ? ");
            }
            // 직급 필터
            if (positionId > 0) {
                sql.append("AND e.position_id = ? ");
            }
            // 재직 상태 필터
            if (status != null && !status.equals("all")) {
                sql.append("AND e.status = ? ");
            }
 
            sql.append("ORDER BY e.emp_no ASC");
 
            pstmt = con.prepareStatement(sql.toString());
 
            // 파라미터 바인딩
            int idx = 1;
            if (keyword != null && !keyword.trim().isEmpty()) {
                pstmt.setString(idx++, "%" + keyword.trim() + "%");
                pstmt.setString(idx++, "%" + keyword.trim() + "%");
            }
            if (deptId > 0) {
                pstmt.setInt(idx++, deptId);
            }
            if (positionId > 0) {
                pstmt.setInt(idx++, positionId);
            }
            if (status != null && !status.equals("all")) {
                pstmt.setString(idx++, status);
            }
 
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
            	EmpDTO dto = new EmpDTO();
            	dto.setEmp_id(rs.getInt("emp_id"));
            	dto.setEmp_name(rs.getString("emp_name"));
                dto.setEmp_no(rs.getString("emp_no"));
                dto.setDept_id(rs.getInt("dept_id"));
                dto.setPosition_id(rs.getInt("position_id"));
                dto.setHire_date(rs.getString("hire_date"));
                dto.setResign_date(rs.getString("resign_date"));
                dto.setEmp_type(rs.getString("emp_type"));
                dto.setStatus(rs.getString("status"));
                dto.setBase_salary(rs.getInt("base_salary"));
                dto.setBirth_date(rs.getString("birth_date"));
                dto.setGender(rs.getString("gender"));
                dto.setAddress(rs.getString("address"));
                dto.setEmergency_contact(rs.getString("emergency_contact"));
                dto.setBank_account(rs.getString("bank_account"));
                dto.setEmail(rs.getString("email"));
                dto.setPhone(rs.getString("phone"));
                dto.setCreated_at(rs.getString("created_at"));
                dto.setDept_name(rs.getString("dept_name"));
                dto.setPosition_name(rs.getString("position_name"));
                vlist.addElement(dto);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (con != null) con.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return vlist;
	}
	
	
}
	
	
	
	

