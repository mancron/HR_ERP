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

    // ── 기존 메서드 (유지) ────────────────────────────────────

    public Vector<DeptDTO> deptList() {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Vector<DeptDTO> vlist = new Vector<>();
        try {
            con = DatabaseConnection.getConnection();
            String sql = "SELECT dept_id, dept_name FROM department ORDER BY dept_id ASC";
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
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResources(rs, pstmt, con);
        }
        return deptName;
    }

    // ── 신규 추가 메서드 ──────────────────────────────────────

    /**
     * 전체 부서 목록 (sort_order, dept_id 기준 정렬)
     */
    public List<DeptDTO> getAllDepts() {
        List<DeptDTO> list = new ArrayList<>();
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "SELECT dept_id, dept_name, parent_dept_id, manager_id, " +
                         "dept_level, sort_order, is_active, closed_at, created_at " +
                         "FROM department ORDER BY sort_order ASC, dept_id ASC";
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
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResources(rs, pstmt, con);
        }
        return list;
    }

    /**
     * 단건 부서 조회
     */
    public DeptDTO getDeptById(int deptId) {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        DeptDTO dto = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "SELECT dept_id, dept_name, parent_dept_id, manager_id, " +
                         "dept_level, sort_order, is_active, closed_at, created_at " +
                         "FROM department WHERE dept_id = ?";
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

    /**
     * 특정 부서 소속 직원 목록 조회
     * EmpDTO 기준:
     *   - 상태: e.status ('재직'/'휴직'/'퇴직') — is_active 컬럼 없음
     *   - 직급명: JOIN으로 가져오는 position_name
     *   - 사번: emp_no (String)
     * 반환 Map 키: empNo, empName, posName, status
     */
    /**
     * 특정 부서 및 그 하위 부서에 속한 모든 직원 목록 조회 (재귀 쿼리 사용)
     */
    public List<Map<String, Object>> getMembersByDeptId(int deptId) {
        List<Map<String, Object>> list = new ArrayList<>();
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            
            // 정렬 조건: 
            // 1. 상태가 '재직'인 사람 우선 (CASE문 사용)
            // 2. 직급 레벨이 높은 순 (DESC)
            // 3. 직급이 같다면 사번(emp_no) 오름차순 (ASC)
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
                         "ORDER BY " +
                         "  CASE WHEN e.status = '재직' THEN 1 ELSE 2 END ASC, " + // 재직자 상단, 휴직/퇴직 하단
                         "  p.position_level DESC, " +                           // 직급 높은 순
                         "  e.emp_no ASC";                                        // 사번 순
                         
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

    /**
     * 부서장 후보 사원 목록 (재직자만)
     * EmpDTO 기준: status = '재직' 조건
     * 반환 Map 키: empId, empName, posName
     */
    public List<Map<String, Object>> getEmpList() {
        List<Map<String, Object>> list = new ArrayList<>();
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "SELECT e.emp_id, e.emp_name, " +
                         "COALESCE(p.position_name, '미지정') AS position_name " +
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

    /**
     * 부서 신규 등록
     */
    public boolean insertDept(DeptDTO dept) {
        Connection con = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "INSERT INTO department " +
                         "(dept_name, parent_dept_id, manager_id, dept_level, sort_order, is_active) " +
                         "VALUES (?, ?, ?, ?, ?, 1)";
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, dept.getDept_name());
            pstmt.setInt(2, dept.getParent_dept_id());
            pstmt.setInt(3, dept.getManager_id());
            pstmt.setInt(4, dept.getDept_level());
            pstmt.setInt(5, dept.getSort_order());
            success = pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResources(null, pstmt, con);
        }
        return success;
    }

    /**
     * 부서 정보 수정
     */
    public boolean updateDept(DeptDTO dept) {
        Connection con = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        try {
            con = DatabaseConnection.getConnection();
            String sql = "UPDATE department SET " +
                         "dept_name = ?, parent_dept_id = ?, manager_id = ?, " +
                         "dept_level = ?, sort_order = ?, is_active = ? " +
                         "WHERE dept_id = ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, dept.getDept_name());
            pstmt.setInt(2, dept.getParent_dept_id());
            pstmt.setInt(3, dept.getManager_id());
            pstmt.setInt(4, dept.getDept_level());
            pstmt.setInt(5, dept.getSort_order());
            pstmt.setInt(6, dept.getIs_active());
            pstmt.setInt(7, dept.getDept_id());
            success = pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResources(null, pstmt, con);
        }
        return success;
    }

    /**
     * 부서 폐지: 실제 삭제 대신 is_active=0, closed_at=NOW()
     */
    public boolean deleteDept(int deptId) {
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        boolean success = false;

        try {
            con = DatabaseConnection.getConnection();

            // 1. 소속 직원이 있는지 확인 (재직 중인 직원)
            String checkEmpSql = "SELECT COUNT(*) FROM employee WHERE dept_id = ? AND status = '재직'";
            pstmt = con.prepareStatement(checkEmpSql);
            pstmt.setInt(1, deptId);
            rs = pstmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return false; // 직원이 있으면 중단
            }
            rs.close(); // ResultSet 먼저 닫기
            pstmt.close(); // PreparedStatement 닫기

            // 2. 하위 부서가 있는지 확인 (is_active = 1인 부서만)
            String checkChildSql = "SELECT COUNT(*) FROM department WHERE parent_dept_id = ? AND is_active = 1";
            pstmt = con.prepareStatement(checkChildSql);
            pstmt.setInt(1, deptId);
            rs = pstmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                return false; // 하위 부서가 있으면 중단
            }
            rs.close();
            pstmt.close();

            // 3. 실제 폐지 업데이트 실행
            String sql = "UPDATE department SET is_active = 0, closed_at = NOW() WHERE dept_id = ?";
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, deptId);
            
            success = pstmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResources(rs, pstmt, con);
        }
        return success;
    }
    
    /**
     * 비활성 부서 목록 조회 (is_active = 0)
     * HR 관리자 탭에서 폐지된 부서들을 확인할 때 사용
     */
    public List<DeptDTO> getInactiveDepts() {
        List<DeptDTO> list = new ArrayList<>();
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            // is_active = 0인 부서만 조회하며, 폐지일(closed_at) 최신순으로 정렬
            String sql = "SELECT dept_id, dept_name, parent_dept_id, manager_id, " +
                         "dept_level, sort_order, is_active, closed_at, created_at " +
                         "FROM department " +
                         "WHERE is_active = 0 " +
                         "ORDER BY closed_at DESC, dept_id ASC";
            
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
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResources(rs, pstmt, con);
        }
        return list;
    }

    private void closeResources(ResultSet rs, PreparedStatement pstmt, Connection con) {
        try {
            if (rs != null)    rs.close();
            if (pstmt != null) pstmt.close();
            if (con != null)   con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}