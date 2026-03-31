package com.hrms.sys.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class HolidayDTO {

    private int           holidayId;   // PK
    private LocalDate     holidayDate; // 공휴일 날짜
    private String        holidayName; // 공휴일명
    private int           holidayYear; // 연도
    private String        dayOfWeek;   // 요일 (DB 컬럼 없음 — Service에서 계산해서 세팅)
    private LocalDateTime createdAt;
    private String holidayDateStr;
    
    
    
	public String getHolidayDateStr() {
		return holidayDateStr;
	}
	public void setHolidayDateStr(String holidayDateStr) {
		this.holidayDateStr = holidayDateStr;
	}
	public int getHolidayId() {
		return holidayId;
	}
	public void setHolidayId(int holidayId) {
		this.holidayId = holidayId;
	}
	public LocalDate getHolidayDate() {
		return holidayDate;
	}
	public void setHolidayDate(LocalDate holidayDate) {
		this.holidayDate = holidayDate;
	}
	public String getHolidayName() {
		return holidayName;
	}
	public void setHolidayName(String holidayName) {
		this.holidayName = holidayName;
	}
	public int getHolidayYear() {
		return holidayYear;
	}
	public void setHolidayYear(int holidayYear) {
		this.holidayYear = holidayYear;
	}
	public String getDayOfWeek() {
		return dayOfWeek;
	}
	public void setDayOfWeek(String dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

    
    
}