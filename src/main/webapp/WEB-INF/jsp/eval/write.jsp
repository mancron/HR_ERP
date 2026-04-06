<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn"  uri="http://java.sun.com/jsp/jstl/functions" %>

<link rel="stylesheet" href="${pageContext.request.contextPath}/css/eval/evaluation.css">

<%-- ── 에러 메시지 ── --%>
<c:if test="${not empty param.error}">
    <div class="eval-error-msg">
        <c:choose>
            <c:when test="${param.error == 'self_eval'}">⚠ 자기 자신을 상위/동료 평가 대상으로 선택할 수 없습니다.</c:when>
            <c:when test="${param.error == 'position_denied'}">⚠ 권한 없음: 본인보다 높거나 같은 직급은 상위평가 대상이 될 수 없습니다.</c:when>
            <c:when test="${param.error == 'forbidden'}">⚠ 접근 권한이 없습니다.</c:when>
            <c:when test="${param.error == 'already_confirmed'}">⚠ 이미 최종확정된 평가는 수정할 수 없습니다.</c:when>
            <%-- duplicate / already_confirmed_other 는 JS alert으로 처리 --%>
            <c:when test="${param.error == 'save_fail'}">⚠ 저장 중 오류가 발생했습니다.</c:when>
            <c:otherwise>⚠ 잘못된 요청입니다.</c:otherwise>
        </c:choose>
    </div>
</c:if>

<%-- ── 반려 안내 배너 ── --%>
<c:if test="${isRejected == true}">
    <div class="eval-reject-banner">
        ⚠ 이 평가는 반려되었습니다. 내용을 검토하고 수정 후 재제출해 주세요.
    </div>
</c:if>

