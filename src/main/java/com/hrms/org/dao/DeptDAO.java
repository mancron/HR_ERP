package com.hrms.org.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Vector;

import com.hrms.emp.dto.EmpDTO;
import com.hrms.org.dto.DeptDTO;
import com.hrms.common.db.DatabaseConnection;

public class DeptDAO {
		
		public DeptDAO() {}
		
		public Vector<DeptDTO> deptList() {
	        Connection con = null;
	        PreparedStatement pstmt = null;
	        ResultSet rs = null;
	        String sql = null;
	        Vector<DeptDTO> vlist = new Vector<DeptDTO>();
	        
	        try {
	            // pool 객체로부터 connection을 가져오도록 수정
	            con = DatabaseConnection.getConnection(); 
	            sql = "select dept_id, dept_name from department order by dept_id asc";
	            pstmt = con.prepareStatement(sql);
	            rs = pstmt.executeQuery();
	            
	            while (rs.next()) {
	                DeptDTO dto = new DeptDTO();
	                dto.setDept_id(rs.getInt("dept_id"));
	                dto.setDept_name(rs.getString("dept_name"));
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
		
		public String getDeptNameById(int deptId) {
		    Connection con = null;
		    PreparedStatement pstmt = null;
		    ResultSet rs = null;
		    String deptName = "소속 미지정";

		    try {
		        con = DatabaseConnection.getConnection();
		        String sql = "SELECT dept_name FROM department WHERE dept_id = ?";
		        pstmt = con.prepareStatement(sql);
		        pstmt.setInt(1, deptId);
		        rs = pstmt.executeQuery();

		        if (rs.next()) {
		            deptName = rs.getString("dept_name");
		        }
		    } catch (Exception e) {
		        e.printStackTrace();
		    } finally {
		        // ... (기존 close 로직 동일)
		    }
		    return deptName;
		}
}
