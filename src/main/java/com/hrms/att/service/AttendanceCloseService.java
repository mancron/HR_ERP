package com.hrms.att.service;

import com.hrms.att.dao.AttendanceCloseDAO;
import com.hrms.common.db.DatabaseConnection;
import com.hrms.sal.service.SalaryCalcService;

import java.sql.Connection;

public class AttendanceCloseService {

    private final AttendanceCloseDAO dao       = new AttendanceCloseDAO();
    private final SalaryCalcService  salService = new SalaryCalcService();

    /**
     * 근태 마감 + 급여 자동 재계산
     * 하나의 트랜잭션으로 묶어서 처리
     */
    public void closeAndRecalculate(int year, int month, int actorId) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. 이미 마감됐으면 차단
            if (dao.isClosedTx(year, month, conn)) {
                throw new RuntimeException(
                    year + "년 " + month + "월은 이미 마감된 상태입니다.");
            }

            // 2. 근태 마감
            dao.closeMonthTx(year, month, actorId, conn);

            // 3. 대기 급여 삭제 후 재계산 (같은 트랜잭션)
            salService.recalculateInTransaction(year, month, conn);

            conn.commit();

        } catch (RuntimeException e) {
            rollback(conn);
            throw e;
        } catch (Exception e) {
            rollback(conn);
            throw new RuntimeException("근태 마감 중 오류가 발생했습니다.", e);
        } finally {
            close(conn);
        }
    }

    private void rollback(Connection conn) {
        if (conn != null) try { conn.rollback(); } catch (Exception e) { e.printStackTrace(); }
    }

    private void close(Connection conn) {
        if (conn != null) {
            try { conn.setAutoCommit(true); conn.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }
}