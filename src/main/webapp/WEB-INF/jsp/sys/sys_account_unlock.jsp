<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ERP-HRMS - 계정 잠금 해제</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/sys/account_unlock.css">
</head>
<body>

    <jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />

    <div id="main-wrapper">
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />

        <main class="app-content">
            <h1 class="page-title">계정 잠금 해제</h1>
            <p class="page-desc">로그인 5회 연속 실패로 잠긴 계정을 해제합니다.</p>

            <%-- 성공/에러 메시지 --%>
            <c:if test="${not empty successMsg}">
                <div class="alert alert-success">✅ <c:out value="${successMsg}" /></div>
            </c:if>
            <c:if test="${not empty errorMsg}">
                <div class="alert alert-error">⚠ <c:out value="${errorMsg}" /></div>
            </c:if>

            <div class="card table-card">
                <div class="table-header">
                    <span class="table-title">
                        잠금 계정 목록
                        <span class="badge-count">
                            총 <c:out value="${lockedCount}" />건
                        </span>
                    </span>
                </div>
                <div class="table-wrap">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>사번</th>
                                <th>이름</th>
                                <th>부서</th>
                                <th>잠금 일시</th>
                                <th>실패 횟수</th>
                                <th>처리</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:choose>
                                <c:when test="${empty lockedList}">
                                    <tr>
                                        <td colspan="6" class="empty-row">
                                            잠금된 계정이 없습니다.
                                        </td>
                                    </tr>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach var="account" items="${lockedList}">
                                        <tr class="locked-row">
                                            <td><c:out value="${account.empNo}" /></td>
                                            <td><strong><c:out value="${account.empName}" /></strong></td>
                                            <td><c:out value="${account.deptName}" /></td>
                                            <td><c:out value="${account.lockedAtStr}" /></td>
                                            <td>
                                                <span class="attempts-badge">
                                                    <c:out value="${account.loginAttempts}" />회
                                                </span>
                                            </td>
                                            <td>
												<form action="${pageContext.request.contextPath}/sys/accountUnlock"
												      method="post"
												      onsubmit="return confirm('<c:out value="${account.empName}" /> 직원의 계정 잠금을 해제하시겠습니까?')">
												    <input type="hidden" name="accountId"     value="${account.accountId}">
												    <input type="hidden" name="loginAttempts" value="${account.loginAttempts}"> <%-- 추가 --%>
												    <button type="submit" class="btn btn-success btn-sm">잠금 해제</button>
												</form>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                        </tbody>
                    </table>
                </div>
            </div>

        </main>
    </div>

    <script src="${pageContext.request.contextPath}/js/sidebar.js"></script>
</body>
</html>