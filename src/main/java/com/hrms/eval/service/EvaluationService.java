package com.hrms.eval.service;

import com.hrms.eval.dao.EvaluationDAO;
import com.hrms.eval.dto.EvaluationDTO;
import com.hrms.eval.dto.EvaluationItemDTO;
import com.hrms.common.util.NotificationUtil;

import java.math.BigDecimal;
import java.util.*;

public class EvaluationService {

    private EvaluationDAO evalDao = new EvaluationDAO();

    // ── 기존 메서드 (시그니처 유지) ──────────────────────────

    public String getGradeColor(String grade) {
        if (grade == null) return "#94a3b8";
        switch (grade) {
            case "S": return "#ef4444";
            case "A": return "#f59e0b";
            case "B": return "#3b82f6";
            case "C": return "#22c55e";
            default:  return "#94a3b8";
        }
    }

    public Vector<Map<String, Object>> getEmployeeList() { return evalDao.getEmployeeList(); }

    public Vector<String> getEvaluationItemNames() { return evalDao.getEvaluationItemNames(); }

    public String calculateGrade(double avg) {
        if (avg >= 95) return "S"; if (avg >= 85) return "A";
        if (avg >= 75) return "B"; if (avg >= 60) return "C"; return "D";
    }

    public boolean submitEvaluation(EvaluationDTO eval, Vector<EvaluationItemDTO> items) {
        if (items == null || items.isEmpty()) return false;
        double sum = 0;
        for (EvaluationItemDTO item : items) sum += item.getScore().doubleValue();
        double avg = sum / items.size();
        eval.setTotalScore(new BigDecimal(avg));
        eval.setGrade(calculateGrade(avg));
        return evalDao.insertEvaluation(eval, items);
    }

    // ── 권한 체크 헬퍼 ───────────────────────────────────────

    /**
     * 인사평가 확정/반려 권한: HR담당자만
     * 관리자는 일반사용자로 취급 → 권한 없음
     */
    private boolean isHrOnly(String userRole) {
        return "HR담당자".equals(userRole);
    }

    // ── 대상자 목록 ──────────────────────────────────────────

    /**
     * 평가 유형별 대상자 목록
     * - HR담당자: 전체 재직자 (관리자는 일반사용자 취급)
     * - 자기평가: 본인만
     * - 상위평가: 하위직급 재직자
     * - 동료평가: 동일직급 재직자 (본인 제외)
     */
    public Vector<Map<String, Object>> getEmployeeListForEvaluator(
            int evaluatorId, String userRole, String evalType) {
        if ("HR담당자".equals(userRole))
            return evalDao.getEmployeeList();
        int posLevel = evalDao.getPositionLevelByEmpId(evaluatorId);
        return evalDao.getEmployeeListForEvaluator(evaluatorId, posLevel, evalType);
    }

    /** 기존 시그니처 호환 */
    public Vector<Map<String, Object>> getEmployeeListForEvaluator(int evaluatorId, String userRole) {
        return getEmployeeListForEvaluator(evaluatorId, userRole, "상위평가");
    }

    // ── 조회 ─────────────────────────────────────────────────

    public Map<String, Object> getEvaluationById(int evalId) {
        return evalDao.getEvaluationById(evalId);
    }

    public List<BigDecimal> getItemScoresByEvalId(int evalId, Vector<String> itemNames) {
        return evalDao.getItemScoresByEvalId(evalId, itemNames);
    }

    public Map<String, Object> loadExistingEval(int empId, int year, String period,
                                                  String type, int evaluatorId) {
        return evalDao.getEvaluationByCondition(empId, year, period, type, evaluatorId);
    }

    public boolean isAlreadyConfirmed(int empId, int year, String period, String type) {
        return evalDao.isAlreadyConfirmed(empId, year, period, type);
    }

    public boolean isSelfEvalBlocked(int targetEmpId, int evaluatorId, String evalType) {
        if ("자기평가".equals(evalType)) return false;
        return targetEmpId == evaluatorId;
    }

    public boolean isPositionBlocked(int targetEmpId, int evaluatorId, String evalType) {
        if (!"상위평가".equals(evalType)) return false;
        int myLevel     = evalDao.getPositionLevelByEmpId(evaluatorId);
        int targetLevel = evalDao.getPositionLevelByEmpId(targetEmpId);
        return targetLevel >= myLevel;
    }

    public Vector<Map<String, Object>> getEvaluationStatusList(
            int year, String period, String type, String searchTarget, String searchEvaluator) {
        return evalDao.getEvaluationStatusList(year, period, type, searchTarget, searchEvaluator);
    }

    public Map<String, Integer> getEvaluationSummary(int year, String period, String type) {
        return evalDao.getEvaluationSummary(year, period, type);
    }

    // ── 확정 / 반려 ───────────────────────────────────────────

