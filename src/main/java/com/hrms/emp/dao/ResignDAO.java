package com.hrms.emp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
	
    
    
}
