<%@ page contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/attendance.css">

<style>
.leave-container {
	display: flex;
	gap: 20px;
}

.leave-left {
	flex: 3;
}

.leave-right {
	flex: 1;
}

.leave-summary {
	display: flex;
	gap: 15px;
	margin-bottom: 20px;
}

.leave-card {
    background: #ffffff;
    padding: 25px;
    border-radius: 12px;
    box-shadow: 0 2px 10px rgba(0,0,0,0.05);
    margin-top: 15px;
}

.leave-card input,
.leave-card select,
.leave-card textarea {
    width: 100%;
    padding: 10px;
    margin-top: 5px;
    margin-bottom: 15px;
    border: 1px solid #ccc;
    border-radius: 6px;
    font-size: 14px;
}

.leave-card textarea {
    resize: none;
}

.summary-box {
	flex: 1;
	padding: 15px;
	border-radius: 10px;
	text-align: center;
	font-weight: bold;
}

.summary-total {
	background: #dbe2ef;
}

.summary-remain {
	background: #d4edda;
}

.summary-used {
	background: #fff3cd;
}

.leave-card {
	background: white;
	padding: 20px;
	border-radius: 10px;
}

.leave-right-box {
	background: white;
	padding: 15px;
	border-radius: 10px;
}

.leave-row {
	display: flex;
	gap: 10px;
}

.leave-row input {
	flex: 1;
}
</style>

<jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />

<div id="main-wrapper">
	<jsp:include page="/WEB-INF/jsp/common/header.jsp" />

	<main class="app-content">

		<h2>휴가 신청</h2>
		<br>
		<div class="leave-container">

			<!-- 좌측 -->
			<div class="leave-left">

				<!-- 연차 요약 -->
				<div class="leave-summary">
					<div class="summary-box summary-total">
						부여 연차<br> ${annual.totalDays}일
					</div>
					<div class="summary-box summary-remain">
						잔여 연차<br> ${annual.remainDays}일
					</div>
					<div class="summary-box summary-used">
						사용 연차<br> ${annual.usedDays}일
					</div>
				</div>

				<!-- 신청 폼 -->
				<div class="leave-card">

					<form action="${pageContext.request.contextPath}/att/leave/req"
						method="post">

						<label>휴가 유형</label> <select name="leave_type" id="leaveType">
							<option value="연차">연차</option>
							<option value="반차">반차</option>
							<option value="병가">병가</option>
							<option value="공가">공가</option>
							<option value="경조사">경조사</option>
						</select>

						<div id="halfTypeDiv" style="display: none;">
							<label>반차 구분</label> <select name="half_type">
								<option value="오전">오전</option>
								<option value="오후">오후</option>
							</select>
						</div>

						<div class="leave-row">
							<div>
								<label>시작일</label> <input type="date" name="start_date" required>
							</div>
							<div>
								<label>종료일</label> <input type="date" name="end_date" required>
							</div>
						</div>

						<br> <label>사유</label>
						<textarea name="reason" rows="4"></textarea>

						<br>

						<div style="text-align: right;">
							<button class="att-btn att-btn-in" type="submit">신청</button>
						</div>

					</form>
				</div>
			</div>

			<!-- 우측 -->
			<div class="leave-right">

				<div class="leave-right-box">
					<h3>신청 내역</h3>

					<table class="att-table">
						<tr>
							<th>기간</th>
							<th>유형</th>
							<th>상태</th>
						</tr>

						<!-- 나중에 연결 -->
						<tr>
							<td>3/19~3/20</td>
							<td>연차</td>
							<td>승인</td>
						</tr>
						<tr>
							<td>3/21(오후)</td>
							<td>반차</td>
							<td>승인</td>
						</tr>
						<tr>
							<td>3/26~3/28</td>
							<td>연차</td>
							<td>대기</td>
						</tr>

					</table>
				</div>

			</div>

		</div>

	</main>
</div>

<script src="${pageContext.request.contextPath}/js/sidebar.js"></script>

<script>
	const leaveType = document.getElementById("leaveType");
	const halfTypeDiv = document.getElementById("halfTypeDiv");

	leaveType.addEventListener("change", function() {
		halfTypeDiv.style.display = (this.value === "반차") ? "block" : "none";
	});
</script>