package com.hrms.emp.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

import com.hrms.emp.dto.LeaveDTO;
import com.hrms.emp.dto.ResignDTO;
import com.hrms.emp.service.ApprovalService;

@WebServlet("/emp/approvalHistory")
public class ApprovalHistoryServlet extends HttpServlet {

    private ApprovalService approvalService = new ApprovalService();

    // 페이지당 항목 수
    private static final int PAGE_SIZE    = 10;
    // 블럭당 페이지 수
    private static final int PAGE_PER_BLOCK = 5;

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

        // ── 권한 플래그 계산 ──────────────────────────────
        boolean isPresident       = "최종승인자".equals(userRole);
        boolean isHrManager   = "HR담당자".equals(userRole);
        boolean isDeptManager = approvalService.isDeptManager(loginEmpId);
        boolean isApprover    = isHrManager || isDeptManager || isPresident;

        // ── 결재 처리 결과 검색 조건 ──────────────────────
        // 휴직/복직 처리 결과 검색
        String leaveDoneStatus   = request.getParameter("leaveDoneStatus");
        String leaveDoneKeyword  = request.getParameter("leaveDoneKeyword");
        String leaveDoneDeptName = request.getParameter("leaveDoneDeptName");
        String leaveDoneType     = request.getParameter("leaveDoneType");    // 휴직/복직
        if (leaveDoneStatus == null) leaveDoneStatus = "all";

        // 퇴직 처리 결과 검색
        String resignDoneStatus   = request.getParameter("resignDoneStatus");
        String resignDoneKeyword  = request.getParameter("resignDoneKeyword");
        String resignDoneDeptName = request.getParameter("resignDoneDeptName");
        if (resignDoneStatus == null) resignDoneStatus = "all";

        // ── 내 처리 결과 검색 조건 ────────────────────────
        String myLeaveDoneType   = request.getParameter("myLeaveDoneType");
        String myLeaveDoneStatus = request.getParameter("myLeaveDoneStatus");
        String myResignDoneStatus = request.getParameter("myResignDoneStatus");
        if (myLeaveDoneStatus  == null) myLeaveDoneStatus  = "all";
        if (myResignDoneStatus == null) myResignDoneStatus = "all";

        // ── 페이지 파라미터 ───────────────────────────────
        // 결재 처리 결과 — 휴직/복직 페이지
        int leaveDonePage = parseIntParam(request.getParameter("leaveDonePage"), 1);
        // 결재 처리 결과 — 퇴직 페이지
        int resignDonePage = parseIntParam(request.getParameter("resignDonePage"), 1);
        // 내 처리 결과 — 휴직/복직 페이지
        int myLeaveDonePage = parseIntParam(request.getParameter("myLeaveDonePage"), 1);
        // 내 처리 결과 — 퇴직 페이지
        int myResignDonePage = parseIntParam(request.getParameter("myResignDonePage"), 1);

        // ── 결재 처리 결과 조회 (완료, 결재자만) ──────────
        List<LeaveDTO>  leaveDoneList  = null;
        List<ResignDTO> resignDoneList = null;
        int leaveDoneTotalCount  = 0;
        int resignDoneTotalCount = 0;

        if (isApprover) {
            if (isHrManager || isPresident) {
                leaveDoneTotalCount = approvalService.getLeaveDoneCount(
                        leaveDoneStatus, leaveDoneKeyword, leaveDoneDeptName, leaveDoneType);
                leaveDoneList = approvalService.getLeaveDoneList(
                        leaveDoneStatus, leaveDoneKeyword, leaveDoneDeptName, leaveDoneType,
                        calcOffset(leaveDonePage), PAGE_SIZE);

                resignDoneTotalCount = approvalService.getResignDoneCount(
                        resignDoneStatus, resignDoneKeyword, resignDoneDeptName);
                resignDoneList = approvalService.getResignDoneList(
                        resignDoneStatus, resignDoneKeyword, resignDoneDeptName,
                        calcOffset(resignDonePage), PAGE_SIZE);

            } else if (isDeptManager) {
                leaveDoneTotalCount = approvalService.getLeaveDoneCountForDeptManager(
                        loginEmpId, leaveDoneStatus, leaveDoneKeyword, leaveDoneDeptName, leaveDoneType);
                leaveDoneList = approvalService.getLeaveDoneListForDeptManager(
                        loginEmpId, leaveDoneStatus, leaveDoneKeyword, leaveDoneDeptName, leaveDoneType,
                        calcOffset(leaveDonePage), PAGE_SIZE);

                resignDoneTotalCount = approvalService.getResignDoneCountForDeptManager(
                        loginEmpId, resignDoneStatus, resignDoneKeyword, resignDoneDeptName);
                resignDoneList = approvalService.getResignDoneListForDeptManager(
                        loginEmpId, resignDoneStatus, resignDoneKeyword, resignDoneDeptName,
                        calcOffset(resignDonePage), PAGE_SIZE);
            }
        }

