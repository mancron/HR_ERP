package com.hrms.sys.dto;

import java.util.List;
import java.util.Map;

public class TextToSqlResultDTO {

    private String       generatedSql;   // 모델이 생성한 SQL
    private List<String> columns;        // 컬럼명 목록 (테이블 헤더용)
    private List<Map<String, Object>> rows; // 결과 데이터
    private String       errorMsg;       // 오류 메시지 (실행 실패 시)
    private int          rowCount;       // 결과 행 수
    
	public String getGeneratedSql() {
		return generatedSql;
	}
	public void setGeneratedSql(String generatedSql) {
		this.generatedSql = generatedSql;
	}
	public List<String> getColumns() {
		return columns;
	}
	public void setColumns(List<String> columns) {
		this.columns = columns;
	}
	public List<Map<String, Object>> getRows() {
		return rows;
	}
	public void setRows(List<Map<String, Object>> rows) {
		this.rows = rows;
	}
	public String getErrorMsg() {
		return errorMsg;
	}
	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}
	public int getRowCount() {
		return rowCount;
	}
	public void setRowCount(int rowCount) {
		this.rowCount = rowCount;
	}

    
}