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

        // 세션 정보 및 권한 설정
        int loginEmpId = (Integer) session.getAttribute("empId");
        String userRole = (String) session.getAttribute("userRole");
        if (userRole == null) userRole = "일반사원";
        
        boolean isHr = "HR담당자".equals(userRole);
        boolean isCEO = "사장님".equals(userRole) || "최종승인자".equals(userRole);

        // 파라미터 확인
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

        // 1. 권한 체크를 위한 사번 추출 (평가자 및 피평가자)
        Object evalIdObj = evalData.get("evaluatorId");
        if (evalIdObj == null) evalIdObj = evalData.get("EVALUATOR_ID");
        int evaluatorId = (evalIdObj != null) ? Integer.parseInt(String.valueOf(evalIdObj)) : 0;

        Object targetIdObj = evalData.get("empId"); 
        if (targetIdObj == null) targetIdObj = evalData.get("EMP_ID");
        int targetEmpId = (targetIdObj != null) ? Integer.parseInt(String.valueOf(targetIdObj)) : 0;

        // 2. 보안 필터: HR, CEO, 평가자(작성자), 피평가자(본인) 모두 일단 접근은 허용
        boolean isAuthorized = isHr || isCEO || (loginEmpId == evaluatorId) || (loginEmpId == targetEmpId);

        if (!isAuthorized) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "상세 정보 조회 권한이 없습니다.");
            return;
        }

        // 3. [핵심] 피평가자 본인이 조회하는 경우, 평가자 정보를 익명 처리
        // HR/CEO가 아니고, 내가 '피평가자' 본인이며, 내가 나를 평가한 것이 아닐 때 실행
        if (!isHr && !isCEO && loginEmpId == targetEmpId && loginEmpId != evaluatorId) {
            evalData.put("evaluatorName", "익명");
            evalData.put("evaluatorId", 0);
            evalData.put("evaluatorDept", "비공개");
        }

        // --- 반려 여부 및 사유 처리 ---
        String evalComment = (String) evalData.get("evalComment");
        boolean isRejected = evalComment != null && evalComment.contains("[반려]");
        evalData.put("isRejected", isRejected);

        String rejectReason = "";
        if (evalComment != null && evalComment.contains("[반려 사유]")) {
            int idx = evalComment.indexOf("[반려 사유]");
            rejectReason = evalComment.substring(idx + "[반려 사유]".length()).trim();
        }

        // 항목별 점수 조회
        Vector<String> itemNames = evalService.getEvaluationItemNames();
        List<BigDecimal> itemScores = evalService.getItemScoresByEvalId(evalId, itemNames);

        // JSP 데이터 바인딩
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
