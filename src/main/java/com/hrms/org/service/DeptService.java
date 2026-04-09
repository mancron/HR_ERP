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
    public List<Map<String, Object>> findDeptIdByEmpName(String n) { return deptDao.findDeptIdByEmpName(n);}
    public int    getMemberCount(int id)        { return deptDao.getMemberCount(id); }
    public int    getChildDeptCount(int id)     { return deptDao.getChildDeptCount(id); }
    public List<DeptDTO> getInactiveDeptList()  { return deptDao.getInactiveDepts(); }

    /**
     * 계층형 트리 조립 — managerName 포함
     */
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
                List<Map<String, Object>> children =
                    (List<Map<String, Object>>) nodeMap.get(parentId).get("children");
                children.add(node);
            }
        }
        return roots;
    }

    /**
     * 부서 저장/수정 (레벨 동기화 포함)
     */

    public String saveDept(DeptDTO dept) {
        // 1. 필수값 체크
        if (dept.getDept_name() == null || dept.getDept_name().trim().isEmpty()) {
            return "NO_DEPT_NAME";
        }

        // 기존 데이터 가져오기 (수정 시 비교용)
        DeptDTO oldDept = (dept.getDept_id() != 0) ? deptDao.getDeptById(dept.getDept_id()) : null;
        
        // 2. 유효성 검사 (자기 자신을 부모로 설정 방지)
        if (dept.getDept_id() != 0 && dept.getDept_id() == dept.getParent_dept_id()) {
            return "SELF_PARENT";
        }

        // 3. 레벨(Level) 계산
        int newLevel = 1;
        if (dept.getParent_dept_id() != 0) {
            DeptDTO parent = deptDao.getDeptById(dept.getParent_dept_id());
            if (parent != null) newLevel = parent.getDept_level() + 1;
        }
        dept.setDept_level(newLevel);

        // 4. 순서 조정 (Shift)
        // 동일 부모 내에서 이동인지 확인
        if (oldDept != null && oldDept.getParent_dept_id() == dept.getParent_dept_id()) {
            int oldOrder = oldDept.getSort_order();
            int newOrder = dept.getSort_order();

            if (oldOrder != newOrder) {
                // DAO의 shiftSortOrder를 사용하여 사이 번호들 조정
                deptDao.shiftSortOrder(dept.getParent_dept_id(), oldOrder, newOrder);
            }
        } else {
            // 신규 등록이거나 부모 부서가 바뀌는 경우 (기존 방식 유지)
            // oldOrder 위치에 0을 넣어 '새 위치 이후를 밀어내기' 하도록 처리 (오버로딩된 DAO 기준)
            deptDao.shiftSortOrder(dept.getParent_dept_id(), 0, dept.getSort_order());
        }

        // 5. 저장 실행
        boolean success = false;
        if (dept.getDept_id() == 0) {
            int newId = deptDao.insertDept(dept);
            if (newId > 0) {
                dept.setDept_id(newId);
                success = true;
            }
        } else {
            success = deptDao.updateDept(dept);
        }

        // 6. [핵심] 순서 복구 및 정렬 (Restore / Reorder)
        if (success) {
            // 6-1. 현재 (새로운) 부모 부서의 순서 재정렬 (1, 2, 3... 부여)
            deptDao.reorderSortOrder(dept.getParent_dept_id());

            // 6-2. 부모 부서가 바뀌었다면, 이전 부모 부서의 빈자리도 메워줌
            if (oldDept != null && oldDept.getParent_dept_id() != dept.getParent_dept_id()) {
                deptDao.reorderSortOrder(oldDept.getParent_dept_id());
            }
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
    
    public boolean isManager(int empId) {
        return deptDao.isManager(empId);
    }
    public String deleteDept(int deptId) {
        if (deptDao.hasActiveMembersRecursive(deptId)) return "HAS_MEMBERS";
        if (deptDao.getChildDeptCount(deptId)         > 0) return "HAS_CHILDREN";
        return deptDao.deleteDept(deptId) ? "SUCCESS" : "FAIL";
    }
}