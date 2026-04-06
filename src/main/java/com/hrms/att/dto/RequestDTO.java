package com.hrms.att.dto;

//상세보기에 사용하는 DTO
public class RequestDTO {

    private int id;
    private String date;
    private String type;
    private String status;
    private String reason;
    private String applyDate;

    private String empName;
    private String approverName;
    private String approveDate;
    private String rejectReason;
    private String deptName;
    private String position;

    private String approverDept;
    private String approverPosition;
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}
	public String getEmpName() {
		return empName;
	}
	public void setEmpName(String empName) {
		this.empName = empName;
	}
	public String getApproverName() {
		return approverName;
	}
	public void setApproverName(String approverName) {
		this.approverName = approverName;
	}
	public String getApproveDate() {
		return approveDate;
	}
	public void setApproveDate(String approveDate) {
		this.approveDate = approveDate;
	}
	public String getRejectReason() {
		return rejectReason;
	}
	public void setRejectReason(String rejectReason) {
		this.rejectReason = rejectReason;
	}
	public String getApplyDate() {
		return applyDate;
	}
	public void setApplyDate(String applyDate) {
		this.applyDate = applyDate;
	}
	public String getDeptName() {
		return deptName;
	}
	public void setDeptName(String deptName) {
		this.deptName = deptName;
	}
	public String getPosition() {
		return position;
	}
	public void setPosition(String position) {
		this.position = position;
	}
	public String getApproverDept() {
		return approverDept;
	}
	public void setApproverDept(String approverDept) {
		this.approverDept = approverDept;
	}
	public String getApproverPosition() {
		return approverPosition;
	}
	public void setApproverPosition(String approverPosition) {
		this.approverPosition = approverPosition;
	}
}
