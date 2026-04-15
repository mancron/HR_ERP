package com.hrms.eval.service;

import com.hrms.eval.dao.EvaluationDAO;
import com.hrms.eval.dto.EvaluationDTO;
import com.hrms.eval.dto.EvaluationItemDTO;
import com.hrms.common.util.NotificationUtil;

import java.math.BigDecimal;
import java.util.*;

public class EvaluationService {

    private EvaluationDAO evalDao = new EvaluationDAO();

    // ── [1] 기초 유틸 및 데이터 조회 ──────────────────────────────

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

    public Vector<Map<String, Object>> getEmployeeList() {
        return evalDao.getEmployeeList();
    }

    public Vector<String> getEvaluationItemNames() {
        return evalDao.getEvaluationItemNames();
    }

    public String calculateGrade(double avg) {
        if (avg >= 95) return "S";
        if (avg >= 85) return "A";
        if (avg >= 75) return "B";
        if (avg >= 60) return "C";
        return "D";
    }

    // ── [2] 평가 제출 로직 (반려 태그 처리 포함) ─────────────────────

    /**
     * 평가 제출
     * N-5: 서버사이드 코멘트/항목 유효성 체크
     * 재제출 시 [반려] 태그 + [반려 사유] 이하 모두 제거
     */
    public boolean submitEvaluation(EvaluationDTO eval, Vector<EvaluationItemDTO> items) throws Exception {
        if (items == null || items.isEmpty())
            throw new Exception("평가 항목 점수가 누락되었습니다.");
        if (eval.getEvalComment() == null || eval.getEvalComment().trim().isEmpty())
            throw new Exception("평가 의견(코멘트)을 입력해 주세요.");

        // 재제출 시 [반려 사유] 이하 제거 후 [반려] 태그 제거
        String comment = eval.getEvalComment().trim();
        int reasonIdx = comment.indexOf("[반려 사유]");
        if (reasonIdx >= 0) {
            comment = comment.substring(0, reasonIdx).trim();
        }
        comment = comment.replace("[반려]", "").trim();
        eval.setEvalComment(comment);

        // 보안 체크
        if (isSelfEvalBlocked(eval.getEmpId(), eval.getEvaluatorId(), eval.getEvalType()))
            throw new Exception("자기평가 외의 유형으로 본인을 평가할 수 없습니다.");
        if (isPositionBlocked(eval.getEmpId(), eval.getEvaluatorId(), eval.getEvalType()))
            throw new Exception("선택한 대상자를 평가할 권한이 없습니다 (직급 제한).");

        // 점수 계산
        double sum = 0;
        for (EvaluationItemDTO item : items) sum += item.getScore().doubleValue();
        double avg = sum / items.size();
        eval.setTotalScore(new BigDecimal(avg));
        eval.setGrade(calculateGrade(avg));

        boolean result = evalDao.insertEvaluation(eval, items);
        if (!result) throw new Exception("데이터베이스 저장 중 오류가 발생했습니다.");
        return true;
    }

    // ── [3] 권한 관리 및 대상자 필터링 ───────────────────────────

    /**
     * 확정/반려 권한 체크
     * HR담당자 + 사장님 + 최종승인자 모두 허용
     */
    private boolean isHrOnly(String userRole) {
        return "HR담당자".equals(userRole) || "사장님".equals(userRole) || "최종승인자".equals(userRole);
    }

