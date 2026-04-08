package com.hrms.eval.controller;

import com.hrms.eval.service.EvaluationService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

import java.io.IOException;
import java.util.*;

@WebServlet("/eval/status")
public class EvalStatusServlet extends HttpServlet {

    private EvaluationService evalService = new EvaluationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("empId") == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }

        int    loginEmpId = (Integer) session.getAttribute("empId");
        String userRole   = (String)  session.getAttribute("userRole");
        if (userRole == null) userRole = "일반사원";
        boolean isHr = "HR담당자".equals(userRole);

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        String yearParam = request.getParameter("year");
        int    year      = (yearParam != null && !yearParam.isEmpty())
                           ? Integer.parseInt(yearParam) : currentYear;
        String period    = request.getParameter("period") != null ? request.getParameter("period") : "전체";
        String type      = request.getParameter("type")   != null ? request.getParameter("type")   : "전체";

        String searchTarget    = request.getParameter("searchTarget");
        String searchEvaluator = request.getParameter("searchEvaluator");

        List<Integer> yearList = new ArrayList<>();
        for (int i = 0; i < 3; i++) yearList.add(currentYear - i);

        // [NEW-6 해결] statusList와 summary 모두에 검색 필터(이름)를 적용
        // 테이블 리스트 가져오기
        Vector<Map<String, Object>> statusList =
                evalService.getEvaluationStatusList(year, period, type, searchTarget, searchEvaluator);
        
        // [수정 포인트] 요약 카드 집계 시에도 검색어 파라미터를 전달하여 검색 결과와 요약 수치를 동기화
        Map<String, Integer> summary =
                evalService.getEvaluationSummary(year, period, type, searchTarget, searchEvaluator);

        request.setAttribute("statusList",      statusList);
        request.setAttribute("summary",         summary);
        request.setAttribute("yearList",        yearList);
        request.setAttribute("selectedYear",    year);
        request.setAttribute("selectedPeriod",  period);
        request.setAttribute("selectedType",    type);
        request.setAttribute("searchTarget",    searchTarget    != null ? searchTarget    : "");
        request.setAttribute("searchEvaluator", searchEvaluator != null ? searchEvaluator : "");
        request.setAttribute("loginEmpId",      loginEmpId);
        request.setAttribute("userRole",        userRole);
        request.setAttribute("isHr",            isHr);

        request.setAttribute("viewPage", "/WEB-INF/jsp/eval/status.jsp");
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }
}