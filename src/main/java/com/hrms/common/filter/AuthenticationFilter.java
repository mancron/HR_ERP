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
        // 필터 초기화 시 필요한 로직이 있다면 작성
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
        // 로그인 화면, 로그인 처리 서블릿, 정적 리소스는 무조건 통과시킨다.
        // 주의: 이 경로가 실제 프로젝트와 다르면 무한 리다이렉트에 빠지니 정확히 맞춰라.
        if (path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/")
                || path.equals("/login.jsp") || path.equals("/auth/login.do")) {
            chain.doFilter(request, response);
            return;
        }

        // 2. 인증 검증 (Authentication)
        // false 옵션: 세션이 없으면 새로 생성하지 않고 null을 반환한다.
        HttpSession session = req.getSession(false);
        boolean isLoggedIn = (session != null && session.getAttribute("userRole") != null);

        if (!isLoggedIn) {
            // 미인증 사용자는 로그인 페이지로 강제 이동
        	res.sendRedirect(contextPath + "/auth/login.do");
            return;
        }

        // 3. 인가 검증 (Authorization)
        // 시스템 관리 영역(/sys/) 접근 시 권한을 확인한다.
        if (path.startsWith("/sys/")) {
            String role = (String) session.getAttribute("userRole");
            if (!"관리자".equals(role)) {
                // 관리자가 아닐 경우 403 Forbidden 에러 반환 (해킹/비정상 접근 시도 차단)
                // 필요하다면 res.sendRedirect(contextPath + "/main") 으로 메인으로 튕겨내도 된다.
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "시스템 관리자만 접근할 수 있는 메뉴입니다.");
                return;
            }
        }

        // 모든 보안 검증을 통과한 정상 요청만 다음 필터나 서블릿으로 보낸다.
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // 자원 해제 로직
    }
}