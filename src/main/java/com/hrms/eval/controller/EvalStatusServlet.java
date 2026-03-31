package com.hrms.eval.controller;

import com.hrms.eval.dao.EvaluationDAO;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;
import java.util.*;

@WebServlet("/eval/status")
public class EvalStatusServlet extends HttpServlet {
    private EvaluationDAO evalDao = new EvaluationDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // 현재 연도 동적으로 가져오기
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        // 필터 파라미터 수집 (기본값: 현재 연도 / 하반기 / 상위평가)
        int year = request.getParameter("year") != null
                ? Integer.parseInt(request.getParameter("year")) : currentYear;
        String period = request.getParameter("period") != null
                ? request.getParameter("period") : "하반기";
        String type = request.getParameter("type") != null
                ? request.getParameter("type") : "상위평가";

        // 연도 드롭다운용 목록 (현재 연도 기준 최근 3년)
        List<Integer> yearList = new ArrayList<>();
        for (int i = 0; i < 3; i++) yearList.add(currentYear - i);

        // 데이터 조회
        Vector<Map<String, Object>> statusList = evalDao.getEvaluationStatusList(year, period, type);
        Map<String, Integer> summary = evalDao.getEvaluationSummary(year, period, type);

        // JSP로 데이터 전달
        request.setAttribute("statusList", statusList);
        request.setAttribute("summary", summary);
        request.setAttribute("yearList", yearList);
        request.setAttribute("selectedYear", year);
        request.setAttribute("selectedPeriod", period);
        request.setAttribute("selectedType", type);

        request.setAttribute("viewPage", "/WEB-INF/jsp/eval/status.jsp");
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }
}