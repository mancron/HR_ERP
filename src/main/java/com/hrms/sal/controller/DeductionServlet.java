package com.hrms.sal.controller;

import com.hrms.sal.dto.DeductionRateDTO;
import com.hrms.sal.service.DeductionRateService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@WebServlet("/sal/deduction")
public class DeductionServlet extends HttpServlet {

    private DeductionRateService service;

    @Override
    public void init() throws ServletException {
        this.service = new DeductionRateService();
    }

    // ── GET: 목록 조회 + 수정 폼 --%>
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // PRG 후 1회성 메시지 처리
        HttpSession session = request.getSession(false);
        if (session != null) {
            String successMsg = (String) session.getAttribute("successMsg");
            String errorMsg   = (String) session.getAttribute("errorMsg");
            if (successMsg != null) {
                request.setAttribute("successMsg", successMsg);
                session.removeAttribute("successMsg");
            }
            if (errorMsg != null) {
                request.setAttribute("errorMsg", errorMsg);
                session.removeAttribute("errorMsg");
            }
        }

        // 수정 모드: rateId 파라미터가 있으면 해당 데이터 조회
        String rateIdParam = request.getParameter("edit");
        if (rateIdParam != null && !rateIdParam.trim().isEmpty()) {
            try {
                int rateId = Integer.parseInt(rateIdParam.trim());
                DeductionRateDTO editTarget = service.getById(rateId);
                request.setAttribute("editTarget", editTarget);
            } catch (NumberFormatException e) {
                // 잘못된 파라미터 무시
            }
        }

        List<DeductionRateDTO> rateList = service.getAll();
        request.setAttribute("rateList", rateList);

        request.getRequestDispatcher("/WEB-INF/jsp/sal/sal_deduction.jsp")
               .forward(request, response);
    }

    // ── POST: 추가 / 수정
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        String action = request.getParameter("action");

        try {
        	BigDecimal nationalPension = new BigDecimal(request.getParameter("nationalPensionRate").trim()).divide(BigDecimal.valueOf(100)); // 4.500 → 0.04500
            BigDecimal healthInsurance    = new BigDecimal(request.getParameter("healthInsuranceRate").trim()).divide(BigDecimal.valueOf(100));
            BigDecimal longTermCare       = new BigDecimal(request.getParameter("longTermCareRate").trim()).divide(BigDecimal.valueOf(100));
            BigDecimal employmentInsurance = new BigDecimal(request.getParameter("employmentInsuranceRate").trim()).divide(BigDecimal.valueOf(100));

            if ("add".equals(action)) {
                int targetYear = Integer.parseInt(request.getParameter("targetYear").trim());
                service.add(targetYear, nationalPension, healthInsurance,
                            longTermCare, employmentInsurance);
                session.setAttribute("successMsg", targetYear + "년 공제율이 추가되었습니다.");

            } else if ("update".equals(action)) {
                int rateId = Integer.parseInt(request.getParameter("rateId").trim());
                service.update(rateId, nationalPension, healthInsurance,
                               longTermCare, employmentInsurance);
                session.setAttribute("successMsg", "공제율이 수정되었습니다.");
            }

        } catch (NumberFormatException e) {
            session.setAttribute("errorMsg", "입력값이 올바르지 않습니다. 숫자를 입력해주세요.");
        } catch (RuntimeException e) {
            session.setAttribute("errorMsg", e.getMessage());
        }

        // PRG 패턴
        response.sendRedirect(request.getContextPath() + "/sal/deduction");
    }
}