package com.hrms.sys.service;

import com.hrms.common.db.DatabaseConnection;
import com.hrms.sys.dao.NotificationDAO;
import com.hrms.sys.dto.NotificationDTO;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class NotificationService {

    private final NotificationDAO notificationDAO = new NotificationDAO();

    /** 미읽음 수 조회 (헤더 AJAX용) */
    public int getUnreadCount(int empId) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            return notificationDAO.countUnread(empId, conn);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }

    /** 알림 목록 조회 + 시간 포맷 + 배지 색상 세팅 */
    public List<NotificationDTO> getNotifications(int empId, boolean unreadOnly) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            List<NotificationDTO> list = notificationDAO.selectByEmpId(empId, unreadOnly, conn);
            for (NotificationDTO dto : list) {
                dto.setCreatedAtStr(formatRelativeTime(dto.getCreatedAt()));
                dto.setBadgeColor(resolveBadgeColor(dto.getNotiType()));
            }
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("알림 조회 중 오류가 발생했습니다.", e);
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }

    /** 단건 읽음 처리 */
    public void markAsRead(long notiId, int empId) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            notificationDAO.markAsRead(notiId, empId, conn);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }

    /** 전체 읽음 처리 */
    public void markAllAsRead(int empId) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            notificationDAO.markAllAsRead(empId, conn);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("읽음 처리 중 오류가 발생했습니다.", e);
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }

    // ── Private 유틸 ──

    /** "10분 전", "3시간 전", "어제", "3일 전" 형태로 변환 */
    private String formatRelativeTime(LocalDateTime createdAt) {
        if (createdAt == null) return "";
        Duration duration = Duration.between(createdAt, LocalDateTime.now());
        long minutes = duration.toMinutes();
        long hours   = duration.toHours();
        long days    = duration.toDays();

        if (minutes < 1)   return "방금";
        if (minutes < 60)  return minutes + "분 전";
        if (hours   < 24)  return hours   + "시간 전";
        if (days    == 1)  return "어제";
        return days + "일 전";
    }

    /** noti_type별 배지 색상 클래스 반환 */
    private String resolveBadgeColor(String notiType) {
        if (notiType == null) return "badge-gray";
        switch (notiType) {
            case "LEAVE_APPROVED":    return "badge-green";
            case "LEAVE_REJECTED":    return "badge-red";
            case "LEAVE_PENDING":     return "badge-yellow";
            case "OVERTIME_APPROVED": return "badge-green";
            case "OVERTIME_REJECTED": return "badge-red";
            case "OVERTIME_PENDING":  return "badge-yellow";
            case "SALARY_PAID":       return "badge-blue";
            case "EVAL_CONFIRMED":    return "badge-purple";
            case "ACCOUNT_LOCKED":    return "badge-red";
            case "PASSWORD_RESET":    return "badge-yellow";
            default:                  return "badge-gray";
        }
    }
}