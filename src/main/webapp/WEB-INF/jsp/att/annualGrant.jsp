<%@ page contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>HR ERP - 연차 일괄 부여</title>

<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/style.css">
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/att/annualGrant.css">
</head>

<jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />

<div id="main-wrapper">
	<jsp:include page="/WEB-INF/jsp/common/header.jsp" />

	<div class="container">

		<h2 class="page-title">연차 일괄 부여</h2>

		<!-- 안내 박스 -->
		<div class="card info-box">
			<div class="card info-box modern-card">
				<h3 class="card-title">연차 부여 기준</h3>

				<div class="rule-grid">

					<div class="rule-item">
						<div class="rule-icon">📅</div>
						<div>
							<div class="rule-title">1년 미만</div>
							<div class="rule-desc">매월 개근 시 1일 (최대 11일)</div>
						</div>
					</div>

					<div class="rule-item">
						<div class="rule-icon">🟢</div>
						<div>
							<div class="rule-title">1년 이상</div>
							<div class="rule-desc">기본 15일</div>
						</div>
					</div>

					<div class="rule-item">
						<div class="rule-icon">📈</div>
						<div>
							<div class="rule-title">3년 이상</div>
							<div class="rule-desc">2년마다 1일 추가</div>
						</div>
					</div>

					<div class="rule-item highlight">
						<div class="rule-icon">⭐</div>
						<div>
							<div class="rule-title">최대 연차</div>
							<div class="rule-desc">25일</div>
						</div>
					</div>

				</div>
			</div>
		</div>

		<!-- 🔥 부서 선택 -->
		<form method="get"
			action="${pageContext.request.contextPath}/att/annual/grant"
			class="filter-box">
			<select name="deptId">
				<option value="0">전체 부서</option>

				<c:forEach var="dept" items="${deptList}" varStatus="status">
					<option value="${status.index + 1}"
						${param.deptId == (status.index + 1) ? 'selected' : ''}>
						${dept}</option>
				</c:forEach>
			</select>

			<button type="submit" class="btn">조회</button>
		</form>
		<!-- 🔥 좌우 분할 -->
		<div class="grant-wrapper">

			<!-- ✅ 미부여 -->
			<div class="grant-box">
				<h3>미부여 직원</h3>

				<table class="table">
					<tr>
						<th>이름</th>
						<th>부서</th>
						<th>직책</th>
						<th>입사일</th>
						<th>근속</th>
						<th>부여 예정</th>
					</tr>

					<c:forEach var="emp" items="${notGranted}">
						<tr>
							<td>${emp.empName}</td>
							<td>${emp.deptName}</td>
							<td>${emp.positionName}</td>
							<td>${emp.hireDate}</td>
							<td>${emp.years}년차</td>
							<td>${emp.annualDays}일</td>
						</tr>
					</c:forEach>

					<c:if test="${empty notGranted}">
						<tr>
							<td colspan="6">미부여 직원이 없습니다.</td>
						</tr>
					</c:if>
				</table>
			</div>


			<!-- ✅ 부여 완료 -->
			<div class="grant-box">
				<h3>부여 완료 직원</h3>

				<table class="table">
					<tr>
						<th>이름</th>
						<th>부서</th>
						<th>직책</th>
						<th>입사일</th>
						<th>근속</th>
						<th>부여됨</th>
					</tr>

					<c:forEach var="emp" items="${granted}">
						<tr>
							<td>${emp.empName}</td>
							<td>${emp.deptName}</td>
							<td>${emp.positionName}</td>
							<td>${emp.hireDate}</td>
							<td>${emp.years}년차</td>
							<td>${emp.annualDays}일</td>
						</tr>
					</c:forEach>

					<c:if test="${empty granted}">
						<tr>
							<td colspan="6">부여된 직원이 없습니다.</td>
						</tr>
					</c:if>
				</table>
			</div>

		</div>


		<!-- 🔥 일괄 실행 -->
		<div class="card action-box">
			<h3>일괄 부여 실행</h3>

			<form method="post"
				action="${pageContext.request.contextPath}/att/annual/grant">
				<input type="hidden" name="deptId" value="${param.deptId}">
				<button type="submit" class="btn btn-primary"
					onclick="return confirm('미부여 직원에게 연차를 부여하시겠습니까?');">연차 일괄
					부여 실행</button>
			</form>

			<p class="warning-text">※ 이미 부여된 직원은 제외됩니다.</p>
		</div>


		<!-- 결과 메시지 -->
		<c:if test="${not empty param.success}">
			<div class="alert success">연차 일괄 부여가 완료되었습니다.</div>
		</c:if>

	</div>
</div>

<script
	src="<c:out value='${pageContext.request.contextPath}/js/sidebar.js'/>"></script>