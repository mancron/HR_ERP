package com.hrms.org.service;

import com.hrms.org.dao.DeptDAO;
import com.hrms.org.dto.DeptDTO;
import java.util.*;

public class DeptService {

    private DeptDAO deptDao = new DeptDAO();

    // 기본 조회 메서드
    public List<DeptDTO> getAllDepts() { return deptDao.getAllDepts(); }
    public DeptDTO getDeptById(int deptId) { return deptDao.getDeptById(deptId); }
    public List<Map<String, Object>> getMembersByDeptId(int deptId) { return deptDao.getMembersByDeptId(deptId); }
    public List<Map<String, Object>> getEmpList() { return deptDao.getEmpList(); }
    public int findDeptIdByEmpName(String empName) { return deptDao.findDeptIdByEmpName(empName); }
    public int getMemberCount(int deptId) { return deptDao.getMemberCount(deptId); }
    public int getChildDeptCount(int deptId) { return deptDao.getChildDeptCount(deptId); }
    
    // DAO 메서드명과 일치시킴: getInactiveDepts()
    public List<DeptDTO> getInactiveDeptList() { return deptDao.getInactiveDepts(); }

    /**
     * 계층형 조직도 트리 조립
     */
    public List<Map<String, Object>> getDeptTree() {
        List<DeptDTO> allDepts = deptDao.getAllDepts();
        // 순서 유지를 위해 LinkedHashMap 사용
        Map<Integer, Map<String, Object>> nodeMap = new LinkedHashMap<>();
        
        for (DeptDTO d : allDepts) {
            if (d.getIs_active() == 0) continue;
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("deptId", d.getDept_id());
            node.put("deptName", d.getDept_name());
            node.put("parentId", d.getParent_dept_id());
            node.put("isActive", d.getIs_active());
            node.put("deptLevel", d.getDept_level());
            node.put("sortOrder", d.getSort_order());
            node.put("children", new ArrayList<Map<String, Object>>());
            nodeMap.put(d.getDept_id(), node);
        }

        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> node : nodeMap.values()) {
            int parentId = (int) node.get("parentId");
            if (parentId == 0 || !nodeMap.containsKey(parentId)) {
                roots.add(node);
            } else {
                List<Map<String, Object>> children = (List<Map<String, Object>>) nodeMap.get(parentId).get("children");
                children.add(node);
            }
        }
        return roots;
    }

    /**
     * 부서 저장 및 수정 (레벨 동기화 포함)
     */
    public String saveDept(DeptDTO dept) {
        DeptDTO oldDept = (dept.getDept_id() != 0) ? deptDao.getDeptById(dept.getDept_id()) : null;

        if (dept.getDept_id() != 0 && dept.getDept_id() == dept.getParent_dept_id()) return "SELF_PARENT";
        if (dept.getDept_id() != 0 && dept.getParent_dept_id() != 0) {
            if (isCircularReference(dept.getDept_id(), dept.getParent_dept_id())) return "CIRCULAR_REFERENCE";
        }

        int newLevel = 1;
        if (dept.getParent_dept_id() != 0) {
            DeptDTO parent = deptDao.getDeptById(dept.getParent_dept_id());
            if (parent != null) {
                newLevel = parent.getDept_level() + 1;
            }
        }
        
        if (dept.getDept_id() != 0) {
            int maxSubDepth = getMaxSubDepth(dept.getDept_id(), 0); 
            if (newLevel + maxSubDepth > 5) return "LEVEL_EXCEEDED";
        } else {
            if (newLevel > 5) return "LEVEL_EXCEEDED";
        }

        dept.setDept_level(newLevel);

        if (dept.getDept_id() != 0 && dept.getIs_active() == 0) {
            if (deptDao.getMemberCount(dept.getDept_id()) > 0) return "HAS_MEMBERS_INACTIVE";
            if (deptDao.getChildDeptCount(dept.getDept_id()) > 0) return "HAS_CHILDREN_INACTIVE";
        }

        boolean success = (dept.getDept_id() == 0) ? deptDao.insertDept(dept) : deptDao.updateDept(dept);

        if (success && oldDept != null && oldDept.getDept_level() != newLevel) {
            int levelDiff = newLevel - oldDept.getDept_level();
            updateChildLevelsRecursive(dept.getDept_id(), levelDiff);
        }

        return success ? "SUCCESS" : "FAIL";
    }

    private int getMaxSubDepth(int parentId, int currentDepth) {
        List<DeptDTO> children = deptDao.getChildDepts(parentId);
        int maxDepth = currentDepth;
        for (DeptDTO child : children) {
            int depth = getMaxSubDepth(child.getDept_id(), currentDepth + 1);
            if (depth > maxDepth) maxDepth = depth;
        }
        return maxDepth;
    }

    private void updateChildLevelsRecursive(int parentId, int diff) {
        List<DeptDTO> children = deptDao.getChildDepts(parentId);
        for (DeptDTO child : children) {
            int updatedLevel = child.getDept_level() + diff;
            deptDao.updateDeptLevel(child.getDept_id(), updatedLevel);
            updateChildLevelsRecursive(child.getDept_id(), diff);
        }
    }

    private boolean isCircularReference(int currentDeptId, int targetParentId) {
        List<DeptDTO> all = deptDao.getAllDepts();
        return checkIsChild(all, currentDeptId, targetParentId);
    }

    private boolean checkIsChild(List<DeptDTO> all, int parentId, int targetId) {
        for (DeptDTO d : all) {
            if (d.getParent_dept_id() == parentId) {
                if (d.getDept_id() == targetId) return true;
                if (checkIsChild(all, d.getDept_id(), targetId)) return true;
            }
        }
        return false;
    }

    public String deleteDept(int deptId) {
        if (deptDao.hasActiveMembersRecursive(deptId)) return "HAS_MEMBERS";
        if (deptDao.getChildDeptCount(deptId) > 0) return "HAS_CHILDREN";
        return deptDao.deleteDept(deptId) ? "SUCCESS" : "FAIL";
    }
}