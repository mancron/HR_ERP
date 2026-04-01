package com.hrms.org.controller;

import com.hrms.org.dto.DeptDTO;
import com.hrms.org.service.DeptService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/org/dept")
public class DeptManageServlet extends HttpServlet {

    private DeptService deptService = new DeptService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();
        String userRole = (String) session.getAttribute("userRole");
        // [수정] 세션에 저장된 본인의 부서 ID를 가져옵니다. (로그인 시 세션에 담아두어야 함)
        Integer myDeptId = (Integer) session.getAttribute("userDeptId");
        
        boolean isPrivileged = "관리자".equals(userRole) || "HR담당자".equals(userRole);

        String action = request.getParameter("action");

        // 1. 부서 검색 (AJAX) - 이건 모든 사용자 허용
        if ("findDeptByEmp".equals(action)) {
            String empName = request.getParameter("empName");
            int foundDeptId = deptService.findDeptIdByEmpName(empName);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"deptId\": " + foundDeptId + "}");
            return; 
        }

        // 공통 데이터 로드
        List<Map<String, Object>> deptTree = deptService.getDeptTree();
        List<DeptDTO> allDepts = deptService.getAllDepts();
        List<Map<String, Object>> empList = deptService.getEmpList();

        if (isPrivileged) {
            request.setAttribute("inactiveDepts", deptService.getInactiveDeptList());
        }

        String deptIdParam = request.getParameter("deptId");
        DeptDTO selectedDept = null;
        List<Map<String, Object>> memberList = null;
        int selectedDeptId = 0;

        // 2. [ID Enumeration 방어 로직 적용]
        if (deptIdParam == null && action == null) {
            // 기본 진입 시: 본인 부서 정보를 보여주거나, 없으면 1번 부서(보통 본사)
            selectedDeptId = (myDeptId != null) ? myDeptId : 1; 
            selectedDept = deptService.getDeptById(selectedDeptId);
            memberList = deptService.getMembersByDeptId(selectedDeptId);
        } else if ("new".equals(action) && isPrivileged) {
            selectedDept = new DeptDTO();
            selectedDept.setIs_active(1); 
        } else if (deptIdParam != null && !deptIdParam.isEmpty()) {
            try {
                int requestedId = Integer.parseInt(deptIdParam);
                
                // [핵심] 권한이 없는데 본인 부서가 아닌 ID를 조회하려 하면 컷!
                if (!isPrivileged && myDeptId != null && requestedId != myDeptId) {
                    // 남의 부서를 훔쳐보려 하면 본인 부서로 강제 고정하거나 에러 페이지 유도
                    selectedDeptId = myDeptId;
                    request.setAttribute("authError", "본인 부서 정보만 조회 가능합니다.");
                } else {
                    selectedDeptId = requestedId;
                }
                
                selectedDept = deptService.getDeptById(selectedDeptId);
                memberList = deptService.getMembersByDeptId(selectedDeptId);
            } catch (NumberFormatException e) { e.printStackTrace(); }
        }

        if (selectedDept == null) {
            selectedDept = new DeptDTO();
            selectedDept.setIs_active(1); 
        }

        // Attribute 셋팅 (기존 유지)
        request.setAttribute("isPrivileged", isPrivileged); 
        request.setAttribute("deptTree", deptTree);
        request.setAttribute("allDepts", allDepts);
        request.setAttribute("empList", empList);
        request.setAttribute("selectedDept", selectedDept);
        request.setAttribute("selectedDeptId", selectedDeptId);
        request.setAttribute("memberList", memberList);

        request.setAttribute("viewPage", "/WEB-INF/jsp/org/deptManage.jsp");
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        String userRole = (String) session.getAttribute("userRole");

        // CUD(생성/수정/삭제)는 오직 권한자만 가능 (물리적 차단)
        if (!"관리자".equals(userRole) && !"HR담당자".equals(userRole)) {
            response.sendRedirect(request.getContextPath() + "/org/dept?error=no_auth");
            return;
        }

        String action = request.getParameter("action");

        try {
            if ("delete".equals(action)) {
                int deptId = Integer.parseInt(request.getParameter("dept_id"));
                String result = deptService.deleteDept(deptId);

                if ("SUCCESS".equals(result)) {
                    response.sendRedirect(request.getContextPath() + "/org/dept");
                } else if ("HAS_MEMBERS".equals(result)) {
                    int count = deptService.getMemberCount(deptId);
                    response.sendRedirect(request.getContextPath() + "/org/dept?deptId=" + deptId + "&error=has_members&count=" + count);
                } else if ("HAS_CHILDREN".equals(result)) {
                    response.sendRedirect(request.getContextPath() + "/org/dept?deptId=" + deptId + "&error=has_children");
                } else {
                    response.sendRedirect(request.getContextPath() + "/org/dept?error=delete_fail");
                }
            } 
            else {
                // 저장 및 수정 로직 (기존 유지)
                DeptDTO dept = new DeptDTO();
                String idStr = request.getParameter("dept_id");
                dept.setDept_id(idStr != null && !idStr.isEmpty() ? Integer.parseInt(idStr) : 0);
                dept.setDept_name(request.getParameter("dept_name"));
                dept.setParent_dept_id(Integer.parseInt(request.getParameter("parent_dept_id")));
                dept.setManager_id(Integer.parseInt(request.getParameter("manager_id")));
                dept.setSort_order(Integer.parseInt(request.getParameter("sort_order")));
                dept.setIs_active(Integer.parseInt(request.getParameter("is_active")));

                boolean ok = deptService.saveDept(dept);
                if (ok) {
                    response.sendRedirect(request.getContextPath() + "/org/dept?deptId=" + dept.getDept_id());
                } else {
                    response.sendRedirect(request.getContextPath() + "/org/dept?error=save_fail");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/org/dept?error=fail");
        }
    }
}