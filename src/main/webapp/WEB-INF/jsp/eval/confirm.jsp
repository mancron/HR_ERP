<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>평가 확정</title>
<link rel="stylesheet" href="${ctxPath}/css/eval/evaluation.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
<style>
    body { margin:0; background:#fff; font-family:'Pretendard','Apple SD Gothic Neo',sans-serif; }
    .cw  { padding:22px 26px; }
    .ct  { font-size:16px; font-weight:700; color:#1e293b; margin-bottom:16px;
           display:flex; align-items:center; gap:8px; }
    .ro  { width:100%; padding:8px 11px; border:1px solid #e2e8f0; border-radius:6px;
           background:#f8fafc; color:#64748b; font-size:13px; box-sizing:border-box; }
    .ig  { display:grid; grid-template-columns:1fr 1fr; gap:10px 16px; margin-bottom:16px; }
    .ig .form-group label { display:block; font-size:12px; font-weight:600; color:#64748b; margin-bottom:4px; }
    .sro { opacity:0.6; pointer-events:none; width:100%; }
    .ba  { display:flex; justify-content:flex-end; gap:10px;
           margin-top:18px; padding-top:14px; border-top:1px solid #f1f5f9; }
    .stag { display:inline-block; padding:3px 11px; border-radius:20px; font-size:12px; font-weight:600; }
    .sc   { background:#dcfce7; color:#16a34a; }
    .sw   { background:#fef9c3; color:#ca8a04; }
    .sr   { background:#fee2e2; color:#dc2626; }
    #rmsg { font-size:13px; color:#dc2626; margin-top:6px; text-align:right; min-height:18px; }
</style>
</head>
<body>
<div class="cw">

    <div class="ct">
        📋 ${evalData.empName} 평가 상세
        <c:choose>
            <c:when test="${evalData.evalStatus == '최종확정'}">
                <span class="stag sc">최종확정</span>
            </c:when>
            <c:when test="${evalData.isRejected}">
                <span class="stag sr">반려됨</span>
            </c:when>
            <c:otherwise>
                <span class="stag sw">${evalData.evalStatus}</span>
            </c:otherwise>
        </c:choose>
    </div>

    <div class="ig">
        <div class="form-group"><label>평가 대상자</label><input class="ro" value="${evalData.empName}" readonly></div>
        <div class="form-group"><label>평가 연도</label><input class="ro" value="${evalData.evalYear}년" readonly></div>
        <div class="form-group"><label>평가 기간</label><input class="ro" value="${evalData.evalPeriod}" readonly></div>
        <div class="form-group"><label>평가 유형</label><input class="ro" value="${evalData.evalType}" readonly></div>
    </div>

    <div style="font-weight:700;margin-bottom:12px;font-size:13px;">📊 항목별 점수</div>
    <c:forEach var="itemName" items="${itemNames}" varStatus="loop">
        <div class="score-item">
            <div class="score-info"><span>${itemName}</span></div>
            <div class="slider-container">
                <fmt:parseNumber var="sc"
                    value="${not empty itemScores ? itemScores[loop.index] : 0}" integerOnly="true"/>
                <input type="range" class="sro" min="0" max="100" value="${sc}">
                <span class="current-val">${sc}</span><span class="max-val">/100</span>
            </div>
        </div>
    </c:forEach>

    <div class="result-box">
        <div>
            <div style="font-size:13px;color:#64748b;">종합 점수</div>
            <div class="avg-value">
                <c:choose>
                    <c:when test="${not empty evalData.totalScore}">
                        <fmt:formatNumber value="${evalData.totalScore}" pattern="0.0"/>점
                    </c:when>
                    <c:otherwise>—</c:otherwise>
                </c:choose>
            </div>
        </div>
        <div style="text-align:right;">
            <div style="font-size:13px;color:#64748b;">등급</div>
            <div class="grade-badge" style="color:${gradeColor};">${evalData.grade}</div>
        </div>
    </div>

    <c:if test="${not empty evalData.confirmedAt}">
        <div style="font-size:13px;color:#94a3b8;margin-bottom:10px;">
            확정일시: <fmt:formatDate value="${evalData.confirmedAt}" pattern="yyyy-MM-dd HH:mm"/>
        </div>
    </c:if>

    <label style="font-size:13px;color:#64748b;">평가 코멘트</label>
    <textarea readonly style="background:#f8fafc;color:#334155;margin-top:6px;">${evalData.evalComment}</textarea>

    <div id="rmsg"></div>

    <div class="ba">
        <button class="btn btn-save" onclick="closeModal()">닫기</button>

        <%--
            HR담당자만 확정/반려 버튼 표시
            관리자는 일반사용자로 취급 → 버튼 없음
        --%>
        <c:if test="${isHr && evalData.evalStatus != '최종확정'}">
            <button class="btn btn-dept-delete" onclick="doAction('reject')">반려</button>
            <button class="btn btn-submit"      onclick="doAction('confirm')">최종 확정</button>
        </c:if>
    </div>

</div>

<script>
// EvalConfirmServlet에서 request.setAttribute로 직접 전달
// → iframe 내 EL contextPath 빈값 문제 방지
const ctxPath = '${ctxPath}';
const evalId  = '${evalId}';

function doAction(action) {
    const msg = action === 'confirm'
        ? '최종 확정하시겠습니까?\n확정 후에는 수정이 불가하며 audit_log에 기록됩니다.\n대상자에게 알림이 발송됩니다.'
        : '이 평가를 반려하시겠습니까?\n반려 시 작성자에게 알림이 발송됩니다.\n작성자가 재제출해야 합니다.';
    if (!confirm(msg)) return;

    const rmsg = document.getElementById('rmsg');
    rmsg.innerText = '처리 중...';

    fetch(ctxPath + '/eval/confirm', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: 'action=' + action + '&evalId=' + evalId
    })
    .then(r => r.json())
    .then(data => {
        if (data.ok) {
            // 부모창(status.jsp) 즉시 새로고침 후 모달 닫기
            if (window.parent && window.parent.reloadStatusTable) {
                window.parent.reloadStatusTable();
            } else {
                window.parent.location.reload();
            }
            closeModal();
        } else {
            rmsg.innerText = '처리 실패: ' + (data.msg || '오류');
        }
    })
    .catch(() => { document.getElementById('rmsg').innerText = '서버 오류가 발생했습니다.'; });
}

function closeModal() {
    if (window.parent && window.parent.closeConfirmModal) {
        window.parent.closeConfirmModal();
    }
}
</script>
</body>
</html>
