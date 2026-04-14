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
     * 권한 판정
     * - HR담당자 : isPrivileged = true  → 수정/등록/폐지/비활성 탭 모두 허용
     * - 그 외(관리자 포함) : isPrivileged = false → 조회만 허용 (일반 사용자 취급)
     */
    private boolean resolvePrivilege(HttpSession session) {
        if (session == null) return false;
        Object roleObj = session.getAttribute("userRole");
        String userRole = (roleObj != null) ? String.valueOf(roleObj).trim() : "";
        return "HR담당자".equals(userRole);
    }

    // ──────────────────────────────────────────
    // GET
    // ──────────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();
        boolean isPrivileged = resolvePrivilege(session);
        String action = request.getParameter("action");

        // 신규 등록 페이지: HR담당자만 접근 가능
        if ("new".equals(action) && !isPrivileged) {
            response.sendRedirect(request.getContextPath() + "/org/dept?error=no_auth");
            return;
        }

        // AJAX 사원 검색 처리
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
            // 모든 부서 목록을 먼저 가져옴 (하위 부서 수집에 필요)
            List<DeptDTO> allDepts = deptService.getAllDepts();
            
            request.setAttribute("deptTree",     deptService.getDeptTree());
            request.setAttribute("allDepts",     allDepts);
            request.setAttribute("isPrivileged", String.valueOf(isPrivileged));

            // 비활성 부서 목록: HR담당자만 조회 가능
            if (isPrivileged) {
                request.setAttribute("inactiveDepts", deptService.getInactiveDeptList());
            }

            // 선택 부서 결정
            String deptIdParam = request.getParameter("deptId");
            int selectedDeptId = 1;

            if (deptIdParam != null && !deptIdParam.isEmpty()) {
                try {
                    selectedDeptId = Integer.parseInt(deptIdParam);
                } catch (NumberFormatException e) {
                    selectedDeptId = 1;
                }
            } else {
                // 세션에서 본인 부서 ID 가져오기
                Object loginUserObj = session.getAttribute("loginUser");
                if (loginUserObj instanceof EmpDTO) {
                    selectedDeptId = ((EmpDTO) loginUserObj).getDept_id();
                }
            }

            DeptDTO selectedDept = null;
            List<Map<String, Object>> memberList = null;
            // [추가] 하위 부서 ID들을 담을 리스트
            java.util.List<Integer> childIds = new java.util.ArrayList<>();

            if ("new".equals(action) && isPrivileged) {
                selectedDept = new DeptDTO();
                selectedDept.setIs_active(1);
            } else {
                selectedDept = deptService.getDeptById(selectedDeptId);

                if (selectedDept != null && allDepts != null) {
                    collectChildIds(selectedDept.getDept_id(), allDepts, childIds);
                }

                // 비활성 부서: HR담당자가 아니면 접근 차단
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
            request.setAttribute("childIds",        childIds);
            request.setAttribute("viewPage", "/WEB-INF/jsp/org/deptManage.jsp");
            request.getRequestDispatcher("/index.jsp").forward(request, response);

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/main");
        }
    }

    // ──────────────────────────────────────────
    // POST — HR담당자만 진입 가능
    // ──────────────────────────────────────────
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession();

        // 1. 세션에서 로그인한 사원의 ID(actorId) 추출
        // 세션에 저장된 객체가 EmpDTO라고 가정합니다. (상단 import 참고)
        Object loginUserObj = session.getAttribute("loginUser");
        int actorId = 0;
        if (loginUserObj instanceof com.hrms.emp.dto.EmpDTO) {
            actorId = ((com.hrms.emp.dto.EmpDTO) loginUserObj).getEmp_id();
        }

        // 2. 권한 판정 (HR담당자만 허용)
        if (!resolvePrivilege(session)) {
            response.sendRedirect(request.getContextPath() + "/org/dept?error=no_auth");
            return;
        }

        String action = request.getParameter("action");
        String idStr  = request.getParameter("dept_id");
        int existingId = 0;
        boolean isParsingError = false;

        // 부서 ID 파싱 로직
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
                    throw new Exception("잘못된 부서 ID");
                }
            }

            // [부서 삭제/비활성화]
            if ("delete".equals(action)) {
                // 수정된 서비스 호출: actorId 전달
                String result = deptService.deleteDept(existingId, actorId);
                String msg = "SUCCESS".equals(result)
                    ? "msg=deleted"
                    : "error=" + result.toLowerCase();
                response.sendRedirect(request.getContextPath() + "/org/dept?" + msg);

            // [부서 등록/수정]
            } else if ("update".equals(action) || "insert".equals(action)) {
                int deptId = "update".equals(action) ? existingId : 0;
                DeptDTO dept = createDeptFromRequest(request, deptId);

                // sort_order 범위 검증 (서버사이드)
                if (dept.getSort_order() > 99) {
                    String target = "update".equals(action)
                        ? "deptId=" + existingId
                        : "action=new";
                    response.sendRedirect(request.getContextPath()
                        + "/org/dept?" + target + "&error=sort_order_limit");
                    return;
                }

                // 수정된 서비스 호출: actorId 전달
                String result = deptService.saveDept(dept, actorId);

                if ("SUCCESS".equals(result)) {
                    int redirectId = (deptId == 0) ? dept.getDept_id() : deptId;
                    response.sendRedirect(request.getContextPath()
                        + "/org/dept?deptId=" + redirectId + "&msg=success");
                } else {
                    String target = "update".equals(action)
                        ? "deptId=" + existingId
                        : "action=new";
                    response.sendRedirect(request.getContextPath()
                        + "/org/dept?" + target + "&error=" + result.toLowerCase());
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

    // ──────────────────────────────────────────
    // 헬퍼 메서드
    // ──────────────────────────────────────────
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
    
    private void collectChildIds(int parentId, List<DeptDTO> allDepts, List<Integer> childIds) {
        if (allDepts == null) return;
        for (DeptDTO d : allDepts) {
            if (d.getParent_dept_id() == parentId) {
                childIds.add(d.getDept_id());
                collectChildIds(d.getDept_id(), allDepts, childIds);
            }
        }
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