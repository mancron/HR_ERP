package com.hrms.emp.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Vector;

import com.hrms.emp.dao.EmpDAO;
import com.hrms.emp.dto.EmpDTO;
import com.hrms.util.DatabaseConnection;

public class EmpService {
    
    private EmpDAO empDao;

    public EmpService() {
        // 서비스가 생성될 때 DAO도 함께 준비시킵니다.
        empDao = new EmpDAO();
    }

    // 1. 직원 목록 가져오기 서비스
    public Vector<EmpDTO> getEmployeeList() {
        Connection con = null;
        Vector<EmpDTO> list = null;
        
        try {
            con = DatabaseConnection.getConnection();
            // 목록 조회는 단순 SELECT라서 수동 커밋(setAutoCommit)을 생략해도 무방하지만, 
            // 일관성을 위해 적어주는 것도 좋습니다.
            
            // DAO에게 커넥션을 넘겨주며 일을 시킵니다!
            list = empDao.getEmpList(con); 
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Service 계층에서 최종적으로 커넥션을 닫아줍니다. (README 필수 사항)
            try {
                if (con != null) con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    // 2. 특정 직원 상세 정보 가져오기 서비스
    public EmpDTO getEmployeeDetail(String empNo) {
        Connection con = null;
        EmpDTO empDetail = null;
        
        try {
            con = DatabaseConnection.getConnection();
            
            // 상세 조회 (SELECT)
            empDetail = empDao.getEmpDetail(con, empNo);
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (con != null) con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return empDetail;
    }
}