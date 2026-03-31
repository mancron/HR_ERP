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

        // 1. 조직도 트리 (좌측 패널용 계층 Map)
        List<Map<String, Object>> deptTree = deptService.getDeptTree();

        // 2. 상위 부서 드롭다운용 전체 부서 목록
        List<DeptDTO> allDepts = deptService.getAllDepts();

        // 3. 부서장 후보 사원 목록
        List<Map<String, Object>> empList = deptService.getEmpList();

        // 4. 선택 부서 처리
        String deptIdParam = request.getParameter("deptId");
        String action      = request.getParameter("action");
        String errorParam  = request.getParameter("error");

        DeptDTO selectedDept = new DeptDTO();
        List<Map<String, Object>> memberList = null;
        int selectedDeptId = 0;

        if ("new".equals(action)) {
            // 신규 추가 모드: 빈 폼
            selectedDept = new DeptDTO();

        } else if (deptIdParam != null && !deptIdParam.isEmpty()) {
            try {
                selectedDeptId = Integer.parseInt(deptIdParam);
                selectedDept   = deptService.getDeptById(selectedDeptId);
                
                // [수정 포인트 1] DAO가 수정되었다면, 여기서 하위 부서원까지 포함된 리스트를 가져오게 됩니다.
                memberList     = deptService.getMembersByDeptId(selectedDeptId);
                
                if (selectedDept == null) selectedDept = new DeptDTO();
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        // 5. 에러 메시지 처리
        if ("has_members".equals(errorParam)) {
            request.setAttribute("errorMsg", "소속 직원이 있어 부서를 폐지할 수 없습니다.");
        } else if ("save_fail".equals(errorParam)) {
            request.setAttribute("errorMsg", "저장 중 오류가 발생했습니다.");
        } else if ("no_auth".equals(errorParam)) {
            // [추가] 권한 부족 메시지 처리
            request.setAttribute("errorMsg", "해당 작업을 수행할 권한이 없습니다.");
        }

        // TODO: 회사명을 DB 설정값 또는 공통 상수에서 가져오도록 수정
        request.setAttribute("companyName",    "(주)예시회사");
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

        // [수정 포인트 2] 보안: 세션 권한 체크
        HttpSession session = request.getSession();
        String userRole = (String) session.getAttribute("userRole");

        // 관리자나 HR담당자가 아니면 POST 요청(저장/삭제) 처리 거부
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
                // 저장 (신규 or 수정)
                DeptDTO dept = new DeptDTO();

                String deptIdStr = request.getParameter("dept_id");
                dept.setDept_id(deptIdStr != null && !deptIdStr.isEmpty()
                        ? Integer.parseInt(deptIdStr) : 0);

                dept.setDept_name(request.getParameter("dept_name"));

                String parentStr = request.getParameter("parent_dept_id");
                dept.setParent_dept_id(parentStr != null && !parentStr.isEmpty()
                        ? Integer.parseInt(parentStr) : 0);

                String managerStr = request.getParameter("manager_id");
                dept.setManager_id(managerStr != null && !managerStr.isEmpty()
                        ? Integer.parseInt(managerStr) : 0);

                String sortStr = request.getParameter("sort_order");
                dept.setSort_order(sortStr != null && !sortStr.isEmpty()
                        ? Integer.parseInt(sortStr) : 1);

                String activeStr = request.getParameter("is_active");
                dept.setIs_active(activeStr != null && !activeStr.isEmpty()
                        ? Integer.parseInt(activeStr) : 1);

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