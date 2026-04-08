package com.hrms.att.service;

import com.hrms.att.dao.AttendanceDAO;
import com.hrms.att.dao.LeaveDAO;
import com.hrms.att.dto.AttendanceDTO;
import com.hrms.common.db.DatabaseConnection;
import com.hrms.common.util.NotificationUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;

public class AttendanceStatusService {

	private AttendanceDAO attendanceDAO = new AttendanceDAO();
	private LeaveDAO leaveDAO = new LeaveDAO();

	// =========================
	// 🔒 마감 체크 (추후 DB 연결)
	// =========================
	private boolean isClosed(int year, int month) {
		return false;
	}

	private void validateClosed(LocalDate date) {
		if (isClosed(date.getYear(), date.getMonthValue())) {
			throw new RuntimeException("마감된 데이터는 수정할 수 없습니다.");
		}
	}

	// =========================
	// 1️⃣ 결근 처리
	// =========================
	public void markAbsent(int empId, LocalDate date) {

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

			// 3. 기존 데이터 확인
			AttendanceDTO existing = attendanceDAO.getAttendanceByDate(empId, Date.valueOf(date), conn);

			if (existing != null && existing.getCheckIn() != null) {
				throw new RuntimeException("출근 기록이 있는 날은 결근 처리 불가");
			}

			// 4. INSERT or UPDATE
			if (existing == null) {
				attendanceDAO.insertAbsent(empId, Date.valueOf(date), conn);
			} else {
				attendanceDAO.updateStatus(empId, Date.valueOf(date), "결근", null, conn);
			}

			// 5. 커밋
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

		// 6. 알림 (커밋 이후 실행)
		NotificationUtil.sendAttendanceAbsent(empId, date.toString());
	}

	// =========================
	// 2️⃣ 퇴근 미처리 수정
	// =========================
	public void updateCheckout(int empId, LocalDate date, Time checkout, String note) {

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
			AttendanceDTO updated =
			    attendanceDAO.getAttendanceByDate(empId, Date.valueOf(date), conn);

			// 🔥 최신 값으로 계산
			String newStatus = calculateStatus(
			    updated.getCheckIn(),
			    updated.getCheckOut()
			);

			attendanceDAO.updateStatus(empId, Date.valueOf(date), newStatus, "자동 상태 변경", conn);

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

	// =========================
	// 3️⃣ 전체 근태 수정
	// =========================
	public void updateAttendance(int empId, LocalDate date, Time checkIn, Time checkOut, String status, String note) {

		Connection conn = null;

		try {
			conn = DatabaseConnection.getConnection();
			conn.setAutoCommit(false);

			// 1. 마감 체크
			validateClosed(date);

			// 2. 근태 시간만 수정
			attendanceDAO.updateTime(empId, Date.valueOf(date), checkIn, checkOut, conn);

			// 3. 커밋
			conn.commit();

		} catch (Exception e) {
			try {
				if (conn != null)
					conn.rollback();
			} catch (Exception ignore) {
			}
			throw new RuntimeException("근태 수정 실패", e);

		} finally {
			try {
				if (conn != null)
					conn.close();
			} catch (Exception ignore) {
			}
		}

		// 4. 알림
		NotificationUtil.sendAttendanceUpdated(empId, date.toString());
	}

	// 출근 시간 수정
	public void updateCheckIn(int empId, LocalDate date, Time checkIn, String note) {

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

			AttendanceDTO updated =
				    attendanceDAO.getAttendanceByDate(empId, Date.valueOf(date), conn);

				String newStatus = calculateStatus(
				    updated.getCheckIn(),
				    updated.getCheckOut()
				);

			attendanceDAO.updateStatus(empId, Date.valueOf(date), newStatus, "자동 상태 변경", conn);

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
}