package com.hrms.sys.controller;

import com.hrms.sys.dto.PasswordResetDTO;
import com.hrms.sys.service.PasswordResetService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebServlet("/sys/passwordReset")
public class PasswordResetServlet extends HttpServlet {

    private PasswordResetService passwordResetService;

    @Override
    public void init() throws ServletException {
        this.passwordResetService = new PasswordResetService();
    }

    // ─────────────────────────────────────────────
    // GET: 직원 검색 / 직원 선택
    // ─────────────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 세션 먼저 꺼내기 (false = 없으면 새로 만들지 않음)
        HttpSession session = request.getSession(false);

        // PRG 이후 1회성 임시 비밀번호 처리
        if ("true".equals(request.getParameter("resetDone")) && session != null) {
            String tempPw = (String) session.getAttribute("tempPassword");
            if (tempPw != null) {
                request.setAttribute("tempPassword", tempPw);
                session.removeAttribute("tempPassword"); // 세션에서 즉시 제거 (1회 표시 보장)
                session.removeAttribute("resetEmpId");
            }
        }

        String keyword    = request.getParameter("keyword");
        String selectedId = request.getParameter("selectedEmpId");

        // 1. 검색어가 있으면 검색 실행
        if (keyword != null && !keyword.trim().isEmpty()) {
            List<PasswordResetDTO> results = passwordResetService.searchEmployees(keyword.trim());
            request.setAttribute("searchResults", results);
            request.setAttribute("searchCount",   results.size());
        }

        // 2. 선택된 직원이 있으면 카드 렌더링용 DTO 조회
        if (selectedId != null && !selectedId.trim().isEmpty()) {
            try {
                int empId = Integer.parseInt(selectedId.trim());
                PasswordResetDTO selected = passwordResetService.findEmployeeById(empId);
                if (selected != null) {
                    request.setAttribute("selectedEmployee", selected);
                } else {
                    request.setAttribute("errorMsg", "선택한 직원 정보를 찾을 수 없습니다.");
                }
            } catch (NumberFormatException e) {
                request.setAttribute("errorMsg", "잘못된 요청입니다.");
            }
        }

        request.getRequestDispatcher("/WEB-INF/jsp/sys/sys_password_reset.jsp")
               .forward(request, response);
    }

    // ─────────────────────────────────────────────
    // POST: 비밀번호 초기화 실행
    // ─────────────────────────────────────────────
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ── 1. 권한 이중 검증 (Filter 이후 서블릿에서 한 번 더 확인) ──
        HttpSession session = request.getSession(false);
        if (session == null || !"관리자".equals(session.getAttribute("userRole"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "관리자만 접근할 수 있습니다.");
            return;
        }

        // ── 2. 파라미터 수신 ──
        String empIdParam = request.getParameter("empId");
        String keyword    = request.getParameter("keyword"); // 검색어 유지용 (리다이렉트에 사용)

        if (empIdParam == null || empIdParam.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/sys/passwordReset?error=no_target");
            return;
        }

        int empId;
        try {
            empId = Integer.parseInt(empIdParam.trim());
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/sys/passwordReset?error=invalid_id");
            return;
        }

        // ── 3. 작업자 emp_id 세션에서 추출 ──
        // 세션에 empId가 저장되어 있다면 그 값 사용, 없으면 userName으로 조회는 Service에서 처리
        Integer actorEmpId = (Integer) session.getAttribute("empId"); // 로그인 시 저장했다면 사용

        // ── 4. 비밀번호 초기화 서비스 호출 (트랜잭션: UPDATE + audit_log INSERT) ──
        String tempPassword = passwordResetService.resetPassword(empId, actorEmpId);

        // ── 5. PRG 패턴: 처리 완료 후 GET으로 리다이렉트 (새로고침 중복 방지) ──
        //    임시 비밀번호는 세션에 1회 저장 후 GET에서 꺼내 표시 → 새로고침 시 사라짐
        session.setAttribute("tempPassword", tempPassword);
        session.setAttribute("resetEmpId",   empId);

        // keyword가 있으면 검색 상태 유지
        String redirectUrl = request.getContextPath() + "/sys/passwordReset?resetDone=true";
        if (keyword != null && !keyword.trim().isEmpty()) {
            redirectUrl += "&keyword=" + java.net.URLEncoder.encode(keyword.trim(), "UTF-8");
        }
        response.sendRedirect(redirectUrl);
    }
}