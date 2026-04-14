package com.hrms.common.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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

@WebFilter("/*")
public class AuthenticationFilter implements Filter {

    private static final Set<String> ADMIN_ALLOWED_PREFIXES = new HashSet<>(Arrays.asList(
        "/main",
        "/auth/",
        "/notification",
        "/att/record",
        "/att/leave/req",
        "/att/overtime/req",
        "/att/annual",
        "/sal/slip",
        "/eval/write",
        "/eval/status",
        "/eval/confirm",
        "/emp/list",
        "/emp/detail",
        "/emp/approvalHistory",
        "/emp/history",
        "/emp/leave",
        "/emp/resign",
        "/emp/update",
        "/emp/approval",
        "/sys/",
        "/org/"
    ));

    private static final Set<String> ADMIN_MANAGER_EXTRA_PREFIXES = new HashSet<>(Arrays.asList(
        "/att/leave/approve",
        "/att/overtime/approve",
        "/emp/approval",
        "/emp/approvalHistory"
    ));

    private static final Set<String> CEO_ALLOWED_POST_PREFIXES = new HashSet<>(Arrays.asList(
    	    "/emp/approval",
    	    "/emp/leave",
    	    "/emp/resign",
    	    "/auth/",
    	    "/att/record",
    	    "/sys/sqlQuery",
    	    "/notification"   // 알람 읽기만 추가
    ));
    
 // CEO 전용 접근 금지 경로 (GET 포함 전면 차단)
    private static final Set<String> CEO_BLOCKED_PREFIXES = new HashSet<>(Arrays.asList(
        "/att/leave/req",
        "/att/overtime/req"
    ));

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  req  = (HttpServletRequest)  request;
        HttpServletResponse res  = (HttpServletResponse) response;

        String requestURI  = req.getRequestURI();
        String contextPath = req.getContextPath();
        String path        = requestURI.substring(contextPath.length());
        String method      = req.getMethod();

