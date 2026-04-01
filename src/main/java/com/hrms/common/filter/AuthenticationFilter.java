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

     // 1. [핵심 수정] 검증 예외 경로 (Bypass)
        // 로그인 화면 진입(/auth/login), 실제 로그인 처리(.do), 정적 리소스는 무조건 통과
        if (path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/")
                || path.equals("/auth/login")    // 로그인 페이지 서블릿 주소 추가
                || path.equals("/auth/login.do") // 로그인 처리 서블릿 주소
                || path.contains("login.jsp")) { // JSP 파일 직접 접근(권장하진 않지만 예외처리)
            
            chain.doFilter(request, response);
            return;
        }

        // 2. 인증 검증
        HttpSession session = req.getSession(false);
        boolean isLoggedIn = (session != null && session.getAttribute("userRole") != null);

        if (!isLoggedIn) {
            // [중요] 리다이렉트 경로가 위 예외 경로(1번)에 반드시 포함되어야 함!
            res.sendRedirect(contextPath + "/auth/login"); 
            return;
        }

        // 3. 인가 검증 (/sys/ 관리자 체크)
        if (path.startsWith("/sys/")) {
            String role = (String) session.getAttribute("userRole");
            if (!"관리자".equals(role)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "관리자 권한이 필요합니다.");
                return;
            }
        }
        
        // 	4. 근태/휴가 관리자 기능 권한 체크
        if (path.startsWith("/att/leave/approve") 
                || path.startsWith("/att/status") 
                || path.startsWith("/att/annual/grant")) {

            String role = (String) session.getAttribute("userRole");

            if (!"관리자".equals(role) && !"HR관리자".equals(role)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "해당 기능은 관리자 또는 HR 관리자만 접근 가능합니다.");
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