<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<link rel="stylesheet" href="${pageContext.request.contextPath}/css/eval/evaluation.css">

<div class="status-container">

    <%-- ── 헤더: 필터 폼 + 평가 작성 버튼 ── --%>
    <div class="header-area">
        <h2 class="section-title">평가 현황</h2>
        <form action="${pageContext.request.contextPath}/eval/status" method="get" class="filter-form">

            <%-- 연도: 컨트롤러에서 yearList(List<Integer>) 전달 시 동적 생성, 없으면 현재 연도 기준 최근 3년 --%>
            <select name="year">
                <c:choose>
                    <c:when test="${not empty yearList}">
                        <c:forEach var="y" items="${yearList}">
                            <option value="${y}" ${selectedYear == y ? 'selected' : ''}>${y}년</option>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <jsp:useBean id="now" class="java.util.Date" />
                        <fmt:formatDate var="currentYear" value="${now}" pattern="yyyy"/>
                        <c:forEach var="i" begin="0" end="2">
                            <fmt:parseNumber var="yr" value="${currentYear - i}" integerOnly="true"/>
                            <option value="${yr}" ${selectedYear == yr ? 'selected' : ''}>${yr}년</option>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </select>

            <%-- evalPeriod: 상반기 / 하반기 / 연간 --%>
            <select name="period">
                <option value="상반기" ${selectedPeriod == '상반기' ? 'selected' : ''}>상반기</option>
                <option value="하반기" ${selectedPeriod == '하반기' ? 'selected' : ''}>하반기</option>
                <option value="연간"   ${selectedPeriod == '연간'   ? 'selected' : ''}>연간</option>
            </select>

            <%-- evalType: 상위평가 / 자기평가 / 동료평가 --%>
            <select name="type">
                <option value="상위평가" ${selectedType == '상위평가' ? 'selected' : ''}>상위평가</option>
                <option value="자기평가" ${selectedType == '자기평가' ? 'selected' : ''}>자기평가</option>
                <option value="동료평가" ${selectedType == '동료평가' ? 'selected' : ''}>동료평가</option>
            </select>

            <button type="submit" class="btn-search">조회</button>
        </form>
        <a href="${pageContext.request.contextPath}/eval/write" class="btn-add">+ 평가 작성</a>
    </div>

    <%-- ── 요약 카드: EvaluationDAO.getEvaluationSummary() → Map<String,Integer> ── --%>
    <div class="summary-cards">
        <div class="card s-card"><span>S 등급</span><strong>${summary.S}</strong></div>
        <div class="card a-card"><span>A 등급</span><strong>${summary.A}</strong></div>
        <div class="card b-card"><span>B 등급</span><strong>${summary.B}</strong></div>
        <div class="card c-card"><span>C 등급</span><strong>${summary.C}</strong></div>
        <div class="card d-card"><span>D 등급</span><strong>${summary.D}</strong></div>
        <div class="card pending-card"><span>미완료</span><strong>${summary['미완료']}</strong></div>
    </div>

    <%-- ── 테이블: EvaluationDAO.getEvaluationStatusList() → Vector<Map<String,Object>> ──
         Map 키 목록: evalId / empName / deptName / score(BigDecimal) /
                      grade / status / evaluatorName / confirmedAt(Timestamp)
    --%>
    <div class="table-wrapper">
        <table class="status-table">
            <thead>
                <tr>
                    <th>이름</th>
                    <th>부서</th>
                    <th>점수</th>
                    <th>등급</th>
                    <th>평가 상태</th>
                    <th>평가자</th>
                    <th>확정일시</th>
                    <th>상세</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="item" items="${statusList}">
                    <tr>
                        <%-- empName: JOIN된 사원명 --%>
                        <td>${item.empName}</td>

                        <%-- deptName: DAO에서 '개발1팀' 하드코딩 중 → 추후 실제 JOIN으로 교체 필요 --%>
                        <td>${item.deptName}</td>

                        <%-- score: BigDecimal, null이면 — --%>
                        <td>
                            <c:choose>
                                <c:when test="${item.score != null}">
                                    <fmt:formatNumber value="${item.score}" pattern="0.#"/>
                                </c:when>
                                <c:otherwise>—</c:otherwise>
                            </c:choose>
                        </td>

                        <%-- grade: S/A/B/C/D, null이면 — --%>
                        <td>
                            <c:choose>
                                <c:when test="${not empty item.grade}">
                                    <span class="badge-${item.grade}">${item.grade}</span>
                                </c:when>
                                <c:otherwise>—</c:otherwise>
                            </c:choose>
                        </td>

                        <%-- status: 작성중 / 최종확정 --%>
                        <td>
                            <span class="status-${item.status == '최종확정' ? 'complete' : 'working'}">
                                ${item.status}
                            </span>
                        </td>

                        <%-- evaluatorName: JOIN된 평가자명 --%>
                        <td>${item.evaluatorName}</td>

                        <%-- confirmedAt: Timestamp → yyyy-MM-dd, null이면 — --%>
                        <td>
                            <c:choose>
                                <c:when test="${not empty item.confirmedAt}">
                                    <fmt:formatDate value="${item.confirmedAt}" pattern="yyyy-MM-dd"/>
                                </c:when>
                                <c:otherwise>—</c:otherwise>
                            </c:choose>
                        </td>

                        <%-- 상태 무관하게 수정 버튼으로 통일 --%>
                        <td>
                            <button class="btn-edit"
                                onclick="location.href='${pageContext.request.contextPath}/eval/write?id=${item.evalId}'">
                                수정
                            </button>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>

</div>
