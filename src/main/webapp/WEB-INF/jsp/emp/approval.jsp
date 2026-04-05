<%@ page contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>HR ERP - 휴직·퇴직 신청 현황</title>
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/style.css">
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/emp/approval.css">
<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
<script src="${pageContext.request.contextPath}/js/sidebar.js"></script>
<script src="${pageContext.request.contextPath}/js/emp/approval.js"></script>
</head>
<body data-context-path="${pageContext.request.contextPath}">

	<jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />

	<div id="main-wrapper">
		<jsp:include page="/WEB-INF/jsp/common/header.jsp" />
		<main class="app-content">

			<%-- ===== 결재 섹션 (최종승인자/HR담당자/부서장만 노출) ===== --%>
			<c:if test="${isApprover}">
				<h1>결재 현황</h1>
				<br>

				<%-- ===== 휴직/복직 결재 ===== --%>
				<h2 class="section-title">휴직·복직 신청</h2>
				<form action="${pageContext.request.contextPath}/emp/approval"
					method="get">
					<div class="search-bar">
						<%-- 상태 필터 --%>
						<select name="leaveStatus">
							<option value="대기"
								<c:if test="${leaveStatusFilter == '대기'}">selected</c:if>>대기</option>
							<option value="부서장승인"
								<c:if test="${leaveStatusFilter == '부서장승인'}">selected</c:if>>부서장승인</option>
							<option value="HR담당자승인"
								<c:if test="${leaveStatusFilter == 'HR담당자승인'}">selected</c:if>>HR담당자승인</option>
							<option value="all"
								<c:if test="${leaveStatusFilter == 'all'}">selected</c:if>>전체</option>
						</select>
						<%-- 유형 필터 --%>
						<select name="leaveType">
							<option value="" <c:if test="${empty leaveType}">selected</c:if>>유형
								전체</option>
							<option value="휴직"
								<c:if test="${leaveType == '휴직'}">selected</c:if>>휴직</option>
							<option value="복직"
								<c:if test="${leaveType == '복직'}">selected</c:if>>복직</option>
						</select>
						<%-- 부서 필터 (HR담당자/최종승인자만 노출) --%>
						<c:if test="${isHrManager || isPresident}">
							<input type="text" name="leaveDeptName" value="${leaveDeptName}"
								placeholder="부서명 검색">
						</c:if>
						<%-- 이름 검색 --%>
						<input type="text" name="leaveKeyword" value="${leaveKeyword}"
							placeholder="신청자 이름 검색">
						<button type="submit">검색</button>
					</div>
				</form>
				<div class="card">
					<table>
						<thead>
							<tr>
								<th>신청자</th>
								<th>부서</th>
								<th>유형</th>
								<th>시작일</th>
								<th>종료일</th>
								<th>사유</th>
								<th>상태</th>
								<th>상세</th>
								<th>처리</th>
							</tr>
						</thead>
						<tbody>
							<c:choose>
								<c:when test="${empty leaveApprovalList}">
									<tr>
										<td colspan="9">신청 내역이 없습니다.</td>
									</tr>
								</c:when>
								<c:otherwise>
									<c:forEach var="item" items="${leaveApprovalList}">
										<tr>
											<td><strong>${item.emp_name}</strong></td>
											<td>${item.dept_name}</td>
											<td>${item.leave_type}</td>
											<td>${item.start_date}</td>
											<td><c:choose>
													<c:when test="${empty item.end_date}">-</c:when>
													<c:otherwise>${item.end_date}</c:otherwise>
												</c:choose></td>
											<td class="td-reason">${item.reason}</td>
											<td><span class="status-badge status-${item.status}">${item.status}</span></td>
											<td>
												<button class="btn-detail"
													onclick="openApprovalModal('${pageContext.request.contextPath}/emp/approvalDetail?type=leave&id=${item.request_id}')">
													상세</button>
											</td>
											<td><c:if
													test="${item.status == '대기' && isHrManager && item.reqIsPresident}">
													<button class="btn-approve"
														onclick="location.href='${pageContext.request.contextPath}/emp/approvalAction?type=leave&id=${item.request_id}&action=approve'">승인(확정)</button>
													<button class="btn-reject"
														onclick="rejectWithReason('${pageContext.request.contextPath}/emp/approvalAction?type=leave&id=${item.request_id}&action=reject')">반려</button>
												</c:if> <c:if
													test="${item.status == '대기' && isDeptManager && !item.reqIsPresident}">
													<button class="btn-approve"
														onclick="location.href='${pageContext.request.contextPath}/emp/approvalAction?type=leave&id=${item.request_id}&action=approve'">승인</button>
													<button class="btn-reject"
														onclick="rejectWithReason('${pageContext.request.contextPath}/emp/approvalAction?type=leave&id=${item.request_id}&action=reject')">반려</button>
												</c:if> <c:if
													test="${item.status == '부서장승인' && isHrManager && !item.hrDept}">
													<button class="btn-approve"
														onclick="location.href='${pageContext.request.contextPath}/emp/approvalAction?type=leave&id=${item.request_id}&action=approve'">승인</button>
													<button class="btn-reject"
														onclick="rejectWithReason('${pageContext.request.contextPath}/emp/approvalAction?type=leave&id=${item.request_id}&action=reject')">반려</button>
												</c:if> <c:if
													test="${item.status == 'HR담당자승인' && isPresident && !item.hrDept}">
													<button class="btn-approve"
														onclick="location.href='${pageContext.request.contextPath}/emp/approvalAction?type=leave&id=${item.request_id}&action=approve'">승인</button>
													<button class="btn-reject"
														onclick="rejectWithReason('${pageContext.request.contextPath}/emp/approvalAction?type=leave&id=${item.request_id}&action=reject')">반려</button>
												</c:if> <c:if
													test="${item.status == '부서장승인' && isPresident && item.hrDept}">
													<button class="btn-approve"
														onclick="location.href='${pageContext.request.contextPath}/emp/approvalAction?type=leave&id=${item.request_id}&action=approve'">승인</button>
													<button class="btn-reject"
														onclick="rejectWithReason('${pageContext.request.contextPath}/emp/approvalAction?type=leave&id=${item.request_id}&action=reject')">반려</button>
												</c:if></td>
										</tr>
									</c:forEach>
								</c:otherwise>
							</c:choose>
						</tbody>
					</table>
				</div>

				<%-- ===== 퇴직 결재 ===== --%>
				<h2 class="section-title" style="margin-top: 32px;">퇴직 신청</h2>
				<form action="${pageContext.request.contextPath}/emp/approval"
					method="get">
					<div class="search-bar">
						<select name="resignStatus">
							<option value="대기"
								<c:if test="${resignStatusFilter == '대기'}">selected</c:if>>대기</option>
							<option value="부서장승인"
								<c:if test="${resignStatusFilter == '부서장승인'}">selected</c:if>>부서장승인</option>
							<option value="HR담당자승인"
								<c:if test="${resignStatusFilter == 'HR담당자승인'}">selected</c:if>>HR담당자승인</option>
							<option value="all"
								<c:if test="${resignStatusFilter == 'all'}">selected</c:if>>전체</option>
						</select>
						<c:if test="${isHrManager || isPresident}">
							<input type="text" name="resignDeptName"
								value="${resignDeptName}" placeholder="부서명 검색">
						</c:if>
						<input type="text" name="resignKeyword" value="${resignKeyword}"
							placeholder="신청자 이름 검색">
						<button type="submit">검색</button>
					</div>
				</form>
				<div class="card">
					<table>
						<thead>
							<tr>
								<th>신청자</th>
								<th>부서</th>
								<th>희망 퇴직일</th>
								<th>사유</th>
								<th>상태</th>
								<th>상세</th>
								<th>처리</th>
							</tr>
						</thead>
						<tbody>
							<c:choose>
								<c:when test="${empty resignApprovalList}">
									<tr>
										<td colspan="7">신청 내역이 없습니다.</td>
									</tr>
								</c:when>
								<c:otherwise>
									<c:forEach var="item" items="${resignApprovalList}">
										<tr>
											<td><strong>${item.emp_name}</strong></td>
											<td>${item.dept_name}</td>
											<td>${item.resign_date}</td>
											<td class="td-reason">${item.reason}</td>
											<td><span class="status-badge status-${item.status}">${item.status}</span></td>
											<td>
												<button class="btn-detail"
													onclick="openApprovalModal('${pageContext.request.contextPath}/emp/approvalDetail?type=resign&id=${item.request_id}')">
													상세</button>
											</td>
											<td><c:if
													test="${item.status == '대기' && isHrManager && item.reqIsPresident}">
													<button class="btn-approve"
														onclick="location.href='${pageContext.request.contextPath}/emp/approvalAction?type=resign&id=${item.request_id}&action=approve'">승인(확정)</button>
													<button class="btn-reject"
														onclick="rejectWithReason('${pageContext.request.contextPath}/emp/approvalAction?type=resign&id=${item.request_id}&action=reject')">반려</button>
												</c:if> <c:if
													test="${item.status == '대기' && isDeptManager && !item.reqIsPresident}">
													<button class="btn-approve"
														onclick="location.href='${pageContext.request.contextPath}/emp/approvalAction?type=resign&id=${item.request_id}&action=approve'">승인</button>
													<button class="btn-reject"
														onclick="rejectWithReason('${pageContext.request.contextPath}/emp/approvalAction?type=resign&id=${item.request_id}&action=reject')">반려</button>
												</c:if> <c:if
													test="${item.status == '부서장승인' && isHrManager && !item.hrDept}">
													<button class="btn-approve"
														onclick="location.href='${pageContext.request.contextPath}/emp/approvalAction?type=resign&id=${item.request_id}&action=approve'">승인</button>
													<button class="btn-reject"
														onclick="rejectWithReason('${pageContext.request.contextPath}/emp/approvalAction?type=resign&id=${item.request_id}&action=reject')">반려</button>
												</c:if> <c:if
													test="${item.status == 'HR담당자승인' && isPresident && !item.hrDept}">
													<button class="btn-approve"
														onclick="location.href='${pageContext.request.contextPath}/emp/approvalAction?type=resign&id=${item.request_id}&action=approve'">승인</button>
													<button class="btn-reject"
														onclick="rejectWithReason('${pageContext.request.contextPath}/emp/approvalAction?type=resign&id=${item.request_id}&action=reject')">반려</button>
												</c:if> <c:if
													test="${item.status == '부서장승인' && isPresident && item.hrDept}">
													<button class="btn-approve"
														onclick="location.href='${pageContext.request.contextPath}/emp/approvalAction?type=resign&id=${item.request_id}&action=approve'">승인</button>
													<button class="btn-reject"
														onclick="rejectWithReason('${pageContext.request.contextPath}/emp/approvalAction?type=resign&id=${item.request_id}&action=reject')">반려</button>
												</c:if></td>
										</tr>
									</c:forEach>
								</c:otherwise>
							</c:choose>
						</tbody>
					</table>
				</div>
				<br>
				<br>
				<br>
				<hr>
			</c:if>

			<%-- ===== 내 신청 현황 (모든 사용자) ===== --%>
			<h1 style="margin-top: 40px;">내 신청 현황</h1>
			<br>

			<%-- 내 휴직/복직 신청 (미완료) --%>
			<h2 class="section-title">휴직·복직 신청</h2>
			<form action="${pageContext.request.contextPath}/emp/approval"
				method="get">
				<div class="search-bar">
					<select name="myLeaveType">
						<option value="" <c:if test="${empty myLeaveType}">selected</c:if>>유형
							전체</option>
						<option value="휴직"
							<c:if test="${myLeaveType == '휴직'}">selected</c:if>>휴직</option>
						<option value="복직"
							<c:if test="${myLeaveType == '복직'}">selected</c:if>>복직</option>
					</select> <select name="myLeaveStatus">
						<option value="all"
							<c:if test="${myLeaveStatus == 'all'}">selected</c:if>>상태
							전체</option>
						<option value="대기"
							<c:if test="${myLeaveStatus == '대기'}">selected</c:if>>대기</option>
						<option value="부서장승인"
							<c:if test="${myLeaveStatus == '부서장승인'}">selected</c:if>>부서장승인</option>
						<option value="HR담당자승인"
							<c:if test="${myLeaveStatus == 'HR담당자승인'}">selected</c:if>>HR담당자승인</option>
					</select>
					<button type="submit">검색</button>
				</div>
			</form>
			<div class="card">
				<table>
					<thead>
						<tr>
							<th>유형</th>
							<th>시작일</th>
							<th>종료일</th>
							<th>사유</th>
							<th>상태</th>
							<th>신청일</th>
							<th>상세</th>
							<th>관리</th>
						</tr>
					</thead>
					<tbody>
						<c:choose>
							<c:when test="${empty myLeaveList}">
								<tr>
									<td colspan="8">신청 내역이 없습니다.</td>
								</tr>
							</c:when>
							<c:otherwise>
								<c:forEach var="item" items="${myLeaveList}">
									<tr>
										<td>${item.leave_type}</td>
										<td>${item.start_date}</td>
										<td><c:choose>
												<c:when test="${empty item.end_date}">-</c:when>
												<c:otherwise>${item.end_date}</c:otherwise>
											</c:choose></td>
										<td class="td-reason">${item.reason}</td>
										<td><span class="status-badge status-${item.status}">${item.status}</span></td>
										<td>${item.created_at.toString().replace('T', ' ')}</td>
										<td>
											<button class="btn-detail"
												onclick="openApprovalModal('${pageContext.request.contextPath}/emp/approvalDetail?type=leave&id=${item.request_id}')">
												상세</button>
										</td>
										<td><c:if test="${item.status == '대기'}">
												<button class="btn-modify" 
												onclick="openApprovalModal('${pageContext.request.contextPath}/emp/leave?emp_no=${item.emp_no}&mode=edit&id=${item.request_id}')">변경</button>
												<button class="btn-cancel" onclick="if(confirm('정말 철회하시겠습니까?'))
  											    	  location.href='${pageContext.request.contextPath}/emp/leaveWithdraw?id=${item.request_id}'">철회</button>
											</c:if></td>
									</tr>
								</c:forEach>
							</c:otherwise>
						</c:choose>
					</tbody>
				</table>
			</div>

			<%-- 내 퇴직 신청 (미완료) --%>
			<h2 class="section-title" style="margin-top: 32px;">퇴직 신청</h2>
			<form action="${pageContext.request.contextPath}/emp/approval"
				method="get">
				<div class="search-bar">
					<select name="myResignStatus">
						<option value="all"
							<c:if test="${myResignStatus == 'all'}">selected</c:if>>상태
							전체</option>
						<option value="대기"
							<c:if test="${myResignStatus == '대기'}">selected</c:if>>대기</option>
						<option value="부서장승인"
							<c:if test="${myResignStatus == '부서장승인'}">selected</c:if>>부서장승인</option>
						<option value="HR담당자승인"
							<c:if test="${myResignStatus == 'HR담당자승인'}">selected</c:if>>HR담당자승인</option>
					</select>
					<button type="submit">검색</button>
				</div>
			</form>
			<div class="card">
				<table>
					<thead>
						<tr>
							<th>희망 퇴직일</th>
							<th>사유</th>
							<th>상태</th>
							<th>신청일</th>
							<th>상세</th>
						</tr>
					</thead>
					<tbody>
						<c:choose>
							<c:when test="${empty myResignList}">
								<tr>
									<td colspan="5">신청 내역이 없습니다.</td>
								</tr>
							</c:when>
							<c:otherwise>
								<c:forEach var="item" items="${myResignList}">
									<tr>
										<td>${item.resign_date}</td>
										<td class="td-reason">${item.reason}</td>
										<td><span class="status-badge status-${item.status}">${item.status}</span></td>
										<td>${item.created_at.toString().replace('T', ' ')}</td>
										<td>
											<button class="btn-detail"
												onclick="openApprovalModal('${pageContext.request.contextPath}/emp/approvalDetail?type=resign&id=${item.request_id}')">
												상세</button>
										</td>
									</tr>
								</c:forEach>
							</c:otherwise>
						</c:choose>
					</tbody>
				</table>
			</div>

		</main>
	</div>

	<%-- ===== 신청 상세 모달 ===== --%>
	<div id="approvalDetailModal" class="modal-overlay">
		<div class="modal-content">
			<div class="modal-header">
				<h2>신청 상세 정보</h2>
				<button type="button" id="closeApprovalModalBtn"
					style="cursor: pointer;">✕</button>
			</div>
			<iframe id="approvalModalIframe" src=""></iframe>
		</div>
	</div>


</body>
</html>
