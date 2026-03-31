package com.hrms.sys.dto;

public class PasswordResetDTO {

    private int    empId;        // DB emp_id (INT) — JOIN 조건, Service에서 int로 사용
    private String empNo;        // 사번 (EMP001 형식) — 임시 비밀번호 생성 재료
    private String empName;      // 직원명 — JSP 아바타 첫 글자, 이름 표시용
    private String deptName;     // 부서명
    private String posName;      // 직급명
    private String tempPassword; // 평문 임시 비밀번호 (화면 1회 표시용, DB 저장 안 됨)
	public int getEmpId() {
		return empId;
	}
	public void setEmpId(int empId) {
		this.empId = empId;
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
	public String getPosName() {
		return posName;
	}
	public void setPosName(String posName) {
		this.posName = posName;
	}
	public String getTempPassword() {
		return tempPassword;
	}
	public void setTempPassword(String tempPassword) {
		this.tempPassword = tempPassword;
	}


    
    
}