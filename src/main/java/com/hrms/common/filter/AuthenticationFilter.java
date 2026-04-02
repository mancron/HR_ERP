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

@WebFilter("/*")
public class AuthenticationFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String requestURI = req.getRequestURI();
        String contextPath = req.getContextPath();
        String path = requestURI.substring(contextPath.length());

        // 1. [Bypass] 검증 예외 경로 (로그인 없이 접근 가능)
        if (path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/")
                || path.equals("/auth/login")
                || path.equals("/auth/login.do")
                || path.contains("login.jsp")) {
            
            chain.doFilter(request, response);
            return;
        }

        // 2. [Authentication] 인증 검증 (로그인 여부 확인)
        HttpSession session = req.getSession(false);
        String userRole = (session != null) ? (String) session.getAttribute("userRole") : null;

        if (userRole == null) {
            // 비동기 세션 연장(ping) 요청인 경우 401 에러 반환
            String action = req.getParameter("action");
            if ("ping".equals(action)) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // 일반 요청인데 세션이 없는 경우 -> 로그인 페이지로 이동 (?timeout=y 추가)
            res.sendRedirect(contextPath + "/auth/login?timeout=y");
            return;
        }

        // 3. [Authorization] 인가 검증 (URL 직접 접근 차단)
        // (1) 관리자 전용 경로 (/sys/ 등) 체크
        if (path.startsWith("/sys/")) {
            if (!"관리자".equals(userRole)) {
                // 관리자가 아닌데 주소창에 직접 치고 들어온 경우 -> 메인으로 튕기기
                res.sendRedirect(contextPath + "/main?msg=no_admin_auth");
                return;
            }
        }

        // (2) 중요 액션(수정/삭제 등)에 대한 추가 권한 체크가 필요하다면 여기서 처리
        /*
        String action = req.getParameter("action");
        if ("delete".equals(action) && !"관리자".equals(userRole)) {
             res.sendRedirect(contextPath + "/main?msg=no_permission");
             return;
        }
        */

        // 모든 보안 검증 통과 시 다음 필터나 서블릿으로 전달
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}