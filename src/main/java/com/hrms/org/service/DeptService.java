package com.hrms.org.service;

import com.hrms.org.dao.DeptDAO;
import com.hrms.org.dto.DeptDTO;
import java.util.*;

public class DeptService {

    private DeptDAO deptDao = new DeptDAO();

    public List<DeptDTO> getAllDepts() { return deptDao.getAllDepts(); }
    public DeptDTO getDeptById(int deptId) { return deptDao.getDeptById(deptId); }
    public List<Map<String, Object>> getMembersByDeptId(int deptId) { return deptDao.getMembersByDeptId(deptId); }
    public List<Map<String, Object>> getEmpList() { return deptDao.getEmpList(); }
    public int findDeptIdByEmpName(String empName) { return deptDao.findDeptIdByEmpName(empName); }
    public int getMemberCount(int deptId) { return deptDao.getMemberCount(deptId); }
    public int getChildDeptCount(int deptId) { return deptDao.getChildDeptCount(deptId); }
    public List<DeptDTO> getInactiveDeptList() { return deptDao.getInactiveDepts(); }

    public List<Map<String, Object>> getDeptTree() {
        List<DeptDTO> allDepts = deptDao.getAllDepts();
        List<Map<String, Object>> empList = deptDao.getEmpList();
        Map<Integer, String> empNameMap = new HashMap<>();
        for (Map<String, Object> emp : empList) {
            empNameMap.put((Integer) emp.get("empId"), (String) emp.get("empName"));
        }

        Map<Integer, Map<String, Object>> nodeMap = new HashMap<>();
        for (DeptDTO d : allDepts) {
            if (d.getIs_active() == 0) continue;
            Map<String, Object> node = new HashMap<>();
            node.put("deptId", d.getDept_id());
            node.put("deptName", d.getDept_name());
            node.put("parentId", d.getParent_dept_id());
            node.put("managerId", d.getManager_id());
            node.put("managerName", empNameMap.getOrDefault(d.getManager_id(), ""));
            node.put("isActive", d.getIs_active());
            node.put("deptLevel", d.getDept_level());
            node.put("sortOrder", d.getSort_order());
            node.put("children", new ArrayList<Map<String, Object>>());
            nodeMap.put(d.getDept_id(), node);
        }

        List<Map<String, Object>> roots = new ArrayList<>();
        for (DeptDTO d : allDepts) {
            if (d.getIs_active() == 0) continue;
            int parentId = d.getParent_dept_id();
            Map<String, Object> node = nodeMap.get(d.getDept_id());
            if (parentId == 0 || !nodeMap.containsKey(parentId)) roots.add(node);
            else ((List<Map<String, Object>>) nodeMap.get(parentId).get("children")).add(node);
        }
        return roots;
    }

    public boolean saveDept(DeptDTO dept) {
        if (dept.getParent_dept_id() == 0) {
            dept.setDept_level(1);
        } else {
            DeptDTO parent = deptDao.getDeptById(dept.getParent_dept_id());
            dept.setDept_level(parent != null ? parent.getDept_level() + 1 : 1);
        }
        return (dept.getDept_id() == 0) ? deptDao.insertDept(dept) : deptDao.updateDept(dept);
    }

    public String deleteDept(int deptId) {
        if (deptDao.getMemberCount(deptId) > 0) return "HAS_MEMBERS";
        if (deptDao.getChildDeptCount(deptId) > 0) return "HAS_CHILDREN";
        return deptDao.deleteDept(deptId) ? "SUCCESS" : "FAIL";
    }
}