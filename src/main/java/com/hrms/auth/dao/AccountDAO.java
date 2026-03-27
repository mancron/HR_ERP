package com.hrms.auth.dao;

import com.hrms.auth.dto.AccountDTO;
import com.hrms.common.db.DBConnectionMgr;
import java.sql.*;

public class AccountDAO {
    private DBConnectionMgr pool;

    public AccountDAO() {
        pool = DBConnectionMgr.getInstance();
    }

    public AccountDTO getAccountByUsername(String username) {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        AccountDTO dto = null;
        try {
            con = pool.getConnection();
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
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            pool.freeConnection(con, pstmt, rs);
        }
        return dto;
    }

    public String getPasswordByUserId(String userId) {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String passwordHash = null;
        try {
            con = pool.getConnection();
            // 컬럼명이 'password_hash'이므로 수정
            String sql = "SELECT password_hash FROM account WHERE username = ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, userId);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                passwordHash = rs.getString("password_hash");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            pool.freeConnection(con, pstmt, rs); // 풀링 반납
        }
        return passwordHash;
    }

    public boolean updatePassword(String userId, String newHashedPw) {
        Connection con = null;
        PreparedStatement pstmt = null;
        boolean isSuccess = false;
        try {
            con = pool.getConnection();
            // 컬럼명 'password_hash'로 업데이트
            String sql = "UPDATE account SET password_hash = ? WHERE username = ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, newHashedPw);
            pstmt.setString(2, userId);
            
            int result = pstmt.executeUpdate();
            if (result > 0) isSuccess = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            pool.freeConnection(con, pstmt); // ResultSet 없는 버전으로 반납
        }
        return isSuccess;
    }
}