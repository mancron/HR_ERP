<%@ page contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>HR ERP - 초과근무 신청</title>

<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/att/overtime.css">
</head>

<!-- 사이드바 -->
<jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />

<div id="main-wrapper">

	<!-- 헤더 -->
	<jsp:include page="/WEB-INF/jsp/common/header.jsp" />

	<main class="app-content">

		<!-- 카드 형태 -->
		<div class="att-card">
			<h3>초과근무 신청</h3>

			<form action="${pageContext.request.contextPath}/att/overtime/req" method="post">

				<table class="form-table">

					<tr>
						<th>근무 날짜</th>
						<td>
							<input type="date" name="otDate" required>
						</td>
					</tr>

					<tr>
						<th>시작 시간</th>
						<td>
							<input type="time" name="startTime" required>
						</td>
					</tr>

					<tr>
						<th>종료 시간</th>
						<td>
							<input type="time" name="endTime" required>
						</td>
					</tr>

					<tr>
						<th>사유</th>
						<td>
							<textarea name="reason" rows="4" cols="50" required></textarea>
						</td>
					</tr>

				</table>

				<br>

				<div class="btn-group">
					<button type="submit" class="att-btn att-btn-in">신청</button>
					<button type="button" class="att-btn att-btn-out"
						onclick="history.back()">취소</button>
				</div>

			</form>
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

<script src="${pageContext.request.contextPath}/js/sidebar.js"></script>