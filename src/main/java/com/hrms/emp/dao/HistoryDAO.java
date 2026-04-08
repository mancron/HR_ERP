package com.hrms.emp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.hrms.emp.dto.HistoryDTO;

public class HistoryDAO {

    // 공통 JOIN 절 — 중복 제거
    private static final String BASE_SELECT =
        "SELECT h.*, " +
        "e.emp_name, e.emp_no, " +
        "fd.dept_name AS from_dept_name, " +
        "td.dept_name AS to_dept_name, " +
        "fp.position_name AS from_position_name, " +
        "tp.position_name AS to_position_name, " +
        "a.emp_name AS approved_by_name " +
        "FROM personnel_history h " +
        "JOIN employee e            ON h.emp_id           = e.emp_id " +
        "LEFT JOIN department fd    ON h.from_dept_id     = fd.dept_id " +
        "LEFT JOIN department td    ON h.to_dept_id       = td.dept_id " +
        "LEFT JOIN job_position fp  ON h.from_position_id = fp.position_id " +
        "LEFT JOIN job_position tp  ON h.to_position_id   = tp.position_id " +
        "LEFT JOIN employee a       ON h.approved_by      = a.emp_id ";

    /** 특정 직원의 인사발령 이력 조회 */
    public List<HistoryDTO> getHistoryByEmpId(Connection con, int empId) throws SQLException {
        String sql = BASE_SELECT +
            "WHERE h.emp_id = ? " +
            "ORDER BY h.change_date DESC";
        return executeQuery(con, sql, empId);
    }

    /** 전사 전체 이력 조회 + 검색 필터 (HR담당자·CEO) */
    public List<HistoryDTO> getHistoryList(Connection con, String keyword, String changeType, String yearMonth)
            throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append("WHERE 1=1 ");
        appendFilters(sql, keyword, changeType, yearMonth);
        sql.append("ORDER BY h.change_date DESC");
        return executeQuery(con, sql.toString(), buildParams(null, keyword, changeType, yearMonth));
    }

    /** 팀원 + 본인 이력 조회 (부서장) — 본인 dept_id 소속 직원 전체 */
    public List<HistoryDTO> getHistoryListByDept(Connection con, int managerEmpId,
            String keyword, String changeType, String yearMonth) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT)
            .append("WHERE e.dept_id = (SELECT dept_id FROM employee WHERE emp_id = ?) ");
        appendFilters(sql, keyword, changeType, yearMonth);
        sql.append("ORDER BY h.change_date DESC");
        return executeQuery(con, sql.toString(), buildParams(managerEmpId, keyword, changeType, yearMonth));
    }

    /** 본인 이력만 조회 (일반직원·관리자) */
    public List<HistoryDTO> getHistoryListByEmp(Connection con, int empId,
            String keyword, String changeType, String yearMonth) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT)
            .append("WHERE h.emp_id = ? ");
        appendFilters(sql, keyword, changeType, yearMonth);
        sql.append("ORDER BY h.change_date DESC");
        return executeQuery(con, sql.toString(), buildParams(empId, keyword, changeType, yearMonth));
    }

    // ─── private 유틸 ────────────────────────────────────────────────

    /** 검색 필터 조건 추가 */
    private void appendFilters(StringBuilder sql, String keyword, String changeType, String yearMonth) {
        if (keyword    != null && !keyword.isEmpty())    sql.append("AND e.emp_name LIKE ? ");
        if (changeType != null && !changeType.isEmpty()) sql.append("AND h.change_type = ? ");
        if (yearMonth  != null && !yearMonth.isEmpty())  sql.append("AND DATE_FORMAT(h.change_date, '%Y-%m') = ? ");
    }

    /** 파라미터 배열 구성 — firstParam이 null이면 첫 번째 ? 없음 */
    private Object[] buildParams(Integer firstParam, String keyword, String changeType, String yearMonth) {
        List<Object> params = new ArrayList<>();
        if (firstParam != null) params.add(firstParam);
        if (keyword    != null && !keyword.isEmpty())    params.add("%" + keyword + "%");
        if (changeType != null && !changeType.isEmpty()) params.add(changeType);
        if (yearMonth  != null && !yearMonth.isEmpty())  params.add(yearMonth);
        return params.toArray();
    }

    /** PreparedStatement 실행 공통 메서드 */
    private List<HistoryDTO> executeQuery(Connection con, String sql, Object... params) throws SQLException {
        List<HistoryDTO> list = new ArrayList<>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) pstmt.setObject(i + 1, params[i]);
            rs = pstmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } finally {
            if (rs    != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
        return list;
    }

    /** ResultSet → HistoryDTO 매핑 */
    private HistoryDTO mapRow(ResultSet rs) throws SQLException {
        HistoryDTO dto = new HistoryDTO();
        dto.setHistory_id(rs.getInt("history_id"));
        dto.setEmp_id(rs.getInt("emp_id"));
        dto.setChange_type(rs.getString("change_type"));
        dto.setFrom_dept_id(rs.getInt("from_dept_id"));
        dto.setTo_dept_id(rs.getInt("to_dept_id"));
        dto.setFrom_position_id(rs.getInt("from_position_id"));
        dto.setTo_position_id(rs.getInt("to_position_id"));
        dto.setFrom_role(rs.getString("from_role"));
        dto.setTo_role(rs.getString("to_role"));
        dto.setChange_date(rs.getTimestamp("change_date") != null
            ? rs.getTimestamp("change_date").toLocalDateTime() : null);
        dto.setReason(rs.getString("reason"));
        dto.setApproved_by(rs.getInt("approved_by"));
        dto.setCreated_at(rs.getTimestamp("created_at") != null
            ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        dto.setEmp_name(rs.getString("emp_name"));
        dto.setEmp_no(rs.getString("emp_no"));
        dto.setFrom_dept_name(rs.getString("from_dept_name"));
        dto.setTo_dept_name(rs.getString("to_dept_name"));
        dto.setFrom_position_name(rs.getString("from_position_name"));
        dto.setTo_position_name(rs.getString("to_position_name"));
        dto.setApproved_by_name(rs.getString("approved_by_name"));
        return dto;
    }
}