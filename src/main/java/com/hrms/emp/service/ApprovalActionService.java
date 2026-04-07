package com.hrms.emp.service;

import java.sql.Connection;
import com.hrms.common.db.DatabaseConnection;
import com.hrms.emp.dao.ApprovalActionDAO;
import com.hrms.emp.dto.HistoryDTO;

public class ApprovalActionService {

	private ApprovalActionDAO dao = new ApprovalActionDAO();

	public boolean isDeptManager(int empId) {
		Connection con = null;
		try {
			con = DatabaseConnection.getConnection();
			return dao.isDeptManager(con, empId);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				if (con != null)
					con.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public String approve(String type, int requestId, int loginEmpId, boolean isHrManager, boolean isDeptManager,
			boolean isPresident) {
		Connection con = null;
		try {
			con = DatabaseConnection.getConnection();
			con.setAutoCommit(false);

			String currentStatus = dao.getStatus(con, type, requestId);
			if (currentStatus == null)
				return "신청 정보를 찾을 수 없습니다.";

			int reqEmpId = dao.getEmpId(con, type, requestId);
			boolean isHrDept = dao.isHrDept(con, type, requestId);
			boolean isReqPresident = dao.isPresident(con, type, requestId); // 신청자가 최종승인자인지
			boolean isSelf = (reqEmpId == loginEmpId); // 본인 신청 여부

			int result = 0;

			// ── 최종승인자 신청 흐름: 대기 → HR담당자 승인(확정) ──
			if (isReqPresident) {
			    if ("대기".equals(currentStatus) && isHrManager && !isSelf) {
			        result = dao.approveHrManagerForPresident(con, type, requestId, loginEmpId);
			        if (result > 0)
			            applyFinalApproval(con, type, requestId, loginEmpId);
			    } else {
			        con.rollback();
			        return "현재 상태에서 승인할 수 없습니다.";
			    }

			// ── 인사팀 흐름 ──
			} else if (isHrDept) {
				if ("대기".equals(currentStatus)) {
					// 부서장 자가승인 또는 타인 부서장 승인
					boolean isSelfDeptMgr = dao.isSelfDeptManager(con, type, requestId, loginEmpId);
					if (isSelf && isSelfDeptMgr) {
						// 인사팀 부서장: 본인 신청 자가승인 허용
						result = dao.approveDeptManager(con, type, requestId, loginEmpId);
					} else if (!isSelf && isDeptManager) {
						int deptManagerId = dao.getDeptManagerId(con, type, requestId);
						if (deptManagerId != loginEmpId) {
							con.rollback();
							return "해당 신청의 결재 권한이 없습니다.";
						}
						result = dao.approveDeptManager(con, type, requestId, loginEmpId);
					} else {
						con.rollback();
						return "현재 상태에서 승인할 수 없습니다.";
					}

				} else if ("부서장승인".equals(currentStatus) && isPresident && !isSelf) {
					result = dao.approvePresident(con, type, requestId, loginEmpId);
					if (result > 0)
						applyFinalApproval(con, type, requestId, loginEmpId);

				} else {
					con.rollback();
					return "현재 상태에서 승인할 수 없습니다.";
				}

			// ── 일반 부서 흐름 ──
			} else {
				if ("대기".equals(currentStatus)) {
					boolean isSelfDeptMgr = dao.isSelfDeptManager(con, type, requestId, loginEmpId);
					if (isSelf && isSelfDeptMgr) {
						// 일반 부서장: 본인 신청 자가승인 허용
						result = dao.approveDeptManager(con, type, requestId, loginEmpId);
					} else if (!isSelf && isDeptManager) {
						int deptManagerId = dao.getDeptManagerId(con, type, requestId);
						if (deptManagerId != loginEmpId) {
							con.rollback();
							return "해당 신청의 결재 권한이 없습니다.";
						}
						result = dao.approveDeptManager(con, type, requestId, loginEmpId);
					} else {
						con.rollback();
						return "현재 상태에서 승인할 수 없습니다.";
					}

				} else if ("부서장승인".equals(currentStatus) && isHrManager && !isSelf) {
					result = dao.approveHrManager(con, type, requestId, loginEmpId);

				} else if ("HR담당자승인".equals(currentStatus) && isPresident && !isSelf) {
					result = dao.approvePresident(con, type, requestId, loginEmpId);
					if (result > 0)
						applyFinalApproval(con, type, requestId, loginEmpId);

				} else {
					con.rollback();
					return "현재 상태에서 승인할 수 없습니다.";
				}
			}

			if (result > 0) {
				con.commit();
				return "승인이 완료되었습니다.";
			} else {
				con.rollback();
				return "승인 처리 중 오류가 발생했습니다.";
			}

		} catch (Exception e) {
			e.printStackTrace();
			try {
				if (con != null)
					con.rollback();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return "오류가 발생했습니다.";
		} finally {
			try {
				if (con != null)
					con.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public String reject(String type, int requestId, int loginEmpId, boolean isHrManager, boolean isDeptManager,
			boolean isPresident, String rejectReason) {
		Connection con = null;
		try {
			con = DatabaseConnection.getConnection();
			con.setAutoCommit(false);

			String currentStatus = dao.getStatus(con, type, requestId);
			if (currentStatus == null)
				return "신청 정보를 찾을 수 없습니다.";

			int reqEmpId = dao.getEmpId(con, type, requestId);
			boolean isHrDept = dao.isHrDept(con, type, requestId);
			boolean isReqPresident = dao.isPresident(con, type, requestId);
			boolean isSelf = (reqEmpId == loginEmpId);
			boolean canReject = false;

// 최종승인자 신청: HR담당자만 반려 가능
			if (isReqPresident) {
				if ("대기".equals(currentStatus) && isHrManager && !isSelf) {
					canReject = true;
				}

// 인사팀 흐름
			} else if (isHrDept) {
				if ("대기".equals(currentStatus) && isDeptManager) {
					boolean isSelfDeptMgr = dao.isSelfDeptManager(con, type, requestId, loginEmpId);
					canReject = isSelf ? isSelfDeptMgr : (dao.getDeptManagerId(con, type, requestId) == loginEmpId);
				} else if ("부서장승인".equals(currentStatus) && isPresident && !isSelf) {
					canReject = true;
				}

// 일반 부서 흐름
			} else {
				if ("대기".equals(currentStatus) && isDeptManager) {
					boolean isSelfDeptMgr = dao.isSelfDeptManager(con, type, requestId, loginEmpId);
					canReject = isSelf ? isSelfDeptMgr : (dao.getDeptManagerId(con, type, requestId) == loginEmpId);
				} else if ("부서장승인".equals(currentStatus) && isHrManager && !isSelf) {
					canReject = true;
				} else if ("HR담당자승인".equals(currentStatus) && isPresident && !isSelf) {
					canReject = true;
				}
			}

			if (!canReject) {
				con.rollback();
				return "현재 상태에서 반려할 수 없습니다.";
			}

			int result = dao.reject(con, type, requestId, rejectReason != null ? rejectReason : "");

			if (result > 0) {
				con.commit();
				return "반려 처리되었습니다.";
			} else {
				con.rollback();
				return "반려 처리 중 오류가 발생했습니다.";
			}

		} catch (Exception e) {
			e.printStackTrace();
			try {
				if (con != null)
					con.rollback();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return "오류가 발생했습니다.";
		} finally {
			try {
				if (con != null)
					con.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// 최종 승인 후 employee 테이블 실제 반영
	private void applyFinalApproval(Connection con, String type, int requestId, int loginEmpId) throws Exception {
	    int empId = dao.getEmpId(con, type, requestId);
	    int[] deptInfo = dao.getEmpDeptAndStatus(con, empId);
	    int currentDeptId = deptInfo[0];
	    int[] posAndRole = dao.getEmpPositionAndRole(con, empId);
	    int currentPositionId = posAndRole[0];
	    String currentRole = posAndRole[1] > 0 ? "부서장" : "일반";

	    HistoryDTO history = new HistoryDTO();
	    history.setEmp_id(empId);
	    history.setFrom_dept_id(currentDeptId);
	    history.setFrom_position_id(currentPositionId);
	    history.setFrom_role(currentRole);
	    history.setApproved_by(loginEmpId);

	    if ("leave".equals(type)) {
	        String leaveType = dao.getLeaveType(con, requestId);
	        String newStatus = "휴직".equals(leaveType) ? "휴직" : "재직";
	        String startDate = dao.getLeaveStartDate(con, requestId);
	        history.setChange_type(leaveType);
	        history.setChange_date(java.time.LocalDate.parse(startDate).atStartOfDay());
	        history.setReason(dao.getLeaveReason(con, requestId));
	        // leave는 to = from 그대로 (부서/직급 변동 없음)
	        history.setTo_dept_id(currentDeptId);
	        history.setTo_position_id(currentPositionId);
	        history.setTo_role(currentRole);

	        dao.insertPersonnelHistory(con, history);
	        dao.updateEmployeeStatus(con, empId, newStatus);

	    } else {
	        String resignDate = dao.getResignDate(con, requestId);
	        history.setChange_type("퇴직");
	        history.setChange_date(java.time.LocalDate.parse(resignDate).atStartOfDay());
	        history.setReason(dao.getResignReason(con, requestId));
	        // 퇴직 후 소속 없음
	        history.setTo_dept_id(0);
	        history.setTo_position_id(0);
	        history.setTo_role(null);

	        // ① 이력 먼저 (from 정보 살아있을 때)
	        dao.insertPersonnelHistory(con, history);
	        // ② 퇴직 처리
	        dao.updateEmployeeResign(con, empId, resignDate);
	        // ③ 부서/직급 NULL
	        dao.clearEmployeeDeptAndPosition(con, empId);
	        // ④ 부서장이었으면 department.manager_id NULL
	        dao.clearDeptManagerIfResign(con, empId);
	    }
	}
}