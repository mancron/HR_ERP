package com.hrms.att.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.hrms.common.db.DatabaseConnection;

public class AnnualDAO {

    public boolean existsAnnual(int empId, int year) {

        String sql = "SELECT 1 FROM annual_leave WHERE emp_id=? AND leave_year=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, empId);
            ps.setInt(2, year);

            ResultSet rs = ps.executeQuery();
            return rs.next();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
    
    //연차 부여
    public void insertAnnualLeave(int empId, int year, int totalDays) {

        String sql = "INSERT INTO annual_leave "
            + "(emp_id, leave_year, total_days, used_days, remain_days) "
            + "VALUES (?, ?, ?, 0, ?) ";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, empId);
            ps.setInt(2, year);
            ps.setInt(3, totalDays);
            ps.setInt(4, totalDays); // remain_days = total

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public int getTotalDays(int empId, int year) {

        String sql = "SELECT total_days FROM annual_leave WHERE emp_id=? AND leave_year=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, empId);
            ps.setInt(2, year);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("total_days");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
}