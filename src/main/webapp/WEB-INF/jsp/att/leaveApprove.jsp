<%@ page contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>HR ERP - 휴가 승인</title>
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/att/leaveApprove.css">
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/att/common/modal.css">
	<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>

<jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />

<div id="main-wrapper">
	<jsp:include page="/WEB-INF/jsp/common/header.jsp" />

	<main class="app-content">

		<h2 class="page-title">휴가 승인 관리</h2>
		<form method="get"
			action="${pageContext.request.contextPath}/att/leave/approve"
			class="filter-box">

			<!-- 🔹 부서 선택 -->
			<select name="dept">
				<option value="">전체 부서</option>

				<c:forEach var="d" items="${deptList}">
					<option value="${d}" <c:if test="${d eq dept}">selected</c:if>>
						<c:out value="${d}" />
					</option>
				</c:forEach>
			</select>

			<!-- 🔹 정렬 선택 -->
			<select name="sort">
				<option value="">정렬 선택</option>

				<option value="name_asc"
					<c:if test="${sort eq 'name_asc'}">selected</c:if>>이름 가나다순</option>

				<option value="name_desc"
					<c:if test="${sort eq 'name_desc'}">selected</c:if>>이름 가나다
					역순</option>

				<option value="position_asc"
					<c:if test="${sort eq 'position_asc'}">selected</c:if>>직급
					오름차순</option>

				<option value="position_desc"
					<c:if test="${sort eq 'position_desc'}">selected</c:if>>
					직급 내림차순</option>
			</select>
			<!-- 기간 조회 -->
			<input type="date" name="startDate" value="${startDate}"> ~ <input
				type="date" name="endDate" value="${endDate}">

			<button type="submit" class="btn approve-btn">조회</button>

		</form>
		<div class="leave-right-box">

			<table class="leave-approve-table">
				<thead>
					<tr>
						<th>부서</th>
						<th>직급</th>
						<th>신청자</th>
						<th>기간</th>
						<th>유형</th>
						<th>일수</th>
						<th>사유</th>
						<th>상태</th>
						<th>처리</th>
					</tr>
				</thead>

				<tbody>
					<c:choose>

						<c:when test="${not empty list}">
							<c:forEach var="item" items="${list}">
								<tr>

									<td><c:out value="${item.deptName}" /></td>
									<td><c:out value="${item.position}" /></td>
									<td><c:out value="${item.empName}" /></td>

									<td><c:out value="${item.startDate}" /> ~ <c:out
											value="${item.endDate}" /></td>

									<td><c:out value="${item.leaveType}" /> <c:if
											test="${item.leaveType eq '반차'}">
            - <c:out value="${item.halfType}" />
										</c:if></td>

									<td><c:out value="${item.days}" /></td>
									<td><c:out value="${item.reason}" /></td>

									<td><span class="status pending"> <c:out
												value="${item.status}" />
									</span></td>
									<td class="action-cell">
										<!-- 승인 -->
										<form
											action="${pageContext.request.contextPath}/leave/updateStatus"
											method="post">

											<input type="hidden" name="leaveId" value="${item.leaveId}">
											<input type="hidden" name="status" value="승인">

											<button type="button" class="btn approve-btn"
												onclick="approveLeave(this.form)">승인</button>
										</form> <!-- 반려 -->
										<form
											action="${pageContext.request.contextPath}/leave/updateStatus"
											method="post" onsubmit="return confirmReject(this);">

											<input type="hidden" name="leaveId" value="${item.leaveId}">
											<input type="hidden" name="status" value="반려"> <input
												type="hidden" name="reason">

											<button type="button" class="btn reject-btn"
												onclick="showRejectForm(this)">반려</button>
										</form>

									</td>

								</tr>
							</c:forEach>
						</c:when>

						<c:otherwise>
							<tr>
								<td colspan="9" class="empty-row">승인 대기 중인 휴가가 없습니다.</td>
							</tr>
						</c:otherwise>

					</c:choose>
				</tbody>

			</table>

		</div>

	</main>
</div>
<script>
	const contextPath = "${pageContext.request.contextPath}";
</script>
<script src="${pageContext.request.contextPath}/js/sidebar.js"></script>
<script src="${pageContext.request.contextPath}/js/att/leave.js"></script>

<div id="confirmModal" class="modal">
	<div class="modal-content">

		<p id="confirmMessage"></p>

		<div
			style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px;">
			<button id="confirmYes" class="att-btn att-btn-in">확인</button>
			<button onclick="closeConfirmModal()" class="att-btn att-btn-cancle">취소</button>
		</div>

	</div>
</div>