package com.hrms.sal.service;

import com.hrms.common.db.DatabaseConnection;
import com.hrms.sal.dao.SalarySlipDAO;
import com.hrms.sal.dto.SalarySlipDTO;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class SalarySlipService {

    private final SalarySlipDAO dao = new SalarySlipDAO();

    /** 명세서 조회 */
    public SalarySlipDTO getSlip(int empId, int year, int month) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            return dao.selectSlip(empId, year, month, conn);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("명세서 조회 중 오류가 발생했습니다.", e);
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }

    /** 전체 직원 목록 (관리자/HR담당자용) */
    public List<String[]> getEmpList() {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            return dao.selectEmpList(conn);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("직원 목록 조회 중 오류가 발생했습니다.", e);
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }

    /** 해당 직원의 급여 존재 연도/월 목록 */
    public List<String[]> getAvailableMonths(int empId) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            return dao.selectAvailableMonths(empId, conn);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("조회 가능 기간 조회 중 오류가 발생했습니다.", e);
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }

    /** 연도 목록 (현재 기준 최근 3년) */
    public int[] getYearOptions() {
        int cur = LocalDate.now().getYear();
        return new int[]{cur, cur - 1, cur - 2};
    }
}