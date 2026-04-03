package com.hrms.common.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

// 모든 URL 요청을 가로채서 검증한다.
@WebFilter("/*")
public class AuthenticationFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String requestURI = req.getRequestURI();
        String contextPath = req.getContextPath();
        
        // Context Path를 제외한 실제 요청 경로 추출
        String path = requestURI.substring(contextPath.length());

        // 1. 검증 예외 경로 (Bypass)
        if (path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/")
                || path.equals("/auth/login")
                || path.equals("/auth/login.do")
                || path.contains("login.jsp")
                || path.startsWith("/api/")) { 

            chain.doFilter(request, response);
            return;
        }

        // 2. 인증 검증 (Authentication)
        HttpSession session = req.getSession(false);
        boolean isLoggedIn = (session != null && session.getAttribute("userRole") != null);

        if (!isLoggedIn) {
            res.sendRedirect(contextPath + "/auth/login"); 
            return;
        }

        // --- 3. 권한 및 상태 추출 ---
        String role = (String) session.getAttribute("userRole");
        // 로그인 시 세션에 Boolean 타입으로 isManager를 넣었다고 가정
        Boolean isManagerAttr = (Boolean) session.getAttribute("isManager");
        boolean isManager = (isManagerAttr != null && isManagerAttr);

        // --- 4. 인가 검증 (Authorization) 로직 ---

        // [시스템] 관리자 전용
        if (path.startsWith("/sys/")) {
            if (!"관리자".equals(role)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "시스템 관리 권한이 필요합니다.");
                return;
            }
        }
        
        // [조직 관리] 부서/직급 관리는 HR 전용
        if (path.startsWith("/org/")) {
            if (!"HR담당자".equals(role)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "조직 관리 권한이 필요합니다.");
                return;
            }
        }

        // [직원 관리] 직원 등록 (HR 전용)
        if (path.startsWith("/emp/reg")) {
            if (!"HR담당자".equals(role)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "직원 등록 권한이 필요합니다.");
                return;
            }
        }

        // [직원 관리] 휴직·복직·퇴직 승인 (HR, 최종승인자, 부서장만)
        // 주의: /emp/approvalHistory (단순 내역 조회)는 통과시키고 실제 결재 화면만 막음
        if (path.startsWith("/emp/approval") && !path.startsWith("/emp/approvalHistory")) {
            if (!"HR담당자".equals(role) && !"최종승인자".equals(role) && !isManager) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "결재 권한이 없습니다.");
                return;
            }
        }

        // [근태 관리] 휴가 승인 (HR, 부서장만)
        if (path.startsWith("/att/leave/approve")) {
            if (!"HR담당자".equals(role) && !isManager) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "휴가 승인 권한이 없습니다.");
                return;
            }
        }

        // [근태 관리] 전사 근태 보정, 연차 부여 (HR 전용)
        if (path.startsWith("/att/status") || path.startsWith("/att/annual/grant")) {
            if (!"HR담당자".equals(role)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "근태/연차 관리 권한이 필요합니다.");
                return;
            }
        }

        // [급여 관리] 계산, 전사 현황, 공제율 관리 (HR 전용)
        if (path.startsWith("/sal/calc") || path.startsWith("/sal/status") || path.startsWith("/sal/deduction")) {
            if (!"HR담당자".equals(role)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "급여 관리 권한이 필요합니다.");
                return;
            }
        }

        // [인사 평가] 실무 평가 작성 (최종승인자 불가)
        if (path.startsWith("/eval/write")) {
            if ("최종승인자".equals(role)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "최종승인자는 실무 평가를 직접 작성하지 않습니다.");
                return;
            }
        }

        // 모든 보안 검증을 통과한 정상 요청만 다음으로 보낸다.
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}