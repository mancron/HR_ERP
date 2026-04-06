package com.hrms.eval.controller;

import com.hrms.eval.dao.EvaluationDAO;
import com.hrms.eval.dto.EvaluationDTO;
import com.hrms.eval.dto.EvaluationItemDTO;
import com.hrms.eval.service.EvaluationService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@WebServlet("/eval/write")
public class EvalWriteServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private EvaluationService evalService = new EvaluationService();
    private EvaluationDAO     evalDao     = new EvaluationDAO();

    // ── GET ──────────────────────────────────────────────────
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
        // HR담당자만 확정/반려 권한 — 관리자는 일반사용자 취급
        boolean isHr = "HR담당자".equals(userRole);

        Vector<String> itemNames = evalService.getEvaluationItemNames();
        String currentGrade = "A";

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        List<Integer> yearList = new ArrayList<>();
        for (int i = 0; i < 3; i++) yearList.add(currentYear - i);

        // ── 수정 모드 ──
        String idParam = request.getParameter("id");
        if (idParam != null && !idParam.isEmpty()) {
            try {
                int evalId = Integer.parseInt(idParam);
                Map<String, Object> evalData = evalDao.getEvaluationById(evalId);

                if (evalData == null || evalData.isEmpty()) {
                    response.sendRedirect(request.getContextPath() + "/eval/status?error=not_found");
                    return;
                }

                String evalStatus       = (String)  evalData.get("evalStatus");
                Object ownerObj         = evalData.get("evaluatorId");
                int    ownerEvaluatorId = (ownerObj != null) ? (Integer) ownerObj : -1;
                boolean isOwner         = (ownerEvaluatorId == loginEmpId);

                // 최종확정 → 수정 불가
                if ("최종확정".equals(evalStatus)) {
                    response.sendRedirect(request.getContextPath() + "/eval/status?error=already_confirmed");
                    return;
                }

                // 권한 체크: 본인 작성 or HR담당자만
                if (!isOwner && !isHr) {
                    response.sendRedirect(request.getContextPath() + "/eval/status?error=forbidden");
                    return;
                }

                List<BigDecimal> itemScores = evalDao.getItemScoresByEvalId(evalId, itemNames);
                currentGrade = (String) evalData.getOrDefault("grade", "A");

                String evalComment = (String) evalData.get("evalComment");
                boolean isRejected = (evalComment != null && evalComment.startsWith("[반려]"));
                request.setAttribute("isRejected", isRejected);
                request.setAttribute("evalData",   evalData);
                request.setAttribute("itemScores", itemScores);

            } catch (NumberFormatException e) { e.printStackTrace(); }
        }

        // 직급/유형 기반 대상자 목록
        String evalTypeForList = request.getParameter("evalType");
        if (evalTypeForList == null || evalTypeForList.isEmpty()) {
            evalTypeForList = "상위평가";
        }
        Vector<Map<String, Object>> targetList =
                evalService.getEmployeeListForEvaluator(loginEmpId, userRole, evalTypeForList);
        request.setAttribute("selectedEvalType", evalTypeForList);

        request.setAttribute("targetList",  targetList);
        request.setAttribute("itemNames",   itemNames);
        request.setAttribute("yearList",    yearList);
        request.setAttribute("gradeColor",  evalService.getGradeColor(currentGrade));
        request.setAttribute("loginEmpId",  loginEmpId);
        request.setAttribute("isHr",        isHr);

        request.setAttribute("viewPage", "/WEB-INF/jsp/eval/write.jsp");
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }

    // ── POST ─────────────────────────────────────────────────
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("empId") == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login?error=session_expired");
            return;
        }

        int    loginEmpId = (Integer) session.getAttribute("empId");
        String userRole   = (String)  session.getAttribute("userRole");
        if (userRole == null) userRole = "일반사원";
        boolean isHr = "HR담당자".equals(userRole);

        // AJAX 분기
        String ajaxAction = request.getParameter("ajaxAction");
        if ("load".equals(ajaxAction)) {
            handleLoadExisting(request, response, loginEmpId);
            return;
        }
        if ("getTargets".equals(ajaxAction)) {
            handleGetTargets(request, response, loginEmpId, userRole);
            return;
        }

        try {
            EvaluationDTO eval = new EvaluationDTO();
            String evalIdStr = request.getParameter("evalId");
            if (evalIdStr != null && !evalIdStr.isEmpty())
                eval.setEvalId(Integer.parseInt(evalIdStr));

            int targetEmpId = Integer.parseInt(request.getParameter("empId"));
            eval.setEmpId(targetEmpId);
            eval.setEvalYear(Integer.parseInt(request.getParameter("evalYear")));
            eval.setEvalPeriod(request.getParameter("evalPeriod"));
            eval.setEvalType(request.getParameter("evalType"));
            eval.setEvalComment(request.getParameter("evalComment"));
            eval.setEvaluatorId(loginEmpId);
            eval.setEvalStatus("작성중"); // write에서는 항상 '작성중' (DB CHECK 제약 대응)

            // 자기 자신 상위/동료 평가 차단
            if (evalService.isSelfEvalBlocked(targetEmpId, loginEmpId, eval.getEvalType())) {
                response.sendRedirect(request.getContextPath() + "/eval/write?error=self_eval");
                return;
            }

            // 직급 역전 차단 (HR담당자는 제외)
            if (!isHr && evalService.isPositionBlocked(targetEmpId, loginEmpId, eval.getEvalType())) {
                response.sendRedirect(request.getContextPath() + "/eval/write?error=position_denied");
                return;
            }

            // 신규 작성 시 중복 체크
            boolean isNewWrite = (eval.getEvalId() == 0);
            if (isNewWrite) {
                Map<String, Object> existing = evalService.loadExistingEval(
                        targetEmpId, eval.getEvalYear(), eval.getEvalPeriod(),
                        eval.getEvalType(), loginEmpId);
                if (existing != null) {
                    // 이미 작성한 평가가 있음 → alert 후 해당 수정 페이지로 이동
                    int existingId = (Integer) existing.get("evalId");
                    response.sendRedirect(request.getContextPath()
                            + "/eval/write?id=" + existingId + "&alert=duplicate");
                    return;
                }
                if (evalService.isAlreadyConfirmed(targetEmpId, eval.getEvalYear(),
                        eval.getEvalPeriod(), eval.getEvalType())) {
                    // 이미 최종확정된 평가가 있음 → alert 후 status로 이동
                    response.sendRedirect(request.getContextPath()
                            + "/eval/status?alert=already_confirmed");
                    return;
                }
            }

            String[] names  = request.getParameterValues("itemNames");
            String[] scores = request.getParameterValues("scores");
            Vector<EvaluationItemDTO> itemList = new Vector<>();
            if (names != null && scores != null) {
                for (int i = 0; i < names.length; i++) {
                    EvaluationItemDTO item = new EvaluationItemDTO();
                    item.setItemName(names[i]);
                    item.setScore(new BigDecimal(scores[i]));
                    item.setMaxScore(new BigDecimal("100"));
                    itemList.add(item);
                }
            }

            boolean ok = evalService.submitEvaluation(eval, itemList);
            if (ok) response.sendRedirect(request.getContextPath() + "/eval/status");
            else    response.sendRedirect(request.getContextPath() + "/eval/write?error=save_fail");

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/eval/write?error=invalid_input");
        }
    }

    // ── AJAX: 대상자 목록 갱신 ───────────────────────────────
    private void handleGetTargets(HttpServletRequest request, HttpServletResponse response,
                                   int loginEmpId, String userRole) throws IOException {
        response.setContentType("application/json; charset=UTF-8");
        try {
            String evalType = request.getParameter("evalType");
            if (evalType == null || evalType.isEmpty()) evalType = "상위평가";

            Vector<Map<String, Object>> list =
                    evalService.getEmployeeListForEvaluator(loginEmpId, userRole, evalType);
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                Map<String, Object> m = list.get(i);
                if (i > 0) json.append(",");
                json.append("{\"empId\":").append(m.get("empId"))
                    .append(",\"empName\":\"").append(m.get("empName")).append("\"")
                    .append(",\"pos\":\"").append(m.get("pos")).append("\"}");
            }
            json.append("]");
            response.getWriter().write(json.toString());
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().write("[]");
        }
    }

    // ── AJAX: 기존 평가 불러오기 ─────────────────────────────
    private void handleLoadExisting(HttpServletRequest request, HttpServletResponse response,
                                    int loginEmpId) throws IOException {
        response.setContentType("application/json; charset=UTF-8");
        try {
            String empIdStr = request.getParameter("empId");
            String yearStr  = request.getParameter("evalYear");
            String period   = request.getParameter("evalPeriod");
            String type     = request.getParameter("evalType");

            if (empIdStr == null || empIdStr.isEmpty() || yearStr == null || yearStr.isEmpty()
                    || period == null || period.isEmpty() || type == null || type.isEmpty()) {
                response.getWriter().write("{\"found\":false,\"msg\":\"조건을 모두 입력해주세요.\"}");
                return;
            }

            int empId = Integer.parseInt(empIdStr);
            int year  = Integer.parseInt(yearStr);

            Map<String, Object> existing =
                    evalService.loadExistingEval(empId, year, period, type, loginEmpId);

            if (existing != null) {
                int    evalId    = (Integer) existing.get("evalId");
                String comment   = (String)  existing.get("evalComment");
                boolean isRejected = (comment != null && comment.startsWith("[반려]"));
                response.getWriter().write("{\"found\":true,\"evalId\":" + evalId
                        + ",\"isRejected\":" + isRejected + "}");
            } else {
                boolean isConfirmed = isAlreadyConfirmed(empId, year, period, type);
                if (isConfirmed)
                    response.getWriter().write("{\"found\":false,\"msg\":\"이미 최종확정된 평가입니다. 불러올 수 없습니다.\"}");
                else
                    response.getWriter().write("{\"found\":false,\"msg\":\"해당 조건의 기존 평가가 없습니다.\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().write("{\"found\":false,\"msg\":\"서버 오류가 발생했습니다.\"}");
        }
    }

    private boolean isAlreadyConfirmed(int empId, int year, String period, String type) {
        String sql = "SELECT COUNT(*) FROM evaluation "
                + "WHERE emp_id=? AND eval_year=? AND eval_period=? AND eval_type=? AND eval_status='최종확정'";
        try (java.sql.Connection conn = com.hrms.common.db.DatabaseConnection.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, empId); pstmt.setInt(2, year);
            pstmt.setString(3, period); pstmt.setString(4, type);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }
}