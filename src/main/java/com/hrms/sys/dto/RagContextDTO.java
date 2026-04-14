package com.hrms.sys.dto;

import java.util.List;

/**
 * Python RAG 서버 /api/v1/search_context 응답을 담는 DTO.
 *
 * 응답 JSON 구조:
 * {
 *   "matched_categories":  ["ATTENDANCE", "EMPLOYEE"],
 *   "few_shot_examples":   [{"question": "...", "sql": "..."}],
 *   "similarity_scores":   [0.92, 0.85]
 * }
 *
 * NOTE: getter/setter는 사용자가 직접 작성.
 */
public class RagContextDTO {

    /** RAG 서버가 반환한 카테고리 목록 (예: ["ATTENDANCE", "EMPLOYEE"]) */
    private List<String> matchedCategories;

    /** 검색된 Few-shot 예제 목록 */
    private List<FewShotExample> fewShotExamples;

    /** 각 예제의 유사도 점수 (0.0 ~ 1.0) */
    private List<Double> similarityScores;

    /**
     * RAG 서버 호출 성공 여부.
     * false면 DynamicPromptBuilder 폴백으로 전환.
     * (Python 서버 응답 JSON에는 없는 필드 — Java에서 직접 세팅)
     */
    
    private boolean ragAvailable;

    
    public List<String> getMatchedCategories() { return matchedCategories; }
    public void setMatchedCategories(List<String> matchedCategories) { this.matchedCategories = matchedCategories; }

    public List<FewShotExample> getFewShotExamples() { return fewShotExamples; }
    public void setFewShotExamples(List<FewShotExample> fewShotExamples) { this.fewShotExamples = fewShotExamples; }

    public List<Double> getSimilarityScores() { return similarityScores; }
    public void setSimilarityScores(List<Double> similarityScores) { this.similarityScores = similarityScores; }

    public boolean isRagAvailable() { return ragAvailable; }
    public void setRagAvailable(boolean ragAvailable) { this.ragAvailable = ragAvailable; }

    
    // ──────────────────────────────────────────────
    // 내부 클래스 — Few-shot 예제 1건
    // ──────────────────────────────────────────────
    public static class FewShotExample {

        /** 예제 질문 (자연어) */
        private String question;

        /** 예제 SQL */
        private String sql;

		public String getQuestion() {
			return question;
		}

		public void setQuestion(String question) {
			this.question = question;
		}

		public String getSql() {
			return sql;
		}

		public void setSql(String sql) {
			this.sql = sql;
		}

        
        
    }
    
    
}
