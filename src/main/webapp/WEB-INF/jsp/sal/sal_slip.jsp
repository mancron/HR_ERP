<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>HR ERP - 급여 명세서</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/sal/sal_slip.css">
</head>
<body>
    <jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />
    <div id="main-wrapper">
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
        <main class="app-content">

            <%-- 헤더: 타이틀 + 인쇄 버튼 --%>
            <div class="slip-header no-print">
                <h1 class="page-title">급여 명세서</h1>
                <c:if test="${not empty slip}">
                    <button type="button" class="btn btn-secondary"
                            onclick="window.print()">🖨 인쇄</button>
                </c:if>
            </div>

				<%-- 검색 필터: 연도/월만, 직원 선택 드롭다운 제거 --%>
				<form action="${pageContext.request.contextPath}/sal/slip"
				      method="get" class="search-bar no-print">
				
				    <%-- 연도 선택 --%>
				    <select name="year" class="form-control select-sm">
				        <c:forEach var="y" items="${yearOptions}">
				            <option value="${y}" ${selectedYear == y ? 'selected' : ''}>
				                <c:out value="${y}" />년
				            </option>
				        </c:forEach>
				    </select>
				
				    <%-- 월 선택 --%>
				    <select name="month" class="form-control select-sm">
				        <c:forEach begin="1" end="12" var="m">
				            <option value="${m}" ${selectedMonth == m ? 'selected' : ''}>
				                <c:out value="${m}" />월
				            </option>
				        </c:forEach>
				    </select>
				
				    <button type="submit" class="btn btn-primary btn-sm">조회</button>
				</form>

            <%-- 명세서 없음 --%>
            <c:if test="${empty slip}">
                <div class="empty-slip">
                    <p>📋 해당 기간의 급여 명세서가 없습니다.</p>
                    <p style="font-size:12px; color:#9ca3af; margin-top:6px;">
                        급여 계산이 완료된 후 조회 가능합니다.
                    </p>
                </div>
            </c:if>

            <%-- 명세서 카드 --%>
            <c:if test="${not empty slip}">
                <div class="slip-card">

                    <%-- 명세서 타이틀 --%>
                    <div class="slip-title-bar">
                        <div>
                            <span class="slip-period">
                                <c:out value="${slip.salaryYear}" />년
                                <c:out value="${slip.salaryMonth}" />월
                            </span>
                            급여 명세서
                        </div>
                        <span class="badge ${slip.status == '완료' ? 'badge-green' : 'badge-yellow'}">
                            <c:out value="${slip.status}" />
                        </span>
                    </div>

                    <%-- 직원 정보 --%>
                    <div class="emp-info-grid">
                        <div class="emp-info-item">
                            <span class="label">성명</span>
                            <span class="value"><c:out value="${slip.empName}" /></span>
                        </div>
                        <div class="emp-info-item">
                            <span class="label">사번</span>
                            <span class="value"><c:out value="${slip.empNo}" /></span>
                        </div>
                        <div class="emp-info-item">
                            <span class="label">부서</span>
                            <span class="value"><c:out value="${slip.deptName}" /></span>
                        </div>
                        <div class="emp-info-item">
                            <span class="label">직급</span>
                            <span class="value"><c:out value="${slip.positionName}" /></span>
                        </div>
                        <div class="emp-info-item">
                            <span class="label">지급일</span>
                            <span class="value">
                                <c:choose>
                                    <c:when test="${not empty slip.payDate}">
                                        <c:out value="${slip.payDate}" />
                                    </c:when>
                                    <c:otherwise>—</c:otherwise>
                                </c:choose>
                            </span>
                        </div>
                    </div>

                    <%-- 지급/공제 2열 테이블 --%>
                    <div class="slip-table-grid">

                        <%-- 지급 항목 --%>
                        <div class="slip-section">
                            <div class="slip-section-title pay">지급 항목</div>
                            <table class="slip-table">
                                <tbody>
                                    <tr>
                                        <th>기본급</th>
                                        <td>
                                            <fmt:formatNumber value="${slip.baseSalary}" pattern="#,###" />원
                                        </td>
                                    </tr>
                                    <tr>
                                        <th>식대</th>
                                        <td>
                                            <fmt:formatNumber value="${slip.mealAllowance}" pattern="#,###" />원
                                        </td>
                                    </tr>
                                    <tr>
                                        <th>교통비</th>
                                        <td>
                                            <fmt:formatNumber value="${slip.transportAllowance}" pattern="#,###" />원
                                        </td>
                                    </tr>
                                    <tr>
                                        <th>직책수당</th>
                                        <td>
                                            <fmt:formatNumber value="${slip.positionAllowance}" pattern="#,###" />원
                                        </td>
                                    </tr>
                                    <tr>
                                        <th>초과근무수당</th>
                                        <td>
                                            <fmt:formatNumber value="${slip.overtimePay}" pattern="#,###" />원
                                        </td>
                                    </tr>
                                    <tr>
                                        <th>기타수당</th>
                                        <td>
                                            <fmt:formatNumber value="${slip.otherAllowance}" pattern="#,###" />원
                                        </td>
                                    </tr>
                                </tbody>
                                <tfoot>
                                    <tr class="total-row">
                                        <th>지급합계</th>
                                        <td>
                                            <fmt:formatNumber value="${slip.grossSalary}" pattern="#,###" />원
                                        </td>
                                    </tr>
                                </tfoot>
                            </table>
                        </div>

                        <%-- 공제 항목 --%>
                        <div class="slip-section">
                            <div class="slip-section-title deduct">공제 항목</div>
                            <table class="slip-table">
                                <tbody>
                                    <tr>
                                        <th>국민연금</th>
                                        <td>
                                            <fmt:formatNumber value="${slip.nationalPension}" pattern="#,###" />원
                                        </td>
                                    </tr>
                                    <tr>
                                        <th>건강보험</th>
                                        <td>
                                            <fmt:formatNumber value="${slip.healthInsurance}" pattern="#,###" />원
                                        </td>
                                    </tr>
                                    <tr>
                                        <th>장기요양보험</th>
                                        <td>
                                            <fmt:formatNumber value="${slip.longTermCare}" pattern="#,###" />원
                                        </td>
                                    </tr>
                                    <tr>
                                        <th>고용보험</th>
                                        <td>
                                            <fmt:formatNumber value="${slip.employmentInsurance}" pattern="#,###" />원
                                        </td>
                                    </tr>
                                    <tr>
                                        <th>
                                            무급공제
                                            <c:if test="${slip.unpaidLeaveDays > 0}">
                                                <span class="sub-label">
                                                    (<c:out value="${slip.unpaidLeaveDays}" />일)
                                                </span>
                                            </c:if>
                                        </th>
                                        <td>
                                            <fmt:formatNumber value="${slip.unpaidDeduction}" pattern="#,###" />원
                                        </td>
                                    </tr>
                                    <tr>
                                        <th>소득세</th>
                                        <td>
                                            <fmt:formatNumber value="${slip.incomeTax}" pattern="#,###" />원
                                        </td>
                                    </tr>
                                    <tr>
                                        <th>지방소득세</th>
                                        <td>
                                            <fmt:formatNumber value="${slip.localIncomeTax}" pattern="#,###" />원
                                        </td>
                                    </tr>
                                </tbody>
                                <tfoot>
                                    <tr class="total-row">
                                        <th>공제합계</th>
                                        <td>
                                            <fmt:formatNumber value="${slip.totalDeduction}" pattern="#,###" />원
                                        </td>
                                    </tr>
                                </tfoot>
                            </table>
                        </div>

                    </div>

                    <%-- 실수령액 --%>
                    <div class="net-salary-bar">
                        <span class="net-label">실수령액</span>
                        <span class="net-value">
                            <fmt:formatNumber value="${slip.netSalary}" pattern="#,###" />원
                        </span>
                    </div>

                </div>
            </c:if>

        </main>
    </div>

    <script src="${pageContext.request.contextPath}/js/sidebar.js"></script>
</body>
</html>