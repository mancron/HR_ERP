package com.hrms.sys.controller;

import com.hrms.sys.dto.AccountUnlockDTO;
import com.hrms.sys.service.AccountUnlockService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebServlet("/sys/accountUnlock")
public class AccountUnlockServlet extends HttpServlet {

    private AccountUnlockService accountUnlockService;

    @Override
    public void init() throws ServletException {
        this.accountUnlockService = new AccountUnlockService();
    }

    // ── GET: 잠금 계정 목록 조회 ──
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // PRG 후 1회성 메시지 처리
        HttpSession session = request.getSession(false);
        if (session != null) {
            String successMsg = (String) session.getAttribute("successMsg");
            String errorMsg   = (String) session.getAttribute("errorMsg");
            if (successMsg != null) {
                request.setAttribute("successMsg", successMsg);
                session.removeAttribute("successMsg");
            }
            if (errorMsg != null) {
                request.setAttribute("errorMsg", errorMsg);
                session.removeAttribute("errorMsg");
            }
        }

        List<AccountUnlockDTO> lockedList = accountUnlockService.getLockedAccounts();
        request.setAttribute("lockedList",  lockedList);
        request.setAttribute("lockedCount", lockedList.size());

        request.getRequestDispatcher("/WEB-INF/jsp/sys/sys_account_unlock.jsp")
               .forward(request, response);
    }

    // ── POST: 잠금 해제 실행 ──
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 권한 이중 검증
        HttpSession session = request.getSession(false);
        if (session == null || !"관리자".equals(session.getAttribute("userRole"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "관리자만 접근할 수 있습니다.");
            return;
        }

        String accountIdParam    = request.getParameter("accountId");
        String loginAttemptsParam = request.getParameter("loginAttempts");

        try {
            int accountId     = Integer.parseInt(accountIdParam.trim());
            int loginAttempts = Integer.parseInt(loginAttemptsParam.trim());
            Integer actorEmpId = (Integer) session.getAttribute("empId");

            accountUnlockService.unlockAccount(accountId, actorEmpId, loginAttempts);
            session.setAttribute("successMsg", "계정 잠금이 해제되었습니다.");

        } catch (RuntimeException e) {
            session.setAttribute("errorMsg", e.getMessage());
        }

        // PRG 패턴
        response.sendRedirect(request.getContextPath() + "/sys/accountUnlock");
    }
}