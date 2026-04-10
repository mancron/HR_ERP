package com.hrms.att.service;

import com.hrms.att.dao.AttendanceCloseDAO;
import com.hrms.att.dao.AttendanceDAO;
import com.hrms.att.dao.AttendanceLogDAO;
import com.hrms.att.dao.LeaveDAO;
import com.hrms.att.dto.AttIssueDTO;
import com.hrms.att.dto.AttendanceDTO;
import com.hrms.common.db.DatabaseConnection;
import com.hrms.common.util.NotificationUtil;
import com.hrms.emp.dao.EmpDAO;

import java.sql.Connection;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class AttendanceStatusService {

	private AttendanceDAO attendanceDAO = new AttendanceDAO();
	private LeaveDAO leaveDAO = new LeaveDAO();
	private AttendanceLogDAO logDAO = new AttendanceLogDAO();
	private AttendanceCloseDAO closeDAO = new AttendanceCloseDAO();
	private AttendanceIssueService issueService = new AttendanceIssueService();
	private EmpDAO empDAO = new EmpDAO();

	public boolean isClosed(int year, int month) {
		return closeDAO.isClosed(year, month);
	}

	private void validateClosed(LocalDate date) {
		if (isClosed(date.getYear(), date.getMonthValue())) {
			throw new RuntimeException("마감된 데이터는 수정할 수 없습니다.");
		}
	}

	// =========================
	// 1️⃣ 결근 처리
	// =========================
	public void markAbsent(int empId, LocalDate date, int actorId) {

		Connection conn = null;

		try {
			conn = DatabaseConnection.getConnection();
			conn.setAutoCommit(false);

			// 1. 마감 체크
			validateClosed(date);

			// 2. 휴가 체크
			if (leaveDAO.existsByDate(empId, date)) {
				throw new RuntimeException("휴가 날짜는 결근 처리할 수 없습니다.");
			}

			// 🔥 3. BEFORE 데이터
			AttendanceDTO before = attendanceDAO.getAttendanceByDate(empId, Date.valueOf(date), conn);

			if (before != null && before.getCheckIn() != null) {
				throw new RuntimeException("출근 기록이 있는 날은 결근 처리 불가");
			}

			// 🔥 기존 값 저장
			Time oldIn = before != null ? before.getCheckIn() : null;
			Time oldOut = before != null ? before.getCheckOut() : null;
			String oldStatus = before != null ? before.getStatus() : null;

			// 4. INSERT or UPDATE
			if (before == null) {
				attendanceDAO.insertAbsent(empId, Date.valueOf(date), conn);
			} else {
				attendanceDAO.updateStatus(empId, Date.valueOf(date), "결근", null, conn);
			}

			// 🔥 5. AFTER 데이터 (결근 상태)
			AttendanceDTO after = attendanceDAO.getAttendanceByDate(empId, Date.valueOf(date), conn);

			// 🔥 6. 로그 저장
			logDAO.insertLog(empId, date, actorId, "ABSENT", oldIn, after.getCheckIn(), oldOut, after.getCheckOut(),
					oldStatus, "결근", "결근 처리", conn);

			// 7. 커밋
			conn.commit();

		} catch (Exception e) {
			try {
				if (conn != null)
					conn.rollback();
			} catch (Exception ignore) {
			}
			throw new RuntimeException("결근 처리 실패", e);

		} finally {
			try {
				if (conn != null)
					conn.close();
			} catch (Exception ignore) {
			}
		}

		// 8. 알림
		NotificationUtil.sendAttendanceAbsent(empId, date.toString());
	}

	// =========================
	// 2️⃣ 퇴근 미처리 수정
	// =========================
	public void updateCheckout(int empId, LocalDate date, Time checkout, String note, int actorId) {

		Connection conn = null;

		try {
			conn = DatabaseConnection.getConnection();
			conn.setAutoCommit(false);

			// 1. 마감 체크
			validateClosed(date);

			AttendanceDTO dto = attendanceDAO.getAttendanceByDate(empId, Date.valueOf(date), conn);

			if (dto == null || dto.getCheckIn() == null) {
				throw new RuntimeException("출근 기록이 없는 날은 퇴근 처리 불가");
			}

			// 2. 퇴근 수정
			attendanceDAO.updateCheckout(empId, Date.valueOf(date), checkout, note, conn);

			// 🔥 반드시 다시 조회
			AttendanceDTO updated = attendanceDAO.getAttendanceByDate(empId, Date.valueOf(date), conn);

			// 🔥 최신 값으로 계산
			String newStatus = calculateStatus(updated.getCheckIn(), updated.getCheckOut());

			attendanceDAO.updateStatus(empId, Date.valueOf(date), newStatus, "자동 상태 변경", conn);

			logDAO.insertLog(empId, date, actorId, "CHECKOUT_FIX", dto.getCheckIn(), updated.getCheckIn(),
					dto.getCheckOut(), updated.getCheckOut(), dto.getStatus(), newStatus, note, conn);

			// 3. 커밋
			conn.commit();

		} catch (Exception e) {
			try {
				if (conn != null)
					conn.rollback();
			} catch (Exception ignore) {
			}
			throw new RuntimeException("퇴근 수정 실패", e);

		} finally {
			try {
				if (conn != null)
					conn.close();
			} catch (Exception ignore) {
			}
		}

		// 4. 알림
		NotificationUtil.sendAttendanceCheckoutUpdated(empId, date.toString());
	}

	// 출근 시간 수정
	public void updateCheckIn(int empId, LocalDate date, Time checkIn, String note, int actorId) {

		Connection conn = null;

		try {
			conn = DatabaseConnection.getConnection();
			conn.setAutoCommit(false);

			validateClosed(date);

			AttendanceDTO dto = attendanceDAO.getAttendanceByDate(empId, Date.valueOf(date), conn);

			if (dto == null) {
				throw new RuntimeException("출근 기록 없음");
			}

// 👉 출근만 수정
			attendanceDAO.updateCheckIn(empId, Date.valueOf(date), checkIn, note, conn);

			AttendanceDTO updated = attendanceDAO.getAttendanceByDate(empId, Date.valueOf(date), conn);

			String newStatus = calculateStatus(updated.getCheckIn(), updated.getCheckOut());

			attendanceDAO.updateStatus(empId, Date.valueOf(date), newStatus, "자동 상태 변경", conn);

			logDAO.insertLog(empId, date, actorId, "CHECKOUT_FIX", dto.getCheckIn(), updated.getCheckIn(),
					dto.getCheckOut(), updated.getCheckOut(), dto.getStatus(), newStatus, note, conn);

			conn.commit();

		} catch (Exception e) {
			try {
				if (conn != null)
					conn.rollback();
			} catch (Exception ignore) {
			}
			throw new RuntimeException("출근 수정 실패", e);

		} finally {
			try {
				if (conn != null)
					conn.close();
			} catch (Exception ignore) {
			}
		}
	}

	// 상태값 업데이트
	private String calculateStatus(Time checkIn, Time checkOut) {

		if (checkIn != null && checkOut != null) {
			if (checkIn.toLocalTime().isAfter(LocalTime.of(9, 0))) {
				return "지각";
			} else {
				return "출근";
			}
		}

		return "결근";
	}

	public void closeMonth(int year, int month, int actorId) {

		LocalDate now = LocalDate.now();

		// 현재 월 마감 방지
		if (year == now.getYear() && month == now.getMonthValue()) {
			throw new IllegalStateException("현재 월은 마감할 수 없습니다.");
		}

		// 이미 마감된 월
		if (isClosed(year, month)) {
			throw new IllegalStateException("이미 마감된 월입니다.");
		}

		// 미퇴근 체크
		if (existsUnfinishedCheckoutAll(year, month)) {
			throw new IllegalStateException("퇴근 미처리 데이터가 존재합니다.");
		}

		// 결근 후보 체크
		if (existsAbsentCandidateAll(year, month)) {
			throw new IllegalStateException("결근 후보 데이터가 존재합니다.");
		}

		// 마감 처리
		closeDAO.closeMonth(year, month, actorId);
	}

	public boolean existsUnfinishedCheckoutAll(int year, int month) {

		List<Integer> empList = empDAO.getAllEmpIds(); // 재직자만 가져오도록 해야 함

		for (int empId : empList) {

			List<AttIssueDTO> issues = issueService.getIssues(empId, year, month);

			boolean hasUnfinished = issues.stream().anyMatch(i -> "미퇴근".equals(i.getType()));

			if (hasUnfinished) {
				return true;
			}
		}

		return false;
	}

	public boolean existsAbsentCandidateAll(int year, int month) {

		List<Integer> empList = empDAO.getAllEmpIds(); // 재직자만

		for (int empId : empList) {

			List<AttIssueDTO> issues = issueService.getIssues(empId, year, month);

			boolean hasAbsent = issues.stream().anyMatch(i -> "결근 후보".equals(i.getType()));

			if (hasAbsent) {
				return true;
			}
		}

		return false;
	}

}