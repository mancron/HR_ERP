package com.hrms.org.controller;

import com.hrms.org.dto.DeptDTO;
import com.hrms.org.service.DeptService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.PrintWriter;
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

        if ("new".equals(action) && !isPrivileged) {
            response.sendRedirect(request.getContextPath() + "/org/dept?error=no_auth");
            return;
        }

        // [수정 포인트] AJAX 검색 결과 JSON 생성 로직 보완
        if ("findDeptByEmp".equals(action)) {
            String searchVal = request.getParameter("empName");
            List<Map<String, Object>> results = deptService.findDeptIdByEmpName(searchVal);
            
            response.setContentType("application/json; charset=UTF-8");
            PrintWriter out = response.getWriter();

            if (results.isEmpty()) {
                out.write("{\"status\": \"none\", \"deptId\": 0}");
            } 
            else if (results.size() == 1) {
                // 단일 결과일 때도 키값 매칭 유연화
                Map<String, Object> m = results.get(0);
                Object dId = getMapValue(m, "deptId");
                out.write("{\"status\": \"success\", \"deptId\": " + (dId != null ? dId : 0) + "}");
            } 
            else {
                StringBuilder sb = new StringBuilder();
                sb.append("{\"status\": \"multiple\", \"list\": [");
                for (int i = 0; i < results.size(); i++) {
                    Map<String, Object> m = results.get(i);
                    
                    // [해결] undefined 방지를 위해 모든 필드를 명시적으로 추출 (empName 추가)
                    Object dId = getMapValue(m, "deptId");
                    Object dName = getMapValue(m, "deptName");
                    Object pName = getMapValue(m, "posName");
                    Object eName = getMapValue(m, "empName"); // JSP에서 요구하는 이름 필드
                    Object eNo = getMapValue(m, "empNo");

                    sb.append(String.format(
                        "{\"deptId\":%s, \"deptName\":\"%s\", \"posName\":\"%s\", \"empName\":\"%s\", \"empNo\":\"%s\"}", 
                        (dId != null ? dId : 0),
                        escapeJsonValue(dName), 
                        escapeJsonValue(pName), 
                        escapeJsonValue(eName),
                        escapeJsonValue(eNo)
                    ));
                    
                    if (i < results.size() - 1) sb.append(",");
                }
                sb.append("]}");
                out.write(sb.toString());
            }
            out.flush();
            return;
        }

        request.setAttribute("deptTree",     deptService.getDeptTree());
        request.setAttribute("allDepts",     deptService.getAllDepts());
        request.setAttribute("isPrivileged", isPrivileged);

        if (isPrivileged) {
            request.setAttribute("inactiveDepts", deptService.getInactiveDeptList());
        }

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

            if (selectedDept != null && selectedDept.getIs_active() == 0 && !isPrivileged) {
                response.sendRedirect(request.getContextPath() + "/org/dept?error=no_auth");
                return;
            }

            memberList = deptService.getMembersByDeptId(selectedDeptId);
        }

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
        String idStr = request.getParameter("dept_id");
        
        int existingId = 0;
        boolean isParsingError = false;

        try {
            if (idStr != null && !idStr.isEmpty()) {
                existingId = Integer.parseInt(idStr);
            }
        } catch (NumberFormatException e) {
            isParsingError = true; 
            String referer = request.getHeader("Referer");
            if (referer != null && referer.contains("deptId=")) {
                try {
                    String refId = referer.split("deptId=")[1].split("&")[0];
                    existingId = Integer.parseInt(refId);
                } catch (Exception ignored) {}
            }
        }

        try {
            if (!"insert".equals(action)) {
                if (isParsingError || existingId <= 0) {
                    throw new Exception("조작된 요청 감지: 정상적인 부서 ID가 아님");
                }
            }

            if ("delete".equals(action)) {
                String result = deptService.deleteDept(existingId); 
                if ("SUCCESS".equals(result)) {
                    response.sendRedirect(request.getContextPath() + "/org/dept?msg=deleted");
                } else {
                    response.sendRedirect(request.getContextPath() + "/org/dept?deptId=" + existingId + "&error=" + result.toLowerCase());
                }
            } 
            else if ("update".equals(action)) {
                DeptDTO dept = createDeptFromRequest(request, existingId);
                String result = deptService.saveDept(dept); 
                if ("SUCCESS".equals(result)) {
                    response.sendRedirect(request.getContextPath() + "/org/dept?deptId=" + existingId + "&msg=success");
                } else {
                    response.sendRedirect(request.getContextPath() + "/org/dept?deptId=" + existingId + "&error=" + result.toLowerCase());
                }
            } 
            else if ("insert".equals(action)) {
                DeptDTO dept = createDeptFromRequest(request, 0);
                String result = deptService.saveDept(dept); 
                if ("SUCCESS".equals(result)) {
                    response.sendRedirect(request.getContextPath() + "/org/dept?deptId=" + dept.getDept_id() + "&msg=success");
                } else {
                    response.sendRedirect(request.getContextPath() + "/org/dept?action=new&error=" + result.toLowerCase());
                }
            }
            else {
                throw new Exception("알 수 없는 요청 액션: " + action);
            }

        } catch (Exception e) {
            e.printStackTrace();
            String redirectUrl = request.getContextPath() + "/org/dept?error=fail";
            if (existingId > 0) {
                redirectUrl += "&deptId=" + existingId;
            }
            response.sendRedirect(redirectUrl);
        }
    }

    private DeptDTO createDeptFromRequest(HttpServletRequest request, int id) {
        DeptDTO dept = new DeptDTO();
        dept.setDept_id(id);
        dept.setDept_name(request.getParameter("dept_name"));
        
        String parentStr = request.getParameter("parent_dept_id");
        int parentId = 0;
        if (parentStr != null && !parentStr.isEmpty()) {
            try {
                parentId = Integer.parseInt(parentStr);
            } catch (NumberFormatException e) {
                parentId = 0;
            }
        }
        dept.setParent_dept_id(parentId);
        
        String sortStr = request.getParameter("sort_order");
        int sortOrder = 1;
        if (sortStr != null && !sortStr.isEmpty()) {
            try { 
                sortOrder = (int) Double.parseDouble(sortStr); 
            } catch (NumberFormatException e) { 
                sortOrder = 1; 
            }
        }
        dept.setSort_order(sortOrder);
        
        String activeStr = request.getParameter("is_active");
        int isActive = 1;
        if (activeStr != null && !activeStr.isEmpty()) {
            try {
                isActive = Integer.parseInt(activeStr);
            } catch (NumberFormatException e) {
                isActive = 1;
            }
        }
        dept.setIs_active(isActive);
        
        return dept;
    }

    // [중요] Map에서 대소문자/언더바 구분 없이 값을 가져오는 헬퍼 메서드
    private Object getMapValue(Map<String, Object> map, String key) {
        if (map == null) return null;
        if (map.containsKey(key)) return map.get(key); // camelCase (deptId)
        
        // 대문자 snake_case 시도 (DEPT_ID)
        String upperSnake = key.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
        if (map.containsKey(upperSnake)) return map.get(upperSnake);
        
        // 소문자 snake_case 시도 (dept_id)
        if (map.containsKey(upperSnake.toLowerCase())) return map.get(upperSnake.toLowerCase());
        
        return null;
    }

    private String escapeJsonValue(Object value) {
        if (value == null) return "";
        return value.toString()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}