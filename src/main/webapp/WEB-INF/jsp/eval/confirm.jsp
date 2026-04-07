<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn"  uri="http://java.sun.com/jsp/jstl/functions" %>

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
<body style="margin:0; background:#fff; font-family:'Pretendard',sans-serif;">

<div class="cw">

    <%-- 헤더 영역 및 상태 배지 --%>
    <div class="ct">
        📋 ${evalData.empName} 평가 상세
        <c:choose>
            <c:when test="${evalData.evalStatus == '최종확정'}">
                <span class="stag sc">최종확정</span>
            </c:when>
            <%-- C-1 대비: Map 데이터 혹은 상태 문자열 확인 --%>
            <c:when test="${evalData.isRejected || fn:contains(evalData.evalComment, '[반려]') || evalData.evalStatus == '반려됨'}">
                <span class="stag sr">반려됨</span>
            </c:when>
            <c:otherwise>
                <span class="stag sw">${evalData.evalStatus}</span>
            </c:otherwise>
        </c:choose>
    </div>

    <%-- 기본 정보 그리드 --%>
    <div class="ig">
        <div class="form-group"><label>평가 대상자</label><input class="ro" value="${evalData.empName}" readonly></div>
        <div class="form-group"><label>평가 연도</label><input class="ro" value="${evalData.evalYear}년" readonly></div>
        <div class="form-group"><label>평가 기간</label><input class="ro" value="${evalData.evalPeriod}" readonly></div>
        <div class="form-group"><label>평가 유형</label><input class="ro" value="${evalData.evalType}" readonly></div>
    </div>

    <%-- 점수 리스트 --%>
    <div style="font-weight:700;margin-bottom:12px;font-size:13px;color:#1e293b;">📊 항목별 점수</div>
    <c:forEach var="itemName" items="${itemNames}" varStatus="loop">
        <div class="score-item" style="margin-bottom:15px;">
            <div class="score-info" style="margin-bottom:6px;"><span>${itemName}</span></div>
            <div class="slider-container">
                <fmt:parseNumber var="sc" value="${not empty itemScores ? itemScores[loop.index] : 0}" integerOnly="true"/>
                <input type="range" class="sro" min="0" max="100" value="${sc}">
                <span class="current-val" style="font-size:15px;">${sc}</span><span class="max-val">/100</span>
            </div>
        </div>
    </c:forEach>

    <%-- 종합 결과 --%>
    <div class="result-box" style="padding:15px 20px; margin:20px 0;">
        <div>
            <div style="font-size:12px;color:#64748b;">종합 점수</div>
            <div class="avg-value" style="font-size:24px;">
                <c:choose>
                    <c:when test="${not empty evalData.totalScore}">
                        <fmt:formatNumber value="${evalData.totalScore}" pattern="0.0"/>점
                    </c:when>
                    <c:otherwise>—</c:otherwise>
                </c:choose>
            </div>
        </div>
        <div style="text-align:right;">
            <div style="font-size:12px;color:#64748b;">등급</div>
            <div class="grade-badge" style="color:${gradeColor}; font-size:32px;">${evalData.grade}</div>
        </div>
    </div>

    <%-- 확정일시 표시 --%>
    <c:if test="${not empty evalData.confirmedAt}">
        <div style="font-size:12px;color:#94a3b8;margin-bottom:10px;">
            확정일시: <fmt:formatDate value="${evalData.confirmedAt}" pattern="yyyy-MM-dd HH:mm"/>
        </div>
    </c:if>

    <%-- 평가 코멘트 (N-2 적용: [반려] 태그 제거 후 출력) --%>
    <label style="font-size:13px;font-weight:600;color:#64748b;">평가 코멘트</label>
    <textarea readonly>${fn:replace(evalData.evalComment, '[반려]', '')}</textarea>

    <%-- 메시지 출력 영역 --%>
    <div id="rmsg"></div>

    <%-- 하단 버튼 영역 --%>
    <div class="ba">
        <button class="btn btn-save" style="padding:8px 20px;" onclick="closeModal()">닫기</button>

        <c:if test="${isHr && evalData.evalStatus != '최종확정'}">
            <button class="btn btn-dept-delete" style="padding:8px 20px;" onclick="doAction('reject')">반려</button>
            <button class="btn btn-submit" style="padding:8px 20px; background:#3b82f6;" onclick="doAction('confirm')">최종 확정</button>
        </c:if>
    </div>

</div>

<script>
const ctxPath = '${ctxPath}';
const evalId  = '${evalId}';

function doAction(action) {
    const msg = action === 'confirm'
        ? '최종 확정하시겠습니까?\n확정 후에는 수정이 불가하며 평가 결과가 공식 반영됩니다.'
        : '이 평가를 반려하시겠습니까?\n반려 시 작성자에게 알림이 발송되며 재작성이 필요합니다.';
    
    if (!confirm(msg)) return;

    const rmsg = document.getElementById('rmsg');
    rmsg.style.color = '#3b82f6';
    rmsg.innerText = '처리 중...';

    fetch(ctxPath + '/eval/confirm', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: 'action=' + action + '&evalId=' + evalId
    })
    .then(r => r.json())
    .then(data => {
        if (data.ok) {
            if (window.parent && window.parent.reloadStatusTable) {
                window.parent.reloadStatusTable();
            } else {
                window.parent.location.reload();
            }
            closeModal();
        } else {
            rmsg.style.color = '#dc2626';
            rmsg.innerText = '처리 실패: ' + (data.msg || '오류');
        }
    })
    .catch(() => { 
        rmsg.style.color = '#dc2626';
        rmsg.innerText = '서버 오류가 발생했습니다.'; 
    });
}

function closeModal() {
    if (window.parent && window.parent.closeConfirmModal) {
        window.parent.closeConfirmModal();
    }
}
</script>
</body>
</html>