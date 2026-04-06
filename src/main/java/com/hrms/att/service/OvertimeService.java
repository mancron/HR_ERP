package com.hrms.att.service;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import com.hrms.att.dao.OvertimeDAO;
import com.hrms.att.dto.OvertimeDTO;
import com.hrms.att.dto.RequestDTO;
import com.hrms.common.db.DatabaseConnection;
import com.hrms.common.util.NotificationUtil;
import com.hrms.emp.dao.EmpDAO;
import com.hrms.org.dao.DeptDAO;
import com.hrms.org.dto.DeptDTO;
import com.hrms.sys.dao.NotificationDAO;

public class OvertimeService {

	private DeptDAO deptDAO = new DeptDAO();
	private EmpDAO empDAO = new EmpDAO();
	private OvertimeDAO overtimeDAO = new OvertimeDAO();
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

			// 핵심: 자기 자신이면 스킵
			if (managerId != 0 && managerId != empId) {
				return managerId;
			}

			// 상위 부서로 이동
			deptId = dept.getParent_dept_id();
		}

		// 여기까지 오면 구조가 잘못된 것
		throw new RuntimeException("승인자 없음 (조직 구조 확인 필요)");
	}

	// 신청 + 알림 (DTO 기반)
	public void applyOvertime(OvertimeDTO dto) {

		Connection conn = null;

		try {
			conn = DatabaseConnection.getConnection();
			conn.setAutoCommit(false);

			// 1. 승인자 찾기
			int approverId = findApprover(dto.getEmpId());
			dto.setApproverId(approverId);
			
			if (overtimeDAO.existsValidOvertime(dto.getEmpId(), dto.getOtDate())) {
			    throw new RuntimeException("이미 해당 날짜에 처리 중이거나 승인된 초과근무가 있습니다.");
			}
			
			// 2. 저장
			overtimeDAO.insertOvertime(conn, dto);

			// 3. 알림에 필요한 데이터 미리 확보
			String empName = empDAO.getEmployeeById(dto.getEmpId()).getEmp_name();

			String date = dto.getOtDate().toString().substring(5).replace("-", "/");
			String start = dto.getStartTime().toString().substring(0, 5);
			String end = dto.getEndTime().toString().substring(0, 5);

			String dateTime = date + " " + start + "~" + end;

			conn.commit();

			// 4. NotificationUtil 사용
			NotificationUtil.sendOvertimePending(approverId, empName, dateTime, dto.getOtId() // PK 있으면, 없으면 null 가능
			);

		} catch (Exception e) {

			try {
				if (conn != null)
					conn.rollback();
			} catch (Exception ex) {
			}

			throw new RuntimeException(e);

		} finally {
			try {
				if (conn != null)
					conn.close();
			} catch (Exception e) {
			}
		}
	}

	// 초과근무 신청 리스트
	public List<RequestDTO> getMyOvertimeList(int empId, int year, int month) {

		List<OvertimeDTO> list = overtimeDAO.getMyListByMonth(empId, year, month);

		List<RequestDTO> result = new ArrayList<>();

		for (OvertimeDTO dto : list) {

			RequestDTO r = new RequestDTO();

			r.setId(dto.getOtId());
			r.setDate(dto.getOtDate() + " " + dto.getStartTime().toString().substring(0, 5) + " ~ "
					+ dto.getEndTime().toString().substring(0, 5));
			r.setType("초과근무");
			r.setStatus(dto.getStatus());
			r.setReason(dto.getReason());

			result.add(r);
		}

		return result;
	}

	// 초과근무 상세 정보
	public OvertimeDTO getOvertimeDetail(int id) {
		return overtimeDAO.findById(id);
	}

	public boolean cancelOvertime(int id, int empId) {
		return overtimeDAO.cancel(id, empId);
	}

	//승인 + 반려 처리
	public boolean approveOvertime(int otId, int approverId, String status, String reason) {

		Connection conn = null;

		try {
			conn = DatabaseConnection.getConnection();
			conn.setAutoCommit(false);

			// 1. 데이터 조회
			OvertimeDTO ot = overtimeDAO.findById(conn, otId);

			if (ot == null) {
				throw new RuntimeException("데이터 없음");
			}

			int requesterId = ot.getEmpId();

			// 2. 자기 승인 방지
			if (requesterId == approverId) {
				throw new RuntimeException("자신의 신청은 승인할 수 없습니다.");
			}

			// 3. 승인자 null 방어 + 권한 체크
			Integer approver = ot.getApproverId();
			if (approver == null) {
				throw new RuntimeException("승인자가 없는 데이터입니다.");
			}
			if (approver != approverId) {
				throw new RuntimeException("승인 권한 없음");
			}

			// 4. 상태 체크
			if (!"대기".equals(ot.getStatus())) {
				throw new RuntimeException("이미 처리됨");
			}

			// 5. 상태 변경
			boolean result = overtimeDAO.updateStatus(conn, otId, status, reason);
			if (!result) {
				throw new RuntimeException("상태 변경 실패");
			}

			// 6. 알림용 데이터 미리 확보
			String date = ot.getOtDate().toString().substring(5).replace("-", "/");
			String start = ot.getStartTime().toString().substring(0, 5);
			String end = ot.getEndTime().toString().substring(0, 5);
			String dateTime = date + " " + start + "~" + end;

			conn.commit();

			// 7. NotificationUtil 사용
			if ("승인".equals(status)) {

				NotificationUtil.sendOvertimeApproved(requesterId, dateTime, otId);

			} else if ("반려".equals(status)) {

				NotificationUtil.sendOvertimeRejected(requesterId, dateTime, reason, otId);

			} else {
				throw new RuntimeException("잘못된 상태값");
			}

			return true;

		} catch (Exception e) {

			try {
				if (conn != null)
					conn.rollback();
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			throw new RuntimeException(e);

		} finally {
			try {
				if (conn != null)
					conn.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public List<OvertimeDTO> getPendingOvertimes(String dept, String sort, String startDate, String endDate,
			int approverId) {

		try (Connection conn = DatabaseConnection.getConnection()) {
			return overtimeDAO.getPendingList(conn, dept, sort, startDate, endDate, approverId);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public List<String> getPendingDeptList() {

		try (Connection conn = DatabaseConnection.getConnection()) {
			return overtimeDAO.getPendingDeptList(conn);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
