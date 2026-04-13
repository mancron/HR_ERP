package com.hrms.att.dao;

import com.hrms.att.dto.AnnualLeaveDTO;
import com.hrms.att.dto.LeaveDTO;
import com.hrms.common.db.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LeaveDAO {

	// 1. 휴가 신청 (INSERT)
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
			pstmt.setInt(8, dto.getApproverId());

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

	// 6. 휴가 승인 / 반려 처리
	public boolean updateLeaveStatus(Connection conn, int leaveId, int approverId, String status, String reason)
			throws Exception {

		String sql = "UPDATE leave_request " + "SET status = ?, " + "    approver_id = ?, "
				+ "    approved_at = NOW(), " + "    reject_reason = ? " + "WHERE leave_id = ?";

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
	public List<String> getPendingDeptList(int approverId) {

		List<String> list = new ArrayList<>();

		String sql = "SELECT DISTINCT d.dept_name " + "FROM leave_request lr "
				+ "JOIN employee e ON lr.emp_id = e.emp_id " + "JOIN department d ON e.dept_id = d.dept_id "
				+ "WHERE lr.status = '대기' AND lr.approver_id = ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setInt(1, approverId);

			try (ResultSet rs = pstmt.executeQuery()) {

				while (rs.next()) {
					list.add(rs.getString("dept_name"));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	// 잔여 연차 값 업데이트
	public void updateAnnualLeave(Connection conn, int empId, double days) throws Exception {
		String sql = "UPDATE annual_leave SET " + "used_days = used_days + ?, " + "remain_days = remain_days - ? "
				+ "WHERE emp_id = ? " + "AND leave_year = YEAR(NOW())";

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

	// 휴가 상세 정보 가져오기
	public LeaveDTO getLeaveById(int leaveId) {

		String sql = "SELECT lr.*, " + "       e.emp_name, jp.position_name, d.dept_name, "
				+ "       a.emp_name AS approver_name, " + "       ap.position_name AS approver_position, "
				+ "       ad.dept_name AS approver_dept " + "FROM leave_request lr "
				+ "JOIN employee e ON lr.emp_id = e.emp_id " + "JOIN job_position jp ON e.position_id = jp.position_id "
				+ "JOIN department d ON e.dept_id = d.dept_id " + "LEFT JOIN employee a ON lr.approver_id = a.emp_id "
				+ "LEFT JOIN job_position ap ON a.position_id = ap.position_id "
				+ "LEFT JOIN department ad ON a.dept_id = ad.dept_id " + "WHERE lr.leave_id = ?";

		try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setInt(1, leaveId);
			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
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
				dto.setApprovedAt(rs.getTimestamp("approved_at"));
				dto.setRejectReason(rs.getString("reject_reason"));
				dto.setCreatedAt(rs.getTimestamp("created_at"));

				dto.setEmpName(rs.getString("emp_name"));
				dto.setPosition(rs.getString("position_name"));
				dto.setDeptName(rs.getString("dept_name"));

				dto.setApproverId(rs.getInt("approver_id"));
				dto.setApproverName(rs.getString("approver_name"));
				dto.setApproverPosition(rs.getString("approver_position"));
				dto.setApproverDept(rs.getString("approver_dept"));

				return dto;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	// 특정 날짜에 휴가인지 확인
	public boolean existsByDate(int empId, LocalDate date) {

		String sql = "SELECT COUNT(*) FROM leave_request " + "WHERE emp_id = ? " + "AND status = '승인' "
				+ "AND ? BETWEEN start_date AND end_date";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setInt(1, empId);
			pstmt.setDate(2, java.sql.Date.valueOf(date));

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

	// 연도 조회
	public List<Integer> getAvailableYears() {

		List<Integer> list = new ArrayList<>();

		String sql = "SELECT DISTINCT leave_year FROM annual_leave ORDER BY leave_year DESC";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				list.add(rs.getInt("leave_year"));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	// 연차 현황 필터 조회
	public List<AnnualLeaveDTO> getAnnualLeaveList(int year, String dept, String name, int offset, int size) {

		List<AnnualLeaveDTO> list = new ArrayList<>();

		StringBuilder sql = new StringBuilder();

		sql.append("SELECT e.emp_id, e.emp_name, d.dept_name, ");
		sql.append("       a.total_days, a.used_days, a.remain_days ");
		sql.append("FROM annual_leave a ");
		sql.append("JOIN employee e ON a.emp_id = e.emp_id ");
		sql.append("JOIN department d ON e.dept_id = d.dept_id ");
		sql.append("WHERE a.leave_year = ? ");

		if (dept != null && !dept.isEmpty()) {
			sql.append("AND d.dept_name = ? ");
		}

		if (name != null && !name.isEmpty()) {
			sql.append("AND e.emp_name LIKE ? ");
		}

		sql.append("ORDER BY e.emp_name ");
		sql.append("LIMIT ?, ? ");

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql.toString())) {

			int idx = 1;
			ps.setInt(idx++, year);

			if (dept != null && !dept.isEmpty()) {
				ps.setString(idx++, dept);
			}

			if (name != null && !name.isEmpty()) {
				ps.setString(idx++, "%" + name + "%");
			}
			
			ps.setInt(idx++, offset);
			ps.setInt(idx++, size);
			
			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				AnnualLeaveDTO dto = new AnnualLeaveDTO();

				dto.setEmpId(rs.getInt("emp_id"));
				dto.setEmpName(rs.getString("emp_name"));
				dto.setDeptName(rs.getString("dept_name"));

				dto.setTotalDays(rs.getDouble("total_days"));
				dto.setUsedDays(rs.getDouble("used_days"));
				dto.setRemainDays(rs.getDouble("remain_days"));

				list.add(dto);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}
	
	//페이징용 카운트
	public int getAnnualLeaveCount(int year, String dept, String name) {

	    int count = 0;

	    StringBuilder sql = new StringBuilder();

	    sql.append("SELECT COUNT(*) ");
	    sql.append("FROM employee e ");
	    sql.append("JOIN department d ON e.dept_id = d.dept_id ");
	    sql.append("WHERE 1=1 ");

	    // 🔥 부서 필터
	    if (dept != null && !dept.isEmpty()) {
	        sql.append("AND d.dept_name = ? ");
	    }

	    // 🔥 이름 검색
	    if (name != null && !name.isEmpty()) {
	        sql.append("AND e.emp_name LIKE ? ");
	    }

	    try (Connection conn = DatabaseConnection.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

	        int idx = 1;

	        // dept
	        if (dept != null && !dept.isEmpty()) {
	            pstmt.setString(idx++, dept);
	        }

	        // name
	        if (name != null && !name.isEmpty()) {
	            pstmt.setString(idx++, "%" + name + "%");
	        }

	        ResultSet rs = pstmt.executeQuery();

	        if (rs.next()) {
	            count = rs.getInt(1);
	        }

	    } catch (Exception e) {
	        throw new RuntimeException("연차 현황 COUNT 조회 실패", e);
	    }

	    return count;
	}

	// 연차 부여 연차 조정
	public void adjustTotalDays(Connection conn, int empId, double adjustDays) throws Exception {

		String sql = "UPDATE annual_leave " + "SET total_days = total_days + ?, " + "    remain_days = remain_days + ? "
				+ "WHERE emp_id = ? " + "AND leave_year = YEAR(NOW())";

		try (PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setDouble(1, adjustDays);
			ps.setDouble(2, adjustDays);
			ps.setInt(3, empId);

			ps.executeUpdate();
		}
	}

	public List<LeaveDTO> getPendingLeavesAll(
	        String dept, String sort,
	        String startDate, String endDate,
	        int empId, int offset, int size) {

	    List<LeaveDTO> list = new ArrayList<>();

	    StringBuilder sql = new StringBuilder();

	    sql.append("SELECT lr.*, e.emp_name, jp.position_name, d.dept_name ");
	    sql.append("FROM leave_request lr ");
	    sql.append("JOIN employee e ON lr.emp_id = e.emp_id ");
	    sql.append("JOIN job_position jp ON e.position_id = jp.position_id ");
	    sql.append("JOIN department d ON e.dept_id = d.dept_id ");
	    sql.append("WHERE lr.status = '대기' ");
	    sql.append("AND lr.emp_id != ? "); // 자기 제외

	    // 🔥 부서 필터
	    if (dept != null && !dept.isEmpty()) {
	        sql.append("AND d.dept_name = ? ");
	    }

	    // 🔥 기간 필터
	    if (startDate != null && endDate != null && !startDate.isEmpty() && !endDate.isEmpty()) {
	        sql.append("AND lr.start_date <= ? ");
	        sql.append("AND lr.end_date >= ? ");
	    }

	    // 🔥 정렬
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

	    sql.append("LIMIT ?, ? ");

	    try (Connection conn = DatabaseConnection.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

	        int idx = 1;

	        pstmt.setInt(idx++, empId);

	        if (dept != null && !dept.isEmpty()) {
	            pstmt.setString(idx++, dept);
	        }

	        if (startDate != null && endDate != null && !startDate.isEmpty() && !endDate.isEmpty()) {
	            pstmt.setDate(idx++, Date.valueOf(endDate));
	            pstmt.setDate(idx++, Date.valueOf(startDate));
	        }

	        pstmt.setInt(idx++, offset);
	        pstmt.setInt(idx++, size);

	        ResultSet rs = pstmt.executeQuery();

	        while (rs.next()) {
	            list.add(mapRow(rs));
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return list;
	}
	
	public List<LeaveDTO> getPendingByApprover(
	        int approverId, int offset, int size) {

	    List<LeaveDTO> list = new ArrayList<>();

	    String sql =
	        "SELECT lr.*, e.emp_name, jp.position_name, d.dept_name " +
	        "FROM leave_request lr " +
	        "JOIN employee e ON lr.emp_id = e.emp_id " +
	        "JOIN job_position jp ON e.position_id = jp.position_id " +
	        "JOIN department d ON e.dept_id = d.dept_id " +
	        "WHERE lr.status = '대기' " +
	        "AND lr.approver_id = ? " +
	        "ORDER BY lr.created_at DESC " +
	        "LIMIT ?, ?";

	    try (Connection conn = DatabaseConnection.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {

	        pstmt.setInt(1, approverId);
	        pstmt.setInt(2, offset);
	        pstmt.setInt(3, size);

	        ResultSet rs = pstmt.executeQuery();

	        while (rs.next()) {
	            list.add(mapRow(rs));
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return list;
	}
	
	private LeaveDTO mapRow(ResultSet rs) throws Exception {

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

	    dto.setApprovedAt(rs.getTimestamp("approved_at"));
	    dto.setRejectReason(rs.getString("reject_reason"));
	    dto.setCreatedAt(rs.getTimestamp("created_at"));

	    dto.setEmpName(rs.getString("emp_name"));
	    dto.setPosition(rs.getString("position_name"));
	    dto.setDeptName(rs.getString("dept_name"));

	    return dto;
	}
	
	public int getPendingCountAll(String dept, String startDate, String endDate, int empId) {

	    StringBuilder sql = new StringBuilder();

	    sql.append("SELECT COUNT(*) ");
	    sql.append("FROM leave_request lr ");
	    sql.append("JOIN employee e ON lr.emp_id = e.emp_id ");
	    sql.append("JOIN department d ON e.dept_id = d.dept_id ");
	    sql.append("WHERE lr.status = '대기' ");
	    sql.append("AND lr.emp_id != ? ");

	    if (dept != null && !dept.isEmpty()) {
	        sql.append("AND d.dept_name = ? ");
	    }

	    if (startDate != null && endDate != null && !startDate.isEmpty() && !endDate.isEmpty()) {
	        sql.append("AND lr.start_date <= ? ");
	        sql.append("AND lr.end_date >= ? ");
	    }

	    try (Connection conn = DatabaseConnection.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

	        int idx = 1;

	        pstmt.setInt(idx++, empId);

	        if (dept != null && !dept.isEmpty()) {
	            pstmt.setString(idx++, dept);
	        }

	        if (startDate != null && endDate != null && !startDate.isEmpty() && !endDate.isEmpty()) {
	            pstmt.setDate(idx++, Date.valueOf(endDate));
	            pstmt.setDate(idx++, Date.valueOf(startDate));
	        }

	        ResultSet rs = pstmt.executeQuery();

	        if (rs.next()) {
	            return rs.getInt(1);
	        }

	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }

	    return 0;
	}
	
	public int getPendingCountByApprover(int approverId) {

	    String sql =
	        "SELECT COUNT(*) FROM leave_request " +
	        "WHERE status = '대기' AND approver_id = ?";

	    try (Connection conn = DatabaseConnection.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(sql)) {

	        pstmt.setInt(1, approverId);

	        ResultSet rs = pstmt.executeQuery();

	        if (rs.next()) {
	            return rs.getInt(1);
	        }

	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }

	    return 0;
	}
}