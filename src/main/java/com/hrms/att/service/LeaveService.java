package com.hrms.att.service;

import com.hrms.att.dao.LeaveDAO;
import com.hrms.att.dto.AnnualLeaveDTO;
import com.hrms.att.dto.LeaveDTO;
import com.hrms.att.dto.RequestDTO;
import com.hrms.common.db.DatabaseConnection;
import com.hrms.common.util.NotificationUtil;
import com.hrms.emp.dao.EmpDAO;
import com.hrms.org.dao.DeptDAO;
import com.hrms.org.dto.DeptDTO;
import com.hrms.sys.dao.HolidayDAO;
import com.hrms.sys.dao.NotificationDAO;

import java.sql.Connection;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LeaveService {

	private LeaveDAO leaveDAO = new LeaveDAO();
	private HolidayDAO holidayDAO = new HolidayDAO();
	private EmpDAO empDAO = new EmpDAO();
	private DeptDAO deptDAO = new DeptDAO();
	private NotificationDAO notificationDAO = new NotificationDAO();

	// 승인자 찾기
	public int findApprover(int empId) {

		int deptId = empDAO.getDeptIdByEmpId(empId);

		while (deptId != 0) {

			DeptDTO dept = deptDAO.getDeptById(deptId);

			if (dept == null) {
				throw new RuntimeException("부서 없음");
			}

			int managerId = dept.getManager_id();
			
			// ⭐ 팀장 있으면 바로 반환
			if (managerId != 0 && managerId != empId) {
	            return managerId;
	        }

			// ⭐ 없으면 상위 부서로 이동
			deptId = dept.getParent_dept_id();
		}

		throw new RuntimeException("승인자 없음");
	}

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

		// 3. 반차 검증
		if ("반차".equals(dto.getLeaveType())) {
			if (!startDate.equals(endDate)) {
				return "invalid_half";
			}
		}

		// 4. 잔여 연차 체크
		double remainDays = leaveDAO.getRemainDays(empId);
		if (days > remainDays) {
			return "not_enough";
		}

		// 5. 기간 중복 체크
		if (leaveDAO.isOverlapping(empId, startDate, endDate)) {
			return "overlap";
		}

		Connection conn = null;

		try {
			conn = DatabaseConnection.getConnection();
			conn.setAutoCommit(false);

			// 🔥 6. 승인자 찾기
			int approverId = findApprover(empId);
			dto.setApproverId(approverId);

			// 🔥 7. 휴가 저장
			boolean result = leaveDAO.insertLeave(dto);
			if (!result) {
				throw new Exception("휴가 저장 실패");
			}

			// 🔥 8. 신청자 이름
			String empName = empDAO.getEmployeeById(empId).getEmp_name();

			// 🔥 9. 기간 문자열
			String period = dto.getStartDate() + " ~ " + dto.getEndDate();

			conn.commit();

			NotificationUtil.sendLeavePending(approverId, empName, period, dto.getLeaveId());

			return "success";

		} catch (Exception e) {

			try {
				if (conn != null)
					conn.rollback();
			} catch (Exception ex) {
			}

			e.printStackTrace();
			return "fail";

		} finally {
			try {
				if (conn != null)
					conn.close();
			} catch (Exception e) {
			}
		}
	}

	public AnnualLeaveDTO getAnnualLeave(int empId) {
		return leaveDAO.getAnnualLeave(empId);
	}

	public double calculateDays(LocalDate start, LocalDate end, String type) {

		// 반차
		if ("반차".equals(type)) {
			return 0.5;
		}

		double days = 0;
		LocalDate date = start;

		try (Connection conn = DatabaseConnection.getConnection()) {

			while (!date.isAfter(end)) {

				boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY
						|| date.getDayOfWeek() == DayOfWeek.SUNDAY;

				boolean isHoliday = holidayDAO.existsByDate(date, conn);

				if (!isWeekend && !isHoliday) {
					days++;
				}

				date = date.plusDays(1);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return days;
	}

	// 휴가 리스트 출력
	public List<LeaveDTO> getLeaveList(int empId) {
		return leaveDAO.getLeaveList(empId);
	}

	// 월별 휴가 리스트 출력
	public List<RequestDTO> getLeaveListByMonth(int empId, int year, int month) {
		List<LeaveDTO> list = leaveDAO.getLeaveListByMonth(empId, year, month);

		List<RequestDTO> result = new ArrayList<RequestDTO>();

		for (LeaveDTO dto : list) {

			RequestDTO r = new RequestDTO();

			r.setId(dto.getLeaveId());
			r.setDate(dto.getStartDate() + " ~ " + dto.getEndDate());
			if ("반차".equals(dto.getLeaveType())) {
				r.setType(dto.getLeaveType() + " (" + dto.getHalfType() + ")");
			} else {
				r.setType(dto.getLeaveType());
			}
			r.setStatus(dto.getStatus());
			r.setReason(dto.getReason());

			result.add(r);
		}

		return result;
	}

	// 신청 취소
	public boolean cancelLeave(int leaveId, int empId) {
		return leaveDAO.cancelLeave(leaveId, empId);
	}

	// 필터 + 정렬 포함 (신규 기능)
	public List<LeaveDTO> getPendingLeaves(String dept, String sort, String startDate, String endDate, int approverId, int offset, int size) {
		return leaveDAO.getPendingLeaves(dept, sort, startDate, endDate, approverId, offset, size);
	}
	
	public int getPendingLeavesCount(String dept, String startDate, String endDate, int approverId) {
		return leaveDAO.getPendingLeavesCount(dept, startDate, endDate, approverId);
	}

	// 부서 목록 조회 (드롭다운용)
	public List<String> getPendingDeptList(int approverId) {
		return leaveDAO.getPendingDeptList(approverId);
	}

	// 휴가 승인 반려 처리
	// 휴가 데이터 수정 + 잔여 연차 값 업데이트 트랜잭션 처리
	public boolean approveLeave(int leaveId, int approverId, String status, String reason) {

		Connection conn = null;

		try {
			conn = DatabaseConnection.getConnection();
			conn.setAutoCommit(false);

			// 🔥 1. 휴가 정보 조회
			LeaveDTO leave = leaveDAO.getLeaveById(leaveId);
			if (leave == null) {
				throw new Exception("휴가 정보 없음");
			}

			int requesterEmpId = leave.getEmpId();

			// 🔥 2. 자기 승인 방지
			if (requesterEmpId == approverId) {
				throw new Exception("자신의 휴가는 승인할 수 없습니다.");
			}

			// 🔥 3. 승인 권한 체크 (중요)
			if (leave.getApproverId() != approverId) {
				throw new Exception("승인 권한이 없습니다.");
			}

			// 🔥 4. 이미 처리된 건 막기
			if (!"대기".equals(leave.getStatus())) {
				throw new Exception("이미 처리된 요청입니다.");
			}

			// 🔥 5. 데이터 미리 확보 (commit 전에!)
			String period = leave.getStartDate() + " ~ " + leave.getEndDate();
			double days = calculateDays(leave.getStartDate().toLocalDate(), leave.getEndDate().toLocalDate(),
					leave.getLeaveType());

			String leaveType = leave.getLeaveType();

			// 🔥 6. 승인 시 연차 차감
			if ("승인".equals(status) && ("연차".equals(leaveType) || "반차".equals(leaveType))) {

				double remain = leaveDAO.getRemainDays(requesterEmpId);
				if (remain < days) {
					throw new Exception("연차 부족");
				}

				leaveDAO.updateAnnualLeave(conn, requesterEmpId, days);
			}

			// 🔥 7. 상태 변경
			boolean result = leaveDAO.updateLeaveStatus(conn, leaveId, approverId, status, reason);
			if (!result) {
				throw new Exception("상태 변경 실패");
			}

			conn.commit(); // ✅ 먼저 커밋

			// 🔔 8. 알림 (커밋 이후!)
			if ("승인".equals(status)) {
				NotificationUtil.sendLeaveApproved(requesterEmpId, period, days, leaveId);
			} else if ("반려".equals(status)) {
				NotificationUtil.sendLeaveRejected(requesterEmpId, period, reason, leaveId);
			}

			return true;

		} catch (Exception e) {

			try {
				if (conn != null)
					conn.rollback();
			} catch (Exception ex) {
			}

			e.printStackTrace();
			return false;

		} finally {
			try {
				if (conn != null) {
					conn.setAutoCommit(true);
					conn.close();
				}
			} catch (Exception e) {
			}
		}
	}

	// 휴가 상세 데이터
	public LeaveDTO getLeaveDetail(int leaveId) {
		return leaveDAO.getLeaveById(leaveId);
	}

	// 특정 날짜 휴가 존재 여부 확인
	public boolean existsLeaveByDate(int empId, LocalDate date) {
		return leaveDAO.existsByDate(empId, date);
	}

}
