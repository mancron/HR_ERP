package com.hrms.eval.service;

import com.hrms.eval.dao.EvaluationDAO;
import com.hrms.eval.dto.EvaluationDTO;
import com.hrms.eval.dto.EvaluationItemDTO;
import com.hrms.common.util.NotificationUtil;

import java.math.BigDecimal;
import java.util.*;

public class EvaluationService {

    private EvaluationDAO evalDao = new EvaluationDAO();

    // ── 기초 로직 ──────────────────────────────────────────

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

    /**
     * 평가 제출
     * N-5: 서버사이드 코멘트/항목 유효성 체크
     * M-1: 자기평가 차단
     * M-2(write 측): 직급 차단
     */
    public boolean submitEvaluation(EvaluationDTO eval, Vector<EvaluationItemDTO> items) throws Exception {
        if (items == null || items.isEmpty())
            throw new Exception("평가 항목 점수가 누락되었습니다.");
        if (eval.getEvalComment() == null || eval.getEvalComment().trim().isEmpty())
            throw new Exception("평가 의견(코멘트)을 입력해 주세요.");

        // 코멘트에 [반려] 태그가 남아있으면 제거 후 저장
        String comment = eval.getEvalComment().trim();
        while (comment.startsWith("[반려]")) {
            comment = comment.substring("[반려]".length()).trim();
        }
        eval.setEvalComment(comment);

        if (isSelfEvalBlocked(eval.getEmpId(), eval.getEvaluatorId(), eval.getEvalType()))
            throw new Exception("자기평가 외의 유형으로 본인을 평가할 수 없습니다.");
        if (isPositionBlocked(eval.getEmpId(), eval.getEvaluatorId(), eval.getEvalType()))
            throw new Exception("선택한 대상자를 평가할 권한이 없습니다 (직급 제한).");

        double sum = 0;
        for (EvaluationItemDTO item : items) sum += item.getScore().doubleValue();
        double avg = sum / items.size();
        eval.setTotalScore(new BigDecimal(avg));
        eval.setGrade(calculateGrade(avg));

        boolean result = evalDao.insertEvaluation(eval, items);
        if (!result) throw new Exception("데이터베이스 저장 중 오류가 발생했습니다.");
        return true;
    }

    // ── 권한 체크 ─────────────────────────────────────────

    /** HR담당자만 확정/반려 가능. 관리자는 일반사용자 취급 */
    private boolean isHrOnly(String userRole) {
        return "HR담당자".equals(userRole);
    }

    // ── 대상자 목록 ───────────────────────────────────────

    /**
     * 평가 유형별 대상자 목록
     * [NEW-4 수정] ADMIN_ALL 특수 분기 제거 — 실제로 사용되지 않는 사문 코드였음
     * HR담당자도 평가 작성 시 본인 직급 기반 목록 표시 (확정/반려와 별개)
     */
 // EvaluationService.java
    public Vector<Map<String, Object>> getEmployeeListForEvaluator(int evaluatorId, String userRole, String evalType) {
        
        // 1. DB에서 이 사용자의 실제 직급 레벨을 직접 조회 (가장 정확함)
        int posLevel = evalDao.getPositionLevelByEmpId(evaluatorId); 
        
        // 만약 사장님/최종승인자인데 특별 처리가 필요하다면 여기서만 살짝 분기
        if ("사장님".equals(userRole) || "최종승인자".equals(userRole)) {
            posLevel = 999; // 모든 하위 직급을 다 볼 수 있는 압도적 숫자
        }


        return evalDao.getEmployeeListForEvaluator(evaluatorId, posLevel, evalType);
    }

    /** 기존 시그니처 호환 */
    public Vector<Map<String, Object>> getEmployeeListForEvaluator(int evaluatorId, String userRole) {
        return getEmployeeListForEvaluator(evaluatorId, userRole, "상위평가");
    }

    // ── 보안 검증 ─────────────────────────────────────────

    /** 자기평가 아닌데 본인 대상 선택 시 차단 */
    public boolean isSelfEvalBlocked(int targetEmpId, int evaluatorId, String evalType) {
        if ("자기평가".equals(evalType)) return false;
        return targetEmpId == evaluatorId;
    }

