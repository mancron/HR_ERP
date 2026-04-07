<%@ page contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>HR ERP - 연차 현황</title>
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/att/attendance.css">
<style>
.annual-table {
	width: 100%;
	border-collapse: collapse;
	margin-top: 20px;
}

.annual-table th, .annual-table td {
	padding: 12px;
	text-align: center;
	border-bottom: 1px solid #eee;
}

.progress {
	width: 100px;
	height: 8px;
	background: #eee;
	border-radius: 5px;
	margin: 0 auto;
}

.bar {
	height: 100%;
	border-radius: 5px;
}

.bar-blue {
	background: #3b82f6;
}

.bar-orange {
	background: #f59e0b;
}

.annual-btn {
	padding: 5px 10px;
	border: 1px solid #ccc;
	background: white;
	cursor: pointer;
	border-radius: 5px;
}

.filter-box {
	display: flex;
	gap: 10px;
	align-items: center;
}
</style>
</head>

<jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />

<div id="main-wrapper">
	<jsp:include page="/WEB-INF/jsp/common/header.jsp" />

	<main class="app-content">

		<h2>연차 현황</h2>

		<!-- 🔍 필터 -->
		<form method="get" action="${pageContext.request.contextPath}/att/annual" class="filter-box">

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
							<button class="annual-btn"
								onclick="openAdjustModal(${item.empId})">조정</button>
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

		<form method="post"
			action="${pageContext.request.contextPath}/annual/adjust">

			<input type="hidden" name="empId" id="empId">

			<div>
				부여 연차: <input type="number" step="0.5" name="totalDays">
			</div>

			<div>
				사용 연차: <input type="number" step="0.5" name="usedDays">
			</div>

			<br>

			<button type="submit">저장</button>
			<button type="button" onclick="closeModal()">취소</button>

		</form>

	</div>
</div>

<script>
function openAdjustModal(empId) {
	document.getElementById("empId").value = empId;
	document.getElementById("adjustModal").style.display = "block";
}

function closeModal() {
	document.getElementById("adjustModal").style.display = "none";
}
</script>

<script src="${pageContext.request.contextPath}/js/sidebar.js"></script>