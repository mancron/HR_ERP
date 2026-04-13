package com.hrms.emp.service;

import java.sql.Connection;
import java.util.List;

import com.hrms.common.db.DatabaseConnection;
import com.hrms.emp.dao.ApprovalActionDAO;
import com.hrms.emp.dao.TransferDAO;
import com.hrms.emp.dto.HistoryDTO;

public class PendingApplyService {

    private ApprovalActionDAO approvalDao = new ApprovalActionDAO();
    private TransferDAO transferDao = new TransferDAO();

    public void processPending() {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            // 1. 미처리 휴직/복직 건 처리
            List<int[]> pendingLeaves = approvalDao.getPendingLeaveApprovals(con);
            for (int[] row : pendingLeaves) {
                int requestId = row[0];
                int empId     = row[1];
                // leave_type 조회 후 상태 업데이트
                String leaveType = approvalDao.getLeaveType(con, requestId);
                String newStatus = "휴직".equals(leaveType) ? "휴직" : "재직";
                approvalDao.updateEmployeeStatus(con, empId, newStatus);
                approvalDao.markLeaveAsApplied(con, requestId);
            }

            // 2. 미처리 퇴직 건 처리
            List<int[]> pendingResigns = approvalDao.getPendingResignApprovals(con);
            for (int[] row : pendingResigns) {
                int requestId = row[0];
                int empId     = row[1];
                String resignDate = approvalDao.getResignDate(con, requestId);
                approvalDao.updateEmployeeResign(con, empId, resignDate);
                approvalDao.clearDeptManagerIfResign(con, empId);
                approvalDao.markResignAsApplied(con, requestId);
            }

            // 3. 미처리 인사발령 건 처리
            List<HistoryDTO> pendingTransfers = transferDao.getPendingTransferApprovals(con);
            for (HistoryDTO dto : pendingTransfers) {
                // employee 부서/직급 업데이트
                // emp_no 조회 필요
                String empNo = approvalDao.getEmpNo(con, dto.getEmp_id());
                transferDao.updateEmployeePosition(con, empNo,
                        dto.getTo_dept_id(), dto.getTo_position_id());

                // 발령 직책이 부서장이면 department.manager_id 업데이트
                if ("부서장".equals(dto.getTo_role())) {
                    // 기존 부서장 해제
                    int prevManagerId = transferDao.getCurrentDeptManagerId(con, dto.getTo_dept_id());
                    if (prevManagerId > 0 && prevManagerId != dto.getEmp_id()) {
                        transferDao.clearDeptManager(con, dto.getTo_dept_id());
                    }
                    transferDao.updateDeptManager(con, dto.getTo_dept_id(), dto.getEmp_id());
                    transferDao.updatePendingRequestsManager(con, dto.getTo_dept_id(), dto.getEmp_id());
                } else if ("일반".equals(dto.getTo_role())) {
                    // 기존 부서장이었으면 해제
                    boolean isManager = transferDao.isDeptManager(con, dto.getEmp_id());
                    if (isManager) {
                        transferDao.clearDeptManager(con, dto.getTo_dept_id());
                    }
                }

                transferDao.markTransferAsApplied(con, dto.getHistory_id());
            }

            con.commit();

        } catch (Exception e) {
            e.printStackTrace();
            try { if (con != null) con.rollback(); } catch (Exception ex) { ex.printStackTrace(); }
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }
}