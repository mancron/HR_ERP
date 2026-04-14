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

            // 1. 세션에서 로그인한 사용자의 사번과 권한을 가져옴
            int    loginEmpId = (Integer) session.getAttribute("empId");
            String userRole   = (String)  session.getAttribute("userRole");
            if (userRole == null) userRole = "일반사원";
            boolean isHr = "HR담당자".equals(userRole) || "사장님".equals(userRole) || "최종승인자".equals(userRole);

            // 2. 파라미터 수집
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

            // 3. 서비스 호출 (loginEmpId와 userRole을 넘겨서 권한별 데이터 필터링 수행)
            // [수정 포인트]: "CEO" 같은 하드코딩 대신 세션의 userRole과 loginEmpId를 직접 전달
            Vector<Map<String, Object>> statusList =
                    evalService.getEvaluationStatusList(year, period, type, searchTarget, searchEvaluator, loginEmpId, userRole);
            
            Map<String, Integer> summary =
                    evalService.getEvaluationSummary(year, period, type, searchTarget, searchEvaluator, loginEmpId, userRole);

            // 4. 결과 설정 및 포워딩
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
