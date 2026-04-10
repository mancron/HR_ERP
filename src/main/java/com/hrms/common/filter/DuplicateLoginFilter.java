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

        // 1. 제외 경로 체크 (기존 유지)
        if (path.contains("/auth/") || path.contains("/css/") || path.contains("/js/") || 
            path.contains("/images/") || path.contains("/favicon.ico")) {
            chain.doFilter(request, response);
            return;
        }

        // 2. 세션 및 중복 체크 시작 (불필요한 "진입" 로그 삭제)
        if (session != null && session.getAttribute("empId") != null) {
            String empId = String.valueOf(session.getAttribute("empId"));
            String currentSessionId = session.getId();
            Map<String, String> loginUsers = LoginServlet.getLoginUsers();
            
            if (loginUsers != null && loginUsers.containsKey(empId)) {
                String validSessionId = loginUsers.get(empId);

                // 3. 중복 판단 (여기가 핵심!)
                if (validSessionId != null && !currentSessionId.equals(validSessionId)) {
                    // 차단될 때만 빨간색(err)으로 한 줄 강렬하게 남김
                    System.err.println("[DuplicateLogin] 차단됨 - 사번: " + empId + " (새 세션에 의해 밀려남)");
                    
                    session.invalidate(); 
                    
                    res.setContentType("text/html; charset=UTF-8");
                    res.getWriter().println("<script>");
                    res.getWriter().println("alert('다른 환경에서 로그인하여 접속이 종료되었습니다.');");
                    res.getWriter().println("location.href='" + req.getContextPath() + "/auth/login';");
                    res.getWriter().println("</script>");
                    res.getWriter().flush();
                    return; 
                }
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}