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
        boolean isPrivileged = "관리자".equals(userRole) || "HR담당자".equals(userRole);

        String action = request.getParameter("action");

        // 1. AJAX 검색
        if ("findDeptByEmp".equals(action)) {
            String empName = request.getParameter("empName");
            int foundDeptId = deptService.findDeptIdByEmpName(empName);
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().write("{\"deptId\": " + foundDeptId + "}");
            return;
        }

        // 2. 공통 데이터 로드
        request.setAttribute("deptTree", deptService.getDeptTree());
        request.setAttribute("allDepts", deptService.getAllDepts());
        request.setAttribute("empList", deptService.getEmpList());
        request.setAttribute("isPrivileged", isPrivileged);

        if (isPrivileged) {
            request.setAttribute("inactiveDepts", deptService.getInactiveDeptList());
        }

        String deptIdParam = request.getParameter("deptId");
        int selectedDeptId = (deptIdParam != null && !deptIdParam.isEmpty()) ? Integer.parseInt(deptIdParam) : (myDeptId != null ? myDeptId : 1);

        if ("new".equals(action) && isPrivileged) {
            DeptDTO newDept = new DeptDTO();
            newDept.setIs_active(1);
            request.setAttribute("selectedDept", newDept);
        } else {
            request.setAttribute("selectedDeptId", selectedDeptId);
            request.setAttribute("selectedDept", deptService.getDeptById(selectedDeptId));
            request.setAttribute("memberList", deptService.getMembersByDeptId(selectedDeptId));
        }

        request.setAttribute("viewPage", "/WEB-INF/jsp/org/deptManage.jsp");
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        try {
            if ("delete".equals(action)) {
                int deptId = Integer.parseInt(request.getParameter("dept_id"));
                String result = deptService.deleteDept(deptId);
                response.sendRedirect(request.getContextPath() + "/org/dept?error=" + result.toLowerCase());
            } else {
                DeptDTO dept = new DeptDTO();
                String idStr = request.getParameter("dept_id");
                dept.setDept_id(idStr != null && !idStr.isEmpty() ? Integer.parseInt(idStr) : 0);
                dept.setDept_name(request.getParameter("dept_name"));
                dept.setParent_dept_id(Integer.parseInt(request.getParameter("parent_dept_id")));
                dept.setManager_id(Integer.parseInt(request.getParameter("manager_id")));
                dept.setSort_order(Integer.parseInt(request.getParameter("sort_order")));
                dept.setIs_active(Integer.parseInt(request.getParameter("is_active")));

                String result = deptService.saveDept(dept);
                // 저장 후 해당 부서 ID를 유지하며 리다이렉트
                response.sendRedirect(request.getContextPath() + "/org/dept?deptId=" + dept.getDept_id() + "&error=" + result.toLowerCase());
            }
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/org/dept?error=fail");
        }
    }
}