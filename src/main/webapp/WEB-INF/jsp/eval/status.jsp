<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%-- 1. 세션 기반 권한 변수 재정의 --%>
<c:set var="loginEmpId" value="${sessionScope.empId}" />
<c:set var="userRole" value="${sessionScope.userRole}" />
<c:set var="isReadOnlyAdmin" value="${userRole == '사장님' || userRole == '최종승인자'}" />

<%-- 데이터 노출을 위한 전체 관리 권한 (HR 포함) --%>
<c:set var="isAdmin" value="${isHr || isReadOnlyAdmin}" />

<link rel="stylesheet" href="${pageContext.request.contextPath}/css/eval/evaluation.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">

<%-- 상세/확정 모달 영역 --%>
<div id="confirmOverlay" class="modal-overlay">
    <div class="modal-content">
        <iframe id="confirmFrame" src="" class="modal-iframe"></iframe>
    </div>
</div>

<div class="status-container">
    <%-- ── 헤더 및 필터 영역 ── --%>
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
            
            <input type="text" name="searchTarget" value="${searchTarget}" placeholder="대상자 이름" class="search-input">
            
            <c:if test="${isAdmin}">
                <input type="text" name="searchEvaluator" value="${searchEvaluator}" placeholder="평가자 이름" class="search-input">
            </c:if>
            
            <button type="submit" class="btn-search">조회</button>
            <button type="button" class="btn-reset" onclick="resetFilter()">초기화</button>
        </form>

        <a href="${pageContext.request.contextPath}/eval/write" class="btn-add">+ 평가 작성</a>
    </div>

    <%-- ── 등급별 요약 카드 ── --%>
    <div class="summary-cards">
        <div class="card s-card"><span>S 등급</span><strong>${summary.S != null ? summary.S : 0}</strong></div>
        <div class="card a-card"><span>A 등급</span><strong>${summary.A != null ? summary.A : 0}</strong></div>
        <div class="card b-card"><span>B 등급</span><strong>${summary.B != null ? summary.B : 0}</strong></div>
        <div class="card c-card"><span>C 등급</span><strong>${summary.C != null ? summary.C : 0}</strong></div>
        <div class="card d-card"><span>D 등급</span><strong>${summary.D != null ? summary.D : 0}</strong></div>
        
        <%-- 
           [최종 로직 결정]
           1. '전체 유형'일 때: 여러 평가 유형이 섞여있으므로, '사람 수'가 아닌 '미완료 문서 수'를 보여줍니다.
           2. 특정 유형 선택 시: 해당 평가에 대한 '미완료자 수(사람 수)' 통계를 보여줍니다.
        --%>
        <div class="card pending-card">
            <span>
                <c:choose>
                    <c:when test="${selectedType == '전체'}">미완료 건수</c:when>
                    <c:otherwise>미완료자 수</c:otherwise>
                </c:choose>
            </span>
            <strong>${summary['미완료'] != null ? summary['미완료'] : 0}</strong>
        </div>
    </div>

    <%-- ── 데이터 테이블 ── --%>
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
                <c:choose>
                    <c:when test="${not empty statusList}">
                        <c:forEach var="item" items="${statusList}">
                            <c:set var="isTarget" value="${item.empId == loginEmpId}"/>
                            <c:set var="isOwner"  value="${item.evaluatorId == loginEmpId}"/>
                            <c:set var="isComplete" value="${item.status == '최종확정'}"/>

                            <c:set var="shouldShow" value="${isComplete || isOwner || isAdmin}" />
                            <c:if test="${selectedType != '전체' && isTarget && !isAdmin && !isOwner}">
                                <c:set var="shouldShow" value="false" />
                            </c:if>

                            <c:if test="${shouldShow}">
                                <tr>
                                    <td>${item.empName}</td>
                                    <td>${item.deptName}</td>
                                    <c:if test="${selectedPeriod == '전체'}"><td>${item.evalPeriod}</td></c:if>
                                    
                                    <c:if test="${selectedType == '전체'}">
                                        <td>
                                            <c:choose>
                                                <c:when test="${isAdmin || isOwner}"> ${item.evalType} </c:when>
                                                <c:otherwise> <span style="color: #999; font-style: italic;">익명</span> </c:otherwise>
                                            </c:choose>
                                        </td>
                                    </c:if>

                                    <td>
                                        <c:choose>
                                            <c:when test="${item.score != null}"> <fmt:formatNumber value="${item.score}" pattern="0.#"/> </c:when>
                                            <c:otherwise>—</c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty item.grade}"> <span class="badge-${item.grade}">${item.grade}</span> </c:when>
                                            <c:otherwise>—</c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${isComplete}"> <span class="status-complete">최종확정</span> </c:when>
                                            <c:when test="${item.isRejected == true}"> <span class="status-reject" title="${item.evalComment}">반려됨</span> </c:when>
                                            <c:otherwise> <span class="status-working">작성중</span> </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${isAdmin || isOwner}"> ${not empty item.evaluatorName ? item.evaluatorName : '—'} </c:when>
                                            <c:otherwise> <span class="eval-anon" style="color: #999; font-style: italic;">익명</span> </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <fmt:formatDate value="${item.confirmedAt}" pattern="yyyy-MM-dd"/>
                                        <c:if test="${empty item.confirmedAt}">—</c:if>
                                    </td>
                                    
                                    <td>
                                        <c:choose>
                                            <c:when test="${isComplete}">
                                                <c:if test="${(isAdmin || isOwner) && !(!isAdmin && isTarget)}">
                                                    <button class="btn-view" onclick="openModal(${item.evalId})">조회</button>
                                                </c:if>
                                            </c:when>
                                            <c:otherwise>
                                                <c:choose>
                                                    <c:when test="${isOwner}">
                                                        <button class="btn-edit" onclick="location.href='${pageContext.request.contextPath}/eval/write?id=${item.evalId}'">수정</button>
                                                    </c:when>
                                                    <c:when test="${isReadOnlyAdmin}">
                                                        <button class="btn-view" onclick="openModal(${item.evalId})">조회</button>
                                                    </c:when>
                                                    <c:when test="${isHr}">
                                                        <button class="btn-edit" onclick="openModal(${item.evalId})">확정</button>
                                                    </c:when>
                                                </c:choose>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:if>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <tr><td colspan="10" style="text-align:center; padding: 50px 0; color: #999;">조회된 평가 데이터가 없습니다.</td></tr>
                    </c:otherwise>
                </c:choose>
            </tbody>
        </table>
    </div>
</div>

<script>
const ctx = '${pageContext.request.contextPath}';
function openModal(evalId) {
    if(!evalId) return;
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
    // 초기화 시 현재 연도의 전체 데이터로 이동
    location.href = ctx + '/eval/status?year=' + new Date().getFullYear() + '&period=전체&type=전체';
}
document.getElementById('confirmOverlay').addEventListener('click', e => { if (e.target.id === 'confirmOverlay') closeConfirmModal(); });
document.addEventListener('keydown', e => { if (e.key === 'Escape') closeConfirmModal(); });
</script>