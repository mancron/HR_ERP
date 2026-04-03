package com.hrms.att.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.hrms.att.dto.OvertimeDTO;

public class OvertimeDAO {

	//초과근무 신청
	public void insertOvertime(Connection conn, OvertimeDTO dto) {
	    String sql = "INSERT INTO overtime_request " +
	            "(emp_id, ot_date, start_time, end_time, ot_hours, reason, status, approver_id) " +
	            "VALUES (?, ?, ?, ?, ?, ?, '대기', ?)";

	    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

	        pstmt.setInt(1, dto.getEmpId());
	        pstmt.setDate(2, dto.getOtDate());
	        pstmt.setTime(3, dto.getStartTime());
	        pstmt.setTime(4, dto.getEndTime());
	        pstmt.setDouble(5, dto.getOtHours());
	        pstmt.setString(6, dto.getReason());
	        pstmt.setInt(7, dto.getApproverId());

	        pstmt.executeUpdate();

	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }
	}
	
	//초과근무 승인, 반려
	public void updateStatus(Connection conn, int otId, String status) {
	    String sql = "UPDATE overtime_request SET status=?, approved_at=NOW() WHERE ot_id=?";

	    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
	        pstmt.setString(1, status);
	        pstmt.setInt(2, otId);
	        pstmt.executeUpdate();
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }
	}
	
	//승인 대상 조회
	public OvertimeDTO findById(Connection conn, int otId) {

	    String sql = "SELECT * FROM overtime_request WHERE ot_id = ?";

	    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

	        pstmt.setInt(1, otId);

	        try (ResultSet rs = pstmt.executeQuery()) {

	            if (rs.next()) {
	                OvertimeDTO dto = new OvertimeDTO();

	                dto.setOtId(rs.getInt("ot_id"));
	                dto.setEmpId(rs.getInt("emp_id"));
	                dto.setOtDate(rs.getDate("ot_date"));
	                dto.setStartTime(rs.getTime("start_time"));
	                dto.setEndTime(rs.getTime("end_time"));
	                dto.setOtHours(rs.getDouble("ot_hours"));
	                dto.setReason(rs.getString("reason"));
	                dto.setStatus(rs.getString("status"));
	                dto.setApproverId(rs.getInt("approver_id"));

	                return dto;
	            }
	        }

	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }

	    return null;
	}
	
}
