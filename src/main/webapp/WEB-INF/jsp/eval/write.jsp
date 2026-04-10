<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<%-- 현재 월을 가져와서 변수에 저장 (기간 제어용) --%>
<c:set var="currentMonth" value="<%= java.time.LocalDate.now().getMonthValue() %>" />

<link rel="stylesheet" href="${pageContext.request.contextPath}/css/eval/evaluation.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">

<c:set var="errorVal" value="${not empty param.error ? param.error : errorCode}" />

<c:if test="${not empty errorVal}">
    <div class="eval-error-msg auto-hide">
        <c:choose>
            <c:when test="${errorVal == 'self_eval'}">⚠ 자기 자신을 상위/동료 평가 대상으로 선택할 수 없습니다.</c:when>
            <c:when test="${errorVal == 'position_denied'}">⚠ 권한 없음: 본인보다 높거나 같은 직급은 상위평가 대상이 될 수 없습니다.</c:when>
            <c:when test="${errorVal == 'forbidden'}">⚠ 접근 권한이 없습니다.</c:when>
            <c:when test="${errorVal == 'already_confirmed'}">⚠ 이미 최종확정된 평가는 수정할 수 없습니다.</c:when>
            <c:when test="${errorVal == 'save_fail'}">⚠ 저장 중 오류가 발생했습니다.</c:when>
            <c:when test="${errorVal == 'comment_required'}">⚠ 평가 의견(코멘트)을 입력해주세요.</c:when>
            <c:when test="${errorVal == 'target_required'}">⚠ 평가 대상자를 반드시 선택해야 합니다.</c:when>
            <c:when test="${errorVal == 'duplicate'}">⚠ 해당 조건으로 이미 작성 중인 데이터가 있습니다.</c:when>
            <c:otherwise>⚠ 잘못된 요청입니다. (${errorVal})</c:otherwise>
        </c:choose>
    </div>
</c:if>

