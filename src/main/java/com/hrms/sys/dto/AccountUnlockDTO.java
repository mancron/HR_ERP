package com.hrms.sys.dto;

public class AccountUnlockDTO {
	
	private int accountId;
	private String empNo;
	private String empName;
	private String deptName;
	private String lockedAtStr;
	private int loginAttempts;
	
	public int getAccountId() {
		return accountId;
	}
	public void setAccountId(int accountId) {
		this.accountId = accountId;
	}
	public String getEmpNo() {
		return empNo;
	}
	public void setEmpNo(String empNo) {
		this.empNo = empNo;
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
	public String getLockedAtStr() {
		return lockedAtStr;
	}
	public void setLockedAtStr(String lockedAtStr) {
		this.lockedAtStr = lockedAtStr;
	}
	public int getLoginAttempts() {
		return loginAttempts;
	}
	public void setLoginAttempts(int loginAttempts) {
		this.loginAttempts = loginAttempts;
	}
	
}
