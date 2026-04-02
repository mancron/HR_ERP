package com.hrms.org.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.hrms.org.dto.DeptDTO;
import com.hrms.common.db.DatabaseConnection;

public class DeptDAO {

    public DeptDAO() {}

    // 기존 로그인/사원목록 호환용
    public Vector<DeptDTO> deptList() {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Vector<DeptDTO> vlist = new Vector<>();
        try {
            con = DatabaseConnection.getConnection();
            String sql = "SELECT dept_id, dept_name FROM department WHERE is_active = 1 ORDER BY dept_id ASC";
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                DeptDTO dto = new DeptDTO();
                dto.setDept_id(rs.getInt("dept_id"));
                dto.setDept_name(rs.getString("dept_name"));
                vlist.addElement(dto);
            }
        } catch (Exception e) { e.printStackTrace();
        } finally { closeResources(rs, pstmt, con); }
        return vlist;
    }

    public String getDeptNameById(int deptId) {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String deptName = "소속 미지정";
        try {
            con = DatabaseConnection.getConnection();
            String sql = "SELECT dept_name FROM department WHERE dept_id = ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, deptId);
            rs = pstmt.executeQuery();
            if (rs.next()) deptName = rs.getString("dept_name");
        } catch (Exception e) { e.printStackTrace();
        } finally { closeResources(rs, pstmt, con); }
        return deptName;
    }

    // 트리 및 전체 조회
    public List<DeptDTO> getAllDepts() {
        List<DeptDTO> list = new ArrayList<>();
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "SELECT * FROM department ORDER BY sort_order ASC, dept_id ASC";
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                DeptDTO dto = new DeptDTO();
                dto.setDept_id(rs.getInt("dept_id"));
                dto.setDept_name(rs.getString("dept_name"));
                dto.setParent_dept_id(rs.getInt("parent_dept_id"));
                dto.setManager_id(rs.getInt("manager_id"));
                dto.setDept_level(rs.getInt("dept_level"));
                dto.setSort_order(rs.getInt("sort_order"));
                dto.setIs_active(rs.getInt("is_active"));
                dto.setClosed_at(rs.getString("closed_at"));
                dto.setCreated_at(rs.getString("created_at"));
                list.add(dto);
            }
        } catch (Exception e) { e.printStackTrace();
        } finally { closeResources(rs, pstmt, con); }
        return list;
    }

    public DeptDTO getDeptById(int deptId) {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        DeptDTO dto = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "SELECT * FROM department WHERE dept_id = ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, deptId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                dto = new DeptDTO();
                dto.setDept_id(rs.getInt("dept_id"));
                dto.setDept_name(rs.getString("dept_name"));
                dto.setParent_dept_id(rs.getInt("parent_dept_id"));
                dto.setManager_id(rs.getInt("manager_id"));
                dto.setDept_level(rs.getInt("dept_level"));
                dto.setSort_order(rs.getInt("sort_order"));
                dto.setIs_active(rs.getInt("is_active"));
            }
        } catch (Exception e) { e.printStackTrace();
        } finally { closeResources(rs, pstmt, con); }
        return dto;
    }

    // 검색 기능용
    public int findDeptIdByEmpName(String empName) {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int deptId = 0;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "SELECT dept_id FROM employee WHERE emp_name = ? AND status != '퇴직' LIMIT 1";
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, empName);
            rs = pstmt.executeQuery();
            if (rs.next()) deptId = rs.getInt("dept_id");
        } catch (Exception e) { e.printStackTrace();
        } finally { closeResources(rs, pstmt, con); }
        return deptId;
    }

    // CUD 로직
    public boolean insertDept(DeptDTO dept) {
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "INSERT INTO department (dept_name, parent_dept_id, manager_id, dept_level, sort_order, is_active) VALUES (?, ?, ?, ?, ?, 1)";
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, dept.getDept_name());
            pstmt.setInt(2, dept.getParent_dept_id());
            pstmt.setInt(3, dept.getManager_id());
            pstmt.setInt(4, dept.getDept_level());
            pstmt.setInt(5, dept.getSort_order());
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false;
        } finally { closeResources(null, pstmt, con); }
    }

    public boolean updateDept(DeptDTO dept) {
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "UPDATE department SET dept_name = ?, parent_dept_id = ?, manager_id = ?, dept_level = ?, sort_order = ?, is_active = ? WHERE dept_id = ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, dept.getDept_name());
            pstmt.setInt(2, dept.getParent_dept_id());
            pstmt.setInt(3, dept.getManager_id());
            pstmt.setInt(4, dept.getDept_level());
            pstmt.setInt(5, dept.getSort_order());
            pstmt.setInt(6, dept.getIs_active());
            pstmt.setInt(7, dept.getDept_id());
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false;
        } finally { closeResources(null, pstmt, con); }
    }

    public boolean deleteDept(int deptId) {
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "UPDATE department SET is_active = 0, closed_at = NOW() WHERE dept_id = ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, deptId);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false;
        } finally { closeResources(null, pstmt, con); }
    }

    // 인원 및 하위부서 카운트 (빨간 줄 해결 핵심)
    public int getMemberCount(int deptId) {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int count = 0;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "SELECT COUNT(*) FROM employee WHERE dept_id = ? AND status != '퇴직'";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, deptId);
            rs = pstmt.executeQuery();
            if (rs.next()) count = rs.getInt(1);
        } catch (Exception e) { e.printStackTrace();
        } finally { closeResources(rs, pstmt, con); }
        return count;
    }

    public int getChildDeptCount(int deptId) {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int count = 0;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "SELECT COUNT(*) FROM department WHERE parent_dept_id = ? AND is_active = 1";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, deptId);
            rs = pstmt.executeQuery();
            if (rs.next()) count = rs.getInt(1);
        } catch (Exception e) { e.printStackTrace();
        } finally { closeResources(rs, pstmt, con); }
        return count;
    }

    // 멤버 리스트 및 비활성 리스트
    public List<Map<String, Object>> getMembersByDeptId(int deptId) {
        List<Map<String, Object>> list = new ArrayList<>();
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "WITH RECURSIVE DeptHierarchy AS (" +
                         "    SELECT dept_id FROM department WHERE dept_id = ? " +
                         "    UNION ALL " +
                         "    SELECT d.dept_id FROM department d " +
                         "    INNER JOIN DeptHierarchy dh ON d.parent_dept_id = dh.dept_id" +
                         ") " +
                         "SELECT e.emp_no, e.emp_name, e.status, p.position_name FROM employee e " +
                         "LEFT JOIN job_position p ON e.position_id = p.position_id " +
                         "WHERE e.dept_id IN (SELECT dept_id FROM DeptHierarchy) " +
                         "ORDER BY CASE WHEN e.status = '재직' THEN 1 ELSE 2 END ASC, p.position_level DESC";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, deptId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("empNo", rs.getString("emp_no"));
                map.put("empName", rs.getString("emp_name"));
                map.put("posName", rs.getString("position_name"));
                map.put("status", rs.getString("status"));
                list.add(map);
            }
        } catch (Exception e) { e.printStackTrace();
        } finally { closeResources(rs, pstmt, con); }
        return list;
    }

    public List<Map<String, Object>> getEmpList() {
        List<Map<String, Object>> list = new ArrayList<>();
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "SELECT e.emp_id, e.emp_name, p.position_name FROM employee e " +
                         "LEFT JOIN job_position p ON e.position_id = p.position_id " +
                         "WHERE e.status = '재직' ORDER BY p.position_level DESC, e.emp_name ASC";
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("empId", rs.getInt("emp_id"));
                map.put("empName", rs.getString("emp_name"));
                map.put("posName", rs.getString("position_name"));
                list.add(map);
            }
        } catch (Exception e) { e.printStackTrace();
        } finally { closeResources(rs, pstmt, con); }
        return list;
    }

    public List<DeptDTO> getInactiveDepts() {
        List<DeptDTO> list = new ArrayList<>();
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "SELECT * FROM department WHERE is_active = 0 ORDER BY closed_at DESC";
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                DeptDTO dto = new DeptDTO();
                dto.setDept_id(rs.getInt("dept_id"));
                dto.setDept_name(rs.getString("dept_name"));
                dto.setClosed_at(rs.getString("closed_at"));
                list.add(dto);
            }
        } catch (Exception e) { e.printStackTrace();
        } finally { closeResources(rs, pstmt, con); }
        return list;
    }

 // 기존 DeptDAO 클래스 내부에 추가
    public boolean hasActiveMembersRecursive(int deptId) {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        boolean hasMembers = false;
        try {
            con = DatabaseConnection.getConnection();
            // WITH RECURSIVE를 통해 해당 부서와 모든 하위 부서의 재직자 수를 카운트
            String sql = "WITH RECURSIVE SubDepts AS (" +
                         "    SELECT dept_id FROM department WHERE dept_id = ? " +
                         "    UNION ALL " +
                         "    SELECT d.dept_id FROM department d INNER JOIN SubDepts sd ON d.parent_dept_id = sd.dept_id" +
                         ") " +
                         "SELECT COUNT(*) FROM employee WHERE dept_id IN (SELECT dept_id FROM SubDepts) AND status = '재직'";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, deptId);
            rs = pstmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                hasMembers = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResources(rs, pstmt, con);
        }
        return hasMembers;
    }
    
    private void closeResources(ResultSet rs, PreparedStatement pstmt, Connection con) {
        try {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
            if (con != null) con.close();
        } catch (Exception e) { e.printStackTrace(); }
    }
}