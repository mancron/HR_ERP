package com.hrms.att.dto;

public class AnnualLeaveDTO {

    private double totalDays;
    private double usedDays;
    private double remainDays;

    public double getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(double totalDays) {
        this.totalDays = totalDays;
    }

    public double getUsedDays() {
        return usedDays;
    }

    public void setUsedDays(double usedDays) {
        this.usedDays = usedDays;
    }

    public double getRemainDays() {
        return remainDays;
    }

    public void setRemainDays(double remainDays) {
        this.remainDays = remainDays;
    }
}