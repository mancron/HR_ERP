package com.hrms.common.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;

/**
 * 모든 보호 페이지에 브라우저 캐시 방지 헤더를 추가하는 필터.
 * 로그아웃 후 뒤로가기 시 캐시된 페이지가 노출되는 것을 차단한다.
 */
@WebFilter("/*")
public class NoCacheFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest  req  = (HttpServletRequest)  request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String uri = req.getRequestURI();

        // 정적 리소스(css, js, 이미지)는 캐시 허용
        if (!isStaticResource(uri)) {
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            resp.setHeader("Pragma",        "no-cache");   // HTTP/1.0 하위 호환
            resp.setDateHeader("Expires",   0);            // 즉시 만료
        }

        chain.doFilter(request, response);
    }

    private boolean isStaticResource(String uri) {
        return uri.endsWith(".css")
            || uri.endsWith(".js")
            || uri.endsWith(".png")
            || uri.endsWith(".jpg")
            || uri.endsWith(".ico")
            || uri.endsWith(".woff2");
    }

    @Override public void init(FilterConfig fc) {}
    @Override public void destroy() {}
}