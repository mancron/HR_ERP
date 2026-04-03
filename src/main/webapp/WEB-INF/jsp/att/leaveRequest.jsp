<%@ page contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>HR ERP - 휴가 신청</title>
<link rel="stylesheet"
	href="<c:out value='${pageContext.request.contextPath}/css/att/attendance.css'/>">
<link rel="stylesheet"
	href="<c:out value='${pageContext.request.contextPath}/css/att/leave.css'/>">
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/common/toast.css">
</head>

<jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />
<div id="main-wrapper">
	<jsp:include page="/WEB-INF/jsp/common/header.jsp" />
	<jsp:include page="/WEB-INF/jsp/common/toast.jsp" />
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
										test="${item.leaveType eq '반차'}"> - <c:out
											value="${item.halfType}" />
									</c:if></td>
								<td>
									<button type="button"
										class="status-btn
    <c:if test='${item.status eq "승인"}'> status-approved</c:if>
    <c:if test='${item.status eq "반려"}'> status-rejected</c:if>
    <c:if test='${item.status eq "대기"}'> status-pending</c:if>"
										onclick="openModal(${item.leaveId})">

										<c:out value="${item.status}" />
									</button> <c:if test="${item.status eq '대기'}">
										<form
											action="${pageContext.request.contextPath}/att/leave/cancel"
											method="post" style="display: inline;">
											<input type="hidden" name="leave_id"
												value="<c:out value='${item.leaveId}'/>"> <input
												type="hidden" name="year" value="${param.year}"> <input
												type="hidden" name="month" value="${param.month}">
											<button type="submit" class="status-btn status-cancel">취소</button>
										</form>
									</c:if>
								</td>
							</tr>
						</c:forEach>
					</table>
				</div>
			</div>
		</div>
	</main>
</div>
<script>
    const contextPath = "${pageContext.request.contextPath}";
</script>
<script
	src="<c:out value='${pageContext.request.contextPath}/js/sidebar.js'/>"></script>
<script src="${pageContext.request.contextPath}/js/att/leave.js"></script>
<script src="${pageContext.request.contextPath}/js/common/toast.js"></script>


<div id="leaveModal" class="modal">
	<div class="modal-content">

		<span class="close" onclick="closeModal()">&times;</span>

		<h3>휴가 상세보기</h3>

		<!-- 신청 정보 -->
		<div class="modal-section">
			<h4>신청 정보</h4>

			<table class="detail-table">
				<tr>
					<th>신청자</th>
					<td id="modalEmp"></td>
				</tr>
				<tr>
					<th>신청일</th>
					<td id="modalApplyDate"></td>
				</tr>
				<tr>
					<th>사용 기간</th>
					<td id="modalPeriod"></td>
				</tr>
				<tr>
					<th>휴가 유형</th>
					<td id="modalType"></td>
				</tr>
				<tr>
					<th>사유</th>
					<td id="modalReason"></td>
				</tr>
			</table>
		</div>

		<!-- 승인 정보 -->
		<div class="modal-section">
			<h4>승인 정보</h4>

			<table class="detail-table">
				<tr>
					<th>상태</th>
					<td id="modalStatus"></td>
				</tr>
				<tr>
					<th>승인자</th>
					<td id="modalApprover"></td>
				</tr>
				<tr>
					<th>처리일</th>
					<td id="modalApproveDate"></td>
				</tr>
				<tr id="modalRejectRow">
					<th>반려 사유</th>
					<td id="modalReject"></td>
				</tr>
			</table>
		</div>

	</div>
</div>