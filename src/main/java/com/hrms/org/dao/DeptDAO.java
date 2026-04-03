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

    /**
     * [추가] 부서 ID로 부서명을 조회하는 메서드 (LoginServlet 호환용)
     */
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
            if (rs.next()) {
                deptName = rs.getString("dept_name");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResources(rs, pstmt, con);
        }
        return deptName;
    }

    /**
     * [호환용] 활성 부서 목록 조회
     */
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
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResources(rs, pstmt, con);
        }
        return vlist;
    }

    /**
     * [전체 조회] 부서장(manager_id) 제외 버전
     */
    public List<DeptDTO> getAllDepts() {
        List<DeptDTO> list = new ArrayList<>();
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "SELECT dept_id, dept_name, parent_dept_id, dept_level, sort_order, is_active, closed_at, created_at " +
                         "FROM department ORDER BY sort_order ASC, dept_id ASC";
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                DeptDTO dto = new DeptDTO();
                dto.setDept_id(rs.getInt("dept_id"));
                dto.setDept_name(rs.getString("dept_name"));
                dto.setParent_dept_id(rs.getInt("parent_dept_id"));
                dto.setDept_level(rs.getInt("dept_level"));
                dto.setSort_order(rs.getInt("sort_order"));
                dto.setIs_active(rs.getInt("is_active"));
                dto.setClosed_at(rs.getString("closed_at"));
                dto.setCreated_at(rs.getString("created_at"));
                list.add(dto);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResources(rs, pstmt, con);
        }
        return list;
    }

    public DeptDTO getDeptById(int deptId) {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        DeptDTO dto = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "SELECT dept_id, dept_name, parent_dept_id, dept_level, sort_order, is_active, closed_at, created_at " +
                         "FROM department WHERE dept_id = ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, deptId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                dto = new DeptDTO();
                dto.setDept_id(rs.getInt("dept_id"));
                dto.setDept_name(rs.getString("dept_name"));
                dto.setParent_dept_id(rs.getInt("parent_dept_id"));
                dto.setDept_level(rs.getInt("dept_level"));
                dto.setSort_order(rs.getInt("sort_order"));
                dto.setIs_active(rs.getInt("is_active"));
                dto.setClosed_at(rs.getString("closed_at"));
                dto.setCreated_at(rs.getString("created_at"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResources(rs, pstmt, con);
        }
        return dto;
    }

    public List<DeptDTO> getChildDepts(int parentId) {
        List<DeptDTO> list = new ArrayList<>();
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            // dept_level, is_active 등 모든 정보가 있어야 서비스의 로직이 돌아감
            String sql = "SELECT * FROM department WHERE parent_dept_id = ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, parentId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                DeptDTO dto = new DeptDTO();
                dto.setDept_id(rs.getInt("dept_id"));
                dto.setDept_name(rs.getString("dept_name"));
                dto.setParent_dept_id(rs.getInt("parent_dept_id"));
                dto.setDept_level(rs.getInt("dept_level"));
                dto.setIs_active(rs.getInt("is_active"));
                list.add(dto);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResources(rs, pstmt, con);
        }
        return list;
    }

    public boolean insertDept(DeptDTO dept) {
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "INSERT INTO department (dept_name, parent_dept_id, dept_level, sort_order, is_active) " +
                         "VALUES (?, ?, ?, ?, 1)";
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, dept.getDept_name());
            pstmt.setInt(2, dept.getParent_dept_id());
            pstmt.setInt(3, dept.getDept_level());
            pstmt.setInt(4, dept.getSort_order());
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            closeResources(null, pstmt, con);
        }
    }

    public boolean updateDept(DeptDTO dept) {
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "UPDATE department SET dept_name = ?, parent_dept_id = ?, " +
                         "dept_level = ?, sort_order = ?, is_active = ? WHERE dept_id = ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, dept.getDept_name());
            pstmt.setInt(2, dept.getParent_dept_id());
            pstmt.setInt(3, dept.getDept_level());
            pstmt.setInt(4, dept.getSort_order());
            pstmt.setInt(5, dept.getIs_active());
            pstmt.setInt(6, dept.getDept_id());
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            closeResources(null, pstmt, con);
        }
    }

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
                         "SELECT e.emp_no, e.emp_name, e.status, " +
                         "COALESCE(p.position_name, '미지정') AS position_name " +
                         "FROM employee e " +
                         "LEFT JOIN job_position p ON e.position_id = p.position_id " +
                         "WHERE e.dept_id IN (SELECT dept_id FROM DeptHierarchy) " +
                         "ORDER BY CASE WHEN e.status = '재직' THEN 1 ELSE 2 END ASC, p.position_level DESC, e.emp_no ASC";
            
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, deptId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("empNo",   rs.getString("emp_no"));
                map.put("empName", rs.getString("emp_name"));
                map.put("posName", rs.getString("position_name"));
                map.put("status",  rs.getString("status"));
                list.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResources(rs, pstmt, con);
        }
        return list;
    }

    public List<Map<String, Object>> getEmpList() {
        List<Map<String, Object>> list = new ArrayList<>();
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "SELECT e.emp_id, e.emp_name, COALESCE(p.position_name, '미지정') AS position_name " +
                         "FROM employee e " +
                         "LEFT JOIN job_position p ON e.position_id = p.position_id " +
                         "WHERE e.status = '재직' " +
                         "ORDER BY p.position_level DESC, e.emp_name ASC";
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("empId",   rs.getInt("emp_id"));
                map.put("empName", rs.getString("emp_name"));
                map.put("posName", rs.getString("position_name"));
                list.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResources(rs, pstmt, con);
        }
        return list;
    }

    public boolean deleteDept(int deptId) {
        if (hasActiveMembersRecursive(deptId)) return false; 
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "UPDATE department SET is_active = 0, closed_at = NOW() WHERE dept_id = ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, deptId);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            closeResources(null, pstmt, con);
        }
    }

    public boolean hasActiveMembersRecursive(int deptId) {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "WITH RECURSIVE SubDepts AS (" +
                         "    SELECT dept_id FROM department WHERE dept_id = ? " +
                         "    UNION ALL " +
                         "    SELECT d.dept_id FROM department d INNER JOIN SubDepts sd ON d.parent_dept_id = sd.dept_id" +
                         ") " +
                         "SELECT COUNT(*) FROM employee WHERE dept_id IN (SELECT dept_id FROM SubDepts) AND status = '재직'";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, deptId);
            rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (Exception e) { e.printStackTrace(); } 
        finally { closeResources(rs, pstmt, con); }
        return false;
    }

    public List<DeptDTO> getInactiveDepts() {
        List<DeptDTO> list = new ArrayList<>();
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "SELECT dept_id, dept_name, closed_at FROM department WHERE is_active = 0 ORDER BY closed_at DESC";
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                DeptDTO dto = new DeptDTO();
                dto.setDept_id(rs.getInt("dept_id"));
                dto.setDept_name(rs.getString("dept_name"));
                dto.setClosed_at(rs.getString("closed_at"));
                list.add(dto);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResources(rs, pstmt, con);
        }
        return list;
    }

    public int getMemberCount(int deptId) {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "SELECT COUNT(*) FROM employee WHERE dept_id = ? AND status != '퇴직'";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, deptId);
            rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        finally { closeResources(rs, pstmt, con); }
        return 0;
    }

    public int getChildDeptCount(int deptId) {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "SELECT COUNT(*) FROM department WHERE parent_dept_id = ? AND is_active = 1";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, deptId);
            rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        finally { closeResources(rs, pstmt, con); }
        return 0;
    }

    public int findDeptIdByEmpName(String empName) {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "SELECT dept_id FROM employee WHERE emp_name = ? AND status != '퇴직' LIMIT 1";
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, empName);
            rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt("dept_id");
        } catch (Exception e) { e.printStackTrace(); }
        finally { closeResources(rs, pstmt, con); }
        return 0;
    }

    /**
     * [추가] 특정 부서의 레벨(깊이)만 업데이트 (계층 이동 시 사용)
     */
    public boolean updateDeptLevel(int deptId, int newLevel) {
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "UPDATE department SET dept_level = ? WHERE dept_id = ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, newLevel);
            pstmt.setInt(2, deptId);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            closeResources(null, pstmt, con);
        }
    }

    
    private void closeResources(ResultSet rs, PreparedStatement pstmt, Connection con) {
        try {
            if (rs != null)    rs.close();
            if (pstmt != null) pstmt.close();
            if (con != null)   con.close();
        } catch (Exception e) { e.printStackTrace(); }
    }
}