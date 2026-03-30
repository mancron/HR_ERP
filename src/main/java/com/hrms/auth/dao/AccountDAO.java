package com.hrms.auth.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.hrms.auth.dto.AccountDTO;
import com.hrms.common.db.DatabaseConnection;

public class AccountDAO {

    // 1. 사용자 정보 조회 (로그인 시 계정 객체 생성)
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

    // 2. 로그인 실패 처리 (실패 횟수 증가 및 5회 시 잠금)
    public void handleLoginFailure(String username) {
        // login_attempts + 1 이 '5'를 넘어서는 순간(즉, 6회째 시도)에 잠그기
        String sql = "UPDATE account SET " +
                     "login_attempts = login_attempts + 1, " +
                     "is_active = CASE WHEN login_attempts + 1 > 5 THEN 0 ELSE is_active END, " +
                     "locked_at = CASE WHEN login_attempts + 1 > 5 THEN CURRENT_TIMESTAMP ELSE locked_at END " +
                     "WHERE username = ?";
                         
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // 3. 로그인 성공 처리 (실패 횟수 초기화 및 로그인 시간 기록)
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
}