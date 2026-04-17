<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>ERP-HRMS - 급여 현황</title>
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
                        <th></th><%-- 토글 버튼 열 --%>
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
                                <td colspan="9" class="empty-row">
                                    조회된 급여 데이터가 없습니다.
                                </td>
                            </tr>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="sal" items="${salaryList}" varStatus="vs">

                                <%-- 메인 행 --%>
                                <tr class="main-row"
                                    onclick="toggleDetail('detail-${vs.index}', this)">
                                    <td class="toggle-cell">
                                        <span class="toggle-icon">▶</span>
                                    </td>
                                    <td><strong><c:out value="${sal.empName}" /></strong></td>
                                    <td><c:out value="${sal.deptName}" /></td>
                                    <td><c:out value="${sal.positionName}" /></td>
                                    <td>
                                        <fmt:formatNumber value="${sal.grossSalary}" pattern="#,###" />
                                    </td>
                                    <td class="deduction-val">
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

                                <%-- 공제 상세 행 (기본 숨김) --%>
                                <tr id="detail-${vs.index}" class="detail-row hidden">
                                    <td colspan="9">
                                        <div class="detail-grid">

                                            <%-- 지급 항목 --%>
                                            <div class="detail-section">
                                                <div class="detail-section-title pay-title">지급 항목</div>
                                                <div class="detail-item">
                                                    <span class="detail-label">기본급</span>
                                                    <span class="detail-value">
                                                        <fmt:formatNumber value="${sal.baseSalary}" pattern="#,###" />원
                                                    </span>
                                                </div>
                                                <div class="detail-item">
                                                    <span class="detail-label">식대</span>
                                                    <span class="detail-value">
                                                        <fmt:formatNumber value="${sal.mealAllowance}" pattern="#,###" />원
                                                    </span>
                                                </div>
                                                <div class="detail-item">
                                                    <span class="detail-label">교통비</span>
                                                    <span class="detail-value">
                                                        <fmt:formatNumber value="${sal.transportAllowance}" pattern="#,###" />원
                                                    </span>
                                                </div>
                                                <div class="detail-item">
                                                    <span class="detail-label">직책수당</span>
                                                    <span class="detail-value">
                                                        <fmt:formatNumber value="${sal.positionAllowance}" pattern="#,###" />원
                                                    </span>
                                                </div>
                                                <div class="detail-item">
                                                    <span class="detail-label">초과근무수당</span>
                                                    <span class="detail-value">
                                                        <fmt:formatNumber value="${sal.overtimePay}" pattern="#,###" />원
                                                    </span>
                                                </div>
                                                <div class="detail-item">
                                                    <span class="detail-label">기타수당</span>
                                                    <span class="detail-value">
                                                        <fmt:formatNumber value="${sal.otherAllowance}" pattern="#,###" />원
                                                    </span>
                                                </div>
                                                <div class="detail-item detail-subtotal">
                                                    <span class="detail-label">지급합계</span>
                                                    <span class="detail-value pay-total">
                                                        <fmt:formatNumber value="${sal.grossSalary}" pattern="#,###" />원
                                                    </span>
                                                </div>
                                            </div>

                                            <%-- 공제 항목 --%>
                                            <div class="detail-section">
                                                <div class="detail-section-title ded-title">공제 항목</div>
                                                <div class="detail-item">
                                                    <span class="detail-label">국민연금</span>
                                                    <span class="detail-value">
                                                        <fmt:formatNumber value="${sal.nationalPension}" pattern="#,###" />원
                                                    </span>
                                                </div>
                                                <div class="detail-item">
                                                    <span class="detail-label">건강보험</span>
                                                    <span class="detail-value">
                                                        <fmt:formatNumber value="${sal.healthInsurance}" pattern="#,###" />원
                                                    </span>
                                                </div>
                                                <div class="detail-item">
                                                    <span class="detail-label">장기요양보험</span>
                                                    <span class="detail-value">
                                                        <fmt:formatNumber value="${sal.longTermCare}" pattern="#,###" />원
                                                    </span>
                                                </div>
                                                <div class="detail-item">
                                                    <span class="detail-label">고용보험</span>
                                                    <span class="detail-value">
                                                        <fmt:formatNumber value="${sal.employmentInsurance}" pattern="#,###" />원
                                                    </span>
                                                </div>
                                                <div class="detail-item">
                                                    <span class="detail-label">
                                                        무급공제
                                                        <span class="detail-days">
                                                            (<c:out value="${sal.unpaidLeaveDays}" />일)
                                                        </span>
                                                    </span>
                                                    <span class="detail-value">
                                                        <fmt:formatNumber value="${sal.unpaidDeduction}" pattern="#,###" />원
                                                    </span>
                                                </div>
                                                <div class="detail-item">
                                                    <span class="detail-label">소득세</span>
                                                    <span class="detail-value">
                                                        <fmt:formatNumber value="${sal.incomeTax}" pattern="#,###" />원
                                                    </span>
                                                </div>
                                                <div class="detail-item">
                                                    <span class="detail-label">지방소득세</span>
                                                    <span class="detail-value">
                                                        <fmt:formatNumber value="${sal.localIncomeTax}" pattern="#,###" />원
                                                    </span>
                                                </div>
                                                <div class="detail-item detail-subtotal">
                                                    <span class="detail-label">공제합계</span>
                                                    <span class="detail-value ded-total">
                                                        <fmt:formatNumber value="${sal.totalDeduction}" pattern="#,###" />원
                                                    </span>
                                                </div>
                                            </div>

                                            <%-- 실수령액 요약 --%>
                                            <div class="detail-net">
                                                <div class="detail-net-label">실수령액</div>
                                                <div class="detail-net-value">
                                                    <fmt:formatNumber value="${sal.netSalary}" pattern="#,###" />원
                                                </div>
                                            </div>

                                        </div>
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
<script>
    function toggleDetail(id, row) {
        const detailRow = document.getElementById(id);
        const icon = row.querySelector('.toggle-icon');
        if (detailRow.classList.contains('hidden')) {
            detailRow.classList.remove('hidden');
            icon.textContent = '▼';
            row.classList.add('active-row');
        } else {
            detailRow.classList.add('hidden');
            icon.textContent = '▶';
            row.classList.remove('active-row');
        }
    }
</script>
</body>
</html>