<c:if test="${isRejected == true}">
    <div class="eval-reject-banner">⚠ 이 평가는 반려되었습니다. 내용을 검토하고 수정 후 재제출해 주세요.</div>
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
                <c:choose>
                    <c:when test="${not empty evalData}">
                        <input type="hidden" name="evalType" value="${evalData.evalType}">
                        <input type="hidden" name="empId" value="${evalData.empId}">
                        <input type="hidden" name="evalYear" value="${evalData.evalYear}">
                        <input type="hidden" name="evalPeriod" value="${evalData.evalPeriod}">

                        <div class="form-group"><label>평가 유형</label><input type="text" class="field-readonly" value="${evalData.evalType}" readonly></div>
                        <div class="form-group"><label>평가 대상자</label><input type="text" class="field-readonly" value="${evalData.empName}" readonly></div>
                        <div class="form-group"><label>평가 연도</label><input type="text" class="field-readonly" value="${evalData.evalYear}년" readonly></div>
                        <div class="form-group"><label>평가 기간</label><input type="text" class="field-readonly" value="${evalData.evalPeriod}" readonly></div>
                    </c:when>
                    <c:otherwise>
                        <div class="form-group">
                            <label>평가 유형 *</label> 
                            <select name="evalType" id="sel_evalType" onchange="onEvalTypeChange(); checkLoadable();">
                                <option value="상위평가" ${(empty selectedEvalType or selectedEvalType == '상위평가') ? 'selected' : ''}>상위평가</option>
                                <option value="자기평가" ${selectedEvalType == '자기평가' ? 'selected' : ''}>자기평가</option>
                                <option value="동료평가" ${selectedEvalType == '동료평가' ? 'selected' : ''}>동료평가</option>
                                <option value="하위평가" ${selectedEvalType == '하위평가' ? 'selected' : ''}>하위평가</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label>평가 대상자 *</label> 
                            <select name="empId" id="sel_empId" required onchange="checkLoadable()">
                                <option value="">대상자를 선택하세요</option>
                                <c:forEach var="emp" items="${targetList}"><option value="${emp.empId}" ${param.empId == emp.empId ? 'selected' : ''}>${emp.empName} (${emp.pos})</option></c:forEach>
                            </select>
                        </div>
                        <div class="form-group">
                            <label>평가 연도 *</label> 
                            <select name="evalYear" id="sel_evalYear" onchange="checkLoadable()">
                                <c:forEach var="y" items="${yearList}"><option value="${y}" ${param.evalYear == y ? 'selected' : ''}>${y}년</option></c:forEach>
                            </select>
                        </div>
                        <div class="form-group">
                            <label>평가 기간 *</label> 
                            <select name="evalPeriod" id="sel_evalPeriod" onchange="checkLoadable()">
                                <option value="상반기" ${param.evalPeriod == '상반기' ? 'selected' : ''} 
                                    ${(currentMonth >= 5 && currentMonth <= 6) ? '' : 'disabled'}>
                                    상반기 ${(currentMonth >= 5 && currentMonth <= 6) ? '' : '(기간 아님)(5~6월)'}
                                </option>
                                <option value="하반기" ${param.evalPeriod == '하반기' ? 'selected' : ''} 
                                    ${(currentMonth == 11 || currentMonth == 12) ? '' : 'disabled'}>
                                    하반기 ${(currentMonth == 11 || currentMonth == 12) ? '' : '(기간 아님)(11~12월)'}
                                </option>
                                <option value="연간" ${param.evalPeriod == '연간' ? 'selected' : ''} 
                                    ${(currentMonth == 12 || currentMonth == 1) ? '' : 'disabled'}>
                                    연간 ${(currentMonth == 12 || currentMonth == 1) ? '' : '(기간 아님)(12~1월)'}
                                </option>
                            </select>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>

            <c:if test="${empty evalData}">
                <div class="load-area">
                    <button type="button" id="btnLoad" class="btn btn-load" disabled onclick="loadExisting()">📂 기존 평가 불러오기</button>
                    <span id="loadMsg" class="helper-text">유형·대상자·연도·기간을 모두 선택하면 활성화됩니다.</span>
                </div>
            </c:if>

            <div class="score-title">📊 항목별 점수 (각 100점 만점)</div>
            <c:forEach var="itemName" items="${itemNames}" varStatus="loop">
                <div class="score-item">
                    <div class="score-info"><span>${itemName}</span></div>
                    <div class="slider-container">
                        <input type="hidden" name="itemNames" value="${itemName}">
                        <fmt:parseNumber var="intScore" value="${not empty itemScores ? itemScores[loop.index] : (not empty paramValues.scores ? paramValues.scores[loop.index] : 80)}" integerOnly="true" />
                        <input type="range" name="scores" min="0" max="100" value="${intScore}" 
                               oninput="document.getElementById('out${loop.index}').innerText=this.value; updateEvaluation();"
                               ${sessionScope.userRole == '최종승인자' ? 'disabled' : ''}>
                        <span class="current-val" id="out${loop.index}">${intScore}</span><span class="max-val">/100</span>
                    </div>
                </div>
            </c:forEach>

            <div class="result-box">
                <div class="res-left">
                    <div class="res-label">종합 점수 (평균)</div>
                    <div class="avg-value" id="avgScore">${not empty evalData && evalData.totalScore != null ? evalData.totalScore : '80.0'}점</div>
                </div>
                <div class="res-right">
                    <div class="res-label">등급</div>
                    <div class="grade-badge" id="gradeBadge">${not empty evalData ? evalData.grade : 'A'}</div>
                </div>
            </div>

            <div class="comment-group">
                <label class="res-label">평가 코멘트</label>
                <c:set var="rawComment" value="${not empty evalData ? evalData.evalComment : ''}" />
                <c:set var="cleanComment" value="${fn:replace(rawComment, '[반려] ', '')}" />
                <textarea name="evalComment" id="evalComment" placeholder="평가 의견을 입력하세요." required 
                          ${sessionScope.userRole == '최종승인자' ? 'readonly' : ''}><c:choose><c:when test="${not empty tempComment}">${tempComment}</c:when><c:otherwise>${cleanComment}</c:otherwise></c:choose></textarea>
            </div>

            <div class="btn-area">
                <c:choose>
                    <c:when test="${sessionScope.userRole == '최종승인자'}">
                        <div class="admin-notice" style="border-left-color: #3b82f6;">
                            <span class="notice-icon" style="color: #3b82f6;">ℹ</span>
                            <p class="notice-text">
                                <strong>조회 전용 모드:</strong> 사장님(최종승인자) 계정은 평가 현황 조회를 위해 모든 페이지에 접근 가능하나, <strong>평가 작성 및 수정은 불가능</strong>합니다.
                            </p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <c:if test="${isHr}">
                            <div class="admin-notice">
                                <span class="notice-icon">ℹ</span>
                                <p class="notice-text">
                                    <strong>관리자 권한:</strong> 현재 HR 권한으로 접속 중입니다. 제출 후 [평가 현황]에서 확정이 가능합니다.
                                </p>
                            </div>
                        </c:if>
                        <button type="submit" name="status" value="작성중" class="btn btn-submit">제출하기</button>
                    </c:otherwise>
                </c:choose>
            </div>
        </form>
    </div>

    <div class="eval-side">
        <div class="side-title">등급 기준표</div>
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
    </div>
</div>

<script>
const ctx = '${pageContext.request.contextPath}';
const isEditMode = ${not empty evalData ? 'true' : 'false'};