<div class="eval-wrapper">
    <div class="eval-main">
        <div class="section-title">
            <c:choose>
                <c:when test="${not empty evalData}">${evalData.empName} 평가 수정</c:when>
                <c:otherwise>평가 작성</c:otherwise>
            </c:choose>
        </div>

        <form action="${pageContext.request.contextPath}/eval/write" method="post" id="evalForm">
            <c:if test="${not empty evalData}">
                <input type="hidden" name="evalId" value="${evalData.evalId}">
            </c:if>

            <div class="form-grid">
                <%-- 평가 유형: 변경 시 대상자 목록 AJAX 갱신 --%>
                <div class="form-group">
                    <label>평가 유형 *</label>
                    <select name="evalType" id="sel_evalType" onchange="onEvalTypeChange(); checkLoadable();">
                        <option value="상위평가" ${(empty evalData && (empty selectedEvalType || selectedEvalType == '상위평가')) || evalData.evalType == '상위평가' ? 'selected' : ''}>상위평가</option>
                        <option value="자기평가" ${evalData.evalType == '자기평가' || selectedEvalType == '자기평가' ? 'selected' : ''}>자기평가</option>
                        <option value="동료평가" ${evalData.evalType == '동료평가' || selectedEvalType == '동료평가' ? 'selected' : ''}>동료평가</option>
                    </select>
                </div>

                <%-- 평가 대상자: evalType에 따라 동적 갱신 --%>
                <div class="form-group">
                    <label>평가 대상자 *</label>
                    <select name="empId" id="sel_empId" required onchange="checkLoadable()">
                        <option value="">대상자를 선택하세요</option>
                        <c:forEach var="emp" items="${targetList}">
                            <option value="${emp.empId}"
                                ${not empty evalData && evalData.empId == emp.empId ? 'selected' : ''}>
                                ${emp.empName} (${emp.pos})
                            </option>
                        </c:forEach>
                    </select>
                </div>

                <div class="form-group">
                    <label>평가 연도 *</label>
                    <select name="evalYear" id="sel_evalYear" onchange="checkLoadable()">
                        <c:forEach var="y" items="${yearList}">
                            <option value="${y}"
                                ${not empty evalData && evalData.evalYear == y ? 'selected' : ''}>${y}년</option>
                        </c:forEach>
                    </select>
                </div>

                <div class="form-group">
                    <label>평가 기간 *</label>
                    <select name="evalPeriod" id="sel_evalPeriod" onchange="checkLoadable()">
                        <option value="상반기" ${not empty evalData && evalData.evalPeriod == '상반기' ? 'selected' : ''}>상반기</option>
                        <option value="하반기" ${not empty evalData && evalData.evalPeriod == '하반기' ? 'selected' : ''}>하반기</option>
                        <option value="연간"   ${not empty evalData && evalData.evalPeriod == '연간'   ? 'selected' : ''}>연간</option>
                    </select>
                </div>
            </div>

            <%-- ── 불러오기 버튼: 신규 작성 모드에서만 표시 ── --%>
            <c:if test="${empty evalData}">
                <div style="margin-bottom:20px; display:flex; align-items:center; gap:10px;">
                    <button type="button" id="btnLoad" class="btn btn-load" disabled onclick="loadExisting()">
                        📂 기존 평가 불러오기
                    </button>
                    <span id="loadMsg" style="font-size:12px;color:#94a3b8;">
                        유형·대상자·연도·기간을 모두 선택하면 활성화됩니다.
                    </span>
                </div>
            </c:if>

            <div style="font-weight:700;margin-bottom:20px;">📊 항목별 점수 (각 100점 만점)</div>

            <c:forEach var="itemName" items="${itemNames}" varStatus="loop">
                <div class="score-item">
                    <div class="score-info"><span>${itemName}</span></div>
                    <div class="slider-container">
                        <input type="hidden" name="itemNames" value="${itemName}">
                        <fmt:parseNumber var="intScore"
                            value="${not empty itemScores ? itemScores[loop.index] : 80}"
                            integerOnly="true"/>
                        <input type="range" name="scores" min="0" max="100"
                            value="${intScore}"
                            oninput="document.getElementById('out${loop.index}').innerText=this.value; updateEvaluation();">
                        <span class="current-val" id="out${loop.index}">${intScore}</span>
                        <span class="max-val">/100</span>
                    </div>
                </div>
            </c:forEach>

            <div class="result-box">
                <div>
                    <div style="font-size:14px;color:#64748b;">종합 점수 (평균)</div>
                    <div class="avg-value" id="avgScore">
                        <c:choose>
                            <c:when test="${not empty evalData && evalData.totalScore != null}">
                                <fmt:formatNumber value="${evalData.totalScore}" pattern="0.0"/>점
                            </c:when>
                            <c:otherwise>80.0점</c:otherwise>
                        </c:choose>
                    </div>
                </div>
                <div style="text-align:right;">
                    <div style="font-size:14px;color:#64748b;">등급</div>
                    <div class="grade-badge" id="gradeBadge" style="color:${gradeColor};">
                        ${not empty evalData ? evalData.grade : 'A'}
                    </div>
                </div>
            </div>

            <c:if test="${not empty evalData && not empty evalData.confirmedAt}">
                <div style="font-size:13px;color:#94a3b8;margin-bottom:16px;">
                    확정일시: <fmt:formatDate value="${evalData.confirmedAt}" pattern="yyyy-MM-dd HH:mm"/>
                </div>
            </c:if>

            <label style="font-size:13px;color:#64748b;">평가 코멘트</label>
            <%--
                반려된 경우 코멘트에 [반려] 태그가 붙어있음.
                textarea에서 [반려] 태그를 제거하고 보여줌 (저장 시에도 서버에서 REPLACE로 제거됨).
            --%>
            <c:set var="rawComment" value="${not empty evalData ? evalData.evalComment : ''}"/>
            <c:set var="cleanComment" value="${fn:replace(rawComment, '[반려] ', '')}"/>
            <textarea name="evalComment" id="evalComment"
                placeholder="평가 의견을 입력하세요." required>${cleanComment}</textarea>

            <div class="btn-area">
                <button type="submit" name="status" value="작성중" class="btn btn-save">제출</button>
            </div>
        </form>
    </div>

    <div class="eval-side">
        <div class="section-title" style="font-size:15px;">등급 기준표</div>
        <table class="grade-table">
            <thead><tr><th>등급</th><th>점수 범위</th><th>의미</th></tr></thead>
            <tbody>
                <tr class="row-s"><td><strong>S</strong></td><td>95점 이상</td><td>최우수</td></tr>
                <tr class="row-a"><td><strong>A</strong></td><td>85 ~ 94</td><td>우수</td></tr>
                <tr><td><strong>B</strong></td><td>75 ~ 84</td><td>양호</td></tr>
                <tr><td><strong>C</strong></td><td>60 ~ 74</td><td>보통</td></tr>
                <tr><td><strong>D</strong></td><td>60점 미만</td><td>미흡</td></tr>
            </tbody>
        </table>
        <div class="warning-box">
            ※ 제출 후 관리자/HR담당자가 최종 확정합니다.<br>
            확정 후에는 수정이 불가합니다.
        </div>
    </div>
</div>

<script>
const ctx = '${pageContext.request.contextPath}';

