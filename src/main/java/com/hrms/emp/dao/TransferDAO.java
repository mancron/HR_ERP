package com.hrms.emp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import com.hrms.emp.dto.TransferDTO;

public class TransferDAO {

    /**
     * 1. 사원 테이블의 부서/직급 정보 수정 (UPDATE employee)
     * 설계서 흐름도: [사원정보 수정] 단계
     */
    public int updateEmployeePosition(Connection con, TransferDTO dto) throws SQLException {
        PreparedStatement pstmt = null;
        String sql = "UPDATE employee SET dept_id = ?, position_id = ? WHERE emp_no = ?";
        
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, dto.getTarget_dept_id());
            pstmt.setInt(2, dto.getTarget_position_id());
            pstmt.setString(3, dto.getEmp_no());
            
            return pstmt.executeUpdate();
        } finally {
            if (pstmt != null) pstmt.close();
        }
    }

    /**
     * 2. 발령 이력 테이블에 내역 삽입 (INSERT INTO personnel_history)
     * 설계서 흐름도: [발력이력 등록] 단계
     */
    public int insertPersonnelHistory(Connection con, TransferDTO dto) throws SQLException {
        PreparedStatement pstmt = null;
        // emp_id를 가져오기 위해 서브쿼리를 사용하거나, DTO에 emp_id가 있다면 바로 사용합니다.
        String sql = "INSERT INTO personnel_history (emp_id, change_type, change_date, "
                   + "from_dept_id, to_dept_id, from_position_id, to_position_id, description) "
                   + "VALUES ((SELECT emp_id FROM employee WHERE emp_no = ?), ?, ?, ?, ?, ?, ?, ?)";
        
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, dto.getEmp_no());
            pstmt.setString(2, dto.getTransfer_type());
            pstmt.setString(3, dto.getTransfer_date());
            pstmt.setInt(4, dto.getPrev_dept_id());
            pstmt.setInt(5, dto.getTarget_dept_id());
            pstmt.setInt(6, dto.getPrev_position_id());
            pstmt.setInt(7, dto.getTarget_position_id());
            pstmt.setString(8, dto.getReason());
            
            return pstmt.executeUpdate();
        } finally {
            if (pstmt != null) pstmt.close();
        }
    }
}