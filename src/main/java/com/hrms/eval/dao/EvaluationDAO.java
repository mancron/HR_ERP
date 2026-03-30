package com.hrms.eval.dao;

import com.hrms.eval.dto.EvaluationDTO;
import com.hrms.eval.dto.EvaluationItemDTO;
import com.hrms.util.DatabaseConnection; // 수정된 연결 클래스 사용

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class EvaluationDAO {

    public boolean insertEvaluation(EvaluationDTO eval, List<EvaluationItemDTO> items) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        boolean success = false;

        // 1. evaluation 테이블 insert 쿼리
        String sqlEval = "INSERT INTO evaluation (emp_id, eval_year, eval_period, eval_type, total_score, grade, eval_comment, eval_status, evaluator_id) "
                       + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        // 2. evaluation_item 테이블 insert 쿼리
        String sqlItem = "INSERT INTO evaluation_item (eval_id, item_name, score) VALUES (?, ?, ?)";

        try {
            // HikariCP로부터 커넥션 획득
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // 트랜잭션 시작 (자동 커밋 방지)

            // [STEP 1] Evaluation 메인 정보 저장
            pstmt = conn.prepareStatement(sqlEval, Statement.RETURN_GENERATED_KEYS);
            pstmt.setInt(1, eval.getEmpId());
            pstmt.setInt(2, eval.getEvalYear());
            pstmt.setString(3, eval.getEvalPeriod());
            pstmt.setString(4, eval.getEvalType());
            pstmt.setBigDecimal(5, eval.getTotalScore());
            pstmt.setString(6, eval.getGrade());
            pstmt.setString(7, eval.getEvalComment());
            pstmt.setString(8, eval.getEvalStatus());
            
            // evaluator_id가 null일 수 있으므로 setSystem 처리
            if (eval.getEvaluatorId() != null) {
                pstmt.setInt(9, eval.getEvaluatorId());
            } else {
                pstmt.setNull(9, Types.INTEGER);
            }
            
            pstmt.executeUpdate();

            // 생성된 eval_id(PK) 가져오기
            rs = pstmt.getGeneratedKeys();
            int generatedEvalId = 0;
            if (rs.next()) {
                generatedEvalId = rs.getInt(1);
            }

            // [STEP 2] 상세 항목들 저장 (Batch 처리로 성능 최적화)
            if (pstmt != null) pstmt.close(); // 이전 pstmt 닫기
            pstmt = conn.prepareStatement(sqlItem);
            
            for (EvaluationItemDTO item : items) {
                pstmt.setInt(1, generatedEvalId);
                pstmt.setString(2, item.getItemName());
                pstmt.setBigDecimal(3, item.getScore());
                pstmt.addBatch(); // 일괄 처리를 위해 예약
            }
            pstmt.executeBatch(); // 한 번에 실행

            conn.commit(); // 모든 과정 성공 시 최종 반영
            success = true;

        } catch (SQLException e) {
            // 하나라도 실패하면 롤백
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
        } finally {
            // HikariCP 커넥션 반납 (닫는 것이 아니라 풀로 돌려보냄)
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close(); 
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return success;
    }
    
    public Vector<Map<String, Object>> getEmployeeList() {
        Vector<Map<String, Object>> list = new Vector<>(); // ArrayList 대신 Vector 사용
        String sql = "SELECT emp_id, emp_name, position_name FROM employee ORDER BY emp_name";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("empId", rs.getInt("emp_id"));
                map.put("empName", rs.getString("emp_name"));
                map.put("pos", rs.getString("position_name"));
                list.add(map);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // [추가] 2. 평가 항목 명칭 조회
    public Vector<String> getEvaluationItemNames() {
        Vector<String> items = new Vector<>();
        items.add("업무성과");
        items.add("직무역량");
        items.add("조직기여도");
        items.add("리더십");
        return items;
    }
}