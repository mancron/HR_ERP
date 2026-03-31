package com.hrms.auth.dto;

import java.sql.Timestamp;

/**
 * 로그인 계정(account) 테이블 DTO
 * 시스템 접근 권한 및 계정 상태(잠금, 실패 횟수 등)를 관리합니다.
 */
public class AccountDTO {
    private int accountId;          // 계정 ID (PK)
    private int empId;              // 직원 ID (FK)
    private String username;        // 로그인 ID
    private String passwordHash;    // BCrypt 해시된 비밀번호
    private String role;            // 관리자/HR담당자/일반
    private Timestamp lastLogin;    // 마지막 로그인 일시
    private int isActive;           // 1=활성, 0=비활성
    private int loginAttempts;      // 연속 로그인 실패 횟수
    private Timestamp passwordChangedAt; // 마지막 비밀번호 변경 일시
    private Timestamp lockedAt;     // 계정 잠금 일시
    private Timestamp createdAt;    // 생성일시

    // 기본 생성자
    public AccountDTO() {}

    // Getter & Setter
    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }

    public int getEmpId() { return empId; }
    public void setEmpId(int empId) { this.empId = empId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Timestamp getLastLogin() { return lastLogin; }
    public void setLastLogin(Timestamp lastLogin) { this.lastLogin = lastLogin; }

    public int getIsActive() { return isActive; }
    public void setIsActive(int isActive) { this.isActive = isActive; }

    public int getLoginAttempts() { return loginAttempts; }
    public void setLoginAttempts(int loginAttempts) { this.loginAttempts = loginAttempts; }

    public Timestamp getPasswordChangedAt() { return passwordChangedAt; }
    public void setPasswordChangedAt(Timestamp passwordChangedAt) { this.passwordChangedAt = passwordChangedAt; }

    public Timestamp getLockedAt() { return lockedAt; }
    public void setLockedAt(Timestamp lockedAt) { this.lockedAt = lockedAt; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}