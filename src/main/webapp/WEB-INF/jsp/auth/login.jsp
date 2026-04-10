<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>HR ERP - 로그인</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/login.css">
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
<script>
    // 네비게이션 상태 초기화
    sessionStorage.removeItem('openNavGroups');
    
    // iframe 탈출 로직 (세션 만료 시 로그인 창이 iframe 내부에 뜨는 것 방지)
    if (self !== top) {
        top.location.href = self.location.href;
    }
</script>
</head>
<body>

    <div class="login-card">
        <div class="logo-area">
            <i class="fa-solid fa-hotel"></i>
            <h2>HR ERP</h2>
            <p>인사관리 시스템</p>
        </div>

        <form action="${pageContext.request.contextPath}/auth/login.do" method="post">
            <div class="input-group">
                <label class="input-label">아이디</label>
                <input type="text" name="username" class="input-box" value="${sessionScope.prevUser}" required>
            </div>

            <div class="input-group">
                <label class="input-label">비밀번호</label>
                <input type="password" name="password" class="input-box" required>
            </div>

            <div class="alert-container">
                <c:set var="errorMsg" value="${sessionScope.loginErrorMsg}" />
                
                <c:choose>
                    <%-- 0. 세션 만료 안내 --%>
                    <c:when test="${param.msg eq 'session_expired'}">
                        <div class="alert-box alert-default">
                            <div class="alert-side">
                                <i class="fa-solid fa-clock-rotate-left"></i>
                                <span class="side-label">만료</span>
                            </div>
                            <div class="alert-content">
                                <strong>세션이 만료되었습니다.</strong><br>
                                보안을 위해 다시 로그인해 주세요.
                            </div>
                        </div>
                    </c:when>

                    <%-- 1. 중복 로그인 --%>
                    <c:when test="${errorMsg eq 'already_logged_in'}">
                        <div class="alert-box alert-danger">
                            <div class="alert-side">
                                <i class="fa-solid fa-user-lock"></i>
                                <span class="side-label">거절</span>
                            </div>
                            <div class="alert-content">
                                <strong>이미 다른 곳에서 로그인 중입니다.</strong><br>
                                기존 접속을 종료한 후 다시 시도해주세요.
                            </div>
                        </div>
                    </c:when>

                    <%-- 2. 퇴사자 접속 차단 --%>
                    <c:when test="${errorMsg eq 'retired_user'}">
                        <div class="alert-box alert-danger">
                            <div class="alert-side">
                                <i class="fa-solid fa-user-slash"></i>
                                <span class="side-label">제한</span>
                            </div>
                            <div class="alert-content">
                                <strong>퇴사 처리된 계정입니다.</strong><br>
                                인사팀에 문의하시기 바랍니다.
                            </div>
                        </div>
                    </c:when>

                    <%-- 3. 계정 없음 --%>
                    <c:when test="${errorMsg eq 'invalid_user'}">
                        <div class="alert-box alert-warning">
                            <div class="alert-side">
                                <i class="fa-solid fa-user-xmark"></i>
                                <span class="side-label">미등록</span>
                            </div>
                            <div class="alert-content">
                                <strong>존재하지 않는 사번이나 아이디입니다.</strong>
                            </div>
                        </div>
                    </c:when>

                    <%-- 4. 계정 잠금 (5회 실패) --%>
                    <c:when test="${errorMsg eq 'account_locked' or fn:substringAfter(errorMsg, 'login_fail_') eq '5'}">
                        <div class="alert-box alert-danger">
                            <div class="alert-side">
                                <i class="fa-solid fa-lock"></i>
                                <span class="side-label">잠금</span>
                            </div>
                            <div class="alert-content">
                                <strong>비밀번호 5회 오류로 계정이 잠겼습니다.</strong><br>
                                관리자(<span class="phone-number">${sessionScope.adminPhone}</span>)에게 문의하세요.
                            </div>
                        </div>
                    </c:when>

                    <%-- 5. 일반 실패 --%>
                    <c:when test="${not empty errorMsg and fn:startsWith(errorMsg, 'login_fail_')}">
                        <div class="alert-box alert-warning">
                            <div class="alert-side">
                                <i class="fa-solid fa-triangle-exclamation"></i>
                                <span class="side-label">안내</span>
                            </div>
                            <div class="alert-content">
                                비밀번호 불일치 (현재 <strong>${fn:substringAfter(errorMsg, 'login_fail_')}</strong>회 실패)
                            </div>
                        </div>
                    </c:when>
                    
                    <%-- 기본 안내 --%>
                    <c:otherwise>
                        <div class="alert-box alert-default">
                            <div class="alert-side">
                                <i class="fa-solid fa-bell"></i>
                                <span class="side-label">안내</span>
                            </div>
                            <div class="alert-content">
                                5회 연속 실패 시 보안을 위해 계정이 잠깁니다.
                            </div>
                        </div>
                    </c:otherwise>
                </c:choose>
                
                <%-- 메시지 확인 후 세션 데이터 제거 --%>
                <c:remove var="loginErrorMsg" scope="session" />
                <c:remove var="prevUser" scope="session" />
            </div>
            
            <button type="submit" class="login-btn">로그인</button>
        </form>

        <div class="footer-info">
            <span>© 2026 HR Management System</span>
        </div>
    </div>
    
    <script>
        window.onload = function() {
            const urlParams = new URLSearchParams(window.location.search);
            
            // 1. 중복 로그인 알림
            const errorMsg = "${sessionScope.loginErrorMsg}";
            if (errorMsg === 'already_logged_in') {
                alert("이미 다른 브라우저나 기기에서 로그인 중입니다.");
            }

            // 2. 세션 만료 알림
            if (urlParams.get('msg') === 'session_expired') {
                alert("세션 시간이 만료되어 자동으로 로그아웃되었습니다.");
            }

            // 3. URL 파라미터 정리 (새로고침 시 중복 알림 방지)
            if (window.location.search) {
                window.history.replaceState({}, document.title, window.location.pathname);
            }
        };
    </script>
</body>
</html>