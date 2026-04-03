package com.hrms.sal.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DeductionRateDTO {

    private int        rateId;
    private int        targetYear;
    private BigDecimal nationalPensionRate;     // 국민연금율
    private BigDecimal healthInsuranceRate;     // 건강보험율
    private BigDecimal longTermCareRate;        // 장기요양율 (건강보험료에 곱함)
    private BigDecimal employmentInsuranceRate; // 고용보험율
    private LocalDateTime createdAt;

    // JSP 표시용 (DB 컬럼 없음 — Service에서 세팅)
    private boolean currentYear; // 현재 연도 여부

	public int getRateId() {
		return rateId;
	}

	public void setRateId(int rateId) {
		this.rateId = rateId;
	}

	public int getTargetYear() {
		return targetYear;
	}

	public void setTargetYear(int targetYear) {
		this.targetYear = targetYear;
	}

	public BigDecimal getNationalPensionRate() {
		return nationalPensionRate;
	}

	public void setNationalPensionRate(BigDecimal nationalPensionRate) {
		this.nationalPensionRate = nationalPensionRate;
	}

	public BigDecimal getHealthInsuranceRate() {
		return healthInsuranceRate;
	}

	public void setHealthInsuranceRate(BigDecimal healthInsuranceRate) {
		this.healthInsuranceRate = healthInsuranceRate;
	}

	public BigDecimal getLongTermCareRate() {
		return longTermCareRate;
	}

	public void setLongTermCareRate(BigDecimal longTermCareRate) {
		this.longTermCareRate = longTermCareRate;
	}

	public BigDecimal getEmploymentInsuranceRate() {
		return employmentInsuranceRate;
	}

	public void setEmploymentInsuranceRate(BigDecimal employmentInsuranceRate) {
		this.employmentInsuranceRate = employmentInsuranceRate;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public boolean isCurrentYear() {
		return currentYear;
	}

	public void setCurrentYear(boolean currentYear) {
		this.currentYear = currentYear;
	}
    
}