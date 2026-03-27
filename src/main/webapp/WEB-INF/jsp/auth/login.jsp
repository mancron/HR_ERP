<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%-- [테스트] BCrypt 임포트 제거 --%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>HR ERP - 인사관리 통합 시스템</title>
    <%-- 외부 CSS 파일 링크: webapp/css/login.css --%>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/login.css">
    <%-- 아이콘 사용을 위한 FontAwesome CDN (경고 아이콘용) --%>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <%-- 폰트 권장: Noto Sans KR --%>
    <link href="https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@400;500;700&display=swap" rel="stylesheet">
</head>
<script>
window.onload = function() {
    const url = new URL(window.location.href);
    const msg = url.searchParams.get('msg');
    
    if (msg) {
        if (msg === 'logout') {
            alert("로그아웃 되었습니다.");
        } else if (msg === 'pw_success') {
            alert("비밀번호가 성공적으로 변경되었습니다. 다시 로그인해주세요.");
        } else if (msg === 'login_fail') {
            alert("아이디 또는 비밀번호를 확인해주세요.");
        }

        url.searchParams.delete('msg');
        window.history.replaceState({}, document.title, url.pathname);
    }
};
</script>
<body>

    <div class="login-card">
        <div class="logo-area">
            <%-- 로고 이미지 경로 (필요시 수정하세요) --%>
            <%-- <img src="${pageContext.request.contextPath}/images/logo.png" alt="로고 아이콘"> --%>
            <%-- 임시로 아이콘 폰트로 대체 (이미지 파일이 있다면 위 img 코드로 대체) --%>
            <i class="fa-solid fa-hotel" style="font-size: 40px; color: #3498db; margin-bottom: 15px;"></i>
            
            <h2>HR ERP</h2>
            <p>인사관리 통합 시스템</p>
        </div>

        <form action="${pageContext.request.contextPath}/auth/login.do" method="post">
            
            <div class="input-group">
                <label class="input-label">아이디 <span class="required">*</span></label>
                <input type="text" name="username" class="input-box" placeholder="아이디" required>
            </div>

            <div class="input-group">
                <label class="input-label">비밀번호 <span class="required">*</span></label>
                <input type="password" name="password" class="input-box" placeholder="비밀번호" required>
            </div>

            <% if ("login_fail".equals(request.getParameter("error"))) { %>
                <div class="alert-box" style="border: 1px solid #E74C3C; background-color: #FDEDEC;">
                    <i class="fa-solid fa-circle-xmark alert-icon" style="color: #E74C3C;"></i>
                    <p style="color: #E74C3C;">아이디 또는 비밀번호가 올바르지 않습니다.</p>
                </div>
            <% } else { %>
                <%-- 기본 경고 박스 (이미지와 동일) --%>
                <div class="alert-box">
                    <i class="fa-solid fa-triangle-exclamation alert-icon"></i>
                    <p>5회 연속 실패 시 계정이 잠깁니다</p>
                </div>
            <% } %>

            <button type="submit" class="login-btn">로그인</button>
        </form>

        <div class="footer-link">
            비밀번호를 잊으셨나요? <a href="#">관리자에게 문의하세요</a>
        </div>
    </div>

</body>
</html>