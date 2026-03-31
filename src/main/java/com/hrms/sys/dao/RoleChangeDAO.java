package com.hrms.sys.dao;

import com.hrms.sys.dto.RoleChangeDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoleChangeDAO {

    /**
     * 전체 활성 계정 목록 조회 (is_active = 1)
     */
    public List<RoleChangeDTO> selectAllAccounts(Connection conn) throws SQLException {
        List<RoleChangeDTO> list = new ArrayList<>();
        String sql =
            "SELECT a.account_id, a.emp_id, a.role, " +
            "       e.emp_no, e.emp_name, d.dept_name " +
            "FROM account a " +
            "JOIN employee   e ON a.emp_id  = e.emp_id " +
            "JOIN department d ON e.dept_id = d.dept_id " +
            "WHERE a.is_active = 1 " +
            "ORDER BY a.account_id ASC";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                RoleChangeDTO dto = new RoleChangeDTO();
                dto.setAccountId(rs.getInt("account_id"));
                dto.setEmpId(rs.getInt("emp_id"));
                dto.setEmpNo(rs.getString("emp_no"));
                dto.setEmpName(rs.getString("emp_name"));
                dto.setDeptName(rs.getString("dept_name"));
                dto.setCurrentRole(rs.getString("role"));
                list.add(dto);
            }
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
        return list;
    }

    /**
     * role UPDATE
     */
    public int updateRole(int accountId, String newRole, Connection conn) throws SQLException {
        String sql = "UPDATE account SET role = ? WHERE account_id = ?";

        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, newRole);
            pstmt.setInt(2, accountId);
            return pstmt.executeUpdate();
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
    }

    /**
     * audit_log INSERT
     */
    public void insertAuditLog(Integer actorId, int accountId,
                               String oldRole, String newRole,
                               Connection conn) throws SQLException {
        String sql =
            "INSERT INTO audit_log " +
            "(actor_id, target_table, target_id, action, column_name, old_value, new_value) " +
            "VALUES (?, 'account', ?, 'UPDATE', 'role', ?, ?)";

        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sql);
            if (actorId != null) {
                pstmt.setInt(1, actorId);
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }
            pstmt.setInt(2, accountId);
            pstmt.setString(3, oldRole);
            pstmt.setString(4, newRole);
            pstmt.executeUpdate();
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
    }
}