// ── 등급 실시간 업데이트 ──────────────────────────────────
function updateEvaluation() {
    const scores = document.getElementsByName('scores');
    let total = 0, count = scores.length;
    if (!count) return;
    scores.forEach(s => { total += parseInt(s.value || 0); });
    const avg = (total / count).toFixed(1);
    document.getElementById('avgScore').innerText = avg + '점';

    let grade = 'D', color = '#94a3b8';
    if      (avg >= 95) { grade = 'S'; color = '#ef4444'; }
    else if (avg >= 85) { grade = 'A'; color = '#f59e0b'; }
    else if (avg >= 75) { grade = 'B'; color = '#3b82f6'; }
    else if (avg >= 60) { grade = 'C'; color = '#22c55e'; }

    const badge = document.getElementById('gradeBadge');
    badge.innerText = grade;
    badge.style.color = color;
}

// ── 평가 유형 변경 시 대상자 목록 AJAX 갱신 ─────────────
function onEvalTypeChange() {
    const evalType = document.getElementById('sel_evalType').value;
    const empSel   = document.getElementById('sel_empId');
    const prevVal  = empSel.value;

    // 로딩 표시
    empSel.innerHTML = '<option value="">조회 중...</option>';
    empSel.disabled  = true;

    fetch(ctx + '/eval/write', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: 'ajaxAction=getTargets&evalType=' + encodeURIComponent(evalType)
    })
    .then(r => r.json())
    .then(list => {
        empSel.innerHTML = '<option value="">대상자를 선택하세요</option>';
        list.forEach(emp => {
            const opt = document.createElement('option');
            opt.value       = emp.empId;
            opt.textContent = emp.empName + ' (' + emp.pos + ')';
            if (String(emp.empId) === prevVal) opt.selected = true;
            empSel.appendChild(opt);
        });
        empSel.disabled = false;
        checkLoadable();
    })
    .catch(() => {
        empSel.innerHTML = '<option value="">조회 실패 — 새로고침 해주세요</option>';
        empSel.disabled  = false;
    });
}

// ── 불러오기 버튼 활성화 (4개 조건 모두 선택 시) ─────────
function checkLoadable() {
    const empId  = document.getElementById('sel_empId')?.value;
    const year   = document.getElementById('sel_evalYear')?.value;
    const period = document.getElementById('sel_evalPeriod')?.value;
    const type   = document.getElementById('sel_evalType')?.value;
    const btn    = document.getElementById('btnLoad');
    const msg    = document.getElementById('loadMsg');
    if (!btn) return;

    if (empId && year && period && type) {
        btn.disabled = false;
        if (msg) msg.innerText = '조건이 일치하는 기존 평가가 있으면 불러올 수 있습니다.';
    } else {
        btn.disabled = true;
        if (msg) msg.innerText = '유형·대상자·연도·기간을 모두 선택하면 활성화됩니다.';
    }
}

// ── 기존 평가 불러오기 (AJAX) ─────────────────────────────
function loadExisting() {
    const empId  = document.getElementById('sel_empId').value;
    const year   = document.getElementById('sel_evalYear').value;
    const period = document.getElementById('sel_evalPeriod').value;
    const type   = document.getElementById('sel_evalType').value;
    const msg    = document.getElementById('loadMsg');
    if (msg) msg.innerText = '조회 중...';

    fetch(ctx + '/eval/write', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: 'ajaxAction=load&empId=' + empId
            + '&evalYear=' + year
            + '&evalPeriod=' + encodeURIComponent(period)
            + '&evalType='  + encodeURIComponent(type)
    })
    .then(r => r.json())
    .then(data => {
        if (data.found && data.evalId) {
            const note = data.isRejected
                ? '\n※ 반려된 평가입니다. 수정 후 재제출해주세요.' : '';
            if (confirm('기존 작성된 평가가 있습니다. 불러오시겠습니까?' + note)) {
                location.href = ctx + '/eval/write?id=' + data.evalId;
            } else {
                if (msg) msg.innerText = '';
            }
        } else {
            if (msg) msg.innerText = data.msg || '해당 조건의 기존 평가가 없습니다.';
        }
    })
    .catch(() => { if (msg) msg.innerText = '불러오기 중 오류가 발생했습니다.'; });
}

// ── 초기화 ───────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', function () {
    updateEvaluation();
    checkLoadable();

    // 중복 평가 alert 처리
    const urlParams = new URLSearchParams(window.location.search);
    const alertType = urlParams.get('alert');
    if (alertType === 'duplicate') {
        alert('이미 같은 조건으로 작성한 평가가 있습니다.\n기존 평가를 불러왔습니다. 확인 후 수정해 주세요.');
    }
});
</script>
