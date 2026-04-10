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
     * 부서 저장/수정
     *
     * [BUG-1 FIX] parent_dept_id = 0 (최상위) 일 때 shiftSortOrder / reorderSortOrder 를
     *             호출하지 않도록 변경.
     *             DB 상 최상위 부서는 parent_dept_id 가 0 이 아닌 경우가 대부분이며,
     *             0 기준 UPDATE 는 의도치 않은 행을 건드릴 수 있음.
     *             → 최상위로의 이동/신규 등록은 sort_order 조정 없이 그대로 저장.
     *
     * [BUG-2 FIX] 5단계 초과 검증: `newLevel + maxSubDepth > 5` 에서
     *             `newLevel + maxSubDepth > 5` 는 맞으나
     *             maxSubDepth 계산 기준을 명확히 함.
     *             자기 자신(newLevel) + 하위 최대 추가 깊이(maxSubDepth) 가 5를 초과할 때만 차단.
     *             즉 newLevel=4, maxSubDepth=1 → 5 → 허용 (5단계까지 OK)
     *                newLevel=4, maxSubDepth=2 → 6 → 차단
     */
    public String saveDept(DeptDTO dept) {

        // 1. 부서명 필수
        if (dept.getDept_name() == null || dept.getDept_name().trim().isEmpty()) {
            return "NO_DEPT_NAME";
        }

        // 2. 수정 시 기존 데이터 로드
        DeptDTO oldDept = (dept.getDept_id() != 0)
            ? deptDao.getDeptById(dept.getDept_id()) : null;

        // 3. 자기 자신을 상위로 지정 방지
        if (dept.getDept_id() != 0 && dept.getDept_id() == dept.getParent_dept_id()) {
            return "SELF_PARENT";
        }

        // 4. 순환 참조 방지
        if (dept.getDept_id() != 0 && dept.getParent_dept_id() != 0) {
            if (isCircularReference(dept.getDept_id(), dept.getParent_dept_id())) {
                return "CIRCULAR_REFERENCE";
            }
        }

        // 5. 레벨 계산
        int newLevel = 1;
        if (dept.getParent_dept_id() != 0) {
            DeptDTO parent = deptDao.getDeptById(dept.getParent_dept_id());
            if (parent != null) newLevel = parent.getDept_level() + 1;
        }

        // 6. 5단계 초과 방지
        //    [BUG-2 FIX] newLevel + maxSubDepth > 5 이면 차단
        //    (newLevel=5, maxSubDepth=0 → 5 → 허용 / newLevel=5, maxSubDepth=1 → 6 → 차단)
        int maxSubDepth = (dept.getDept_id() != 0) ? getMaxSubDepth(dept.getDept_id(), 0) : 0;
        if (newLevel + maxSubDepth > 5) {
            return "LEVEL_EXCEEDED";
        }

        dept.setDept_level(newLevel);

        // 7. 비활성화 시 멤버/자식 체크
        if (dept.getDept_id() != 0 && dept.getIs_active() == 0) {
            if (deptDao.getMemberCount(dept.getDept_id())    > 0) return "HAS_MEMBERS_INACTIVE";
            if (deptDao.getChildDeptCount(dept.getDept_id()) > 0) return "HAS_CHILDREN_INACTIVE";
        }

        // 8. 순서 조정
        //    [BUG-1 FIX] parent_dept_id = 0 (최상위) 인 경우 shiftSortOrder 건너뜀
        //    → 0 기준 UPDATE 는 의도치 않은 전체 부서 정렬을 건드릴 수 있음
        int newParentId = dept.getParent_dept_id();
        int oldParentId = (oldDept != null) ? oldDept.getParent_dept_id() : -1;

        if (newParentId != 0) {
            if (oldDept != null && oldParentId == newParentId) {
                // 같은 부모 내 순서 이동
                int oldOrder = oldDept.getSort_order();
                int newOrder = dept.getSort_order();
                if (oldOrder != newOrder) {
                    deptDao.shiftSortOrder(newParentId, oldOrder, newOrder);
                }
            } else {
                // 신규 등록 또는 부모 변경
                deptDao.shiftSortOrder(newParentId, 0, dept.getSort_order());
            }
        }

        // 9. 저장
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

        // 10. 순서 재정렬 (빈자리 메우기)
        //     [BUG-1 FIX] parent_dept_id = 0 인 경우 reorderSortOrder 건너뜀
        if (success) {
            if (newParentId != 0) {
                deptDao.reorderSortOrder(newParentId);
            }
            if (oldDept != null && oldParentId != newParentId && oldParentId != 0) {
                deptDao.reorderSortOrder(oldParentId);
            }

            // 11. 하위 부서 레벨 동기화
            if (oldDept != null && oldDept.getDept_level() != newLevel) {
                int diff = newLevel - oldDept.getDept_level();
                updateChildLevelsRecursive(dept.getDept_id(), diff);
            }
        }

        return success ? "SUCCESS" : "FAIL";
    }

    // ──────────────────────────────────────────
    // 내부 유틸
    // ──────────────────────────────────────────

    /**
     * 하위 부서의 최대 추가 깊이 계산
     * 예: 자식 1단계, 손자 1단계 → maxSubDepth = 2
     */
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

    public String deleteDept(int deptId) {
        if (deptDao.hasActiveMembersRecursive(deptId)) return "HAS_MEMBERS";
        if (deptDao.getChildDeptCount(deptId) > 0)    return "HAS_CHILDREN";
        return deptDao.deleteDept(deptId) ? "SUCCESS" : "FAIL";
    }
}