package com.hrms.org.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Vector;

import com.hrms.org.dto.PosDTO;
import com.hrms.common.db.DatabaseConnection;

public class PosDAO {

	public PosDAO() {}
	
	public Vector<PosDTO> posList() {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String sql = null;
        Vector<PosDTO> vlist = new Vector<PosDTO>();
        
        try {
            // pool 객체로부터 connection을 가져오도록 수정
            con = DatabaseConnection.getConnection(); 
            sql = "select position_id, position_name from job_position order by position_level desc";
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
            	PosDTO dto = new PosDTO();
                dto.setPosition_id(rs.getInt("position_id"));
                dto.setPosition_name(rs.getString("position_name"));
                vlist.addElement(dto); 
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (con != null) con.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return vlist;
}
	/**
     * 특정 직급 ID로 직급명을 조회하는 메서드
     */
    public String getPositionNameById(int positionId) {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String positionName = "일반"; // 기본값 설정

        try {
            con = DatabaseConnection.getConnection();
            // 직급 ID에 해당하는 직급명을 가져오는 쿼리
            String sql = "SELECT position_name FROM job_position WHERE position_id = ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, positionId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                positionName = rs.getString("position_name");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 자원 해제
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (con != null) con.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return positionName;
    }
}
