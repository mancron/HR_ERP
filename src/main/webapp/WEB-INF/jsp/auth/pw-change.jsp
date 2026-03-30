<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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

    <%-- 에러 메시지 영역: vd.errorMsg가 비어있지 않을 때만 출력 --%>
    <c:if test="${not empty vd.errorMsg}">
        <div class="error-message-box" style="background: #fff5f5; border: 1px solid #feb2b2; padding: 10px; border-radius: 4px; margin-bottom: 20px; color: #c53030; font-size: 14px; text-align: center;">
            ${vd.errorMsg}
        </div>
    </c:if>

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