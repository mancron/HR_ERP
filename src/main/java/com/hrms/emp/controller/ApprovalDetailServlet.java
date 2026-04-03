package com.hrms.emp.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

import com.hrms.emp.dto.LeaveDTO;
import com.hrms.emp.dto.ResignDTO;
import com.hrms.emp.service.ApprovalService;

@WebServlet("/emp/approvalDetail")
public class ApprovalDetailServlet extends HttpServlet {

    private ApprovalService approvalService = new ApprovalService();

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 세션 체크
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("empId") == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }
        Integer loginEmpId = (Integer) session.getAttribute("empId");
        String userRole    = (String)  session.getAttribute("userRole");

        // 파라미터 확인
        String type = request.getParameter("type"); // "leave" or "resign"
        String idStr = request.getParameter("id");

        if (type == null || idStr == null || idStr.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/emp/approval");
            return;
        }

        int requestId;
        try {
            requestId = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/emp/approval");
            return;
        }

        // 권한 플래그
        boolean isHrManager = "HR담당자".equals(userRole);
        boolean isPresident     = "최종승인자".equals(userRole);
        boolean isDeptManager = approvalService.isDeptManager(loginEmpId);

        // 상세 조회 + 접근 권한 확인
        if ("leave".equals(type)) {
            LeaveDTO detail = approvalService.getLeaveDetail(requestId);

            if (detail == null) {
                response.sendRedirect(request.getContextPath() + "/emp/approval");
                return;
            }

            // 본인이거나 부서장이거나 HR담당자/최종승인자만 접근 가능
            if (!canAccess(loginEmpId, isHrManager, isPresident, isDeptManager, detail.getEmp_id(), detail.getDept_manager_id())) {
                printAccessDenied(response, request.getContextPath());
                return;
            }

            request.setAttribute("requestDetail", detail);

        } else if ("resign".equals(type)) {
            ResignDTO detail = approvalService.getResignDetail(requestId);

            if (detail == null) {
                response.sendRedirect(request.getContextPath() + "/emp/approval");
                return;
            }

            if (!canAccess(loginEmpId, isHrManager, isPresident, isDeptManager, detail.getEmp_id(), detail.getDept_manager_id())) {
                printAccessDenied(response, request.getContextPath());
                return;
            }

            request.setAttribute("requestDetail", detail);

        } else {
            response.sendRedirect(request.getContextPath() + "/emp/approval");
            return;
        }

        request.setAttribute("requestType",  type);
        request.setAttribute("isDeptManager", isDeptManager);
        request.setAttribute("isHrManager",   isHrManager);
        request.setAttribute("isPresident",       isPresident);

        request.getRequestDispatcher("/WEB-INF/jsp/emp/approvalDetail.jsp")
               .forward(request, response);
    }

    /**
     * 접근 권한 확인
     * - 본인(신청자)
     * - 해당 신청의 부서장 (dept_manager_id == loginEmpId)
     * - HR담당자 / 최종승인자
     */
    private boolean canAccess(int loginEmpId, boolean isHrManager, boolean isPresident,
                               boolean isDeptManager, int empId, int deptManagerId) {
        if (isHrManager) return true;
        if (isPresident) return true; 
        if (loginEmpId == empId) return true;
        if (isDeptManager && loginEmpId == deptManagerId) return true;
        return false;
    }

    private void printAccessDenied(HttpServletResponse response, String contextPath)
            throws IOException {
        response.setContentType("text/html; charset=UTF-8");
        java.io.PrintWriter out = response.getWriter();
        out.println("<script>");
        out.println("alert('접근 권한이 없습니다.');");
        out.println("history.back();");
        out.println("</script>");
        out.flush();
    }
}