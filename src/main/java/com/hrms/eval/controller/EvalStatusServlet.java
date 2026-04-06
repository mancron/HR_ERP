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
        // HR담당자만 확정 버튼 표시 — 관리자는 일반사용자 취급
        boolean isHr = "HR담당자".equals(userRole);

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int    year   = request.getParameter("year")   != null ? Integer.parseInt(request.getParameter("year"))   : currentYear;
        String period = request.getParameter("period") != null ? request.getParameter("period") : "하반기";
        String type   = request.getParameter("type")   != null ? request.getParameter("type")   : "상위평가";

        String searchTarget    = request.getParameter("searchTarget");
        String searchEvaluator = request.getParameter("searchEvaluator");

        List<Integer> yearList = new ArrayList<>();
        for (int i = 0; i < 3; i++) yearList.add(currentYear - i);

        Vector<Map<String, Object>> statusList =
                evalService.getEvaluationStatusList(year, period, type, searchTarget, searchEvaluator);
        Map<String, Integer> summary = evalService.getEvaluationSummary(year, period, type);

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

        // TODO [알람 포인트 - Status 진입 시]
        // 본인에게 반려된 평가 건 수 조회 → 알림 뱃지용
        // int rejectedCount = evalDao.getMyRejectedCount(loginEmpId);
        // request.setAttribute("myRejectedCount", rejectedCount);

        request.setAttribute("viewPage", "/WEB-INF/jsp/eval/status.jsp");
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }
}
