package com.hrms.emp.dto;

public class TransferDTO {
    private String emp_no;          // 사원번호
    private String transfer_type;   // 발령유형 (입력값)
    private String transfer_date;   // 발령일자 (입력값)
    private String reason;          // 사유 (입력값)
    
    // 부서 정보
    private int prev_dept_id;       // 기존 부서 ID
    private int target_dept_id;     // 발령 부서 ID
    
    // 직급 정보
    private int prev_position_id;   // 기존 직급 ID
    private int target_position_id; // 변경 직급 ID

    public TransferDTO() {}

    // Getter / Setter
    public String getEmp_no() { return emp_no; }
    public void setEmp_no(String emp_no) { this.emp_no = emp_no; }
    public String getTransfer_type() { return transfer_type; }
    public void setTransfer_type(String transfer_type) { this.transfer_type = transfer_type; }
    public String getTransfer_date() { return transfer_date; }
    public void setTransfer_date(String transfer_date) { this.transfer_date = transfer_date; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public int getPrev_dept_id() { return prev_dept_id; }
    public void setPrev_dept_id(int prev_dept_id) { this.prev_dept_id = prev_dept_id; }
    public int getTarget_dept_id() { return target_dept_id; }
    public void setTarget_dept_id(int target_dept_id) { this.target_dept_id = target_dept_id; }
    public int getPrev_position_id() { return prev_position_id; }
    public void setPrev_position_id(int prev_position_id) { this.prev_position_id = prev_position_id; }
    public int getTarget_position_id() { return target_position_id; }
    public void setTarget_position_id(int target_position_id) { this.target_position_id = target_position_id; }
}