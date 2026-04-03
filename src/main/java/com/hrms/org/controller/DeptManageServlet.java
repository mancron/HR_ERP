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
        Integer myDeptId = (Integer) session.getAttribute("userDeptId");
        
        // 1. 권한 체크 (기능 유지)
        boolean isPrivileged = "관리자".equals(userRole) || "HR담당자".equals(userRole);
        String action = request.getParameter("action");

        // 2. AJAX 검색 처리 (기능 유지)
        if ("findDeptByEmp".equals(action)) {
            String empName = request.getParameter("empName");
            int foundDeptId = deptService.findDeptIdByEmpName(empName);
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().write("{\"deptId\": " + foundDeptId + "}");
            return;
        }

        // 3. 공통 데이터 로드
        request.setAttribute("deptTree",     deptService.getDeptTree());
        request.setAttribute("allDepts",     deptService.getAllDepts());
        // [수정] 부서장 기능 제거로 인해 empList(부서장 후보) 로드 로직 제거
        request.setAttribute("isPrivileged", isPrivileged);

        // 4. 비활성 부서 목록 (기능 유지)
        if (isPrivileged) {
            request.setAttribute("inactiveDepts", deptService.getInactiveDeptList());
        }

        // 5. 선택된 부서 정보 처리 (기능 유지)
        String deptIdParam = request.getParameter("deptId");
        DeptDTO selectedDept = null;
        List<Map<String, Object>> memberList = null;
        int selectedDeptId = 0;

        if ("new".equals(action) && isPrivileged) {
            selectedDept = new DeptDTO();
            selectedDept.setIs_active(1); 
        } 
        else {
            if (deptIdParam == null || deptIdParam.isEmpty()) {
                selectedDeptId = (myDeptId != null) ? myDeptId : 1;
            } else {
                try {
                    selectedDeptId = Integer.parseInt(deptIdParam);
                } catch (NumberFormatException e) {
                    selectedDeptId = 1;
                }
            }
            selectedDept = deptService.getDeptById(selectedDeptId);
            memberList = deptService.getMembersByDeptId(selectedDeptId);
        }

        // 6. 데이터 보호 및 방어 코드 (기능 유지)
        if (selectedDept == null) {
            selectedDept = new DeptDTO();
            selectedDept.setIs_active(1);
        } else if (!isPrivileged) {
            selectedDept.setSort_order(0);
            selectedDept.setDept_level(0);
        }

        request.setAttribute("selectedDept",   selectedDept);
        request.setAttribute("selectedDeptId", selectedDeptId);
        request.setAttribute("memberList",     memberList);

        // 7. 뷰 포워딩
        request.setAttribute("viewPage", "/WEB-INF/jsp/org/deptManage.jsp");
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession();
        String userRole = (String) session.getAttribute("userRole");

        // 1. 권한 체크
        if (!"관리자".equals(userRole) && !"HR담당자".equals(userRole)) {
            response.sendRedirect(request.getContextPath() + "/org/dept?error=no_auth");
            return;
        }

        String action = request.getParameter("action");

        try {
            if ("delete".equals(action)) {
                // 부서 삭제 로직 (기능 유지)
                int deptId = Integer.parseInt(request.getParameter("dept_id"));
                String result = deptService.deleteDept(deptId); 

                if ("SUCCESS".equals(result)) {
                    response.sendRedirect(request.getContextPath() + "/org/dept?msg=deleted");
                } else {
                    response.sendRedirect(request.getContextPath() + "/org/dept?deptId=" + deptId + "&error=" + result.toLowerCase());
                }

            } else {
                // 저장/수정 로직
                DeptDTO dept = new DeptDTO();
                String idStr = request.getParameter("dept_id");
                dept.setDept_id(idStr != null && !idStr.isEmpty() ? Integer.parseInt(idStr) : 0);
                dept.setDept_name(request.getParameter("dept_name"));
                
                String parentStr = request.getParameter("parent_dept_id");
                dept.setParent_dept_id(parentStr != null && !parentStr.isEmpty() ? Integer.parseInt(parentStr) : 0);
                
                // [수정] manager_id 파라미터 수집 삭제 (DAO/Service 반영 완료)
                
                String sortStr = request.getParameter("sort_order");
                dept.setSort_order(sortStr != null && !sortStr.isEmpty() ? Integer.parseInt(sortStr) : 1);
                
                String activeStr = request.getParameter("is_active");
                dept.setIs_active(activeStr != null && !activeStr.isEmpty() ? Integer.parseInt(activeStr) : 1);

                // 서비스 호출 (기능 유지)
                String result = deptService.saveDept(dept); 

                if ("SUCCESS".equals(result)) {
                    // 성공 시 저장된 부서 ID를 파라미터로 유지
                    int redirectId = (dept.getDept_id() == 0) ? selectedIdAfterInsert() : dept.getDept_id();
                    response.sendRedirect(request.getContextPath() + "/org/dept?deptId=" + redirectId + "&msg=success");
                } else {
                    response.sendRedirect(request.getContextPath() + "/org/dept?deptId=" + dept.getDept_id() + "&error=" + result.toLowerCase());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/org/dept?error=fail");
        }
    }

    // 신규 등록 후 ID를 특정하기 어려운 경우를 위한 헬퍼 (필요 시 사용)
    private int selectedIdAfterInsert() {
        return 1; // 기본적으로 최상위로 보내거나, 로직에 맞춰 수정 가능
    }
}