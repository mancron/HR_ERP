package com.hrms.att.dao;

import com.hrms.common.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Time;
import java.time.LocalDate;

public class AttendanceLogDAO {

    public void insertLog(
            int empId,
            LocalDate date,
            int actorId,
            String action,
            Time oldIn,
            Time newIn,
            Time oldOut,
            Time newOut,
            String oldStatus,
            String newStatus,
            String note,
            Connection conn
    ) throws Exception {

        String sql = "INSERT INTO att_log " +
                "(emp_id, work_date, actor_id, action, " +
                "old_check_in, new_check_in, old_check_out, new_check_out, " +
                "old_status, new_status, note) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, empId);
            pstmt.setDate(2, java.sql.Date.valueOf(date));
            pstmt.setInt(3, actorId);
            pstmt.setString(4, action);

            pstmt.setTime(5, oldIn);
            pstmt.setTime(6, newIn);
            pstmt.setTime(7, oldOut);
            pstmt.setTime(8, newOut);

            pstmt.setString(9, oldStatus);
            pstmt.setString(10, newStatus);
            pstmt.setString(11, note);

            pstmt.executeUpdate();
        }
    }
}