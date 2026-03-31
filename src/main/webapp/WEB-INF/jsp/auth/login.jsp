<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>HR ERP - 로그인</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/login.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
</head>
<body>

    <div class="login-card" style="margin-top: 100px; width: 420px; margin-left: auto; margin-right: auto; background: #fff; padding: 40px; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.08);">
        <div class="logo-area" style="text-align: center; margin-bottom: 30px;">
            <i class="fa-solid fa-hotel" style="font-size: 40px; color: #3498db; margin-bottom: 15px;"></i>
            <h2 style="margin: 0; color: #2c3e50;">HR ERP</h2>
        </div>

        <form action="${pageContext.request.contextPath}/auth/login.do" method="post">
            <div class="input-group" style="margin-bottom: 20px;">
                <label class="input-label" style="display: block; margin-bottom: 8px; font-weight: bold; font-size: 14px; color: #4a5568;">아이디</label>
                <input type="text" name="username" class="input-box" 
                       value="${param.prevUser}" 
                       style="width: 100%; padding: 12px; border: 1px solid #e2e8f0; border-radius: 6px; box-sizing: border-box;" required>
            </div>

            <div class="input-group" style="margin-bottom: 25px;">
                <label class="input-label" style="display: block; margin-bottom: 8px; font-weight: bold; font-size: 14px; color: #4a5568;">비밀번호</label>
                <input type="password" name="password" class="input-box" 
                       style="width: 100%; padding: 12px; border: 1px solid #e2e8f0; border-radius: 6px; box-sizing: border-box;" required>
            </div>

            <div class="alert-container">
                <c:choose>
                    <%-- [수정] 줄 끊김 방지 디자인: 타이틀 아래로 문구 배치 --%>
                    <c:when test="${param.msg eq 'account_locked'}">
                        <div class="alert-box alert-danger" style="background:#fff5f5; border:1px solid #feb2b2; padding:15px; border-radius:8px; margin-bottom:20px; border-left: 5px solid #e53e3e; box-sizing: border-box;">
                            <%-- 상단: 아이콘 + 타이틀 (한 줄 고정) --%>
                            <div style="display: flex; align-items: center; margin-bottom: 6px;">
                                <i class="fa-solid fa-circle-exclamation" style="color:#c53030; margin-right: 8px; font-size: 16px;"></i>
                                <strong style="color:#c53030; font-size:15px;">계정 잠김 안내</strong>
                            </div>
                            <%-- 하단: 설명 문구 (가로 공간 전체 활용) --%>
                            <div style="text-align: left; color:#c53030; font-size:13px; line-height: 1.5; padding-left: 24px; word-break: keep-all;">
                                5회 이상 로그인 실패로 계정이 잠겼습니다.<br>
                                문의: <strong style="text-decoration: underline;">${not empty param.adminPhone ? param.adminPhone : '010-1234-5678'}</strong>
                            </div>
                        </div>
                    </c:when>

                    <%-- 비밀번호 불일치 --%>
                    <c:when test="${not empty param.msg and fn:startsWith(param.msg, 'login_fail_')}">
                        <div class="alert-box alert-warning" style="background:#fffaf0; border:1px solid #fbd38d; padding:12px; border-radius:6px; margin-bottom:20px; display: flex; align-items: center;">
                            <i class="fa-solid fa-triangle-exclamation" style="color:#9c4221; margin-right: 10px;"></i>
                            <p style="color:#9c4221; font-size:13px; margin:0;">
                                비밀번호 불일치 (현재 <strong>${fn:substringAfter(param.msg, 'login_fail_')}</strong> / 5회)
                            </p>
                        </div>
                    </c:when>
                    
                    <c:when test="${param.msg eq 'invalid_user'}">
                        <div class="alert-box alert-danger" style="background:#fff5f5; border:1px solid #feb2b2; padding:12px; border-radius:6px; margin-bottom:20px; display: flex; align-items: center;">
                            <i class="fa-solid fa-user-xmark" style="color:#c53030; margin-right: 10px;"></i>
                            <p style="color:#c53030; font-size:13px; margin:0;">존재하지 않는 아이디입니다.</p>
                        </div>
                    </c:when>

                    <c:otherwise>
                        <div class="alert-box alert-default" style="background:#ebf8ff; border:1px solid #bee3f8; padding:12px; border-radius:6px; margin-bottom:20px;">
                            <p style="color:#2a4365; font-size:13px; margin:0; text-align: center;">5회 연속 실패 시 보안을 위해 계정이 잠깁니다.</p>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>

            <button type="submit" class="login-btn" 
                    style="width: 100%; padding: 14px; background: #3498db; color: white; border: none; border-radius: 6px; font-size: 16px; font-weight: bold; cursor: pointer;">
                로그인
            </button>
        </form>

        <div class="footer-info" style="margin-top:30px; text-align:center; border-top:1px solid #edf2f7; padding-top:20px;">
            <span style="font-size:12px; color:#a0aec0;">
                © 2026 HR Management System
            </span>
        </div>
    </div>
</body>
</html>