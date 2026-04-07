package com.hrms.emp.service;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.hrms.common.db.DatabaseConnection;
import com.hrms.emp.dao.RegDAO;
import com.hrms.emp.dto.EmpDTO;

public class RegService {

    private RegDAO regDao = new RegDAO();

    // ── username 중복 확인 ──
    public boolean isUsernameExist(String username) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return regDao.isUsernameExist(con, username);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    
    
    // ── 직원 등록 트랜잭션 ──
    public String registerEmployee(EmpDTO dto, String username) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            // 1. 사번 채번
            String empNo = regDao.generateEmpNo(con);
            dto.setEmp_no(empNo);

            // 2. employee INSERT → emp_id 반환
            int empId = regDao.insertEmployee(con, dto);
            if (empId <= 0) {
                con.rollback();
                return null;
            }

            // 3. 임시 비밀번호 생성 → BCrypt 해시
            String tempPw = generateTempPassword();
            String hashed = hashPassword(tempPw);

            // 4. account INSERT
            int r2 = regDao.insertAccount(con, empId, username, hashed);
            if (r2 <= 0) {
            	con.rollback();
            	return null;
            }

            // 5. 연차 계산 → annual_leave INSERT
            LocalDate hireDate = LocalDate.parse(dto.getHire_date());
            double totalDays = calculateAnnualLeave(hireDate);
            int leaveYear = LocalDate.now().getYear();

            int r3 = regDao.insertAnnualLeave(con, empId, leaveYear, totalDays);
            if (r3 <= 0) {
                con.rollback();
                return null;
            }

            con.commit();
            return tempPw;

        } catch (Exception e) {
            e.printStackTrace();
            try { if (con != null) con.rollback(); } catch (Exception ex) { ex.printStackTrace(); }
            return null;
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // ── 입사일 기준 연차 계산 ──
    // 1년 미만: 경과 월 수 (최대 11일)
    // 1년 이상: 15일 + (근속연수 - 1) / 2 (최대 25일)
    private double calculateAnnualLeave(LocalDate hireDate) {
        LocalDate today = LocalDate.now();
        long years  = ChronoUnit.YEARS.between(hireDate, today);
        long months = ChronoUnit.MONTHS.between(hireDate, today);

        if (years < 1) {
            return Math.min(months, 11);
        } else {
            return Math.min(15 + (years - 1) / 2.0, 25);
        }
    }

    // ── 임시 비밀번호 생성 (영문 대소문자 + 숫자 8자리) ──
    private String generateTempPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // ── BCrypt 해시 ──
    private String hashPassword(String password) {
        return org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt());
    }
    
    //이메일 중복확인
    public boolean isEmailExist(String email) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return regDao.isEmailExist(con, email);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }
    
    
    public String getNextEmpNo() {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return regDao.getNextEmpNo(con);
        } catch (Exception e) {
            e.printStackTrace();
            return "-";
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }
}