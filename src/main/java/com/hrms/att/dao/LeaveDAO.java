package com.hrms.att.dao;

import com.hrms.att.dto.AnnualLeaveDTO;
import com.hrms.att.dto.LeaveDTO;
import com.hrms.common.db.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LeaveDAO {

	// 1. 휴가 신청 (INSERT)
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

	// 2. 기간 중복 체크
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

	// 3. 잔여 연차 조회
	public double getRemainDays(int empId) {
		String sql = "SELECT remain_days FROM annual_leave WHERE emp_id = ? AND leave_year = YEAR(NOW())";

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

	// 4. 내 휴가 목록 조회
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

	// 5. 휴가 취소 (대기 상태만)
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
	public List<LeaveDTO> getPendingLeaves(String dept, String sort, String startDate, String endDate) {

	    List<LeaveDTO> list = new ArrayList<>();

	    StringBuilder sql = new StringBuilder();

	    sql.append("SELECT lr.*, e.emp_name, jp.position_name, d.dept_name ");
	    sql.append("FROM leave_request lr ");
	    sql.append("JOIN employee e ON lr.emp_id = e.emp_id ");
	    sql.append("JOIN job_position jp ON e.position_id = jp.position_id ");
	    sql.append("JOIN department d ON e.dept_id = d.dept_id ");
	    sql.append("WHERE lr.status = '대기' ");

	    // 🔥 1. 부서 필터
	    if (dept != null && !dept.isEmpty()) {
	        sql.append("AND d.dept_name = ? ");
	    }

	    // 🔥 2. 기간 필터 (겹침 기준)
	    if (startDate != null && endDate != null &&
	        !startDate.isEmpty() && !endDate.isEmpty()) {

	        sql.append("AND lr.start_date <= ? ");
	        sql.append("AND lr.end_date >= ? ");
	    }

	    // 🔥 3. 정렬
	    if ("name_asc".equals(sort)) {
	        sql.append("ORDER BY e.emp_name ASC ");
	    } else if ("name_desc".equals(sort)) {
	        sql.append("ORDER BY e.emp_name DESC ");
	    } else if ("position_asc".equals(sort)) {
	        sql.append("ORDER BY jp.position_level ASC ");
	    } else if ("position_desc".equals(sort)) {
	        sql.append("ORDER BY jp.position_level DESC ");
	    } else {
	        sql.append("ORDER BY lr.created_at DESC ");
	    }

	    try (Connection conn = DatabaseConnection.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

	        int idx = 1;

	        // 🔥 4. 파라미터 세팅 순서 중요
	        if (dept != null && !dept.isEmpty()) {
	            pstmt.setString(idx++, dept);
	        }

	        if (startDate != null && endDate != null &&
	            !startDate.isEmpty() && !endDate.isEmpty()) {

	            pstmt.setDate(idx++, Date.valueOf(endDate));   // <= endDate
	            pstmt.setDate(idx++, Date.valueOf(startDate)); // >= startDate
	        }

	        ResultSet rs = pstmt.executeQuery();

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
	public boolean updateLeaveStatus(Connection conn, int leaveId, int approverId, String status, String reason) throws Exception {

	    String sql = "UPDATE leave_request " +
	                 "SET status = ?, " +
	                 "    approver_id = ?, " +
	                 "    approved_at = NOW(), " +
	                 "    reject_reason = ? " +
	                 "WHERE leave_id = ?";

	    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

	        pstmt.setString(1, status);
	        pstmt.setInt(2, approverId);

	        if ("반려".equals(status)) {
	            pstmt.setString(3, reason);
	        } else {
	            pstmt.setNull(3, Types.VARCHAR);
	        }

	        pstmt.setInt(4, leaveId);

	        return pstmt.executeUpdate() > 0;
	    }
	}

	// 대기 휴가 목록 -> 부서명 추출
	public List<String> getPendingDeptList() {

		List<String> list = new ArrayList<>();

		String sql = "SELECT DISTINCT d.dept_name " + "FROM leave_request lr "
				+ "JOIN employee e ON lr.emp_id = e.emp_id " + "JOIN department d ON e.dept_id = d.dept_id "
				+ "WHERE lr.status = '대기'";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql);
				ResultSet rs = pstmt.executeQuery()) {

			while (rs.next()) {
				list.add(rs.getString("dept_name"));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}
	
	//잔여 연차 값 업데이트
	public void updateAnnualLeave(Connection conn, int empId, double days) throws Exception {
		String sql = "UPDATE annual_leave SET "
		           + "used_days = used_days + ?, "
		           + "remain_days = remain_days - ? "
		           + "WHERE emp_id = ? "
		           + "AND leave_year = YEAR(NOW())";

	    try (PreparedStatement ps = conn.prepareStatement(sql)) {

	        ps.setDouble(1, days);
	        ps.setDouble(2, days);
	        ps.setInt(3, empId);

	        int result = ps.executeUpdate();

	        if (result == 0) {
	            throw new Exception("연차 정보 없음");
	        }
	    }
	}
	
	//휴가 상세 정보 가져오기
	public LeaveDTO getLeaveById(Connection conn, int leaveId) throws Exception {

	    String sql = "SELECT emp_id, start_date, end_date, leave_type " +
	                 "FROM leave_request WHERE leave_id = ?";

	    try (PreparedStatement ps = conn.prepareStatement(sql)) {

	        ps.setInt(1, leaveId);
	        ResultSet rs = ps.executeQuery();

	        if (rs.next()) {
	            LeaveDTO dto = new LeaveDTO();
	            dto.setEmpId(rs.getInt("emp_id"));
	            dto.setStartDate(rs.getDate("start_date"));
	            dto.setEndDate(rs.getDate("end_date"));
	            dto.setLeaveType(rs.getString("leave_type"));
	            return dto;
	        }
	    }

	    return null;
	}
}