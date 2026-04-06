package com.hrms.emp.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

import com.hrms.emp.service.ApprovalActionService;

@WebServlet("/emp/approvalAction")
public class ApprovalActionServlet extends HttpServlet {

    private ApprovalActionService approvalActionService = new ApprovalActionService();

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
        String type   = request.getParameter("type");   // leave or resign
        String idStr  = request.getParameter("id");
        String action = request.getParameter("action"); // approve or reject

        if (type == null || idStr == null || action == null) {
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
        boolean isHrManager  = "HR담당자".equals(userRole);
        boolean isPresident      = "최종승인자".equals(userRole);
        boolean isDeptManager = approvalActionService.isDeptManager(loginEmpId);

        // 반려 사유 (반려일 때만 사용)
        String rejectReason = request.getParameter("rejectReason");

        String resultMsg;

        if ("approve".equals(action)) {
            resultMsg = approvalActionService.approve(
                type, requestId, loginEmpId, isHrManager, isDeptManager, isPresident);
        } else if ("reject".equals(action)) {
            resultMsg = approvalActionService.reject(
                type, requestId, loginEmpId, isHrManager, isDeptManager, isPresident, rejectReason);
        } else {
            resultMsg = "잘못된 요청입니다.";
        }

        // 결과 메시지 출력 후 approval 페이지로 이동
        response.setContentType("text/html; charset=UTF-8");
        java.io.PrintWriter out = response.getWriter();
        out.println("<script>");
        out.println("alert('" + resultMsg + "');");
        out.println("if (window.parent && window.parent !== window) {");
        out.println("    // iframe 안에서 실행된 경우 — iframe src 초기화 후 부모창 새로고침");
        out.println("    window.parent.document.getElementById('approvalModalIframe').src = '';");
        out.println("    window.parent.document.getElementById('approvalDetailModal').classList.remove('active');");
        out.println("    window.parent.location.reload();");
        out.println("} else {");
        out.println("    location.href='" + request.getContextPath() + "/emp/approval';");
        out.println("}");
        out.println("</script>");
        out.flush();
    }
}