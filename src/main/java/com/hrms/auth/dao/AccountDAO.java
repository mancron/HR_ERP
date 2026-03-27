package com.hrms.auth.dao;

import com.hrms.auth.dto.AccountDTO;
import com.hrms.util.DatabaseConnection; // 팀장님이 만든 클래스로 임포트 변경
import java.sql.*;

public class AccountDAO {

    // 생성자와 DBConnectionMgr 변수 제거 (static 메서드 사용)
    public AccountDAO() {}

    public AccountDTO getAccountByUsername(String username) {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        AccountDTO dto = null;
        try {
            // DatabaseConnection을 통한 커넥션 획득
            con = DatabaseConnection.getConnection();
            String sql = "SELECT * FROM account WHERE username = ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                dto = new AccountDTO();
                dto.setAccountId(rs.getInt("account_id"));
                dto.setEmpId(rs.getInt("emp_id"));
                dto.setUsername(rs.getString("username"));
                dto.setPasswordHash(rs.getString("password_hash")); 
                dto.setRole(rs.getString("role"));
                dto.setIsActive(rs.getInt("is_active"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // 표준 자원 반납 (HikariCP 풀 반납)
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
            if (con != null) try { con.close(); } catch (SQLException e) {}
        }
        return dto;
    }

    public String getPasswordByUserId(String userId) {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String passwordHash = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "SELECT password_hash FROM account WHERE username = ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, userId);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                passwordHash = rs.getString("password_hash");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
            if (con != null) try { con.close(); } catch (SQLException e) {}
        }
        return passwordHash;
    }

    public boolean updatePassword(String userId, String newHashedPw) {
        Connection con = null;
        PreparedStatement pstmt = null;
        boolean isSuccess = false;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "UPDATE account SET password_hash = ? WHERE username = ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, newHashedPw);
            pstmt.setString(2, userId);
            
            int result = pstmt.executeUpdate();
            if (result > 0) isSuccess = true;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
            if (con != null) try { con.close(); } catch (SQLException e) {}
        }
        return isSuccess;
    }
}