<%@ page contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>ERP-HRMS - 출퇴근</title>
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/att/attendance.css">
	<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>

<jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />

<div id="main-wrapper">
	<jsp:include page="/WEB-INF/jsp/common/header.jsp" />

	<main class="app-content">

		<!-- 상단 카드 -->
		<div class="att-card">
			<h3 id="currentDate"></h3>
			<h1 id="currentTime"></h1>

			<p>
				출근 시간: <strong> <c:out
						value="${attendance.checkIn != null ? attendance.checkIn : '-'}" />
				</strong> · 현재 근무시간: <span id="workInfo"></span>
			</p>

			<input type="hidden" id="checkInValue" value="${attendance.checkIn}" />

			<br>

			<!-- 버튼 -->
			<div class="btn-group">

				<form action="${pageContext.request.contextPath}/att/record"
					method="post">
					<input type="hidden" name="action" value="checkin">
					<button class="att-btn att-btn-in"
						<c:if test="${attendance.checkIn != null or isHoliday}">disabled</c:if>>
						출근</button>
				</form>

				<form action="${pageContext.request.contextPath}/att/record"
					method="post">

					<input type="hidden" name="action" value="checkout">

					<button type="button" class="att-btn att-btn-out"
						onclick="confirmCheckout(this.form)"
						<c:if test="${attendance.checkIn == null or attendance.checkOut != null or isHoliday}">
            disabled
        </c:if>>
						퇴근</button>

				</form>

			</div>
		</div>

		<br> <br>

		<!-- 통계 -->
		<div class="summary-container">
			<div class="summary-card">
				출근일수<br>
				<c:out value="${summary.workDays}" />
				일
			</div>
			<div class="summary-card">
				총 근무시간<br>
				<c:out value="${summary.totalHours}" />
				시간
			</div>
			<div class="summary-card">
				출근<br>
				<c:out value="${summary.attendCount}" />
				회
			</div>
			<div class="summary-card">
				지각<br>
				<c:out value="${summary.lateCount}" />
				회
			</div>
			<div class="summary-card">
				결근<br>
				<c:out value="${summary.absentCount}" />
				일
			</div>
			<div class="summary-card">
				미처리<br>
				<c:out value="${summary.noCheckoutCount}" />
				건
			</div>
		</div>
		<form method="get"
			action="${pageContext.request.contextPath}/att/record">
			<input type="month" name="month" value="${month}">
			<button type="submit">조회</button>
		</form>
		<!-- 테이블 -->
		<table class="att-table">
			<tr>
				<th>날짜</th>
				<th>출근</th>
				<th>퇴근</th>
				<th>근무시간</th>
				<th>상태</th>
			</tr>

			<c:forEach var="dto" items="${list}">
				<tr>
					<td><c:out value="${dto.workDate}" /> (<c:out
							value="${dto.dayOfWeek}" />)</td>
					<td><c:out value="${dto.checkIn != null ? dto.checkIn : '-'}" /></td>
					<td><c:out
							value="${dto.checkOut != null ? dto.checkOut : '-'}" /></td>
					<td><c:out value="${dto.workHours}" /></td>

					<td><span
						class="status-badge
        <c:choose>
        	<c:when test='${dto.status == "휴가"}'>status-leave</c:when>
            <c:when test='${dto.status == "결근"}'>status-absent</c:when>
            <c:when test='${dto.status == "지각"}'>status-late</c:when>
            <c:when test='${dto.status == "퇴근미처리"}'>status-no</c:when>
            <c:otherwise>status-normal</c:otherwise>
        </c:choose>
    ">
							<c:out value="${dto.status}" />
					</span></td>
				</tr>
			</c:forEach>

		</table>

	</main>
</div>

<script src="${pageContext.request.contextPath}/js/att/attendance.js"></script>
<script src="${pageContext.request.contextPath}/js/sidebar.js"></script>

<div id="confirmModal" class="modal">
	<div class="modal-content">

		<!-- 메시지 -->
		<p id="confirmMessage" style="font-size: 16px; margin-bottom: 20px;"></p>

		<!-- 버튼 영역 -->
		<div style="display: flex; justify-content: flex-end; gap: 10px;">

			<button id="confirmYes" class="modal-btn modal-btn-ok">확인</button>
			<button type="button" onclick="closeConfirmModal()"
				class="modal-btn modal-btn-cancel">취소</button>

		</div>

	</div>
</div>