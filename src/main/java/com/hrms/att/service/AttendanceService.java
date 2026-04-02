package com.hrms.att.service;

import com.hrms.att.dao.AttendanceDAO;
import com.hrms.att.dto.AttendanceDTO;
import com.hrms.att.dto.AttendanceSummaryDTO;
import com.hrms.common.db.DatabaseConnection;
import com.hrms.sys.dao.HolidayDAO;

import java.sql.Connection;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.ArrayList;
import java.time.format.TextStyle;
import java.util.Locale;

public class AttendanceService {

	private AttendanceDAO dao = new AttendanceDAO();
	private LeaveService leaveService = new LeaveService();
	private HolidayDAO holidayDAO = new HolidayDAO();

	// 1. 오늘 근태 조회
	public AttendanceDTO getTodayAttendance(int empId) {
		AttendanceDTO dto = dao.getTodayAttendance(empId);
		applyNoCheckoutStatus(dto);
		return dto;
	}

	// 2. 출근 처리
	public void checkIn(int empId) {
		AttendanceDTO dto = dao.getTodayAttendance(empId);
		if (dto != null)
			return;
		String status = getStatus();
		dao.checkIn(empId, status);
	}

	// 3. 퇴근 처리
	public void checkOut(int empId) {
		AttendanceDTO dto = dao.getTodayAttendance(empId);
		if (dto == null || dto.getCheckOut() != null)
			return;
		double workHours = calcWorkHours(dto);
		dao.checkOut(empId, workHours);
	}

	// 4. 공휴일 체크
	public boolean isHoliday() {
		return dao.isHoliday();
	}

	// ===== 내부 로직 =====

	// 출근 상태 판단
	private String getStatus() {
		LocalTime now = LocalTime.now();
		return now.isBefore(LocalTime.of(9, 0)) ? "출근" : "지각";
	}

	// 근무시간 계산
	private double calcWorkHours(AttendanceDTO dto) {
		LocalTime in = dto.getCheckIn().toLocalTime();
		LocalTime out = LocalTime.now();
		long minutes = Duration.between(in, out).toMinutes();
		return minutes / 60.0;
	}

	// 출퇴근 리스트
	public List<AttendanceDTO> getMonthlyAttendance(int empId, String yearMonth) {
		List<AttendanceDTO> list = dao.getMonthlyAttendance(empId, yearMonth);
		for (AttendanceDTO dto : list) {
			applyNoCheckoutStatus(dto);
		}
		return list;
	}

	// 퇴근 미처리 체크
	private void applyNoCheckoutStatus(AttendanceDTO dto) {
		if (dto == null)
			return;
		// 퇴근 안했고
		if (dto.getCheckOut() == null) {
			java.time.LocalDate today = java.time.LocalDate.now();
			java.time.LocalDate workDate = dto.getWorkDate().toLocalDate();
			// 오늘이 아니면 → 퇴근 미처리
			if (!workDate.equals(today)) {
				dto.setStatus("퇴근미처리");
			}
		}
	}

	// 요일 설정
	private void setDayOfWeek(AttendanceDTO dto, LocalDate date) {
		dto.setDayOfWeek(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.KOREAN));
	}

	// 월별 + 결근 + 주말 처리
	public List<AttendanceDTO> getMonthlyWithAbsent(int empId, String yearMonth) {

		List<AttendanceDTO> dbList = dao.getMonthlyAttendance(empId, yearMonth);
		List<AttendanceDTO> result = new ArrayList<>();

		YearMonth ym = YearMonth.parse(yearMonth);
		int lastDay = ym.lengthOfMonth();
		LocalDate today = LocalDate.now();

		try (Connection conn = DatabaseConnection.getConnection()) {

			for (int day = 1; day <= lastDay; day++) {

				LocalDate date = ym.atDay(day);

				if (date.isAfter(today))
					break;

				DayOfWeek dow = date.getDayOfWeek();

				AttendanceDTO found = null;

				for (AttendanceDTO dto : dbList) {
					if (dto.getWorkDate().toLocalDate().equals(date)) {
						found = dto;
						break;
					}
				}

				// 🔥 여기서 공휴일 체크
				boolean isHoliday = HolidayDAO.existsByDate(date, conn);

				boolean isWeekend = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);

				// 주말 + 공휴일 동일 처리
				if (isWeekend || isHoliday) {
					if (found != null) {
						applyNoCheckoutStatus(found);
						setDayOfWeek(found, date);
						result.add(found);
					}
					continue;
				}

				// 평일 처리
				if (found == null) {

					AttendanceDTO dto = new AttendanceDTO();
					dto.setWorkDate(java.sql.Date.valueOf(date));

					boolean isLeave = leaveService.existsLeaveByDate(empId, date);

					if (isLeave) {
						dto.setStatus("휴가");
					} else {
						dto.setStatus("-");
					}

					setDayOfWeek(dto, date);
					result.add(dto);

				} else {
					applyNoCheckoutStatus(found);
					setDayOfWeek(found, date);
					result.add(found);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;

	}

	// 월별 통계
	public AttendanceSummaryDTO getMonthlySummary(List<AttendanceDTO> list) {

		int workDays = 0;
		int lateCount = 0;
		int attendCount = 0;
		int absentCount = 0;
		int noCheckoutCount = 0;
		double totalHours = 0;

		for (AttendanceDTO dto : list) {

			String status = dto.getStatus();

			// 결근
			if ("결근".equals(status)) {
				absentCount++;
				continue;
			}

			// 출근 횟수
			attendCount++;

			// 지각
			if ("지각".equals(status)) {
				lateCount++;
			}

			// 퇴근 미처리
			if ("퇴근미처리".equals(status)) {
				noCheckoutCount++;
			}

			// 정상 근무 (퇴근 완료)
			if (dto.getCheckOut() != null) {
				workDays++;
				totalHours += dto.getWorkHours();
			}
		}

		AttendanceSummaryDTO summary = new AttendanceSummaryDTO();
		summary.setWorkDays(workDays);
		summary.setTotalHours(totalHours);
		summary.setAttendCount(attendCount);
		summary.setLateCount(lateCount);
		summary.setAbsentCount(absentCount);
		summary.setNoCheckoutCount(noCheckoutCount);

		return summary;
	}

}