<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>HR ERP - AI 데이터 조회</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/sys/sql_query.css">
</head>
<body>

    <jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />

    <div id="main-wrapper">
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />

        <main class="app-content">
            <h1 class="page-title">AI 데이터 조회</h1>
            <p class="page-desc">자연어로 질문하면 AI가 SQL을 생성하고 결과를 표로 보여줍니다.</p>

            <%-- 질문 입력 폼 --%>
            <div class="query-card">
                <form action="${pageContext.request.contextPath}/sys/sqlQuery"
                      method="post" id="queryForm">
                    <div class="input-row">
                        <input type="text"
                               name="question"
                               class="question-input"
                               placeholder="예: 인사팀 직원 목록을 보여줘 / 이번달 급여 현황은?"
                               value="<c:out value="${question}" />"
                               autocomplete="off"
                               required>
                        <button type="submit" class="btn btn-primary" id="submitBtn">
                            🔍 조회
                        </button>
                    </div>
                </form>

                <%-- 예시 질문 버튼 --%>
                <div class="example-wrap">
                    <span class="example-label">예시:</span>
                    <button type="button" class="example-btn" onclick="setQuestion('재직 중인 직원 목록을 보여줘')">재직 직원 목록</button>
                    <button type="button" class="example-btn" onclick="setQuestion('부서별 직원 수를 보여줘')">부서별 직원 수</button>
                    <button type="button" class="example-btn" onclick="setQuestion('잠긴 계정 목록을 보여줘')">잠긴 계정 목록</button>
                    <button type="button" class="example-btn" onclick="setQuestion('2025년 공휴일 목록을 보여줘')">2025년 공휴일</button>
                </div>
            </div>

            <%-- 결과 영역 --%>
            <c:if test="${not empty result}">

                <%-- 생성된 SQL 표시 --%>
                <div class="sql-box">
                    <span class="sql-label">생성된 SQL</span>
                    <code class="sql-code"><c:out value="${result.generatedSql}" /></code>
                </div>

                <%-- 에러 메시지 --%>
                <c:if test="${not empty result.errorMsg}">
                    <div class="alert alert-error">⚠ <c:out value="${result.errorMsg}" /></div>
                </c:if>

                <%-- 결과 테이블 --%>
                <c:if test="${empty result.errorMsg and not empty result.columns}">
                    <div class="result-header">
                        조회 결과
                        <span class="result-count">총 <c:out value="${result.rowCount}" />건</span>
                    </div>

                    <div class="table-wrap">
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <c:forEach var="col" items="${result.columns}">
                                        <th><c:out value="${col}" /></th>
                                    </c:forEach>
                                </tr>
                            </thead>
                            <tbody>
                                <c:choose>
                                    <c:when test="${empty result.rows}">
                                        <tr>
                                            <td colspan="${fn:length(result.columns)}" class="empty-row">
                                                조회된 데이터가 없습니다.
                                            </td>
                                        </tr>
                                    </c:when>
                                    <c:otherwise>
                                        <c:forEach var="row" items="${result.rows}">
                                            <tr>
                                                <c:forEach var="col" items="${result.columns}">
                                                    <td><c:out value="${row[col]}" /></td>
                                                </c:forEach>
                                            </tr>
                                        </c:forEach>
                                    </c:otherwise>
                                </c:choose>
                            </tbody>
                        </table>
                    </div>
                </c:if>

            </c:if>

        </main>
    </div>

    <script src="${pageContext.request.contextPath}/js/sidebar.js"></script>
    <script>
        function setQuestion(q) {
            document.querySelector('input[name="question"]').value = q;
        }

        // 조회 중 로딩 표시
        document.getElementById('queryForm').addEventListener('submit', function() {
            const btn = document.getElementById('submitBtn');
            btn.textContent = '⏳ AI 생성 중...';
            btn.disabled = true;
        });
    </script>
</body>
</html>