package com.hrms.common.util;

import com.hrms.common.db.DatabaseConnection;
import com.hrms.sys.dao.NotificationDAO;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * ===================================================== 알림 발송 공통 유틸리티
 * ===================================================== 사용 방법: 1. 반드시 메인 트랜잭션
 * conn.commit() 완료 후 호출 2. static 메서드이므로 인스턴스 생성 불필요 3. 알림 실패는 로그만 남기고 무시됨 (메인
 * 로직에 영향 없음)
 *
 * 예시: conn.commit(); // 메인 트랜잭션 커밋 NotificationUtil.sendPasswordReset(empId,
 * accountId); // 알림 발송 =====================================================
 */

public class NotificationUtil {

	private static final NotificationDAO notificationDAO = new NotificationDAO();

	// ── 알림 발송 공통 내부 메서드 ──
	private static void send(int empId, String notiType, String refTable, Integer refId, String message) {
		// 트랜잭션 외부 격리 — 알림 실패가 메인 로직에 영향 없음
		Connection conn = null;
		try {
			conn = DatabaseConnection.getConnection();
			notificationDAO.insert(empId, notiType, refTable, refId, message, conn);
		} catch (Exception e) {
			// 알림 실패는 로그만 남기고 무시
			e.printStackTrace();
		} finally {
			if (conn != null)
				try {
					conn.close();
				} catch (SQLException e) {
				}
		}
	}

	// ══════════════════════════════════════
	// 휴가 관련
	// ══════════════════════════════════════

	/** 휴가 신청 → 승인권자에게 알림 */
	public static void sendLeavePending(int approverEmpId, String requesterName, String period, int leaveId) {
		String msg = requesterName + " 님이 연차 휴가를 신청했습니다. (" + period + ")";
		send(approverEmpId, "LEAVE_PENDING", "leave_request", leaveId, msg);
	}

	/** 휴가 승인 → 신청자에게 알림 */
	public static void sendLeaveApproved(int requesterEmpId, String period, double days, int leaveId) {
		String msg = "휴가 신청이 승인되었습니다. (" + period + ", " + days + "일)";
		send(requesterEmpId, "LEAVE_APPROVED", "leave_request", leaveId, msg);
	}

	/** 휴가 반려 → 신청자에게 알림 */
	public static void sendLeaveRejected(int requesterEmpId, String period, String reason, int leaveId) {
		String msg = "휴가 신청이 반려되었습니다. (" + period + ")" + (reason != null ? " 사유: " + reason : "");
		send(requesterEmpId, "LEAVE_REJECTED", "leave_request", leaveId, msg);
	}

	// ══════════════════════════════════════
	// 초과근무 관련
	// ══════════════════════════════════════

	/** 초과근무 신청 → 승인권자에게 알림 */
	public static void sendOvertimePending(int approverEmpId, String requesterName, String dateTime, int otId) {
		String msg = requesterName + " 님이 초과근무를 신청했습니다. (" + dateTime + ")";
		send(approverEmpId, "OVERTIME_PENDING", "overtime_request", otId, msg);
	}

	/** 초과근무 승인 → 신청자에게 알림 */
	public static void sendOvertimeApproved(int requesterEmpId, String dateTime, int otId) {
		String msg = "초과근무 신청이 승인되었습니다. (" + dateTime + ")";
		send(requesterEmpId, "OVERTIME_APPROVED", "overtime_request", otId, msg);
	}

	/** 초과근무 반려 → 신청자에게 알림 */
	public static void sendOvertimeRejected(int requesterEmpId, String dateTime, String reason, int otId) {
		String msg = "초과근무 신청이 반려되었습니다. (" + dateTime + ")" + (reason != null ? " 사유: " + reason : "");
		send(requesterEmpId, "OVERTIME_REJECTED", "overtime_request", otId, msg);
	}

	// ══════════════════════════════════════
	// 급여 관련
	// ══════════════════════════════════════

	/** 급여 지급 완료 → 해당 직원에게 알림 */
	public static void sendSalaryPaid(int empId, int year, int month, int netSalary, int salaryId) {
		String msg = year + "년 " + month + "월 급여가 지급되었습니다. " + "(실수령액: " + String.format("%,d", netSalary) + "원)";
		send(empId, "SALARY_PAID", "salary", salaryId, msg);
	}

	// ══════════════════════════════════════
	// 평가 관련
	// ══════════════════════════════════════

	/** 인사평가 최종확정 → 대상 직원에게 알림 */
	public static void sendEvalConfirmed(int empId, int year, String period, String grade, int evalId) {
		String msg = year + "년 " + period + " 인사평가가 최종확정되었습니다. (등급: " + grade + ")";
		send(empId, "EVAL_CONFIRMED", "evaluation", evalId, msg);
	}

	// ══════════════════════════════════════
	// 계정/시스템 관련
	// ══════════════════════════════════════

