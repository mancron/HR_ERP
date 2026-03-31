package com.hrms.emp.dto;

import java.time.LocalDateTime;

public class HistoryDTO {

	private int history_id;
	private int emp_id;
	private String change_type;
	private int from_dept_id;
	private int to_dept_id;
	private int from_position_id;
	private int to_position_id;
	private LocalDateTime change_date;
	private String reason;
	private int approved_by;
	private LocalDateTime craeted_at;
	
	public HistoryDTO() {}
	
	public int getHistory_id() {
		return history_id;
	}
	public void setHistory_id(int history_id) {
		this.history_id = history_id;
	}
	public int getEmp_id() {
		return emp_id;
	}
	public void setEmp_id(int emp_id) {
		this.emp_id = emp_id;
	}
	public String getChange_type() {
		return change_type;
	}
	public void setChange_type(String change_type) {
		this.change_type = change_type;
	}
	public int getFrom_dept_id() {
		return from_dept_id;
	}
	public void setFrom_dept_id(int from_dept_id) {
		this.from_dept_id = from_dept_id;
	}
	public int getTo_dept_id() {
		return to_dept_id;
	}
	public void setTo_dept_id(int to_dept_id) {
		this.to_dept_id = to_dept_id;
	}
	public int getFrom_position_id() {
		return from_position_id;
	}
	public void setFrom_position_id(int from_position_id) {
		this.from_position_id = from_position_id;
	}
	public int getTo_position_id() {
		return to_position_id;
	}
	public void setTo_position_id(int to_position_id) {
		this.to_position_id = to_position_id;
	}
	public LocalDateTime getChange_date() {
		return change_date;
	}
	public void setChange_date(LocalDateTime change_date) {
		this.change_date = change_date;
	}
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}
	public int getApproved_by() {
		return approved_by;
	}
	public void setApproved_by(int approved_by) {
		this.approved_by = approved_by;
	}
	public LocalDateTime getCraeted_at() {
		return craeted_at;
	}
	public void setCraeted_at(LocalDateTime craeted_at) {
		this.craeted_at = craeted_at;
	}
}