    /**
     * 최종확정 처리 (HR담당자만)
     * - audit_log: EvaluationDAO 트랜잭션 내 직접 INSERT
     * - 알림: NotificationUtil.sendEvalConfirmed → 대상자에게 발송
     *
     * TODO [급여 인상 연동 포인트]
     * 확정 후 급여 인상 처리:
     *   1. SalaryPolicyService.getRaiseRate(grade) → 등급별 인상률 조회
     *   2. SalaryService.applyRaise(empId, raiseRate) → base_salary 업데이트
     *   3. audit_log에 급여 변경 이력 추가
     */
    public boolean confirmEvaluation(int evalId, String userRole, int actorId) {
        if (!isHrOnly(userRole)) return false;

        // DAO에서 확정 + audit_log 트랜잭션 처리 → 대상자 emp_id 반환
        int targetEmpId = evalDao.confirmEvaluationWithLog(evalId, actorId);
        if (targetEmpId < 0) return false;

        // 알림 발송 (트랜잭션 commit 완료 후 호출)
        try {
            Map<String, Object> evalData = evalDao.getEvaluationById(evalId);
            if (evalData != null && !evalData.isEmpty()) {
                int    evalYear   = (Integer) evalData.get("evalYear");
                String evalPeriod = (String)  evalData.get("evalPeriod");
                String grade      = (String)  evalData.get("grade");
                NotificationUtil.sendEvalConfirmed(targetEmpId, evalYear, evalPeriod, grade, evalId);
            }
        } catch (Exception e) {
            // 알림 실패는 로그만 남기고 무시 (메인 로직에 영향 없음)
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 반려 처리 (HR담당자만)
     * - audit_log: EvaluationDAO 트랜잭션 내 직접 INSERT
     * - 알림: NotificationUtil.sendEvalRejected → 평가 작성자에게 발송
     * - DB CHECK 제약 우회: eval_status='작성중' 복귀 + eval_comment에 [반려] 태그
     * - 재제출 시 REPLACE로 [반려] 태그 자동 제거
     *
     * TODO [알람 포인트 - 반려 시]
     * NotificationUtil에 sendEvalRejected 추가 후 아래 주석 해제
     */
    public boolean rejectEvaluation(int evalId, String userRole, int actorId) {
        if (!isHrOnly(userRole)) return false;

        // DAO에서 반려 + audit_log 트랜잭션 처리 → 평가 작성자 evaluator_id 반환
        int evaluatorId = evalDao.rejectEvaluationWithLog(evalId, actorId);
        if (evaluatorId < 0) return false;

        // 알림 발송 (트랜잭션 commit 완료 후 호출)
        try {
            Map<String, Object> evalData = evalDao.getEvaluationById(evalId);
            if (evalData != null && !evalData.isEmpty()) {
                int    evalYear   = (Integer) evalData.get("evalYear");
                String evalPeriod = (String)  evalData.get("evalPeriod");
                String msg = evalYear + "년 " + evalPeriod + " 인사평가가 반려되었습니다. 내용 검토 후 재제출해 주세요.";
                // NotificationUtil 내부 send() 패턴 동일하게 직접 활용
                // sendEvalRejected가 NotificationUtil에 추가되면 아래로 교체:
                // NotificationUtil.sendEvalRejected(evaluatorId, evalYear, evalPeriod, evalId);
                // 현재: sendEvalConfirmed와 동일 방식으로 직접 send 호출 가능하나
                // NotificationUtil.send()가 private이므로 아래처럼 우회 처리
                // → NotificationUtil에 sendEvalRejected 메서드 추가 필요 (아래 참조)
                sendEvalRejectedNotification(evaluatorId, evalYear, evalPeriod, evalId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 반려 알림 발송 헬퍼
     * NotificationUtil에 sendEvalRejected가 없으므로 임시 처리
     * → NotificationUtil에 아래 메서드 추가 권장:
     *
     * public static void sendEvalRejected(int empId, int year, String period, int evalId) {
     *     String msg = year + "년 " + period + " 인사평가가 반려되었습니다. 내용 검토 후 재제출해 주세요.";
     *     send(empId, "EVAL_REJECTED", "evaluation", evalId, msg);
     * }
     */
    /**
     * 반려 알림 발송
     * NotificationUtil.send()가 private이므로 NotificationDAO를 직접 사용
     * NotificationUtil에 sendEvalRejected가 추가되면 아래로 교체:
     *   NotificationUtil.sendEvalRejected(evaluatorId, year, period, evalId);
     */
    private void sendEvalRejectedNotification(int evaluatorId, int year, String period, int evalId) {
        try {
            com.hrms.sys.dao.NotificationDAO notificationDAO = new com.hrms.sys.dao.NotificationDAO();
            java.sql.Connection conn = com.hrms.common.db.DatabaseConnection.getConnection();
            String msg = year + "년 " + period + " 인사평가가 반려되었습니다. 내용 검토 후 재제출해 주세요.";
            notificationDAO.insert(evaluatorId, "EVAL_REJECTED", "evaluation", evalId, msg, conn);
            conn.close();
        } catch (Exception e) {
            // 알림 실패는 로그만 남기고 무시 (NotificationUtil과 동일 정책)
            e.printStackTrace();
        }
    }
}