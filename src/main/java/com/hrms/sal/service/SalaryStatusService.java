package com.hrms.sal.service;

import com.hrms.common.db.DatabaseConnection;
import com.hrms.sal.dao.SalaryStatusDAO;
import com.hrms.sal.dto.SalaryStatusDTO;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SalaryStatusService {

    private final SalaryStatusDAO dao = new SalaryStatusDAO();

    /**
     * 급여 현황 전체 데이터 조회
     * @return salaryList, deptList, summary(통계) 포함한 Map
     */
    public Map<String, Object> getSalaryStatus(int year, int month, int deptId) {
        Map<String, Object> result = new HashMap<>();
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();

            // 급여 목록
            List<SalaryStatusDTO> salaryList = dao.selectByYearMonth(year, month, deptId, conn);
            result.put("salaryList", salaryList);

            // 부서 목록 (필터용)
            result.put("deptList", dao.selectDeptListWithName(conn));

            // 요약 통계 계산
            long totalGross     = 0;
            long totalNet       = 0;
            int  empCount       = salaryList.size();

            for (SalaryStatusDTO dto : salaryList) {
                totalGross += dto.getGrossSalary();
                totalNet   += dto.getNetSalary();
            }

            long avgNet = empCount > 0 ? totalNet / empCount : 0;

            result.put("totalGross", totalGross);
            result.put("totalNet",   totalNet);
            result.put("avgNet",     avgNet);
            result.put("empCount",   empCount);

            return result;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("급여 현황 조회 중 오류가 발생했습니다.", e);
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }

    /** 연도 목록 생성 (현재 연도 기준 최근 3년) */
    public int[] getYearOptions() {
        int currentYear = LocalDate.now().getYear();
        return new int[]{currentYear, currentYear - 1, currentYear - 2};
    }
}