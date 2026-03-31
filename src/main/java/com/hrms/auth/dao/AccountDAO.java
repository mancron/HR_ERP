package com.hrms.auth.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.hrms.auth.dto.AccountDTO;
import com.hrms.common.db.DatabaseConnection; // 수정된 패키지 경로 반영

public class AccountDAO {

    // [추가] 관리자(역할이 '관리자')인 사원의 연락처 가져오기
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
        return "051-890-0000"; // DB 조회 실패 시 기본값
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
        // [수정] 5회 시도까지는 활성화(is_active=1)를 유지하고, 
        // 5회를 초과(> 5)하는 6회째 시도부터 잠금 처리되도록 부등호 수정
        String sql = "UPDATE account SET " +
                     "login_attempts = login_attempts + 1, " +
                     "is_active = CASE WHEN login_attempts + 1 >= 5 THEN 0 ELSE is_active END, " +
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
}