package com.hrms.sal.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import com.hrms.att.service.AttendanceStatusService;
import com.hrms.common.db.DatabaseConnection;
import com.hrms.common.util.NotificationUtil;
import com.hrms.sal.dao.SalaryCalcDAO;
import com.hrms.sal.dto.DeductionRateDTO;
import com.hrms.sal.dto.SalaryCalcDTO;

public class SalaryCalcService {

    private final SalaryCalcDAO dao = new SalaryCalcDAO();
    private AttendanceStatusService attendanceStatusService = new AttendanceStatusService();

    // ─────────────────────────────────────────────
    //  급여 계산 (전 직원 INSERT)
    // ─────────────────────────────────────────────
    public void calculate(int year, int month) {
    	
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            DeductionRateDTO rate = dao.selectDeductionRate(year, conn);
            if (rate == null) {
                throw new RuntimeException(year + "년 공제율 데이터가 없습니다. 공제율 관리에서 먼저 등록해주세요.");
            }

            List<SalaryCalcDTO> empList = dao.selectEmployeesForCalc(conn);
            if (empList.isEmpty()) {
                throw new RuntimeException("재직 중인 직원이 없습니다.");
            }

            int skipped = 0;
            for (SalaryCalcDTO dto : empList) {
                // 이미 완료 처리된 직원은 건너뜀
                if (dao.existsSalary(dto.getEmpId(), year, month, conn)) {
                    skipped++;
                    continue;
                }
                calcAndFill(dto, year, month, rate, conn);
                dao.insertSalary(dto, year, month, conn);
            }

            conn.commit();

        } catch (SQLException e) {
            rollback(conn);
            e.printStackTrace();
            throw new RuntimeException("급여 계산 중 오류가 발생했습니다.", e);
        } catch (RuntimeException e) {
            rollback(conn);
            throw e;
        } finally {
            close(conn);
        }
    }

    // ─────────────────────────────────────────────
    //  재계산 (대기 삭제 후 재INSERT)
    // ─────────────────────────────────────────────
    public void recalculate(int year, int month) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // status='대기'만 삭제, '완료'는 절대 건드리지 않음
            dao.deleteUnpaidSalaries(year, month, conn);

            DeductionRateDTO rate = dao.selectDeductionRate(year, conn);
            if (rate == null) {
                throw new RuntimeException(year + "년 공제율 데이터가 없습니다.");
            }

            List<SalaryCalcDTO> empList = dao.selectEmployeesForCalc(conn);
            for (SalaryCalcDTO dto : empList) {
                // 이미 완료 처리된 직원은 재계산 제외
                if (dao.existsSalary(dto.getEmpId(), year, month, conn)) continue;
                calcAndFill(dto, year, month, rate, conn);
                dao.insertSalary(dto, year, month, conn);
            }

            conn.commit();

        } catch (SQLException e) {
            rollback(conn);
            e.printStackTrace();
            throw new RuntimeException("재계산 중 오류가 발생했습니다.", e);
        } catch (RuntimeException e) {
            rollback(conn);
            throw e;
        } finally {
            close(conn);
        }
    }

 // ─────────────────────────────────────────────
//  근태 마감 여부 조회 (외부에서 호출용)
// ─────────────────────────────────────────────
public boolean isAttendanceClosed(int year, int month) {
    Connection conn = null;
    try {
        conn = DatabaseConnection.getConnection();
        return dao.isAttendanceClosed(year, month, conn);
    } catch (SQLException e) {
        e.printStackTrace();
        throw new RuntimeException("근태 마감 조회 중 오류가 발생했습니다.", e);
    } finally {
        close(conn);
    }
}

