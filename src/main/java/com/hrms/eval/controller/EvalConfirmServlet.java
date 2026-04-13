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

        int loginEmpId = (Integer) session.getAttribute("empId");
        String userRole = (String) session.getAttribute("userRole");
        if (userRole == null) userRole = "일반사원";
        
        boolean isHr = "HR담당자".equals(userRole);
        boolean isCEO = "사장님".equals(userRole) || "최종승인자".equals(userRole);

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

        // 1. 사번 추출 및 Null 방어
        Object evalIdObj = evalData.get("evaluatorId");
        if (evalIdObj == null) evalIdObj = evalData.get("EVALUATOR_ID");
        int evaluatorId = (evalIdObj != null) ? Integer.parseInt(String.valueOf(evalIdObj).trim()) : -1;

        Object targetIdObj = evalData.get("empId"); 
        if (targetIdObj == null) targetIdObj = evalData.get("EMP_ID");
        int targetEmpId = (targetIdObj != null) ? Integer.parseInt(String.valueOf(targetIdObj).trim()) : -1;

        // 2. 보안 필터 (피평가자는 상세 보기 버튼이 없으므로, URL 직접 접근 시에도 차단)
        boolean isAuthorized = isHr || isCEO || (loginEmpId == evaluatorId);

        if (!isAuthorized) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "상세 정보 조회 권한이 없습니다.");
            return;
        }

        // 3. 익명화 처리 (혹시 모를 예외 상황 대비 유지)
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
        String grade = (String) evalData.get("grade");
        request.setAttribute("evalData",     evalData);
        request.setAttribute("itemNames",    itemNames);
        request.setAttribute("itemScores",   itemScores);
        request.setAttribute("gradeColor",   evalService.getGradeColor(grade != null ? grade : ""));
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

            // [추가] 중복 처리 방지 로직
            Map<String, Object> currentData = evalService.getEvaluationById(evalId);
            if (currentData != null && "최종확정".equals(currentData.get("status"))) {
                sendJson(response, false, "이미 최종확정된 평가입니다.");
                return;
            }

            if ("confirm".equals(action)) {
                boolean ok = evalService.confirmEvaluation(evalId, userRole, loginEmpId);
                sendJson(response, ok, ok ? "success" : "처리 중 오류가 발생했습니다.");

            } else if ("reject".equals(action)) {
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
        // 이스케이프 보완 (줄바꿈 및 특수문자 대응)
        String safeMsg = (msg != null) ? msg.replace("\\", "\\\\")
                                            .replace("\"", "\\\"")
                                            .replace("\n", "\\n")
                                            .replace("\r", "") : "";
        response.getWriter().write("{\"ok\":" + ok + ",\"msg\":\"" + safeMsg + "\"}");
    }
}