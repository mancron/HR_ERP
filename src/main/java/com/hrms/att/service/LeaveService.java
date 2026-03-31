package com.hrms.att.service;

import com.hrms.att.dao.LeaveDAO;
import com.hrms.att.dto.AnnualLeaveDTO;
import com.hrms.att.dto.LeaveDTO;
import com.hrms.common.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class LeaveService {

	private LeaveDAO leaveDAO = new LeaveDAO();

	// 휴가 신청 처리
	public String applyLeave(LeaveDTO dto) {
		int empId = dto.getEmpId();
		Date startDate = dto.getStartDate();
		Date endDate = dto.getEndDate();
		double days = calculateDays(startDate.toLocalDate(), endDate.toLocalDate(), dto.getLeaveType());
		dto.setDays(days);
		String reason = dto.getReason();
		if (!"반차".equals(dto.getLeaveType())) {
			dto.setHalfType(null);
		}
		// 1. 사유 공백 체크
		if (reason == null || reason.trim().isEmpty()) {
			return "empty_reason";
		}
		// 2. 날짜 검증
		if (startDate.after(endDate)) {
			return "invalid_date";
		}
		// 3. 잔여 연차 체크
		double remainDays = leaveDAO.getRemainDays(empId);
		if (days > remainDays) {
			return "not_enough";
		}
		// 4. 기간 중복 체크
		if (leaveDAO.isOverlapping(empId, startDate, endDate)) {
			return "overlap";
		}
		// 5. DB 저장
		boolean result = leaveDAO.insertLeave(dto);
		if (!result) {
			return "fail";
		}
		return "success";
	}

	public AnnualLeaveDTO getAnnualLeave(int empId) {
		return leaveDAO.getAnnualLeave(empId);
	}

	public double calculateDays(LocalDate start, LocalDate end, String leaveType) {

		long daysBetween = ChronoUnit.DAYS.between(start, end) + 1;

		if ("반차".equals(leaveType)) {
			return 0.5;
		}

		return daysBetween;
	}

	// 휴가 리스트 출력
	public List<LeaveDTO> getLeaveList(int empId) {
		return leaveDAO.getLeaveList(empId);
	}

	// 월별 휴가 리스트 출력
	public List<LeaveDTO> getLeaveListByMonth(int empId, int year, int month) {
		return leaveDAO.getLeaveListByMonth(empId, year, month);
	}

	// 신청 취소
	public boolean cancelLeave(int leaveId, int empId) {
		return leaveDAO.cancelLeave(leaveId, empId);
	}

	// 1. 기존 방식 (전체 조회 - 호환용)
	public List<LeaveDTO> getPendingLeaves() {
		return leaveDAO.getPendingLeaves(null, null, null, null);
	}

	// 2. 필터 + 정렬 포함 (신규 기능)
	public List<LeaveDTO> getPendingLeaves(String dept, String sort, String startDate, String endDate) {
		return leaveDAO.getPendingLeaves(dept, sort, startDate, endDate);
	}

	// 3. 부서 목록 조회 (드롭다운용)
	public List<String> getPendingDeptList() {
		return leaveDAO.getPendingDeptList();
	}

	// 휴가 승인 반려 처리
	// 휴가 데이터 수정 + 잔여 연차 값 업데이트 트랜잭션 처리
	public boolean approveLeave(int leaveId, int approverId, String status, String reason) {
		Connection conn = null;
		try {
			conn = DatabaseConnection.getConnection();
			conn.setAutoCommit(false); // 🔥 트랜잭션 시작
			// 휴가 정보 조회
			LeaveDTO leave = leaveDAO.getLeaveById(conn, leaveId);
			if (leave == null) {
				throw new Exception("휴가 정보 없음");
			}
			int empId = leave.getEmpId();
			// 휴가 일수 계산
			double days = calculateDays(leave.getStartDate().toLocalDate(), leave.getEndDate().toLocalDate(),
					leave.getLeaveType());
			// 승인일 경우만 연차 차감
			if ("승인".equals(status)) {
				double remain = leaveDAO.getRemainDays(empId);
				if (remain < days) {
					throw new Exception("연차 부족");
				}
				leaveDAO.updateAnnualLeave(conn, empId, days);
			}
			// 상태 변경 (승인/반려 공통)
			boolean result = leaveDAO.updateLeaveStatus(conn, leaveId, approverId, status, reason);
			if (!result) {
				throw new Exception("상태 변경 실패");
			}
			conn.commit(); // 성공 시 커밋
			return true;
		} catch (Exception e) {
			try {
				if (conn != null)
					conn.rollback(); // 실패 시 롤백
			} catch (Exception ex) {
			}
			e.printStackTrace();
		} finally {
			try {
				if (conn != null) {
					conn.setAutoCommit(true);
					conn.close();
				}
			} catch (Exception e) {
			}
		}
		return false;
	}
}