// ─────────────────────────────────────────────
//  개별 지급 처리
// ─────────────────────────────────────────────
public void payOne(int salaryId, int actorEmpId, int year, int month) {

    // ── 근태 마감 여부 검증 ──
    if (!isAttendanceClosed(year, month)) {
        throw new RuntimeException(year + "년 " + month + "월 근태가 마감되지 않았습니다. 근태 마감 후 지급 처리해주세요.");
    }

    Connection conn = null;
    try {
        conn = DatabaseConnection.getConnection();
        conn.setAutoCommit(false);

        int updated = dao.updatePayOne(salaryId, conn);
        if (updated == 0) {
            throw new RuntimeException("이미 지급 완료된 급여이거나 존재하지 않는 데이터입니다.");
        }
        dao.insertAuditLog(actorEmpId, salaryId, "대기", "완료", conn);
        conn.commit();

    } catch (SQLException e) {
        rollback(conn);
        e.printStackTrace();
        throw new RuntimeException("지급 처리 중 오류가 발생했습니다.", e);
    } catch (RuntimeException e) {
        rollback(conn);
        throw e;
    } finally {
        close(conn);
    }

    // 알림: 트랜잭션 외부 격리
    try {
        Connection tmpConn = DatabaseConnection.getConnection();
        try {
            SalaryCalcDTO info = dao.selectSalaryById(salaryId, tmpConn);
            if (info != null) {
                NotificationUtil.sendSalaryPaid(
                    info.getEmpId(), info.getSalaryYear(),
                    info.getSalaryMonth(), info.getNetSalary(), salaryId
                );
            }
        } finally { tmpConn.close(); }
    } catch (Exception e) {
        System.err.println("[알림 실패 무시] payOne: " + e.getMessage());
    }
}

// ─────────────────────────────────────────────
//  전체 지급 처리
// ─────────────────────────────────────────────
public void payAll(int year, int month, int actorEmpId) {

    // ── 근태 마감 여부 검증 ──
    if (!isAttendanceClosed(year, month)) {
        throw new RuntimeException(year + "년 " + month + "월 근태가 마감되지 않았습니다. 근태 마감 후 지급 처리해주세요.");
    }

    Connection conn = null;
    List<Integer> paidIds = null;
    try {
        conn = DatabaseConnection.getConnection();
        conn.setAutoCommit(false);

        int updated = dao.updatePayAll(year, month, conn);
        if (updated == 0) {
            throw new RuntimeException("지급 처리할 대기 급여가 없습니다.");
        }

        paidIds = dao.selectSalaryIdsByMonth(year, month, conn);
        for (int sid : paidIds) {
            dao.insertAuditLog(actorEmpId, sid, "대기", "완료", conn);
        }
        conn.commit();

    } catch (SQLException e) {
        rollback(conn);
        e.printStackTrace();
        throw new RuntimeException("전체 지급 처리 중 오류가 발생했습니다.", e);
    } catch (RuntimeException e) {
        rollback(conn);
        throw e;
    } finally {
        close(conn);
    }

    // 알림: 트랜잭션 외부 격리
    if (paidIds != null) {
        for (int sid : paidIds) {
            try {
                Connection tmpConn = DatabaseConnection.getConnection();
                try {
                    SalaryCalcDTO info = dao.selectSalaryById(sid, tmpConn);
                    if (info != null) {
                        NotificationUtil.sendSalaryPaid(
                            info.getEmpId(), info.getSalaryYear(),
                            info.getSalaryMonth(), info.getNetSalary(), sid
                        );
                    }
                } finally { tmpConn.close(); }
            } catch (Exception e) {
                System.err.println("[알림 실패 무시] payAll salaryId=" + sid + ": " + e.getMessage());
            }
        }
    }
}

/**
 * 외부 트랜잭션에서 호출용 재계산
 * AttendanceCloseService에서 근태 마감과 같은 트랜잭션으로 묶음
 */