        // ── 내 처리 결과 조회 (완료, 모든 사용자) ─────────
        int myLeaveDoneTotalCount = approvalService.getMyLeaveDoneCount(
                loginEmpId, myLeaveDoneType, myLeaveDoneStatus);
        List<LeaveDTO> myLeaveDoneList = approvalService.getMyLeaveDoneList(
                loginEmpId, myLeaveDoneType, myLeaveDoneStatus,
                calcOffset(myLeaveDonePage), PAGE_SIZE);

        int myResignDoneTotalCount = approvalService.getMyResignDoneCount(
                loginEmpId, myResignDoneStatus);
        List<ResignDTO> myResignDoneList = approvalService.getMyResignDoneList(
                loginEmpId, myResignDoneStatus,
                calcOffset(myResignDonePage), PAGE_SIZE);

        // ── JSP에 전달 ────────────────────────────────────
        request.setAttribute("isApprover",    isApprover);
        request.setAttribute("isPresident",       isPresident);
        request.setAttribute("isHrManager",   isHrManager);
        request.setAttribute("isDeptManager", isDeptManager);

        // 결재 처리 결과 — 휴직/복직
        request.setAttribute("leaveDoneStatus",   leaveDoneStatus);
        request.setAttribute("leaveDoneKeyword",  leaveDoneKeyword);
        request.setAttribute("leaveDoneDeptName", leaveDoneDeptName);
        request.setAttribute("leaveDoneType",     leaveDoneType);
        request.setAttribute("leaveDoneList",     leaveDoneList);
        setPageAttributes(request, "leaveDone", leaveDoneTotalCount, leaveDonePage);

        // 결재 처리 결과 — 퇴직
        request.setAttribute("resignDoneStatus",   resignDoneStatus);
        request.setAttribute("resignDoneKeyword",  resignDoneKeyword);
        request.setAttribute("resignDoneDeptName", resignDoneDeptName);
        request.setAttribute("resignDoneList",     resignDoneList);
        setPageAttributes(request, "resignDone", resignDoneTotalCount, resignDonePage);

        // 내 처리 결과 — 휴직/복직
        request.setAttribute("myLeaveDoneType",   myLeaveDoneType);
        request.setAttribute("myLeaveDoneStatus", myLeaveDoneStatus);
        request.setAttribute("myLeaveDoneList",   myLeaveDoneList);
        setPageAttributes(request, "myLeaveDone", myLeaveDoneTotalCount, myLeaveDonePage);

        // 내 처리 결과 — 퇴직
        request.setAttribute("myResignDoneStatus", myResignDoneStatus);
        request.setAttribute("myResignDoneList",   myResignDoneList);
        setPageAttributes(request, "myResignDone", myResignDoneTotalCount, myResignDonePage);

        request.getRequestDispatcher("/WEB-INF/jsp/emp/approvalHistory.jsp")
               .forward(request, response);
    }

    // ── 페이징 계산 헬퍼 ──────────────────────────────

    // offset 계산: (현재페이지 - 1) * 페이지크기
    private int calcOffset(int nowPage) {
        return (nowPage - 1) * PAGE_SIZE;
    }

    // 파라미터 int 파싱 (실패 시 기본값 반환)
    private int parseIntParam(String param, int defaultVal) {
        if (param == null) return defaultVal;
        try { return Math.max(1, Integer.parseInt(param)); }
        catch (NumberFormatException e) { return defaultVal; }
    }

    // 페이징 관련 attribute 일괄 세팅
    // prefix: leaveDone / resignDone / myLeaveDone / myResignDone
    private void setPageAttributes(HttpServletRequest request,
            String prefix, int totalCount, int nowPage) {
        int totalPage  = (totalCount == 0) ? 1 : (int) Math.ceil((double) totalCount / PAGE_SIZE);
        nowPage        = Math.min(nowPage, totalPage); // 범위 초과 방지

        int nowBlock   = (int) Math.ceil((double) nowPage / PAGE_PER_BLOCK);
        int totalBlock = (int) Math.ceil((double) totalPage / PAGE_PER_BLOCK);
        int pageStart  = (nowBlock - 1) * PAGE_PER_BLOCK + 1;
        int pageEnd    = Math.min(nowBlock * PAGE_PER_BLOCK, totalPage);

        request.setAttribute(prefix + "TotalCount",  totalCount);
        request.setAttribute(prefix + "NowPage",     nowPage);
        request.setAttribute(prefix + "TotalPage",   totalPage);
        request.setAttribute(prefix + "NowBlock",    nowBlock);
        request.setAttribute(prefix + "TotalBlock",  totalBlock);
        request.setAttribute(prefix + "PageStart",   pageStart);
        request.setAttribute(prefix + "PageEnd",     pageEnd);
        request.setAttribute(prefix + "PagePerBlock", PAGE_PER_BLOCK);
    }
}