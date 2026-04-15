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

        // HR담당자 + 사장님 + 최종승인자 모두 관리자 취급 (확정 버튼 표시 등 JSP 분기용)
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
        // 일반 직원: 본인 관련(작성자 or 대상자) 데이터만 조회
        // HR담당자/사장님/최종승인자: 전체 조회
        Vector<Map<String, Object>> statusList =
                evalService.getEvaluationStatusList(year, period, type, searchTarget, searchEvaluator,
                                                     loginEmpId, userRole);

        // summary: 연도/기간/유형 + 권한 기준 집계
        // type == "전체" → 미완료 문서 건수 / type == 특정 → 미완료자 수
        Map<String, Integer> summary =
                evalService.getEvaluationSummary(year, period, type, searchTarget, searchEvaluator,
                                                  loginEmpId, userRole);

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

        // TODO [알람 포인트]
        // 본인에게 반려된 평가 건 수 → 알림 뱃지용
        // int rejectedCount = evalDao.getMyRejectedCount(loginEmpId);
        // request.setAttribute("myRejectedCount", rejectedCount);

        request.setAttribute("viewPage", "/WEB-INF/jsp/eval/status.jsp");
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }
}