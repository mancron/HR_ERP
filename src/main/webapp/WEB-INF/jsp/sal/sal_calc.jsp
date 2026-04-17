<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>ERP-HRMS - 급여 계산·지급</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/sal/sal_calc.css">
</head>
<body>
    <jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />
    <div id="main-wrapper">
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
        <main class="app-content">
            <h1 class="page-title">급여 계산·지급</h1>

            <%-- 알림 메시지 --%>
            <c:if test="${not empty successMsg}">
                <div class="alert alert-success"><c:out value="${successMsg}" /></div>
            </c:if>
            <c:if test="${not empty errorMsg}">
                <div class="alert alert-error"><c:out value="${errorMsg}" /></div>
            </c:if>

            <%-- 필터 + 액션 버튼 --%>
            <div class="calc-toolbar">
                <%-- 연도/월 필터 (GET) --%>
                <form action="${pageContext.request.contextPath}/sal/calc"
                      method="get" class="filter-form">
                    <select name="year" class="form-control select-sm">
                        <c:forEach var="y" items="${yearOptions}">
                            <option value="${y}" ${selectedYear == y ? 'selected' : ''}>
                                <c:out value="${y}" />년
                            </option>
                        </c:forEach>
                    </select>
                    <select name="month" class="form-control select-sm">
                        <c:forEach begin="1" end="12" var="m">
                            <option value="${m}" ${selectedMonth == m ? 'selected' : ''}>
                                <c:out value="${m}" />월
                            </option>
                        </c:forEach>
                    </select>
                    <button type="submit" class="btn btn-secondary btn-sm">조회</button>
                </form>

                <%-- 계산 / 재계산 / 전체지급 (POST) --%>
                <div class="action-btns">
                    <%-- 급여 계산 --%>
                    <form action="${pageContext.request.contextPath}/sal/calc"
                          method="post" class="inline-form">
                        <input type="hidden" name="action" value="calculate">
                        <input type="hidden" name="year"   value="${selectedYear}">
                        <input type="hidden" name="month"  value="${selectedMonth}">
                        <button type="submit" class="btn btn-primary btn-sm">급여 계산</button>
                    </form>

                    <%-- 재계산 --%>
                    <form action="${pageContext.request.contextPath}/sal/calc"
                          method="post" class="inline-form"
                          onsubmit="return confirm('대기 상태 급여를 삭제 후 재계산합니다.\n완료 건은 영향받지 않습니다. 진행하시겠습니까?')">
                        <input type="hidden" name="action" value="recalculate">
                        <input type="hidden" name="year"   value="${selectedYear}">
                        <input type="hidden" name="month"  value="${selectedMonth}">
                        <button type="submit" class="btn btn-secondary btn-sm">재계산</button>
                    </form>

							<%-- 전체 지급 처리 버튼 — isClosed && hasPending 일 때만 활성화 --%>
							<form action="${pageContext.request.contextPath}/sal/calc"
							      method="post" class="inline-form"
							      onsubmit="return confirm('대기 중인 급여를 전체 지급 처리합니다.\n이 작업은 되돌릴 수 없습니다. 진행하시겠습니까?')">
							    <input type="hidden" name="action" value="payAll">
							    <input type="hidden" name="year"   value="${selectedYear}">
							    <input type="hidden" name="month"  value="${selectedMonth}">
							    <button type="submit" class="btn btn-success btn-sm"
							            ${(!isClosed || !hasPending) ? 'disabled' : ''}>
							        전체 지급 처리
							    </button>
							</form>
							
							<%-- 근태 마감 버튼 — 미마감 상태일 때만 노출 --%>
<c:if test="${!isClosed}">
    <form action="${pageContext.request.contextPath}/att/close"
          method="post" class="inline-form"
          onsubmit="return confirm(
              '${selectedYear}년 ${selectedMonth}월 근태를 마감합니다.\n' +
              '대기 상태 급여가 현재 근태 기준으로 자동 재계산됩니다.\n' +
              '마감 후에는 되돌릴 수 없습니다. 진행하시겠습니까?')">
        <input type="hidden" name="year"  value="${selectedYear}">
        <input type="hidden" name="month" value="${selectedMonth}">
        <button type="submit" class="btn btn-danger btn-sm">
            🔒 근태 마감
        </button>
    </form>
