<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>HR ERP - 비밀번호 초기화</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/sys/password_reset.css">
</head>
<body>

    <jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />

    <div id="main-wrapper">
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />

        <main class="app-content">
            <h1 style="font-size:20px; font-weight:700; margin-bottom:6px;">비밀번호 초기화</h1>
            <p style="font-size:13px; color:var(--gray-500,#6b7280); margin-bottom:24px;">
                직원의 비밀번호를 임시 비밀번호로 초기화합니다.
            </p>

            <div class="pw-reset-wrap">

                <%-- ① 에러 메시지 --%>
                <c:if test="${not empty errorMsg}">
                    <div class="error-box" style="margin-bottom:16px;">
                        ⚠ <c:out value="${errorMsg}" />
                    </div>
                </c:if>

                <%-- ② 초기화 결과 (1회 표시) --%>
                <c:if test="${not empty tempPassword}">
                    <div class="result-box" style="margin-bottom:16px;">
                        <div style="font-size:13px; font-weight:600; margin-bottom:6px;">✅ 비밀번호 초기화 완료</div>
                        <div>임시 비밀번호: <span class="pw-value"><c:out value="${tempPassword}" /></span></div>
                        <div class="pw-hint">직원에게 안전하게 전달하세요. 이 비밀번호는 다시 확인할 수 없습니다.</div>
                    </div>
                </c:if>

                <div class="card" style="padding:20px; display:flex; flex-direction:column; gap:16px;">

                    <%-- ③ 직원 검색 폼 (GET) --%>
                    <form action="${pageContext.request.contextPath}/sys/passwordReset" method="get">
                        <div style="font-size:13px; font-weight:600; margin-bottom:8px;">대상 직원 검색</div>
                        <div style="display:flex; gap:8px;">
                            <input class="form-control"
                                   type="text"
                                   name="keyword"
                                   value="${param.keyword}"
                                   placeholder="이름, 사번, 부서명 입력"
                                   style="flex:1; padding:8px; border:1px solid #ddd; border-radius:4px;">
                            <button type="submit" class="btn btn-secondary">검색</button>
                        </div>
                    </form>

                    <%-- ④ 검색 결과 테이블 --%>
                    <c:if test="${not empty searchResults}">
                        <div>
                            <div style="font-size:12px; font-weight:600; margin-bottom:6px;">
                                검색 결과 (<c:out value="${searchCount}" />명)
                            </div>
                            <div style="overflow-x:auto; border-radius:6px; border:1px solid #eee;">
                                <table class="search-result-table">
                                    <thead>
                                        <tr>
                                            <th>사번</th>
                                            <th>이름</th>
                                            <th>부서</th>
                                            <th>직급</th>
                                            <th>선택</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach var="emp" items="${searchResults}">
                                            <tr>
                                                <td><c:out value="${emp.empId}" /></td>
                                                <td><c:out value="${emp.empName}" /></td>
                                                <td><c:out value="${emp.deptName}" /></td>
                                                <td><c:out value="${emp.posName}" /></td>
                                                <td>
                                                    <%-- 선택 시 empId를 GET 파라미터로 전달해 선택된 직원 카드를 렌더링 --%>
                                                    <a href="${pageContext.request.contextPath}/sys/passwordReset?keyword=${param.keyword}&selectedEmpId=${emp.empId}">
                                                        <button type="button" class="btn-select">선택</button>
                                                    </a>
                                                </td>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </c:if>

                    <%-- 검색어는 있으나 결과 없음 --%>
                    <c:if test="${not empty param.keyword and empty searchResults and empty selectedEmployee}">
                        <div style="padding:12px; background:#f9fafb; border-radius:6px; font-size:13px; color:#6b7280; text-align:center;">
                            검색 결과가 없습니다.
                        </div>
                    </c:if>

                    <%-- ⑤ 선택된 직원 카드 + 초기화 폼 (POST) --%>
                    <c:if test="${not empty selectedEmployee}">
                        <div>
                            <div style="font-size:12px; font-weight:600; margin-bottom:8px;">선택된 직원</div>
                            <div class="selected-emp-card">
                                <div class="emp-avatar">
                                    <%-- 이름 첫 글자: JSTL fn:substring 사용 (taglib 선언 필요) --%>
                                    <c:out value="${fn:substring(selectedEmployee.empName, 0, 1)}" />
                                </div>
                                <div>
                                    <div class="emp-info-name"><c:out value="${selectedEmployee.empName}" /></div>
                                    <div class="emp-info-sub">
                                        <c:out value="${selectedEmployee.empId}" /> ·
                                        <c:out value="${selectedEmployee.deptName}" /> ·
                                        <c:out value="${selectedEmployee.posName}" />
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="warn-box">
                            ⚠ 임시 비밀번호가 자동 생성됩니다. 화면에 1회만 표시되며 DB에는 해시값만 저장됩니다.
                        </div>

                        <%-- 초기화 실행: POST --%>
                        <form action="${pageContext.request.contextPath}/sys/passwordReset" method="post"
                              onsubmit="return confirmReset('<c:out value="${selectedEmployee.empName}" />')">
                            <input type="hidden" name="empId"   value="${selectedEmployee.empId}">
                            <input type="hidden" name="keyword" value="${param.keyword}">

                            <div style="display:flex; justify-content:flex-end; gap:8px; margin-top:4px;">
                                <a href="${pageContext.request.contextPath}/sys/passwordReset">
                                    <button type="button" class="btn btn-secondary">취소</button>
                                </a>
                                <button type="submit" class="btn btn-primary">초기화 실행</button>
                            </div>
                        </form>
                    </c:if>

                </div>
            </div>
        </main>
    </div>

    <script src="${pageContext.request.contextPath}/js/sidebar.js"></script>
<script src="${pageContext.request.contextPath}/js/sys/password_reset.js"></script>
</body>
</html>