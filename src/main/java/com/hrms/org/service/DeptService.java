package com.hrms.org.service;

import com.hrms.org.dao.DeptDAO;
import com.hrms.org.dto.DeptDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeptService {

    private DeptDAO deptDao = new DeptDAO();

    public List<DeptDTO> getAllDepts() {
        return deptDao.getAllDepts();
    }

    public DeptDTO getDeptById(int deptId) {
        return deptDao.getDeptById(deptId);
    }

    public List<Map<String, Object>> getMembersByDeptId(int deptId) {
        return deptDao.getMembersByDeptId(deptId);
    }

    public List<Map<String, Object>> getEmpList() {
        return deptDao.getEmpList();
    }

    /**
     * 계층형 조직도 트리 조립
     *
     * DB의 parent_dept_id 값이 0 또는 NULL인 부서를 루트로 처리.
     * 루트가 하나도 없을 경우(parent_dept_id가 다른 값으로 저장된 경우 대비)
     * dept_level = 1 인 부서를 루트로 fallback.
     *
     * 반환 구조:
     * List<Map> — 최상위 부서 목록
     *   각 Map 키: deptId, deptName, managerId, managerName,
     *              isActive, deptLevel, sortOrder, children(List<Map>)
     */
    public List<Map<String, Object>> getDeptTree() {
        // 1. DB에서 전체 부서 및 직원 목록 조회
        List<DeptDTO> allDepts = deptDao.getAllDepts();
        List<Map<String, Object>> empList = deptDao.getEmpList();

        // 2. empId → empName 매핑용 Map 생성 (관리자 이름 표시용)
        Map<Integer, String> empNameMap = new HashMap<>();
        for (Map<String, Object> emp : empList) {
            empNameMap.put((Integer) emp.get("empId"), (String) emp.get("empName"));
        }

        // 3. 부서 ID를 키로 하는 노드 맵 생성
        Map<Integer, Map<String, Object>> nodeMap = new HashMap<>();

        // [1차 순회] 활성화된 부서들만 Map 노드로 생성
        for (DeptDTO d : allDepts) {
            // 비활성화된(폐지된) 부서는 트리 구성에서 제외
            if (d.getIs_active() == 0) {
                continue;
            }

            Map<String, Object> node = new HashMap<>();
            node.put("deptId",      d.getDept_id());
            node.put("deptName",    d.getDept_name());
            node.put("parentId",    d.getParent_dept_id());
            node.put("managerId",   d.getManager_id());
            node.put("managerName", empNameMap.getOrDefault(d.getManager_id(), ""));
            node.put("isActive",    d.getIs_active());
            node.put("deptLevel",   d.getDept_level());
            node.put("sortOrder",   d.getSort_order());
            node.put("children",    new ArrayList<Map<String, Object>>());
            
            nodeMap.put(d.getDept_id(), node);
        }

        List<Map<String, Object>> roots = new ArrayList<>();

        // [2차 순회] 부모-자식 관계 연결
        for (DeptDTO d : allDepts) {
            // 이미 1차에서 제외된 부서는 건너뜀
            if (d.getIs_active() == 0) {
                continue;
            }

            int parentId = d.getParent_dept_id();
            Map<String, Object> node = nodeMap.get(d.getDept_id());

            // 부모 ID가 0이거나, 부모 노드가 Map에 없는 경우(최상위 루트)
            if (parentId == 0 || !nodeMap.containsKey(parentId)) {
                roots.add(node);
            } else {
                // 부모 노드의 children 리스트에 현재 노드 추가
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> siblings =
                    (List<Map<String, Object>>) nodeMap.get(parentId).get("children");
                siblings.add(node);
            }
        }

        // [Fallback] 만약 논리적 루트는 없는데 데이터는 있는 경우, 최소 레벨 부서를 루트로 지정
        if (roots.isEmpty() && !nodeMap.isEmpty()) {
            int minLevel = allDepts.stream()
                .filter(d -> d.getIs_active() == 1) // 활성 부서 중에서만 계산
                .mapToInt(DeptDTO::getDept_level)
                .min()
                .orElse(1);
                
            for (Map.Entry<Integer, Map<String, Object>> entry : nodeMap.entrySet()) {
                Map<String, Object> node = entry.getValue();
                if ((int)node.get("deptLevel") == minLevel) {
                    roots.add(node);
                }
            }
        }

        return roots;
    }

    /**
     * 부서 저장 (신규/수정 통합)
     * dept_id == 0 → INSERT, dept_id > 0 → UPDATE
     * dept_level 자동 계산
     */
    public boolean saveDept(DeptDTO dept) {
        if (dept.getParent_dept_id() == 0) {
            dept.setDept_level(1);
        } else {
            DeptDTO parent = deptDao.getDeptById(dept.getParent_dept_id());
            dept.setDept_level(parent != null ? parent.getDept_level() + 1 : 1);
        }

        if (dept.getDept_id() == 0) {
            return deptDao.insertDept(dept);
        } else {
            return deptDao.updateDept(dept);
        }
    }

    /**
     * 부서 폐지
     * 소속 재직 직원(status='재직')이 있으면 false 반환
     */
    public boolean deleteDept(int deptId) {
        List<Map<String, Object>> members = deptDao.getMembersByDeptId(deptId);
        // 재직 중인 직원만 체크
        long activeCount = members.stream()
            .filter(m -> "재직".equals(m.get("status")))
            .count();
        if (activeCount > 0) return false;
        return deptDao.deleteDept(deptId);
    }
    
    /**
    * 비활성화(폐지)된 부서 목록 조회
    * 관리자/HR담당자 전용 탭에서 사용됨
    */
   public List<DeptDTO> getInactiveDeptList() {
       // DAO에서 is_active = 0인 데이터를 가져옵니다.
       return deptDao.getInactiveDepts();
   }
}