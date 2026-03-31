package com.hrms.att.service;

import com.hrms.att.dao.LeaveDAO;
import com.hrms.att.dto.AnnualLeaveDTO;
import com.hrms.att.dto.LeaveDTO;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class LeaveService {

    private LeaveDAO leaveDAO = new LeaveDAO();
    
    // 휴가 신청 처리
    public String applyLeave(LeaveDTO dto) {

        int empId = dto.getEmpId();
        Date startDate = dto.getStartDate();
        Date endDate = dto.getEndDate();
        double days = dto.getDays();
        String reason = dto.getReason();

        if (!"반차".equals(dto.getLeaveType())) {
            dto.setHalfType(null);
        }
        
        // 🔥 1. 사유 공백 체크
        if (reason == null || reason.trim().isEmpty()) {
            return "empty_reason";
        }

        // 🔥 2. 날짜 검증
        if (startDate.after(endDate)) {
            return "invalid_date";
        }

        // 3. 잔여 연차 체크
        double remainDays = leaveDAO.getRemainDays(empId);
        if (days > remainDays) {
            return "not_enough";
        }

        // 4. 기간 중복 체크
        if (leaveDAO.isOverlapping(empId, startDate, endDate)) {
            return "overlap";
        }

        // 5. DB 저장
        boolean result = leaveDAO.insertLeave(dto);
        if (!result) {
            return "fail";
        }

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
    
    //휴가 리스트 출력
    public List<LeaveDTO> getLeaveList(int empId) {
        return leaveDAO.getLeaveList(empId);
    }
    
    //월별 휴가 리스트 출력
    public List<LeaveDTO> getLeaveListByMonth(int empId, int year, int month) {
        return leaveDAO.getLeaveListByMonth(empId, year, month);
    }
    
    //신청 취소
    public boolean cancelLeave(int leaveId, int empId) {
        return leaveDAO.cancelLeave(leaveId, empId);
    }
    
    //승인 대기 목록 조회
    public List<LeaveDTO> getPendingLeaves() {
        return leaveDAO.getPendingLeaves();
    }
    
    //휴가 승인 및 반려
    public boolean updateLeaveStatus(int leaveId, int approverId, String status, String reason) {
        return leaveDAO.updateLeaveStatus(leaveId, approverId, status, reason);
    }
    
}
