package com.hrms.eval.service;

import com.hrms.eval.dao.EvaluationDAO;
import com.hrms.eval.dto.EvaluationDTO;
import com.hrms.eval.dto.EvaluationItemDTO;
import java.math.BigDecimal;
import java.util.List;
import java.util.Vector;

public class EvaluationService {
    private EvaluationDAO dao = new EvaluationDAO();

    public boolean saveEvaluation(EvaluationDTO eval, Vector<EvaluationItemDTO> items) {
        // 1. 종합 점수 계산 (Vector의 요소들을 순회)
        double sum = 0;
        for (EvaluationItemDTO item : items) {
            sum += item.getScore().doubleValue();
        }
        double avg = items.isEmpty() ? 0 : sum / items.size();
        eval.setTotalScore(new BigDecimal(avg)); // EvaluationDTO의 totalScore 세팅

        // 2. 등급 판정
        String grade = "D";
        if (avg >= 90) grade = "S";
        else if (avg >= 80) grade = "A";
        else if (avg >= 70) grade = "B";
        else if (avg >= 60) grade = "C";
        eval.setGrade(grade);

        // 3. DAO의 insertEvaluation 호출 (기존 DAO 매개변수 타입을 List에서 Vector 혹은 List로 유지)
        return dao.insertEvaluation(eval, items);
    }
}