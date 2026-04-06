package com.hrms.att.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.hrms.att.dto.OvertimeDTO;
import com.hrms.common.db.DatabaseConnection;

public class OvertimeDAO {

	// 초과근무 신청
	public void insertOvertime(Connection conn, OvertimeDTO dto) {
		String sql = "INSERT INTO overtime_request "
				+ "(emp_id, ot_date, start_time, end_time, ot_hours, reason, status, approver_id) "
				+ "VALUES (?, ?, ?, ?, ?, ?, '대기', ?)";

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

	// 초과근무 승인, 반려
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

	// 승인 대상 조회
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

	// 초과근무 신청 리스트
	public List<OvertimeDTO> getMyList(int empId) {

		List<OvertimeDTO> list = new ArrayList<OvertimeDTO>();

		String sql = "SELECT ot_id, emp_id, ot_date, start_time, end_time, "
				+ "ot_hours, reason, status, approver_id, approved_at, created_at " + "FROM overtime_request "
				+ "WHERE emp_id = ? " + "ORDER BY created_at DESC";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setInt(1, empId);

			try (ResultSet rs = pstmt.executeQuery()) {

				while (rs.next()) {

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

					Timestamp approvedAt = rs.getTimestamp("approved_at");
					if (approvedAt != null) {
						dto.setApprovedAt(approvedAt);
					}

					Timestamp createdAt = rs.getTimestamp("created_at");
					if (createdAt != null) {
						dto.setCreatedAt(createdAt);
					}

					list.add(dto);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("초과근무 목록 조회 실패", e);
		}

		return list;
	}

	// 초과근무 월별 신청 리스트
	public List<OvertimeDTO> getMyListByMonth(int empId, int year, int month) {

		List<OvertimeDTO> list = new ArrayList<>();

		String sql = "SELECT * FROM overtime_request " + "WHERE emp_id = ? " + "AND YEAR(ot_date) = ? "
				+ "AND MONTH(ot_date) = ? " + "AND status != '취소' " + "ORDER BY created_at DESC";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setInt(1, empId);
			pstmt.setInt(2, year);
			pstmt.setInt(3, month);

			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) {
				OvertimeDTO dto = new OvertimeDTO();

				dto.setOtId(rs.getInt("ot_id"));
				dto.setEmpId(rs.getInt("emp_id"));
				dto.setOtDate(rs.getDate("ot_date"));
				dto.setStartTime(rs.getTime("start_time"));
				dto.setEndTime(rs.getTime("end_time"));
				dto.setOtHours(rs.getDouble("ot_hours"));
				dto.setReason(rs.getString("reason"));
				dto.setStatus(rs.getString("status"));

				list.add(dto);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	// 초과근무 상세정보 불러오기
	public OvertimeDTO findById(int id) {

		String sql = "SELECT o.*, " + "       e.emp_name, d.dept_name, jp.position_name, "
				+ "       a.emp_name AS approver_name, " + "       ajp.position_name AS approver_position, "
				+ "       ad.dept_name AS approver_dept " + "FROM overtime_request o "
				+ "LEFT JOIN employee e ON o.emp_id = e.emp_id " + "LEFT JOIN department d ON e.dept_id = d.dept_id "
				+ "LEFT JOIN job_position jp ON e.position_id = jp.position_id " + // 🔥 수정
				"LEFT JOIN employee a ON o.approver_id = a.emp_id "
				+ "LEFT JOIN department ad ON a.dept_id = ad.dept_id "
				+ "LEFT JOIN job_position ajp ON a.position_id = ajp.position_id " + // 🔥 수정
				"WHERE o.ot_id = ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setInt(1, id);
			ResultSet rs = pstmt.executeQuery();

			if (rs.next()) {

				OvertimeDTO dto = new OvertimeDTO();

				// 기본
				dto.setOtId(rs.getInt("ot_id"));
				dto.setEmpId(rs.getInt("emp_id"));
				dto.setOtDate(rs.getDate("ot_date"));
				dto.setStartTime(rs.getTime("start_time"));
				dto.setEndTime(rs.getTime("end_time"));
				dto.setOtHours(rs.getDouble("ot_hours"));
				dto.setReason(rs.getString("reason"));
				dto.setStatus(rs.getString("status"));
				dto.setApproverId(rs.getInt("approver_id"));

				// 시간
				dto.setApprovedAt(rs.getTimestamp("approved_at"));
				dto.setCreatedAt(rs.getTimestamp("created_at"));

				// 신청자
				dto.setEmpName(rs.getString("emp_name"));
				dto.setDeptName(rs.getString("dept_name"));
				dto.setPosition(rs.getString("position_name")); // 🔥 정확

				// 승인자
				dto.setApproverName(rs.getString("approver_name"));
				dto.setApproverDept(rs.getString("approver_dept"));
				dto.setApproverPosition(rs.getString("approver_position"));

				return dto;
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("초과근무 상세 조회 실패", e);
		}

		return null;
	}

	public boolean cancel(int id, int empId) {

		String sql = "UPDATE overtime_request " + "SET status = '취소' " + "WHERE ot_id = ? " + "AND emp_id = ? "
				+ "AND status = '대기'";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setInt(1, id);
			pstmt.setInt(2, empId);

			int result = pstmt.executeUpdate();

			return result > 0;

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("초과근무 취소 실패", e);
		}
	}

	public List<OvertimeDTO> getPendingList(Connection conn, String dept, String sort, String startDate, String endDate,
			int approverId) {

		List<OvertimeDTO> list = new ArrayList<>();

		StringBuilder sql = new StringBuilder();

		sql.append("SELECT o.*, e.emp_name, d.dept_name, jp.position_name " + "FROM overtime_request o "
				+ "JOIN employee e ON o.emp_id = e.emp_id " + "JOIN department d ON e.dept_id = d.dept_id "
				+ "JOIN job_position jp ON e.position_id = jp.position_id " + "WHERE o.status = '대기' "
				+ "AND o.approver_id = ? ");

// 🔥 동적 조건
		if (dept != null && !dept.isEmpty()) {
			sql.append(" AND d.dept_name = ? ");
		}

		if (startDate != null && !startDate.isEmpty()) {
			sql.append(" AND o.ot_date >= ? ");
		}

		if (endDate != null && !endDate.isEmpty()) {
			sql.append(" AND o.ot_date <= ? ");
		}

// 🔥 정렬
		sql.append(" ORDER BY ");

		switch (sort) {
		case "name_asc":
			sql.append(" e.emp_name ASC ");
			break;
		case "name_desc":
			sql.append(" e.emp_name DESC ");
			break;
		case "position_asc":
			sql.append(" jp.position_name ASC ");
			break;
		case "position_desc":
			sql.append(" jp.position_name DESC ");
			break;
		default:
			sql.append(" o.created_at DESC ");
		}

		try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

			int idx = 1;

			pstmt.setInt(idx++, approverId);

			if (dept != null && !dept.isEmpty()) {
				pstmt.setString(idx++, dept);
			}

			if (startDate != null && !startDate.isEmpty()) {
				pstmt.setString(idx++, startDate);
			}

			if (endDate != null && !endDate.isEmpty()) {
				pstmt.setString(idx++, endDate);
			}

			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) {

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
				
				dto.setEmpName(rs.getString("emp_name"));
				dto.setDeptName(rs.getString("dept_name"));
				dto.setPosition(rs.getString("position_name"));

				list.add(dto);
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("승인 대기 목록 조회 실패", e);
		}

		return list;
	}
	
	public List<String> getPendingDeptList(Connection conn) {

	    List<String> list = new ArrayList<>();

	    String sql =
	        "SELECT DISTINCT d.dept_name " +
	        "FROM overtime_request o " +
	        "JOIN employee e ON o.emp_id = e.emp_id " +
	        "JOIN department d ON e.dept_id = d.dept_id " +
	        "WHERE o.status = '대기' " +
	        "ORDER BY d.dept_name";

	    try (PreparedStatement pstmt = conn.prepareStatement(sql);
	         ResultSet rs = pstmt.executeQuery()) {

	        while (rs.next()) {
	            list.add(rs.getString("dept_name"));
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	        throw new RuntimeException("부서 목록 조회 실패", e);
	    }

	    return list;
	}
}
