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

        // ── 1. 마감 먼저 단독 트랜잭션으로 커밋 ──
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            if (dao.isClosedTx(year, month, conn)) {
                throw new RuntimeException(
                    year + "년 " + month + "월은 이미 마감된 상태입니다.");
            }

            dao.closeMonthTx(year, month, actorId, conn);
            conn.commit(); // 마감 확정

        } catch (RuntimeException e) {
            rollback(conn);
            throw e;
        } catch (Exception e) {
            rollback(conn);
            throw new RuntimeException("근태 마감 중 오류가 발생했습니다.", e);
        } finally {
            close(conn);
        }

        // ── 2. 급여 재계산은 별도 트랜잭션 ──
        // 실패해도 마감은 유지, 로그만 출력
        try {
            salService.recalculate(year, month);
        } catch (Exception e) {
            System.err.println("[급여 재계산 실패 — 마감은 유지됨] " + e.getMessage());
            e.printStackTrace();
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