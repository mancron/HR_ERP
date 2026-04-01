package com.hrms.sys.dao;

import com.hrms.sys.dto.HolidayDTO;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class HolidayDAO {

    /** 연도별 공휴일 목록 조회 (날짜 오름차순) */
    public List<HolidayDTO> selectByYear(int year, Connection conn) throws SQLException {
        List<HolidayDTO> list = new ArrayList<>();
        String sql =
            "SELECT holiday_id, holiday_date, holiday_name, holiday_year, created_at " +
            "FROM public_holiday " +
            "WHERE holiday_year = ? " +
            "ORDER BY holiday_date ASC, holiday_name ASC";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, year);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                HolidayDTO dto = new HolidayDTO();
                dto.setHolidayId(rs.getInt("holiday_id"));
                dto.setHolidayDate(rs.getDate("holiday_date").toLocalDate());
                dto.setHolidayName(rs.getString("holiday_name"));
                dto.setHolidayYear(rs.getInt("holiday_year"));
                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) dto.setCreatedAt(createdAt.toLocalDateTime());
                list.add(dto);
            }
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
        return list;
    }

    /** 중복 체크 — UNIQUE KEY(holiday_date, holiday_name) 기준 */
    public boolean existsByDateAndName(LocalDate date, String name, Connection conn) throws SQLException {
        String sql =
            "SELECT COUNT(*) FROM public_holiday " +
            "WHERE holiday_date = ? AND holiday_name = ?";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setDate(1, Date.valueOf(date));
            pstmt.setString(2, name);
            rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } finally {
            if (rs    != null) try { rs.close();    } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
        return false;
    }

    /** 공휴일 INSERT */
    public int insert(HolidayDTO dto, Connection conn) throws SQLException {
        String sql =
            "INSERT INTO public_holiday (holiday_date, holiday_name, holiday_year) " +
            "VALUES (?, ?, ?)";

        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setDate(1, Date.valueOf(dto.getHolidayDate()));
            pstmt.setString(2, dto.getHolidayName());
            pstmt.setInt(3, dto.getHolidayYear());
            return pstmt.executeUpdate();
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
    }

    /** 공휴일 DELETE (holiday_id 기준) */
    public int deleteById(int holidayId, Connection conn) throws SQLException {
        String sql = "DELETE FROM public_holiday WHERE holiday_id = ?";

        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, holidayId);
            return pstmt.executeUpdate();
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }
    }
    
    /** 특정 날짜가 공휴일인지 확인 */
    public static boolean existsByDate(LocalDate date, Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM public_holiday WHERE holiday_date = ?";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setDate(1, Date.valueOf(date));
            rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        }

        return false;
    }
    
}