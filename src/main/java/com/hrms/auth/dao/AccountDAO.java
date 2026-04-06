package com.hrms.auth.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.hrms.auth.dto.AccountDTO;
import com.hrms.common.db.DatabaseConnection;

public class AccountDAO {

    // 관리자 연락처 가져오기
    public String getAdminContact() {
        String sql = "SELECT e.phone FROM employee e " +
                     "JOIN account a ON e.emp_id = a.emp_id " +
                     "WHERE a.role = '관리자' LIMIT 1";
        
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                String phone = rs.getString("phone");
                return (phone != null && !phone.isEmpty()) ? phone : "051-890-0000";
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "051-890-0000";
    }

    // 1. 사용자 정보 조회
    public AccountDTO getAccountByUsername(String username) {
        String sql = "SELECT * FROM account WHERE username = ?";
        
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    AccountDTO dto = new AccountDTO();
                    dto.setAccountId(rs.getInt("account_id"));
                    dto.setEmpId(rs.getInt("emp_id"));
                    dto.setUsername(rs.getString("username"));
                    dto.setPasswordHash(rs.getString("password_hash"));
                    dto.setRole(rs.getString("role"));
                    // 이 부분이 DTO의 isActive 필드와 정확히 매핑되어야 합니다.
                    dto.setIsActive(rs.getInt("is_active")); 
                    dto.setLoginAttempts(rs.getInt("login_attempts"));
                    dto.setLockedAt(rs.getTimestamp("locked_at"));
                    return dto;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 2. 로그인 실패 처리
    public void handleLoginFailure(String username) {
        String sql = "UPDATE account SET " +
                     "login_attempts = login_attempts + 1, " +
                     "locked_at = CASE WHEN login_attempts + 1 >= 5 THEN CURRENT_TIMESTAMP ELSE locked_at END " +
                     "WHERE username = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // 3. 로그인 성공 처리
    public void handleLoginSuccess(String username) {
        String sql = "UPDATE account SET login_attempts = 0, last_login = CURRENT_TIMESTAMP WHERE username = ?";
        
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 4. 비밀번호 업데이트
    public boolean updatePassword(String userId, String newHashedPw) {
        String sql = "UPDATE account SET password_hash = ?, password_changed_at = CURRENT_TIMESTAMP WHERE username = ?";
        
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            
            pstmt.setString(1, newHashedPw);
            pstmt.setString(2, userId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int[] getAdminEmpIds() {
        String sql = "SELECT emp_id FROM account WHERE role = '관리자' AND is_active = 1";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            List<Integer> ids = new ArrayList<>();
            while (rs.next()) {
                ids.add(rs.getInt("emp_id"));
            }
            return ids.stream().mapToInt(Integer::intValue).toArray();

        } catch (SQLException e) {
            e.printStackTrace();
            return new int[0];
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
            if (conn  != null) try { conn.close();  } catch (SQLException e) {}
        }
    }
}