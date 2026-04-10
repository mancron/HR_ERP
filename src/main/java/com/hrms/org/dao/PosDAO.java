package com.hrms.org.dao;

import java.sql.*;
import java.util.Vector;
import com.hrms.org.dto.PosDTO;
import com.hrms.common.db.DatabaseConnection;

public class PosDAO {

    public PosDAO() {}

    // [기존 유지] 단순 목록 (id, name만)
    public Vector<PosDTO> posList() {
        Connection con = null; PreparedStatement pstmt = null; ResultSet rs = null;
        Vector<PosDTO> vlist = new Vector<>();
        try {
            con = DatabaseConnection.getConnection();
            String sql = "select position_id, position_name from job_position order by position_level desc";
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                PosDTO dto = new PosDTO();
                dto.setPosition_id(rs.getInt("position_id"));
                dto.setPosition_name(rs.getString("position_name"));
                vlist.addElement(dto);
            }
        } catch (Exception e) { e.printStackTrace(); }
        finally { close(con, pstmt, rs); }
        return vlist;
    }

    // [기존 유지] ID로 이름 찾기
    public String getPositionNameById(int positionId) {
        Connection con = null; PreparedStatement pstmt = null; ResultSet rs = null;
        String name = "일반";
        try {
            con = DatabaseConnection.getConnection();
            String sql = "SELECT position_name FROM job_position WHERE position_id = ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, positionId);
            rs = pstmt.executeQuery();
            if (rs.next()) name = rs.getString("position_name");
        } catch (Exception e) { e.printStackTrace(); }
        finally { close(con, pstmt, rs); }
        return name;
    }

    // [기존 유지] 모든 컬럼 조회
    public Vector<PosDTO> posListFull() {
        Connection con = null; PreparedStatement pstmt = null; ResultSet rs = null;
        Vector<PosDTO> vlist = new Vector<>();
        try {
            con = DatabaseConnection.getConnection();
            // 서브쿼리를 이용해 직급별 인원수(emp_count)를 함께 가져옴
            String sql = "SELECT p.*, " +
                         "(SELECT COUNT(*) FROM employee e WHERE e.position_id = p.position_id) as emp_count " +
                         "FROM job_position p ORDER BY p.position_level DESC";
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                PosDTO dto = new PosDTO();
                dto.setPosition_id(rs.getInt("position_id"));
                dto.setPosition_name(rs.getString("position_name"));
                dto.setPosition_level(rs.getInt("position_level"));
                dto.setBase_salary(rs.getBigDecimal("base_salary"));
                dto.setMeal_allowance(rs.getInt("meal_allowance"));
                dto.setTransport_allowance(rs.getInt("transport_allowance"));
                dto.setPosition_allowance(rs.getInt("position_allowance"));
                dto.setIs_active(rs.getInt("is_active"));
                dto.setEmp_count(rs.getInt("emp_count")); 
                
                vlist.add(dto);
            }
        } catch (Exception e) { e.printStackTrace(); }
        finally { close(con, pstmt, rs); }
        return vlist;
    }

    // [기존 유지] 한 명만 가져오기
    public PosDTO getPositionById(int id) {
        Connection con = null; PreparedStatement pstmt = null; ResultSet rs = null;
        PosDTO dto = null;
        try {
            con = DatabaseConnection.getConnection();
            pstmt = con.prepareStatement("SELECT * FROM job_position WHERE position_id = ?");
            pstmt.setInt(1, id);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                dto = new PosDTO();
                dto.setPosition_id(rs.getInt("position_id"));
                dto.setPosition_name(rs.getString("position_name"));
                dto.setBase_salary(rs.getBigDecimal("base_salary"));
                dto.setMeal_allowance(rs.getInt("meal_allowance"));
                dto.setTransport_allowance(rs.getInt("transport_allowance"));
                dto.setPosition_allowance(rs.getInt("position_allowance"));
                dto.setIs_active(rs.getInt("is_active"));
            }
        } catch (Exception e) { e.printStackTrace(); }
        finally { close(con, pstmt, rs); }
        return dto;
    }

 // [수정 통합] 감사 로그 연동 업데이트 (트랜잭션 적용 + High 5 Race Condition 방어)
 // [수정] 원본 로직 유지 + 테이블명 'employee' 반영 버전
    public boolean updatePositionWithLog(PosDTO dto, Integer actorId, String[] columns, String[] oldValues, String[] newValues) {
        Connection con = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false); // 트랜잭션 시작

            // --- 1. job_position 업데이트 (High 5 대응 로직 포함) ---
            StringBuilder updateSql = new StringBuilder();
            updateSql.append("UPDATE job_position SET base_salary=?, meal_allowance=?, transport_allowance=?, position_allowance=?, is_active=? ");
            updateSql.append("WHERE position_id=? ");
            
            // [High 5] 비활성화 시도 시, 그 순간에 직원이 0명인지 DB 레벨에서 다시 확인
            // 테이블명을 사용자님 DB에 맞춰 'employee'로 수정했습니다.
            if (dto.getIs_active() == 0) {
                updateSql.append("AND (SELECT COUNT(*) FROM employee WHERE position_id = ?) = 0");
            }

            pstmt = con.prepareStatement(updateSql.toString());
            pstmt.setBigDecimal(1, dto.getBase_salary());
            pstmt.setInt(2, dto.getMeal_allowance());
            pstmt.setInt(3, dto.getTransport_allowance());
            pstmt.setInt(4, dto.getPosition_allowance());
            pstmt.setInt(5, dto.getIs_active());
            pstmt.setInt(6, dto.getPosition_id());
            
            // 비활성화 조건일 경우 서브쿼리의 파라미터(?) 추가 세팅
            if (dto.getIs_active() == 0) {
                pstmt.setInt(7, dto.getPosition_id());
            }

            int affectedRows = pstmt.executeUpdate();
            
            // 업데이트된 행이 없다면 (직원이 존재하여 조건에 맞지 않거나, ID가 틀린 경우)
            if (affectedRows == 0) {
                con.rollback(); 
                return false;   
            }
            pstmt.close(); // 다음 작업을 위해 닫기


            // --- 2. audit_log 기록 (기본 로직 유지) ---
            String logSql = "INSERT INTO audit_log (actor_id, target_table, target_id, action, column_name, old_value, new_value) VALUES (?, 'job_position', ?, 'UPDATE', ?, ?, ?)";
            pstmt = con.prepareStatement(logSql);

            for (int i = 0; i < columns.length; i++) {
                // 값이 변경된 항목만 로그에 기록 (Null 체크 추가로 안전성 강화)
                if (oldValues[i] != null && !oldValues[i].equals(newValues[i])) {
                    if (actorId != null && actorId > 0) {
                        pstmt.setInt(1, actorId);
                    } else {
                        pstmt.setNull(1, java.sql.Types.INTEGER);
                    }
                    pstmt.setInt(2, dto.getPosition_id());
                    pstmt.setString(3, columns[i]);
                    pstmt.setString(4, oldValues[i]);
                    pstmt.setString(5, newValues[i]);
                    pstmt.addBatch();
                }
            }
            
            pstmt.executeBatch();
            con.commit(); // 트랜잭션 커밋
            success = true;

        } catch (Exception e) {
            try { 
                if (con != null) {
                    con.rollback(); 
                    System.err.println("Transaction Rollback executed due to error.");
                }
            } catch (SQLException se) { 
                se.printStackTrace(); 
            }
            e.printStackTrace();
        } finally {
            close(con, pstmt, null);
        }
        return success;
    }

    public int getEmployeeCountByPosition(int positionId) {
        Connection con = null; PreparedStatement pstmt = null; ResultSet rs = null;
        int count = 0;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "SELECT COUNT(*) FROM employee WHERE position_id = ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, positionId);
            rs = pstmt.executeQuery();
            if (rs.next()) count = rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        finally { close(con, pstmt, rs); }
        return count;
    }
    
    private void close(Connection con, PreparedStatement pstmt, ResultSet rs) {
        try { if(rs!=null) rs.close(); if(pstmt!=null) pstmt.close(); if(con!=null) con.close(); } catch (Exception e) {}
    }
}