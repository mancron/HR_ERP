<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> <%-- JSTL 사용을 위한 필수 선언 --%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>HR ERP - 로그인</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/login.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
</head>
<body>

    <%-- 1. 상단 시스템 알림 바 (계정 잠김 또는 비밀번호 변경 성공 시) --%>
    <c:if test="${not empty vd.systemNotice}">
        <div class="system-alert-bar" style="background: #E74C3C; color: white; text-align: center; padding: 15px; font-weight: bold; position: fixed; top: 0; left: 0; width: 100%; z-index: 9999;">
            ${vd.systemNotice}
        </div>
        <div style="margin-top: 50px;"></div> <%-- 알림바 두께만큼 여백 --%>
    </c:if>

    <div class="login-card" style="margin-top: 80px;">
        <div class="logo-area">
            <i class="fa-solid fa-hotel" style="font-size: 40px; color: #3498db; margin-bottom: 15px;"></i>
            <h2>HR ERP</h2>
        </div>

        <form action="${pageContext.request.contextPath}/auth/login.do" method="post">
            <div class="input-group">
                <label class="input-label">아이디</label>
                <%-- 2. 이전 입력 아이디 복구: param.prevUser(URL 파라미터) 사용 --%>
                <input type="text" name="username" class="input-box" value="${not empty param.prevUser ? param.prevUser : ''}" required>
            </div>

            <div class="input-group">
                <label class="input-label">비밀번호</label>
                <input type="password" name="password" class="input-box" required>
            </div>

            <div class="alert-container">
                <%-- 3. 메시지에 따른 조건부 렌더링 --%>
                <c:choose>
                    <%-- 비밀번호 실패 시 횟수 표시 --%>
                    <c:when test="${not empty param.msg and param.msg.startsWith('login_fail_')}">
                        <div class="alert-box alert-warning" style="background:#fffaf0; border:1px solid #fbd38d; padding:12px; border-radius:6px; margin-bottom:20px;">
                            <p style="color:#9c4221; font-size:13px; margin:0;">
                                비밀번호 불일치 (현재 <strong>${vd.failCount}</strong> / 5회)
                            </p>
                        </div>
                    </c:when>
                    
                    <%-- 일반 상태일 때 안내 메시지 --%>
                    <c:when test="${empty vd.systemNotice}">
                        <div class="alert-box alert-default" style="background:#ebf8ff; border:1px solid #bee3f8; padding:12px; border-radius:6px; margin-bottom:20px;">
                            <p style="color:#2a4365; font-size:13px; margin:0;">5회 연속 실패 시 계정이 잠깁니다.</p>
                        </div>
                    </c:when>
                </c:choose>
            </div>

            <button type="submit" class="login-btn">로그인</button>
        </form>

        <div class="footer-info" style="margin-top:30px; text-align:center; border-top:1px solid #eee; padding-top:20px;">
            <%-- 4. 관리자 문의처 표시 --%>
            <span style="font-size:14px; color:#777;">문의: ${vd.adminPhone}</span>
        </div>
    </div>
</body>
</html>