package com.hrms.sal.dto;

public class SalaryStatusDTO {

    private int    salaryId;
    private int    empId;
    private String empName;
    private String deptName;
    private String positionName;
    private int    baseSalary;
    private int    grossSalary;    // 지급합계
    private int    totalDeduction; // 공제합계
    private int    netSalary;      // 실수령액
    private String payDate;        // 지급일 (포맷 문자열)
    private String status;         // 대기/완료
    private int    salaryYear;
    private int    salaryMonth;
	public int getSalaryId() {
		return salaryId;
	}
	public void setSalaryId(int salaryId) {
		this.salaryId = salaryId;
	}
	public int getEmpId() {
		return empId;
	}
	public void setEmpId(int empId) {
		this.empId = empId;
	}
	public String getEmpName() {
		return empName;
	}
	public void setEmpName(String empName) {
		this.empName = empName;
	}
	public String getDeptName() {
		return deptName;
	}
	public void setDeptName(String deptName) {
		this.deptName = deptName;
	}
	public String getPositionName() {
		return positionName;
	}
	public void setPositionName(String positionName) {
		this.positionName = positionName;
	}
	public int getBaseSalary() {
		return baseSalary;
	}
	public void setBaseSalary(int baseSalary) {
		this.baseSalary = baseSalary;
	}
	public int getGrossSalary() {
		return grossSalary;
	}
	public void setGrossSalary(int grossSalary) {
		this.grossSalary = grossSalary;
	}
	public int getTotalDeduction() {
		return totalDeduction;
	}
	public void setTotalDeduction(int totalDeduction) {
		this.totalDeduction = totalDeduction;
	}
	public int getNetSalary() {
		return netSalary;
	}
	public void setNetSalary(int netSalary) {
		this.netSalary = netSalary;
	}
	public String getPayDate() {
		return payDate;
	}
	public void setPayDate(String payDate) {
		this.payDate = payDate;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public int getSalaryYear() {
		return salaryYear;
	}
	public void setSalaryYear(int salaryYear) {
		this.salaryYear = salaryYear;
	}
	public int getSalaryMonth() {
		return salaryMonth;
	}
	public void setSalaryMonth(int salaryMonth) {
		this.salaryMonth = salaryMonth;
	}    
}