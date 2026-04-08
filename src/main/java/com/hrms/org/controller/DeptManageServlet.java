package com.hrms.org.controller;

import com.hrms.org.dto.DeptDTO;
import com.hrms.org.service.DeptService;
import com.hrms.emp.dto.EmpDTO;
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

    /**
     * 권한 판정 로직 (수정본)
     * 1. HR담당자: 모든 부서에 대해 true 반환
     * 2. 일반 사원: 수정하려는 부서의 manager_id가 본인 사번(empId)과 일치할 때만 true 반환
     */
    private boolean resolvePrivilege(HttpSession session, int targetDeptId) {
        if (session == null) return false;

        // 세션에서 권한(role)과 사번(empId) 추출
        Object roleObj = session.getAttribute("userRole");
        Object empIdObj = session.getAttribute("empId");
        
        String userRole = (roleObj != null) ? String.valueOf(roleObj).trim() : "";

        // [1] HR담당자는 프리패스
        if ("HR담당자".equals(userRole)) {
            return true;
        }

        // [2] 부서장 여부 체크 (기존 서비스의 getDeptById 활용)
        if (empIdObj != null && targetDeptId > 0) {
            try {
                int myEmpId = Integer.parseInt(String.valueOf(empIdObj));
                
                // DB에서 해당 부서의 상세 정보(manager_id 포함)를 가져옴
                DeptDTO targetDept = deptService.getDeptById(targetDeptId);
                
                // 부서 정보가 존재하고, 해당 부서에 등록된 manager_id가 내 사번과 일치하는지 확인
                if (targetDept != null && targetDept.getManager_id() == myEmpId) {
                    return true; // 이 부서의 관리 권한이 있음
                }
            } catch (NumberFormatException e) {
                // 사번 형식이 잘못된 경우 로그 출력 후 false 유지
                e.printStackTrace();
            }
        }

        // 위 조건에 해당하지 않으면 권한 없음
        return false;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();
        String action = request.getParameter("action");

        // [부서 ID 결정] 파라미터가 없으면 로그인 유저의 부서 ID를 기본값으로 사용
        String deptIdParam = request.getParameter("deptId");
        int selectedDeptId = 1;

        if (deptIdParam != null && !deptIdParam.isEmpty()) {
            try {
                selectedDeptId = Integer.parseInt(deptIdParam);
            } catch (NumberFormatException e) {
                selectedDeptId = 1;
            }
        } else {
            Object loginUserObj = session.getAttribute("loginUser");
            if (loginUserObj instanceof EmpDTO) {
                selectedDeptId = ((EmpDTO) loginUserObj).getDept_id();
            }
        }

        // [권한 판정] 선택된 부서 ID를 기준으로 수정 권한 여부 확인
        boolean isPrivileged = resolvePrivilege(session, selectedDeptId);

        // 신규 등록(new) 페이지는 오직 HR담당자만 접근 가능하도록 설정
        if ("new".equals(action)) {
            Object roleObj = session.getAttribute("userRole");
            if (!"HR담당자".equals(String.valueOf(roleObj).trim())) {
                response.sendRedirect(request.getContextPath() + "/org/dept?error=no_auth");
                return;
            }
        }

        // AJAX 사원 검색 처리 (기존 로직 유지)
        if ("findDeptByEmp".equals(action)) {
            String searchVal = request.getParameter("empName");
            List<Map<String, Object>> results = deptService.findDeptIdByEmpName(searchVal);

            response.setContentType("application/json; charset=UTF-8");
            PrintWriter out = response.getWriter();

            if (results == null || results.isEmpty()) {
                out.write("{\"status\": \"none\", \"deptId\": 0}");
            } else if (results.size() == 1) {
                Object dId = getMapValue(results.get(0), "deptId");
                out.write("{\"status\": \"success\", \"deptId\": " + (dId != null ? dId : 0) + "}");
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("{\"status\": \"multiple\", \"list\": [");
                for (int i = 0; i < results.size(); i++) {
                    Map<String, Object> m = results.get(i);
                    sb.append(String.format(
                        "{\"deptId\":%s,\"deptName\":\"%s\",\"posName\":\"%s\",\"empName\":\"%s\",\"empNo\":\"%s\"}",
                        getMapValue(m, "deptId") != null ? getMapValue(m, "deptId") : 0,
                        escapeJsonValue(getMapValue(m, "deptName")),
                        escapeJsonValue(getMapValue(m, "posName")),
                        escapeJsonValue(getMapValue(m, "empName")),
                        escapeJsonValue(getMapValue(m, "empNo"))
                    ));
                    if (i < results.size() - 1) sb.append(",");
                }
                sb.append("]}");
                out.write(sb.toString());
            }
            out.flush();
            return;
        }

        try {
            request.setAttribute("deptTree",     deptService.getDeptTree());
            request.setAttribute("allDepts",     deptService.getAllDepts());
            request.setAttribute("isPrivileged", String.valueOf(isPrivileged));

            // 비활성 부서 목록은 권한이 있을 때만 조회 가능
            if (isPrivileged) {
                request.setAttribute("inactiveDepts", deptService.getInactiveDeptList());
            }

            DeptDTO selectedDept = null;
            List<Map<String, Object>> memberList = null;

            if ("new".equals(action)) {
                // 이미 위에서 HR담당자 체크를 했으므로 바로 생성
                selectedDept = new DeptDTO();
                selectedDept.setIs_active(1);
            } else {
                selectedDept = deptService.getDeptById(selectedDeptId);

                // 비관리자의 비활성 부서 접근 차단
                if (selectedDept != null && selectedDept.getIs_active() == 0 && !isPrivileged) {
                    response.sendRedirect(request.getContextPath() + "/org/dept?error=no_auth");
                    return;
                }
                memberList = deptService.getMembersByDeptId(selectedDeptId);
            }

            if (selectedDept == null) {
                selectedDept = new DeptDTO();
                selectedDept.setIs_active(1);
            }

            request.setAttribute("selectedDept",   selectedDept);
            request.setAttribute("selectedDeptId", selectedDeptId);
            request.setAttribute("memberList",      memberList);

            request.setAttribute("viewPage", "/WEB-INF/jsp/org/deptManage.jsp");
            request.getRequestDispatcher("/index.jsp").forward(request, response);

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/main");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession();
        String action = request.getParameter("action");
        String idStr  = request.getParameter("dept_id");
        
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

        // [POST 권한 판정] 해당 부서에 대한 수정 권한이 있는지 최종 확인
        boolean isPrivileged = resolvePrivilege(session, existingId);

        if (!isPrivileged) {
            response.sendRedirect(request.getContextPath() + "/org/dept?error=no_auth");
            return;
        }

        try {
            if (!"insert".equals(action)) {
                if (isParsingError || existingId <= 0) {
                    throw new Exception("잘못된 부서 ID");
                }
            }

            if ("delete".equals(action)) {
                String result = deptService.deleteDept(existingId);
                String msg = "SUCCESS".equals(result) ? "msg=deleted" : "error=" + result.toLowerCase();
                response.sendRedirect(request.getContextPath() + "/org/dept?" + msg);

            } else if ("update".equals(action) || "insert".equals(action)) {
                int deptId = "update".equals(action) ? existingId : 0;
                DeptDTO dept = createDeptFromRequest(request, deptId);

                if (dept.getSort_order() > 99) {
                    String target = "update".equals(action) ? "deptId=" + existingId : "action=new";
                    response.sendRedirect(request.getContextPath() + "/org/dept?" + target + "&error=sort_order_limit");
                    return;
                }

                String result = deptService.saveDept(dept);
                if ("SUCCESS".equals(result)) {
                    int redirectId = (deptId == 0) ? dept.getDept_id() : deptId;
                    response.sendRedirect(request.getContextPath() + "/org/dept?deptId=" + redirectId + "&msg=success");
                } else {
                    String target = "update".equals(action) ? "deptId=" + existingId : "action=new";
                    response.sendRedirect(request.getContextPath() + "/org/dept?" + target + "&error=" + result.toLowerCase());
                }
            } else {
                throw new Exception("알 수 없는 액션: " + action);
            }

        } catch (Exception e) {
            e.printStackTrace();
            String redirectUrl = request.getContextPath() + "/org/dept?error=fail";
            if (existingId > 0) redirectUrl += "&deptId=" + existingId;
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
            try { parentId = Integer.parseInt(parentStr); }
            catch (NumberFormatException e) { parentId = 0; }
        }
        dept.setParent_dept_id(parentId);

        String sortStr = request.getParameter("sort_order");
        int sortOrder = 1;
        if (sortStr != null && !sortStr.isEmpty()) {
            try { sortOrder = (int) Double.parseDouble(sortStr); }
            catch (NumberFormatException e) { sortOrder = 1; }
        }
        dept.setSort_order(sortOrder);

        String activeStr = request.getParameter("is_active");
        int isActive = 1;
        if (activeStr != null && !activeStr.isEmpty()) {
            try { isActive = Integer.parseInt(activeStr); }
            catch (NumberFormatException e) { isActive = 1; }
        }
        dept.setIs_active(isActive);

        return dept;
    }

    private Object getMapValue(Map<String, Object> map, String key) {
        if (map == null) return null;
        if (map.containsKey(key)) return map.get(key);
        String upperSnake = key.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
        if (map.containsKey(upperSnake)) return map.get(upperSnake);
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