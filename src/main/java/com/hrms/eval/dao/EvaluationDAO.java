package com.hrms.eval.dao;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

import com.hrms.common.db.DatabaseConnection;
import com.hrms.eval.dto.EvaluationDTO;
import com.hrms.eval.dto.EvaluationItemDTO;

public class EvaluationDAO {

	// ═══════════════════════════════════════════════════════
	// 저장 / 수정
	// ═══════════════════════════════════════════════════════

	/**
	 * 평가 저장 (신규 or ON DUPLICATE KEY UPDATE) DB CHECK: eval_status IN ('작성중','최종확정')
	 * [반려] 태그는 REPLACE로 자동 제거 (반려 후 재제출 시 정상화)
	 */
	public boolean insertEvaluation(EvaluationDTO eval, List<EvaluationItemDTO> items) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		boolean success = false;

		// 이미 최종확정된 건이면 저장 차단 (DAO 이중 방어)
		try (Connection connCheck = DatabaseConnection.getConnection();
				PreparedStatement psCheck = connCheck.prepareStatement("SELECT eval_status FROM evaluation "
						+ "WHERE emp_id=? AND eval_year=? AND eval_period=? AND eval_type=?")) {
			psCheck.setInt(1, eval.getEmpId());
			psCheck.setInt(2, eval.getEvalYear());
			psCheck.setString(3, eval.getEvalPeriod());
			psCheck.setString(4, eval.getEvalType());
			try (ResultSet rsCheck = psCheck.executeQuery()) {
				if (rsCheck.next() && "최종확정".equals(rsCheck.getString("eval_status"))) {
					return false;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		String sqlEval = "INSERT INTO evaluation " + "(emp_id, eval_year, eval_period, eval_type, total_score, grade, "
				+ " eval_comment, eval_status, evaluator_id) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
				+ "ON DUPLICATE KEY UPDATE " + "total_score  = VALUES(total_score), " + "grade        = VALUES(grade), "
				+ "eval_comment = REPLACE(VALUES(eval_comment), '[반려] ', ''), " + "eval_status  = VALUES(eval_status), "
				+ "evaluator_id = VALUES(evaluator_id), "
				+ "confirmed_at = IF(VALUES(eval_status)='최종확정', NOW(), confirmed_at)";

		String sqlItem = "INSERT INTO evaluation_item (eval_id, item_name, score, max_score) VALUES (?,?,?,?)";
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
			if (eval.getEvaluatorId() != null)
				pstmt.setInt(9, eval.getEvaluatorId());
			else
				pstmt.setNull(9, Types.INTEGER);
			pstmt.executeUpdate();

			int targetEvalId = 0;
			rs = pstmt.getGeneratedKeys();
			if (rs.next()) {
				targetEvalId = rs.getInt(1);
			} else {
				String findSql = "SELECT eval_id FROM evaluation "
						+ "WHERE emp_id=? AND eval_year=? AND eval_period=? AND eval_type=?";
				try (PreparedStatement psId = conn.prepareStatement(findSql)) {
					psId.setInt(1, eval.getEmpId());
					psId.setInt(2, eval.getEvalYear());
					psId.setString(3, eval.getEvalPeriod());
					psId.setString(4, eval.getEvalType());
					try (ResultSet rsId = psId.executeQuery()) {
						if (rsId.next())
							targetEvalId = rsId.getInt(1);
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
			if (conn != null)
				try {
					conn.rollback();
				} catch (SQLException ex) {
					ex.printStackTrace();
				}
			e.printStackTrace();
		} finally {
			closeResources(rs, pstmt, conn);
		}
		return success;
	}

	/**
	 * 최종확정 처리 + audit_log 기록 (트랜잭션 통합) PosDAO의 updatePositionWithLog 방식과 동일하게 하나의
	 * 트랜잭션으로 처리
	 *
	 * @return 확정된 평가의 emp_id (알림 발송용), 실패 시 -1
	 */
	public int confirmEvaluationWithLog(int evalId, int actorId) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		int targetEmpId = -1;

		try {
			conn = DatabaseConnection.getConnection();
			conn.setAutoCommit(false);

			// 1. 현재 상태 조회 (audit_log old_value + 알림용 emp_id 확보)
			String oldStatus = "작성중";
			try (PreparedStatement psGet = conn
					.prepareStatement("SELECT eval_status, emp_id FROM evaluation WHERE eval_id=?")) {
				psGet.setInt(1, evalId);
				try (ResultSet rsGet = psGet.executeQuery()) {
					if (rsGet.next()) {
						oldStatus = rsGet.getString("eval_status");
						targetEmpId = rsGet.getInt("emp_id");
					}
				}
			}

			// 2. 평가 확정
			String sqlConfirm = "UPDATE evaluation SET eval_status='최종확정', confirmed_at=NOW() WHERE eval_id=?";
			pstmt = conn.prepareStatement(sqlConfirm);
			pstmt.setInt(1, evalId);
			pstmt.executeUpdate();
			pstmt.close();

			// 3. audit_log INSERT (PosDAO 방식과 동일)
			String sqlLog = "INSERT INTO audit_log "
					+ "(actor_id, target_table, target_id, action, column_name, old_value, new_value) "
					+ "VALUES (?, 'evaluation', ?, 'UPDATE', 'eval_status', ?, '최종확정')";
			pstmt = conn.prepareStatement(sqlLog);
			if (actorId > 0)
				pstmt.setInt(1, actorId);
			else
				pstmt.setNull(1, Types.INTEGER);
			pstmt.setInt(2, evalId);
			pstmt.setString(3, oldStatus);
			pstmt.executeUpdate();

			conn.commit();

		} catch (SQLException e) {
			if (conn != null)
				try {
					conn.rollback();
				} catch (SQLException ex) {
					ex.printStackTrace();
				}
			e.printStackTrace();
			targetEmpId = -1;
		} finally {
			closeResources(null, pstmt, conn);
		}
		return targetEmpId;
	}

	/**
	 * 반려 처리 + audit_log 기록 (트랜잭션 통합) rejectReason: HR담당자가 입력한 반려 사유 (빈 문자열 허용)
	 *
	 * 저장 형식: "[반려] 기존코멘트\n[반려 사유] 사유내용" 중복 반려 차단: 이미 [반려] 태그가 있으면 return -1
	 *
	 * @return 반려된 평가의 evaluator_id, 실패/중복반려 시 -1
	 */
	public int rejectEvaluationWithLog(int evalId, int actorId, String rejectReason) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		int evaluatorId = -1;

		try {
			conn = DatabaseConnection.getConnection();
			conn.setAutoCommit(false);

			// 1. 현재 상태 + 평가자 ID + 기존 코멘트 조회
			String oldStatus = "작성중";
			String currentComment = "";
			try (PreparedStatement psGet = conn.prepareStatement(
					"SELECT eval_status, evaluator_id, eval_comment FROM evaluation WHERE eval_id=?")) {
				psGet.setInt(1, evalId);
				try (ResultSet rsGet = psGet.executeQuery()) {
					if (rsGet.next()) {
						oldStatus = rsGet.getString("eval_status");
						evaluatorId = rsGet.getInt("evaluator_id");
						currentComment = rsGet.getString("eval_comment");
						if (currentComment == null)
							currentComment = "";
					}
				}
			}

			// 중복 반려 차단: 이미 [반려] 태그 있으면 처리 거부
			if (currentComment.contains("[반려]")) {
				conn.rollback();
				return -1;
			}

			// 2. eval_comment 구성: "[반려] 기존코멘트\n[반려 사유] 사유"
			String trimmedComment = currentComment.trim();
			String reasonPart = (rejectReason != null && !rejectReason.trim().isEmpty())
					? "\n[반려 사유] " + rejectReason.trim()
					: "";
			String newComment = "[반려] " + trimmedComment + reasonPart;

			String sqlReject = "UPDATE evaluation " + "SET eval_status='작성중', " + "    eval_comment = ?, "
					+ "    confirmed_at = NULL " + "WHERE eval_id=?";
			pstmt = conn.prepareStatement(sqlReject);
			pstmt.setString(1, newComment);
			pstmt.setInt(2, evalId);
			pstmt.executeUpdate();
			pstmt.close();

			// 3. audit_log INSERT
			String sqlLog = "INSERT INTO audit_log "
					+ "(actor_id, target_table, target_id, action, column_name, old_value, new_value) "
					+ "VALUES (?, 'evaluation', ?, 'UPDATE', 'eval_status', ?, '작성중(반려)')";
			pstmt = conn.prepareStatement(sqlLog);
			if (actorId > 0)
				pstmt.setInt(1, actorId);
			else
				pstmt.setNull(1, Types.INTEGER);
			pstmt.setInt(2, evalId);
			pstmt.setString(3, oldStatus);
			pstmt.executeUpdate();

			conn.commit();

		} catch (SQLException e) {
			if (conn != null)
				try {
					conn.rollback();
				} catch (SQLException ex) {
					ex.printStackTrace();
				}
			e.printStackTrace();
			evaluatorId = -1;
		} finally {
			closeResources(null, pstmt, conn);
		}
		return evaluatorId;
	}

	/** 기존 시그니처 호환 (rejectReason 없는 경우 빈 문자열) */
	public int rejectEvaluationWithLog(int evalId, int actorId) {
		return rejectEvaluationWithLog(evalId, actorId, "");
	}

	// ═══════════════════════════════════════════════════════
	// 조회
	// ═══════════════════════════════════════════════════════

	public Map<String, Object> getEvaluationById(int evalId) {
		Map<String, Object> map = new HashMap<>();
		String sql = "SELECT e.eval_id, e.emp_id, emp.emp_name, " + "e.eval_year, e.eval_period, e.eval_type, "
				+ "e.total_score, e.grade, e.eval_comment, " + "e.eval_status, e.evaluator_id, e.confirmed_at "
				+ "FROM evaluation e " + "JOIN employee emp ON e.emp_id = emp.emp_id " + "WHERE e.eval_id = ?";
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
					map.put("confirmedAt", rs.getTimestamp("confirmed_at"));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return map;
	}

	public List<BigDecimal> getItemScoresByEvalId(int evalId, Vector<String> itemNames) {
		Map<String, BigDecimal> scoreMap = new HashMap<>();
		String sql = "SELECT item_name, score FROM evaluation_item WHERE eval_id = ?";
		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setInt(1, evalId);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next())
					scoreMap.put(rs.getString("item_name"), rs.getBigDecimal("score"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		List<BigDecimal> result = new ArrayList<>();
		for (String name : itemNames)
			result.add(scoreMap.getOrDefault(name, new BigDecimal("80")));
		return result;
	}

	/**
	 * 평가 현황 목록 (대상자/평가자 이름 검색 포함) isRejected: eval_comment가 [반려]로 시작하면 true
	 */

	public Vector<Map<String, Object>> getEvaluationStatusList(int year, String period, String type,
            String searchTarget, String searchEvaluator, int loginEmpId, String userRole) {

        Vector<Map<String, Object>> list = new Vector<>();
        
        // [수정] 프로젝트 공통 권한 명칭인 "사장님", "HR담당자", "최종승인자"로 변경
        boolean isPrivileged = "사장님".equals(userRole) || "HR담당자".equals(userRole) || "최종승인자".equals(userRole);

        StringBuilder sql = new StringBuilder("SELECT e.eval_id, emp.emp_name, e.emp_id, " 
                + "COALESCE(d.dept_name,'미지정') AS dept_name, "
                + "e.total_score, e.grade, e.eval_status, e.evaluator_id, "
                + "evalr.emp_name AS evaluator_name, e.confirmed_at, e.eval_comment, "
                + "e.eval_year, e.eval_period, e.eval_type " 
                + "FROM evaluation e "
                + "JOIN employee emp ON e.emp_id = emp.emp_id " 
                + "LEFT JOIN department d ON emp.dept_id = d.dept_id "
                + "LEFT JOIN employee evalr ON e.evaluator_id = evalr.emp_id " 
                + "WHERE e.eval_year=? ");
        
        List<Object> params = new ArrayList<>();
        params.add(year);

        // 권한이 없는 일반 사용자는 본인이 대상자이거나 평가자인 데이터만 조회
        if (!isPrivileged && loginEmpId > 0) {
            sql.append("AND (e.emp_id = ? OR e.evaluator_id = ?) ");
            params.add(loginEmpId);
            params.add(loginEmpId);
        }

        if (period != null && !period.isEmpty() && !"전체".equals(period)) {
            sql.append("AND e.eval_period=? ");
            params.add(period);
        }
        if (type != null && !type.isEmpty() && !"전체".equals(type)) {
            sql.append("AND e.eval_type=? ");
            params.add(type);
        }
        if (searchTarget != null && !searchTarget.trim().isEmpty()) {
            sql.append("AND emp.emp_name LIKE ? ");
            params.add("%" + searchTarget.trim() + "%");
        }
        if (searchEvaluator != null && !searchEvaluator.trim().isEmpty()) {
            sql.append("AND evalr.emp_name LIKE ? ");
            params.add("%" + searchEvaluator.trim() + "%");
        }
        
        sql.append("ORDER BY e.eval_year DESC, e.created_at DESC");

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++)
                pstmt.setObject(i + 1, params.get(i));
                
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    String evalComment = rs.getString("eval_comment");
                    map.put("evalId", rs.getInt("eval_id"));
                    map.put("empId", rs.getInt("emp_id"));
                    map.put("empName", rs.getString("emp_name"));
                    map.put("deptName", rs.getString("dept_name"));
                    map.put("score", rs.getBigDecimal("total_score"));
                    map.put("grade", rs.getString("grade"));
                    map.put("status", rs.getString("eval_status"));
                    map.put("evaluatorId", rs.getInt("evaluator_id"));
                    map.put("evaluatorName", rs.getString("evaluator_name"));
                    map.put("confirmedAt", rs.getTimestamp("confirmed_at"));
                    map.put("evalComment", evalComment);
                    map.put("isRejected", evalComment != null && evalComment.startsWith("[반려]"));
                    map.put("evalYear", rs.getInt("eval_year"));
                    map.put("evalPeriod", rs.getString("eval_period"));
                    map.put("evalType", rs.getString("eval_type"));
                    list.add(map);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /** 기존 시그니처 호환용 (기본값은 가장 낮은 권한인 일반사원으로 설정) */
    public Vector<Map<String, Object>> getEvaluationStatusList(int year, String period, String type) {
        return getEvaluationStatusList(year, period, type, null, null, 0, "일반사원");
    }

    /**
     * 평가 통계 요약 집계
     */
    public Map<String, Integer> getEvaluationSummary(int year, String period, String type, String searchTarget,
            String searchEvaluator, int loginEmpId, String userRole) {
        
        Map<String, Integer> summary = new HashMap<>();
        summary.put("S", 0); summary.put("A", 0); summary.put("B", 0);
        summary.put("C", 0); summary.put("D", 0); summary.put("미완료", 0);

        // [수정] 프로젝트 공통 권한 명칭으로 변경
        boolean isPrivileged = "사장님".equals(userRole) || "HR담당자".equals(userRole) || "최종승인자".equals(userRole);

        // 1. 등급별 인원 집계
        StringBuilder sqlGrade = new StringBuilder(
                "SELECT grade, COUNT(*) AS cnt FROM evaluation WHERE eval_status='최종확정' AND eval_year=? ");
        List<Object> params = new ArrayList<>();
        params.add(year);

        if (!isPrivileged && loginEmpId > 0) {
            sqlGrade.append("AND (emp_id = ? OR evaluator_id = ?) ");
            params.add(loginEmpId);
            params.add(loginEmpId);
        }

        if (period != null && !"전체".equals(period) && !period.isEmpty()) {
            sqlGrade.append("AND eval_period=? ");
            params.add(period);
        }
        if (type != null && !"전체".equals(type) && !type.isEmpty()) {
            sqlGrade.append("AND eval_type=? ");
            params.add(type);
        }
        sqlGrade.append("GROUP BY grade");

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sqlGrade.toString())) {
            for (int i = 0; i < params.size(); i++)
                pstmt.setObject(i + 1, params.get(i));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String grade = rs.getString("grade");
                    if (grade != null && summary.containsKey(grade)) {
                        summary.put(grade, rs.getInt("cnt"));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 2. 미완료 계산
        if (!isPrivileged) {
            String sqlPending = "SELECT COUNT(*) FROM evaluation WHERE eval_status <> '최종확정' AND eval_year=? "
                              + "AND (emp_id = ? OR evaluator_id = ?)";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sqlPending)) {
                pstmt.setInt(1, year);
                pstmt.setInt(2, loginEmpId);
                pstmt.setInt(3, loginEmpId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) summary.put("미완료", rs.getInt(1));
                }
            } catch (SQLException e) { e.printStackTrace(); }
        } else {
            boolean isAllPeriod = (period == null || period.isEmpty() || "전체".equals(period));
            boolean isAllType = (type == null || type.isEmpty() || "전체".equals(type));

            if (isAllPeriod && isAllType) {
                String sqlPendingAll = "SELECT COUNT(DISTINCT emp_id) AS pending_cnt FROM evaluation "
                        + "WHERE eval_status <> '최종확정' AND eval_year=? ";
                try (Connection conn = DatabaseConnection.getConnection();
                        PreparedStatement pstmt = conn.prepareStatement(sqlPendingAll)) {
                    pstmt.setInt(1, year);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) summary.put("미완료", rs.getInt("pending_cnt"));
                    }
                } catch (SQLException e) { e.printStackTrace(); }
            } else {
                int totalTarget = 0;
                String sqlTargetCount = "SELECT COUNT(*) FROM employee e JOIN job_position p ON e.position_id = p.position_id "
                                      + "WHERE e.status='재직' AND p.position_level < 6";
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sqlTargetCount);
                     ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) totalTarget = rs.getInt(1);
                } catch (SQLException e) { e.printStackTrace(); }

                StringBuilder sqlConfirmedTarget = new StringBuilder("SELECT COUNT(DISTINCT emp_id) FROM evaluation WHERE eval_status='최종확정' AND eval_year=? ");
                if (!isAllPeriod) sqlConfirmedTarget.append("AND eval_period='").append(period).append("' ");
                if (!isAllType) sqlConfirmedTarget.append("AND eval_type='").append(type).append("' ");

                int confirmedCount = 0;
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sqlConfirmedTarget.toString())) {
                    pstmt.setInt(1, year);
                    try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) confirmedCount = rs.getInt(1); }
                } catch (SQLException e) { e.printStackTrace(); }
                summary.put("미완료", Math.max(0, totalTarget - confirmedCount));
            }
        }
        return summary;
    }

    /** 기존 시그니처 호환용 */
    public Map<String, Integer> getEvaluationSummary(int year, String period, String type) {
        return getEvaluationSummary(year, period, type, null, null, 0, "일반사원");
    }

	/** 불러오기: 본인 작성 + 최종확정 아닌 것 */
	public Map<String, Object> getEvaluationByCondition(int empId, int year, String period, String type,
			int evaluatorId) {
		String sql = "SELECT eval_id, eval_status, eval_comment FROM evaluation "
				+ "WHERE emp_id=? AND eval_year=? AND eval_period=? "
				+ "AND eval_type=? AND evaluator_id=? AND eval_status != '최종확정'";
		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setInt(1, empId);
			pstmt.setInt(2, year);
			pstmt.setString(3, period);
			pstmt.setString(4, type);
			pstmt.setInt(5, evaluatorId);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					Map<String, Object> map = new HashMap<>();
					map.put("evalId", rs.getInt("eval_id"));
					map.put("evalStatus", rs.getString("eval_status"));
					map.put("evalComment", rs.getString("eval_comment"));
					return map;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * [신규] 타인이 동일 조건으로 이미 작성 중인 평가가 있는지 확인 (Servlet 로직 이관)
	 */
	public boolean isOccupiedByOther(int empId, int year, String period, String type, int myEmpId) {
		String sql = "SELECT evaluator_id FROM evaluation "
				+ "WHERE emp_id=? AND eval_year=? AND eval_period=? AND eval_type=? " + "AND eval_status='작성중'";
		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setInt(1, empId);
			pstmt.setInt(2, year);
			pstmt.setString(3, period);
			pstmt.setString(4, type);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					// 작성 중인 평가자가 본인이 아니면 true(점유됨) 반환
					return rs.getInt("evaluator_id") != myEmpId;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * [수정] 최종확정 건 존재 여부 확인 (평가자 무관하게 이미 끝난 평가인지 체크)
	 */
	public boolean isAlreadyConfirmed(int empId, int year, String period, String type) {
		String sql = "SELECT COUNT(*) FROM evaluation "
				+ "WHERE emp_id=? AND eval_year=? AND eval_period=? AND eval_type=? " + "AND eval_status='최종확정'";
		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setInt(1, empId);
			pstmt.setInt(2, year);
			pstmt.setString(3, period);
			pstmt.setString(4, type);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next())
					return rs.getInt(1) > 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public int getPositionLevelByEmpId(int empId) {
		String sql = "SELECT p.position_level FROM employee e "
				+ "JOIN job_position p ON e.position_id = p.position_id WHERE e.emp_id=?";
		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setInt(1, empId);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next())
					return rs.getInt("position_level");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * 평가 유형별 대상자 목록 자기평가: 본인만 / 상위평가: 하위직급 / 동료평가: 동일직급
	 */
	/**
	 * 평가 유형별 대상자 목록 - 자기평가: 본인만 - 상위평가: position_level < 내 레벨 (하위직급) - 동료평가:
	 * position_level = 내 레벨 (동일직급, 본인 제외) - 하위평가: position_level > 내 레벨 (상위직급 — 사원이
	 * 부장 평가 등 역방향) - posLevel=0(직급 없음) 또는 최고직급자(하위가 없음): 상위평가 시 전체 재직자 반환
	 */
	/**
	 * [수정] 평가 유형별 대상자 목록 조회 (사장님 레벨 6 제외)
	 */
	public Vector<Map<String, Object>> getEmployeeListForEvaluator(int evaluatorId, int posLevel, String evalType) {
	    Vector<Map<String, Object>> list = new Vector<>();
	    String sql;
	    boolean needSecondParam = true;

	    // [수정] 공통 필터에 인사팀 상호 평가 방지 로직 추가
	    // e.dept_id != (내 부서 ID) 조건을 걸되, 내가 인사팀일 때만 이 조건이 작동하도록 서브쿼리 결합
	    String commonFilter = "WHERE e.status='재직' AND e.emp_id != ? AND p.position_level < 6 "
	            + "AND NOT (e.dept_id = (SELECT dept_id FROM employee WHERE emp_id = ?) "
	            + "         AND e.dept_id IN (SELECT dept_id FROM department WHERE dept_name LIKE '%인사%')) ";

	    if ("자기평가".equals(evalType)) {
	        sql = "SELECT e.emp_id, e.emp_name, COALESCE(p.position_name,'사원') AS pos "
	                + "FROM employee e LEFT JOIN job_position p ON e.position_id = p.position_id "
	                + "WHERE e.emp_id = ?";
	        needSecondParam = false;
	    } else if ("동료평가".equals(evalType)) {
	        sql = "SELECT e.emp_id, e.emp_name, COALESCE(p.position_name,'사원') AS pos "
	                + "FROM employee e LEFT JOIN job_position p ON e.position_id = p.position_id " + commonFilter
	                + "AND p.position_level = ? ORDER BY e.emp_name ASC";
	    } else if ("하위평가".equals(evalType)) {
	        sql = "SELECT e.emp_id, e.emp_name, COALESCE(p.position_name,'사원') AS pos "
	                + "FROM employee e LEFT JOIN job_position p ON e.position_id = p.position_id " + commonFilter
	                + "AND p.position_level > ? ORDER BY p.position_level ASC, e.emp_name ASC";
	    } else { // 상위평가 및 기타
	        sql = "SELECT e.emp_id, e.emp_name, COALESCE(p.position_name,'사원') AS pos "
	                + "FROM employee e LEFT JOIN job_position p ON e.position_id = p.position_id " + commonFilter
	                + (posLevel == 0 || posLevel >= 999 ? "" : "AND p.position_level < ? ")
	                + "ORDER BY p.position_level DESC, e.emp_name ASC";
	        if (posLevel == 0 || posLevel >= 999)
	            needSecondParam = false;
	    }

	    try (Connection conn = DatabaseConnection.getConnection();
	            PreparedStatement pstmt = conn.prepareStatement(sql)) {
	        
	        // 첫 번째 파라미터: 본인 제외 (e.emp_id != ?)
	        pstmt.setInt(1, evaluatorId);
	        
	        // [중요] 추가된 두 번째 파라미터 처리
	        if (!"자기평가".equals(evalType)) {
	            // commonFilter 내부의 (WHERE emp_id = ?) 에 대응
	            pstmt.setInt(2, evaluatorId);
	            if (needSecondParam) {
	                pstmt.setInt(3, posLevel);
	            }
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

	/**
	 * [수정] 일반 사원 목록 조회 (사장님 레벨 6 제외)
	 */
	public Vector<Map<String, Object>> getEmployeeList() {
		Vector<Map<String, Object>> list = new Vector<>();
		String sql = "SELECT e.emp_id, e.emp_name, COALESCE(p.position_name,'사원') AS pos "
				+ "FROM employee e JOIN job_position p ON e.position_id = p.position_id "
				+ "WHERE e.status='재직' AND p.position_level < 6 " // 사장님 제외
				+ "ORDER BY p.position_level DESC, e.emp_name ASC";
		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql);
				ResultSet rs = pstmt.executeQuery()) {
			while (rs.next()) {
				Map<String, Object> map = new HashMap<>();
				map.put("empId", rs.getInt("emp_id"));
				map.put("empName", rs.getString("emp_name"));
				map.put("pos", rs.getString("pos"));
				list.add(map);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return list;
	}

	/** 기존 시그니처 호환 */
	public Vector<Map<String, Object>> getEmployeeListForEvaluator(int evaluatorId, int posLevel) {
		return getEmployeeListForEvaluator(evaluatorId, posLevel, "상위평가");
	}

	public Vector<String> getEvaluationItemNames() {
		Vector<String> items = new Vector<>();
		items.add("업무성과");
		items.add("직무역량");
		items.add("조직기여도");
		items.add("리더십");
		return items;
	}

	private void closeResources(ResultSet rs, PreparedStatement pstmt, Connection conn) {
		try {
			if (rs != null)
				rs.close();
			if (pstmt != null)
				pstmt.close();
			if (conn != null)
				conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
