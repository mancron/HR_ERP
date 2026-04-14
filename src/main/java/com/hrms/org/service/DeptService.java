package com.hrms.org.service;

import com.hrms.org.dao.DeptDAO;
import com.hrms.org.dto.DeptDTO;
import java.util.*;

public class DeptService {

    private DeptDAO deptDao = new DeptDAO();

    public List<DeptDTO>              getAllDepts()               { return deptDao.getAllDepts(); }
    public DeptDTO                    getDeptById(int id)         { return deptDao.getDeptById(id); }
    public List<Map<String, Object>>  getMembersByDeptId(int id)  { return deptDao.getMembersByDeptId(id); }
    public List<Map<String, Object>>  getEmpList()                { return deptDao.getEmpList(); }
    public List<Map<String, Object>>  findDeptIdByEmpName(String n) { return deptDao.findDeptIdByEmpName(n); }
    public int    getMemberCount(int id)        { return deptDao.getMemberCount(id); }
    public int    getChildDeptCount(int id)     { return deptDao.getChildDeptCount(id); }
    public List<DeptDTO> getInactiveDeptList()  { return deptDao.getInactiveDepts(); }

    public String saveDept(DeptDTO dept) { return saveDept(dept, 0); }
    public String deleteDept(int deptId) { return deleteDept(deptId, 0); }

    public List<Map<String, Object>> getDeptTree() {
        List<DeptDTO> allDepts = deptDao.getAllDepts();
        Map<Integer, Map<String, Object>> nodeMap = new LinkedHashMap<>();

        for (DeptDTO d : allDepts) {
            if (d.getIs_active() == 0) continue;
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("deptId",      d.getDept_id());
            node.put("deptName",    d.getDept_name());
            node.put("parentId",    d.getParent_dept_id());
            node.put("isActive",    d.getIs_active());
            node.put("deptLevel",   d.getDept_level());
            node.put("sortOrder",   d.getSort_order());
            node.put("managerName", d.getManager_name());
            node.put("children",    new ArrayList<Map<String, Object>>());
            nodeMap.put(d.getDept_id(), node);
        }

        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> node : nodeMap.values()) {
            int parentId = (int) node.get("parentId");
            if (parentId == 0 || !nodeMap.containsKey(parentId)) {
                roots.add(node);
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) nodeMap.get(parentId).get("children");
                children.add(node);
            }
        }
        return roots;
    }

    /**
     * 부서 저장/수정 (에러 수정 버전)
     */
    public String saveDept(DeptDTO dept, int actorId) {
        // 1. 필수 체크
        if (dept.getDept_name() == null || dept.getDept_name().trim().isEmpty()) return "NO_DEPT_NAME";

        // 2. 변수 사전 선언
        boolean isUpdate = (dept.getDept_id() != 0);
        DeptDTO oldDept = isUpdate ? deptDao.getDeptById(dept.getDept_id()) : null;
        int oldParentId = (oldDept != null) ? oldDept.getParent_dept_id() : -1;
        int newParentId = dept.getParent_dept_id();

        // 3. 검증 (자기 참조 및 순환 참조)
        if (isUpdate && dept.getDept_id() == newParentId) return "SELF_PARENT";
        if (isUpdate && newParentId != 0 && isCircularReference(dept.getDept_id(), newParentId)) return "CIRCULAR_REFERENCE";

        // 4. 레벨 및 깊이 체크
        int newLevel = 1;
        if (newParentId != 0) {
            DeptDTO parent = deptDao.getDeptById(newParentId);
            if (parent != null) newLevel = parent.getDept_level() + 1;
        }
        int maxSubDepth = isUpdate ? getMaxSubDepth(dept.getDept_id(), 0) : 0;
        if (newLevel + maxSubDepth > 5) return "LEVEL_EXCEEDED";
        dept.setDept_level(newLevel);

        // 5. 비활성화 시 제약 체크
        if (isUpdate && dept.getIs_active() == 0) {
            if (deptDao.getMemberCount(dept.getDept_id()) > 0) return "HAS_MEMBERS_INACTIVE";
            if (deptDao.getChildDeptCount(dept.getDept_id()) > 0) return "HAS_CHILDREN_INACTIVE";
        }

        // 6. 순서 조정 (Shift)
        if (oldDept != null && oldParentId == newParentId) {
            if (oldDept.getSort_order() != dept.getSort_order()) {
                deptDao.shiftSortOrder(newParentId, oldDept.getSort_order(), dept.getSort_order());
            }
        } else {
            deptDao.shiftSortOrder(newParentId, 0, dept.getSort_order());
        }

        // 7. DB 저장 및 결과 획득
        int targetId = 0;
        boolean success = false;
        if (isUpdate) {
            success = deptDao.updateDept(dept);
            targetId = dept.getDept_id();
        } else {
            targetId = deptDao.insertDept(dept);
            success = (targetId > 0);
        }

        // 8. 후처리 및 감사 로그 기록
        if (success) {
            deptDao.reorderSortOrder(newParentId);
            if (oldDept != null && oldParentId != newParentId) {
                deptDao.reorderSortOrder(oldParentId);
                int diff = newLevel - oldDept.getDept_level();
                updateChildLevelsRecursive(dept.getDept_id(), diff);
            }

            // --- 로그 기록 섹션 수정 ---
            String logAction = isUpdate ? "UPDATE" : "INSERT";
            String columnName = "부서정보"; // 통합 수정 시
            String oldValue = isUpdate ? String.format("명:%s, 부모:%d", oldDept.getDept_name(), oldDept.getParent_dept_id()) : null;
            String newValue = String.format("명:%s, 부모:%d, 순서:%d", dept.getDept_name(), newParentId, dept.getSort_order());

            // DAO의 수정된 insertAuditLog(actorId, action, table, targetId, column, old, new) 호출
            deptDao.insertAuditLog(actorId, logAction, "department", targetId, columnName, oldValue, newValue);
        }

        return success ? "SUCCESS" : "FAIL";
    }

    public String deleteDept(int deptId, int actorId) {
        DeptDTO dept = deptDao.getDeptById(deptId);
        if (dept == null) return "NOT_FOUND";
        if (deptDao.hasActiveMembersRecursive(deptId)) return "HAS_MEMBERS";
        if (deptDao.getChildDeptCount(deptId) > 0) return "HAS_CHILDREN";
        
        boolean success = deptDao.deleteDept(deptId);
        if (success) {
            deptDao.insertAuditLog(actorId, "DELETE", "department", deptId, "is_active", "1", "0(비활성화)");
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
            deptDao.updateDeptLevel(child.getDept_id(), child.getDept_level() + diff);
            updateChildLevelsRecursive(child.getDept_id(), diff);
        }
    }

    private boolean isCircularReference(int currentDeptId, int targetParentId) {
        return checkIsChild(deptDao.getAllDepts(), currentDeptId, targetParentId);
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
}