<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.hrms.auth.service.AuthService" %>
<%@ page import="java.util.Map" %>
<%
    // 1. 서비스 호출
    AuthService authService = new AuthService();
    
    // 파라미터에서 에러 코드 확인
    String error = request.getParameter("error");
    
    // 2. 서비스로부터 화면에 뿌릴 데이터(에러 메시지 등)를 받아옴
    Map<String, String> vd = authService.getPwChangeViewData(error);
    String errorMsg = vd.get("errorMsg");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>비밀번호 변경</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/pw_change.css">
</head>
<body>

<div class="pw-change-container">
    <h3>비밀번호 변경</h3>

    <%-- [기능 유지] alert() 대신 에러 메시지 표시 영역 --%>
    <% if (!errorMsg.isEmpty()) { %>
        <div class="error-message-box" style="background: #fff5f5; border: 1px solid #feb2b2; padding: 10px; border-radius: 4px; margin-bottom: 20px; color: #c53030; font-size: 14px; text-align: center;">
            <%= errorMsg %>
        </div>
    <% } %>

    <form action="${pageContext.request.contextPath}/auth/pw-change" method="post">
        <div class="form-group">
            <label>현재 비밀번호</label>
            <input type="password" name="currentPw" required>
        </div>
        
        <div class="form-group">
            <label>새 비밀번호</label>
            <input type="password" name="newPw" required>
        </div>
        
        <div class="form-group">
            <label>새 비밀번호 확인</label>
            <input type="password" name="confirmPw" required>
        </div>
        
        <button type="submit" class="submit-btn">
            비밀번호 변경 저장
        </button>
    </form>
</div>

</body>
</html>