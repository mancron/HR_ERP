<%@ page contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>HR ERP - 휴가 신청</title>
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/att/common/requestList.css">
	<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/att/common/requestDetail.css">
<link rel="stylesheet"
	href="<c:out value='${pageContext.request.contextPath}/css/att/leave.css'/>">
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/att/common/toast.css">
	<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>

<jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />
<div id="main-wrapper">
	<jsp:include page="/WEB-INF/jsp/common/header.jsp" />
	<jsp:include page="/WEB-INF/jsp/att/common/toast.jsp" />
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
								<label>시작일</label> <input type="date" name="start_date" required
									value="${formData.startDate}">
							</div>
							<div>
								<label>종료일</label> <input type="date" name="end_date" required
									value="${formData.endDate}"><input type="hidden"
									name="end_date" id="hiddenEndDate">
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

					<jsp:include page="/WEB-INF/jsp/att/common/requestList.jsp" />
				</div>
			</div>
		</div>
	</main>
</div>
<script>
	const contextPath = "${pageContext.request.contextPath}";
	const currentPath = "/att/leave";
</script>
<script
	src="<c:out value='${pageContext.request.contextPath}/js/sidebar.js'/>"></script>
<script src="${pageContext.request.contextPath}/js/att/leave.js"></script>
<script src="${pageContext.request.contextPath}/js/att/common/toast.js"></script>
<script
	src="${pageContext.request.contextPath}/js/att/common/requestDetail.js"></script>

<jsp:include page="/WEB-INF/jsp/att/common/requestDetail.jsp" />