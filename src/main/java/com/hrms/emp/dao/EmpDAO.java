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

	//직원 상세정보
	public Vector<EmpDTO> getAllEmp() {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String sql = null;
        Vector<EmpDTO> vlist = new Vector<EmpDTO>();
        
        try {
            // pool 객체로부터 connection을 가져오도록 수정
            con = DatabaseConnection.getConnection(); 
            sql = "select * from employee order by emp_no asc";
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
	
	
	
	

