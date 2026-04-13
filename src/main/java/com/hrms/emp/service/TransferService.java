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

            // 적용일 기준 즉시 처리 여부 판단
            java.time.LocalDate changeDate = dto.getChange_date() != null
                    ? dto.getChange_date().toLocalDate()
                    : java.time.LocalDate.now();
            boolean isToday = !changeDate.isAfter(java.time.LocalDate.now());

            if (isToday) {
                // 1. employee 테이블 부서/직급 즉시 업데이트
                int r1 = transferDao.updateEmployeePosition(con, empNo,
                        dto.getTo_dept_id(), dto.getTo_position_id());
                if (r1 <= 0) { con.rollback(); return false; }

                // 2. 기존 부서장이었다면 기존 부서 manager_id 초기화
                boolean isCurrentManager = transferDao.isDeptManager(con, dto.getEmp_id());
                if (isCurrentManager) {
                    transferDao.clearDeptManager(con, dto.getFrom_dept_id());
                }

                // 3. 발령 직책이 부서장이면 새 부서 manager_id 업데이트
                if ("부서장".equals(targetRole)) {
                    int prevManagerId = transferDao.getCurrentDeptManagerId(con, dto.getTo_dept_id());
                    if (prevManagerId > 0 && prevManagerId != dto.getEmp_id()) {
                        HistoryDTO prevManagerHistory = transferDao.getEmpInfoForHistory(con, prevManagerId);
                        if (prevManagerHistory != null) {
                            prevManagerHistory.setChange_type("발령");
                            prevManagerHistory.setChange_date(dto.getChange_date());
                            prevManagerHistory.setReason("부서장 변경으로 인한 직책 해제");
                            prevManagerHistory.setApproved_by(dto.getApproved_by());
                            prevManagerHistory.setIs_applied(1); // 즉시처리이므로 applied
                            transferDao.insertPersonnelHistory(con, prevManagerHistory);
                        }
                    }
                    transferDao.updateDeptManager(con, dto.getTo_dept_id(), dto.getEmp_id());
                    transferDao.updatePendingRequestsManager(con, dto.getTo_dept_id(), dto.getEmp_id());
                }
            }
            // 미래 날짜면 employee 업데이트 없이 이력만 INSERT

            // 4. 이력 INSERT (is_applied 세팅)
            dto.setIs_applied(isToday ? 1 : 0);
            int r2 = transferDao.insertPersonnelHistory(con, dto);
            if (r2 <= 0) { con.rollback(); return false; }

            con.commit();
            return true;

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