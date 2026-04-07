<%@ page contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>HR ERP - 초과근무 신청</title>

<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/att/common/requestList.css">
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/att/common/requestDetail.css">
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/att/overtime.css">
	<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>

<!-- 사이드바 -->
<jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />

<div id="main-wrapper">

	<!-- 헤더 -->
	<jsp:include page="/WEB-INF/jsp/common/header.jsp" />

	<main class="app-content">
		<h2>초과근무 신청</h2>
		<div class="overtime-container">

			<!-- 🔥 좌측: 신청 -->
			<div class="overtime-left">

				<div class="att-card">


					<form action="${pageContext.request.contextPath}/att/overtime/req"
						method="post">

						<table class="form-table">

							<tr>
								<th>근무 날짜</th>
								<td><input type="date" name="otDate" required></td>
							</tr>

							<tr>
								<th>시작 시간</th>
								<td><input type="time" name="startTime" required></td>
							</tr>

							<tr>
								<th>종료 시간</th>
								<td><input type="time" name="endTime" required></td>
							</tr>

							<tr>
								<th>사유</th>
								<td><textarea name="reason" rows="4" cols="50" required></textarea>
								</td>
							</tr>

						</table>

						<br>

						<div class="btn-group">
							<button type="submit" class="att-btn att-btn-in">신청</button>
						</div>

					</form>
				</div>

			</div>

			<!-- 🔥 우측: 리스트 -->
			<div class="overtime-right">
				<form method="get"
					action="${pageContext.request.contextPath}/att/overtime/req"
					style="margin-bottom: 10px;">
					<input type="month" name="month" value="<c:out value='${month}'/>">
					<button type="submit">조회</button>
				</form>
				<div class="att-card">
					<h3>신청 내역</h3>

					<!-- 공통 리스트 -->
					<jsp:include page="/WEB-INF/jsp/att/common/requestList.jsp" />
				</div>

			</div>

		</div>

	</main>
</div>

<!-- JS -->
<script>
	document.querySelector("form").addEventListener("submit", function(e) {

		const start = document.querySelector("input[name='startTime']").value;
		const end = document.querySelector("input[name='endTime']").value;

		if (start >= end) {
			alert("종료시간이 시작시간보다 늦어야 합니다.");
			e.preventDefault();
		}
	});
</script>
<script>
	const contextPath = "${pageContext.request.contextPath}";
	const currentPath = "/att/overtime";
</script>

<script
	src="${pageContext.request.contextPath}/js/att/common/requestDetail.js"></script>
<script src="${pageContext.request.contextPath}/js/sidebar.js"></script>

<jsp:include page="/WEB-INF/jsp/att/common/requestDetail.jsp" />