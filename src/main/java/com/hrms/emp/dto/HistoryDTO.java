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
	private String from_role;  // 이전 직책
	private String to_role;    // 변경 직책
	private LocalDateTime change_date;
	private String reason;
	private int approved_by;
	private LocalDateTime created_at;
	private int is_applied;  // 0=미처리, 1=처리완료
	
	//JOIN 결과 필드
	private String emp_name;           // 직원 이름
	private String emp_no;             // 직원 사번
	private String from_dept_name;     // 이전 부서명
	private String to_dept_name;       // 발령 부서명
	private String from_position_name; // 이전 직급명
	private String to_position_name;   // 변경 직급명
	private String approved_by_name;   // 승인자 이름
	
	

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
	public String getFrom_role() {
		return from_role;
	}
	public void setFrom_role(String from_role) {
		this.from_role = from_role;
	}
	public String getTo_role() {
		return to_role;
	}
	public void setTo_role(String to_role) {
		this.to_role = to_role;
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
	public LocalDateTime getCreated_at() {
		return created_at;
	}
	public void setCreated_at(LocalDateTime created_at) {
		this.created_at = created_at;
	}
	public String getEmp_name() {
		return emp_name;
	}
	public void setEmp_name(String emp_name) {
		this.emp_name = emp_name;
	}
	public String getEmp_no() {
		return emp_no;
	}
	public void setEmp_no(String emp_no) {
		this.emp_no = emp_no;
	}
	public String getFrom_dept_name() {
		return from_dept_name;
	}
	public void setFrom_dept_name(String from_dept_name) {
		this.from_dept_name = from_dept_name;
	}
	public String getTo_dept_name() {
		return to_dept_name;
	}
	public void setTo_dept_name(String to_dept_name) {
		this.to_dept_name = to_dept_name;
	}
	public String getFrom_position_name() {
		return from_position_name;
	}
	public void setFrom_position_name(String from_position_name) {
		this.from_position_name = from_position_name;
	}
	public String getTo_position_name() {
		return to_position_name;
	}
	public void setTo_position_name(String to_position_name) {
		this.to_position_name = to_position_name;
	}
	public String getApproved_by_name() {
		return approved_by_name;
	}
	public void setApproved_by_name(String approved_by_name) {
		this.approved_by_name = approved_by_name;
	}
	public int getIs_applied() {
		return is_applied;
	}
	public void setIs_applied(int is_applied) {
		this.is_applied = is_applied;
	}
}
