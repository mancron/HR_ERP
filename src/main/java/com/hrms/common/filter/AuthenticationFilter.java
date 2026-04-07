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
    // 관리자 허용 경로
    // 관리자도 직원이므로 본인 급여·근태·평가·내역 조회는 허용
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
        "/emp/approvalHistory",  // [추가] 본인 휴직·복직·퇴직 내역 조회
        "/sys/"                  // 시스템 메뉴 전체 (하위에서 세부 경로 통제)
    ));

    // ──────────────────────────────────────────────────────────────────
    // 관리자 + 부서장 겸직 시 추가 허용 경로
    // ──────────────────────────────────────────────────────────────────
    private static final Set<String> ADMIN_MANAGER_EXTRA_PREFIXES = new HashSet<>(Arrays.asList(
        "/att/leave/approve",
        "/att/overtime/approve",
        "/emp/approval",
        "/emp/approvalHistory"
    ));

    // ──────────────────────────────────────────────────────────────────
    // CEO POST 허용 경로
    // CEO는 읽기 전용이 원칙이나 최종결재·본인 계정·출퇴근은 허용
    // ──────────────────────────────────────────────────────────────────
    private static final Set<String> CEO_ALLOWED_POST_PREFIXES = new HashSet<>(Arrays.asList(
        "/emp/approval",
        "/auth/",
        "/att/record"
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

        // ── 1. 인증 예외 경로 ────────────────────────────────────────
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

        // ── 2. 인증 검증 ─────────────────────────────────────────────
        HttpSession session    = req.getSession(false);
        boolean     isLoggedIn = (session != null && session.getAttribute("userRole") != null);

        if (!isLoggedIn) {
            res.sendRedirect(contextPath + "/auth/login");
            return;
        }

        // ── 3. 세션에서 권한 추출 ────────────────────────────────────
        String  role          = (String)  session.getAttribute("userRole");
        Boolean isManagerAttr = (Boolean) session.getAttribute("isManager");
        boolean isManager     = (isManagerAttr != null && isManagerAttr);

        // ── 4. 인가 검증 ─────────────────────────────────────────────

        // [관리자] 허용 경로 외 전면 차단
        // 이 블록에서 허용/차단이 완전히 결정되고 return → 아래 블록 실행 안 됨
        if ("관리자".equals(role)) {
            boolean allowed = ADMIN_ALLOWED_PREFIXES.stream()
                                .anyMatch(p -> path.startsWith(p));

            if (!allowed && isManager) {
                allowed = ADMIN_MANAGER_EXTRA_PREFIXES.stream()
                            .anyMatch(p -> path.startsWith(p));
            }

            if (!allowed) {
                res.sendRedirect(contextPath + "/main");
                return;
            }
            // 허용된 경로 → 아래 개별 블록 건너뜀
            chain.doFilter(request, response);
            return;
        }

        // ── 이하는 관리자에게 실행되지 않음 ─────────────────────────

        // [시스템] 관리자 아닌 사람의 /sys/ 접근 처리
        // /sys/sqlQuery 만 HR담당자·CEO 허용, 나머지는 전부 차단
        if (path.startsWith("/sys/")) {
            if (path.startsWith("/sys/sqlQuery")) {
                if (!"HR담당자".equals(role) && !"최종승인자".equals(role)) {
                    res.sendRedirect(contextPath + "/main");
                    return;
                }
            } else {
                res.sendRedirect(contextPath + "/main");
                return;
            }
        }

        // [CEO] 읽기 전용 강제
        if ("최종승인자".equals(role)) {
            if ("POST".equalsIgnoreCase(method) ||
                "PUT".equalsIgnoreCase(method)  ||
                "DELETE".equalsIgnoreCase(method)) {
                boolean ceoPostAllowed = CEO_ALLOWED_POST_PREFIXES.stream()
                                            .anyMatch(p -> path.startsWith(p));
                if (!ceoPostAllowed) {
                    res.sendError(HttpServletResponse.SC_FORBIDDEN, "최종승인자는 조회 권한만 보유합니다.");
                    return;
                }
            }
        }

        // [조직 관리] GET은 전 직원 허용, 쓰기는 HR담당자 전용
        if (path.startsWith("/org/")) {
            if (!"GET".equalsIgnoreCase(method) && !"HR담당자".equals(role)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "조직 관리 권한이 필요합니다.");
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

        // [직원 관리] 휴직·복직·퇴직 승인 — HR담당자·CEO·부서장
        // /emp/approvalHistory 는 전 역할 허용이므로 제외
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

        // [근태 관리] 전사 근태 현황·보정 — HR담당자·CEO
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

        // [인사 평가] 평가 작성 — CEO 불가
        if (path.startsWith("/eval/write")) {
            if ("최종승인자".equals(role)) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "최종승인자는 실무 평가를 직접 작성하지 않습니다.");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
