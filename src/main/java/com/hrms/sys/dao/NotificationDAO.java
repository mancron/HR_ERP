package com.hrms.sys.dao;

import com.hrms.sys.dto.NotificationDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    /** 알림 INSERT */
    public void insert(int empId, String notiType, String refTable,
                       Integer refId, String message, Connection conn) throws SQLException {
        String sql =
            "INSERT INTO notification (emp_id, noti_type, ref_table, ref_id, message) " +
            "VALUES (?, ?, ?, ?, ?)";
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, empId);
            pstmt.setString(2, notiType);
            pstmt.setString(3, refTable);
            if (refId != null) pstmt.setInt(4, refId);
            else               pstmt.setNull(4, Types.INTEGER);
            pstmt.setString(5, message);
            pstmt.executeUpdate();
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
    }

    /** 미읽음 알림 수 조회 (헤더 배지용) */
    public int countUnread(int empId, Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notification WHERE emp_id = ? AND is_read = 0";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, empId);
            rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
        return 0;
    }

    /** 알림 목록 조회 (최신순, 50건 제한) */
    public List<NotificationDTO> selectByEmpId(int empId, boolean unreadOnly, Connection conn)
            throws SQLException {
        List<NotificationDTO> list = new ArrayList<>();
        String sql =
            "SELECT noti_id, emp_id, noti_type, ref_table, ref_id, " +
            "       message, is_read, read_at, created_at " +
            "FROM notification " +
            "WHERE emp_id = ? " +
            (unreadOnly ? "AND is_read = 0 " : "") +
            "ORDER BY created_at DESC LIMIT 50";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, empId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                NotificationDTO dto = new NotificationDTO();
                dto.setNotiId(rs.getLong("noti_id"));
                dto.setEmpId(rs.getInt("emp_id"));
                dto.setNotiType(rs.getString("noti_type"));
                dto.setRefTable(rs.getString("ref_table"));
                int refId = rs.getInt("ref_id");
                if (!rs.wasNull()) dto.setRefId(refId);
                dto.setMessage(rs.getString("message"));
                dto.setIsRead(rs.getInt("is_read"));
                Timestamp readAt = rs.getTimestamp("read_at");
                if (readAt != null) dto.setReadAt(readAt.toLocalDateTime());
                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) dto.setCreatedAt(createdAt.toLocalDateTime());
                list.add(dto);
            }
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
        return list;
    }

    /** 단건 읽음 처리 */
    public int markAsRead(long notiId, int empId, Connection conn) throws SQLException {
        String sql =
            "UPDATE notification SET is_read = 1, read_at = NOW() " +
            "WHERE noti_id = ? AND emp_id = ? AND is_read = 0";
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, notiId);
            pstmt.setInt(2, empId);
            return pstmt.executeUpdate();
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
    }

    /** 전체 읽음 처리 */
    public int markAllAsRead(int empId, Connection conn) throws SQLException {
        String sql =
            "UPDATE notification SET is_read = 1, read_at = NOW() " +
            "WHERE emp_id = ? AND is_read = 0";
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, empId);
            return pstmt.executeUpdate();
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
    }
}