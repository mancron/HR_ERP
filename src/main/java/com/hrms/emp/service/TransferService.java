package com.hrms.emp.service;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import com.hrms.common.db.DatabaseConnection;
import com.hrms.emp.dao.TransferDAO;
import com.hrms.emp.dto.EmpDTO;
import com.hrms.emp.dto.HistoryDTO;

public class TransferService {

    // 1. 발령 전용 DAO인 TransferDAO를 사용하도록 수정
    private TransferDAO transferDao = new TransferDAO();

    //인사발령 실행 (트랜잭션 처리)
    public boolean executeTransfer(String empNo, HistoryDTO dto, String targetRole) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            // 1. employee 테이블 부서/직급 업데이트
            int r1 = transferDao.updateEmployeePosition(con, empNo,
                    dto.getTo_dept_id(), dto.getTo_position_id());

            // 2. 기존 부서장이었다면 기존 부서 manager_id 초기화
            boolean isCurrentManager = transferDao.isDeptManager(con, dto.getEmp_id());
            if (isCurrentManager) {
                transferDao.clearDeptManager(con, dto.getFrom_dept_id());
            }

            // 3. 발령 직책이 부서장이면 새 부서 manager_id 업데이트
            if ("부서장".equals(targetRole)) {
                // 기존 부서장이 있는지 확인
                int prevManagerId = transferDao.getCurrentDeptManagerId(con, dto.getTo_dept_id());

                if (prevManagerId > 0 && prevManagerId != dto.getEmp_id()) {
                    // 기존 부서장의 정보 조회 후 이력 기록
                    HistoryDTO prevManagerHistory = transferDao.getEmpInfoForHistory(con, prevManagerId);
                    if (prevManagerHistory != null) {
                        prevManagerHistory.setChange_type("발령");
                        prevManagerHistory.setChange_date(dto.getChange_date());
                        prevManagerHistory.setReason("부서장 변경으로 인한 직책 해제");
                        prevManagerHistory.setApproved_by(dto.getApproved_by());
                        transferDao.insertPersonnelHistory(con, prevManagerHistory);
                    }
                }

                transferDao.updateDeptManager(con, dto.getTo_dept_id(), dto.getEmp_id());
            }
            
            // 4. 이력 INSERT
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
    }

    public List<EmpDTO> getDeptList() {
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
    
    public List<EmpDTO> getPositionList() {
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
    
    public boolean isDeptManager(int empId) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return transferDao.isDeptManager(con, empId);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }
    
    
}