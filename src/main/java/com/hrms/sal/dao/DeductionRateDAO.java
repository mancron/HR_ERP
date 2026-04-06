package com.hrms.sal.dao;

import com.hrms.sal.dto.DeductionRateDTO;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DeductionRateDAO {

    /** 전체 공제율 목록 조회 (연도 내림차순) */
    public List<DeductionRateDTO> selectAll(Connection conn) throws SQLException {
        List<DeductionRateDTO> list = new ArrayList<>();
        String sql =
            "SELECT rate_id, target_year, national_pension_rate, " +
            "       health_insurance_rate, long_term_care_rate, " +
            "       employment_insurance_rate, created_at " +
            "FROM deduction_rate " +
            "ORDER BY target_year DESC";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
        return list;
    }

    /** 단건 조회 (수정 폼용) */
    public DeductionRateDTO selectById(int rateId, Connection conn) throws SQLException {
        String sql =
            "SELECT rate_id, target_year, national_pension_rate, " +
            "       health_insurance_rate, long_term_care_rate, " +
            "       employment_insurance_rate, created_at " +
            "FROM deduction_rate WHERE rate_id = ?";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, rateId);
            rs = pstmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
        return null;
    }

    /** 연도 중복 체크 */
    public boolean existsByYear(int targetYear, Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM deduction_rate WHERE target_year = ?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, targetYear);
            rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
        return false;
    }

    /** INSERT */
    public void insert(DeductionRateDTO dto, Connection conn) throws SQLException {
        String sql =
            "INSERT INTO deduction_rate " +
            "(target_year, national_pension_rate, health_insurance_rate, " +
            " long_term_care_rate, employment_insurance_rate) " +
            "VALUES (?, ?, ?, ?, ?)";

        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, dto.getTargetYear());
            pstmt.setBigDecimal(2, dto.getNationalPensionRate());
            pstmt.setBigDecimal(3, dto.getHealthInsuranceRate());
            pstmt.setBigDecimal(4, dto.getLongTermCareRate());
            pstmt.setBigDecimal(5, dto.getEmploymentInsuranceRate());
            pstmt.executeUpdate();
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
    }

    /** UPDATE */
    public int update(DeductionRateDTO dto, Connection conn) throws SQLException {
        String sql =
            "UPDATE deduction_rate " +
            "SET national_pension_rate = ?, health_insurance_rate = ?, " +
            "    long_term_care_rate = ?, employment_insurance_rate = ? " +
            "WHERE rate_id = ?";

        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setBigDecimal(1, dto.getNationalPensionRate());
            pstmt.setBigDecimal(2, dto.getHealthInsuranceRate());
            pstmt.setBigDecimal(3, dto.getLongTermCareRate());
            pstmt.setBigDecimal(4, dto.getEmploymentInsuranceRate());
            pstmt.setInt(5, dto.getRateId());
            return pstmt.executeUpdate();
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
    }

    // ── Private 유틸 ──
    private DeductionRateDTO mapRow(ResultSet rs) throws SQLException {
        DeductionRateDTO dto = new DeductionRateDTO();
        dto.setRateId(rs.getInt("rate_id"));
        dto.setTargetYear(rs.getInt("target_year"));
        dto.setNationalPensionRate(rs.getBigDecimal("national_pension_rate"));
        dto.setHealthInsuranceRate(rs.getBigDecimal("health_insurance_rate"));
        dto.setLongTermCareRate(rs.getBigDecimal("long_term_care_rate"));
        dto.setEmploymentInsuranceRate(rs.getBigDecimal("employment_insurance_rate"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) dto.setCreatedAt(createdAt.toLocalDateTime());
        return dto;
    }
}