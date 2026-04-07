package com.hrms.common.filter;

import com.hrms.auth.controller.LoginServlet;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;

@WebFilter("/*")
public class DuplicateLoginFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("[Filter] 중복 로그인 방지 필터 초기화 완료!");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);
        String path = req.getRequestURI();

        // [로그 1] 모든 요청에 대해 일단 경로를 찍어봅니다.
        System.out.println("[Filter] 진입 요청 경로: " + path);

        // 1. 제외 경로 체크
        if (path.contains("/auth/") || path.contains("/css/") || path.contains("/js/") || 
            path.contains("/images/") || path.contains("/favicon.ico")) {
            System.out.println("[Filter] 제외 경로 통과: " + path);
            chain.doFilter(request, response);
            return;
        }

        // 2. 세션 체크
        if (session != null && session.getAttribute("empId") != null) {
            String empId = String.valueOf(session.getAttribute("empId"));
            String currentSessionId = session.getId();
            
            Map<String, String> loginUsers = LoginServlet.getLoginUsers();
            
            // [로그 2] 로그인된 사용자 발견 시 정보 출력
            System.out.println("[Filter] 검사 대상자 - 사번: " + empId + " | 세션ID: " + currentSessionId);

            if (loginUsers != null && loginUsers.containsKey(empId)) {
                String validSessionId = loginUsers.get(empId);
                System.out.println("[Filter] 맵에 등록된 최신 ID: " + validSessionId);

                // 3. 중복 판단
                if (validSessionId != null && !currentSessionId.equals(validSessionId)) {
                    System.out.println("!!! [Filter] 중복 로그인 감지 - 사번 " + empId + " 차단함 !!!");
                    
                    session.invalidate();
                    
                    res.setContentType("text/html; charset=UTF-8");
                    res.getWriter().println("<script>");
                    res.getWriter().println("alert('다른 환경에서 로그인하여 접속이 종료되었습니다.');");
                    res.getWriter().println("location.href='" + req.getContextPath() + "/auth/login';");
                    res.getWriter().println("</script>");
                    res.getWriter().flush();
                    return; 
                } else {
                    System.out.println("[Filter] 결과: 최신 세션입니다. (정상)");
                }
            } else {
                System.out.println("[Filter] 결과: 맵에 정보가 없습니다.");
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}