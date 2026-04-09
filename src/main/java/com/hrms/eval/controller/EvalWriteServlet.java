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

    /** doGet/doPost 공통 기본 속성 세팅 */
    private void setCommonAttributes(HttpServletRequest request, int loginEmpId, String userRole) {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        List<Integer> yearList = new ArrayList<>();
        for (int i = 0; i < 3; i++) yearList.add(currentYear - i);

        request.setAttribute("itemNames",  evalService.getEvaluationItemNames());
        request.setAttribute("yearList",   yearList);
        request.setAttribute("loginEmpId", loginEmpId);
        request.setAttribute("isHr",       "HR담당자".equals(userRole));
    }

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
        boolean isHr = "HR담당자".equals(userRole);

        setCommonAttributes(request, loginEmpId, userRole);

        String idParam     = request.getParameter("id");
        String currentGrade = "A";

        if (idParam != null && !idParam.isEmpty()) {
            try {
                int evalId = Integer.parseInt(idParam);
                Map<String, Object> evalData = evalDao.getEvaluationById(evalId);

                if (evalData == null || evalData.isEmpty()) {
                    response.sendRedirect(request.getContextPath() + "/eval/status?error=not_found");
                    return;
                }

                String  evalStatus      = (String)  evalData.get("evalStatus");
                Object  ownerObj        = evalData.get("evaluatorId");
                int     ownerEvaluatorId = (ownerObj != null) ? (Integer) ownerObj : -1;
                boolean isOwner         = (ownerEvaluatorId == loginEmpId);

                if ("최종확정".equals(evalStatus)) {
                    response.sendRedirect(request.getContextPath() + "/eval/status?error=already_confirmed");
                    return;
                }
                if (!isOwner && !isHr) {
                    response.sendRedirect(request.getContextPath() + "/eval/status?error=forbidden");
                    return;
                }

                Vector<String>   itemNames  = (Vector<String>) request.getAttribute("itemNames");
                List<BigDecimal> itemScores = evalDao.getItemScoresByEvalId(evalId, itemNames);
                currentGrade = (String) evalData.getOrDefault("grade", "A");

                String  evalComment = (String) evalData.get("evalComment");
                boolean isRejected  = (evalComment != null && evalComment.startsWith("[반려]"));

                request.setAttribute("isRejected", isRejected);
                request.setAttribute("evalData",   evalData);
                request.setAttribute("itemScores", itemScores);

            } catch (NumberFormatException e) { e.printStackTrace(); }
        }

        String evalTypeForList = request.getParameter("evalType");
        if (evalTypeForList == null || evalTypeForList.isEmpty()) evalTypeForList = "상위평가";

        request.setAttribute("selectedEvalType", evalTypeForList);
        request.setAttribute("targetList",
                evalService.getEmployeeListForEvaluator(loginEmpId, userRole, evalTypeForList));
        request.setAttribute("gradeColor", evalService.getGradeColor(currentGrade));

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

        String evalIdStr   = request.getParameter("evalId");
        String empIdStr    = request.getParameter("empId");
        String evalComment = request.getParameter("evalComment");
        String evalType    = request.getParameter("evalType");

        try {
            // ── [보안 검증 추가] 수정 모드일 때 상태값 및 권한 체크 ──
            if (evalIdStr != null && !evalIdStr.isEmpty()) {
                int checkEvalId = Integer.parseInt(evalIdStr);
                Map<String, Object> currentEval = evalDao.getEvaluationById(checkEvalId);

                if (currentEval != null && !currentEval.isEmpty()) {
                    String dbStatus = (String) currentEval.get("evalStatus");
                    Object ownerObj = currentEval.get("evaluatorId");
                    int ownerId = (ownerObj != null) ? (Integer) ownerObj : -1;

                    // 1. 이미 최종확정된 경우 서버에서 수정 원천 차단
                    if ("최종확정".equals(dbStatus)) {
                        throw new Exception("already_confirmed");
                    }

                    // 2. 작성자 본인이 아니고 HR담당자도 아닌 경우 수정 거부
                    if (ownerId != loginEmpId && !"HR담당자".equals(userRole)) {
                        throw new Exception("forbidden");
                    }
                }
            }

            // 필수값 검증
            if (empIdStr == null || empIdStr.isEmpty())
                throw new Exception("target_required");
            if (evalComment == null || evalComment.trim().isEmpty())
                throw new Exception("comment_required");

            EvaluationDTO eval = new EvaluationDTO();
            if (evalIdStr != null && !evalIdStr.isEmpty())
                eval.setEvalId(Integer.parseInt(evalIdStr));

            int targetEmpId = Integer.parseInt(empIdStr);
            eval.setEmpId(targetEmpId);
            eval.setEvalYear(Integer.parseInt(request.getParameter("evalYear")));
            eval.setEvalPeriod(request.getParameter("evalPeriod"));
            eval.setEvalType(evalType);
            eval.setEvalComment(evalComment);
            eval.setEvaluatorId(loginEmpId);
            eval.setEvalStatus("작성중");

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

            // [NEW-8 수정] 중복 처리 검증
            boolean isNewWrite = (eval.getEvalId() == 0);
            if (isNewWrite) {
                // 본인 작성 중복 체크
                Map<String, Object> myExisting = evalService.loadExistingEval(
                        targetEmpId, eval.getEvalYear(), eval.getEvalPeriod(),
                        eval.getEvalType(), loginEmpId);
                if (myExisting != null)
                    throw new Exception("duplicate");

                // [M-3 수정] 타인이 같은 조건으로 이미 작성중인 건이 있으면 차단
                if (isOccupiedByOther(targetEmpId, eval.getEvalYear(),
                        eval.getEvalPeriod(), eval.getEvalType(), loginEmpId)) {
                    throw new Exception("occupied_by_other");
                }

                if (evalService.isAlreadyConfirmed(targetEmpId, eval.getEvalYear(),
                        eval.getEvalPeriod(), eval.getEvalType()))
                    throw new Exception("already_confirmed_other");
            }

            evalService.submitEvaluation(eval, itemList);
            response.sendRedirect(request.getContextPath() + "/eval/status");

        } catch (Exception e) {
            e.printStackTrace();
            setCommonAttributes(request, loginEmpId, userRole);
            request.setAttribute("errorCode",  e.getMessage());
            request.setAttribute("tempComment", evalComment);

            if (evalIdStr != null && !evalIdStr.isEmpty()) {
                try {
                    int evalId = Integer.parseInt(evalIdStr);
                    request.setAttribute("evalData",
                            evalDao.getEvaluationById(evalId));
                    request.setAttribute("itemScores",
                            evalDao.getItemScoresByEvalId(evalId, evalService.getEvaluationItemNames()));
                } catch (Exception ex) { ex.printStackTrace(); }
            }

            request.setAttribute("selectedEvalType", evalType);
            request.setAttribute("targetList",
                    evalService.getEmployeeListForEvaluator(loginEmpId, userRole, evalType));
            request.setAttribute("gradeColor", evalService.getGradeColor("A"));

            request.setAttribute("viewPage", "/WEB-INF/jsp/eval/write.jsp");
            request.getRequestDispatcher("/index.jsp").forward(request, response);
        }
    }

    /**
     * [M-3] 타인이 동일 조건으로 이미 작성중인 평가가 있는지 확인
     * ON DUPLICATE KEY UPDATE가 타인 건을 evaluator_id만 바꿔 덮어쓰는 문제 방지
     */
    private boolean isOccupiedByOther(int empId, int year, String period,
                                       String type, int myEmpId) {
        String sql = "SELECT evaluator_id FROM evaluation "
                + "WHERE emp_id=? AND eval_year=? AND eval_period=? AND eval_type=? "
                + "AND eval_status='작성중'";
        try (java.sql.Connection conn = com.hrms.common.db.DatabaseConnection.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, empId); pstmt.setInt(2, year);
            pstmt.setString(3, period); pstmt.setString(4, type);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int existingEvaluatorId = rs.getInt("evaluator_id");
                    return existingEvaluatorId != myEmpId; // 본인이 아닌 타인이 작성중
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
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
                String empName = escapeForJson(String.valueOf(m.get("empName")));
                String pos     = escapeForJson(String.valueOf(m.get("pos")));
                if (i > 0) json.append(",");
                json.append("{\"empId\":").append(m.get("empId"))
                    .append(",\"empName\":\"").append(empName).append("\"")
                    .append(",\"pos\":\"").append(pos).append("\"}");
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
                String msg = isConfirmed
                        ? "이미 최종확정된 평가입니다. 불러올 수 없습니다."
                        : "해당 조건의 기존 평가가 없습니다.";
                response.getWriter().write("{\"found\":false,\"msg\":\"" + escapeForJson(msg) + "\"}");
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

    /** JSON 응답용 이스케이프 (XSS + JSON 파싱 오류 방지) */
    private String escapeForJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("<",  "&lt;")
                    .replace(">",  "&gt;")
                    .replace("\n", " ")
                    .replace("\r", "");
    }
}