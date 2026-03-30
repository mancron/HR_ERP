package com.hrms.att.service;

import com.hrms.att.dao.LeaveDAO;
import com.hrms.att.dto.AnnualLeaveDTO;
import com.hrms.att.dto.LeaveDTO;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class LeaveService {

    private LeaveDAO leaveDAO = new LeaveDAO();
    
    // 휴가 신청 처리
    public String applyLeave(LeaveDTO dto) {

        int empId = dto.getEmpId();
        Date startDate = dto.getStartDate();
        Date endDate = dto.getEndDate();
        double days = dto.getDays();

        // 1. 잔여 연차 체크
        double remainDays = leaveDAO.getRemainDays(empId);
        if (days > remainDays) {
            return "not_enough";
        }

        // 2. 기간 중복 체크
        if (leaveDAO.isOverlapping(empId, startDate, endDate)) {
            return "overlap";
        }

        // 3. DB 저장
        boolean result = leaveDAO.insertLeave(dto);
        if (!result) {
            return "fail";
        }

        // 4. 성공
        return "success";
    }
    
    public AnnualLeaveDTO getAnnualLeave(int empId) {
        return leaveDAO.getAnnualLeave(empId);
    }
    
    public double calculateDays(LocalDate start, LocalDate end, String leaveType) {

        long daysBetween = ChronoUnit.DAYS.between(start, end) + 1;

        if ("반차".equals(leaveType)) {
            return 0.5;
        }

        return daysBetween;
    }
}
