<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<script>
    // 페이지 로드 시 실행
    window.onload = function() {
        const urlParams = new URLSearchParams(window.location.search);
        const error = urlParams.get('error');
        
        if (error === 'mismatch') {
            alert("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        } else if (error === 'fail') {
            alert("현재 비밀번호가 일치하지 않거나 변경에 실패했습니다.");
        }
    };
</script>

<div class="pw-change-container" style="background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); max-width: 500px; margin: 20px auto;">
    <h3>비밀번호 변경</h3>
    <form action="${pageContext.request.contextPath}/auth/pw-change" method="post">
        <div style="margin-bottom: 15px;">
            <label>현재 비밀번호</label>
            <input type="password" name="currentPw" style="width: 100%; padding: 8px; border: 1px solid #ddd;" required>
        </div>
        <div style="margin-bottom: 15px;">
            <label>새 비밀번호</label>
            <input type="password" name="newPw" style="width: 100%; padding: 8px; border: 1px solid #ddd;" required>
        </div>
        <div style="margin-bottom: 20px;">
            <label>새 비밀번호 확인</label>
            <input type="password" name="confirmPw" style="width: 100%; padding: 8px; border: 1px solid #ddd;" required>
        </div>
        
        <button type="submit" style="background: #2151A2; color: white; border: none; padding: 10px 20px; cursor: pointer; border-radius: 4px; width: 100%;">
            비밀번호 변경 저장
        </button>
    </form>
</div>