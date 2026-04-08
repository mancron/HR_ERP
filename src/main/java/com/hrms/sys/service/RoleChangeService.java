package com.hrms.sys.service;

import com.hrms.sys.dao.RoleChangeDAO;
import com.hrms.sys.dto.RoleChangeDTO;
import com.hrms.common.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class RoleChangeService {

    private final RoleChangeDAO roleChangeDAO = new RoleChangeDAO();

    // 허용 권한 목록
    /**
     * 전체 계정 목록 조회
     */
    public List<RoleChangeDTO> getAllAccounts() {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            return roleChangeDAO.selectAllAccounts(conn);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("계정 목록 조회 중 오류가 발생했습니다.", e);
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }
    
    /** DB에서 유효 권한 목록 조회 */
//    public List<String> getValidRoles() {
//        Connection conn = null;
//        try {
//            conn = DatabaseConnection.getConnection();
//            return roleChangeDAO.selectDistinctRoles(conn);
//        } catch (SQLException e) {
//            e.printStackTrace();
//            throw new RuntimeException("권한 목록 조회 중 오류가 발생했습니다.", e);
//        } finally {
//            if (conn != null) try { conn.close(); } catch (SQLException e) {}
//        }
//    }
    
    private static final List<String> VALID_ROLES = 
    	    List.of("관리자", "일반", "HR담당자", "최종승인자");

    	public List<String> getValidRoles() {
    	    return VALID_ROLES; // DB 상태와 무관하게 항상 4개 반환
    	}

    /**
     * 권한 변경 + audit_log 기록 (트랜잭션)
     *
     * @param accountId  변경 대상 account_id
     * @param targetEmpId 변경 대상 emp_id (자기 자신 차단용)
     * @param actorEmpId 작업자(관리자) emp_id
     * @param oldRole    변경 전 권한
     * @param newRole    변경 후 권한
     */
    public void changeRole(int accountId, int targetEmpId,
                           Integer actorEmpId, String oldRole, String newRole) {

        // 1. 자기 자신 권한 변경 차단 (백엔드 방어)
        if (actorEmpId != null && actorEmpId == targetEmpId) {
            throw new RuntimeException("자기 자신의 권한은 변경할 수 없습니다.");
        }

        // ── 수정: DB에서 읽어온 목록으로 검증 ──
        List<String> validRoles = getValidRoles();
        if (!validRoles.contains(newRole)) {
            throw new RuntimeException("유효하지 않은 권한값입니다: " + newRole);
        }

        if (oldRole.equals(newRole)) {
            throw new RuntimeException("현재 권한과 동일합니다. 변경할 권한을 선택해주세요.");
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // ── 트랜잭션 시작

            // 4. role UPDATE
            int updated = roleChangeDAO.updateRole(accountId, newRole, conn);
            if (updated == 0) {
                throw new RuntimeException("변경할 계정을 찾을 수 없습니다. (account_id=" + accountId + ")");
            }

            // 5. audit_log INSERT
            roleChangeDAO.insertAuditLog(actorEmpId, accountId, oldRole, newRole, conn);

            conn.commit(); // ── 커밋

        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException re) { re.printStackTrace(); }
            e.printStackTrace();
            throw new RuntimeException("권한 변경 중 데이터베이스 오류가 발생했습니다.", e);
        } catch (RuntimeException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException re) { re.printStackTrace(); }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }
}