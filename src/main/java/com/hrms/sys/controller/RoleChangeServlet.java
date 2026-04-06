package com.hrms.sys.controller;

import com.hrms.sys.dto.RoleChangeDTO;
import com.hrms.sys.service.RoleChangeService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebServlet("/sys/roleChange")
public class RoleChangeServlet extends HttpServlet {

    private RoleChangeService roleChangeService;

    @Override
    public void init() throws ServletException {
        this.roleChangeService = new RoleChangeService();
    }

    // ── GET: 전체 계정 목록 조회 ──
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session != null) {
            String successMsg = (String) session.getAttribute("successMsg");
            String errorMsg   = (String) session.getAttribute("errorMsg");
            if (successMsg != null) { request.setAttribute("successMsg", successMsg); session.removeAttribute("successMsg"); }
            if (errorMsg   != null) { request.setAttribute("errorMsg",   errorMsg);   session.removeAttribute("errorMsg");   }
        }

        List<RoleChangeDTO> accountList = roleChangeService.getAllAccounts();
        request.setAttribute("accountList", accountList);

        // ── 추가: DB에서 읽은 권한 목록을 JSP에 전달 ──
        List<String> validRoles = roleChangeService.getValidRoles();
        request.setAttribute("validRoles", validRoles);

        Integer myEmpId = (session != null) ? (Integer) session.getAttribute("empId") : null;
        request.setAttribute("myEmpId", myEmpId);

        request.getRequestDispatcher("/WEB-INF/jsp/sys/sys_role_change.jsp")
               .forward(request, response);
    }

    // ── POST: 권한 변경 실행 ──
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 권한 이중 검증
        HttpSession session = request.getSession(false);
        if (session == null || !"관리자".equals(session.getAttribute("userRole"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "관리자만 접근할 수 있습니다.");
            return;
        }

        request.setCharacterEncoding("UTF-8");

        try {
            int     accountId    = Integer.parseInt(request.getParameter("accountId").trim());
            int     targetEmpId  = Integer.parseInt(request.getParameter("targetEmpId").trim());
            String  oldRole      = request.getParameter("oldRole").trim();
            String  newRole      = request.getParameter("newRole").trim();
            Integer actorEmpId   = (Integer) session.getAttribute("empId");

            roleChangeService.changeRole(accountId, targetEmpId, actorEmpId, oldRole, newRole);
            session.setAttribute("successMsg", "권한이 변경되었습니다. (" + oldRole + " → " + newRole + ")");

        } catch (RuntimeException e) {
            session.setAttribute("errorMsg", e.getMessage());
        }

        // PRG 패턴
        response.sendRedirect(request.getContextPath() + "/sys/roleChange");
    }
}