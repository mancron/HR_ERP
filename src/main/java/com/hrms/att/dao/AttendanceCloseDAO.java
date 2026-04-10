package com.hrms.att.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.hrms.common.db.DatabaseConnection;

public class AttendanceCloseDAO {

    /** 기존 유지 — 단순 조회용 */
    public boolean isClosed(int year, int month) {
        String sql = "SELECT is_closed FROM attendance_close WHERE year=? AND month=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, year);
            pstmt.setInt(2, month);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getBoolean("is_closed");
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** 기존 유지 — 단독 마감용 */
    public void closeMonth(int year, int month, int actorId) {
        String sql =
            "INSERT INTO attendance_close (year, month, is_closed, closed_by, closed_at) " +
            "VALUES (?, ?, true, ?, NOW()) " +
            "ON DUPLICATE KEY UPDATE is_closed=true, closed_by=?, closed_at=NOW()";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, year);
            pstmt.setInt(2, month);
            pstmt.setInt(3, actorId);
            pstmt.setInt(4, actorId);
            pstmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("마감 실패", e);
        }
    }

    /** 추가 — 트랜잭션용 (외부 Connection 사용) */
    public boolean isClosedTx(int year, int month, Connection conn) throws Exception {
        String sql = "SELECT is_closed FROM attendance_close WHERE year=? AND month=?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, year);
            pstmt.setInt(2, month);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getBoolean("is_closed");
            return false;
        }
    }

    /** 추가 — 트랜잭션용 (외부 Connection 사용) */
    public void closeMonthTx(int year, int month, int actorId, Connection conn) throws Exception {
        String sql =
            "INSERT INTO attendance_close (year, month, is_closed, closed_by, closed_at) " +
            "VALUES (?, ?, true, ?, NOW()) " +
            "ON DUPLICATE KEY UPDATE is_closed=true, closed_by=?, closed_at=NOW()";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, year);
            pstmt.setInt(2, month);
            pstmt.setInt(3, actorId);
            pstmt.setInt(4, actorId);
            pstmt.executeUpdate();
        }
    }
}