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
    public boolean insertLeaveRequest(LeaveDTO dto) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);
            int result = leaveDao.insertLeaveRequest(con, dto);
            if (result > 0) {
                con.commit();
                return true;
            } else {
                con.rollback();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            try { if (con != null) con.rollback(); } catch (Exception ex) { ex.printStackTrace(); }
            return false;
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }
    
    // 휴직/복직 신청 철회
    public String withdrawLeave(int requestId, int empId) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            int result = leaveDao.withdrawLeave(con, requestId, empId);
            return result > 0 ? "철회가 완료되었습니다." : "철회할 수 없습니다. (대기 상태만 가능)";
        } catch (Exception e) {
            e.printStackTrace();
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
}