function updateEvaluation() {
    const scores = document.getElementsByName('scores');
    let total = 0, count = scores.length;
    if (!count) return;
    scores.forEach(s => { total += parseInt(s.value || 0); });
    const avg = (total / count).toFixed(1);
    document.getElementById('avgScore').innerText = avg + '점';
    let grade = 'D', color = '#94a3b8';
    if (avg >= 95) { grade = 'S'; color = '#ef4444'; }
    else if (avg >= 85) { grade = 'A'; color = '#f59e0b'; }
    else if (avg >= 75) { grade = 'B'; color = '#3b82f6'; }
    else if (avg >= 60) { grade = 'C'; color = '#22c55e'; }
    const badge = document.getElementById('gradeBadge');
    badge.innerText = grade; 
    badge.style.color = color;
}

function onEvalTypeChange() {
    if (isEditMode) return;
    
    const evalType = document.getElementById('sel_evalType').value;
    const empSel = document.getElementById('sel_empId');
    const btnLoad = document.getElementById('btnLoad');
    
    console.log(">> 선택된 평가 유형:", evalType);
    
    // 로딩 상태 표시
    empSel.innerHTML = '<option value="">조회 중...</option>';
    empSel.disabled = true;
    if(btnLoad) btnLoad.disabled = true;

    fetch(ctx + '/eval/write', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: 'ajaxAction=getTargets&evalType=' + encodeURIComponent(evalType)
    })
    .then(r => {
        if (!r.ok) throw new Error('Network response was not ok');
        return r.json();
    })
    .then(list => {
        console.log(">> 수신된 대상자 리스트:", list);
        
        empSel.innerHTML = '<option value="">대상자를 선택하세요</option>';
        
        if (!list || list.length === 0) {
            empSel.innerHTML = '<option value="">평가 가능한 대상자가 없습니다</option>';
        } else {
            list.forEach(emp => {
                const opt = document.createElement('option');
                opt.value = emp.empId;
                // 서버에서 보낸 필드명(empName, pos)과 정확히 일치 확인
                opt.textContent = emp.empName + ' (' + emp.pos + ')';
                empSel.appendChild(opt);
            });
        }
        
        empSel.disabled = false;
        checkLoadable(); // 목록 갱신 후 버튼 상태 다시 체크
    })
    .catch(err => { 
        console.error(">> AJAX 오류 발생:", err);
        empSel.innerHTML = '<option value="">조회 실패 (콘솔 확인)</option>'; 
        empSel.disabled = false; 
    });
}

function checkLoadable() {
    if (isEditMode) return;
    
    const btn = document.getElementById('btnLoad');
    const empSel = document.getElementById('sel_empId');
    const yearSel = document.getElementById('sel_evalYear');
    const periodSel = document.getElementById('sel_evalPeriod');
    const typeSel = document.getElementById('sel_evalType');

    if (!btn || !empSel || !yearSel || !periodSel || !typeSel) return;

    const empId = empSel.value;
    const year = yearSel.value;
    const period = periodSel.value;
    const type = typeSel.value;
    
    // 에러 발생 지점 수정: 옵션이 선택되었는지 먼저 확인
    let isPeriodDisabled = true;
    if (periodSel.selectedIndex !== -1) {
        isPeriodDisabled = periodSel.options[periodSel.selectedIndex].disabled;
    }
    
    // 모든 값이 있고, 선택한 기간이 'disabled' 상태가 아닐 때만 버튼 활성화
    btn.disabled = !(empId && year && period && type && !isPeriodDisabled);
}

function loadExisting() {
    const empId = document.getElementById('sel_empId').value;
    const year = document.getElementById('sel_evalYear').value;
    const period = document.getElementById('sel_evalPeriod').value;
    const type = document.getElementById('sel_evalType').value;
    fetch(ctx + '/eval/write', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: 'ajaxAction=load&empId=' + empId + '&evalYear=' + year + '&evalPeriod=' + encodeURIComponent(period) + '&evalType=' + encodeURIComponent(type)
    })
    .then(r => r.json())
    .then(data => {
        if (data.found && data.evalId) {
            if (confirm('기존 작성된 평가를 불러오시겠습니까?')) location.href = ctx + '/eval/write?id=' + data.evalId;
        } else { alert(data.msg || '기존 평가가 없습니다.'); }
    });
}

document.addEventListener('DOMContentLoaded', function () {
    updateEvaluation();
    checkLoadable();
    const errorMessages = document.querySelectorAll('.auto-hide');
    errorMessages.forEach(msg => {
        setTimeout(() => {
            msg.classList.add('fade-out');
            setTimeout(() => { msg.style.display = "none"; }, 800);
        }, 3000); 
    });
});
</script>