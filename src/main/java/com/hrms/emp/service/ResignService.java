package com.hrms.emp.service;

import java.sql.Connection;
import com.hrms.common.db.DatabaseConnection;
import com.hrms.emp.dao.ResignDAO;

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
    
    
}