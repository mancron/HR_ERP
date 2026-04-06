package com.hrms.sal.dao;

import com.hrms.sal.dto.SalarySlipDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SalarySlipDAO {

    /** 특정 직원의 특정 연월 명세서 조회 */
    public SalarySlipDTO selectSlip(int empId, int year, int month,
                                    Connection conn) throws SQLException {
        String sql =
            "SELECT s.*, e.emp_name, e.emp_no, d.dept_name, p.position_name " +
            "FROM salary s " +
            "JOIN employee    e ON s.emp_id      = e.emp_id " +
            "JOIN department  d ON e.dept_id     = d.dept_id " +
            "JOIN job_position p ON e.position_id = p.position_id " +
            "WHERE s.emp_id = ? AND s.salary_year = ? AND s.salary_month = ?";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, empId);
            pstmt.setInt(2, year);
            pstmt.setInt(3, month);
            rs = pstmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
        return null;
    }

    /** 전체 직원 목록 조회 (관리자/HR담당자용 — 직원 선택 드롭다운) */
    public List<String[]> selectEmpList(Connection conn) throws SQLException {
        String sql =
            "SELECT e.emp_id, e.emp_name, d.dept_name " +
            "FROM employee e " +
            "JOIN department d ON e.dept_id = d.dept_id " +
            "WHERE e.status = '재직' " +
            "ORDER BY d.dept_name ASC, e.emp_name ASC";

        List<String[]> list = new ArrayList<>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new String[]{
                    String.valueOf(rs.getInt("emp_id")),
                    rs.getString("emp_name"),
                    rs.getString("dept_name")
                });
            }
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
        return list;
    }

    /** 해당 직원의 급여 데이터가 있는 연도/월 목록 조회 */
    public List<String[]> selectAvailableMonths(int empId, Connection conn)
            throws SQLException {
        String sql =
            "SELECT salary_year, salary_month " +
            "FROM salary WHERE emp_id = ? " +
            "ORDER BY salary_year DESC, salary_month DESC";

        List<String[]> list = new ArrayList<>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, empId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new String[]{
                    String.valueOf(rs.getInt("salary_year")),
                    String.valueOf(rs.getInt("salary_month"))
                });
            }
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
        return list;
    }

    // ── Private 유틸 ──
    private SalarySlipDTO mapRow(ResultSet rs) throws SQLException {
        SalarySlipDTO dto = new SalarySlipDTO();
        dto.setSalaryId(rs.getInt("salary_id"));
        dto.setEmpId(rs.getInt("emp_id"));
        dto.setSalaryYear(rs.getInt("salary_year"));
        dto.setSalaryMonth(rs.getInt("salary_month"));
        dto.setEmpName(rs.getString("emp_name"));
        dto.setEmpNo(rs.getString("emp_no"));
        dto.setDeptName(rs.getString("dept_name"));
        dto.setPositionName(rs.getString("position_name"));
        // 지급
        dto.setBaseSalary(rs.getInt("base_salary"));
        dto.setMealAllowance(rs.getInt("meal_allowance"));
        dto.setTransportAllowance(rs.getInt("transport_allowance"));
        dto.setPositionAllowance(rs.getInt("position_allowance"));
        dto.setOvertimePay(rs.getInt("overtime_pay"));
        dto.setOtherAllowance(rs.getInt("other_allowance"));
        dto.setGrossSalary(rs.getInt("gross_salary"));
        // 공제
        dto.setNationalPension(rs.getInt("national_pension"));
        dto.setHealthInsurance(rs.getInt("health_insurance"));
        dto.setLongTermCare(rs.getInt("long_term_care"));
        dto.setEmploymentInsurance(rs.getInt("employment_insurance"));
        dto.setUnpaidLeaveDays(rs.getBigDecimal("unpaid_leave_days"));
        dto.setUnpaidDeduction(rs.getInt("unpaid_deduction"));
        dto.setIncomeTax(rs.getInt("income_tax"));
        dto.setLocalIncomeTax(rs.getInt("local_income_tax"));
        dto.setTotalDeduction(rs.getInt("total_deduction"));
        dto.setNetSalary(rs.getInt("net_salary"));
        // 기타
        Date payDate = rs.getDate("pay_date");
        if (payDate != null) dto.setPayDate(payDate.toString());
        dto.setStatus(rs.getString("status"));
        return dto;
    }
}