package com.hrms.att.dao;

import com.hrms.att.dto.AnnualLeaveDTO;
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
				+ "(emp_id, leave_type, half_type, start_date, end_date, days, reason, status) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, '대기')";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setInt(1, dto.getEmpId());
			pstmt.setString(2, dto.getLeaveType());
			pstmt.setString(3, dto.getHalfType());
			pstmt.setDate(4, dto.getStartDate());
			pstmt.setDate(5, dto.getEndDate());
			pstmt.setDouble(6, dto.getDays());
			pstmt.setString(7, dto.getReason());

			return pstmt.executeUpdate() > 0;

		} catch (Exception e) {
			e.printStackTrace(); // 🔥 콘솔 꼭 확인
		}

		return false;
	}

	// =========================================================
	// 2. 기간 중복 체크
	// =========================================================
	public boolean isOverlapping(int empId, Date start, Date end) {
		String sql = "SELECT COUNT(*) " + "FROM leave_request " + "WHERE emp_id = ? " + "AND status IN ('대기', '승인') "
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

		String sql = "SELECT * FROM leave_request " + "WHERE emp_id = ? " + "AND status != '취소' "
				+ "ORDER BY start_date DESC";

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
					dto.setStatus(rs.getString("status"));

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
		String sql = "UPDATE leave_request " + "SET status = '취소' "
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

	// 남은 휴가일수 조회
	public AnnualLeaveDTO getAnnualLeave(int empId) {

		String sql = "SELECT total_days, used_days, remain_days " + "FROM annual_leave "
				+ "WHERE emp_id = ? AND leave_year = YEAR(NOW())";

		AnnualLeaveDTO dto = null;

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setInt(1, empId);

			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					dto = new AnnualLeaveDTO();
					dto.setTotalDays(rs.getDouble("total_days"));
					dto.setUsedDays(rs.getDouble("used_days"));
					dto.setRemainDays(rs.getDouble("remain_days"));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return dto;
	}

	// 월별 휴가 사용 및 신청 내역
	public List<LeaveDTO> getLeaveListByMonth(int empId, int year, int month) {

		List<LeaveDTO> list = new ArrayList<>();

		String sql = "SELECT * FROM leave_request " + "WHERE emp_id = ? " + "AND status != '취소' "
				+ "AND YEAR(start_date) = ? " + "AND MONTH(start_date) = ? " + "ORDER BY start_date DESC";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setInt(1, empId);
			pstmt.setInt(2, year);
			pstmt.setInt(3, month);

			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					LeaveDTO dto = new LeaveDTO();

					dto.setLeaveId(rs.getInt("leave_id"));
					dto.setLeaveType(rs.getString("leave_type"));
					dto.setHalfType(rs.getString("half_type"));
					dto.setStartDate(rs.getDate("start_date"));
					dto.setEndDate(rs.getDate("end_date"));
					dto.setDays(rs.getDouble("days"));
					dto.setStatus(rs.getString("status"));

					list.add(dto);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	// 대기 휴가 목록
	public List<LeaveDTO> getPendingLeaves() {

	    List<LeaveDTO> list = new ArrayList<>();

	    String sql = "SELECT lr.*, e.emp_name, jp.position_name, d.dept_name " +
	             "FROM leave_request lr " +
	             "JOIN employee e ON lr.emp_id = e.emp_id " +
	             "JOIN job_position jp ON e.position_id = jp.position_id " +
	             "JOIN department d ON e.dept_id = d.dept_id " +
	             "WHERE lr.status = '대기' " +
	             "ORDER BY lr.created_at DESC";

	    try (Connection conn = DatabaseConnection.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql);
	         ResultSet rs = pstmt.executeQuery()) {

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

	            dto.setApproverId((Integer) rs.getObject("approver_id"));
	            dto.setApprovedAt(rs.getTimestamp("approved_at"));
	            dto.setRejectReason(rs.getString("reject_reason"));
	            dto.setCreatedAt(rs.getTimestamp("created_at"));

	            // 🔥 추가된 부분
	            dto.setEmpName(rs.getString("emp_name"));
	            dto.setPosition(rs.getString("position_name"));
	            dto.setDeptName(rs.getString("dept_name"));

	            list.add(dto);
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return list;
	}

	// 6. 휴가 승인 / 반려 처리
	public boolean updateLeaveStatus(int leaveId, int approverId, String status, String reason) {

		String sql = "UPDATE leave_request " + "SET status = ?, " + "    approver_id = ?, "
				+ "    approved_at = NOW(), " + "    reject_reason = ? " + "WHERE leave_id = ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, status);
			pstmt.setInt(2, approverId);

			if ("반려".equals(status)) {
				pstmt.setString(3, reason);
			} else {
				pstmt.setNull(3, Types.VARCHAR);
			}

			pstmt.setInt(4, leaveId);

			return pstmt.executeUpdate() > 0;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}
}