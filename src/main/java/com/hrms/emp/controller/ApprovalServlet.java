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

@WebServlet("/emp/approval")
public class ApprovalServlet extends HttpServlet {

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

        // ── 권한 플래그 계산 ──────────────────────────────
        boolean isPresident       = "최종승인자".equals(userRole);
        boolean isHrManager   = "HR담당자".equals(userRole);
        boolean isDeptManager = approvalService.isDeptManager(loginEmpId);
        boolean isApprover    = isHrManager || isDeptManager || isPresident;

        // ── 결재 현황 필터 파라미터 ───────────────────────
        String leaveStatusFilter  = request.getParameter("leaveStatus");
        String resignStatusFilter = request.getParameter("resignStatus");
        if (leaveStatusFilter  == null) leaveStatusFilter  = (isHrManager || isPresident) ? "all" : "대기";
        if (resignStatusFilter == null) resignStatusFilter = (isHrManager || isPresident) ? "all" : "대기";

        // ── 결재 현황 검색 조건 ───────────────────────────
        // 휴직/복직 검색 조건
        String leaveKeyword  = request.getParameter("leaveKeyword");
        String leaveDeptName = request.getParameter("leaveDeptName");
        String leaveType     = request.getParameter("leaveType");     // 휴직/복직

        // 퇴직 검색 조건
        String resignKeyword  = request.getParameter("resignKeyword");
        String resignDeptName = request.getParameter("resignDeptName");

        // 내 신청 현황 검색 조건
        String myLeaveType      = request.getParameter("myLeaveType");
        String myLeaveStatus    = request.getParameter("myLeaveStatus");
        String myResignStatus   = request.getParameter("myResignStatus");
        if (myLeaveStatus  == null) myLeaveStatus  = "all";
        if (myResignStatus == null) myResignStatus = "all";

        // ── 결재 현황 목록 조회 (미완료, 결재자만) ────────
        List<LeaveDTO>  leaveApprovalList  = null;
        List<ResignDTO> resignApprovalList = null;

        if (isApprover) {
            if (isHrManager || isPresident) {
                leaveApprovalList  = approvalService.getLeaveListForHr(
                        leaveStatusFilter, leaveKeyword, leaveDeptName, leaveType);
                resignApprovalList = approvalService.getResignListForHr(
                        resignStatusFilter, resignKeyword, resignDeptName);
            } else if (isDeptManager) {
                leaveApprovalList  = approvalService.getLeaveListForDeptManager(
                        loginEmpId, leaveStatusFilter, leaveKeyword, leaveDeptName, leaveType);
                resignApprovalList = approvalService.getResignListForDeptManager(
                        loginEmpId, resignStatusFilter, resignKeyword, resignDeptName);
            }
        }

        // ── 내 신청 현황 조회 (미완료, 모든 사용자) ───────
        List<LeaveDTO>  myLeaveList  = approvalService.getMyLeaveList(loginEmpId, myLeaveType, myLeaveStatus);
        List<ResignDTO> myResignList = approvalService.getMyResignList(loginEmpId, myResignStatus);

        // ── JSP에 전달 ────────────────────────────────────
        request.setAttribute("isApprover",        isApprover);
        request.setAttribute("isDeptManager",      isDeptManager);
        request.setAttribute("isHrManager",        isHrManager);
        request.setAttribute("isPresident",            isPresident);

        // 결재 현황 필터값
        request.setAttribute("leaveStatusFilter",  leaveStatusFilter);
        request.setAttribute("resignStatusFilter", resignStatusFilter);

        // 결재 현황 검색값 (폼 선택값 유지용)
        request.setAttribute("leaveKeyword",       leaveKeyword);
        request.setAttribute("leaveDeptName",      leaveDeptName);
        request.setAttribute("leaveType",          leaveType);
        request.setAttribute("resignKeyword",      resignKeyword);
        request.setAttribute("resignDeptName",     resignDeptName);

        // 내 신청 현황 검색값
        request.setAttribute("myLeaveType",        myLeaveType);
        request.setAttribute("myLeaveStatus",      myLeaveStatus);
        request.setAttribute("myResignStatus",     myResignStatus);

        // 목록
        request.setAttribute("leaveApprovalList",  leaveApprovalList);
        request.setAttribute("resignApprovalList", resignApprovalList);
        request.setAttribute("myLeaveList",        myLeaveList);
        request.setAttribute("myResignList",       myResignList);

        request.getRequestDispatcher("/WEB-INF/jsp/emp/approval.jsp")
               .forward(request, response);
    }
}