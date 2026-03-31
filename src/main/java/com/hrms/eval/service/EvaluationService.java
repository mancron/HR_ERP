package com.hrms.eval.service;

import com.hrms.eval.dao.EvaluationDAO;
import com.hrms.eval.dto.EvaluationDTO;
import com.hrms.eval.dto.EvaluationItemDTO;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Vector;
import java.util.HashMap;

public class EvaluationService {
    private EvaluationDAO evalDao = new EvaluationDAO();

    // [추가] 등급에 따른 색상 코드를 반환하는 로직 (UI 보조용)
    public String getGradeColor(String grade) {
        switch (grade) {
            case "S": return "#ef4444"; // Red
            case "A": return "#f59e0b"; // Orange
            case "B": return "#3b82f6"; // Blue
            case "C": return "#22c55e"; // Green
            default: return "#94a3b8";  // Gray
        }
    }

    // [기존 기능 유지] 사원 목록 조회
    public Vector<Map<String, Object>> getEmployeeList() {
        return evalDao.getEmployeeList();
    }

    // [기존 기능 유지] 평가 항목명 조회
    public Vector<String> getEvaluationItemNames() {
        return evalDao.getEvaluationItemNames();
    }

    // [기존 기능 유지] 등급 계산 (JSP에서도 이 기준을 따름)
    public String calculateGrade(double avg) {
        if (avg >= 95) return "S";
        if (avg >= 85) return "A";
        if (avg >= 75) return "B";
        if (avg >= 60) return "C";
        return "D";
    }

    public boolean submitEvaluation(EvaluationDTO eval, Vector<EvaluationItemDTO> items) {
        if (items == null || items.isEmpty()) return false;
        double sum = 0;
        for (EvaluationItemDTO item : items) {
            sum += item.getScore().doubleValue();
        }
        double avg = sum / items.size();
        eval.setTotalScore(new BigDecimal(avg));
        eval.setGrade(calculateGrade(avg));
        return evalDao.insertEvaluation(eval, items);
    }
}