public void recalculateInTransaction(int year, int month,
                                     Connection conn) throws SQLException {

    // 대기 상태만 삭제 (완료 건 보호)
    dao.deleteUnpaidSalaries(year, month, conn);

    DeductionRateDTO rate = dao.selectDeductionRate(year, conn);
    if (rate == null) {
        throw new RuntimeException(year + "년 공제율 데이터가 없습니다.");
    }

    List<SalaryCalcDTO> empList = dao.selectEmployeesForCalc(conn);
    for (SalaryCalcDTO dto : empList) {
        // 완료 처리된 직원은 건너뜀
        if (dao.existsSalary(dto.getEmpId(), year, month, conn)) continue;
        calcAndFill(dto, year, month, rate, conn);
        dao.insertSalary(dto, year, month, conn);
    }
}

    // ─────────────────────────────────────────────
    //  화면 출력용 목록 조회
    // ─────────────────────────────────────────────
    public List<SalaryCalcDTO> getSalaryList(int year, int month) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            return dao.selectSalaryList(year, month, conn);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("급여 목록 조회 중 오류가 발생했습니다.", e);
        } finally {
            close(conn);
        }
    }

    /** 연도 목록 */
    public int[] getYearOptions() {
        int cur = LocalDate.now().getYear();
        return new int[]{cur, cur - 1, cur - 2};
    }

    // ─────────────────────────────────────────────
    //  계산 공식 (핵심 로직)
    // ─────────────────────────────────────────────
    private void calcAndFill(SalaryCalcDTO dto, int year, int month,
            DeductionRateDTO rate, Connection conn) throws SQLException {
		
		int baseSalary = dto.getBaseSalary();
		
		// ── 초과근무수당 ──
		double overtimeHours = dao.selectOvertimeHours(dto.getEmpId(), year, month, conn);
		int overtimePay = (int) Math.round((baseSalary / 209.0) * overtimeHours * 1.5);
		dto.setOvertimePay(overtimePay);
		dto.setOtherAllowance(0);
		
		// ── 지급합계 ──
		int gross = baseSalary
		 + dto.getMealAllowance()
		 + dto.getTransportAllowance()
		 + dto.getPositionAllowance()
		 + overtimePay;
		dto.setGrossSalary(gross);
		
		// ── 4대보험 (각각 버림) ──
		int nationalPension     = (int)(gross * rate.getNationalPensionRate().doubleValue());
		int healthInsurance     = (int)(gross * rate.getHealthInsuranceRate().doubleValue());
		int longTermCare        = (int)(healthInsurance * rate.getLongTermCareRate().doubleValue());
		int employmentInsurance = (int)(gross * rate.getEmploymentInsuranceRate().doubleValue());
		
		dto.setNationalPension(nationalPension);
		dto.setHealthInsurance(healthInsurance);
		dto.setLongTermCare(longTermCare);
		dto.setEmploymentInsurance(employmentInsurance);
		
		// ── 무급 공제 ──
		BigDecimal unpaidDays = dao.selectUnpaidLeaveDays(dto.getEmpId(), year, month, conn);
		int unpaidDeduction   = (int)((baseSalary / 22.0) * unpaidDays.doubleValue());
		dto.setUnpaidLeaveDays(unpaidDays);
		dto.setUnpaidDeduction(unpaidDeduction);
		
		// ── 소득세 ──
		int incomeTax      = dao.selectIncomeTax(gross, year, conn);
		int localIncomeTax = (int)(incomeTax * 0.10);
		dto.setIncomeTax(incomeTax);
		dto.setLocalIncomeTax(localIncomeTax);
		
		// ── 공제합계: 각 항목 직접 합산 ──
		int totalDeduction = nationalPension
		          + healthInsurance
		          + longTermCare
		          + employmentInsurance
		          + unpaidDeduction
		          + incomeTax
		          + localIncomeTax;
		dto.setTotalDeduction(totalDeduction);
		
		// ── 실수령액: gross - totalDeduction 으로 정확히 맞춤 ──
		// DB CHECK: net_salary = gross_salary - total_deduction 을 반드시 만족해야 함
		int netSalary = gross - totalDeduction;
		if (netSalary < 0) netSalary = 0;
		
		// ⚠ netSalary가 0으로 보정된 경우 totalDeduction도 맞춰줌
		if (gross - totalDeduction < 0) {
		totalDeduction = gross;
		dto.setTotalDeduction(totalDeduction);
		}
		
		dto.setNetSalary(netSalary);
		}

    // ── 공통 유틸 ──
    private void rollback(Connection conn) {
        if (conn != null) try { conn.rollback(); } catch (SQLException e) { e.printStackTrace(); }
    }

    private void close(Connection conn) {
        if (conn != null) {
            try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }
}