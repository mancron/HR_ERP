package com.hrms.sys.controller;

import com.hrms.sys.dto.TextToSqlResultDTO;
import com.hrms.sys.service.TextToSqlService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/sys/sqlQuery")
public class TextToSqlServlet extends HttpServlet {

    private TextToSqlService textToSqlService;

    @Override
    public void init() throws ServletException {
        this.textToSqlService = new TextToSqlService();
    }

    // ── GET: 빈 화면 표시 ──
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.getRequestDispatcher("/WEB-INF/jsp/sys/sys_sql_query.jsp")
               .forward(request, response);
    }

    // ── POST: 쿼리 실행 ──
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 권한 이중 검증
        HttpSession session = request.getSession(false);
        String role = (session != null) ? (String) session.getAttribute("userRole") : null;

        // 관리자 + HR담당자 + 최종승인자 허용
        boolean allowed = "관리자".equals(role)
                       || "HR담당자".equals(role)
                       || "최종승인자".equals(role);

        if (!allowed) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        

        request.setCharacterEncoding("UTF-8");
        String question = request.getParameter("question");

        if (question == null || question.trim().isEmpty()) {
            request.setAttribute("errorMsg", "질문을 입력해주세요.");
            request.getRequestDispatcher("/WEB-INF/jsp/sys/sys_sql_query.jsp")
                   .forward(request, response);
            return;
        }

        // 서비스 호출
        TextToSqlResultDTO result = textToSqlService.query(question.trim());

        request.setAttribute("result",   result);
        request.setAttribute("question", question.trim());

        request.getRequestDispatcher("/WEB-INF/jsp/sys/sys_sql_query.jsp")
               .forward(request, response);
    }
}