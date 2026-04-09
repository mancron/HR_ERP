package com.hrms.org.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import com.hrms.org.dto.PosDTO;
import com.hrms.org.service.PosService; // 서비스 임포트 추가

@WebServlet("/org/position/edit")
public class PosEditServlet extends HttpServlet {
    private PosService posService = new PosService(); // DAO 대신 Service 주입

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String idParam = request.getParameter("id");
        
        if (idParam == null || !idParam.matches("\\d+")) {
            response.sendError(400, "유효하지 않은 직급 ID입니다.");
            return;
        }

        // 서비스에 데이터 조회 요청
        PosDTO pos = posService.getPositionDetail(Integer.parseInt(idParam));
        
        if (pos == null) {
            response.sendError(404, "존재하지 않는 직급 정보입니다.");
            return;
        }

        // [Low 12] CSRF 토큰 생성
        String csrfToken = UUID.randomUUID().toString();
        request.getSession().setAttribute("csrfToken", csrfToken);

        request.setAttribute("pos", pos);
        request.getRequestDispatcher("/WEB-INF/jsp/org/pos_edit_form.jsp").forward(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        
        // 1. 보안 검증 (세션 및 CSRF)
        if (session == null || session.getAttribute("empId") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        String sessionToken = (String) session.getAttribute("csrfToken");
        String requestToken = request.getParameter("csrfToken");

        if (sessionToken == null || !sessionToken.equals(requestToken)) {
            response.sendError(400, "유효하지 않은 보안 토큰입니다.");
            return;
        }
        session.removeAttribute("csrfToken");

        // 2. 권한 확인
        String role = (String) session.getAttribute("userRole");
        if (!"관리자".equals(role) && !"HR담당자".equals(role)) {
            response.sendError(403, "수정 권한이 없습니다.");
            return;
        }

        // 3. 파라미터 수집 및 기본 검증
        String posIdStr = request.getParameter("position_id");
        String baseSalaryStr = request.getParameter("base_salary");
        String isActiveStr = request.getParameter("is_active");
        
        if (posIdStr == null || baseSalaryStr == null || isActiveStr == null || baseSalaryStr.trim().isEmpty()) {
            response.sendRedirect("edit?id=" + posIdStr + "&status=error");
            return;
        }

        try {
            // 4. 서비스에 전달할 DTO 조립
            PosDTO newDto = new PosDTO();
            newDto.setPosition_id(Integer.parseInt(posIdStr));
            newDto.setBase_salary(new BigDecimal(baseSalaryStr));
            newDto.setMeal_allowance(Integer.parseInt(request.getParameter("meal_allowance")));
            newDto.setTransport_allowance(Integer.parseInt(request.getParameter("transport_allowance")));
            newDto.setPosition_allowance(Integer.parseInt(request.getParameter("position_allowance")));
            newDto.setIs_active(Integer.parseInt(isActiveStr));

            Integer actorId = (Integer) session.getAttribute("empId");

            // 5. 비즈니스 로직 실행 요청 (핵심!)
            String status = posService.updatePositionProcess(newDto, actorId);

            // 6. 결과 페이지 리다이렉트
            if ("not_found".equals(status)) {
                response.sendError(404);
            } else {
                response.sendRedirect("edit?id=" + posIdStr + "&status=" + status);
            }

        } catch (NumberFormatException e) {
            response.sendRedirect("edit?id=" + posIdStr + "&status=error");
        }
    }
}