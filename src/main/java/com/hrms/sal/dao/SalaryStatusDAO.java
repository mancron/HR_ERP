package com.hrms.sal.dao;

import com.hrms.sal.dto.SalaryStatusDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SalaryStatusDAO {

    /**
     * 연도/월 + 부서 조건으로 급여 목록 조회
     * deptId = 0 이면 전체 조회
     */
    public List<SalaryStatusDTO> selectByYearMonth(int year, int month,
                                                    int deptId, Connection conn)
            throws SQLException {

        StringBuilder sql = new StringBuilder(
            "SELECT s.salary_id, s.emp_id, e.emp_name, d.dept_name, " +
            "       p.position_name, s.base_salary, s.gross_salary, " +
            "       s.total_deduction, s.net_salary, " +
            "       DATE_FORMAT(s.pay_date, '%Y-%m-%d') AS pay_date, " +
            "       s.status, s.salary_year, s.salary_month " +
            "FROM salary s " +
            "JOIN employee    e ON s.emp_id      = e.emp_id " +
            "JOIN department  d ON e.dept_id     = d.dept_id " +
            "JOIN job_position p ON e.position_id = p.position_id " +
            "WHERE s.salary_year = ? AND s.salary_month = ? " +
            "AND e.status = '재직' "
        );

        if (deptId > 0) {
            sql.append("AND e.dept_id = ? ");
        }
        sql.append("ORDER BY d.dept_name ASC, e.emp_name ASC");

        List<SalaryStatusDTO> list = new ArrayList<>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql.toString());
            pstmt.setInt(1, year);
            pstmt.setInt(2, month);
            if (deptId > 0) pstmt.setInt(3, deptId);

            rs = pstmt.executeQuery();
            while (rs.next()) {
                SalaryStatusDTO dto = new SalaryStatusDTO();
                dto.setSalaryId(rs.getInt("salary_id"));
                dto.setEmpId(rs.getInt("emp_id"));
                dto.setEmpName(rs.getString("emp_name"));
                dto.setDeptName(rs.getString("dept_name"));
                dto.setPositionName(rs.getString("position_name"));
                dto.setBaseSalary(rs.getInt("base_salary"));
                dto.setGrossSalary(rs.getInt("gross_salary"));
                dto.setTotalDeduction(rs.getInt("total_deduction"));
                dto.setNetSalary(rs.getInt("net_salary"));
                dto.setPayDate(rs.getString("pay_date"));
                dto.setStatus(rs.getString("status"));
                dto.setSalaryYear(rs.getInt("salary_year"));
                dto.setSalaryMonth(rs.getInt("salary_month"));
                list.add(dto);
            }
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
        return list;
    }

    /** 부서 목록 조회 (필터용) */
    public List<int[]> selectDeptList(Connection conn) throws SQLException {
        String sql =
            "SELECT dept_id, dept_name FROM department " +
            "WHERE is_active = 1 AND dept_level >= 2 " +
            "ORDER BY sort_order ASC";

        List<int[]> list = new ArrayList<>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new int[]{rs.getInt("dept_id"), 0});
            }
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
        return list;
    }

    /** 부서 목록 조회 (이름 포함) */
    public List<String[]> selectDeptListWithName(Connection conn) throws SQLException {
        String sql =
            "SELECT dept_id, dept_name FROM department " +
            "WHERE is_active = 1 AND dept_level >= 2 " +
            "ORDER BY sort_order ASC";

        List<String[]> list = new ArrayList<>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new String[]{
                    String.valueOf(rs.getInt("dept_id")),
                    rs.getString("dept_name")
                });
            }
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
        return list;
    }
}