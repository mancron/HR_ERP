package com.hrms.sal.dto;

import java.math.BigDecimal;

public class SalaryCalcDTO {

    private int        salaryId;
    private int        empId;
    private String     empName;
    private String     deptName;
    private String     positionName;

    // 지급 항목
    private int        baseSalary;
    private int        mealAllowance;
    private int        transportAllowance;
    private int        positionAllowance;
    private int        overtimePay;
    private int        otherAllowance;
    private int        grossSalary;

    // 공제 항목
    private int        nationalPension;
    private int        healthInsurance;
    private int        longTermCare;
    private int        employmentInsurance;
    private BigDecimal unpaidLeaveDays;
    private int        unpaidDeduction;
    private int        incomeTax;
    private int        localIncomeTax;
    private int        totalDeduction;
    private int        netSalary;

    private String     payDate;
    private String     status;      // 대기 / 완료
    
    private int salaryYear;
    private int salaryMonth;
    
    
    
    
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
	public int getMealAllowance() {
		return mealAllowance;
	}
	public void setMealAllowance(int mealAllowance) {
		this.mealAllowance = mealAllowance;
	}
	public int getTransportAllowance() {
		return transportAllowance;
	}
	public void setTransportAllowance(int transportAllowance) {
		this.transportAllowance = transportAllowance;
	}
	public int getPositionAllowance() {
		return positionAllowance;
	}
	public void setPositionAllowance(int positionAllowance) {
		this.positionAllowance = positionAllowance;
	}
	public int getOvertimePay() {
		return overtimePay;
	}
	public void setOvertimePay(int overtimePay) {
		this.overtimePay = overtimePay;
	}
	public int getOtherAllowance() {
		return otherAllowance;
	}
	public void setOtherAllowance(int otherAllowance) {
		this.otherAllowance = otherAllowance;
	}
	public int getGrossSalary() {
		return grossSalary;
	}
	public void setGrossSalary(int grossSalary) {
		this.grossSalary = grossSalary;
	}
	public int getNationalPension() {
		return nationalPension;
	}
	public void setNationalPension(int nationalPension) {
		this.nationalPension = nationalPension;
	}
	public int getHealthInsurance() {
		return healthInsurance;
	}
	public void setHealthInsurance(int healthInsurance) {
		this.healthInsurance = healthInsurance;
	}
	public int getLongTermCare() {
		return longTermCare;
	}
	public void setLongTermCare(int longTermCare) {
		this.longTermCare = longTermCare;
	}
	public int getEmploymentInsurance() {
		return employmentInsurance;
	}
	public void setEmploymentInsurance(int employmentInsurance) {
		this.employmentInsurance = employmentInsurance;
	}
	public BigDecimal getUnpaidLeaveDays() {
		return unpaidLeaveDays;
	}
	public void setUnpaidLeaveDays(BigDecimal unpaidLeaveDays) {
		this.unpaidLeaveDays = unpaidLeaveDays;
	}
	public int getUnpaidDeduction() {
		return unpaidDeduction;
	}
	public void setUnpaidDeduction(int unpaidDeduction) {
		this.unpaidDeduction = unpaidDeduction;
	}
	public int getIncomeTax() {
		return incomeTax;
	}
	public void setIncomeTax(int incomeTax) {
		this.incomeTax = incomeTax;
	}
	public int getLocalIncomeTax() {
		return localIncomeTax;
	}
	public void setLocalIncomeTax(int localIncomeTax) {
		this.localIncomeTax = localIncomeTax;
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

    
}