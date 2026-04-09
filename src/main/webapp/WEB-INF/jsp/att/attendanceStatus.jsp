<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>HR ERP - 근태 현황</title>

<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/att/attendanceStatus.css">
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/style.css">
</head>

<jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />

<div id="main-wrapper">

	<jsp:include page="/WEB-INF/jsp/common/header.jsp" />

	<main class="app-content">

		<!-- 🔥 카드 영역 -->
		<div class="att-card">

			<h3>
				근태 현황 관리 (${year}년 ${month}월)
				<c:if test="${isClosed}">
					<span style="color: red; font-size: 14px;">[마감됨]</span>
				</c:if>
			</h3>

			<form method="get"
				action="${pageContext.request.contextPath}/att/status">

				<div class="filter-box">

					<input type="month" name="month" value="${param.month}"> <select
						name="dept">
						<option value="">전체 부서</option>
						<c:forEach var="d" items="${deptList}">
							<option value="${d}" ${param.dept == d ? 'selected' : ''}>
								${d}</option>
						</c:forEach>
					</select>

					<button type="submit">조회</button>
					<button type="button" onclick="closeMonthFromInput()">마감</button>

				</div>

			</form>

			<!-- ========================= -->
			<!-- 📊 테이블 -->
			<!-- ========================= -->
			<div class="table-wrapper">

				<table class="attendance-table">

					<thead>
						<tr>
							<th>이름</th>
							<th>부서</th>
							<th>직급</th>
							<th>출근</th>
							<th>지각</th>
							<th>결근</th>
							<th>휴가</th>
							<th>퇴근 미처리</th>
							<th>관리</th>
						</tr>
					</thead>

					<tbody>
						<c:forEach var="item" items="${list}">
							<tr>

								<td>${item.empName}</td>
								<td>${item.deptName}</td>
								<td>${item.position}</td>

								<td>${item.workDays}</td>
								<td>${item.lateCount}</td>
								<td class="absent">${item.absentCount}</td>
								<td>${item.leaveDays}</td>
								<td>${item.noCheckoutCount}</td>

								<td><c:choose>
										<c:when test="${isClosed}">
											<button class="btn-fix" disabled
												style="background: #ccc; cursor: not-allowed;">마감됨
											</button>
										</c:when>
										<c:otherwise>
											<button class="btn-fix" onclick="openFixModal(${item.empId})">
												근태 보정</button>
										</c:otherwise>
									</c:choose></td>

							</tr>
						</c:forEach>
					</tbody>

				</table>

			</div>

		</div>

	</main>
</div>

<div id="fixModal" class="modal">
	<div class="modal-content">

		<h3>근태 보정</h3>
		<div style="margin-bottom: 10px;">
			<input type="checkbox" id="checkAllAbsent"> <label
				for="checkAllAbsent">결근 후보 전체 선택</label>
		</div>
		<form id="fixForm">

			<input type="hidden" name="empId" id="fixEmpId">

			<div id="issueList" class="issue-list">
				<!-- JS로 채워짐 -->
			</div>

			<div class="modal-buttons">
				<button class="btn-absent" onclick="submitFix('ABSENT')">결근
					처리</button>
				<button class="btn-checkin" onclick="submitFix('CHECKIN_FIX')">출근
					보정</button>
				<button class="btn-checkout" onclick="submitFix('CHECKOUT_FIX')">퇴근
					보정</button>
				<button type="button" class="btn-cancel" onclick="closeModal()">취소</button>
			</div>

		</form>

	</div>
</div>

<script
	src="${pageContext.request.contextPath}/js/att/attendanceStatus.js"></script>
<script src="${pageContext.request.contextPath}/js/sidebar.js"></script>