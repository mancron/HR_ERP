package com.hrms.eval.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.hrms.common.db.DatabaseConnection;
import com.hrms.eval.dto.EvaluationDTO;
import com.hrms.eval.dto.EvaluationItemDTO;

public class EvaluationDAO {

    /**
     * 인사 평가 정보 저장 (신규 저장 또는 기존 데이터 수정)
     */
    public boolean insertEvaluation(EvaluationDTO eval, List<EvaluationItemDTO> items) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        boolean success = false;

        String sqlEval = "INSERT INTO evaluation (emp_id, eval_year, eval_period, eval_type, total_score, grade, eval_comment, eval_status, evaluator_id) "
                       + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                       + "ON DUPLICATE KEY UPDATE "
                       + "total_score = VALUES(total_score), "
                       + "grade = VALUES(grade), "
                       + "eval_comment = VALUES(eval_comment), "
                       + "eval_status = VALUES(eval_status), "
                       + "evaluator_id = VALUES(evaluator_id), "
                       + "confirmed_at = IF(VALUES(eval_status) = '최종확정', NOW(), confirmed_at)";

        String sqlItem    = "INSERT INTO evaluation_item (eval_id, item_name, score) VALUES (?, ?, ?)";
        String sqlDelItems = "DELETE FROM evaluation_item WHERE eval_id = ?";

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            pstmt = conn.prepareStatement(sqlEval, Statement.RETURN_GENERATED_KEYS);
            pstmt.setInt(1, eval.getEmpId());
            pstmt.setInt(2, eval.getEvalYear());
            pstmt.setString(3, eval.getEvalPeriod());
            pstmt.setString(4, eval.getEvalType());
            pstmt.setBigDecimal(5, eval.getTotalScore());
            pstmt.setString(6, eval.getGrade());
            pstmt.setString(7, eval.getEvalComment());
            pstmt.setString(8, eval.getEvalStatus());
            if (eval.getEvaluatorId() != null) {
                pstmt.setInt(9, eval.getEvaluatorId());
            } else {
                pstmt.setNull(9, Types.INTEGER);
            }
            pstmt.executeUpdate();

