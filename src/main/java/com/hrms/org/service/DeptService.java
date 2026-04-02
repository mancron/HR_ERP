package com.hrms.org.service;

import com.hrms.org.dao.DeptDAO;
import com.hrms.org.dto.DeptDTO;
import java.util.*;

public class DeptService {

    private DeptDAO deptDao = new DeptDAO();

    // --- 기존 단순 조회 메서드들 유지 ---
    public List<DeptDTO> getAllDepts() { return deptDao.getAllDepts(); }
    public DeptDTO getDeptById(int deptId) { return deptDao.getDeptById(deptId); }
    public List<Map<String, Object>> getMembersByDeptId(int deptId) { return deptDao.getMembersByDeptId(deptId); }
    public List<Map<String, Object>> getEmpList() { return deptDao.getEmpList(); }
    public int findDeptIdByEmpName(String empName) { return deptDao.findDeptIdByEmpName(empName); }
    public int getMemberCount(int deptId) { return deptDao.getMemberCount(deptId); }
    public int getChildDeptCount(int deptId) { return deptDao.getChildDeptCount(deptId); }
    public List<DeptDTO> getInactiveDeptList() { return deptDao.getInactiveDepts(); }

    /**
     * [버그 1 수정] 부서 트리 생성 (무제한 레벨 지원)
     * 레벨 4, 5가 실종되지 않도록 nodeMap을 활용한 계층형 구조 재구축
     */
    public List<Map<String, Object>> getDeptTree() {
        List<DeptDTO> allDepts = deptDao.getAllDepts();
        List<Map<String, Object>> empList = deptDao.getEmpList();
        
        // 사원 이름 매핑 (관리자 이름 표시용)
        Map<Integer, String> empNameMap = new HashMap<>();
        for (Map<String, Object> emp : empList) {
            empNameMap.put((Integer) emp.get("empId"), (String) emp.get("empName"));
        }

        // 모든 활성 부서를 Map에 먼저 저장 (순서 유지를 위해 LinkedHashMap)
        Map<Integer, Map<String, Object>> nodeMap = new LinkedHashMap<>();
        for (DeptDTO d : allDepts) {
            if (d.getIs_active() == 0) continue; // 비활성 부서는 트리에 포함 안 함
            
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("deptId", d.getDept_id());
            node.put("deptName", d.getDept_name());
            node.put("parentId", d.getParent_dept_id());
            node.put("managerId", d.getManager_id());
            node.put("managerName", empNameMap.getOrDefault(d.getManager_id(), ""));
            node.put("isActive", d.getIs_active());
            node.put("deptLevel", d.getDept_level());
            node.put("sortOrder", d.getSort_order());
            node.put("children", new ArrayList<Map<String, Object>>()); // 자식 리스트 초기화
            
            nodeMap.put(d.getDept_id(), node);
        }

        List<Map<String, Object>> roots = new ArrayList<>();
        
        // 부모-자식 관계 연결
        for (Map<String, Object> node : nodeMap.values()) {
            int parentId = (int) node.get("parentId");
            
            // 최상위 부서이거나, 부모가 비활성 상태라 nodeMap에 없을 경우 Root로 간주
            if (parentId == 0 || !nodeMap.containsKey(parentId)) {
                roots.add(node);
            } else {
                // 부모의 children 리스트를 가져와서 현재 노드 추가
                List<Map<String, Object>> parentChildren = (List<Map<String, Object>>) nodeMap.get(parentId).get("children");
                parentChildren.add(node);
            }
        }
        return roots;
    }

    /**
     * [버그 2 수정 & 레벨 5 제한] 부서 저장 및 수정
     * 반환값: SUCCESS, FAIL, LEVEL_EXCEEDED, HAS_MEMBERS_INACTIVE
     */
    public String saveDept(DeptDTO dept) {
        // 1. 레벨 계산 및 제한 (Level 5 초과 방지)
        if (dept.getParent_dept_id() == 0) {
            dept.setDept_level(1);
        } else {
            DeptDTO parent = deptDao.getDeptById(dept.getParent_dept_id());
            if (parent != null) {
                int nextLevel = parent.getDept_level() + 1;
                if (nextLevel > 5) {
                    return "LEVEL_EXCEEDED"; // 5단계 초과 시 즉시 차단
                }
                dept.setDept_level(nextLevel);
            } else {
                dept.setDept_level(1); // 부모 정보 없으면 1로 초기화
            }
        }

        // 2. 비활성화 시 검증 (버그 2: 인원이 있으면 비활성 금지)
        // 신규 생성이 아닌 '수정'인 경우 && 비활성화(0)로 상태를 변경하려 할 때
        if (dept.getDept_id() != 0 && dept.getIs_active() == 0) {
            // 해당 부서에 인원이 있는지 체크
            if (deptDao.getMemberCount(dept.getDept_id()) > 0) {
                return "HAS_MEMBERS_INACTIVE"; 
            }
            // 하위 부서가 살아있는데 상위만 죽이는 것도 방지 (선택 사항)
            if (deptDao.getChildDeptCount(dept.getDept_id()) > 0) {
                return "HAS_CHILDREN_INACTIVE";
            }
        }

        // 3. 실제 DB 반영
        boolean success = (dept.getDept_id() == 0) ? deptDao.insertDept(dept) : deptDao.updateDept(dept);
        return success ? "SUCCESS" : "FAIL";
    }

    /**
     * 부서 삭제 (기존 기능 유지)
     */
    public String deleteDept(int deptId) {
        if (deptDao.getMemberCount(deptId) > 0) return "HAS_MEMBERS";
        if (deptDao.getChildDeptCount(deptId) > 0) return "HAS_CHILDREN";
        return deptDao.deleteDept(deptId) ? "SUCCESS" : "FAIL";
    }
}