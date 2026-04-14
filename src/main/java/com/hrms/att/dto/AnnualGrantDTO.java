package com.hrms.att.dto;

import java.time.LocalDate;

public class AnnualGrantDTO {
	
	private int empId;
    private String empName;
    private String deptName;
    private String positionName;
    private LocalDate hireDate;
    private int years;
    private int annualDays;
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
	public LocalDate getHireDate() {
		return hireDate;
	}
	public void setHireDate(LocalDate hireDate) {
		this.hireDate = hireDate;
	}
	public int getYears() {
		return years;
	}
	public void setYears(int years) {
		this.years = years;
	}
	public int getAnnualDays() {
		return annualDays;
	}
	public void setAnnualDays(int annualDays) {
		this.annualDays = annualDays;
	}
}
