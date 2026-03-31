package com.hrms.sys.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;

import org.mindrot.jbcrypt.BCrypt;

import com.hrms.common.db.DatabaseConnection;
import com.hrms.sys.dao.PasswordResetDAO;
import com.hrms.sys.dto.PasswordResetDTO;

public class PasswordResetService {

    private final PasswordResetDAO passwordResetDAO = new PasswordResetDAO();

    // ──────────────────────────────────────
    // 조회용 (트랜잭션 불필요 — 단순 SELECT)
    // ──────────────────────────────────────

    public List<PasswordResetDTO> searchEmployees(String keyword) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            return passwordResetDAO.searchEmployees(keyword, conn);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("직원 검색 중 오류가 발생했습니다.", e);
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }

    public PasswordResetDTO findEmployeeById(int empId) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            return passwordResetDAO.findEmployeeById(empId, conn);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("직원 조회 중 오류가 발생했습니다.", e);
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }

    // ──────────────────────────────────────
    // 비밀번호 초기화 (트랜잭션 필수)
    // ──────────────────────────────────────

    /**
     * @param empId      초기화 대상 직원 emp_id
     * @param actorEmpId 작업자(관리자) emp_id — audit_log actor_id
     * @return 화면에 1회 표시할 평문 임시 비밀번호
     */
    public String resetPassword(int empId, Integer actorEmpId) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // ── 트랜잭션 시작

            // 1. 대상 직원 존재 여부 확인
            PasswordResetDTO target = passwordResetDAO.findEmployeeById(empId, conn);
            if (target == null) {
                throw new RuntimeException("초기화 대상 직원을 찾을 수 없습니다. (emp_id=" + empId + ")");
            }

            // 2. 임시 비밀번호 생성: 사번 + 랜덤 4자리
            String tempPassword = generateTempPassword(target.getEmpNo());

            // 3. SHA-256 해시
            String hashedPassword = BCrypt.hashpw(tempPassword, BCrypt.gensalt());

            // 4. account 테이블 UPDATE
            int updated = passwordResetDAO.updatePassword(empId, hashedPassword, conn);
            if (updated == 0) {
                throw new RuntimeException("비밀번호 업데이트 실패 — 해당 직원의 계정이 없습니다.");
            }

            // 5. audit_log INSERT
            int accountId = passwordResetDAO.findAccountIdByEmpId(empId, conn);
            passwordResetDAO.insertAuditLog(
                actorEmpId,
                "account",
                accountId,
                "UPDATE",
                "password_hash",
                "이전 비밀번호",          // old_value — 해시값 저장 불필요
                "임시 비밀번호로 초기화",   // new_value — 작업 내용 명시
                conn
            );

            conn.commit(); // ── 커밋

            // 6. 평문 임시 비밀번호 반환 (DB에는 절대 저장 안 됨)
            return tempPassword;

        } catch (SQLException e) {
            // 트랜잭션 롤백
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException re) { re.printStackTrace(); }
            }
            e.printStackTrace();
            throw new RuntimeException("비밀번호 초기화 중 데이터베이스 오류가 발생했습니다.", e);
        } catch (RuntimeException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException re) { re.printStackTrace(); }
            }
            throw e;
        } finally {
            // 자원 역순 해제
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // autoCommit 복구
                    conn.close();
                } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    // ──────────────────────────────────────
    // Private 유틸 메서드
    // ──────────────────────────────────────

    /** 사번 + 랜덤 4자리 숫자 조합 */
    private String generateTempPassword(String empNo) {
        int random4 = new Random().nextInt(9000) + 1000; // 1000~9999
        return empNo + random4;
    }

    /** SHA-256 해시 변환 */
    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}