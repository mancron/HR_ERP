package com.hrms.att.dto;

public class AnnualLeaveDTO {

    private double totalDays;
    private double usedDays;
    private double remainDays;
    private int empId;
    private String empName;
    private String deptName;

    public double getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(double totalDays) {
        this.totalDays = totalDays;
    }

    public double getUsedDays() {
        return usedDays;
    }

    public void setUsedDays(double usedDays) {
        this.usedDays = usedDays;
    }

    public double getRemainDays() {
        return remainDays;
    }

    public void setRemainDays(double remainDays) {
        this.remainDays = remainDays;
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
	
	public double getUsageRate() {
	    if (totalDays == 0) return 0;
	    return (usedDays / totalDays) * 100;
	}
}