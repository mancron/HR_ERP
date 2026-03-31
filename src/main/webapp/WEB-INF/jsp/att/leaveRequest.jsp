<%@ page contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<link rel="stylesheet"
	href="<c:out value='${pageContext.request.contextPath}/css/attendance.css'/>">

<link rel="stylesheet"
	href="<c:out value='${pageContext.request.contextPath}/css/leave.css'/>">

<!-- 알림 -->
<c:if test="${param.msg eq 'success'}">
	<script>
		alert("휴가 신청이 완료되었습니다.");
	</script>
</c:if>

<c:if test="${param.error eq 'not_enough'}">
	<script>
		alert("잔여 연차가 부족합니다.");
	</script>
</c:if>

<c:if test="${param.error eq 'overlap'}">
	<script>
		alert("이미 해당 기간에 신청된 휴가가 있습니다.");
	</script>
</c:if>

<c:if test="${param.error eq 'empty_reason'}">
	<script>
		alert("휴가 사유를 입력해주세요.");
	</script>
</c:if>

<c:if test="${param.error eq 'invalid_date'}">
	<script>
		alert("시작일은 종료일보다 늦을 수 없습니다.");
	</script>
</c:if>

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
						부여 연차<br>
						<c:out value="${annual.totalDays}" />
						일
					</div>
					<div class="summary-box summary-remain">
						잔여 연차<br>
						<c:out value="${annual.remainDays}" />
						일
					</div>
					<div class="summary-box summary-used">
						사용 연차<br>
						<c:out value="${annual.usedDays}" />
						일
					</div>
				</div>

				<!-- 신청 폼 -->
				<div class="leave-card">

					<form
						action="<c:out value='${pageContext.request.contextPath}/att/leave/req'/>"
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
				<form method="get"
					action="${pageContext.request.contextPath}/att/leave/req"
					style="margin-bottom: 10px;">

					<input type="month" name="month" value="<c:out value='${month}'/>">

					<button type="submit">조회</button>
				</form>
				<div class="leave-right-box">
					<h3>신청 내역</h3>

					<table class="att-table">
						<tr>
							<th>기간</th>
							<th>유형</th>
							<th>상태</th>
						</tr>

						<c:forEach var="item" items="${list}">
							<tr>
								<td><c:out value="${item.startDate}" /> ~ <c:out
										value="${item.endDate}" /></td>

								<td><c:out value="${item.leaveType}" /> <c:if
										test="${item.leaveType eq '반차'}">
    - <c:out value="${item.halfType}" />
									</c:if></td>

								<td><c:out value="${item.status}" /> <!-- 🔥 상태 옆에 버튼 -->
									<c:if test="${item.status eq '대기'}">
										<form
											action="${pageContext.request.contextPath}/att/leave/cancel"
											method="post" style="display: inline;">

											<input type="hidden" name="leave_id"
												value="<c:out value='${item.leaveId}'/>">

											<button type="submit" class="cancel-btn">취소</button>
										</form>
									</c:if></td>
							</tr>
						</c:forEach>

					</table>
				</div>

			</div>

		</div>

	</main>
</div>

<script
	src="<c:out value='${pageContext.request.contextPath}/js/sidebar.js'/>"></script>
<script src="${pageContext.request.contextPath}/js/leave.js"></script>