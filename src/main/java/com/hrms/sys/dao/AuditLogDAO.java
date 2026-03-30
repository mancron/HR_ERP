package com.hrms.sys.dao;

import com.hrms.sys.dto.AuditLogDTO;
import com.hrms.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO {

    public List<AuditLogDTO> selectAuditLogs(String targetTable, String startDate, String endDate) {
        List<AuditLogDTO> logList = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        // 1. LEFT JOIN 쿼리 (시스템 로그 누락 방지)
        StringBuilder sql = new StringBuilder(
            "SELECT a.log_id, a.actor_id, e.emp_name AS actor_name, " +
            "a.target_table, a.target_id, a.action, a.column_name, " +
            "a.old_value, a.new_value, a.created_at " +
            "FROM audit_log a " +
            "LEFT JOIN employee e ON a.actor_id = e.emp_id " +
            "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        // 2. 동적 파라미터 조립
        if (targetTable != null && !targetTable.trim().isEmpty()) {
            sql.append("AND a.target_table = ? ");
            params.add(targetTable.trim());
        }
        if (startDate != null && !startDate.trim().isEmpty()) {
            sql.append("AND DATE(a.created_at) >= ? ");
            params.add(startDate.trim());
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            sql.append("AND DATE(a.created_at) <= ? ");
            params.add(endDate.trim());
        }

        sql.append("ORDER BY a.created_at DESC");

        try {
            conn = DatabaseConnection.getConnection();
            pstmt = conn.prepareStatement(sql.toString());

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            rs = pstmt.executeQuery();

            // 3. DTO 매핑
            while (rs.next()) {
                AuditLogDTO dto = new AuditLogDTO();
                
                dto.setLogId(rs.getLong("log_id"));
                
                // actor_id가 NULL일 수 있으므로 rs.wasNull() 체크
                long actorId = rs.getLong("actor_id");
                if (rs.wasNull()) {
                    dto.setActorId(null);
                } else {
                    dto.setActorId(actorId);
                }
                
                dto.setActorName(rs.getString("actor_name"));
                dto.setTargetTable(rs.getString("target_table"));
                dto.setTargetId(rs.getLong("target_id"));
                dto.setAction(rs.getString("action"));
                dto.setColumnName(rs.getString("column_name"));
                dto.setOldValue(rs.getString("old_value"));
                dto.setNewValue(rs.getString("new_value"));
                
                // Timestamp -> LocalDateTime 변환
                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    dto.setCreatedAt(createdAt.toLocalDateTime());
                }

                logList.add(dto);
            }
        } catch (SQLException e) {
            // 실무에서는 e.printStackTrace() 대신 전역 로거 활용 권장
            e.printStackTrace(); 
            throw new RuntimeException("감사 로그 조회 중 데이터베이스 오류 발생", e);
        } finally {
            // 4. 자원 역순 해제
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }

        return logList;
    }
}