<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<link rel="stylesheet" href="${pageContext.request.contextPath}/css/eval/evaluation.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">

<div id="confirmOverlay" class="modal-overlay">
    <div class="modal-content">
        <iframe id="confirmFrame" src="" class="modal-iframe"></iframe>
    </div>
</div>

<div class="status-container">

    <%-- ── 헤더 ── --%>
    <div class="header-area">
        <h2 class="section-title">평가 현황</h2>

        <form action="${pageContext.request.contextPath}/eval/status" method="get" class="filter-form" id="filterForm">
            <select name="year">
                <c:forEach var="y" items="${yearList}">
                    <c:if test="${y != 0}">
                        <option value="${y}" ${selectedYear == y ? 'selected' : ''}>${y}년</option>
                    </c:if>
                </c:forEach>
            </select>
            <select name="period">
                <option value="전체"   ${selectedPeriod == '전체'   ? 'selected' : ''}>전체 기간</option>
                <option value="상반기" ${selectedPeriod == '상반기' ? 'selected' : ''}>상반기</option>
                <option value="하반기" ${selectedPeriod == '하반기' ? 'selected' : ''}>하반기</option>
                <option value="연간"   ${selectedPeriod == '연간'   ? 'selected' : ''}>연간</option>
            </select>
            <select name="type">
                <option value="전체"   ${selectedType == '전체'   ? 'selected' : ''}>전체 유형</option>
                <option value="상위평가" ${selectedType == '상위평가' ? 'selected' : ''}>상위평가</option>
                <option value="자기평가" ${selectedType == '자기평가' ? 'selected' : ''}>자기평가</option>
                <option value="동료평가" ${selectedType == '동료평가' ? 'selected' : ''}>동료평가</option>
                <option value="하위평가" ${selectedType == '하위평가' ? 'selected' : ''}>하위평가</option>
            </select>
            <input type="text" name="searchTarget"    value="${searchTarget}"
                placeholder="대상자 이름" class="search-input">
            <input type="text" name="searchEvaluator" value="${searchEvaluator}"
                placeholder="평가자 이름" class="search-input">
            <button type="submit" class="btn-search">조회</button>
            <button type="button" class="btn-reset" onclick="resetFilter()">초기화</button>
        </form>

        <a href="${pageContext.request.contextPath}/eval/write" class="btn-add">+ 평가 작성</a>
    </div>

    <%-- ── 요약 카드 ── --%>
    <div class="summary-cards">
        <div class="card s-card"><span>S 등급</span><strong>${summary.S}</strong></div>
        <div class="card a-card"><span>A 등급</span><strong>${summary.A}</strong></div>
        <div class="card b-card"><span>B 등급</span><strong>${summary.B}</strong></div>
        <div class="card c-card"><span>C 등급</span><strong>${summary.C}</strong></div>
        <div class="card d-card"><span>D 등급</span><strong>${summary.D}</strong></div>
        <div class="card pending-card"><span>미완료</span><strong>${summary['미완료']}</strong></div>
    </div>

    <%-- ── 테이블 ── --%>
    <div class="table-wrapper">
        <table class="status-table" id="statusTable">
            <thead>
                <tr>
                    <th>이름</th><th>부서</th>
                    <c:if test="${selectedPeriod == '전체'}"><th>기간</th></c:if>
                    <c:if test="${selectedType == '전체'}"><th>유형</th></c:if>
                    <th>점수</th><th>등급</th>
                    <th>평가 상태</th><th>평가자</th><th>확정일시</th><th>상세</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="item" items="${statusList}">
                    <tr>
                        <td>${item.empName}</td>
                        <td>${item.deptName}</td>
                        <c:if test="${selectedPeriod == '전체'}"><td>${item.evalPeriod}</td></c:if>
                        <c:if test="${selectedType == '전체'}"><td>${item.evalType}</td></c:if>
                        <td>
                            <c:choose>
                                <c:when test="${item.score != null}">
                                    <fmt:formatNumber value="${item.score}" pattern="0.#"/>
                                </c:when>
                                <c:otherwise>—</c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${not empty item.grade}">
                                    <span class="badge-${item.grade}">${item.grade}</span>
                                </c:when>
                                <c:otherwise>—</c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${item.status == '최종확정'}">
                                    <span class="status-complete">최종확정</span>
                                </c:when>
                                <c:when test="${item.isRejected == true}">
                                    <span class="status-reject">반려됨</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="status-working">작성중</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:choose>
                                <%-- 하위평가이더라도 인사담당자(isHr)이거나 사장님(최종승인자)이면 실명 노출 --%>
                                <c:when test="${item.evalType == '하위평가' && !isHr && sessionScope.userRole != '최종승인자'}">
                                    <span class="eval-anon">익명</span>
                                </c:when>
                                <c:otherwise>
                                    ${item.evaluatorName}
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${not empty item.confirmedAt}">
                                    <fmt:formatDate value="${item.confirmedAt}" pattern="yyyy-MM-dd"/>
                                </c:when>
                                <c:otherwise>—</c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:set var="isOwner" value="${item.evaluatorId == loginEmpId}"/>
                            <c:set var="isCEO" value="${sessionScope.userRole == '최종승인자'}"/>
                            <c:choose>
                                <%-- 1. 최종확정된 상태일 때 --%>
                                <c:when test="${item.status == '최종확정'}">
                                    <c:if test="${isHr || isOwner || isCEO}">
                                        <button class="btn-view" onclick="openModal(${item.evalId})">보기</button>
                                    </c:if>
                                </c:when>
                                
                                <%-- 2. 작성 중이거나 반려 상태일 때 --%>
                                <c:otherwise>
                                    <c:choose>
                                        <%-- 사장님은 본인 것이 아닐 때 '조회' 버튼 (조회만 가능) --%>
                                        <c:when test="${isCEO && !isOwner}">
                                            <button class="btn-view" onclick="openModal(${item.evalId})">조회</button>
                                        </c:when>
                                        <%-- 인사담당자는 본인 것이 아닐 때 '확정' 버튼 (확정 처리 권한) --%>
                                        <c:when test="${isHr && !isOwner}">
                                            <button class="btn-edit" onclick="openModal(${item.evalId})">확정</button>
                                        </c:when>
                                        <%-- 본인이 작성 중인 것은 '수정' 버튼 --%>
                                        <c:when test="${isOwner}">
                                            <button class="btn-edit"
                                                onclick="location.href='${pageContext.request.contextPath}/eval/write?id=${item.evalId}'">
                                                수정
                                            </button>
                                        </c:when>
                                    </c:choose>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
</div>

<script>
const ctx = '${pageContext.request.contextPath}';

function openModal(evalId) {
    document.getElementById('confirmFrame').src = ctx + '/eval/confirm?id=' + evalId;
    document.getElementById('confirmOverlay').style.display = 'block';
    document.body.style.overflow = 'hidden';
}

function closeConfirmModal() {
    document.getElementById('confirmOverlay').style.display = 'none';
    document.getElementById('confirmFrame').src = '';
    document.body.style.overflow = '';
}

function resetFilter() {
    const currentYear = new Date().getFullYear();
    location.href = ctx + '/eval/status?year=' + currentYear;
}

// 오버레이 및 ESC 닫기 로직
document.getElementById('confirmOverlay').addEventListener('click', e => { if (e.target.id === 'confirmOverlay') closeConfirmModal(); });
document.addEventListener('keydown', e => { if (e.key === 'Escape') closeConfirmModal(); });
</script>