</c:if>
<c:if test="${isClosed}">
    <span class="closed-badge">✅ 근태 마감 완료</span>
</c:if>
                    
                </div>
            </div>

			<%-- 근태 마감 상태 안내 --%>
			<c:choose>
			    <c:when test="${isClosed}">
			        <div class="calc-notice closed">
			            ✅ 근태가 마감되었습니다. 급여 계산 및 지급 처리가 가능합니다.
			        </div>
			    </c:when>
			    <c:otherwise>
			        <div class="calc-notice">
			            ⚠ 근태가 마감되지 않았습니다. 급여 계산은 가능하나 <strong>지급 처리는 근태 마감 후</strong> 가능합니다.
			        </div>
			    </c:otherwise>
			</c:choose>
			
			<%-- 경고: 완료 건 수정 불가 --%>
			<div class="calc-notice warn">
			    ⚠ status=완료 급여는 수정 불가 — 재계산은 대기 상태에서만 허용됩니다
			</div>

            <%-- 급여 목록 테이블 --%>
            <div class="table-card">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>이름</th>
                            <th>부서</th>
                            <th>직급</th>
                            <th>기본급</th>
                            <th>지급합계</th>
                            <th>공제합계</th>
                            <th>실수령액</th>
                            <th>지급일</th>
                            <th>상태</th>
                            <th>처리</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${empty salaryList}">
                                <tr>
                                    <td colspan="10" class="empty-row">
                                        조회된 급여 데이터가 없습니다. 급여 계산을 먼저 실행해주세요.
                                    </td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="sal" items="${salaryList}">
                                    <tr class="${sal.status == '완료' ? 'row-done' : ''}">
                                        <td><strong><c:out value="${sal.empName}" /></strong></td>
                                        <td><c:out value="${sal.deptName}" /></td>
                                        <td><c:out value="${sal.positionName}" /></td>
                                        <td>
                                            <fmt:formatNumber value="${sal.baseSalary}" pattern="#,###" />
                                        </td>
                                        <td>
                                            <fmt:formatNumber value="${sal.grossSalary}" pattern="#,###" />
                                        </td>
                                        <td>
                                            <fmt:formatNumber value="${sal.totalDeduction}" pattern="#,###" />
                                        </td>
                                        <td class="net-col">
                                            <fmt:formatNumber value="${sal.netSalary}" pattern="#,###" />
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${not empty sal.payDate}">
                                                    <c:out value="${sal.payDate}" />
                                                </c:when>
                                                <c:otherwise>—</c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <span class="badge ${sal.status == '완료' ? 'badge-green' : 'badge-yellow'}">
                                                <c:out value="${sal.status}" />
                                            </span>
                                        </td>
                                        <td>
												<%-- 개별 지급 버튼 — isClosed 일 때만 활성화 --%>
												<c:if test="${sal.status == '대기'}">
												    <c:choose>
												        <c:when test="${isClosed}">
												            <form action="${pageContext.request.contextPath}/sal/calc"
												                  method="post" class="inline-form"
												                  onsubmit="return confirm('${sal.empName}님의 급여를 지급 처리하시겠습니까?')">
												                <input type="hidden" name="action"   value="payOne">
												                <input type="hidden" name="salaryId" value="${sal.salaryId}">
												                <input type="hidden" name="year"     value="${selectedYear}">
												                <input type="hidden" name="month"    value="${selectedMonth}">
												                <button type="submit" class="btn btn-pay btn-xs">지급</button>
												            </form>
												        </c:when>
												        <c:otherwise>
												            <span class="lock-mark" title="근태 마감 후 지급 가능">🔒</span>
												        </c:otherwise>
												    </c:choose>
												</c:if>
                                            
                                            
                                            <c:if test="${sal.status == '완료'}">
                                                <span class="done-mark">✓</span>
                                            </c:if>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>

        </main>
    </div>
    <script src="${pageContext.request.contextPath}/js/sidebar.js"></script>
</body>
</html>