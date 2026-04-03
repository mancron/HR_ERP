<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>비밀번호 변경</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/pw_change.css">
</head>
<body>

<div class="pw-change-container">
    <div class="pw-header">
        <h2>비밀번호 변경</h2>
        <p>현재 비밀번호를 확인 후 새 비밀번호로 변경합니다</p>
    </div>

    <c:if test="${not empty vd.errorMsg}">
        <div class="error-message-box">
            ${vd.errorMsg}
        </div>
    </c:if>

    <form action="${pageContext.request.contextPath}/auth/pw-change" method="post" id="pwForm">
        <div class="form-group">
            <label>현재 비밀번호 <span class="required">*</span></label>
            <input type="password" name="currentPw" id="currentPw" placeholder="현재 비밀번호 입력" required>
        </div>
        
        <div class="form-group">
            <label>새 비밀번호 <span class="required">*</span></label>
            <input type="password" name="newPw" id="newPw" placeholder="8자 이상, 영문+숫자+특수문자" required>
        </div>
        
        <div class="form-group">
            <label>새 비밀번호 확인 <span class="required">*</span></label>
            <input type="password" name="confirmPw" id="confirmPw" placeholder="새 비밀번호 재입력" required>
        </div>

        <div class="validation-checklist">
            <span class="check-item" id="cond-len"><i>✓</i> 8자 이상</span>
            <span class="check-item" id="cond-eng"><i>✓</i> 영문 포함</span>
            <span class="check-item" id="cond-num"><i>✓</i> 숫자 포함</span>
            <span class="check-item" id="cond-spec"><i>✓</i> 특수문자 포함</span>
            <span class="check-item" id="cond-match"><i>✓</i> 비밀번호 일치</span>
        </div>
        
        <div class="btn-group">
            <button type="button" class="btn-cancel" onclick="history.back()">취소</button>
            <button type="submit" class="btn-submit" id="submitBtn" disabled>변경 완료</button>
        </div>
    </form>
</div>

<script>
// 실시간 유효성 검사 스크립트 (기존 로직 유지)
const newPwInput = document.getElementById('newPw');
const confirmPwInput = document.getElementById('confirmPw');
const submitBtn = document.getElementById('submitBtn');

const conds = {
    len: document.getElementById('cond-len'),
    eng: document.getElementById('cond-eng'),
    num: document.getElementById('cond-num'),
    spec: document.getElementById('cond-spec'),
    match: document.getElementById('cond-match')
};

function validate() {
    const val = newPwInput.value;
    const confirmVal = confirmPwInput.value;

    const isLenValid = val.length >= 8;
    const isEngValid = /[a-zA-Z]/.test(val);
    const isNumValid = /[0-9]/.test(val);
    const isSpecValid = /[~!@#$%^&*()_+|<>?:{}]/.test(val);
    const isMatchValid = val.length > 0 && val === confirmVal;

    updateUI(conds.len, isLenValid);
    updateUI(conds.eng, isEngValid);
    updateUI(conds.num, isNumValid);
    updateUI(conds.spec, isSpecValid);
    updateUI(conds.match, isMatchValid);

    submitBtn.disabled = !(isLenValid && isEngValid && isNumValid && isSpecValid && isMatchValid);
}

function updateUI(el, isValid) {
    if (isValid) el.classList.add('valid');
    else el.classList.remove('valid');
}

newPwInput.addEventListener('input', validate);
confirmPwInput.addEventListener('input', validate);
</script>

</body>
</html>