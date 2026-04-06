package com.hrms.att.dto;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

public class OvertimeDTO {

    private int otId;
    private int empId;
    private Date otDate;
    private Time startTime;
    private Time endTime;
    private double otHours;
    private String reason;
    private String status;
    private int approverId;
    private Timestamp approvedAt;
    private Timestamp createdAt;
    private String empName;
    private String position;
    private String deptName;
    private String approverName;
    private String approverPosition;
    private String approverDept;
    
    
    public int getOtId() {
        return otId;
    }

    public void setOtId(int otId) {
        this.otId = otId;
    }

    public int getEmpId() {
        return empId;
    }

    public void setEmpId(int empId) {
        this.empId = empId;
    }

    public Date getOtDate() {
        return otDate;
    }

    public void setOtDate(Date otDate) {
        this.otDate = otDate;
    }

    public Time getStartTime() {
        return startTime;
    }

    public void setStartTime(Time startTime) {
        this.startTime = startTime;
    }

    public Time getEndTime() {
        return endTime;
    }

    public void setEndTime(Time endTime) {
        this.endTime = endTime;
    }

    public double getOtHours() {
        return otHours;
    }

    public void setOtHours(double otHours) {
        this.otHours = otHours;
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

    public int getApproverId() {
        return approverId;
    }

    public void setApproverId(int approverId) {
        this.approverId = approverId;
    }

	public Timestamp getApprovedAt() {
		return approvedAt;
	}

	public void setApprovedAt(Timestamp approvedAt) {
		this.approvedAt = approvedAt;
	}

	public Timestamp getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
	}

	public String getEmpName() {
		return empName;
	}

	public void setEmpName(String empName) {
		this.empName = empName;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public String getDeptName() {
		return deptName;
	}

	public void setDeptName(String deptName) {
		this.deptName = deptName;
	}

	public String getApproverName() {
		return approverName;
	}

	public void setApproverName(String approverName) {
		this.approverName = approverName;
	}

	public String getApproverPosition() {
		return approverPosition;
	}

	public void setApproverPosition(String approverPosition) {
		this.approverPosition = approverPosition;
	}

	public String getApproverDept() {
		return approverDept;
	}

	public void setApproverDept(String approverDept) {
		this.approverDept = approverDept;
	}
    
}