        if (path.startsWith("/css/")      ||
            path.startsWith("/js/")       ||
            path.startsWith("/images/")   ||
            path.equals("/auth/login")    ||
            path.equals("/auth/login.do") ||
            path.contains("login.jsp")    ||
            path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session    = req.getSession(false);
        boolean     isLoggedIn = (session != null && session.getAttribute("userRole") != null);

        if (!isLoggedIn) {
            res.sendRedirect(contextPath + "/auth/login");
            return;
        }

        String  role          = (String)  session.getAttribute("userRole");
        Boolean isManagerAttr = (Boolean) session.getAttribute("isManager");
        boolean isManager     = (isManagerAttr != null && isManagerAttr);

        if (path.startsWith("/eval/confirm")) {
            // POST(확정/반려)는 HR담당자만 가능
            if ("POST".equalsIgnoreCase(method) && !"HR담당자".equals(role)) {
                sendForbidden(req, res);
                return;
            }
            // GET(조회)은 일단 통과 -> 서블릿에서 본인 사번인지 체크함
            chain.doFilter(request, response);
            return;
        }
        
        // [관리자] 허용 경로 외 전면 차단
        if ("관리자".equals(role)) {
            boolean allowed = ADMIN_ALLOWED_PREFIXES.stream()
                                .anyMatch(p -> path.startsWith(p));
            if (path.startsWith("/att/annual/grant")) {
                allowed = false;
            }
            
            if (!allowed && path.startsWith("/org/") && "GET".equalsIgnoreCase(method)) {
                allowed = true;
            }

            
            if (!allowed && isManager) {
                allowed = ADMIN_MANAGER_EXTRA_PREFIXES.stream()
                            .anyMatch(p -> path.startsWith(p));
            }
            if (!allowed) {
            	sendForbidden(req, res);
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        // [시스템] sqlQuery는 HR담당자·CEO, 나머지는 차단
        if (path.startsWith("/sys/")) {
            if (path.startsWith("/sys/sqlQuery")) {
                if (!"HR담당자".equals(role) && !"최종승인자".equals(role)) {
                	sendForbidden(req, res);
                    return;
                }
            } else {
            	sendForbidden(req, res);
                return;
            }
        }

        // [CEO] 읽기 전용 강제
        if ("최종승인자".equals(role)) {
            
            boolean ceoPagesBlocked = CEO_BLOCKED_PREFIXES.stream()
                                        .anyMatch(p -> path.startsWith(p));
            if (ceoPagesBlocked) {
                sendForbidden(req, res);
                return;
            }

            // 기존 POST 차단 로직
            if ("POST".equalsIgnoreCase(method) || 
                "PUT".equalsIgnoreCase(method)  || 
                "DELETE".equalsIgnoreCase(method)) {
                boolean ceoPostAllowed = CEO_ALLOWED_POST_PREFIXES.stream()
                                            .anyMatch(p -> path.startsWith(p));
                if (!ceoPostAllowed) {
                    sendForbidden(req, res);
                    return;
                }
            }
        }

        // [조직 관리] GET은 전 직원, 쓰기는 HR담당자 전용
        if (path.startsWith("/org/")) {
            if (!"GET".equalsIgnoreCase(method) && !"HR담당자".equals(role)) {
                sendForbidden(req, res);
                return;
            }
        }

        // [직원 관리] 직원 등록 — HR담당자 전용 (접근 자체 차단)
        if (path.startsWith("/emp/reg")) {
            if (!"HR담당자".equals(role)) {
                sendForbidden(req, res);
                return;
            }
        }

//        // emp/reg 차단 로직 아래에 삽입)
//        if (path.startsWith("/emp/approval")) {
//            if (!"HR담당자".equals(role) && !isManager && !"최종승인자".equals(role)) {
//                sendForbidden(req, res);
//                return;
//            }
//        }

        if (path.startsWith("/att/leave/approve")) {
            if (!"HR담당자".equals(role) && !isManager && !"최종승인자".equals(role)) {
                sendForbidden(req, res);
                return;
            }
        }

        // [근태 관리] 초과근무 승인 — HR담당자·부서장
        if (path.startsWith("/att/overtime/approve")) {
            if (!"HR담당자".equals(role) && !isManager) {
                sendForbidden(req, res);
                return;
            }
        }

        // [근태 관리] 전사 근태 현황 — HR담당자·CEO
        if (path.startsWith("/att/status")) {
            if (!"HR담당자".equals(role) && !"최종승인자".equals(role)) {
                sendForbidden(req, res);
                return;
            }
        }

        // [근태 관리] 연차 일괄 부여 — HR담당자 전용
        if (path.startsWith("/att/annual/grant")) {
            if (!"HR담당자".equals(role)) {
                sendForbidden(req, res);
                return;
            }
        }
        
        if (path.startsWith("/att/annual") && !path.startsWith("/att/annual/grant")) {
            if ("최종승인자".equals(role)) {
                sendForbidden(req, res);
                return;
            }
        }
        
        // [근태 관리] 연차 현황 — HR담당자 전용
        if (path.startsWith("/att/annual/adjust")) {
            if (!"HR담당자".equals(role)) {
                sendForbidden(req, res);
                return;
            }
        }

        // [급여 관리] 급여 계산·지급 — HR담당자 전용
        if (path.startsWith("/sal/calc")) {
            if (!"HR담당자".equals(role)) {
                sendForbidden(req, res);
                return;
            }
        }

        // [급여 관리] 전사 급여 현황 — HR담당자·CEO
        if (path.startsWith("/sal/status")) {
            if (!"HR담당자".equals(role) && !"최종승인자".equals(role)) {
                sendForbidden(req, res);
                return;
            }
        }

        // [급여 관리] 공제율 관리 — HR담당자 전용
        if (path.startsWith("/sal/deduction")) {
            if (!"HR담당자".equals(role)) {
                sendForbidden(req, res);
                return;
            }
        }

        // [인사 평가] 평가 작성 — CEO는 조회를 위해 GET 요청(페이지 진입)은 허용
        if (path.startsWith("/eval/write")) {
            if ("최종승인자".equals(role)) {
                if ("POST".equalsIgnoreCase(method)) { // 사장님이 '저장' 버튼을 누를 때 호출됨
                    sendForbidden(req, res); // 403 에러로 튕겨냄
                    return;
                }
            }
        }
       
        chain.doFilter(request, response);
    }
    
    private void sendForbidden(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        req.getRequestDispatcher("/WEB-INF/jsp/common/error_403.jsp")
           .forward(req, res);
    }

    @Override
    public void destroy() {}
}