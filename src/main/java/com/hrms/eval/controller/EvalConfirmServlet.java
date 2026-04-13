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

        // [C-1] isRejected를 evalData Map에 추가
        // getEvaluationById()는 evalComment만 반환하므로 여기서 직접 계산해서 주입
        String evalComment = (String) evalData.get("evalComment");
        boolean isRejected = evalComment != null && evalComment.contains("[반려]");
        evalData.put("isRejected", isRejected);

        // [반려 사유] 추출해서 별도 attribute로 전달
        // 형식: "평가자 코멘트\n[반려 사유] HR이 적은 사유"
        String rejectReason = "";
        if (evalComment != null && evalComment.contains("[반려 사유]")) {
            int idx = evalComment.indexOf("[반려 사유]");
            rejectReason = evalComment.substring(idx + "[반려 사유]".length()).trim();
        }

        Vector<String>   itemNames  = evalService.getEvaluationItemNames();
        List<BigDecimal> itemScores = evalService.getItemScoresByEvalId(evalId, itemNames);

        // iframe 내부 EL contextPath 문제 방지 → attribute로 직접 전달
        request.setAttribute("evalData",     evalData);
        request.setAttribute("itemNames",    itemNames);
        request.setAttribute("itemScores",   itemScores);
        request.setAttribute("gradeColor",   evalService.getGradeColor((String) evalData.get("grade")));
        request.setAttribute("userRole",     userRole);
        request.setAttribute("isHr",         isHr);
        request.setAttribute("isRejected",   isRejected);
        request.setAttribute("rejectReason", rejectReason);
        request.setAttribute("ctxPath",      request.getContextPath());
        request.setAttribute("evalId",       evalId);

        request.getRequestDispatcher("/WEB-INF/jsp/eval/confirm.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("empId") == null) {
            sendJson(response, false, "세션이 만료되었습니다.");
            return;
        }

        int    loginEmpId = (Integer) session.getAttribute("empId");
        String userRole   = (String)  session.getAttribute("userRole");
        if (userRole == null) userRole = "일반사원";

        if (!"HR담당자".equals(userRole)) {
            sendJson(response, false, "인사평가 확정 권한이 없습니다.");
            return;
        }

        String action  = request.getParameter("action");
        String idParam = request.getParameter("evalId");

        if (idParam == null || idParam.trim().isEmpty()) {
            sendJson(response, false, "잘못된 접근입니다 (ID 누락).");
            return;
        }

        try {
            int evalId = Integer.parseInt(idParam.trim());

            if ("confirm".equals(action)) {
                boolean ok = evalService.confirmEvaluation(evalId, userRole, loginEmpId);

                // TODO [급여 인상 연동 포인트]
                // ok가 true일 때 급여 인상 처리:
                // Map<String,Object> evalData = evalService.getEvaluationById(evalId);
                // salaryService.applyGradeRaise((Integer)evalData.get("empId"), (String)evalData.get("grade"), loginEmpId);

                sendJson(response, ok, ok ? "success" : "처리 중 오류가 발생했습니다.");

            } else if ("reject".equals(action)) {
                // 반려 사유 파라미터 수신
                String rejectReason = request.getParameter("rejectReason");
                if (rejectReason == null) rejectReason = "";
                rejectReason = rejectReason.trim();

                boolean ok = evalService.rejectEvaluation(evalId, userRole, loginEmpId, rejectReason);
                sendJson(response, ok, ok ? "success" : "처리 중 오류가 발생했습니다.");

            } else {
                sendJson(response, false, "유효하지 않은 요청입니다.");
            }

        } catch (NumberFormatException e) {
            sendJson(response, false, "잘못된 접근입니다 (ID 형식 오류).");
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(response, false, e.getMessage());
        }
    }

    private void sendJson(HttpServletResponse response, boolean ok, String msg) throws IOException {
        response.setContentType("application/json; charset=UTF-8");
        String safeMsg = (msg != null) ? msg.replace("\\", "\\\\").replace("\"", "\\\"") : "";
        response.getWriter().write("{\"ok\":" + ok + ",\"msg\":\"" + safeMsg + "\"}");
    }
}
