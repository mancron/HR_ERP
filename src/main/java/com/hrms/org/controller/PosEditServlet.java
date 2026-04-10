package com.hrms.org.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import com.hrms.org.dto.PosDTO;
import com.hrms.org.service.PosService;

@WebServlet("/org/position/edit")
public class PosEditServlet extends HttpServlet {
    private PosService posService = new PosService();

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        
        // [수정] 권한 체크: HR담당자가 아니면 폼 진입 자체를 차단 (일반 사용자/관리자 모두 포함)
        String role = (session != null) ? (String) session.getAttribute("userRole") : null;
        if (!"HR담당자".equals(role)) {
            // 403 에러를 던져서 JSP에서 작성한 '접근 권한 없음' 메시지가 나오도록 유도하거나 바로 차단
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "HR담당자 전용 메뉴입니다.");
            return;
        }

        String idParam = request.getParameter("id");
        if (idParam == null || !idParam.matches("\\d+")) {
            response.sendError(400, "유효하지 않은 직급 ID입니다.");
            return;
        }

        PosDTO pos = posService.getPositionDetail(Integer.parseInt(idParam));
        if (pos == null) {
            response.sendError(404, "존재하지 않는 직급 정보입니다.");
            return;
        }

        String csrfToken = UUID.randomUUID().toString();
        session.setAttribute("csrfToken", csrfToken);

        request.setAttribute("pos", pos);
        request.getRequestDispatcher("/WEB-INF/jsp/org/pos_edit_form.jsp").forward(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        
        // 1. 로그인 확인
        if (session == null || session.getAttribute("empId") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        // 2. 권한 확인 [수정]: '관리자' 권한을 조건에서 삭제 (오직 HR담당자만 통과)
        String role = (String) session.getAttribute("userRole");
        if (!"HR담당자".equals(role)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "수정 권한이 없습니다.");
            return;
        }

        // 3. CSRF 검증
        String sessionToken = (String) session.getAttribute("csrfToken");
        String requestToken = request.getParameter("csrfToken");

        if (sessionToken == null || !sessionToken.equals(requestToken)) {
            response.sendError(400, "유효하지 않은 보안 토큰입니다.");
            return;
        }
        session.removeAttribute("csrfToken");

        // 4. 파라미터 수집 및 비즈니스 로직 실행 (기존과 동일)
        String posIdStr = request.getParameter("position_id");
        String baseSalaryStr = request.getParameter("base_salary");
        String isActiveStr = request.getParameter("is_active");
        
        if (posIdStr == null || baseSalaryStr == null || isActiveStr == null || baseSalaryStr.trim().isEmpty()) {
            response.sendRedirect("edit?id=" + posIdStr + "&status=error");
            return;
        }

        try {
            PosDTO newDto = new PosDTO();
            newDto.setPosition_id(Integer.parseInt(posIdStr));
            newDto.setBase_salary(new BigDecimal(baseSalaryStr));
            newDto.setMeal_allowance(Integer.parseInt(request.getParameter("meal_allowance")));
            newDto.setTransport_allowance(Integer.parseInt(request.getParameter("transport_allowance")));
            newDto.setPosition_allowance(Integer.parseInt(request.getParameter("position_allowance")));
            newDto.setIs_active(Integer.parseInt(isActiveStr));

            Integer actorId = (Integer) session.getAttribute("empId");

            String status = posService.updatePositionProcess(newDto, actorId);

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