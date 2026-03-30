package com.hrms.eval.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 인사 평가(evaluation) 테이블 DTO
 */
public class EvaluationDTO {
    private int evalId;            // 평가 ID (PK)
    private int empId;             // 평가 대상자 emp_id (FK)
    private int evalYear;          // 평가 연도
    private String evalPeriod;     // 상반기/하반기/연간
    private String evalType;       // 자기평가/상위평가/동료평가
    private BigDecimal totalScore; // 종합 점수 (0~100)
    private String grade;          // 등급 (S/A/B/C/D)
    private String evalComment;    // 평가 의견
    private String evalStatus;     // 작성중/최종확정
    private Integer evaluatorId;   // 평가자 emp_id (FK, null 허용을 위해 Integer 사용)
    private Timestamp confirmedAt; // 최종확정 처리 일시
    private Timestamp createdAt;   // 생성일시

    // 기본 생성자
    public EvaluationDTO() {}

    // Getter & Setter
    public int getEvalId() { return evalId; }
    public void setEvalId(int evalId) { this.evalId = evalId; }

    public int getEmpId() { return empId; }
    public void setEmpId(int empId) { this.empId = empId; }

    public int getEvalYear() { return evalYear; }
    public void setEvalYear(int evalYear) { this.evalYear = evalYear; }

    public String getEvalPeriod() { return evalPeriod; }
    public void setEvalPeriod(String evalPeriod) { this.evalPeriod = evalPeriod; }

    public String getEvalType() { return evalType; }
    public void setEvalType(String evalType) { this.evalType = evalType; }

    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getEvalComment() { return evalComment; }
    public void setEvalComment(String evalComment) { this.evalComment = evalComment; }

    public String getEvalStatus() { return evalStatus; }
    public void setEvalStatus(String evalStatus) { this.evalStatus = evalStatus; }

    public Integer getEvaluatorId() { return evaluatorId; }
    public void setEvaluatorId(Integer evaluatorId) { this.evaluatorId = evaluatorId; }

    public Timestamp getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Timestamp confirmedAt) { this.confirmedAt = confirmedAt; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}