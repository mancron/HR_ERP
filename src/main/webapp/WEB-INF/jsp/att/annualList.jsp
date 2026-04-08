<%@ page contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>HR ERP - 연차 현황</title>
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/att/annual.css">

</head>

<jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />

<div id="main-wrapper">
	<jsp:include page="/WEB-INF/jsp/common/header.jsp" />

	<main class="app-content">

		<h2>연차 현황</h2>

		<!-- 🔍 필터 -->
		<form method="get"
			action="${pageContext.request.contextPath}/att/annual"
			class="filter-box">

			<!-- 연도 -->
			<select name="year">
				<c:forEach var="y" items="${yearList}">
					<option value="${y}" ${y == year ? 'selected' : ''}>${y}년
					</option>
				</c:forEach>
			</select>

			<!-- 부서 -->
			<select name="dept">
				<option value="">전체 직원</option>
				<c:forEach var="d" items="${deptList}">
					<option value="${d}" ${d == dept ? 'selected' : ''}>${d}</option>
				</c:forEach>
			</select>

			<!-- 이름 검색 -->
			<input type="text" name="name" value="${name}" placeholder="이름 검색">

			<button type="submit">조회</button>
		</form>

		<!-- 📊 테이블 -->
		<table class="annual-table">
			<thead>
				<tr>
					<th>이름</th>
					<th>부서</th>
					<th>부여</th>
					<th>사용</th>
					<th>잔여</th>
					<th>사용률</th>
					<th>조정</th>
				</tr>
			</thead>

			<tbody>
				<c:forEach var="item" items="${list}">
					<tr>
						<td>${item.empName}</td>
						<td>${item.deptName}</td>
						<td>${item.totalDays}</td>
						<td>${item.usedDays}</td>

						<!-- 잔여 강조 -->
						<td style="color:${item.remainDays <= 7 ? 'orange' : 'black'}">
							${item.remainDays}</td>

						<!-- 사용률 -->
						<td><c:set var="rate" value="${item.usageRate}" />

							<div class="progress">
								<div class="bar ${rate >= 60 ? 'bar-orange' : 'bar-blue'}"
									style="width:${rate}%"></div>
							</div> ${fn:substringBefore(rate, '.')}%</td>

						<td>
							<button type="button"
								onclick="openAdjustModal(${item.empId}, ${item.totalDays})">
								조정</button>
						</td>
					</tr>
				</c:forEach>

			</tbody>
		</table>

	</main>
</div>

<!-- 🔧 조정 모달 -->
<div id="adjustModal" class="modal" style="display: none;">
	<div class="modal-content">

		<h3>연차 조정</h3>

		<!-- 현재 연차 -->
		<div>
			현재 연차: <span id="currentTotal"></span>일
		</div>

		<form method="post"
			action="${pageContext.request.contextPath}/att/annual/adjust">

			<!-- 사번 -->
			<input type="hidden" name="empId" id="empId">

			<!-- 연차 입력 -->
			<div>
				변경 연차: <input type="number" step="0.5" name="totalDays" required>
			</div>

			<!-- 변경량 표시 -->
			<div id="diff" style="margin-top: 5px; font-weight: bold;"></div>

			<br>

			<button type="submit">저장</button>
			<button type="button" onclick="closeModal()">취소</button>

		</form>

	</div>
</div>

<script src="${pageContext.request.contextPath}/js/sidebar.js"></script>
<script src="${pageContext.request.contextPath}/js/att/annual.js"></script>