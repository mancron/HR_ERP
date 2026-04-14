package com.hrms.sys.dto;

/**
 * rag_success_log 테이블 매핑 DTO.
 * NOTE: getter/setter는 사용자가 직접 작성.
 */
public class RagSuccessLogDTO {

    /** PK */
    private long logId;

    /** 사용자 자연어 질문 */
    private String question;

    /** LLM이 생성하고 실행 성공한 SQL */
    private String generatedSql;

    /** RAG 또는 폴백이 결정한 카테고리 (콤마 구분 문자열, 예: "ATTENDANCE,EMPLOYEE") */
    private String category;

    /** 쿼리 결과 행 수 */
    private int rowCount;

    /** true=RAG 경로, false=폴백(키워드 매칭) 경로 */
    private boolean usedRag;

    /** RAG 최고 유사도 점수 (폴백 시 null) */
    private Double similarity;

    /** false=미반영, true=벡터 DB 반영 완료 */
    private boolean upserted;

	public long getLogId() {
		return logId;
	}

	public void setLogId(long logId) {
		this.logId = logId;
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public String getGeneratedSql() {
		return generatedSql;
	}

	public void setGeneratedSql(String generatedSql) {
		this.generatedSql = generatedSql;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public int getRowCount() {
		return rowCount;
	}

	public void setRowCount(int rowCount) {
		this.rowCount = rowCount;
	}

	public boolean isUsedRag() {
		return usedRag;
	}

	public void setUsedRag(boolean usedRag) {
		this.usedRag = usedRag;
	}

	public Double getSimilarity() {
		return similarity;
	}

	public void setSimilarity(Double similarity) {
		this.similarity = similarity;
	}

	public boolean isUpserted() {
		return upserted;
	}

	public void setUpserted(boolean upserted) {
		this.upserted = upserted;
	}


    
}
