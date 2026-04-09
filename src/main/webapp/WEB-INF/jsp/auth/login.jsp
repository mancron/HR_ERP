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
<script>sessionStorage.removeItem('openNavGroups');</script>
</head>
<body>

    <div class="login-card" style="margin-top: 100px; width: 420px; margin-left: auto; margin-right: auto; background: #fff; padding: 40px; border-radius: 12px; box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);">
        <div class="logo-area" style="text-align: center; margin-bottom: 30px;">
            <i class="fa-solid fa-hotel" style="font-size: 40px; color: #3498db; margin-bottom: 15px;"></i>
            <h2 style="margin: 0; color: #2c3e50;">HR ERP</h2>
        </div>

        <form action="${pageContext.request.contextPath}/auth/login.do" method="post">
            <div class="input-group" style="margin-bottom: 20px;">
                <label class="input-label" style="display: block; margin-bottom: 8px; font-weight: bold; font-size: 14px; color: #4a5568;">아이디</label>
                <input type="text" name="username" class="input-box" value="${sessionScope.prevUser}" style="width: 100%; padding: 12px; border: 1px solid #e2e8f0; border-radius: 6px; box-sizing: border-box;" required>
            </div>

            <div class="input-group" style="margin-bottom: 25px;">
                <label class="input-label" style="display: block; margin-bottom: 8px; font-weight: bold; font-size: 14px; color: #4a5568;">비밀번호</label>
                <input type="password" name="password" class="input-box" style="width: 100%; padding: 12px; border: 1px solid #e2e8f0; border-radius: 6px; box-sizing: border-box;" required>
            </div>

            <div class="alert-container">
                <c:set var="errorMsg" value="${sessionScope.loginErrorMsg}" />
                
                <c:choose>
                    <%-- 0. 세션 만료 안내 (최우선순위) --%>
                    <c:when test="${param.msg eq 'session_expired'}">
                        <div class="alert-box alert-warning" style="border: 1px solid #3498db; background: #f0f9ff;">
                            <div class="alert-side" style="background: transparent; color: #3498db;">
                                <i class="fa-solid fa-clock-rotate-left"></i>
                                <span class="side-label" style="background: transparent;">만료</span>
                            </div>
                            <div class="alert-content" style="border-left: 1px solid #3498db;">
                                <span style="font-weight: bold; color: #2980b9;">세션이 만료되었습니다.</span><br>
                                <span style="font-size: 12px;">보안을 위해 다시 로그인해 주세요.</span>
                            </div>
                        </div>
                    </c:when>

                    <%-- 1. 중복 로그인 --%>
                    <c:when test="${errorMsg eq 'already_logged_in'}">
                        <div class="alert-box alert-danger" style="border: 1px solid #e74c3c; background: #fff5f5;">
                            <div class="alert-side" style="background: transparent; color: #e74c3c;">
                                <i class="fa-solid fa-user-lock"></i>
                                <span class="side-label" style="background: transparent;">거절</span>
                            </div>
                            <div class="alert-content" style="border-left: 1px solid #e74c3c;">
                                <span style="font-weight: bold; color: #c0392b;">이미 다른 곳에서 로그인 중입니다.</span><br>
                                <span style="font-size: 12px;">기존 접속을 종료한 후 다시 시도해주세요.</span>
                            </div>
                        </div>
                    </c:when>

                    <%-- 2. 퇴사자 접속 차단 --%>
                    <c:when test="${errorMsg eq 'retired_user'}">
                        <div class="alert-box alert-danger">
                            <div class="alert-side"><i class="fa-solid fa-user-slash"></i><span class="side-label">제한</span></div>
                            <div class="alert-content">
                                <span style="font-weight: bold;">퇴사 처리된 계정입니다.</span><br>
                                <span style="font-size: 12px;">인사팀에 문의하시기 바랍니다.</span>
                            </div>
                        </div>
                    </c:when>

                    <%-- 3. 계정 없음 --%>
                    <c:when test="${errorMsg eq 'invalid_user'}">
                        <div class="alert-box alert-warning">
                            <div class="alert-side"><i class="fa-solid fa-user-xmark"></i><span class="side-label">미등록</span></div>
                            <div class="alert-content"><span>존재하지 않는 사번이나 아이디입니다.</span></div>
                        </div>
                    </c:when>

                    <%-- 4. 계정 잠금 (5회 실패) --%>
                    <c:when test="${errorMsg eq 'account_locked' or fn:substringAfter(errorMsg, 'login_fail_') eq '5'}">
                        <div class="alert-box alert-danger">
                            <div class="alert-side"><i class="fa-solid fa-lock"></i><span class="side-label">잠금</span></div>
                            <div class="alert-content">
                                <span style="font-weight: bold;">비밀번호 5회 오류로 계정이 잠겼습니다.</span><br>
                                <span style="font-size: 12px;">관리자(${sessionScope.adminPhone})에게 문의하세요.</span>
                            </div>
                        </div>
                    </c:when>

                    <%-- 5. 일반 실패 및 공지사항 --%>
                    <c:when test="${not empty errorMsg and fn:startsWith(errorMsg, 'login_fail_')}">
                        <div class="alert-box alert-warning">
                            <div class="alert-side"><i class="fa-solid fa-triangle-exclamation"></i><span class="side-label">안내</span></div>
                            <div class="alert-content"><span>비밀번호 불일치 (현재 <strong>${fn:substringAfter(errorMsg, 'login_fail_')}</strong>회 실패)</span></div>
                        </div>
                    </c:when>
                    

                    <c:otherwise>
                        <div class="alert-box alert-default">
                            <div class="alert-side"><i class="fa-solid fa-bell"></i><span class="side-label">안내</span></div>
                            <div class="alert-content"><span>5회 연속 실패 시 보안을 위해 계정이 잠깁니다.</span></div>
                        </div>
                    </c:otherwise>
                </c:choose>
                
                <c:remove var="loginErrorMsg" scope="session" />
                <c:remove var="prevUser" scope="session" />
            </div>
            
            <button type="submit" class="login-btn" style="width: 100%; padding: 14px; background: #3498db; color: white; border: none; border-radius: 6px; font-size: 16px; font-weight: bold; cursor: pointer; margin-top: 10px;">로그인</button>
        </form>

        <div class="footer-info" style="margin-top: 30px; text-align: center; border-top: 1px solid #edf2f7; padding-top: 20px;">
            <span style="font-size: 12px; color: #a0aec0;"> © 2026 HR Management System </span>
        </div>
    </div>
    
    <script>
        window.onload = function() {
            // 1. 세션 에러 메시지 (중복 로그인 등)
            const errorMsg = "${sessionScope.loginErrorMsg}";
            if (errorMsg === 'already_logged_in') {
                alert("이미 다른 브라우저나 기기에서 로그인 중입니다.");
            }

            // 2. [추가] URL 파라미터 확인 (세션 만료 알림)
            const urlParams = new URLSearchParams(window.location.search);
            if (urlParams.get('msg') === 'session_expired') {
                alert("세션 시간이 만료되어 자동으로 로그아웃되었습니다.");
            }

            // 3. URL 지우기 (새로고침 시 알림 반복 방지)
            if (window.location.search) {
                window.history.replaceState({}, document.title, window.location.pathname);
            }
        };
    </script>
</body>
</html>