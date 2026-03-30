package com.hrms.att.dto;

public class AttendanceSummaryDTO {

	private int workDays;
	private double totalHours;
	private int attendCount;
	private int lateCount;
	private int absentCount;
	private int noCheckoutCount;

	public int getWorkDays() {
		return workDays;
	}

	public void setWorkDays(int workDays) {
		this.workDays = workDays;
	}

	public double getTotalHours() {
		return totalHours;
	}

	public void setTotalHours(double totalHours) {
		this.totalHours = totalHours;
	}

	public int getAttendCount() {
		return attendCount;
	}

	public void setAttendCount(int attendCount) {
		this.attendCount = attendCount;
	}

	public int getLateCount() {
		return lateCount;
	}

	public void setLateCount(int lateCount) {
		this.lateCount = lateCount;
	}

	public int getAbsentCount() {
		return absentCount;
	}

	public void setAbsentCount(int absentCount) {
		this.absentCount = absentCount;
	}

	public int getNoCheckoutCount() {
		return noCheckoutCount;
	}

	public void setNoCheckoutCount(int noCheckoutCount) {
		this.noCheckoutCount = noCheckoutCount;
	}
}
