package com.hrms.emp.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

import com.hrms.emp.service.LeaveService;

@WebServlet("/emp/leaveWithdraw")
public class LeaveWithdrawServlet extends HttpServlet {
    private LeaveService leaveService = new LeaveService();

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("empId") == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }
        Integer loginEmpId = (Integer) session.getAttribute("empId");

        String idStr = request.getParameter("id");
        int requestId = Integer.parseInt(idStr);

        // 본인 신청이고 대기 상태인지 확인 후 삭제
        String resultMsg = leaveService.withdrawLeave(requestId, loginEmpId);

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