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

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 세션 체크
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("empId") == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }
        String userRole = (String) session.getAttribute("userRole");

        // 최종승인자/HR담당자만 접근 가능
        if (!"최종승인자".equals(userRole) && !"HR담당자".equals(userRole)) {
            response.setContentType("text/html; charset=UTF-8");
            java.io.PrintWriter out = response.getWriter();
            out.println("<script>");
            out.println("alert('접근 권한이 없습니다.');");
            out.println("history.back();");
            out.println("</script>");
            out.flush();
            return;
        }

        // 검색 파라미터
        String keyword    = request.getParameter("keyword");
        String changeType = request.getParameter("changeType");
        String yearMonth = request.getParameter("yearMonth");
        if (changeType == null) changeType = "";

        // 전체 이력 조회
        List<HistoryDTO> historyList = historyService.getHistoryList(keyword, changeType, yearMonth);

        request.setAttribute("historyList", historyList);
        request.setAttribute("keyword",     keyword);
        request.setAttribute("changeType",  changeType);
        request.setAttribute("yearMonth", yearMonth);

        request.getRequestDispatcher("/WEB-INF/jsp/emp/history.jsp")
               .forward(request, response);
    }
}