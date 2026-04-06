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

    // ──────────────────────────────────────────────
    // 공통 매핑 헬퍼: ResultSet → DeptDTO
    // (manager_name 포함)
    // ──────────────────────────────────────────────
    private DeptDTO mapRow(ResultSet rs) throws Exception {
        DeptDTO dto = new DeptDTO();
        dto.setDept_id(rs.getInt("dept_id"));
        dto.setDept_name(rs.getString("dept_name"));
        dto.setParent_dept_id(rs.getInt("parent_dept_id"));
        dto.setManager_id(rs.getInt("manager_id"));
        dto.setDept_level(rs.getInt("dept_level"));
        dto.setSort_order(rs.getInt("sort_order"));
        dto.setIs_active(rs.getInt("is_active"));
        // closed_at / created_at 는 컬럼이 없는 쿼리에서 호출될 수도 있으므로 try-catch
        try { dto.setClosed_at(rs.getString("closed_at")); } catch (Exception ignored) {}
        try { dto.setCreated_at(rs.getString("created_at")); } catch (Exception ignored) {}
        // manager_name: LEFT JOIN 결과 (없으면 null)
        try { dto.setManager_name(rs.getString("manager_name")); } catch (Exception ignored) {}
        return dto;
    }

    /**
     * 부서 ID로 부서명 조회 (LoginServlet 호환)
     */
    public String getDeptNameById(int deptId) {
        Connection con = null; PreparedStatement pstmt = null; ResultSet rs = null;
        String deptName = "소속 미지정";
        try {
            con = DatabaseConnection.getConnection();
            pstmt = con.prepareStatement("SELECT dept_name FROM department WHERE dept_id = ?");
            pstmt.setInt(1, deptId);
            rs = pstmt.executeQuery();
            if (rs.next()) deptName = rs.getString("dept_name");
        } catch (Exception e) { e.printStackTrace(); }
        finally { closeResources(rs, pstmt, con); }
        return deptName;
    }

    /**
     * 활성 부서 목록 (Vector, 호환용)
     */
    public Vector<DeptDTO> deptList() {
        Connection con = null; PreparedStatement pstmt = null; ResultSet rs = null;
        Vector<DeptDTO> vlist = new Vector<>();
        try {
            con = DatabaseConnection.getConnection();
            pstmt = con.prepareStatement(
                "SELECT dept_id, dept_name FROM department WHERE is_active = 1 ORDER BY dept_id ASC");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                DeptDTO dto = new DeptDTO();
                dto.setDept_id(rs.getInt("dept_id"));
                dto.setDept_name(rs.getString("dept_name"));
                vlist.addElement(dto);
            }
        } catch (Exception e) { e.printStackTrace(); }
        finally { closeResources(rs, pstmt, con); }
        return vlist;
    }

    /**
     * 전체 부서 목록 — 부서장 이름 JOIN 포함
     */
    public List<DeptDTO> getAllDepts() {
        List<DeptDTO> list = new ArrayList<>();
        Connection con = null; PreparedStatement pstmt = null; ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql =
                "SELECT d.dept_id, d.dept_name, d.parent_dept_id, d.manager_id, " +
                "       d.dept_level, d.sort_order, d.is_active, d.closed_at, d.created_at, " +
                "       e.emp_name AS manager_name " +
                "FROM department d " +
                "LEFT JOIN employee e ON e.emp_id = d.manager_id " +
                "ORDER BY d.sort_order ASC, d.dept_id ASC";
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (Exception e) { e.printStackTrace(); }
        finally { closeResources(rs, pstmt, con); }
        return list;
    }

    /**
     * 단일 부서 조회 — 부서장 이름 JOIN 포함
     */
    public DeptDTO getDeptById(int deptId) {
        Connection con = null; PreparedStatement pstmt = null; ResultSet rs = null;
        DeptDTO dto = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql =
                "SELECT d.dept_id, d.dept_name, d.parent_dept_id, d.manager_id, " +
                "       d.dept_level, d.sort_order, d.is_active, d.closed_at, d.created_at, " +
                "       e.emp_name AS manager_name " +
                "FROM department d " +
                "LEFT JOIN employee e ON e.emp_id = d.manager_id " +
                "WHERE d.dept_id = ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, deptId);
            rs = pstmt.executeQuery();
            if (rs.next()) dto = mapRow(rs);
        } catch (Exception e) { e.printStackTrace(); }
        finally { closeResources(rs, pstmt, con); }
        return dto;
    }

    /**
     * 자식 부서 목록
     */
    public List<DeptDTO> getChildDepts(int parentId) {
        List<DeptDTO> list = new ArrayList<>();
        Connection con = null; PreparedStatement pstmt = null; ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql =
                "SELECT d.dept_id, d.dept_name, d.parent_dept_id, d.manager_id, " +
                "       d.dept_level, d.sort_order, d.is_active, d.closed_at, d.created_at, " +
                "       e.emp_name AS manager_name " +
                "FROM department d " +
                "LEFT JOIN employee e ON e.emp_id = d.manager_id " +
                "WHERE d.parent_dept_id = ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, parentId);
            rs = pstmt.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (Exception e) { e.printStackTrace(); }
        finally { closeResources(rs, pstmt, con); }
        return list;
    }

    /**
     * 부서 신규 등록 (manager_id 제외 — 인사발령에서 관리)
     */
    public int insertDept(DeptDTO dept) {
        Connection con = null; PreparedStatement pstmt = null; ResultSet rs = null;
        String sql = "INSERT INTO department (dept_name, parent_dept_id, dept_level, sort_order, is_active) " +
                     "VALUES (?, ?, ?, ?, 1)";
        try {
            con = DatabaseConnection.getConnection();
            // Statement.RETURN_GENERATED_KEYS 대신 PreparedStatement 상수를 사용하면 더 안전합니다.
            pstmt = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, dept.getDept_name());
            pstmt.setInt(2, dept.getParent_dept_id());
            pstmt.setInt(3, dept.getDept_level());
            pstmt.setInt(4, dept.getSort_order());

            int result = pstmt.executeUpdate();
            if (result > 0) {
                rs = pstmt.getGeneratedKeys();
                if (rs.next()) return rs.getInt(1); // 신규 생성된 dept_id 반환
            }
        } catch (Exception e) { e.printStackTrace(); }
        finally { closeResources(rs, pstmt, con); }
        return 0;
    }

    /**
     * 부서 수정 (manager_id 제외 — 인사발령에서 관리)
     */
    public boolean updateDept(DeptDTO dept) {
        Connection con = null; PreparedStatement pstmt = null;
        // is_active가 1(활성)로 들어오면 closed_at을 NULL로 밀어버립니다.
        String sql = "UPDATE department SET dept_name = ?, parent_dept_id = ?, " +
                     "dept_level = ?, sort_order = ?, is_active = ?, " +
                     "closed_at = (CASE WHEN ? = 1 THEN NULL ELSE closed_at END) " +
                     "WHERE dept_id = ?";
        try {
            con = DatabaseConnection.getConnection();
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, dept.getDept_name());
            pstmt.setInt(2, dept.getParent_dept_id());
            pstmt.setInt(3, dept.getDept_level());
            pstmt.setInt(4, dept.getSort_order());
            pstmt.setInt(5, dept.getIs_active());
            pstmt.setInt(6, dept.getIs_active()); // CASE 문 체크용
            pstmt.setInt(7, dept.getDept_id());
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
        finally { closeResources(null, pstmt, con); }
    }

    /**
     * 소속 직원 목록 (재귀 CTE, 하위 부서 포함)
     */
    public List<Map<String, Object>> getMembersByDeptId(int deptId) {
        List<Map<String, Object>> list = new ArrayList<>();
        Connection con = null; PreparedStatement pstmt = null; ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql =
                "WITH RECURSIVE DeptHierarchy AS ( " +
                "    SELECT dept_id FROM department WHERE dept_id = ? " +
                "    UNION ALL " +
                "    SELECT d.dept_id FROM department d " +
                "    INNER JOIN DeptHierarchy dh ON d.parent_dept_id = dh.dept_id " +
                ") " +
                "SELECT e.emp_no, e.emp_name, e.status, " +
                "COALESCE(p.position_name, '미지정') AS position_name " +
                "FROM employee e " +
                "LEFT JOIN job_position p ON e.position_id = p.position_id " +
                "WHERE e.dept_id IN (SELECT dept_id FROM DeptHierarchy) " +
                "ORDER BY CASE WHEN e.status = '재직' THEN 1 ELSE 2 END ASC, " +
                "p.position_level DESC, e.emp_no ASC";
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
        } catch (Exception e) { e.printStackTrace(); }
        finally { closeResources(rs, pstmt, con); }
        return list;
    }

    public List<Map<String, Object>> getEmpList() {
        List<Map<String, Object>> list = new ArrayList<>();
        Connection con = null; PreparedStatement pstmt = null; ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql =
                "SELECT e.emp_id, e.emp_name, COALESCE(p.position_name,'미지정') AS position_name " +
                "FROM employee e " +
                "LEFT JOIN job_position p ON e.position_id = p.position_id " +
                "WHERE e.status = '재직' ORDER BY p.position_level DESC, e.emp_name ASC";
            pstmt = con.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("empId",   rs.getInt("emp_id"));
                map.put("empName", rs.getString("emp_name"));
                map.put("posName", rs.getString("position_name"));
                list.add(map);
            }
        } catch (Exception e) { e.printStackTrace(); }
        finally { closeResources(rs, pstmt, con); }
        return list;
    }

    public boolean deleteDept(int deptId) {
        if (hasActiveMembersRecursive(deptId)) return false;
        Connection con = null; PreparedStatement pstmt = null;
        try {
            con = DatabaseConnection.getConnection();
            pstmt = con.prepareStatement(
                "UPDATE department SET is_active = 0, closed_at = NOW() WHERE dept_id = ?");
            pstmt.setInt(1, deptId);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
        finally { closeResources(null, pstmt, con); }
    }

    public boolean hasActiveMembersRecursive(int deptId) {
        Connection con = null; PreparedStatement pstmt = null; ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql =
                "WITH RECURSIVE SubDepts AS ( " +
                "    SELECT dept_id FROM department WHERE dept_id = ? " +
                "    UNION ALL " +
                "    SELECT d.dept_id FROM department d " +
                "    INNER JOIN SubDepts sd ON d.parent_dept_id = sd.dept_id " +
                "    WHERE d.is_active = 1 " + // 비활성 하위 부서는 탐색 제외
                ") " +
                "SELECT COUNT(*) FROM employee " +
                "WHERE dept_id IN (SELECT dept_id FROM SubDepts) " +
                "AND status = '재직'";
                
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
        Connection con = null; PreparedStatement pstmt = null; ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            pstmt = con.prepareStatement(
                "SELECT dept_id, dept_name, closed_at FROM department " +
                "WHERE is_active = 0 ORDER BY closed_at DESC");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                DeptDTO dto = new DeptDTO();
                dto.setDept_id(rs.getInt("dept_id"));
                dto.setDept_name(rs.getString("dept_name"));
                dto.setClosed_at(rs.getString("closed_at"));
                list.add(dto);
            }
        } catch (Exception e) { e.printStackTrace(); }
        finally { closeResources(rs, pstmt, con); }
        return list;
    }

    public int getMemberCount(int deptId) {
        Connection con = null; PreparedStatement pstmt = null; ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            pstmt = con.prepareStatement(
                "SELECT COUNT(*) FROM employee WHERE dept_id = ? AND status != '퇴직'");
            pstmt.setInt(1, deptId);
            rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        finally { closeResources(rs, pstmt, con); }
        return 0;
    }

    public int getChildDeptCount(int deptId) {
        Connection con = null; PreparedStatement pstmt = null; ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            pstmt = con.prepareStatement(
                "SELECT COUNT(*) FROM department WHERE parent_dept_id = ? AND is_active = 1");
            pstmt.setInt(1, deptId);
            rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        finally { closeResources(rs, pstmt, con); }
        return 0;
    }

    public List<Map<String, Object>> findDeptIdByEmpName(String searchVal) {
        List<Map<String, Object>> list = new ArrayList<>();
        Connection con = null; PreparedStatement pstmt = null; ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            
            // 사번(EMP로 시작)인지 이름인지에 따라 조건을 분기하는 쿼리
            String sql = "SELECT e.dept_id, d.dept_name, p.position_name, e.emp_name, e.emp_no " +
                         "FROM employee e " +
                         "LEFT JOIN department d ON e.dept_id = d.dept_id " +
                         "LEFT JOIN job_position p ON e.position_id = p.position_id " +
                         "WHERE (e.emp_name = ? OR e.emp_no = ?) AND e.status != '퇴직'";

            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, searchVal); // 이름으로 검색 시
            pstmt.setString(2, searchVal); // 사번으로 검색 시 (둘 중 하나만 걸리면 됨)
            
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("deptId",   rs.getInt("dept_id"));
                map.put("deptName", rs.getString("dept_name"));
                map.put("posName",  rs.getString("position_name"));
                map.put("empName",  rs.getString("emp_name")); // 이 줄이 서버에 반영됐는지 확인!
                map.put("empNo",    rs.getString("emp_no"));   // 이 줄이 서버에 반영됐는지 확인!
                list.add(map);
            }
        } catch (Exception e) { e.printStackTrace(); }
        finally { closeResources(rs, pstmt, con); }
        return list;
    }

    public boolean updateDeptLevel(int deptId, int newLevel) {
        Connection con = null; PreparedStatement pstmt = null;
        try {
            con = DatabaseConnection.getConnection();
            pstmt = con.prepareStatement(
                "UPDATE department SET dept_level = ? WHERE dept_id = ?");
            pstmt.setInt(1, newLevel);
            pstmt.setInt(2, deptId);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
        finally { closeResources(null, pstmt, con); }
    }

    private void closeResources(ResultSet rs, PreparedStatement pstmt, Connection con) {
        try { if (rs    != null) rs.close();    } catch (Exception e) { e.printStackTrace(); }
        try { if (pstmt != null) pstmt.close(); } catch (Exception e) { e.printStackTrace(); }
        try { if (con   != null) con.close();   } catch (Exception e) { e.printStackTrace(); }
    }
}