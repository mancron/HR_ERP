package com.hrms.att.service;

import com.hrms.att.dao.AttendanceDAO;
import com.hrms.att.dao.LeaveDAO;
import com.hrms.att.dto.AttendanceDTO;
import com.hrms.att.dto.AttIssueDTO;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

public class AttendanceIssueService {

	private AttendanceDAO attendanceDAO = new AttendanceDAO();
	private LeaveDAO leaveDAO = new LeaveDAO();

	public List<AttIssueDTO> getIssues(int empId, int year, int month) {

		List<AttIssueDTO> result = new ArrayList<>();

		String ym = String.format("%04d-%02d", year, month);

		List<AttendanceDTO> attList = attendanceDAO.getMonthlyAttendance(empId, ym);

		Map<LocalDate, AttendanceDTO> attMap = attList.stream()
				.collect(Collectors.toMap(a -> a.getWorkDate().toLocalDate(), a -> a));

		YearMonth yearMonth = YearMonth.of(year, month);
		LocalDate today = LocalDate.now();

		for (int d = 1; d <= yearMonth.lengthOfMonth(); d++) {

			LocalDate date = yearMonth.atDay(d);

			// 미래 날짜 제외
			if (year == today.getYear() && month == today.getMonthValue()) {
				if (date.isAfter(today))
					break;
			}

			if (isWeekend(date))
				continue;

			AttendanceDTO att = attMap.get(date);

			AttIssueDTO dto = new AttIssueDTO();
			dto.setDate(date.toString());

			// 🔥 1. 결근 후보
			if (att == null) {

				if (!leaveDAO.existsByDate(empId, date)) {
					dto.setType("결근 후보");
					result.add(dto);
				}

				continue;
			}

			// 👉 이미 결근으로 저장된 경우
			if ("결근".equals(att.getStatus())) {
				dto.setType("결근");
				result.add(dto);
				continue;
			}

			// 🔥 2. 미퇴근
			if (att.getCheckIn() != null && att.getCheckOut() == null) {
				dto.setType("미퇴근");
				dto.setCheckIn(String.valueOf(att.getCheckIn()));
				result.add(dto);
				continue;
			}

			// 🔥 3. 지각
			if (att.getCheckIn() != null && att.getCheckOut() != null
					&& att.getCheckIn().toLocalTime().isAfter(LocalTime.of(9, 0))) {

				dto.setType("지각");
				dto.setCheckIn(String.valueOf(att.getCheckIn()));
				dto.setCheckOut(String.valueOf(att.getCheckOut()));
				result.add(dto);
			}
		}

		return result;
	}

	private boolean isWeekend(LocalDate date) {
		return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
	}
}