            int targetEvalId = 0;
            rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                targetEvalId = rs.getInt(1);
            } else {
                String findIdSql = "SELECT eval_id FROM evaluation WHERE emp_id=? AND eval_year=? AND eval_period=? AND eval_type=?";
                try (PreparedStatement psId = conn.prepareStatement(findIdSql)) {
                    psId.setInt(1, eval.getEmpId());
                    psId.setInt(2, eval.getEvalYear());
                    psId.setString(3, eval.getEvalPeriod());
                    psId.setString(4, eval.getEvalType());
                    try (ResultSet rsId = psId.executeQuery()) {
                        if (rsId.next()) targetEvalId = rsId.getInt(1);
                    }
                }
            }

            try (PreparedStatement psDel = conn.prepareStatement(sqlDelItems)) {
                psDel.setInt(1, targetEvalId);
                psDel.executeUpdate();
            }

            if (pstmt != null) pstmt.close();
            pstmt = conn.prepareStatement(sqlItem);
            for (EvaluationItemDTO item : items) {
                pstmt.setInt(1, targetEvalId);
                pstmt.setString(2, item.getItemName());
                pstmt.setBigDecimal(3, item.getScore());
                pstmt.addBatch();
            }
            pstmt.executeBatch();

            conn.commit();
            success = true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
        } finally {
            closeResources(rs, pstmt, conn);
        }
        return success;
    }

    /**
     * 사원 목록 조회
     */
    public Vector<Map<String, Object>> getEmployeeList() {
        Vector<Map<String, Object>> list = new Vector<>();
        String sql = "SELECT emp_id, emp_name FROM employee ORDER BY emp_name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("empId",   rs.getInt("emp_id"));
                map.put("empName", rs.getString("emp_name"));
                map.put("pos", "사원");
                list.add(map);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /**
     * 평가 항목 명칭 조회
     */
    public Vector<String> getEvaluationItemNames() {
        Vector<String> items = new Vector<>();
        items.add("업무성과");
        items.add("직무역량");
        items.add("조직기여도");
        items.add("리더십");
        return items;
    }

    /**
     * 평가 현황 목록 조회
     */
    public Vector<Map<String, Object>> getEvaluationStatusList(int year, String period, String type) {
        Vector<Map<String, Object>> list = new Vector<>();
        String sql = "SELECT e.eval_id, emp.emp_name, '개발1팀' as dept_name, e.total_score, e.grade, " +
                     "e.eval_status, evalr.emp_name as evaluator_name, e.confirmed_at " +
                     "FROM evaluation e " +
                     "JOIN employee emp ON e.emp_id = emp.emp_id " +
                     "LEFT JOIN employee evalr ON e.evaluator_id = evalr.emp_id " +
                     "WHERE e.eval_year = ? AND e.eval_period = ? AND e.eval_type = ? " +
                     "ORDER BY e.created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, year);
            pstmt.setString(2, period);
            pstmt.setString(3, type);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("evalId",        rs.getInt("eval_id"));
                    map.put("empName",       rs.getString("emp_name"));
                    map.put("deptName",      rs.getString("dept_name"));
                    map.put("score",         rs.getBigDecimal("total_score"));
                    map.put("grade",         rs.getString("grade"));
                    map.put("status",        rs.getString("eval_status"));
                    map.put("evaluatorName", rs.getString("evaluator_name"));
                    map.put("confirmedAt",   rs.getTimestamp("confirmed_at"));
                    list.add(map);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /**
     * 등급별 요약 통계 조회
     */
    public Map<String, Integer> getEvaluationSummary(int year, String period, String type) {
        Map<String, Integer> summary = new HashMap<>();
        summary.put("S", 0); summary.put("A", 0); summary.put("B", 0);
        summary.put("C", 0); summary.put("D", 0); summary.put("미완료", 0);

        String sql = "SELECT grade, eval_status, COUNT(*) as cnt FROM evaluation " +
                     "WHERE eval_year = ? AND eval_period = ? AND eval_type = ? " +
                     "GROUP BY grade, eval_status";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, year);
            pstmt.setString(2, period);
            pstmt.setString(3, type);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String status = rs.getString("eval_status");
                    String grade  = rs.getString("grade");
                    int count     = rs.getInt("cnt");
                    if ("작성중".equals(status)) {
                        summary.put("미완료", summary.get("미완료") + count);
                    } else if (grade != null) {
                        summary.put(grade, count);
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return summary;
    }

    // ── 신규 추가 ─────────────────────────────────────────────

    /**
     * evalId로 평가 단건 조회 (수정 폼 진입 시 사용)
     * 반환 키: evalId, empId, empName, evalYear, evalPeriod, evalType,
     *          totalScore, grade, evalComment, evalStatus, confirmedAt
     */
    public Map<String, Object> getEvaluationById(int evalId) {
        Map<String, Object> map = new HashMap<>();
        String sql = "SELECT e.eval_id, e.emp_id, emp.emp_name, " +
                     "e.eval_year, e.eval_period, e.eval_type, " +
                     "e.total_score, e.grade, e.eval_comment, " +
                     "e.eval_status, e.confirmed_at " +
                     "FROM evaluation e " +
                     "JOIN employee emp ON e.emp_id = emp.emp_id " +
                     "WHERE e.eval_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, evalId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    map.put("evalId",      rs.getInt("eval_id"));
                    map.put("empId",       rs.getInt("emp_id"));
                    map.put("empName",     rs.getString("emp_name"));
                    map.put("evalYear",    rs.getInt("eval_year"));
                    map.put("evalPeriod",  rs.getString("eval_period"));
                    map.put("evalType",    rs.getString("eval_type"));
                    map.put("totalScore",  rs.getBigDecimal("total_score"));
                    map.put("grade",       rs.getString("grade"));
                    map.put("evalComment", rs.getString("eval_comment"));
                    map.put("evalStatus",  rs.getString("eval_status"));
                    map.put("confirmedAt", rs.getTimestamp("confirmed_at"));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return map;
    }

    /**
     * evalId에 해당하는 항목별 점수를 itemNames 순서에 맞춰 반환
     * 항목이 없으면 기본값 80으로 채움
     */
    public List<BigDecimal> getItemScoresByEvalId(int evalId, Vector<String> itemNames) {
        Map<String, BigDecimal> scoreMap = new HashMap<>();
        String sql = "SELECT item_name, score FROM evaluation_item WHERE eval_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, evalId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    scoreMap.put(rs.getString("item_name"), rs.getBigDecimal("score"));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        List<BigDecimal> result = new ArrayList<>();
        for (String name : itemNames) {
            result.add(scoreMap.getOrDefault(name, new BigDecimal("80")));
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────

    private void closeResources(ResultSet rs, PreparedStatement pstmt, Connection conn) {
        try {
            if (rs != null)    rs.close();
            if (pstmt != null) pstmt.close();
            if (conn != null)  conn.close();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}