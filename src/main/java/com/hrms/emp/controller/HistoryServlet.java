package com.hrms.emp.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

import com.hrms.emp.dto.HistoryDTO;
import com.hrms.emp.service.HistoryService;

@WebServlet("/emp/history")
public class HistoryServlet extends HttpServlet {

    private HistoryService historyService = new HistoryService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("empId") == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }

        int     empId     = (Integer) session.getAttribute("empId");
        String  role      = (String)  session.getAttribute("userRole");
        Boolean mgrAttr   = (Boolean) session.getAttribute("isManager");
        boolean isManager = (mgrAttr != null && mgrAttr);

        // 검색 파라미터
        String keyword    = request.getParameter("keyword");
        String changeType = request.getParameter("changeType");
        String yearMonth  = request.getParameter("yearMonth");
        if (changeType == null) changeType = "";

        List<HistoryDTO> historyList;

        if ("HR담당자".equals(role) || "최종승인자".equals(role)) {
            // 전사 전체 이력
            historyList = historyService.getHistoryList(keyword, changeType, yearMonth);

        } else if (isManager) {
            // 부서장 — 팀원 + 본인 이력
            historyList = historyService.getHistoryListByDept(empId, keyword, changeType, yearMonth);

        } else {
            // 일반직원·관리자 — 본인 이력만
            historyList = historyService.getHistoryListByEmp(empId, keyword, changeType, yearMonth);
        }

        request.setAttribute("historyList", historyList);
        request.setAttribute("keyword",     keyword);
        request.setAttribute("changeType",  changeType);
        request.setAttribute("yearMonth",   yearMonth);

        request.getRequestDispatcher("/WEB-INF/jsp/emp/history.jsp")
               .forward(request, response);
    }
}