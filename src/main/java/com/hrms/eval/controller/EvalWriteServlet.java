package com.hrms.eval.controller;

import com.hrms.eval.dto.EvaluationDTO;
import com.hrms.eval.dto.EvaluationItemDTO;
import com.hrms.eval.service.EvaluationService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/eval/write")
public class EvalWriteServlet extends HttpServlet {
    private EvaluationService evalService = new EvaluationService();

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 1. DB에서 평가 대상자(직원) 목록 가져오기
        List<EmployeeDTO> targetList = evalService.getEvaluationTargets();
        
        // 2. DB에서 평가 항목(업무성과, 직무역량 등) 가져오기
        List<EvaluationItemDTO> schemaList = evalService.getEvaluationSchema();

        // 3. JSP에서 쓸 수 있게 setAttribute
        request.setAttribute("targetList", targetList);
        request.setAttribute("schemaList", schemaList);
        
        // 4. 기존 레이아웃 규칙대로 포워딩
        request.setAttribute("viewPage", "/WEB-INF/jsp/eval/write.jsp");
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        // 1. 파라미터 수집 (write.jsp의 input name들과 일치해야 함)
        int empId = Integer.parseInt(request.getParameter("empId"));
        int evalYear = Integer.parseInt(request.getParameter("evalYear"));
        String evalPeriod = request.getParameter("evalPeriod");
        String evalType = request.getParameter("evalType");
        String evalStatus = request.getParameter("status"); 
        String evalComment = request.getParameter("evalComment");

        // 2. 항목별 점수 수집 (배열 처리)
        String[] itemNames = request.getParameterValues("itemNames");
        String[] scores = request.getParameterValues("scores");

        List<EvaluationItemDTO> itemList = new ArrayList<>();
        double totalSum = 0;

        if (itemNames != null && scores != null) {
            for (int i = 0; i < itemNames.length; i++) {
                EvaluationItemDTO item = new EvaluationItemDTO();
                item.setItemName(itemNames[i]);
                BigDecimal score = new BigDecimal(scores[i]);
                item.setScore(score);
                itemList.add(item);
                totalSum += score.doubleValue();
            }
        }

        // 3. DTO 조립 및 서비스 호출
        double avg = totalSum / (itemList.isEmpty() ? 1 : itemList.size());
        
        EvaluationDTO eval = new EvaluationDTO();
        eval.setEmpId(empId);
        eval.setEvalYear(evalYear);
        eval.setEvalPeriod(evalPeriod);
        eval.setEvalType(evalType);
        eval.setTotalScore(new BigDecimal(avg));
        eval.setGrade(evalService.calculateGrade(avg)); // 서비스에서 등급 계산
        eval.setEvalStatus(evalStatus);
        eval.setEvalComment(evalComment);

        // DB 저장
        boolean isSuccess = evalService.submitEvaluation(eval, itemList);

        if (isSuccess) {
            // 성공 시 메인 페이지로 리다이렉트
            response.sendRedirect(request.getContextPath() + "/main");
        } else {
            // 실패 시 다시 작성 페이지로 (에러 파라미터 전달 가능)
            response.sendRedirect(request.getContextPath() + "/eval/write?error=fail");
        }
    }
}