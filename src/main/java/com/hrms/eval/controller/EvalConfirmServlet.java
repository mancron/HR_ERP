package com.hrms.eval.controller;

import com.hrms.eval.service.EvaluationService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * iframe 모달용 평가 확정/반려 서블릿
 * GET  → confirm.jsp 직접 포워딩 (index.jsp 레이아웃 제외)
 * POST → 확정 or 반려 처리 후 JSON → 부모창(status.jsp) reloadStatusTable() 호출
 *
 * 권한: HR담당자만 확정/반려 가능 (관리자는 일반사용자 취급)
 */
@WebServlet("/eval/confirm")
public class EvalConfirmServlet extends HttpServlet {

    private EvaluationService evalService = new EvaluationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("empId") == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String userRole = (String) session.getAttribute("userRole");
        if (userRole == null) userRole = "일반사원";
        boolean isHr = "HR담당자".equals(userRole);

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        int evalId = Integer.parseInt(idParam);
        Map<String, Object> evalData = evalService.getEvaluationById(evalId);

        if (evalData == null || evalData.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Vector<String>   itemNames  = evalService.getEvaluationItemNames();
        List<BigDecimal> itemScores = evalService.getItemScoresByEvalId(evalId, itemNames);

        // iframe 내부 EL contextPath 문제 방지 → attribute로 직접 전달
        request.setAttribute("evalData",   evalData);
        request.setAttribute("itemNames",  itemNames);
        request.setAttribute("itemScores", itemScores);
        request.setAttribute("gradeColor", evalService.getGradeColor((String) evalData.get("grade")));
        request.setAttribute("userRole",   userRole);
        request.setAttribute("isHr",       isHr);
        request.setAttribute("ctxPath",    request.getContextPath());
        request.setAttribute("evalId",     evalId);

        // confirm.jsp → iframe 전용 (index.jsp 거치지 않음)
        request.getRequestDispatcher("/WEB-INF/jsp/eval/confirm.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("empId") == null) {
            sendJson(response, false, "session_expired");
            return;
        }

        int    loginEmpId = (Integer) session.getAttribute("empId");
        String userRole   = (String)  session.getAttribute("userRole");
        if (userRole == null) userRole = "일반사원";

        // HR담당자만 확정/반려 가능 (관리자는 일반사용자 취급)
        if (!"HR담당자".equals(userRole)) {
            sendJson(response, false, "permission_denied");
            return;
        }

        String action  = request.getParameter("action");
        String idParam = request.getParameter("evalId");

        if (idParam == null || idParam.trim().isEmpty()) {
            sendJson(response, false, "invalid_param");
            return;
        }

        int evalId;
        try { evalId = Integer.parseInt(idParam.trim()); }
        catch (NumberFormatException e) { sendJson(response, false, "invalid_param"); return; }

        boolean ok = false;
        if ("confirm".equals(action)) {
            // 확정: audit_log 트랜잭션 통합 + NotificationUtil 알림
            ok = evalService.confirmEvaluation(evalId, userRole, loginEmpId);

            // TODO [급여 인상 연동 포인트]
            // ok가 true일 때 급여 인상 처리:
            // Map<String,Object> evalData = evalService.getEvaluationById(evalId);
            // salaryService.applyGradeRaise((Integer)evalData.get("empId"), (String)evalData.get("grade"), loginEmpId);

        } else if ("reject".equals(action)) {
            // 반려: audit_log 트랜잭션 통합 + 알림 발송
            ok = evalService.rejectEvaluation(evalId, userRole, loginEmpId);
        } else {
            sendJson(response, false, "invalid_action");
            return;
        }

        sendJson(response, ok, ok ? "success" : "db_error");
    }

    private void sendJson(HttpServletResponse response, boolean ok, String msg) throws IOException {
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write("{\"ok\":" + ok + ",\"msg\":\"" + msg + "\"}");
    }
}
