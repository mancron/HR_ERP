<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>HR ERP - 급여 현황</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/sal/salary_status.css">
</head>
<body>
    <jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />
    <div id="main-wrapper">
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
        <main class="app-content">
            <h1 class="page-title">급여 현황</h1>

            <%-- 검색 필터 --%>
            <form action="${pageContext.request.contextPath}/sal/status"
                  method="get" class="search-bar">

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

                <%-- 부서 선택 --%>
                <select name="deptId" class="form-control select-sm">
                    <option value="0" ${selectedDeptId == 0 ? 'selected' : ''}>전체 부서</option>
                    <c:forEach var="dept" items="${deptList}">
                        <option value="${dept[0]}"
                                ${selectedDeptId == dept[0] ? 'selected' : ''}>
                            <c:out value="${dept[1]}" />
                        </option>
                    </c:forEach>
                </select>

                <button type="submit" class="btn btn-primary btn-sm">조회</button>
            </form>

            <%-- 요약 위젯 --%>
            <div class="stat-grid">
                <div class="stat-card blue">
                    <div class="stat-label">총 지급합계</div>
                    <div class="stat-value">
                        <fmt:formatNumber value="${totalGross}" pattern="#,###" />
                    </div>
                    <div class="stat-sub"><c:out value="${empCount}" />명 기준</div>
                </div>
                <div class="stat-card green">
                    <div class="stat-label">총 실수령액</div>
                    <div class="stat-value">
                        <fmt:formatNumber value="${totalNet}" pattern="#,###" />
                    </div>
                    <div class="stat-sub">공제 후 합계</div>
                </div>
                <div class="stat-card warn">
                    <div class="stat-label">1인 평균 실수령액</div>
                    <div class="stat-value">
                        <fmt:formatNumber value="${avgNet}" pattern="#,###" />
                    </div>
                    <div class="stat-sub">재직자 기준</div>
                </div>
            </div>

            <%-- 목록 테이블 --%>
            <div class="table-card">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>이름</th>
                            <th>부서</th>
                            <th>직급</th>
                            <th>지급합계</th>
                            <th>공제합계</th>
                            <th>실수령액</th>
                            <th>지급일</th>
                            <th>상태</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${empty salaryList}">
                                <tr>
                                    <td colspan="8" class="empty-row">
                                        조회된 급여 데이터가 없습니다.
                                    </td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="sal" items="${salaryList}">
                                    <tr>
                                        <td><strong><c:out value="${sal.empName}" /></strong></td>
                                        <td><c:out value="${sal.deptName}" /></td>
                                        <td><c:out value="${sal.positionName}" /></td>
                                        <td>
                                            <fmt:formatNumber value="${sal.grossSalary}" pattern="#,###" />
                                        </td>
                                        <td>
                                            <fmt:formatNumber value="${sal.totalDeduction}" pattern="#,###" />
                                        </td>
                                        <td class="net-salary">
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