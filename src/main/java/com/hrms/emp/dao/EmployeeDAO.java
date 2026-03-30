package com.hrms.emp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.hrms.common.db.DatabaseConnection;
import com.hrms.emp.dto.EmployeeDTO;

public class EmployeeDAO {

    // 세션에 담을 상세 정보 조회 (부서명 포함)
    public EmployeeDTO getEmployeeById(int empId) {
        EmployeeDTO emp = null;
        // employee와 department 테이블을 조인해서 부서명을 가져오거나, 
        // 여기서는 일단 DTO 구조에 맞춰 기본 정보를 가져옵니다.
        String sql = "SELECT e.*, d.dept_name " +
                     "FROM employee e " +
                     "LEFT JOIN department d ON e.dept_id = d.dept_id " +
                     "WHERE e.emp_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, empId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    emp = new EmployeeDTO();
                    emp.setEmpId(rs.getInt("emp_id"));
                    emp.setEmpName(rs.getString("emp_name"));
                    emp.setEmpNo(rs.getString("emp_no"));
                    emp.setDeptId(rs.getInt("dept_id"));
                    // DTO에 deptName 필드를 추가하거나, 임시로 처리
                    // (만약 DTO에 없다면 아래 2번 항목 참고해서 필드 추가하세요!)
                    emp.setEmpType(rs.getString("emp_type"));
                    emp.setStatus(rs.getString("status"));
                    emp.setEmail(rs.getString("email"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return emp;
    }
}