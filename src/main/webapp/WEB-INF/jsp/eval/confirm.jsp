<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn"  uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>평가 확정 상세</title>
<link rel="stylesheet" href="${ctxPath}/css/eval/evaluation.css">
<link rel="stylesheet" href="${ctxPath}/css/style.css">
<style>
    /* 반려 사유 입력 영역 */
    .reject-reason-area {
        display: none;
        margin-top: 14px;
        padding: 14px;
        background: #fff7ed;
        border: 1px solid #fed7aa;
        border-radius: 8px;
    }
    .reject-reason-area label {
        display: block;
        font-size: 13px;
        font-weight: 600;
        color: #c2410c;
        margin-bottom: 6px;
    }
    .reject-reason-area textarea {
        width: 100%;
        height: 80px;
        padding: 8px 10px;
        border: 1px solid #fb923c;
        border-radius: 6px;
        font-size: 13px;
        resize: vertical;
        box-sizing: border-box;
    }
    .reject-reason-area .reason-hint {
        font-size: 11px;
        color: #9a3412;
        margin-top: 4px;
    }

    /* 이미 반려됨 상태 안내 */
    .already-rejected-notice {
        margin-top: 10px;
        padding: 10px 14px;
        background: #fee2e2;
        border: 1px solid #fca5a5;
        border-radius: 6px;
        font-size: 12px;
        color: #dc2626;
        font-weight: 600;
    }

    /* 반려 사유 표시 박스 (읽기전용) */
    .reject-reason-display {
        margin-top: 10px;
        padding: 10px 14px;
        background: #fff7ed;
        border-left: 4px solid #f97316;
        border-radius: 0 6px 6px 0;
        font-size: 13px;
        color: #7c2d12;
    }
    .reject-reason-display strong {
        display: block;
        margin-bottom: 4px;
        color: #c2410c;
    }
</style>
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
            <c:when test="${isRejected || evalData.isRejected}">
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
            <div class="res-grade" style="color:${gradeColor};">${evalData.grade}</div>
        </div>
    </div>

    <%-- 5. 코멘트 영역 (반려 태그 제거 후 표시) --%>
    <div class="form-group">
        <label>평가 코멘트</label>
        <textarea class="txt-area" readonly><c:out value="${fn:trim(fn:replace(fn:replace(evalData.evalComment, '[반려]', ''), '[반려 사유]', '※반려사유:'))}"/></textarea>
    </div>

    <%-- 6. 반려 사유 표시 (이미 반려된 경우, 읽기전용) --%>
    <c:if test="${(isRejected || evalData.isRejected) && not empty rejectReason}">
        <div class="reject-reason-display">
            <strong>📌 반려 사유</strong>
            ${rejectReason}
        </div>
    </c:if>

    <%-- 7. 최종 확정일 --%>
    <c:if test="${not empty evalData.confirmedAt}">
        <div class="confirm-date">
            최종 확정일: <fmt:formatDate value="${evalData.confirmedAt}" pattern="yyyy-MM-dd HH:mm"/>
        </div>
    </c:if>

    <div id="rmsg"></div>

    <%-- 8. 반려 사유 입력 영역 (HR담당자 + 미확정 + 미반려 상태일 때만 표시) --%>
    <c:if test="${isHr && evalData.evalStatus != '최종확정'}">
        <c:choose>
            <c:when test="${isRejected || evalData.isRejected}">
                <%-- 이미 반려됨: 중복 반려 차단 안내 --%>
                <div class="already-rejected-notice">
                    ⚠ 이 평가는 이미 반려된 상태입니다. 작성자가 재제출한 후에 다시 확정하거나 반려할 수 있습니다.
                </div>
            </c:when>
            <c:otherwise>
                <%-- 정상 상태: 반려 사유 입력 가능 --%>
                <div id="rejectReasonArea" class="reject-reason-area">
                    <label>📝 반려 사유 <span style="font-weight:400;color:#9a3412;">(작성자에게 표시됩니다)</span></label>
                    <textarea id="rejectReasonInput" placeholder="반려 사유를 입력해 주세요. (선택 사항)"></textarea>
                    <div class="reason-hint">입력하지 않으면 사유 없이 반려됩니다.</div>
                </div>
            </c:otherwise>
        </c:choose>
    </c:if>

    <%-- 9. 하단 버튼 영역 --%>
    <div class="ba">
        <button class="btn btn-close" onclick="closeModal()">닫기</button>
        <c:if test="${isHr && evalData.evalStatus != '최종확정'}">
            <c:choose>
                <c:when test="${isRejected || evalData.isRejected}">
                    <%-- 이미 반려됨: 반려 버튼 비활성화 --%>
                    <button class="btn btn-reject" disabled title="이미 반려된 상태입니다">반려 불가</button>
                    <button class="btn btn-confirm" onclick="doAction('confirm')">최종 확정</button>
                </c:when>
                <c:otherwise>
                    <button class="btn btn-reject" onclick="showRejectArea()">반려</button>
                    <button class="btn btn-confirm" onclick="doAction('confirm')">최종 확정</button>
                </c:otherwise>
            </c:choose>
        </c:if>
    </div>
</div>

<script>
const ctxPath = '${ctxPath}';
const evalId  = '${evalId}';
let rejectAreaShown = false;

/** 반려 버튼 클릭 → 반려 사유 입력창 표시 후 확인 */
function showRejectArea() {
    const area = document.getElementById('rejectReasonArea');
    if (!area) return;

    if (!rejectAreaShown) {
        area.style.display = 'block';
        rejectAreaShown = true;
        // 버튼 텍스트 변경: "반려" → "반려 확인"
        event.target.textContent = '반려 확인';
        event.target.onclick = function() { doAction('reject'); };
        return;
    }
}

function doAction(action) {
    let confirmMsg;
    if (action === 'confirm') {
        confirmMsg = '최종 확정하시겠습니까?\n확정 후에는 수정이 불가합니다.';
    } else {
        confirmMsg = '이 평가를 반려하시겠습니까?\n반려 시 작성자에게 알림이 발송됩니다.';
    }
    if (!confirm(confirmMsg)) return;

    const rmsg = document.getElementById('rmsg');
    rmsg.style.color = '#3b82f6';
    rmsg.innerText = '처리 중...';

    const params = new URLSearchParams();
    params.append('action', action);
    params.append('evalId', evalId);

    // 반려 시 반려 사유 함께 전송
    if (action === 'reject') {
        const reasonInput = document.getElementById('rejectReasonInput');
        const reason = reasonInput ? reasonInput.value.trim() : '';
        params.append('rejectReason', reason);
    }

    fetch(ctxPath + '/eval/confirm', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params
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
