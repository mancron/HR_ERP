package com.hrms.sys.dao;

import com.hrms.sys.dto.AccountUnlockDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AccountUnlockDAO {

    /**
     * 잠금 계정 목록 조회 (login_attempts >= 5)
     */
    public List<AccountUnlockDTO> selectLockedAccounts(Connection conn) throws SQLException {
        List<AccountUnlockDTO> list = new ArrayList<>();
        String sql =
            "SELECT a.account_id, a.login_attempts, a.locked_at, " +
            "       e.emp_no, e.emp_name, d.dept_name " +
            "FROM account a " +
            "JOIN employee   e ON a.emp_id   = e.emp_id " +
            "JOIN department d ON e.dept_id  = d.dept_id " +
            "WHERE a.login_attempts >= 5 " +
            "  AND a.is_active = 1 " +
            "ORDER BY a.locked_at DESC";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                AccountUnlockDTO dto = new AccountUnlockDTO();
                dto.setAccountId(rs.getInt("account_id"));
                dto.setLoginAttempts(rs.getInt("login_attempts"));
                dto.setEmpNo(rs.getString("emp_no"));
                dto.setEmpName(rs.getString("emp_name"));
                dto.setDeptName(rs.getString("dept_name"));

                // locked_at → 문자열 변환
                Timestamp lockedAt = rs.getTimestamp("locked_at");
                if (lockedAt != null) {
                    dto.setLockedAtStr(lockedAt.toLocalDateTime()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                } else {
                    dto.setLockedAtStr("-");
                }
                list.add(dto);
            }
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
        return list;
    }

    /**
     * 잠금 해제: login_attempts = 0, locked_at = NULL
     */
    public int unlockAccount(int accountId, Connection conn) throws SQLException {
        String sql =
            "UPDATE account " +
            "SET login_attempts = 0, locked_at = NULL " +
            "WHERE account_id = ?";

        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, accountId);
            return pstmt.executeUpdate();
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
    }

    /**
     * audit_log INSERT
     */
    public void insertAuditLog(Integer actorId, int accountId,
                               String oldValue, String newValue,
                               Connection conn) throws SQLException {
        String sql =
            "INSERT INTO audit_log " +
            "(actor_id, target_table, target_id, action, column_name, old_value, new_value) " +
            "VALUES (?, 'account', ?, 'UPDATE', 'login_attempts', ?, ?)";

        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sql);
            if (actorId != null) {
                pstmt.setInt(1, actorId);
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }
            pstmt.setInt(2, accountId);
            pstmt.setString(3, oldValue);
            pstmt.setString(4, newValue);
            pstmt.executeUpdate();
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
    }
}