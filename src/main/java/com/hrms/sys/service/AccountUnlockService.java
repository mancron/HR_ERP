package com.hrms.sys.service;

import com.hrms.sys.dao.AccountUnlockDAO;
import com.hrms.sys.dto.AccountUnlockDTO;
import com.hrms.common.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class AccountUnlockService {

    private final AccountUnlockDAO accountUnlockDAO = new AccountUnlockDAO();

    /**
     * 잠금 계정 목록 조회 (단순 SELECT — 트랜잭션 불필요)
     */
    public List<AccountUnlockDTO> getLockedAccounts() {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            return accountUnlockDAO.selectLockedAccounts(conn);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("잠금 계정 조회 중 오류가 발생했습니다.", e);
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }

    /**
     * 잠금 해제 + audit_log 기록 (트랜잭션 필수)
     *
     * @param accountId  해제 대상 account_id
     * @param actorEmpId 작업자(관리자) emp_id
     * @param loginAttempts 해제 전 실패 횟수 (audit_log old_value용)
     */
    public void unlockAccount(int accountId, Integer actorEmpId, int loginAttempts) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // ── 트랜잭션 시작

            // 1. 잠금 해제
            int updated = accountUnlockDAO.unlockAccount(accountId, conn);
            if (updated == 0) {
                throw new RuntimeException("해제할 계정을 찾을 수 없습니다. (account_id=" + accountId + ")");
            }

            // 2. audit_log INSERT
            accountUnlockDAO.insertAuditLog(
                actorEmpId,
                accountId,
                loginAttempts + "회 실패 (잠금)",  // old_value
                "0회 (잠금 해제)",                  // new_value
                conn
            );

            conn.commit(); // ── 커밋

        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException re) { re.printStackTrace(); }
            e.printStackTrace();
            throw new RuntimeException("잠금 해제 중 데이터베이스 오류가 발생했습니다.", e);
        } catch (RuntimeException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException re) { re.printStackTrace(); }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }
}