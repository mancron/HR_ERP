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
        boolean isPrivileged = "관리자".equals(userRole) || "HR담당자".equals(userRole);

        // 1. 조직도 트리 (활성 부서 전용)
        List<Map<String, Object>> deptTree = deptService.getDeptTree();

        // 2. 상위 부서 목록 (전체 부서)
        List<DeptDTO> allDepts = deptService.getAllDepts();

        // 3. 부서장 후보 목록 (직원 리스트)
        List<Map<String, Object>> empList = deptService.getEmpList();

        // 4. 비활성 부서 목록 (권한이 있을 때만)
        if (isPrivileged) {
            List<DeptDTO> inactiveDepts = deptService.getInactiveDeptList();
            request.setAttribute("inactiveDepts", inactiveDepts);
        }

        // 5. 선택 부서 및 액션 처리
        String deptIdParam = request.getParameter("deptId");
        String action      = request.getParameter("action");
        
        DeptDTO selectedDept = null;
        List<Map<String, Object>> memberList = null;
        int selectedDeptId = 0;

        // [수정 포인트] 신규 추가도 아니고, 파라미터도 없을 때 (즉, 메뉴 클릭 시 첫 진입)
        if (deptIdParam == null && action == null) {
            // 최상위 부서(ID: 1)를 기본값으로 로드
            selectedDeptId = 1; 
            selectedDept   = deptService.getDeptById(selectedDeptId);
            memberList     = deptService.getMembersByDeptId(selectedDeptId);
        } 
        else if ("new".equals(action) && isPrivileged) {
            selectedDept = new DeptDTO();
            selectedDept.setIs_active(1); // 신규 생성 시 기본값 활성
        } 
        else if (deptIdParam != null && !deptIdParam.isEmpty()) {
            try {
                selectedDeptId = Integer.parseInt(deptIdParam);
                selectedDept   = deptService.getDeptById(selectedDeptId);
                memberList     = deptService.getMembersByDeptId(selectedDeptId);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        // 최종 방어 코드: 검색 실패 시 빈 객체 생성
        if (selectedDept == null) {
            selectedDept = new DeptDTO();
            selectedDept.setIs_active(1); 
        } else if (!isPrivileged) {
            // 일반 사용자 권한 필터링
            selectedDept.setSort_order(0);
            selectedDept.setDept_level(0);
            selectedDept.setIs_active(1);
        }

        // 결과 세팅
        request.setAttribute("isPrivileged",   isPrivileged); 
        request.setAttribute("deptTree",       deptTree);
        request.setAttribute("allDepts",       allDepts);
        request.setAttribute("empList",        empList);
        request.setAttribute("selectedDept",   selectedDept);
        request.setAttribute("selectedDeptId", selectedDeptId);
        request.setAttribute("memberList",     memberList);

        request.setAttribute("viewPage", "/WEB-INF/jsp/org/deptManage.jsp");
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession();
        String userRole = (String) session.getAttribute("userRole");

        if (!"관리자".equals(userRole) && !"HR담당자".equals(userRole)) {
            response.sendRedirect(request.getContextPath() + "/org/dept?error=no_auth");
            return;
        }

        String action = request.getParameter("action");

        try {
            if ("delete".equals(action)) {
                int deptId  = Integer.parseInt(request.getParameter("dept_id"));
                boolean ok  = deptService.deleteDept(deptId);
                if (ok) {
                    response.sendRedirect(request.getContextPath() + "/org/dept");
                } else {
                    response.sendRedirect(request.getContextPath()
                            + "/org/dept?deptId=" + deptId + "&error=has_members");
                }

            } else {
                DeptDTO dept = new DeptDTO();
                String deptIdStr = request.getParameter("dept_id");
                dept.setDept_id(deptIdStr != null && !deptIdStr.isEmpty() ? Integer.parseInt(deptIdStr) : 0);
                dept.setDept_name(request.getParameter("dept_name"));
                String parentStr = request.getParameter("parent_dept_id");
                dept.setParent_dept_id(parentStr != null && !parentStr.isEmpty() ? Integer.parseInt(parentStr) : 0);
                String managerStr = request.getParameter("manager_id");
                dept.setManager_id(managerStr != null && !managerStr.isEmpty() ? Integer.parseInt(managerStr) : 0);
                String sortStr = request.getParameter("sort_order");
                dept.setSort_order(sortStr != null && !sortStr.isEmpty() ? Integer.parseInt(sortStr) : 1);
                String activeStr = request.getParameter("is_active");
                dept.setIs_active(activeStr != null && !activeStr.isEmpty() ? Integer.parseInt(activeStr) : 1);

                boolean ok = deptService.saveDept(dept);
                if (ok) {
                    response.sendRedirect(request.getContextPath() + "/org/dept");
                } else {
                    response.sendRedirect(request.getContextPath() + "/org/dept?error=save_fail");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/org/dept?error=save_fail");
        }
    }
}