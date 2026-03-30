package com.hrms.att.dao;

import com.hrms.att.dto.LeaveDTO;
import com.hrms.common.db.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LeaveDAO {

    // =========================================================
    // 1. 휴가 신청 (INSERT)
    // =========================================================
    public boolean insertLeave(LeaveDTO dto) {
        String sql = "INSERT INTO leave_request "
                + "(emp_id, leave_type, half_type, start_date, end_date, days, reason, status, approver_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, '대기', ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, dto.getEmpId());
            pstmt.setString(2, dto.getLeaveType());
            pstmt.setString(3, dto.getHalfType());
            pstmt.setDate(4, dto.getStartDate());
            pstmt.setDate(5, dto.getEndDate());
            pstmt.setDouble(6, dto.getDays());
            pstmt.setString(7, dto.getReason());

            if (dto.getApproverId() != null) {
                pstmt.setInt(8, dto.getApproverId());
            } else {
                pstmt.setNull(8, Types.INTEGER);
            }

            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // =========================================================
    // 2. 기간 중복 체크
    // =========================================================
    public boolean isOverlapping(int empId, Date start, Date end) {
        String sql = "SELECT COUNT(*) "
                + "FROM leave_request "
                + "WHERE emp_id = ? "
                + "AND status IN ('대기', '승인') "
                + "AND (start_date <= ? AND end_date >= ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, empId);
            pstmt.setDate(2, end);
            pstmt.setDate(3, start);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // =========================================================
    // 3. 잔여 연차 조회
    // =========================================================
    public double getRemainDays(int empId) {
        String sql = "SELECT remain_days FROM annual_leave WHERE emp_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, empId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("remain_days");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    // =========================================================
    // 4. 내 휴가 목록 조회
    // =========================================================
    public List<LeaveDTO> getLeaveList(int empId) {
        List<LeaveDTO> list = new ArrayList<>();

        String sql = "SELECT * FROM leave_request WHERE emp_id = ? ORDER BY leave_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, empId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    LeaveDTO dto = new LeaveDTO();

                    dto.setLeaveId(rs.getInt("leave_id"));
                    dto.setEmpId(rs.getInt("emp_id"));
                    dto.setLeaveType(rs.getString("leave_type"));
                    dto.setHalfType(rs.getString("half_type"));
                    dto.setStartDate(rs.getDate("start_date"));
                    dto.setEndDate(rs.getDate("end_date"));
                    dto.setDays(rs.getDouble("days"));
                    dto.setReason(rs.getString("reason"));
                    dto.setStatus(rs.getString("status"));

                    int approverId = rs.getInt("approver_id");
                    if (!rs.wasNull()) {
                        dto.setApproverId(approverId);
                    }

                    dto.setApprovedAt(rs.getTimestamp("approved_at"));
                    dto.setRejectReason(rs.getString("reject_reason"));
                    dto.setCreatedAt(rs.getTimestamp("created_at"));

                    list.add(dto);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // =========================================================
    // 5. 휴가 취소 (대기 상태만)
    // =========================================================
    public boolean cancelLeave(int leaveId, int empId) {
        String sql = "UPDATE leave_request "
                + "SET status = '취소' "
                + "WHERE leave_id = ? AND emp_id = ? AND status = '대기'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, leaveId);
            pstmt.setInt(2, empId);

            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}