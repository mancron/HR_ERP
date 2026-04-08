<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn"  uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>평가 확정 상세</title>
<%-- CSS 파일 연결 --%>
<link rel="stylesheet" href="${ctxPath}/css/eval/evaluation.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>

<div class="cw">
    <%-- 1. 상태 배지 포함 헤더 --%>
    <div class="ct">
        📋 ${evalData.empName} 평가 상세
        <c:choose>
            <c:when test="${evalData.evalStatus == '최종확정'}">
                <span class="stag sc">최종확정</span>
            </c:when>
            <c:when test="${evalData.isRejected || fn:contains(evalData.evalComment, '[반려]') || evalData.evalStatus == '반려됨'}">
                <span class="stag sr">반려됨</span>
            </c:when>
            <c:otherwise>
                <span class="stag sw">${evalData.evalStatus}</span>
            </c:otherwise>
        </c:choose>
    </div>

    <%-- 2. 정보 그리드 영역 --%>
    <div class="ig">
        <div class="form-group full-width">
            <label>평가 대상자</label>
            <input class="ro ro-highlight" value="${evalData.empName}" readonly>
        </div>

        <div class="form-group">
            <label>평가 연도</label>
            <input class="ro" value="${evalData.evalYear}년" readonly>
        </div>
        <div class="form-group">
            <label>평가 기간</label>
            <input class="ro" value="${evalData.evalPeriod}" readonly>
        </div>

        <div class="form-group">
            <label>평가 유형</label>
            <input class="ro" value="${evalData.evalType}" readonly>
        </div>
        <div class="form-group">
            <label>평가자</label>
            <c:choose>
                <c:when test="${evalData.evalType == '하위평가' && !isHr}">
                    <%-- 익명 처리 스타일을 클래스(ro-anonymous)로 분리 --%>
                    <input class="ro ro-anonymous" value="익명 (상향평가 비공개)" readonly>
                </c:when>
                <c:when test="${evalData.evalType == '자기평가' && empty evalData.evaluatorName}">
                    <input class="ro" value="${evalData.empName}" readonly>
                </c:when>
                <c:otherwise>
                    <input class="ro" value="${not empty evalData.evaluatorName ? evalData.evaluatorName : '정보 없음'}" readonly>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <%-- 3. 항목별 점수 영역 --%>
    <div class="score-section">
        <div class="score-section-title">📊 항목별 점수</div>
        <c:forEach var="itemName" items="${itemNames}" varStatus="loop">
            <div class="score-item">
                <label class="score-label">${itemName}</label>
                <div class="slider-container">
                    <fmt:parseNumber var="sc" value="${not empty itemScores ? itemScores[loop.index] : 0}" integerOnly="true"/>
                    <input type="range" class="sro" min="0" max="100" value="${sc}">
                    <span class="current-val">${sc}</span><span class="max-val">/100</span>
                </div>
            </div>
        </c:forEach>
    </div>

    <%-- 4. 종합 결과 카드 --%>
    <div class="result-card">
        <div>
            <div class="res-label">종합 점수</div>
            <div class="res-val">
                <c:choose>
                    <c:when test="${not empty evalData.totalScore}">
                        <fmt:formatNumber value="${evalData.totalScore}" pattern="0.0"/> <span class="unit-text">점</span>
                    </c:when>
                    <c:otherwise>—</c:otherwise>
                </c:choose>
            </div>
        </div>
        <div class="res-grade-area">
            <div class="res-label">등급</div>
            <%-- 등급 색상은 서버 로직(gradeColor)에 따라 동적으로 변하므로 인라인 유지 --%>
            <div class="res-grade" style="color:${gradeColor};">${evalData.grade}</div>
        </div>
    </div>

    <%-- 5. 코멘트 영역 --%>
    <div class="form-group">
        <label>평가 코멘트</label>
        <textarea class="txt-area" readonly>${fn:trim(fn:replace(evalData.evalComment, '[반려]', ''))}</textarea>
    </div>

    <%-- 6. 최종 확정일 --%>
    <c:if test="${not empty evalData.confirmedAt}">
        <div class="confirm-date">
            최종 확정일: <fmt:formatDate value="${evalData.confirmedAt}" pattern="yyyy-MM-dd HH:mm"/>
        </div>
    </c:if>

    <div id="rmsg"></div>

    <%-- 7. 하단 버튼 영역 --%>
    <div class="ba">
        <button class="btn btn-close" onclick="closeModal()">닫기</button>
        <c:if test="${isHr && evalData.evalStatus != '최종확정'}">
            <button class="btn btn-reject" onclick="doAction('reject')">반려</button>
            <button class="btn btn-confirm" onclick="doAction('confirm')">최종 확정</button>
        </c:if>
    </div>
</div>

<script>
const ctxPath = '${ctxPath}';
const evalId  = '${evalId}';

function doAction(action) {
    const msg = action === 'confirm' ? '최종 확정하시겠습니까?' : '이 평가를 반려하시겠습니까?';
    if (!confirm(msg)) return;

    const rmsg = document.getElementById('rmsg');
    rmsg.style.color = '#3b82f6';
    rmsg.innerText = '처리 중...';

    const params = new URLSearchParams();
    params.append('action', action);
    params.append('evalId', evalId);

    fetch(ctxPath + '/eval/confirm', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params
    })
    .then(r => r.json())
    .then(data => {
        if (data.ok) {
            if (window.parent && window.parent.reloadStatusTable) window.parent.reloadStatusTable();
            else window.parent.location.reload();
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
    if (window.parent && window.parent.closeConfirmModal) window.parent.closeConfirmModal();
}
</script>
</body>
</html>