package com.hrms.emp.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.hrms.common.db.DatabaseConnection;
import com.hrms.emp.dao.EmpDAO;
import com.hrms.emp.dao.TransferDAO;
import com.hrms.emp.dto.TransferDTO; // 발령 데이터를 담는 DTO (가정)
import com.hrms.org.dto.DeptDTO;

public class TransferService {

    // 1. 발령 전용 DAO인 TransferDAO를 사용하도록 수정
    private TransferDAO transferDao = new TransferDAO();

    //인사발령 실행 (트랜잭션 처리)
    public boolean executeTransfer(TransferDTO dto) {
        Connection con = null;
        try {
        	 con = DatabaseConnection.getConnection();
             con.setAutoCommit(false);

             // 1. employee 테이블 부서/직급 업데이트
             int r1 = transferDao.updateEmployeePosition(con, empNo,
                          dto.getTo_dept_id(), dto.getTo_position_id());

             // 2. personnel_history 이력 INSERT
             int r2 = transferDao.insertPersonnelHistory(con, dto);

             if (r1 > 0 && r2 > 0) {
                 con.commit();
                 return true;
             } else {
                 con.rollback();
                 return false;
             }
         } catch (Exception e) {
             e.printStackTrace();
             try { if (con != null) con.rollback(); } catch (Exception ex) { ex.printStackTrace(); }
             return false;
         } finally {
             try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
         }

    public List<DeptDTO> getDeptList() {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return transferDao.getDeptList(con);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }
    
    public List<PositionDTO> getPositionList() {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return transferDao.getPositionList(con);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }
    
    private void closeConnection(Connection con) {
        if (con != null) {
            try {
                if (!con.isClosed()) {
                    con.setAutoCommit(true); // 커넥션 풀 반납 전 상태 복구
                    con.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}