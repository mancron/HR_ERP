package com.hrms.sal.dao;

import com.hrms.sal.dto.SalaryCalcDTO;
import com.hrms.sal.dto.DeductionRateDTO;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SalaryCalcDAO {

    /**
     * 재직자 전원 + 직급 수당 정보 조회 (계산용)
     */
    public List<SalaryCalcDTO> selectEmployeesForCalc(Connection conn) throws SQLException {
        String sql =
            "SELECT e.emp_id, e.emp_name, e.base_salary, " +
            "       d.dept_name, " +
            "       p.position_name, p.meal_allowance, " +
            "       p.transport_allowance, p.position_allowance " +
            "FROM employee e " +
            "JOIN department   d ON e.dept_id     = d.dept_id " +
            "JOIN job_position p ON e.position_id = p.position_id " +
            "WHERE e.status = '재직' " +
            "ORDER BY d.dept_name ASC, e.emp_name ASC";

        List<SalaryCalcDTO> list = new ArrayList<>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                SalaryCalcDTO dto = new SalaryCalcDTO();
                dto.setEmpId(rs.getInt("emp_id"));
                dto.setEmpName(rs.getString("emp_name"));
                dto.setBaseSalary(rs.getInt("base_salary"));
                dto.setDeptName(rs.getString("dept_name"));
                dto.setPositionName(rs.getString("position_name"));
                dto.setMealAllowance(rs.getInt("meal_allowance"));
                dto.setTransportAllowance(rs.getInt("transport_allowance"));
                dto.setPositionAllowance(rs.getInt("position_allowance"));
                list.add(dto);
            }
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
        return list;
    }

    /**
     * 해당 직원의 해당 월 승인된 초과근무 시간 합계 조회
     */
    public double selectOvertimeHours(int empId, int year, int month,
                                      Connection conn) throws SQLException {
        String sql =
            "SELECT COALESCE(SUM(ot_hours), 0) AS total_hours " +  // ✅ overtime_hours → ot_hours
            "FROM overtime_request " +
            "WHERE emp_id = ? " +
            "  AND YEAR(ot_date)  = ? " +                           // ✅ work_date → ot_date
            "  AND MONTH(ot_date) = ? " +                           // ✅ work_date → ot_date
            "  AND status = '승인'";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, empId);
            pstmt.setInt(2, year);
            pstmt.setInt(3, month);
            rs = pstmt.executeQuery();
            if (rs.next()) return rs.getDouble("total_hours");
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
        return 0.0;
    }

    /**
     * 해당 직원의 해당 월 무급 공제 일수 조회
     * 무급 대상: leave_request(병가 승인) + attendance(결근)
     */
    public BigDecimal selectUnpaidLeaveDays(int empId, int year, int month,
                                            Connection conn) throws SQLException {
        // 병가(승인) 일수 — 컬럼명: days
        String leaveSql =
            "SELECT COALESCE(SUM(days), 0) AS sick_days " +         // ✅ used_days → days
            "FROM leave_request " +
            "WHERE emp_id     = ? " +
            "  AND leave_type = '병가' " +
            "  AND status     = '승인' " +
            "  AND YEAR(start_date)  = ? " +
            "  AND MONTH(start_date) = ?";

        // 결근 일수 — attendance.status IN ('출근','지각','결근','휴가','출장')
        String absentSql =
            "SELECT COUNT(*) AS absent_days " +
            "FROM attendance " +
            "WHERE emp_id = ? " +
            "  AND YEAR(work_date)  = ? " +
            "  AND MONTH(work_date) = ? " +
            "  AND status = '결근'";

        double sickDays   = 0.0;
        double absentDays = 0.0;

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = conn.prepareStatement(leaveSql);
            pstmt.setInt(1, empId);
            pstmt.setInt(2, year);
            pstmt.setInt(3, month);
            rs = pstmt.executeQuery();
            if (rs.next()) sickDays = rs.getDouble("sick_days");
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }

        try {
            pstmt = conn.prepareStatement(absentSql);
            pstmt.setInt(1, empId);
            pstmt.setInt(2, year);
            pstmt.setInt(3, month);
            rs = pstmt.executeQuery();
            if (rs.next()) absentDays = rs.getDouble("absent_days");
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }

        return BigDecimal.valueOf(sickDays + absentDays);
    }

    /**
     * 해당 연도 공제율 조회
     */
    public DeductionRateDTO selectDeductionRate(int year, Connection conn) throws SQLException {
        String sql =
            "SELECT national_pension_rate, health_insurance_rate, " +
            "       long_term_care_rate, employment_insurance_rate " +
            "FROM deduction_rate WHERE target_year = ?";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, year);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                DeductionRateDTO dto = new DeductionRateDTO();
                dto.setNationalPensionRate(rs.getBigDecimal("national_pension_rate"));
                dto.setHealthInsuranceRate(rs.getBigDecimal("health_insurance_rate"));
                dto.setLongTermCareRate(rs.getBigDecimal("long_term_care_rate"));
                dto.setEmploymentInsuranceRate(rs.getBigDecimal("employment_insurance_rate"));
                return dto;
            }
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
        return null;
    }

    /**
     * 급여 INSERT (계산 결과 저장)
     */
    public void insertSalary(SalaryCalcDTO dto, int year, int month,
                             Connection conn) throws SQLException {
        String sql =
            "INSERT INTO salary " +
            "(emp_id, salary_year, salary_month, " +
            " base_salary, meal_allowance, transport_allowance, position_allowance, " +
            " overtime_pay, other_allowance, gross_salary, " +
            " national_pension, health_insurance, long_term_care, employment_insurance, " +
            " unpaid_leave_days, unpaid_deduction, " +
            " income_tax, local_income_tax, total_deduction, net_salary, status) " +
            "VALUES (?,?,?, ?,?,?,?, ?,?,?, ?,?,?,?, ?,?, ?,?,?,?, '대기')";

        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, dto.getEmpId());
            pstmt.setInt(2, year);
            pstmt.setInt(3, month);
            pstmt.setInt(4, dto.getBaseSalary());
            pstmt.setInt(5, dto.getMealAllowance());
            pstmt.setInt(6, dto.getTransportAllowance());
            pstmt.setInt(7, dto.getPositionAllowance());
            pstmt.setInt(8, dto.getOvertimePay());
            pstmt.setInt(9, dto.getOtherAllowance());
            pstmt.setInt(10, dto.getGrossSalary());
            pstmt.setInt(11, dto.getNationalPension());
            pstmt.setInt(12, dto.getHealthInsurance());
            pstmt.setInt(13, dto.getLongTermCare());
            pstmt.setInt(14, dto.getEmploymentInsurance());
            pstmt.setBigDecimal(15, dto.getUnpaidLeaveDays());
            pstmt.setInt(16, dto.getUnpaidDeduction());
            pstmt.setInt(17, dto.getIncomeTax());
            pstmt.setInt(18, dto.getLocalIncomeTax());
            pstmt.setInt(19, dto.getTotalDeduction());
            pstmt.setInt(20, dto.getNetSalary());
            pstmt.executeUpdate();
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
    }

    /**
     * 재계산: 해당 월 대기 상태 급여만 삭제
     * status='완료'는 절대 삭제하지 않음
     */
    public int deleteUnpaidSalaries(int year, int month, Connection conn) throws SQLException {
        String sql =
            "DELETE FROM salary " +
            "WHERE salary_year = ? AND salary_month = ? AND status = '대기'";

        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, year);
            pstmt.setInt(2, month);
            return pstmt.executeUpdate();
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
    }

    /**
     * 해당 월 급여 목록 조회 (화면 출력용)
     */
    public List<SalaryCalcDTO> selectSalaryList(int year, int month,
                                                Connection conn) throws SQLException {
        String sql =
            "SELECT s.salary_id, s.emp_id, e.emp_name, d.dept_name, p.position_name, " +
            "       s.base_salary, s.gross_salary, s.total_deduction, s.net_salary, " +
            "       DATE_FORMAT(s.pay_date, '%Y-%m-%d') AS pay_date, s.status " +
            "FROM salary s " +
            "JOIN employee    e ON s.emp_id      = e.emp_id " +
            "JOIN department  d ON e.dept_id     = d.dept_id " +
            "JOIN job_position p ON e.position_id = p.position_id " +
            "WHERE s.salary_year = ? AND s.salary_month = ? " +
            "ORDER BY d.dept_name ASC, e.emp_name ASC";

        List<SalaryCalcDTO> list = new ArrayList<>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, year);
            pstmt.setInt(2, month);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                SalaryCalcDTO dto = new SalaryCalcDTO();
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
                list.add(dto);
            }
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
        return list;
    }

    /**
     * 개별 지급 처리: status='완료' + pay_date=오늘
     */
    public int updatePayOne(int salaryId, Connection conn) throws SQLException {
        String sql =
            "UPDATE salary SET status = '완료', pay_date = CURDATE() " +
            "WHERE salary_id = ? AND status = '대기'";

        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, salaryId);
            return pstmt.executeUpdate();
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
    }

    /**
     * 전체 지급 처리: 해당 월 대기 전부 완료
     */
    public int updatePayAll(int year, int month, Connection conn) throws SQLException {
        String sql =
            "UPDATE salary SET status = '완료', pay_date = CURDATE() " +
            "WHERE salary_year = ? AND salary_month = ? AND status = '대기'";

        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, year);
            pstmt.setInt(2, month);
            return pstmt.executeUpdate();
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
    }

    /**
     * audit_log INSERT (지급 처리용)
     */
    public void insertAuditLog(int actorEmpId, int salaryId,
                               String oldVal, String newVal,
                               Connection conn) throws SQLException {
        String sql =
            "INSERT INTO audit_log " +
            "(actor_id, target_table, target_id, action, column_name, old_value, new_value) " +
            "VALUES (?, 'salary', ?, 'UPDATE', 'status', ?, ?)";

        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, actorEmpId);
            pstmt.setInt(2, salaryId);
            pstmt.setString(3, oldVal);
            pstmt.setString(4, newVal);
            pstmt.executeUpdate();
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
    }

    /**
     * 해당 월 완료 건 salary_id 목록 (전체지급 audit_log용)
     */
    public List<Integer> selectSalaryIdsByMonth(int year, int month,
                                                Connection conn) throws SQLException {
        String sql =
            "SELECT salary_id FROM salary " +
            "WHERE salary_year = ? AND salary_month = ? AND status = '완료'";

        List<Integer> ids = new ArrayList<>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, year);
            pstmt.setInt(2, month);
            rs = pstmt.executeQuery();
            while (rs.next()) ids.add(rs.getInt("salary_id"));
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
        return ids;
    }

    
    /** salaryId로 알림 발송에 필요한 정보 조회 */
    public SalaryCalcDTO selectSalaryById(int salaryId, Connection conn) throws SQLException {
        String sql =
            "SELECT emp_id, salary_year, salary_month, net_salary " +
            "FROM salary WHERE salary_id = ?";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, salaryId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                SalaryCalcDTO dto = new SalaryCalcDTO();
                dto.setSalaryId(salaryId);
                dto.setEmpId(rs.getInt("emp_id"));
                dto.setSalaryYear(rs.getInt("salary_year"));
                dto.setSalaryMonth(rs.getInt("salary_month"));
                dto.setNetSalary(rs.getInt("net_salary"));
                return dto;
            }
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
        return null;
    }
    
    
    /** 해당 직원의 해당 월 급여가 이미 존재하는지 확인 */
    public boolean existsSalary(int empId, int year, int month,
                                Connection conn) throws SQLException {
        String sql =
            "SELECT 1 FROM salary " +
            "WHERE emp_id = ? AND salary_year = ? AND salary_month = ?";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, empId);
            pstmt.setInt(2, year);
            pstmt.setInt(3, month);
            rs = pstmt.executeQuery();
            return rs.next();
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
    }
    
    
    /**
     * 해당 연월 근태 마감 여부 조회
     * attendance_close 테이블에 is_closed=true 인 행이 있으면 마감
     */
    public boolean isAttendanceClosed(int year, int month, Connection conn) throws SQLException {
        String sql =
            "SELECT is_closed FROM attendance_close " +
            "WHERE year = ? AND month = ? AND is_closed = TRUE";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, year);
            pstmt.setInt(2, month);
            rs = pstmt.executeQuery();
            return rs.next(); // 행이 있으면 마감됨
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
    }
    
    
    /**
     * 간이세액표 조회 (독신 = fam_1 고정)
     * 10,000천원 이하: DB 테이블 조회
     * 10,000천원 초과: 표 하단 공식 적용
     */
    public int selectIncomeTax(int grossSalary, int year, Connection conn) throws SQLException {

        // ── 10,000천원 이하: 테이블 조회 ──
        if (grossSalary <= 10_000_000) {
            String sql =
                "SELECT fam_1 FROM income_tax_table " +
                "WHERE apply_year = ? " +
                "  AND salary_from <= ? " +
                "  AND salary_to   >  ? " +
                "ORDER BY salary_from DESC LIMIT 1";

            PreparedStatement pstmt = null;
            ResultSet rs = null;
            try {
                pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, year);
                pstmt.setInt(2, grossSalary);
                pstmt.setInt(3, grossSalary);
                rs = pstmt.executeQuery();
                if (rs.next()) return rs.getInt("fam_1");
            } finally {
                if (rs    != null) try { rs.close();    } catch (SQLException e) {}
                if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
            }
            return 0;
        }

        // ── 10,000천원 초과: 표 하단 공식 적용 ──
        // 기준값 조회 (10,000천원일 때 fam_1 세액)
        int base = 1_507_400; // 기본값
        String baseSql =
            "SELECT fam_1 FROM income_tax_table " +
            "WHERE apply_year = ? AND salary_from = 10000000";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(baseSql);
            pstmt.setInt(1, year);
            rs = pstmt.executeQuery();
            if (rs.next()) base = rs.getInt("fam_1");
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }

        // 구간별 공식 (2026 간이세액표 하단 기준)
        if (grossSalary <= 14_000_000) {
            return base + (int)((grossSalary - 10_000_000) * 0.98 * 0.35) + 25_000;

        } else if (grossSalary <= 28_000_000) {
            return base + 1_397_000
                 + (int)((grossSalary - 14_000_000) * 0.98 * 0.38);

        } else if (grossSalary <= 30_000_000) {
            return base + 6_610_600
                 + (int)((grossSalary - 28_000_000) * 0.98 * 0.40);

        } else if (grossSalary <= 45_000_000) {
            return base + 7_394_600
                 + (int)((grossSalary - 30_000_000) * 0.40);

        } else if (grossSalary <= 87_000_000) {
            return base + 13_394_600
                 + (int)((grossSalary - 45_000_000) * 0.42);

        } else {
            return base + 31_034_600
                 + (int)((grossSalary - 87_000_000) * 0.45);
        }
    }
}