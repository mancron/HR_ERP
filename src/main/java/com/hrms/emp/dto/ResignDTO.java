package com.hrms.emp.dto;

import java.time.LocalDateTime;

public class ResignDTO {

	private int request_id;          // 신청 ID (PK)
    private int emp_id;              // 신청자 emp_id (FK)
    private String resign_date;      // 희망 퇴직일
    private String reason;           // 신청 사유
    private String status;           // 대기 / 부서장승인 / 인사승인 / 최종승인 / 반려
    private int dept_manager_id;     // 부서장 emp_id (FK)
    private LocalDateTime dept_approved_at;  // 부서장 승인일시
    private int hr_manager_id;       // 인사담당자 emp_id (FK)
    private LocalDateTime hr_approved_at;    // 인사담당자 승인일시
    private String reject_reason;    // 반려 사유
    private LocalDateTime created_at; // 신청일시
    private int president_id;                  // 최종승인자 emp_id
    private LocalDateTime president_approved_at; // 최종승인자 승인일시
    
    // 조회 시 JOIN으로 가져올 추가 필드
    private String emp_name;         // 신청자 이름
    private String emp_no;           // 신청자 사번
    private String dept_name;        // 신청자 부서명
    private String position_name;    // 신청자 직급명
    private String dept_manager_name; // 부서장 이름
    private String hr_manager_name;  // 인사담당자 이름
    private boolean hrDept; // 신청자가 인사팀 소속인지
	private boolean reqIsPresident; // 신청자가 최종승인자인지
	private String president_name; // 최종승인자 이름

    


	public ResignDTO() {}
    
	
    
 
    
    public int getPresident_id() {
		return president_id;
	}
	public void setPresident_id(int president_id) {
		this.president_id = president_id;
	}
	
	public LocalDateTime getPresident_approved_at() {
		return president_approved_at;
	}
	
	public void setPresident_approved_at(LocalDateTime president_approved_at) {
		this.president_approved_at = president_approved_at;
	}

	public boolean isReqIsPresident() {
		return reqIsPresident;
	}

	public void setReqIsPresident(boolean reqIsPresident) {
		this.reqIsPresident = reqIsPresident;
	}

	public String getPresident_name() {
		return president_name;
	}

	public void setPresident_name(String president_name) {
		this.president_name = president_name;
	}

	public boolean isHrDept() {
		return hrDept;
	}

	public void setHrDept(boolean hrDept) {
		this.hrDept = hrDept;
	}

	public int getRequest_id() {
    	
		return request_id;
	}

	public void setRequest_id(int request_id) {
		this.request_id = request_id;
	}

	public int getEmp_id() {
		return emp_id;
	}

	public void setEmp_id(int emp_id) {
		this.emp_id = emp_id;
	}

	public String getResign_date() {
		return resign_date;
	}

	public void setResign_date(String resign_date) {
		this.resign_date = resign_date;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getDept_manager_id() {
		return dept_manager_id;
	}

	public void setDept_manager_id(int dept_manager_id) {
		this.dept_manager_id = dept_manager_id;
	}

	public LocalDateTime getDept_approved_at() {
		return dept_approved_at;
	}

	public void setDept_approved_at(LocalDateTime dept_approved_at) {
		this.dept_approved_at = dept_approved_at;
	}

	public int getHr_manager_id() {
		return hr_manager_id;
	}

	public void setHr_manager_id(int hr_manager_id) {
		this.hr_manager_id = hr_manager_id;
	}

	public LocalDateTime getHr_approved_at() {
		return hr_approved_at;
	}

	public void setHr_approved_at(LocalDateTime hr_approved_at) {
		this.hr_approved_at = hr_approved_at;
	}

	public String getReject_reason() {
		return reject_reason;
	}

	public void setReject_reason(String reject_reason) {
		this.reject_reason = reject_reason;
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

	public String getDept_name() {
		return dept_name;
	}

	public void setDept_name(String dept_name) {
		this.dept_name = dept_name;
	}

	public String getPosition_name() {
		return position_name;
	}

	public void setPosition_name(String position_name) {
		this.position_name = position_name;
	}

	public String getDept_manager_name() {
		return dept_manager_name;
	}

	public void setDept_manager_name(String dept_manager_name) {
		this.dept_manager_name = dept_manager_name;
	}

	public String getHr_manager_name() {
		return hr_manager_name;
	}

	public void setHr_manager_name(String hr_manager_name) {
		this.hr_manager_name = hr_manager_name;
	}

	
}