    /**
     * 평가 유형별 대상자 목록
     * - 사장/최종승인자: posLevel=999 → DAO에서 전체 재직자 반환
     * - HR담당자: 인사팀 소속 직원 제외 (상호 평가 방지)
     * - 그 외: 본인 직급 기반 필터링
     */
    public Vector<Map<String, Object>> getEmployeeListForEvaluator(
            int evaluatorId, String userRole, String evalType) {
        int posLevel = evalDao.getPositionLevelByEmpId(evaluatorId);

        if ("최종승인자".equals(userRole) || "사장님".equals(userRole)) {
            posLevel = 999;
        }

        Vector<Map<String, Object>> rawList =
                evalDao.getEmployeeListForEvaluator(evaluatorId, posLevel, evalType);

        // HR담당자는 인사팀 소속 직원 목록에서 제외 (상호 평가 방지)
        if ("HR담당자".equals(userRole)) {
            rawList.removeIf(emp -> {
                Object deptObj = emp.get("deptName");
                if (deptObj == null) return false;
                return deptObj.toString().contains("인사");
            });
        }

        return rawList;
    }

    /** 기존 시그니처 호환 */
    public Vector<Map<String, Object>> getEmployeeListForEvaluator(int evaluatorId, String userRole) {
        return getEmployeeListForEvaluator(evaluatorId, userRole, "상위평가");
    }

    // ── [4] 보안 검증 (자기평가 및 직급 역전 방지) ─────────────────────

    /** 자기평가 아닌데 본인 대상 선택 시 차단 */
    public boolean isSelfEvalBlocked(int targetEmpId, int evaluatorId, String evalType) {
        if ("자기평가".equals(evalType)) return false;
        return targetEmpId == evaluatorId;
    }

    /**
     * 직급 역전 차단
     * 상위평가: targetLevel >= myLevel 이면 차단 (내 하위가 아니면 불가)
     * 하위평가: targetLevel <= myLevel 이면 차단 (내 상위가 아니면 불가)
     * 동료평가: targetLevel != myLevel 이면 차단
     */
    public boolean isPositionBlocked(int targetEmpId, int evaluatorId, String evalType) {
        int myLevel     = evalDao.getPositionLevelByEmpId(evaluatorId);
        int targetLevel = evalDao.getPositionLevelByEmpId(targetEmpId);

        if ("상위평가".equals(evalType)) return targetLevel >= myLevel;
        if ("하위평가".equals(evalType)) return targetLevel <= myLevel;
        if ("동료평가".equals(evalType)) return targetLevel != myLevel;
        return false;
    }

    // ── [5] 상태 조회 로직 ─────────────────────────────────────────

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

    /**
     * 평가 현황 목록 조회 — 권한 포함 버전 (loginEmpId, userRole 전달)
     * 일반 직원: 본인 관련(작성자 or 대상자)만 조회
     * HR담당자/사장님/최종승인자: 전체 조회
     */
    public Vector<Map<String, Object>> getEvaluationStatusList(
            int year, String period, String type, String sTarget, String sEval,
            int loginEmpId, String userRole) {
        return evalDao.getEvaluationStatusList(year, period, type, sTarget, sEval, loginEmpId, userRole);
    }

    /** 기존 시그니처 호환 — 전체 조회(HR담당자 권한으로 처리) */
    public Vector<Map<String, Object>> getEvaluationStatusList(
            int year, String period, String type, String sTarget, String sEval) {
        return getEvaluationStatusList(year, period, type, sTarget, sEval, 0, "HR담당자");
    }

    /**
     * 평가 통계 요약 — 권한 포함 버전
     * [미완료 계산 정책]
     * - type == "전체": 미완료 평가 문서 건수 (행 수)
     * - type == 특정 유형: 미완료자 수 (제출 안 한 재직자 수)
     */
    public Map<String, Integer> getEvaluationSummary(int year, String period, String type,
                                                       String searchTarget, String searchEvaluator,
                                                       int loginEmpId, String userRole) {
        return evalDao.getEvaluationSummary(year, period, type, searchTarget, searchEvaluator, loginEmpId, userRole);
    }

    /** 기존 시그니처 호환 */
    public Map<String, Integer> getEvaluationSummary(int year, String period, String type) {
        return getEvaluationSummary(year, period, type, null, null, 0, "HR담당자");
    }

    // ── [6] 확정 및 반려 처리 (알림 발송 포함) ────────────────────────

