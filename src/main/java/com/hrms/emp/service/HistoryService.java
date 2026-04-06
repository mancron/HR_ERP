package com.hrms.emp.service;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import com.hrms.common.db.DatabaseConnection;
import com.hrms.emp.dao.HistoryDAO;
import com.hrms.emp.dto.HistoryDTO;

public class HistoryService {

    private HistoryDAO historyDao = new HistoryDAO();

    // 특정 직원의 인사발령 이력 조회
    public List<HistoryDTO> getHistoryByEmpId(int empId) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return historyDao.getHistoryByEmpId(con, empId);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }
    
    public List<HistoryDTO> getHistoryList(String keyword, String changeType, String yearMonth) {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            return historyDao.getHistoryList(con, keyword, changeType, yearMonth);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }
    
    
}