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
</head>
<body>

    <div class="login-card"
        style="margin-top: 100px; width: 420px; margin-left: auto; margin-right: auto; background: #fff; padding: 40px; border-radius: 12px; box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);">
        <div class="logo-area" style="text-align: center; margin-bottom: 30px;">
            <i class="fa-solid fa-hotel" style="font-size: 40px; color: #3498db; margin-bottom: 15px;"></i>
            <h2 style="margin: 0; color: #2c3e50;">HR ERP</h2>
        </div>

        <form action="${pageContext.request.contextPath}/auth/login.do" method="post">
            <div class="input-group" style="margin-bottom: 20px;">
                <label class="input-label" style="display: block; margin-bottom: 8px; font-weight: bold; font-size: 14px; color: #4a5568;">아이디</label>
                <input type="text" name="username" class="input-box" value="${param.prevUser}"
                    style="width: 100%; padding: 12px; border: 1px solid #e2e8f0; border-radius: 6px; box-sizing: border-box;" required>
            </div>

            <div class="input-group" style="margin-bottom: 25px;">
                <label class="input-label" style="display: block; margin-bottom: 8px; font-weight: bold; font-size: 14px; color: #4a5568;">비밀번호</label>
                <input type="password" name="password" class="input-box"
                    style="width: 100%; padding: 12px; border: 1px solid #e2e8f0; border-radius: 6px; box-sizing: border-box;" required>
            </div>

            <div class="alert-container">
                <c:choose>
                    <c:when test="${param.msg eq 'session_expired'}">
                        <div class="alert-box alert-danger">
                            <div class="alert-side">
                                <i class="fa-solid fa-circle-exclamation"></i>
                                <span class="side-label">종료</span>
                            </div>
                            <div class="alert-content">
                                <span>세션이 만료되어 자동으로 로그아웃되었습니다.</span>
                            </div>
                        </div>
                    </c:when>

                    <c:when test="${not empty param.msg and fn:startsWith(param.msg, 'login_fail_')}">
                        <div class="alert-box alert-warning">
                            <div class="alert-side">
                                <i class="fa-solid fa-triangle-exclamation"></i>
                                <span class="side-label">안내</span>
                            </div>
                            <div class="alert-content">
                                <span>
                                    비밀번호가 일치하지 않습니다.<br>
                                    (현재 <strong>${fn:substringAfter(param.msg, 'login_fail_')}</strong>회 실패)
                                </span>
                            </div>
                        </div>
                    </c:when>

                    <c:when test="${not empty vd.systemNotice}">
                        <div class="alert-box alert-danger">
                            <div class="alert-side">
                                <i class="fa-solid fa-circle-exclamation"></i>
                                <span class="side-label">안내</span>
                            </div>
                            <div class="alert-content">
                                <span>${vd.systemNotice}</span>
                            </div>
                        </div>
                    </c:when>
                    
                    <c:otherwise>
                        <div class="alert-box alert-default">
                            <div class="alert-side">
                                <i class="fa-solid fa-bell"></i>
                                <span class="side-label">안내</span>
                            </div>
                            <div class="alert-content">
                                <span>5회 연속 실패 시 보안을 위해 계정이 잠깁니다.</span>
                            </div>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
            
            <button type="submit" class="login-btn"
                style="width: 100%; padding: 14px; background: #3498db; color: white; border: none; border-radius: 6px; font-size: 16px; font-weight: bold; cursor: pointer;">
                로그인</button>
        </form>

        <div class="footer-info" style="margin-top: 30px; text-align: center; border-top: 1px solid #edf2f7; padding-top: 20px;">
            <span style="font-size: 12px; color: #a0aec0;"> © 2026 HR Management System </span>
        </div>
    </div>

    <script>
        window.onload = function() {
            const urlParams = new URLSearchParams(window.location.search);
            // [추가] 로그아웃 후 페이지 로드가 완료되면 alert 실행
            if (urlParams.get('msg') === 'session_expired') {
                alert("세션이 만료되어 자동으로 로그아웃되었습니다.");
                
                // 알림 확인 후 URL 깔끔하게 정리
                const newUrl = window.location.pathname;
                window.history.replaceState({}, document.title, newUrl);
            }
        };
    </script>
</body>
</html>