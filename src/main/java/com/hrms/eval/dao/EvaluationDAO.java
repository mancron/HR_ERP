package com.hrms.eval.dao;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

import com.hrms.common.db.DatabaseConnection;
import com.hrms.eval.dto.EvaluationDTO;
import com.hrms.eval.dto.EvaluationItemDTO;

public class EvaluationDAO {

    // 1. 저장 / 수정 로직 (기존 유지)
    public boolean insertEvaluation(EvaluationDTO eval, List<EvaluationItemDTO> items) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        boolean success = false;

        try (Connection connCheck = DatabaseConnection.getConnection();
             PreparedStatement psCheck = connCheck.prepareStatement(
                     "SELECT eval_status, evaluator_id FROM evaluation "
                     + "WHERE emp_id=? AND eval_year=? AND eval_period=? AND eval_type=?")) {
            
            psCheck.setInt(1, eval.getEmpId()); 
            psCheck.setInt(2, eval.getEvalYear());
            psCheck.setString(3, eval.getEvalPeriod()); 
            psCheck.setString(4, eval.getEvalType());
            
            try (ResultSet rsCheck = psCheck.executeQuery()) {
                if (rsCheck.next()) {
                    String status = rsCheck.getString("eval_status");
                    int existingEvaluatorId = rsCheck.getInt("evaluator_id");
                    if ("최종확정".equals(status)) return false; 
                    if (existingEvaluatorId != 0 && eval.getEvaluatorId() != null 
                        && existingEvaluatorId != eval.getEvaluatorId()) {
                        return false;
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        String sqlEval = "INSERT INTO evaluation "
                + "(emp_id, eval_year, eval_period, eval_type, total_score, grade, "
                + " eval_comment, eval_status, evaluator_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE "
                + "total_score    = VALUES(total_score), "
                + "grade          = VALUES(grade), "
                + "eval_comment = REPLACE(VALUES(eval_comment), '[반려] ', ''), "
                + "eval_status    = VALUES(eval_status), "
                + "evaluator_id = VALUES(evaluator_id), "
                + "confirmed_at = IF(VALUES(eval_status)='최종확정', NOW(), confirmed_at)";

        String sqlItem     = "INSERT INTO evaluation_item (eval_id, item_name, score, max_score) VALUES (?,?,?,?)";
        String sqlDelItems = "DELETE FROM evaluation_item WHERE eval_id = ?";

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            pstmt = conn.prepareStatement(sqlEval, Statement.RETURN_GENERATED_KEYS);
            pstmt.setInt(1,        eval.getEmpId());
            pstmt.setInt(2,        eval.getEvalYear());
            pstmt.setString(3,     eval.getEvalPeriod());
            pstmt.setString(4,     eval.getEvalType());
            pstmt.setBigDecimal(5, eval.getTotalScore());
            pstmt.setString(6,     eval.getGrade());
            pstmt.setString(7,     eval.getEvalComment());
            pstmt.setString(8,     eval.getEvalStatus());
            if (eval.getEvaluatorId() != null) pstmt.setInt(9, eval.getEvaluatorId());
            else pstmt.setNull(9, Types.INTEGER);
            pstmt.executeUpdate();

            int targetEvalId = 0;
            rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                targetEvalId = rs.getInt(1);
            } else {
                String findSql = "SELECT eval_id FROM evaluation WHERE emp_id=? AND eval_year=? AND eval_period=? AND eval_type=?";
                try (PreparedStatement psId = conn.prepareStatement(findSql)) {
                    psId.setInt(1, eval.getEmpId()); psId.setInt(2, eval.getEvalYear());
                    psId.setString(3, eval.getEvalPeriod()); psId.setString(4, eval.getEvalType());
                    try (ResultSet rsId = psId.executeQuery()) {
                        if (rsId.next()) targetEvalId = rsId.getInt(1);
                    }
                }
            }

            try (PreparedStatement psDel = conn.prepareStatement(sqlDelItems)) {
                psDel.setInt(1, targetEvalId); 
                psDel.executeUpdate();
            }

            pstmt.close();
            pstmt = conn.prepareStatement(sqlItem);
            for (EvaluationItemDTO item : items) {
                pstmt.setInt(1, targetEvalId);
                pstmt.setString(2, item.getItemName());
                pstmt.setBigDecimal(3, item.getScore());
                pstmt.setBigDecimal(4, item.getMaxScore() != null ? item.getMaxScore() : new BigDecimal("100"));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            conn.commit();
            success = true;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
        } finally { closeResources(rs, pstmt, conn); }
        return success;
    }

    public int confirmEvaluationWithLog(int evalId, int actorId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        int targetEmpId = -1;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            String oldStatus = "작성중";
            try (PreparedStatement psGet = conn.prepareStatement("SELECT eval_status, emp_id FROM evaluation WHERE eval_id=?")) {
                psGet.setInt(1, evalId);
                try (ResultSet rsGet = psGet.executeQuery()) {
                    if (rsGet.next()) {
                        oldStatus = rsGet.getString("eval_status");
                        targetEmpId = rsGet.getInt("emp_id");
                    }
                }
            }
            String sqlConfirm = "UPDATE evaluation SET eval_status='최종확정', confirmed_at=NOW() WHERE eval_id=?";
            pstmt = conn.prepareStatement(sqlConfirm);
            pstmt.setInt(1, evalId);
            pstmt.executeUpdate();
            pstmt.close();
            String sqlLog = "INSERT INTO audit_log (actor_id, target_table, target_id, action, column_name, old_value, new_value) VALUES (?, 'evaluation', ?, 'UPDATE', 'eval_status', ?, '최종확정')";
            pstmt = conn.prepareStatement(sqlLog);
            if (actorId > 0) pstmt.setInt(1, actorId); else pstmt.setNull(1, Types.INTEGER);
            pstmt.setInt(2, evalId); pstmt.setString(3, oldStatus);
            pstmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            targetEmpId = -1;
        } finally { closeResources(null, pstmt, conn); }
        return targetEmpId;
    }

    public int rejectEvaluationWithLog(int evalId, int actorId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        int evaluatorId = -1;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            String oldStatus = "작성중";
            try (PreparedStatement psGet = conn.prepareStatement("SELECT eval_status, evaluator_id FROM evaluation WHERE eval_id=?")) {
                psGet.setInt(1, evalId);
                try (ResultSet rsGet = psGet.executeQuery()) {
                    if (rsGet.next()) {
                        oldStatus = rsGet.getString("eval_status");
                        evaluatorId = rsGet.getInt("evaluator_id");
                    }
                }
            }
            String sqlReject = "UPDATE evaluation SET eval_status='작성중', eval_comment = CASE WHEN eval_comment LIKE '[반려] %' THEN eval_comment ELSE CONCAT('[반려] ', COALESCE(eval_comment, '')) END, confirmed_at = NULL WHERE eval_id=?";
            pstmt = conn.prepareStatement(sqlReject);
            pstmt.setInt(1, evalId);
            pstmt.executeUpdate();
            String sqlLog = "INSERT INTO audit_log (actor_id, target_table, target_id, action, column_name, old_value, new_value) VALUES (?, 'evaluation', ?, 'UPDATE', 'eval_status', ?, '작성중(반려)')";
            pstmt = conn.prepareStatement(sqlLog);
            if (actorId > 0) pstmt.setInt(1, actorId); else pstmt.setNull(1, Types.INTEGER);
            pstmt.setInt(2, evalId); pstmt.setString(3, oldStatus);
            pstmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            evaluatorId = -1;
        } finally { closeResources(null, pstmt, conn); }
        return evaluatorId;
    }

