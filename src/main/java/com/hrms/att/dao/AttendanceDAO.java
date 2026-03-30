package com.hrms.att.dao;

import com.hrms.att.dto.AttendanceDTO;
import com.hrms.common.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AttendanceDAO {

    // 1. 오늘 출퇴근 조회
    public AttendanceDTO getTodayAttendance(int empId) {
        String sql = "SELECT * FROM attendance WHERE emp_id = ? AND work_date = CURDATE()";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, empId);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    AttendanceDTO dto = new AttendanceDTO();

                    dto.setAttId(rs.getInt("att_id"));
                    dto.setEmpId(rs.getInt("emp_id"));
                    dto.setWorkDate(rs.getDate("work_date"));
                    dto.setCheckIn(rs.getTime("check_in"));
                    dto.setCheckOut(rs.getTime("check_out"));
                    dto.setWorkHours(rs.getDouble("work_hours"));
                    dto.setStatus(rs.getString("status"));
                    dto.setNote(rs.getString("note"));

                    return dto;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // 2. 출근 처리
    public void checkIn(int empId, String status) {
        String sql = "INSERT INTO attendance (emp_id, work_date, check_in, status) VALUES (?, CURDATE(), NOW(), ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, empId);
            pstmt.setString(2, status);

            pstmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 3. 퇴근 처리
    public void checkOut(int empId, double workHours) {
        String sql = "UPDATE attendance SET check_out = NOW(), work_hours = ? WHERE emp_id = ? AND work_date = CURDATE()";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, workHours);
            pstmt.setInt(2, empId);

            pstmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 4. 공휴일 체크
    public boolean isHoliday() {
        String sql = "SELECT 1 FROM public_holiday WHERE holiday_date = CURDATE()";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            return rs.next();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
    
    public List<AttendanceDTO> getMonthlyAttendance(int empId, String yearMonth) {

        String sql = "SELECT * FROM attendance " +
                     "WHERE emp_id = ? AND DATE_FORMAT(work_date, '%Y-%m') = ? " +
                     "ORDER BY work_date";

        List<AttendanceDTO> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, empId);
            pstmt.setString(2, yearMonth);

            try (ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    AttendanceDTO dto = new AttendanceDTO();

                    dto.setAttId(rs.getInt("att_id"));
                    dto.setEmpId(rs.getInt("emp_id"));
                    dto.setWorkDate(rs.getDate("work_date"));
                    dto.setCheckIn(rs.getTime("check_in"));
                    dto.setCheckOut(rs.getTime("check_out"));
                    dto.setWorkHours(rs.getDouble("work_hours"));
                    dto.setStatus(rs.getString("status"));

                    list.add(dto);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}