    /**
     * 최종확정 (HR담당자 + 사장님 + 최종승인자)
     * M-2: 셀프 확정 차단
     * audit_log + 알림 발송
     *
     * TODO [급여 인상 연동 포인트]
     * 확정 후 등급 기반 급여 인상:
     *   1. SalaryPolicyService.getRaiseRate(grade)
     *   2. SalaryService.applyRaise(empId, raiseRate)
     */
    public boolean confirmEvaluation(int evalId, String userRole, int actorId) throws Exception {
        if (!isHrOnly(userRole))
            throw new Exception("인사평가 확정 권한이 없습니다.");

        Map<String, Object> evalData = evalDao.getEvaluationById(evalId);
        if (evalData == null || evalData.isEmpty())
            throw new Exception("존재하지 않는 평가 데이터입니다.");

        // M-2: 셀프 확정 차단
        int targetEmpId = (Integer) evalData.get("empId");
        if (targetEmpId == actorId)
            throw new Exception("본인의 평가 결과는 직접 확정할 수 없습니다.");

        int confirmedEmpId = evalDao.confirmEvaluationWithLog(evalId, actorId);
        if (confirmedEmpId < 0)
            throw new Exception("DB 처리 중 오류가 발생했습니다.");

        // 알림 발송 (commit 후 — 실패해도 확정 유지)
        try {
            int    evalYear   = (Integer) evalData.get("evalYear");
            String evalPeriod = (String)  evalData.get("evalPeriod");
            String grade      = (String)  evalData.get("grade");
            NotificationUtil.sendEvalConfirmed(confirmedEmpId, evalYear, evalPeriod, grade, evalId);
        } catch (Exception e) { e.printStackTrace(); }

        return true;
    }

    /**
     * 반려 (HR담당자 + 사장님 + 최종승인자) + rejectReason 포함
     * eval_comment 형식: "[반려] 기존코멘트\n[반려 사유] HR이 입력한 사유"
     * 중복 반려 차단: DAO에서 [반려] 태그 있으면 return -1
     */
    public boolean rejectEvaluation(int evalId, String userRole, int actorId, String rejectReason) {
        if (!isHrOnly(userRole)) return false;

        int evaluatorId = evalDao.rejectEvaluationWithLog(evalId, actorId, rejectReason);
        if (evaluatorId < 0) return false;

        // 알림 발송
        try {
            Map<String, Object> evalData = evalDao.getEvaluationById(evalId);
            if (evalData != null) {
                int    evalYear   = (Integer) evalData.get("evalYear");
                String evalPeriod = (String)  evalData.get("evalPeriod");
                sendEvalRejectedNotification(evaluatorId, evalYear, evalPeriod, evalId);
            }
        } catch (Exception e) { e.printStackTrace(); }

        return true;
    }

    /** 기존 시그니처 호환 (rejectReason 없는 경우 빈 문자열) */
    public boolean rejectEvaluation(int evalId, String userRole, int actorId) {
        return rejectEvaluation(evalId, userRole, actorId, "");
    }

    /**
     * 반려 알림 발송
     * NotificationUtil.send()가 private이라 NotificationDAO 직접 사용
     * → NotificationUtil에 sendEvalRejected 추가 시 교체:
     *   NotificationUtil.sendEvalRejected(evaluatorId, year, period, evalId);
     */
    private void sendEvalRejectedNotification(int evaluatorId, int year, String period, int evalId) {
        try (java.sql.Connection conn = com.hrms.common.db.DatabaseConnection.getConnection()) {
            com.hrms.sys.dao.NotificationDAO notificationDAO = new com.hrms.sys.dao.NotificationDAO();
            String msg = year + "년 " + period + " 인사평가가 반려되었습니다. 내용 검토 후 재제출해 주세요.";
            notificationDAO.insert(evaluatorId, "EVAL_REJECTED", "evaluation", evalId, msg, conn);
        } catch (Exception e) { e.printStackTrace(); }
    }
}