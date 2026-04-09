package com.hrms.emp.service;

import java.sql.Connection;
import com.hrms.common.db.DatabaseConnection;
import com.hrms.emp.dao.LeaveDAO;
import com.hrms.emp.dto.LeaveDTO;

public class LeaveService {

    private LeaveDAO leaveDao = new LeaveDAO();

    // 부서장 이름 조회
    public String getDeptManagerName(int deptId) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return leaveDao.getDeptManagerName(con, deptId);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }
    
    // 부서장 emp_id 조회 (INSERT 시 dept_manager_id 세팅용)
    public int getDeptManagerId(int empId) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return leaveDao.getDeptManagerId(con, empId);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }
    
    // 휴직/복직 신청 INSERT
    public int insertLeaveRequest(LeaveDTO dto) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);
            int newRequestId = leaveDao.insertLeaveRequest(con, dto);
            if (newRequestId > 0) {
                con.commit();
                return newRequestId; // ← request_id 반환
            } else {
                con.rollback();
                return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            try { if (con != null) con.rollback(); } catch (Exception ex) { ex.printStackTrace(); }
            return 0;
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }
    
    // 휴직/복직 신청 철회
    public String withdrawLeave(int requestId, int empId) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);
            int result = leaveDao.withdrawLeave(con, requestId, empId);
            if (result > 0) {
                con.commit();
                return "철회가 완료되었습니다.";
            } else {
                con.rollback();
                return "철회할 수 없습니다. (대기 상태만 가능)";
            }
        } catch (Exception e) {
            e.printStackTrace();
            try { if (con != null) con.rollback(); } catch (Exception ex) { ex.printStackTrace(); }
            return "오류가 발생했습니다.";
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }
    
    public LeaveDTO getLeaveById(int requestId, int empId) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return leaveDao.getLeaveById(con, requestId, empId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public boolean updateLeaveRequest(LeaveDTO dto) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);
            int result = leaveDao.updateLeaveRequest(con, dto);
            if (result > 0) { con.commit(); return true; }
            else { con.rollback(); return false; }
        } catch (Exception e) {
            e.printStackTrace();
            try { if (con != null) con.rollback(); } catch (Exception ex) { ex.printStackTrace(); }
            return false;
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }
    
    public boolean hasPendingLeave(int empId) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return leaveDao.hasPendingLeave(con, empId);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }
    
    // 현재 상태에 따른 신청 유형 유효성 검사
    public String validateLeaveType(int empId, String leaveType) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            String currentStatus = leaveDao.getEmpStatus(con, empId);
            if (currentStatus == null) return "직원 정보를 찾을 수 없습니다.";
            if ("재직".equals(currentStatus) && "복직".equals(leaveType))
                return "재직 중에는 복직 신청을 할 수 없습니다.";
            if ("휴직".equals(currentStatus) && "휴직".equals(leaveType))
                return "휴직 중에는 휴직 신청을 할 수 없습니다.";
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }
}