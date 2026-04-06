<%@ page contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>HR ERP - 휴직·퇴직 신청 이력</title>
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

			<%-- ===== 결재 처리 결과 (결재자만) ===== --%>
			<c:if test="${isApprover}">
				<h1>결재 내역</h1>
				<br>

				<%-- 휴직/복직 처리 결과 --%>
				<h2 class="section-title">휴직·복직</h2>
				<form
					action="${pageContext.request.contextPath}/emp/approvalHistory"
					method="get">
					<div class="search-bar">
						<select name="leaveDoneStatus">
							<option value="all"
								<c:if test="${leaveDoneStatus == 'all'}">selected</c:if>>전체</option>
							<option value="최종승인"
								<c:if test="${leaveDoneStatus == '최종승인'}">selected</c:if>>최종승인</option>
							<option value="반려"
								<c:if test="${leaveDoneStatus == '반려'}">selected</c:if>>반려</option>
						</select> <select name="leaveDoneType">
							<option value=""
								<c:if test="${empty leaveDoneType}">selected</c:if>>유형
								전체</option>
							<option value="휴직"
								<c:if test="${leaveDoneType == '휴직'}">selected</c:if>>휴직</option>
							<option value="복직"
								<c:if test="${leaveDoneType == '복직'}">selected</c:if>>복직</option>
						</select>
						<c:if test="${isHrManager || isPresident}">
							<input type="text" name="leaveDoneDeptName"
								value="${leaveDoneDeptName}" placeholder="부서명 검색">
						</c:if>
						<input type="text" name="leaveDoneKeyword"
							value="${leaveDoneKeyword}" placeholder="신청자 이름 검색">
						<button type="submit">검색</button>
					</div>
				</form>
				<p>
					총 <strong>${leaveDoneTotalCount}</strong>건
				</p>
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
							</tr>
						</thead>
						<tbody>
							<c:choose>
								<c:when test="${empty leaveDoneList}">
									<tr>
										<td colspan="8">처리된 내역이 없습니다.</td>
									</tr>
								</c:when>
								<c:otherwise>
									<c:forEach var="item" items="${leaveDoneList}">
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
										</tr>
									</c:forEach>
								</c:otherwise>
							</c:choose>
						</tbody>
					</table>
				</div>
				<%-- 휴직/복직 처리 결과 페이징 --%>
				<div class="pagination">
					<c:set var="leaveDoneBase"
						value="leaveDoneStatus=${leaveDoneStatus}&amp;leaveDoneType=${leaveDoneType}&amp;leaveDoneDeptName=${leaveDoneDeptName}&amp;leaveDoneKeyword=${leaveDoneKeyword}" />
					<c:choose>
						<c:when test="${leaveDoneNowBlock > 1}">
							<c:set var="prev"
								value="${(leaveDoneNowBlock - 2) * leaveDonePagePerBlock + 1}" />
							<a
								href="${pageContext.request.contextPath}/emp/approvalHistory?${leaveDoneBase}&leaveDonePage=${prev}">◀</a>
						</c:when>
						<c:otherwise>
							<a class="disabled">◀</a>
						</c:otherwise>
					</c:choose>
					<c:forEach var="p" begin="${leaveDonePageStart}"
						end="${leaveDonePageEnd}">
						<c:choose>
							<c:when test="${p == leaveDoneNowPage}">
								<a class="active">${p}</a>
							</c:when>
							<c:otherwise>
								<a
									href="${pageContext.request.contextPath}/emp/approvalHistory?${leaveDoneBase}&leaveDonePage=${p}">${p}</a>
							</c:otherwise>
						</c:choose>
					</c:forEach>
					<c:choose>
						<c:when test="${leaveDoneTotalBlock > leaveDoneNowBlock}">
							<c:set var="next"
								value="${leaveDoneNowBlock * leaveDonePagePerBlock + 1}" />
							<a
								href="${pageContext.request.contextPath}/emp/approvalHistory?${leaveDoneBase}&leaveDonePage=${next}">▶</a>
						</c:when>
						<c:otherwise>
							<a class="disabled">▶</a>
						</c:otherwise>
					</c:choose>
				</div>

				<%-- 퇴직 처리 결과 --%>
				<h2 class="section-title" style="margin-top: 40px;">퇴직</h2>
				<form
					action="${pageContext.request.contextPath}/emp/approvalHistory"
					method="get">
					<div class="search-bar">
						<select name="resignDoneStatus">
							<option value="all"
								<c:if test="${resignDoneStatus == 'all'}">selected</c:if>>전체</option>
							<option value="최종승인"
								<c:if test="${resignDoneStatus == '최종승인'}">selected</c:if>>최종승인</option>
							<option value="반려"
								<c:if test="${resignDoneStatus == '반려'}">selected</c:if>>반려</option>
						</select>
						<c:if test="${isHrManager || isPresident}">
							<input type="text" name="resignDoneDeptName"
								value="${resignDoneDeptName}" placeholder="부서명 검색">
						</c:if>
						<input type="text" name="resignDoneKeyword"
							value="${resignDoneKeyword}" placeholder="신청자 이름 검색">
						<button type="submit">검색</button>
					</div>
				</form>
				<p>
					총 <strong>${resignDoneTotalCount}</strong>건
				</p>
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
							</tr>
						</thead>
						<tbody>
							<c:choose>
								<c:when test="${empty resignDoneList}">
									<tr>
										<td colspan="6">처리된 내역이 없습니다.</td>
									</tr>
								</c:when>
								<c:otherwise>
									<c:forEach var="item" items="${resignDoneList}">
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
										</tr>
									</c:forEach>
								</c:otherwise>
							</c:choose>
						</tbody>
					</table>
				</div>
				<%-- 퇴직 처리 결과 페이징 --%>
				<div class="pagination">
					<c:set var="resignDoneBase"
						value="resignDoneStatus=${resignDoneStatus}&amp;resignDoneDeptName=${resignDoneDeptName}&amp;resignDoneKeyword=${resignDoneKeyword}" />
					<c:choose>
						<c:when test="${resignDoneNowBlock > 1}">
							<c:set var="prev"
								value="${(resignDoneNowBlock - 2) * resignDonePagePerBlock + 1}" />
							<a
								href="${pageContext.request.contextPath}/emp/approvalHistory?${resignDoneBase}&resignDonePage=${prev}">◀</a>
						</c:when>
						<c:otherwise>
							<a class="disabled">◀</a>
						</c:otherwise>
					</c:choose>
					<c:forEach var="p" begin="${resignDonePageStart}"
						end="${resignDonePageEnd}">
						<c:choose>
							<c:when test="${p == resignDoneNowPage}">
								<a class="active">${p}</a>
							</c:when>
							<c:otherwise>
								<a
									href="${pageContext.request.contextPath}/emp/approvalHistory?${resignDoneBase}&resignDonePage=${p}">${p}</a>
							</c:otherwise>
						</c:choose>
					</c:forEach>
					<c:choose>
						<c:when test="${resignDoneTotalBlock > resignDoneNowBlock}">
							<c:set var="next"
								value="${resignDoneNowBlock * resignDonePagePerBlock + 1}" />
							<a
								href="${pageContext.request.contextPath}/emp/approvalHistory?${resignDoneBase}&resignDonePage=${next}">▶</a>
						</c:when>
						<c:otherwise>
							<a class="disabled">▶</a>
						</c:otherwise>
					</c:choose>
				</div>
				<br>
				<br>
				<br>
				<hr>
			</c:if>

			<%-- ===== 내 처리 결과 (모든 사용자) ===== --%>
			<h1 style="margin-top: 40px;">내 신청 내역</h1>
			<br>

			<%-- 내 휴직/복직 처리 결과 --%>
			<h2 class="section-title">휴직·복직</h2>
			<form action="${pageContext.request.contextPath}/emp/approvalHistory"
				method="get">
				<div class="search-bar">
					<select name="myLeaveDoneType">
						<option value=""
							<c:if test="${empty myLeaveDoneType}">selected</c:if>>유형
							전체</option>
						<option value="휴직"
							<c:if test="${myLeaveDoneType == '휴직'}">selected</c:if>>휴직</option>
						<option value="복직"
							<c:if test="${myLeaveDoneType == '복직'}">selected</c:if>>복직</option>
					</select> <select name="myLeaveDoneStatus">
						<option value="all"
							<c:if test="${myLeaveDoneStatus == 'all'}">selected</c:if>>전체</option>
						<option value="최종승인"
							<c:if test="${myLeaveDoneStatus == '최종승인'}">selected</c:if>>최종승인</option>
						<option value="반려"
							<c:if test="${myLeaveDoneStatus == '반려'}">selected</c:if>>반려</option>
					</select>
					<button type="submit">검색</button>
				</div>
			</form>
			<p>
				총 <strong>${myLeaveDoneTotalCount}</strong>건
			</p>
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
						</tr>
					</thead>
					<tbody>
						<c:choose>
							<c:when test="${empty myLeaveDoneList}">
								<tr>
									<td colspan="7">처리된 내역이 없습니다.</td>
								</tr>
							</c:when>
							<c:otherwise>
								<c:forEach var="item" items="${myLeaveDoneList}">
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
									</tr>
								</c:forEach>
							</c:otherwise>
						</c:choose>
					</tbody>
				</table>
			</div>
			<%-- 내 휴직/복직 처리 결과 페이징 --%>
			<div class="pagination">
				<c:set var="myLeaveDoneBase"
					value="myLeaveDoneType=${myLeaveDoneType}&amp;myLeaveDoneStatus=${myLeaveDoneStatus}" />
				<c:choose>
					<c:when test="${myLeaveDoneNowBlock > 1}">
						<c:set var="prev"
							value="${(myLeaveDoneNowBlock - 2) * myLeaveDonePagePerBlock + 1}" />
						<a
							href="${pageContext.request.contextPath}/emp/approvalHistory?${myLeaveDoneBase}&myLeaveDonePage=${prev}">◀</a>
					</c:when>
					<c:otherwise>
						<a class="disabled">◀</a>
					</c:otherwise>
				</c:choose>
				<c:forEach var="p" begin="${myLeaveDonePageStart}"
					end="${myLeaveDonePageEnd}">
					<c:choose>
						<c:when test="${p == myLeaveDoneNowPage}">
							<a class="active">${p}</a>
						</c:when>
						<c:otherwise>
							<a
								href="${pageContext.request.contextPath}/emp/approvalHistory?${myLeaveDoneBase}&myLeaveDonePage=${p}">${p}</a>
						</c:otherwise>
					</c:choose>
				</c:forEach>
				<c:choose>
					<c:when test="${myLeaveDoneTotalBlock > myLeaveDoneNowBlock}">
						<c:set var="next"
							value="${myLeaveDoneNowBlock * myLeaveDonePagePerBlock + 1}" />
						<a
							href="${pageContext.request.contextPath}/emp/approvalHistory?${myLeaveDoneBase}&myLeaveDonePage=${next}">▶</a>
					</c:when>
					<c:otherwise>
						<a class="disabled">▶</a>
					</c:otherwise>
				</c:choose>
			</div>

			<%-- 내 퇴직 처리 결과 --%>
			<h2 class="section-title" style="margin-top: 40px;">퇴직</h2>
			<form action="${pageContext.request.contextPath}/emp/approvalHistory"
				method="get">
				<div class="search-bar">
					<select name="myResignDoneStatus">
						<option value="all"
							<c:if test="${myResignDoneStatus == 'all'}">selected</c:if>>전체</option>
						<option value="최종승인"
							<c:if test="${myResignDoneStatus == '최종승인'}">selected</c:if>>최종승인</option>
						<option value="반려"
							<c:if test="${myResignDoneStatus == '반려'}">selected</c:if>>반려</option>
					</select>
					<button type="submit">검색</button>
				</div>
			</form>
			<p>
				총 <strong>${myResignDoneTotalCount}</strong>건
			</p>
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
							<c:when test="${empty myResignDoneList}">
								<tr>
									<td colspan="5">처리된 내역이 없습니다.</td>
								</tr>
							</c:when>
							<c:otherwise>
								<c:forEach var="item" items="${myResignDoneList}">
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
			<%-- 내 퇴직 처리 결과 페이징 --%>
			<div class="pagination">
				<c:set var="myResignDoneBase"
					value="myResignDoneStatus=${myResignDoneStatus}" />
				<c:choose>
					<c:when test="${myResignDoneNowBlock > 1}">
						<c:set var="prev"
							value="${(myResignDoneNowBlock - 2) * myResignDonePagePerBlock + 1}" />
						<a
							href="${pageContext.request.contextPath}/emp/approvalHistory?${myResignDoneBase}&myResignDonePage=${prev}">◀</a>
					</c:when>
					<c:otherwise>
						<a class="disabled">◀</a>
					</c:otherwise>
				</c:choose>
				<c:forEach var="p" begin="${myResignDonePageStart}"
					end="${myResignDonePageEnd}">
					<c:choose>
						<c:when test="${p == myResignDoneNowPage}">
							<a class="active">${p}</a>
						</c:when>
						<c:otherwise>
							<a
								href="${pageContext.request.contextPath}/emp/approvalHistory?${myResignDoneBase}&myResignDonePage=${p}">${p}</a>
						</c:otherwise>
					</c:choose>
				</c:forEach>
				<c:choose>
					<c:when test="${myResignDoneTotalBlock > myResignDoneNowBlock}">
						<c:set var="next"
							value="${myResignDoneNowBlock * myResignDonePagePerBlock + 1}" />
						<a
							href="${pageContext.request.contextPath}/emp/approvalHistory?${myResignDoneBase}&myResignDonePage=${next}">▶</a>
					</c:when>
					<c:otherwise>
						<a class="disabled">▶</a>
					</c:otherwise>
				</c:choose>
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
