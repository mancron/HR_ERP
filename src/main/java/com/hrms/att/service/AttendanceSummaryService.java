package com.hrms.att.service;

import com.hrms.att.dao.AttendanceDAO;
import com.hrms.att.dao.LeaveDAO;
import com.hrms.att.dao.OvertimeDAO;
import com.hrms.att.dto.AttendanceDTO;
import com.hrms.att.dto.LeaveDTO;
import com.hrms.att.dto.OvertimeDTO;
import com.hrms.emp.dao.EmpDAO;
import com.hrms.emp.dto.EmpDTO;
import com.hrms.common.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

public class AttendanceSummaryService {

	private AttendanceDAO attendanceDAO = new AttendanceDAO();
	private LeaveDAO leaveDAO = new LeaveDAO();
	private OvertimeDAO overtimeDAO = new OvertimeDAO();
	private EmpDAO empDAO = new EmpDAO();

	public List<Map<String, Object>> getSummaryList(String keyword, String dept, int positionId, String status,
			int year, int month, int offset, int size) {

		List<Map<String, Object>> result = new ArrayList<>();

		Connection conn = null;

		int deptId = 0;

		if (dept != null && !dept.isEmpty()) {
			deptId = getDeptIdByName(dept);
		}

		try {
			conn = DatabaseConnection.getConnection();

			// 1️ 직원 조회 
			Vector<EmpDTO> empList = empDAO.searchEmpListPaging(conn, keyword, deptId, positionId, status, offset,
					size);

			for (EmpDTO emp : empList) {

				int empId = emp.getEmp_id();

				// 2️ 근태 데이터 조회
				List<AttendanceDTO> attList = attendanceDAO.getMonthlyAttendance(empId,
						String.format("%04d-%02d", year, month));

				List<LeaveDTO> leaveList = leaveDAO.getLeaveListByMonth(empId, year, month);

				List<OvertimeDTO> otList = overtimeDAO.getMyListByMonth(empId, year, month);

				// 3️⃣ 집계 계산
				Map<String, Object> summary = calculate(emp, attList, leaveList, otList, year, month);

				result.add(summary);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (conn != null)
					conn.close();
			} catch (Exception ignore) {
			}
		}

		return result;
	}

	private Map<String, Object> calculate(EmpDTO emp, List<AttendanceDTO> attList, List<LeaveDTO> leaveList,
			List<OvertimeDTO> otList, int year, int month) {

		Map<String, Object> map = new HashMap<>();

		Map<LocalDate, AttendanceDTO> attMap = attList.stream()
				.collect(Collectors.toMap(a -> a.getWorkDate().toLocalDate(), a -> a));

		int workDays = 0; // 정상 출근
		int late = 0; // 지각
		int absent = 0; // 결근
		int absentCandidate = 0; // 결근 후보
		int noCheckout = 0; // 퇴근 미처리

		double leaveDays = 0;

		YearMonth ym = YearMonth.of(year, month);
		LocalDate today = LocalDate.now();

		for (int day = 1; day <= ym.lengthOfMonth(); day++) {

			LocalDate date = ym.atDay(day);

// 미래 날짜 제외
			if (year == today.getYear() && month == today.getMonthValue()) {
				if (date.isAfter(today))
					break;
			}

// 주말 제외
			if (isWeekend(date))
				continue;

			AttendanceDTO att = attMap.get(date);

// 1. 데이터 없음 → 결근 후보
			if (att == null) {

				if (!leaveDAO.existsByDate(emp.getEmp_id(), date)) {
					absentCandidate++;
				}

				continue;
			}

			Time checkIn = att.getCheckIn();
			Time checkOut = att.getCheckOut();

			if ("결근".equals(att.getStatus())) {
				absent++;
				continue;
			}

// 2. 퇴근 미처리
			if (checkIn != null && checkOut == null) {
				noCheckout++;
				continue;
			}

// 3. 정상 / 지각
			if (checkIn != null && checkOut != null) {

				if (checkIn.toLocalTime().isAfter(LocalTime.of(9, 0))) {
					late++;
				} else {
					workDays++;
				}

				continue;
			}

// 4. checkIn 자체가 없는 경우 → 결근
			if (!leaveDAO.existsByDate(emp.getEmp_id(), date)) {
				absent++;
			}
		}

// 휴가 집계
		for (LeaveDTO leave : leaveList) {
			if ("승인".equals(leave.getStatus())) {
				leaveDays += leave.getDays();
			}
		}

// 결과 세팅
		map.put("empId", emp.getEmp_id());
		map.put("empName", emp.getEmp_name());
		map.put("deptName", emp.getDept_name());
		map.put("position", emp.getPosition_name());

		map.put("workDays", workDays);
		map.put("lateCount", late);
		map.put("absentCount", absent);
		map.put("absentCandidateCount", absentCandidate);
		map.put("noCheckoutCount", noCheckout);

		map.put("leaveDays", leaveDays);

		return map;
	}

	// 페이징용 카운트
	public int getSummaryCount(String keyword, String dept, int positionId, String status) {

		int deptId = 0;

		if (dept != null && !dept.isEmpty()) {
			deptId = getDeptIdByName(dept);
		}

		try (Connection conn = DatabaseConnection.getConnection()) {
			return empDAO.getEmpCount(conn, keyword, deptId, positionId, status);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private boolean isWeekend(LocalDate date) {
		return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
	}

	public List<String> getDeptList() {
		return empDAO.getDeptList();
	}

	private int getDeptIdByName(String deptName) {

		String sql = "SELECT dept_id FROM department WHERE dept_name = ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, deptName);
			ResultSet rs = pstmt.executeQuery();

			if (rs.next()) {
				return rs.getInt("dept_id");
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return 0;
	}
}