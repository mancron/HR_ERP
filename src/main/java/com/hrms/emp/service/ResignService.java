package com.hrms.emp.service;

import java.sql.Connection;
import com.hrms.common.db.DatabaseConnection;
import com.hrms.emp.dao.ResignDAO;
import com.hrms.emp.dto.ResignDTO;

public class ResignService {

    private ResignDAO resignDao = new ResignDAO();

    // 부서장 이름 조회
    public String getDeptManagerName(int deptId) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return resignDao.getDeptManagerName(con, deptId);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }
    
    
    public int submitResign(ResignDTO dto) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);
            int newRequestId = resignDao.insertResignRequest(con, dto);
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
    
    public int getDeptManagerId(int empId) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return resignDao.getDeptManagerId(con, empId);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }
    
    public boolean hasPendingResign(int empId) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return resignDao.hasPendingResign(con, empId);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }
    
    public String withdrawResign(int requestId, int empId) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);
            int result = resignDao.withdrawResign(con, requestId, empId);
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
}