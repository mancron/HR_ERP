package com.hrms.sys.dto;

import java.time.LocalDateTime;

public class NotificationDTO {

    private long          notiId;
    private int           empId;
    private String        notiType;
    private String        refTable;
    private Integer       refId;
    private String        message;
    private int           isRead;     // 0=미읽음, 1=읽음
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    // 화면 표시용 (DB 컬럼 없음 — Service에서 세팅)
    private String        createdAtStr;  // "10분 전", "어제" 형태
    private String        badgeColor;   // noti_type별 배지 색상
    
    
    
	public long getNotiId() {
		return notiId;
	}
	public void setNotiId(long notiId) {
		this.notiId = notiId;
	}
	public int getEmpId() {
		return empId;
	}
	public void setEmpId(int empId) {
		this.empId = empId;
	}
	public String getNotiType() {
		return notiType;
	}
	public void setNotiType(String notiType) {
		this.notiType = notiType;
	}
	public String getRefTable() {
		return refTable;
	}
	public void setRefTable(String refTable) {
		this.refTable = refTable;
	}
	public Integer getRefId() {
		return refId;
	}
	public void setRefId(Integer refId) {
		this.refId = refId;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public int getIsRead() {
		return isRead;
	}
	public void setIsRead(int isRead) {
		this.isRead = isRead;
	}
	public LocalDateTime getReadAt() {
		return readAt;
	}
	public void setReadAt(LocalDateTime readAt) {
		this.readAt = readAt;
	}
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
	public String getCreatedAtStr() {
		return createdAtStr;
	}
	public void setCreatedAtStr(String createdAtStr) {
		this.createdAtStr = createdAtStr;
	}
	public String getBadgeColor() {
		return badgeColor;
	}
	public void setBadgeColor(String badgeColor) {
		this.badgeColor = badgeColor;
	}

    
}