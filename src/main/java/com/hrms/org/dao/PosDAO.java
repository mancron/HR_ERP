package com.hrms.org.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Vector;

import com.hrms.org.dto.PosDTO;
import com.hrms.util.DatabaseConnection;

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
	
}