	/** 계정 잠금 → 관리자들에게 알림 (복수 발송) */
	// 사용
	public static void sendAccountLocked(int[] adminEmpIds, String lockedUserName) {
		String msg = lockedUserName + " 님의 계정이 로그인 실패로 잠겼습니다.";
		for (int adminId : adminEmpIds) {
			send(adminId, "ACCOUNT_LOCKED", "account", null, msg);
		}
	}

	/** 비밀번호 초기화 → 해당 직원에게 알림 */
	// 사용
	public static void sendPasswordReset(int empId, int accountId) {
		String msg = "관리자에 의해 비밀번호가 초기화되었습니다. 로그인 후 즉시 변경해주세요.";
		send(empId, "PASSWORD_RESET", "account", accountId, msg);
	}

	// ══════════════════════════════════════
	// 연차 관련
	// ══════════════════════════════════════

	/** 연차 조정 → 해당 직원에게 알림 */
	public static void sendAnnualAdjusted(int empId, double adjustDays) {

		String msg = "연차가 " + (adjustDays > 0 ? "+" : "") + adjustDays + "일 조정되었습니다.";

		send(empId, "ANNUAL_ADJUSTED", "annual_leave", null, msg);
	}
	
	// ══════════════════════════════════════
	// 근태 관련
	// ══════════════════════════════════════

	/** 결근 처리 → 직원에게 알림 */
	public static void sendAttendanceAbsent(int empId, String date) {
	    String msg = date + " 근태가 결근으로 처리되었습니다.";
	    send(empId, "ATT_ABSENT", "attendance", null, msg);
	}

	/** 퇴근 수정 → 직원에게 알림 */
	public static void sendAttendanceCheckoutUpdated(int empId, String date) {
	    String msg = date + " 퇴근 시간이 수정되었습니다.";
	    send(empId, "ATT_CHECKOUT_UPDATED", "attendance", null, msg);
	}

	/** 근태 전체 수정 → 직원에게 알림 */
	public static void sendAttendanceUpdated(int empId, String date) {
	    String msg = date + " 근태 정보가 수정되었습니다.";
	    send(empId, "ATT_UPDATED", "attendance", null, msg);
	}


	// ══════════════════════════════════════
	// 휴직/복직/퇴직 결재 관련
	// ══════════════════════════════════════
	
	 /** 신청 접수 → 부서장에게 알림 */
	 public static void sendApprovalPending(int deptManagerEmpId,
	         String requesterName, String applyType, int requestId) {
	     String msg = requesterName + " 님이 " + applyType + " 신청을 했습니다.";
	     send(deptManagerEmpId, "APPROVAL_PENDING", "leave_of_absence_request", requestId, msg);
	 }
	
	 /** 부서장 승인 → HR담당자에게 알림 */
	 public static void sendApprovalDeptApproved(int hrManagerEmpId,
	         String requesterName, String applyType, int requestId) {
	     String msg = requesterName + " 님의 " + applyType + " 신청이 부서장 승인되었습니다.";
	     send(hrManagerEmpId, "APPROVAL_DEPT_APPROVED", "leave_of_absence_request", requestId, msg);
	 }
	
	 /** HR담당자 승인 → 최종승인자에게 알림 */
	 public static void sendApprovalHrApproved(int presidentEmpId,
	         String requesterName, String applyType, int requestId) {
	     String msg = requesterName + " 님의 " + applyType + " 신청이 HR담당자 승인되었습니다.";
	     send(presidentEmpId, "APPROVAL_HR_APPROVED", "leave_of_absence_request", requestId, msg);
	 }
	
	 /** 최종 승인 → 신청자에게 알림 */
	 public static void sendApprovalFinalApproved(int requesterEmpId,
	         String applyType, int requestId) {
	     String msg = applyType + " 신청이 최종 승인되었습니다.";
	     send(requesterEmpId, "APPROVAL_FINAL", "leave_of_absence_request", requestId, msg);
	 }
	 
	 /** 최종승인자 즉시처리 확인 → HR담당자에게 알림 */
	 public static void sendApprovalPresidentSelf(int hrManagerEmpId,
	         String requesterName, String applyType, int requestId) {
	     String msg = requesterName + " 님의 " + applyType + " 신청이 최종 승인 처리되었습니다.";
	     send(hrManagerEmpId, "APPROVAL_FINAL", "leave_of_absence_request", requestId, msg);
	 }
	
	 /** 반려 → 신청자에게 알림 */
	 public static void sendApprovalRejected(int requesterEmpId,
	         String applyType, String rejectReason, int requestId) {
	     String msg = applyType + " 신청이 반려되었습니다." +
	                  (rejectReason != null && !rejectReason.isEmpty() ? " 사유: " + rejectReason : "");
	     send(requesterEmpId, "APPROVAL_REJECTED", "leave_of_absence_request", requestId, msg);
	 }
}