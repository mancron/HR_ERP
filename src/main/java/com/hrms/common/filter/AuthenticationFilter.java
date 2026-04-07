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

    // ──────────────────────────────────────────────────────────────────
    // 관리자가 접근 가능한 "본인 데이터" 경로
    // 관리자도 회사 직원이므로 본인 급여·근태·평가는 허용
    // ──────────────────────────────────────────────────────────────────
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
        "/sys/"          // 시스템 메뉴 전체 (하위에서 세부 경로 통제)
    ));

    // ──────────────────────────────────────────────────────────────────
    // CEO가 POST 요청을 허용받는 예외 경로
    // CEO는 읽기 전용이 원칙이나 최종결재(휴직·복직·퇴직)는 허용
    // ──────────────────────────────────────────────────────────────────
    private static final Set<String> CEO_ALLOWED_POST_PREFIXES = new HashSet<>(Arrays.asList(
        "/emp/approval",
        "/auth/",           // 비밀번호 변경 등 본인 계정 관리
        "/att/record"       // 본인 출퇴근
    ));

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  req = (HttpServletRequest)  request;
        HttpServletResponse res = (HttpServletResponse) response;

        String requestURI = req.getRequestURI();
        String contextPath = req.getContextPath();
        String path   = requestURI.substring(contextPath.length());
        String method = req.getMethod();

        // ── 1. 인증 예외 경로 (정적 자원, 로그인) ───────────────────
        if (path.startsWith("/css/")    ||
            path.startsWith("/js/")     ||
            path.startsWith("/images/") ||
            path.equals("/auth/login")  ||
            path.equals("/auth/login.do") ||
            path.contains("login.jsp")  ||
            path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        // ── 2. 인증 검증 (Authentication) ───────────────────────────
        HttpSession session    = req.getSession(false);
        boolean     isLoggedIn = (session != null && session.getAttribute("userRole") != null);

        if (!isLoggedIn) {
            res.sendRedirect(contextPath + "/auth/login");
            return;
        }

        // ── 3. 세션에서 권한·상태 추출 ──────────────────────────────
        String  role            = (String)  session.getAttribute("userRole");
        Boolean isManagerAttr   = (Boolean) session.getAttribute("isManager");
        boolean isManager       = (isManagerAttr != null && isManagerAttr);

        // ── 4. 인가 검증 (Authorization) ────────────────────────────

        // [관리자] 허용된 본인 데이터 경로 외 전면 차단
        if ("관리자".equals(role)) {
            boolean allowed = false;
            for (String prefix : ADMIN_ALLOWED_PREFIXES) {
                if (path.startsWith(prefix)) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                res.sendRedirect(contextPath + "/main");
                return;
            }
        }

        // [시스템] 관리자 전용 — /sys/sqlQuery는 HR담당자·CEO도 허용
        if (path.startsWith("/sys/")) {
            if (path.startsWith("/sys/sqlQuery")) {
                if (!"관리자".equals(role) && !"HR담당자".equals(role) && !"최종승인자".equals(role)) {
                    res.sendRedirect(contextPath + "/main");
                    return;
                }
            } else {
                // /sys/sqlQuery 외 시스템 메뉴는 관리자 전용
                if (!"관리자".equals(role)) {
                    res.sendRedirect(contextPath + "/main");
                    return;
                }
            }
        }

        // [CEO] 읽기 전용 강제
        // POST·PUT·DELETE 요청은 허용 목록 외 전면 차단
        if ("최종승인자".equals(role)) {
            if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
                boolean ceoPostAllowed = false;
                for (String prefix : CEO_ALLOWED_POST_PREFIXES) {
                    if (path.startsWith(prefix)) {
                        ceoPostAllowed = true;
                        break;
                    }
                }
                if (!ceoPostAllowed) {
                    res.sendError(HttpServletResponse.SC_FORBIDDEN, "최종승인자는 조회 권한만 보유합니다.");
                    return;
                }
            }
        }

        // [조직 관리] HR담당자·CEO만 허용
        if (path.startsWith("/org/")) {
            if (!"HR담당자".equals(role) && !"최종승인자".equals(role)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "조직 관리 권한이 필요합니다.");
                return;
            }
        }

        // [직원 관리] 관리자 차단 (본인 데이터가 없는 메뉴)
        if (path.startsWith("/emp/")) {
            if ("관리자".equals(role)) {
                res.sendRedirect(contextPath + "/main");
                return;
            }
        }

        // [직원 관리] 직원 등록 — HR담당자 전용
        if (path.startsWith("/emp/reg")) {
            if (!"HR담당자".equals(role)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "직원 등록 권한이 필요합니다.");
                return;
            }
        }

        // [직원 관리] 휴직·복직·퇴직 승인 — HR담당자·CEO(최종결재)·부서장
        // /emp/approvalHistory(단순 내역 조회)는 허용
        if (path.startsWith("/emp/approval") && !path.startsWith("/emp/approvalHistory")) {
            if (!"HR담당자".equals(role) && !"최종승인자".equals(role) && !isManager) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "결재 권한이 없습니다.");
                return;
            }
        }

        // [근태 관리] 휴가 승인 — HR담당자·부서장
        if (path.startsWith("/att/leave/approve")) {
            if (!"HR담당자".equals(role) && !isManager) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "휴가 승인 권한이 없습니다.");
                return;
            }
        }

        // [근태 관리] 초과근무 승인 — HR담당자·부서장
        if (path.startsWith("/att/overtime/approve")) {
            if (!"HR담당자".equals(role) && !isManager) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "초과근무 승인 권한이 없습니다.");
                return;
            }
        }

        // [근태 관리] 전사 근태 현황 — HR담당자·CEO
        if (path.startsWith("/att/status")) {
            if (!"HR담당자".equals(role) && !"최종승인자".equals(role)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "근태 현황 조회 권한이 필요합니다.");
                return;
            }
        }

        // [근태 관리] 연차 일괄 부여 — HR담당자 전용
        if (path.startsWith("/att/annual/grant")) {
            if (!"HR담당자".equals(role)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "연차 관리 권한이 필요합니다.");
                return;
            }
        }

        // [급여 관리] 급여 계산·지급 — HR담당자 전용
        if (path.startsWith("/sal/calc")) {
            if (!"HR담당자".equals(role)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "급여 계산 권한이 필요합니다.");
                return;
            }
        }

        // [급여 관리] 전사 급여 현황 — HR담당자·CEO
        if (path.startsWith("/sal/status")) {
            if (!"HR담당자".equals(role) && !"최종승인자".equals(role)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "급여 현황 조회 권한이 필요합니다.");
                return;
            }
        }

        // [급여 관리] 공제율 관리 — HR담당자 전용
        if (path.startsWith("/sal/deduction")) {
            if (!"HR담당자".equals(role)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "공제율 관리 권한이 필요합니다.");
                return;
            }
        }

        // [인사 평가] 평가 작성 — CEO 불가 (본인 평가가 아닌 실무 평가)
        if (path.startsWith("/eval/write")) {
            if ("최종승인자".equals(role)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "최종승인자는 실무 평가를 직접 작성하지 않습니다.");
                return;
            }
        }

        // 모든 검증 통과 → 다음 필터·서블릿으로 전달
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}