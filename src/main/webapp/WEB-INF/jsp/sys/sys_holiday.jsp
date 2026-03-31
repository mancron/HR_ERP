<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c"  uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>HR ERP - 공휴일 관리</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/sys/holiday.css">
</head>
<body>

    <jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />

    <div id="main-wrapper">
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />

        <main class="app-content">
            <h1 class="page-title">공휴일 관리</h1>

            <%-- ① 성공/에러 메시지 (PRG 후 세션 경유) --%>
            <c:if test="${not empty successMsg}">
                <div class="alert alert-success">✅ <c:out value="${successMsg}" /></div>
            </c:if>
            <c:if test="${not empty errorMsg}">
                <div class="alert alert-error">⚠ <c:out value="${errorMsg}" /></div>
            </c:if>

            <%-- ② 연도 조회 폼 (GET) + 공휴일 추가 버튼 --%>
            <div class="toolbar">
                <form action="${pageContext.request.contextPath}/sys/holiday"
                      method="get" class="toolbar-form">
                    <select name="year" class="form-control select-year">
                        <c:forEach begin="2024" end="2027" var="y">
                            <option value="${y}" ${selectedYear == y ? 'selected' : ''}>
                                <c:out value="${y}" />년
                            </option>
                        </c:forEach>
                    </select>
                    <button type="submit" class="btn btn-primary btn-sm">조회</button>
                </form>

                <div class="toolbar-right">
                    <%-- API 자동 적재 버튼 --%>
                    <form action="${pageContext.request.contextPath}/sys/holiday"
                          method="post"
                          onsubmit="return confirm('${selectedYear}년 공휴일을 공공데이터포털 API에서 자동 적재하시겠습니까?\n중복 항목은 자동으로 건너뜁니다.')">
                        <input type="hidden" name="action" value="fetchApi">
                        <input type="hidden" name="year"   value="${selectedYear}">
                        <button type="submit" class="btn btn-secondary btn-sm">
                            📋 API 자동 적재
                        </button>
                    </form>

                    <%-- 공휴일 추가 폼 토글 버튼 --%>
                    <button type="button" class="btn btn-primary btn-sm"
                            onclick="toggleAddForm()">
                        + 공휴일 추가
                    </button>
                </div>
            </div>

            <%-- ③ 공휴일 추가 폼 (기본 hidden, 버튼 클릭 시 표시) --%>
            <div class="add-form-wrap" id="addFormWrap" style="display:none;">
                <form action="${pageContext.request.contextPath}/sys/holiday"
                      method="post" class="add-form">
                    <input type="hidden" name="action" value="add">
                    <input type="hidden" name="year"   value="${selectedYear}">

                    <div class="form-row">
                        <div class="form-group">
                            <label class="form-label">날짜 <span class="required">*</span></label>
                            <input type="date" name="holidayDate" class="form-control"
                                   min="${selectedYear}-01-01"
                                   max="${selectedYear}-12-31"
                                   required>
                        </div>
                        <div class="form-group">
                            <label class="form-label">공휴일명 <span class="required">*</span></label>
                            <input type="text" name="holidayName" class="form-control"
                                   placeholder="예: 대체공휴일" maxlength="50" required>
                        </div>
                        <div class="form-group form-group-btn">
                            <button type="submit" class="btn btn-primary">추가</button>
                            <button type="button" class="btn btn-secondary"
                                    onclick="toggleAddForm()">취소</button>
                        </div>
                    </div>
                </form>
            </div>

            <%-- ④ API 안내 박스 --%>
            <div class="info-box">
                📋 운영 환경 권장: 공공데이터포털 API 배치 자동 적재 (매년 1월)<br>
                API: https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo
            </div>

            <%-- ⑤ 공휴일 목록 테이블 --%>
            <div class="card table-card">
                <div class="table-header">
                    <span class="table-title">
                        <c:out value="${selectedYear}" />년 공휴일
                        <span class="badge-count">총 <c:out value="${holidayList.size()}" />건</span>
                    </span>
                </div>
                <div class="table-wrap">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>날짜</th>
                                <th>요일</th>
                                <th>공휴일명</th>
                                <th>삭제</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:choose>
                                <c:when test="${empty holidayList}">
                                    <tr>
                                        <td colspan="4" class="empty-row">
                                            등록된 공휴일이 없습니다.
                                        </td>
                                    </tr>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach var="holiday" items="${holidayList}">
                                        <tr class="${holiday.dayOfWeek == '토' ? 'row-sat' :
                                                     holiday.dayOfWeek == '일' ? 'row-sun' : ''}">
                                            <td>
                                                <c:out value="${holiday.holidayDateStr}" />
                                            </td>
                                            <td class="day-cell ${holiday.dayOfWeek == '토' ? 'text-blue' :
                                                                   holiday.dayOfWeek == '일' ? 'text-red' : ''}">
                                                <c:out value="${holiday.dayOfWeek}" />
                                            </td>
                                            <td><c:out value="${holiday.holidayName}" /></td>
                                            <td>
                                                <form action="${pageContext.request.contextPath}/sys/holiday"
                                                      method="post"
                                                      onsubmit="return confirm('<c:out value="${holiday.holidayName}" /> 공휴일을 삭제하시겠습니까?')">
                                                    <input type="hidden" name="action"    value="delete">
                                                    <input type="hidden" name="holidayId" value="${holiday.holidayId}">
                                                    <input type="hidden" name="year"      value="${selectedYear}">
                                                    <button type="submit" class="btn btn-danger btn-sm">삭제</button>
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
    <script>
        function toggleAddForm() {
            const wrap = document.getElementById('addFormWrap');
            wrap.style.display = (wrap.style.display === 'none') ? 'block' : 'none';
        }
    </script>
</body>
</html>