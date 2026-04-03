package com.hrms.sal.service;

import com.hrms.common.db.DatabaseConnection;
import com.hrms.sal.dao.DeductionRateDAO;
import com.hrms.sal.dto.DeductionRateDTO;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class DeductionRateService {

    private final DeductionRateDAO dao = new DeductionRateDAO();

    /** 전체 목록 조회 + 현재 연도 표시 세팅 */
    public List<DeductionRateDTO> getAll() {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            List<DeductionRateDTO> list = dao.selectAll(conn);
            int currentYear = LocalDate.now().getYear();
            for (DeductionRateDTO dto : list) {
                dto.setCurrentYear(dto.getTargetYear() == currentYear);
            }
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("공제율 목록 조회 중 오류가 발생했습니다.", e);
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }

    /** 단건 조회 (수정 폼용) */
    public DeductionRateDTO getById(int rateId) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            return dao.selectById(rateId, conn);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("공제율 조회 중 오류가 발생했습니다.", e);
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }

    /** 신규 연도 추가 */
    public void add(int targetYear,
                    BigDecimal nationalPension,
                    BigDecimal healthInsurance,
                    BigDecimal longTermCare,
                    BigDecimal employmentInsurance) {

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();

            // 중복 연도 사전 검증
            if (dao.existsByYear(targetYear, conn)) {
                throw new RuntimeException(targetYear + "년 공제율이 이미 존재합니다.");
            }

            DeductionRateDTO dto = new DeductionRateDTO();
            dto.setTargetYear(targetYear);
            dto.setNationalPensionRate(nationalPension);
            dto.setHealthInsuranceRate(healthInsurance);
            dto.setLongTermCareRate(longTermCare);
            dto.setEmploymentInsuranceRate(employmentInsurance);

            dao.insert(dto, conn);

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("공제율 추가 중 오류가 발생했습니다.", e);
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }

    /** 공제율 수정 */
    public void update(int rateId,
                       BigDecimal nationalPension,
                       BigDecimal healthInsurance,
                       BigDecimal longTermCare,
                       BigDecimal employmentInsurance) {

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();

            DeductionRateDTO dto = new DeductionRateDTO();
            dto.setRateId(rateId);
            dto.setNationalPensionRate(nationalPension);
            dto.setHealthInsuranceRate(healthInsurance);
            dto.setLongTermCareRate(longTermCare);
            dto.setEmploymentInsuranceRate(employmentInsurance);

            int updated = dao.update(dto, conn);
            if (updated == 0) {
                throw new RuntimeException("수정할 공제율을 찾을 수 없습니다.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("공제율 수정 중 오류가 발생했습니다.", e);
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }
}