    // 2. 조회 로직 (추가 및 수정)

    /**
     * [추가] 특정 조건의 평가 데이터 로드 (M-3 대응)
     * Service의 loadExistingEval에서 호출하는 바로 그 메서드입니다.
     */
    public Map<String, Object> getEvaluationByCondition(int empId, int year, String period, String type, int evaluatorId) {
        String sql = "SELECT eval_id, eval_status, eval_comment FROM evaluation "
                + "WHERE emp_id=? AND eval_year=? AND eval_period=? "
                + "AND eval_type=? AND evaluator_id=? AND eval_status != '최종확정'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, empId); pstmt.setInt(2, year);
            pstmt.setString(3, period); pstmt.setString(4, type);
            pstmt.setInt(5, evaluatorId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("evalId",      rs.getInt("eval_id"));
                    map.put("evalStatus",  rs.getString("eval_status"));
                    map.put("evalComment", rs.getString("eval_comment"));
                    return map;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public Map<String, Object> getEvaluationById(int evalId) {
        Map<String, Object> map = new HashMap<>();
        String sql = "SELECT e.*, emp.emp_name, evalr.emp_name AS evaluator_name " +
                     "FROM evaluation e " +
                     "JOIN employee emp ON e.emp_id = emp.emp_id " +
                     "LEFT JOIN employee evalr ON e.evaluator_id = evalr.emp_id " +
                     "WHERE e.eval_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, evalId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    map.put("evalId", rs.getInt("eval_id")); 
                    map.put("empId", rs.getInt("emp_id"));
                    map.put("empName", rs.getString("emp_name")); 
                    map.put("evalYear", rs.getInt("eval_year"));
                    map.put("evalPeriod", rs.getString("eval_period")); 
                    map.put("evalType", rs.getString("eval_type"));
                    map.put("totalScore", rs.getBigDecimal("total_score")); 
                    map.put("grade", rs.getString("grade"));
                    map.put("evalComment", rs.getString("eval_comment")); 
                    map.put("evalStatus", rs.getString("eval_status"));
                    map.put("evaluatorId", rs.getInt("evaluator_id"));
                    map.put("evaluatorName", rs.getString("evaluator_name")); 
                    map.put("confirmedAt", rs.getTimestamp("confirmed_at"));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return map;
    }

    public List<BigDecimal> getItemScoresByEvalId(int evalId, Vector<String> itemNames) {
        Map<String, BigDecimal> scoreMap = new HashMap<>();
        String sql = "SELECT item_name, score FROM evaluation_item WHERE eval_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, evalId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) scoreMap.put(rs.getString("item_name"), rs.getBigDecimal("score"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        List<BigDecimal> result = new ArrayList<>();
        for (String name : itemNames) result.add(scoreMap.getOrDefault(name, new BigDecimal("80")));
        return result;
    }

    public Vector<Map<String, Object>> getEvaluationStatusList(int year, String period, String type, String searchTarget, String searchEvaluator) {
        Vector<Map<String, Object>> list = new Vector<>();
        StringBuilder sql = new StringBuilder("SELECT e.eval_id, emp.emp_name, COALESCE(d.dept_name,'미지정') AS dept_name, e.total_score, e.grade, e.eval_status, e.evaluator_id, evalr.emp_name AS evaluator_name, e.confirmed_at, e.eval_comment, e.eval_year, e.eval_period, e.eval_type FROM evaluation e JOIN employee emp ON e.emp_id = emp.emp_id LEFT JOIN department d ON emp.dept_id = d.dept_id LEFT JOIN employee evalr ON e.evaluator_id = evalr.emp_id WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        sql.append("AND e.eval_year=? "); params.add(year);
        if (period != null && !period.isEmpty() && !"전체".equals(period)) { sql.append("AND e.eval_period=? "); params.add(period); }
        if (type != null && !type.isEmpty() && !"전체".equals(type)) { sql.append("AND e.eval_type=? "); params.add(type); }
        if (searchTarget != null && !searchTarget.trim().isEmpty()) { sql.append("AND emp.emp_name LIKE ? "); params.add("%" + searchTarget.trim() + "%"); }
        if (searchEvaluator != null && !searchEvaluator.trim().isEmpty()) { sql.append("AND evalr.emp_name LIKE ? "); params.add("%" + searchEvaluator.trim() + "%"); }
        sql.append("ORDER BY e.eval_year DESC, e.created_at DESC");
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) pstmt.setObject(i + 1, params.get(i));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    String evalComment = rs.getString("eval_comment");
                    map.put("evalId", rs.getInt("eval_id")); map.put("empName", rs.getString("emp_name"));
                    map.put("deptName", rs.getString("dept_name")); map.put("score", rs.getBigDecimal("total_score"));
                    map.put("grade", rs.getString("grade")); map.put("status", rs.getString("eval_status"));
                    map.put("evaluatorId", rs.getInt("evaluator_id")); map.put("evaluatorName", rs.getString("evaluator_name"));
                    map.put("confirmedAt", rs.getTimestamp("confirmed_at")); map.put("evalComment", evalComment);
                    map.put("isRejected", evalComment != null && evalComment.startsWith("[반려]"));
                    map.put("evalYear", rs.getInt("eval_year")); map.put("evalPeriod", rs.getString("eval_period"));
                    map.put("evalType", rs.getString("eval_type"));
                    list.add(map);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public Vector<Map<String, Object>> getEvaluationStatusList(int year, String period, String type) { return getEvaluationStatusList(year, period, type, null, null); }

    public Map<String, Integer> getEvaluationSummary(int year, String period, String type, String searchTarget, String searchEvaluator) {
        Map<String, Integer> summary = new HashMap<>();
        summary.put("S", 0); summary.put("A", 0); summary.put("B", 0); summary.put("C", 0); summary.put("D", 0); summary.put("미완료", 0);
        StringBuilder whereClause = new StringBuilder(" FROM evaluation e JOIN employee target ON e.emp_id = target.emp_id LEFT JOIN employee eval ON e.evaluator_id = eval.emp_id WHERE e.eval_year = ? ");
        List<Object> params = new ArrayList<>(); params.add(year);
        if (period != null && !"전체".equals(period) && !period.isEmpty()) { whereClause.append(" AND e.eval_period = ? "); params.add(period); }
        if (type != null && !"전체".equals(type) && !type.isEmpty()) { whereClause.append(" AND e.eval_type = ? "); params.add(type); }
        if (searchTarget != null && !searchTarget.isEmpty()) { whereClause.append(" AND target.emp_name LIKE ? "); params.add("%" + searchTarget + "%"); }
        if (searchEvaluator != null && !searchEvaluator.isEmpty()) { whereClause.append(" AND eval.emp_name LIKE ? "); params.add("%" + searchEvaluator + "%"); }
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sqlGrade = "SELECT e.grade, COUNT(*) AS cnt " + whereClause.toString() + " AND e.eval_status = '최종확정' GROUP BY e.grade";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlGrade)) {
                for (int i = 0; i < params.size(); i++) pstmt.setObject(i + 1, params.get(i));
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String grade = rs.getString("grade");
                        if (grade != null && summary.containsKey(grade)) summary.put(grade, rs.getInt("cnt"));
                    }
                }
            }
            String sqlIncomplete = "SELECT COUNT(*) " + whereClause.toString() + " AND e.eval_status != '최종확정'";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlIncomplete)) {
                for (int i = 0; i < params.size(); i++) pstmt.setObject(i + 1, params.get(i));
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) summary.put("미완료", rs.getInt(1));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return summary;
    }

    public boolean isAlreadyConfirmed(int empId, int year, String period, String type) {
        String sql = "SELECT COUNT(*) FROM evaluation WHERE emp_id=? AND eval_year=? AND eval_period=? AND eval_type=? AND eval_status='최종확정'";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, empId); pstmt.setInt(2, year); pstmt.setString(3, period); pstmt.setString(4, type);
            try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) return rs.getInt(1) > 0; }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public int getPositionLevelByEmpId(int empId) {
        String sql = "SELECT p.position_level FROM employee e JOIN job_position p ON e.position_id = p.position_id WHERE e.emp_id=?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, empId);
            try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) return rs.getInt("position_level"); }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public Vector<Map<String, Object>> getEmployeeListForEvaluator(int evaluatorId, int posLevel, String evalType) {
        Vector<Map<String, Object>> list = new Vector<>();
        String sql = ""; 
        boolean needSecondParam = true;
     // 1. 사장님(Level 6)인 경우: 모든 조건을 무시하고 전체 조회 (가장 우선순위)
        if (posLevel == 6) {
            sql = "SELECT e.emp_id, e.emp_name, COALESCE(p.position_name,'사원') AS pos " +
                  "FROM employee e LEFT JOIN job_position p ON e.position_id = p.position_id " +
                  "WHERE e.status='재직' AND e.emp_id != ? " + 
                  "ORDER BY p.position_level DESC, e.emp_name ASC";
            needSecondParam = false; 
        } 
        // 2. 사장님이 아닐 때만 기존 평가 타입별 로직 수행 (else if로 연결)
        else if ("자기평가".equals(evalType)) {
            sql = "SELECT e.emp_id, e.emp_name, COALESCE(p.position_name,'사원') AS pos " +
                  "FROM employee e LEFT JOIN job_position p ON e.position_id = p.position_id " +
                  "WHERE e.emp_id = ?";
            needSecondParam = false;
        } 
        else if ("동료평가".equals(evalType)) {
            sql = "SELECT e.emp_id, e.emp_name, COALESCE(p.position_name,'사원') AS pos " +
                  "FROM employee e LEFT JOIN job_position p ON e.position_id = p.position_id " +
                  "WHERE e.status='재직' AND e.emp_id != ? AND p.position_level = ? " +
                  "ORDER BY e.emp_name ASC";
        } 
        else if ("하위평가".equals(evalType)) {
            sql = "SELECT e.emp_id, e.emp_name, COALESCE(p.position_name,'사원') AS pos " +
                  "FROM employee e " +
                  "LEFT JOIN job_position p ON e.position_id = p.position_id " +
                  "WHERE e.dept_id = (SELECT dept_id FROM employee WHERE emp_id = ?) " +
                  "AND p.position_level > ? " + 
                  "AND e.status = '재직' " +
                  "ORDER BY p.position_level ASC, e.emp_name ASC";
        } 
        else { // 상위평가
            sql = "SELECT e.emp_id, e.emp_name, COALESCE(p.position_name,'사원') AS pos " +
                  "FROM employee e LEFT JOIN job_position p ON e.position_id = p.position_id " +
                  "WHERE e.status='재직' AND e.emp_id != ? AND p.position_level < ? " +
                  "ORDER BY p.position_level DESC, e.emp_name ASC";
        }

        try (Connection conn = DatabaseConnection.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, evaluatorId);
            if (needSecondParam) {
                pstmt.setInt(2, posLevel);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("empId", rs.getInt("emp_id"));
                    map.put("empName", rs.getString("emp_name"));
                    map.put("pos", rs.getString("pos"));
                    list.add(map);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Vector<Map<String, Object>> getEmployeeListForEvaluator(int evaluatorId, int posLevel) { return getEmployeeListForEvaluator(evaluatorId, posLevel, "상위평가"); }

    public Vector<Map<String, Object>> getEmployeeList() {
        Vector<Map<String, Object>> list = new Vector<>();
        String sql = "SELECT e.emp_id, e.emp_name, COALESCE(p.position_name,'사원') AS pos FROM employee e LEFT JOIN job_position p ON e.position_id = p.position_id WHERE e.status='재직' ORDER BY p.position_level DESC, e.emp_name ASC";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("empId", rs.getInt("emp_id")); map.put("empName", rs.getString("emp_name")); map.put("pos", rs.getString("pos"));
                list.add(map);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public Vector<String> getEvaluationItemNames() {
        Vector<String> items = new Vector<>();
        items.add("업무성과"); items.add("직무역량"); items.add("조직기여도"); items.add("리더십");
        return items;
    }

    private void closeResources(ResultSet rs, PreparedStatement pstmt, Connection conn) {
        try { if (rs != null) rs.close(); if (pstmt != null) pstmt.close(); if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
    }
}