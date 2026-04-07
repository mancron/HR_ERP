package com.hrms.att.dao;

import com.hrms.att.dto.AttendanceDTO;
import com.hrms.common.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
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

		String sql = "SELECT * FROM attendance " + "WHERE emp_id = ? AND DATE_FORMAT(work_date, '%Y-%m') = ? "
				+ "ORDER BY work_date";

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

	// 특정 날짜에 근태 기록 있는지 확인
	public AttendanceDTO getAttendanceByDate(int empId, Date date, Connection conn) throws Exception {

		String sql = "SELECT * FROM attendance WHERE emp_id = ? AND work_date = ?";

		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setInt(1, empId);
			pstmt.setDate(2, date);

			ResultSet rs = pstmt.executeQuery();

			if (rs.next()) {
				AttendanceDTO dto = new AttendanceDTO();

				dto.setAttId(rs.getInt("att_id"));
				dto.setEmpId(rs.getInt("emp_id"));
				dto.setWorkDate(rs.getDate("work_date"));
				dto.setCheckIn(rs.getTime("check_in"));
				dto.setCheckOut(rs.getTime("check_out"));
				dto.setStatus(rs.getString("status"));
				dto.setNote(rs.getString("note"));

				return dto;
			}
		}

		return null;
	}

	// 결근 추가
	public void insertAbsent(int empId, Date date, Connection conn) throws Exception {

		String sql = "INSERT INTO attendance (emp_id, work_date, status) VALUES (?, ?, '결근')";

		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setInt(1, empId);
			pstmt.setDate(2, date);

			pstmt.executeUpdate();
		}
	}

	// 상태 업데이트
	public void updateStatus(int empId, Date date, String status, String note, Connection conn) throws Exception {

		String sql = "UPDATE attendance SET status = ?, note = ? WHERE emp_id = ? AND work_date = ?";

		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, status);
			pstmt.setString(2, note);
			pstmt.setInt(3, empId);
			pstmt.setDate(4, date);

			pstmt.executeUpdate();
		}
	}

	// 퇴근만 수정
	public void updateCheckout(int empId, Date date, Time checkout, String note, Connection conn) throws Exception {

		String sql = "UPDATE attendance SET check_out = ?, note = ? WHERE emp_id = ? AND work_date = ?";

		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setTime(1, checkout);
			pstmt.setString(2, note);
			pstmt.setInt(3, empId);
			pstmt.setDate(4, date);

			pstmt.executeUpdate();
		}
	}

	// 전체 수정
	public void updateAttendance(int empId, Date date, Time checkIn, Time checkOut, String status, String note,
			Connection conn) throws Exception {

		String sql = "UPDATE attendance " + "SET check_in=?, check_out=?, status=?, note=? "
				+ "WHERE emp_id=? AND work_date=?";

		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setTime(1, checkIn);
			pstmt.setTime(2, checkOut);
			pstmt.setString(3, status);
			pstmt.setString(4, note);
			pstmt.setInt(5, empId);
			pstmt.setDate(6, date);

			pstmt.executeUpdate();
		}
	}

}