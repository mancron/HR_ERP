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
    
    
    public boolean submitResign(ResignDTO dto) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);
            int result = resignDao.insertResignRequest(con, dto);
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
}