    /**
     * 직급 역전 차단
     * 상위평가: 내가 대상자보다 상위여야 함 → 대상자 레벨 >= 내 레벨이면 차단
     * 동료평가: 직급이 같아야 함 → 다르면 차단
     */
    public boolean isPositionBlocked(int targetEmpId, int evaluatorId, String evalType) {
        int myLevel     = evalDao.getPositionLevelByEmpId(evaluatorId);
        int targetLevel = evalDao.getPositionLevelByEmpId(targetEmpId);

        if ("상위평가".equals(evalType)) {
            return targetLevel >= myLevel; // 대상이 나보다 높거나 같으면 차단
        }
        if ("하위평가".equals(evalType)) {
            return targetLevel <= myLevel; // 반대로 대상 레벨이 나보다 높거나 같으면 차단
        }
        if ("동료평가".equals(evalType)) {
            return targetLevel != myLevel; // 동일 직급이 아니면 차단
        }
        return false;
    }

    // ── 조회 ─────────────────────────────────────────────

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

    public Vector<Map<String, Object>> getEvaluationStatusList(
            int year, String period, String type, String sTarget, String sEval) {
        return evalDao.getEvaluationStatusList(year, period, type, sTarget, sEval);
    }

    /** * [NEW-6 해결] 평가 현황 요약 데이터 조회
     * 검색 필터(대상자명, 평가자명)를 포함하여 요약 카드를 집계하도록 파라미터 확장
     */
    public Map<String, Integer> getEvaluationSummary(int year, String period, String type, String searchTarget, String searchEvaluator) {
        return evalDao.getEvaluationSummary(year, period, type, searchTarget, searchEvaluator);
    }

    // ── 확정 / 반려 ───────────────────────────────────────

    /**
     * 최종확정 (HR담당자만)
     */
    public boolean confirmEvaluation(int evalId, String userRole, int actorId) throws Exception {
        if (!isHrOnly(userRole))
            throw new Exception("인사평가 확정 권한이 없습니다.");

        Map<String, Object> evalData = evalDao.getEvaluationById(evalId);
        if (evalData == null || evalData.isEmpty())
            throw new Exception("존재하지 않는 평가 데이터입니다.");

        int targetEmpId = (Integer) evalData.get("empId");
        if (targetEmpId == actorId)
            throw new Exception("본인의 평가 결과는 직접 확정할 수 없습니다.");

        int confirmedEmpId = evalDao.confirmEvaluationWithLog(evalId, actorId);
        if (confirmedEmpId < 0)
            throw new Exception("DB 처리 중 오류가 발생했습니다.");

        try {
            int    evalYear   = (Integer) evalData.get("evalYear");
            String evalPeriod = (String)  evalData.get("evalPeriod");
            String grade      = (String)  evalData.get("grade");
            NotificationUtil.sendEvalConfirmed(confirmedEmpId, evalYear, evalPeriod, grade, evalId);
        } catch (Exception e) { e.printStackTrace(); }

        return true;
    }

    /**
     * 반려 (HR담당자만)
     */
    public boolean rejectEvaluation(int evalId, String userRole, int actorId) {
        if (!isHrOnly(userRole)) return false;

        int evaluatorId = evalDao.rejectEvaluationWithLog(evalId, actorId);
        if (evaluatorId < 0) return false;

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

    private void sendEvalRejectedNotification(int evaluatorId, int year, String period, int evalId) {
        try (java.sql.Connection conn = com.hrms.common.db.DatabaseConnection.getConnection()) {
            com.hrms.sys.dao.NotificationDAO notificationDAO = new com.hrms.sys.dao.NotificationDAO();
            String msg = year + "년 " + period + " 인사평가가 반려되었습니다. 내용 검토 후 재제출해 주세요.";
            notificationDAO.insert(evaluatorId, "EVAL_REJECTED", "evaluation", evalId, msg, conn);
        } catch (Exception e) { e.printStackTrace(); }
    }
}