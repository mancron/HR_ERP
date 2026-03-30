package com.hrms.att.dto;

import java.sql.Date;
import java.sql.Time;

public class AttendanceDTO {

	private int attId;
	private int empId;
	private Date workDate;
	private Time checkIn;
	private Time checkOut;
	private double workHours;
	private String status;
	private String note;
	private String dayOfWeek;

	// 기본 생성자
	public AttendanceDTO() {
	}

	// getter / setter
	public int getAttId() {
		return attId;
	}

	public void setAttId(int attId) {
		this.attId = attId;
	}

	public int getEmpId() {
		return empId;
	}

	public void setEmpId(int empId) {
		this.empId = empId;
	}

	public Date getWorkDate() {
		return workDate;
	}

	public void setWorkDate(Date workDate) {
		this.workDate = workDate;
	}

	public Time getCheckIn() {
		return checkIn;
	}

	public void setCheckIn(Time checkIn) {
		this.checkIn = checkIn;
	}

	public Time getCheckOut() {
		return checkOut;
	}

	public void setCheckOut(Time checkOut) {
		this.checkOut = checkOut;
	}

	public double getWorkHours() {
		return workHours;
	}

	public void setWorkHours(double workHours) {
		this.workHours = workHours;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public String getDayOfWeek() {
		return dayOfWeek;
	}

	public void setDayOfWeek(String dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}
}