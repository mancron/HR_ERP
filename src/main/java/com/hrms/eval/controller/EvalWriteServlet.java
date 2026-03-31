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
    private EvaluationDAO evalDao = new EvaluationDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
        // 1. 공통 데이터 준비
        Vector<Map<String, Object>> targetList = evalService.getEmployeeList();
        Vector<String> itemNames = evalService.getEvaluationItemNames();

        // 2. 연도 드롭다운 (현재 기준 최근 3년)
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        List<Integer> yearList = new ArrayList<>();
        for (int i = 0; i < 3; i++) yearList.add(currentYear - i);

        // 3. [핵심] 스크립틀릿 로직을 서블릿으로 이동
        // 초기 등급 색상 설정 (수정 모드면 해당 등급의 색상, 신규면 기본 A등급 색상)
        String idParam = request.getParameter("id");
        String currentGrade = "A"; // 기본값
        
        if (idParam != null && !idParam.isEmpty()) {
            try {
                int evalId = Integer.parseInt(idParam);
                Map<String, Object> evalData = evalDao.getEvaluationById(evalId);
                List<BigDecimal> itemScores = evalDao.getItemScoresByEvalId(evalId, itemNames);

                if (evalData != null) {
                    request.setAttribute("evalData", evalData);
                    request.setAttribute("itemScores", itemScores);
                    request.setAttribute("isUpdate", true);
                    currentGrade = (String) evalData.get("grade"); // 기존 등급 가져오기
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        
        // 등급 색상을 서비스에서 가져와서 전달
        String gradeColor = evalService.getGradeColor(currentGrade != null ? currentGrade : "A");
        request.setAttribute("gradeColor", gradeColor);

        // 4. 데이터 세팅 및 포워딩
        request.setAttribute("targetList", targetList);
        request.setAttribute("itemNames", itemNames);
        request.setAttribute("yearList", yearList);
        request.setAttribute("viewPage", "/WEB-INF/jsp/eval/write.jsp");
        
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        Integer loginEmpId = (Integer) session.getAttribute("empId");

        if (loginEmpId == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login?error=session_expired");
            return;
        }

        try {
            EvaluationDTO eval = new EvaluationDTO();
            // 수정 모드일 경우 evalId 수집
            String evalIdStr = request.getParameter("evalId");
            if(evalIdStr != null) eval.setEvalId(Integer.parseInt(evalIdStr));
            
            eval.setEmpId(Integer.parseInt(request.getParameter("empId")));
            eval.setEvalYear(Integer.parseInt(request.getParameter("evalYear")));
            eval.setEvalPeriod(request.getParameter("evalPeriod"));
            eval.setEvalType(request.getParameter("evalType"));
            eval.setEvalComment(request.getParameter("evalComment"));
            
            String status = request.getParameter("status"); 
            eval.setEvalStatus(status);

            if ("최종확정".equals(status)) {
                eval.setEvaluatorId(loginEmpId);
            }

            String[] names = request.getParameterValues("itemNames");
            String[] scores = request.getParameterValues("scores");
            Vector<EvaluationItemDTO> itemList = new Vector<>();

            if (names != null && scores != null) {
                for (int i = 0; i < names.length; i++) {
                    EvaluationItemDTO item = new EvaluationItemDTO();
                    item.setItemName(names[i]);
                    item.setScore(new BigDecimal(scores[i]));
                    itemList.add(item);
                }
            }

            boolean isSuccess = evalService.submitEvaluation(eval, itemList);

            if (isSuccess) {
                response.sendRedirect(request.getContextPath() + "/eval/status");
            } else {
                response.sendRedirect(request.getContextPath() + "/eval/write?error=save_fail");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(request.getContextPath() + "/eval/write?error=invalid_input");
        }
    }
}