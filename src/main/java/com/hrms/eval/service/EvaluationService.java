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

    public boolean submitEvaluation(EvaluationDTO eval, Vector<EvaluationItemDTO> items) throws Exception {
        if (items == null || items.isEmpty())
            throw new Exception("평가 항목 점수가 누락되었습니다.");
        if (eval.getEvalComment() == null || eval.getEvalComment().trim().isEmpty())
            throw new Exception("평가 의견(코멘트)을 입력해 주세요.");

        // 재제출 시 [반려] 태그와 [반려 사유] 문구 제거 로직
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

    /** HR담당자/사장님/최종승인자 여부 확인 */
    private boolean isHrOnly(String userRole) {
        return "HR담당자".equals(userRole) || "사장님".equals(userRole) || "최종승인자".equals(userRole);
    }

    public Vector<Map<String, Object>> getEmployeeListForEvaluator(int evaluatorId, String userRole, String evalType) {
        int posLevel = evalDao.getPositionLevelByEmpId(evaluatorId);
        
        // 최고 관리 권한자는 전체 목록 조회 가능하게 posLevel 조정
        if ("최종승인자".equals(userRole) || "사장님".equals(userRole)) {
            posLevel = 999;
        }
        
        // 1. 먼저 DAO에서 기본 필터링된 리스트를 가져옵니다.
        Vector<Map<String, Object>> rawList = evalDao.getEmployeeListForEvaluator(evaluatorId, posLevel, evalType);
        
        // 2. 만약 로그인한 사람이 'HR담당자'라면, 리스트에서 '인사팀' 소속 직원을 제거합니다.
        if ("HR담당자".equals(userRole)) {
            rawList.removeIf(emp -> {
                Object deptObj = emp.get("deptName");
                if (deptObj == null) return false;
                String deptName = deptObj.toString();
                return deptName.contains("인사");
            });
        }
        
        return rawList;
    }

    public Vector<Map<String, Object>> getEmployeeListForEvaluator(int evaluatorId, String userRole) {
        return getEmployeeListForEvaluator(evaluatorId, userRole, "상위평가");
    }

    // ── [4] 보안 검증 (자기평가 및 직급 역전 방지) ─────────────────────

    public boolean isSelfEvalBlocked(int targetEmpId, int evaluatorId, String evalType) {
        if ("자기평가".equals(evalType)) return false;
        return targetEmpId == evaluatorId;
    }

    public boolean isPositionBlocked(int targetEmpId, int evaluatorId, String evalType) {
        int myLevel = evalDao.getPositionLevelByEmpId(evaluatorId);
        int targetLevel = evalDao.getPositionLevelByEmpId(targetEmpId);

        if ("상위평가".equals(evalType)) return targetLevel >= myLevel;
        if ("하위평가".equals(evalType)) return targetLevel <= myLevel;
        if ("동료평가".equals(evalType)) return targetLevel != myLevel;
        return false;
    }

    // ── [5] 상태 조회 로직 (메서드 오버로딩 포함) ─────────────────────

    public Map<String, Object> getEvaluationById(int evalId) {
        return evalDao.getEvaluationById(evalId);
    }

    public List<BigDecimal> getItemScoresByEvalId(int evalId, Vector<String> itemNames) {
        return evalDao.getItemScoresByEvalId(evalId, itemNames);
    }

    public Map<String, Object> loadExistingEval(int empId, int year, String period, String type, int evaluatorId) {
        return evalDao.getEvaluationByCondition(empId, year, period, type, evaluatorId);
    }

    public boolean isAlreadyConfirmed(int empId, int year, String period, String type) {
        return evalDao.isAlreadyConfirmed(empId, year, period, type);
    }

    /** [최신버전] 서블릿에서 loginEmpId와 userRole을 넘겨받는 메서드 */
    public Vector<Map<String, Object>> getEvaluationStatusList(
            int year, String period, String type, String sTarget, String sEval, int loginEmpId, String userRole) {
        return evalDao.getEvaluationStatusList(year, period, type, sTarget, sEval, loginEmpId, userRole);
    }

    /** [구버전 호환용] 질문하신 그 부분입니다! */
    public Vector<Map<String, Object>> getEvaluationStatusList(int year, String period, String type, String sTarget, String sEval) {
        // 기존에 이 메서드를 쓰던 서블릿들이 에러나지 않게, 
        // 권한 파라미터가 포함된 위쪽의 '진짜' 메서드를 다시 호출(return)해 주는 겁니다.
        // 기본값으로 0번 사번과 "HR담당자" 권한을 주어 전체 조회가 가능하게 설정한 예시입니다.
        return getEvaluationStatusList(year, period, type, sTarget, sEval, 0, "HR담당자");
    }

    public Map<String, Integer> getEvaluationSummary(int year, String period, String type,
                                                     String searchTarget, String searchEvaluator, int loginEmpId, String userRole) {
        return evalDao.getEvaluationSummary(year, period, type, searchTarget, searchEvaluator, loginEmpId, userRole);
    }

    public Map<String, Integer> getEvaluationSummary(int year, String period, String type) {
        return getEvaluationSummary(year, period, type, null, null, 0, "HR담당자");
    }

    // ── [6] 확정 및 반려 처리 (알림 발송 포함) ────────────────────────

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
            int evalYear = (Integer) evalData.get("evalYear");
            String evalPeriod = (String) evalData.get("evalPeriod");
            String grade = (String) evalData.get("grade");
            NotificationUtil.sendEvalConfirmed(confirmedEmpId, evalYear, evalPeriod, grade, evalId);
        } catch (Exception e) { e.printStackTrace(); }

        return true;
    }

    public boolean rejectEvaluation(int evalId, String userRole, int actorId, String rejectReason) {
        if (!isHrOnly(userRole)) return false;

        int evaluatorId = evalDao.rejectEvaluationWithLog(evalId, actorId, rejectReason);
        if (evaluatorId < 0) return false;

        try {
            Map<String, Object> evalData = evalDao.getEvaluationById(evalId);
            if (evalData != null) {
                int evalYear = (Integer) evalData.get("evalYear");
                String evalPeriod = (String) evalData.get("evalPeriod");
                sendEvalRejectedNotification(evaluatorId, evalYear, evalPeriod, evalId);
            }
        } catch (Exception e) { e.printStackTrace(); }

        return true;
    }

    public boolean rejectEvaluation(int evalId, String userRole, int actorId) {
        return rejectEvaluation(evalId, userRole, actorId, "");
    }

    private void sendEvalRejectedNotification(int evaluatorId, int year, String period, int evalId) {
        try (java.sql.Connection conn = com.hrms.common.db.DatabaseConnection.getConnection()) {
            com.hrms.sys.dao.NotificationDAO notificationDAO = new com.hrms.sys.dao.NotificationDAO();
            String msg = year + "년 " + period + " 인사평가가 반려되었습니다. 내용 검토 후 재제출해 주세요.";
            notificationDAO.insert(evaluatorId, "EVAL_REJECTED", "evaluation", evalId, msg, conn);
        } catch (Exception e) { e.printStackTrace(); }
    }
}