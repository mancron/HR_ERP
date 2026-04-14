package com.hrms.sys.dao;

import com.hrms.common.db.DatabaseConnection;
import com.hrms.sys.dto.RagSuccessLogDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * rag_success_log 테이블 CRUD DAO.
 * 비즈니스 로직 없이 순수 DB 접근만 담당.
 *
 * PreparedStatement 사용 — SQL Injection 원천 차단 ✅
 */
public class RagSuccessLogDAO {

    // ──────────────────────────────────────────────
    // INSERT — 성공 로그 1건 저장
    // ──────────────────────────────────────────────
    public void insert(Connection conn, RagSuccessLogDTO dto) throws SQLException {
        String sql =
            "INSERT INTO rag_success_log " +
            "(question, generated_sql, category, row_count, used_rag, similarity) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dto.getQuestion());
            ps.setString(2, dto.getGeneratedSql());
            ps.setString(3, dto.getCategory());
            ps.setInt   (4, dto.getRowCount());
            ps.setInt   (5, dto.isUsedRag() ? 1 : 0);
            if (dto.getSimilarity() != null) {
                ps.setDouble(6, dto.getSimilarity());
            } else {
                ps.setNull(6, java.sql.Types.DECIMAL);
            }
            ps.executeUpdate();
        }
    }

    // ──────────────────────────────────────────────
    // SELECT — 벡터 DB 미반영 로그 조회 (배치용)
    // ──────────────────────────────────────────────
    public List<RagSuccessLogDTO> findPending(Connection conn) throws SQLException {
        String sql =
            "SELECT log_id, question, generated_sql, category, row_count, used_rag, similarity " +
            "FROM rag_success_log " +
            "WHERE is_upserted = 0 " +
            "ORDER BY created_at ASC";

        List<RagSuccessLogDTO> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                RagSuccessLogDTO dto = new RagSuccessLogDTO();
                dto.setLogId       (rs.getLong  ("log_id"));
                dto.setQuestion    (rs.getString ("question"));
                dto.setGeneratedSql(rs.getString ("generated_sql"));
                dto.setCategory    (rs.getString ("category"));
                dto.setRowCount    (rs.getInt    ("row_count"));
                dto.setUsedRag     (rs.getInt    ("used_rag") == 1);
                double sim = rs.getDouble("similarity");
                dto.setSimilarity  (rs.wasNull() ? null : sim);
                list.add(dto);
            }
        }
        return list;
    }

    // ──────────────────────────────────────────────
    // UPDATE — 벡터 DB 반영 완료 표시
    // ──────────────────────────────────────────────
    public void markUpserted(Connection conn, long logId) throws SQLException {
        String sql = "UPDATE rag_success_log SET is_upserted = 1 WHERE log_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, logId);
            ps.executeUpdate();
        }
    }
}
