package com.hrms.emp.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.hrms.emp.dto.EmpDTO;

public class RegDAO {

    // ── 사번 채번 (트랜잭션 내 FOR UPDATE로 중복 방지) ──
    public String generateEmpNo(Connection con) throws SQLException {
        String sql = "SELECT MAX(CAST(SUBSTRING(emp_no, 4) AS UNSIGNED)) AS max_no " +
                     "FROM employee FOR UPDATE";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            int maxNo = 0;
            if (rs.next() && rs.getObject("max_no") != null) {
                maxNo = rs.getInt("max_no");
            }
            return String.format("EMP%03d", maxNo + 1);
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }

    // ── username 중복 확인 ──
    public boolean isUsernameExist(Connection con, String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM account WHERE username = ?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }

    // ── 직원 INSERT ──
    public int insertEmployee(Connection con, EmpDTO dto) throws SQLException {
        String sql = "INSERT INTO employee " +
                     "(emp_name, emp_no, dept_id, position_id, hire_date, " +
                     "emp_type, status, base_salary, birth_date, gender, " +
                     "address, emergency_contact, bank_account, email, phone) " +
                     "VALUES (?, ?, ?, ?, ?, ?, '재직', ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, dto.getEmp_name());
            pstmt.setString(2, dto.getEmp_no());
            pstmt.setInt(3, dto.getDept_id());
            pstmt.setInt(4, dto.getPosition_id());
            pstmt.setString(5, dto.getHire_date());
            pstmt.setString(6, dto.getEmp_type());
            pstmt.setInt(7, dto.getBase_salary());
            pstmt.setString(8, dto.getBirth_date());
            pstmt.setString(9, dto.getGender());
            pstmt.setString(10, dto.getAddress());
            pstmt.setString(11, dto.getEmergency_contact());
            pstmt.setString(12, dto.getBank_account());
            pstmt.setString(13, dto.getEmail());
            pstmt.setString(14, dto.getPhone());
            pstmt.executeUpdate();

            // 생성된 emp_id 반환
            rs = pstmt.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
            return 0;
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }

    // ── 계정 INSERT ──
    public int insertAccount(Connection con, int empId,
                              String username, String passwordHash) throws SQLException {
        String sql = "INSERT INTO account " +
                     "(emp_id, username, password_hash, role, password_changed_at) " +
                     "VALUES (?, ?, ?, '일반', NOW())";
        PreparedStatement pstmt = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, empId);
            pstmt.setString(2, username);
            pstmt.setString(3, passwordHash);
            return pstmt.executeUpdate();
        } finally {
            if (pstmt != null) pstmt.close();
        }
    }

    // ── 연차 INSERT ──
    public int insertAnnualLeave(Connection con, int empId,
                                  int leaveYear, double totalDays) throws SQLException {
        String sql = "INSERT INTO annual_leave " +
                     "(emp_id, leave_year, total_days, used_days, remain_days) " +
                     "VALUES (?, ?, ?, 0, ?)";
        PreparedStatement pstmt = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, empId);
            pstmt.setInt(2, leaveYear);
            pstmt.setDouble(3, totalDays);
            pstmt.setDouble(4, totalDays);
            return pstmt.executeUpdate();
        } finally {
            if (pstmt != null) pstmt.close();
        }
    }

    // ── 직급 기본급 조회 (직급 선택 시 기본급 자동 세팅용) ──
    public int getBaseSalaryByPosition(Connection con, int positionId) throws SQLException {
        String sql = "SELECT base_salary FROM job_position WHERE position_id = ?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, positionId);
            rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt("base_salary") : 0;
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }
    
    //이메일 중복 확인
    public boolean isEmailExist(Connection con, String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM employee WHERE email = ?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, email);
            rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }
    
    //직원 등록 시 지정될 사번을 미리 확인
    public String getNextEmpNo(Connection con) throws SQLException {
        String sql = "SELECT MAX(CAST(SUBSTRING(emp_no, 4) AS UNSIGNED)) AS max_no " +
                     "FROM employee";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            int maxNo = 0;
            if (rs.next() && rs.getObject("max_no") != null) {
                maxNo = rs.getInt("max_no");
            }
            return String.format("EMP%03d", maxNo + 1);
        } finally {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
        }
    }
    
    // 직원 등록 시 인사발령 이력 INSERT
    public int insertPersonnelHistory(Connection con, int empId,
            int toDeptId, int toPositionId, String toRole,
            String hireDate, int approvedBy) throws SQLException {
        String sql = "INSERT INTO personnel_history " +
                     "(emp_id, change_type, change_date, " +
                     "from_dept_id, to_dept_id, " +
                     "from_position_id, from_role, " +
                     "to_position_id, to_role, " +
                     "reason, approved_by) " +
                     "VALUES (?, '입사', ?, NULL, ?, NULL, NULL, ?, ?, '신규 입사', ?)";
        PreparedStatement pstmt = null;
        try {
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, empId);
            pstmt.setObject(2, java.time.LocalDate.parse(hireDate).atStartOfDay());
            pstmt.setInt(3, toDeptId);
            pstmt.setInt(4, toPositionId);
            pstmt.setString(5, toRole);
            pstmt.setInt(6, approvedBy);
            return pstmt.executeUpdate();
        } finally {
            if (pstmt != null) pstmt.close();
        }
    }
}