package com.hrms.eval.dto;

import java.math.BigDecimal;

public class EvaluationItemDTO {
    private int itemId;           // 평가항목 ID (PK)
    private int evalId;           // 평가 ID (FK)
    private String itemName;      // 항목명 (업무성과/직무역량/조직기여도/리더십)
    private BigDecimal score;     // 획득 점수
    private BigDecimal maxScore;  // 만점 기준

    // 기본 생성자
    public EvaluationItemDTO() {}

    // Getter & Setter
    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public int getEvalId() { return evalId; }
    public void setEvalId(int evalId) { this.evalId = evalId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }

    public BigDecimal getMaxScore() { return maxScore; }
    public void setMaxScore(BigDecimal maxScore) { this.maxScore = maxScore; }
}