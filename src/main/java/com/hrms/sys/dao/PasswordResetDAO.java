package com.hrms.sys.dao;

import com.hrms.sys.dto.PasswordResetDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PasswordResetDAO {

    /**
     * 키워드로 직원 검색 (이름 / 사번 / 부서명) — 재직 중인 직원만
     */
    public List<PasswordResetDTO> searchEmployees(String keyword, Connection conn) throws SQLException {
        List<PasswordResetDTO> list = new ArrayList<>();

        String sql =
            "SELECT e.emp_id, e.emp_name, e.emp_no, d.dept_name, j.position_name " +
            "FROM employee e " +
            "JOIN department  d ON e.dept_id     = d.dept_id " +
            "JOIN job_position j ON e.position_id = j.position_id " +
            "WHERE e.status IN ('재직','휴직') " +
            "  AND (e.emp_name LIKE ? OR e.emp_no LIKE ? OR d.dept_name LIKE ?) " +
            "ORDER BY e.emp_no ASC";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            String like = "%" + keyword + "%";
            pstmt.setString(1, like);
            pstmt.setString(2, like);
            pstmt.setString(3, like);

            rs = pstmt.executeQuery();
            while (rs.next()) {
                PasswordResetDTO dto = new PasswordResetDTO();
                dto.setEmpId(rs.getInt("emp_id"));
                dto.setEmpName(rs.getString("emp_name"));
                dto.setEmpNo(rs.getString("emp_no"));
                dto.setDeptName(rs.getString("dept_name"));
                dto.setPosName(rs.getString("position_name"));
                list.add(dto);
            }
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
        return list;
    }

    /**
     * emp_id로 단일 직원 조회
     */
    public PasswordResetDTO findEmployeeById(int empId, Connection conn) throws SQLException {
        String sql =
            "SELECT e.emp_id, e.emp_name, e.emp_no, d.dept_name, j.position_name " +
            "FROM employee e " +
            "JOIN department  d ON e.dept_id     = d.dept_id " +
            "JOIN job_position j ON e.position_id = j.position_id " +
            "WHERE e.emp_id = ? AND e.status = '재직'";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, empId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                PasswordResetDTO dto = new PasswordResetDTO();
                dto.setEmpId(rs.getInt("emp_id"));
                dto.setEmpName(rs.getString("emp_name"));
                dto.setEmpNo(rs.getString("emp_no"));
                dto.setDeptName(rs.getString("dept_name"));
                dto.setPosName(rs.getString("position_name"));
                return dto;
            }
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
        return null;
    }

    /**
     * account 테이블의 account_id 조회 (audit_log의 target_id용)
     */
    public int findAccountIdByEmpId(int empId, Connection conn) throws SQLException {
        String sql = "SELECT account_id FROM account WHERE emp_id = ?";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, empId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("account_id");
            }
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
        throw new SQLException("emp_id=" + empId + " 에 해당하는 계정을 찾을 수 없습니다.");
    }

    /**
     * 비밀번호 해시 업데이트 + 로그인 실패 횟수 초기화 + 변경 일시 갱신
     */
    public int updatePassword(int empId, String newHash, Connection conn) throws SQLException {
        String sql =
            "UPDATE account " +
            "SET password_hash = ?, password_changed_at = NOW(), login_attempts = 0 " +
            "WHERE emp_id = ?";

        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, newHash);
            pstmt.setInt(2, empId);
            return pstmt.executeUpdate();
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
    }

    /**
     * audit_log INSERT (민감 정보는 호출 전 이미 마스킹 처리된 값이 들어옴)
     */
    public void insertAuditLog(Integer actorId, String targetTable, int targetId,
                               String action, String columnName,
                               String oldValue, String newValue,
                               Connection conn) throws SQLException {
        String sql =
            "INSERT INTO audit_log " +
            "(actor_id, target_table, target_id, action, column_name, old_value, new_value) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sql);
            // actor_id가 null이면 시스템 처리로 기록
            if (actorId != null) {
                pstmt.setInt(1, actorId);
            } else {
                pstmt.setNull(1, java.sql.Types.INTEGER);
            }
            pstmt.setString(2, targetTable);
            pstmt.setInt(3, targetId);
            pstmt.setString(4, action);
            pstmt.setString(5, columnName);
            pstmt.setString(6, oldValue);
            pstmt.setString(7, newValue);
            pstmt.executeUpdate();
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
    }
}