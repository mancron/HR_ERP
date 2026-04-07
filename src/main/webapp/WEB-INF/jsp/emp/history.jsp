<%@ page contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>HR ERP - 인사발령 이력</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/emp/history.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
<script src="${pageContext.request.contextPath}/js/sidebar.js"></script>
</head>
<body data-context-path="${pageContext.request.contextPath}">

<jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />

<div id="main-wrapper">
    <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
    <main class="app-content">

        <h1>인사발령 이력</h1><br>

        <%-- 검색 폼 --%>
        <form action="${pageContext.request.contextPath}/emp/history" method="get">
            <div class="search-bar">
                <select name="changeType">
                    <option value=""     <c:if test="${empty changeType}">selected</c:if>>유형 전체</option>
                    <option value="발령" <c:if test="${changeType == '발령'}">selected</c:if>>발령</option>
                    <option value="승진" <c:if test="${changeType == '승진'}">selected</c:if>>승진</option>
                    <option value="전보" <c:if test="${changeType == '전보'}">selected</c:if>>전보</option>
                    <option value="휴직" <c:if test="${changeType == '휴직'}">selected</c:if>>휴직</option>
					<option value="복직" <c:if test="${changeType == '복직'}">selected</c:if>>복직</option>
					<option value="퇴직" <c:if test="${changeType == '퇴직'}">selected</c:if>>퇴직</option>
                </select>
                <input type="text" name="keyword" value="${keyword}" placeholder="직원 이름 검색">
                <input type="month" name="yearMonth" value="${yearMonth}" placeholder="연월 선택">
                <button type="submit">검색</button>
            </div>
        </form>

        <%-- 이력 테이블 --%>
        <div class="card">
            <table>
                <thead>
                    <tr>
                        <th>직원명</th>
                        <th>사번</th>
                        <th>발령 유형</th>
                        <th>부서 (전 → 후)</th>
						<th>직급 (전 → 후)</th>
						<th>직책 (전 → 후)</th>
                        <th>발령 적용일</th>
                        <th>사유</th>
                        <th>승인자</th>
                    </tr>
                </thead>
                <tbody>
                    <c:choose>
                        <c:when test="${empty historyList}">
                            <tr>
                                <td colspan="9">인사발령 이력이 없습니다.</td>
                            </tr>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="item" items="${historyList}">
                            <tr>
                                <td><strong>${item.emp_name}</strong></td>
                                <td>${item.emp_no}</td>
                                <td><span class="type-badge type-${item.change_type}">${item.change_type}</span></td>
                                <td>
								    <c:choose>
								        <c:when test="${item.from_dept_name == item.to_dept_name}">
								            ${item.to_dept_name}
								        </c:when>
								        <c:otherwise>
								            ${item.from_dept_name} → ${item.to_dept_name}
								        </c:otherwise>
								    </c:choose>
								</td>
                                <td>
								    <c:choose>
								        <c:when test="${item.from_position_name == item.to_position_name}">
								            ${item.to_position_name}
								        </c:when>
								        <c:otherwise>
								            ${item.from_position_name} → ${item.to_position_name}
								        </c:otherwise>
								    </c:choose>
								</td>
                                <td>
								    <c:choose>
								        <c:when test="${item.from_role == item.to_role}">
								            ${item.to_role}
								        </c:when>
								        <c:otherwise>
								            ${item.from_role} → ${item.to_role}
								        </c:otherwise>
								    </c:choose>
								</td>
                                <td>${item.change_date.toString().substring(0, 10)}</td>
                                <td class="td-reason">
                                    <c:choose>
                                        <c:when test="${empty item.reason}">-</c:when>
                                        <c:otherwise>${item.reason}</c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${empty item.approved_by_name}">-</c:when>
                                        <c:otherwise>${item.approved_by_name}</c:otherwise>
                                    </c:choose>
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

</body>
</html>