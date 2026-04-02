<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<c:set var="isAdmin"
	value="${sessionScope.userRole == '관리자' || sessionScope.userRole == 'HR담당자'}" />

<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/org/dept.css">

<style>
/* 기존 error-toast (기본 빨간색) */
.error-toast {
	background-color: #fff1f0;
	color: #cf1322;
	border: 1px solid #ffa39e;
	padding: 12px 20px;
	margin-bottom: 15px;
	border-radius: 6px;
	font-weight: 500;
	display: flex;
	justify-content: space-between;
	align-items: center;
	animation: slideDown 0.3s ease;
}
/* 성공 시 연두색 스타일 추가 */
.error-toast.success-toast {
	background-color: #f6ffed !important;
	color: #389e0d !important;
	border: 1px solid #b7eb8f !important;
}

.btn-msg-close {
	cursor: pointer;
	font-size: 18px;
	line-height: 1;
	margin-left: 10px;
}

@
keyframes slideDown {from { transform:translateY(-10px);
	opacity: 0;
}

to {
	transform: translateY(0);
	opacity: 1;
}
}
</style>

<%-- ── 상단 메시지 영역 (성공/실패 색상 구분 적용) ── --%>
<c:if test="${not empty param.error}">
	<div
		class="error-toast ${param.error == 'success' ? 'success-toast' : ''}"
		id="statusMsg">
		<span> <c:choose>
				<c:when test="${param.error == 'success'}">✅ 정보가 성공적으로 반영되었습니다.</c:when>
				<c:when test="${param.error == 'has_members'}">❌ 해당 부서에 소속 직원이 있어 처리할 수 없습니다.</c:when>
				<c:when test="${param.error == 'has_children'}">❌ 하위 부서가 존재하는 경우 폐지할 수 없습니다.</c:when>
				<c:when test="${param.error == 'level_exceeded'}">❌ 조직도는 최대 5단계까지만 생성 가능합니다.</c:when>
				<c:when test="${param.error == 'has_members_inactive'}">❌ 인원이 소속된 부서는 비활성화할 수 없습니다.</c:when>
				<c:otherwise>⚠️ 오류가 발생했습니다 (${param.error}).</c:otherwise>
			</c:choose>
		</span> <span class="btn-msg-close"
			onclick="this.parentElement.style.display='none'">×</span>
	</div>
</c:if>

<div class="dept-container">
	<%-- ══════════════════════════════════════════
         좌측: 조직도 트리 패널 (5단계 + 동적 아이콘)
         ══════════════════════════════════════════ --%>
	<div class="dept-tree-panel">
		<div class="panel-header">
			<span class="panel-title">🟢 조직도</span>
			<c:if test="${isAdmin}">
				<button class="btn-add-dept"
					onclick="location.href='${pageContext.request.contextPath}/org/dept?action=new'">+
					부서 추가</button>
			</c:if>
		</div>

		<%-- 사원 검색 --%>
		<div class="dept-search-box"
			style="margin-bottom: 10px; display: flex;">
			<input type="text" id="empSearchInput" placeholder="사원명으로 부서 찾기..."
				style="flex: 1; padding: 5px;"
				onkeyup="if(event.keyCode==13)searchDeptByEmp()">
			<button type="button" onclick="searchDeptByEmp()">🔍</button>
		</div>

		<c:if test="${isAdmin}">
			<div class="dept-tabs-container">
				<div class="dept-tabs-wrapper">
					<div class="dept-tab active"
						onclick="switchDeptTab(this, 'active-tree')">활성 조직도</div>
					<div class="dept-tab"
						onclick="switchDeptTab(this, 'inactive-list')">비활성 부서</div>
				</div>
			</div>
		</c:if>

		<div id="active-tree" class="tab-content active">
			<ul class="org-tree">
				<li>
					<div class="tree-company">
						<span class="icon-company">🏢</span> <span>${not empty companyName ? companyName : 'HR ERP SYSTEM'}</span>
					</div>
					<ul class="tree-group">
						<c:forEach var="lv1" items="${deptTree}">
							<li>
								<div
									class="tree-dept-row ${selectedDeptId == lv1.deptId ? 'active' : ''}"
									onclick="location.href='?deptId=${lv1.deptId}'">
									<span
										class="${empty lv1.children ? 'icon-team' : 'icon-folder'}">${empty lv1.children ? '👤' : '📁'}</span>
									<span>${lv1.deptName}</span>
									<c:if test="${not empty lv1.managerName}">
										<span class="manager-name">${lv1.managerName}</span>
									</c:if>
								</div> <c:if test="${not empty lv1.children}">
									<ul class="tree-team-list">
										<c:forEach var="lv2" items="${lv1.children}">
											<li>
												<div
													class="tree-team-row ${selectedDeptId == lv2.deptId ? 'active' : ''}"
													onclick="location.href='?deptId=${lv2.deptId}'">
													<span
														class="${empty lv2.children ? 'icon-team' : 'icon-folder'}">${empty lv2.children ? '👤' : '📁'}</span>
													<span>${lv2.deptName}</span>
													<c:if test="${not empty lv2.managerName}">
														<span class="manager-name">${lv2.managerName}</span>
													</c:if>
												</div> <c:if test="${not empty lv2.children}">
													<ul class="tree-team-list">
														<c:forEach var="lv3" items="${lv2.children}">
															<li>
																<div
																	class="tree-team-row ${selectedDeptId == lv3.deptId ? 'active' : ''}"
																	onclick="location.href='?deptId=${lv3.deptId}'">
																	<span
																		class="${empty lv3.children ? 'icon-team' : 'icon-folder'}">${empty lv3.children ? '👤' : '📁'}</span>
																	<span>${lv3.deptName}</span>
																	<c:if test="${not empty lv3.managerName}">
																		<span class="manager-name">${lv3.managerName}</span>
																	</c:if>
																</div> <c:if test="${not empty lv3.children}">
																	<ul class="tree-team-list">
																		<c:forEach var="lv4" items="${lv3.children}">
																			<li>
																				<div
																					class="tree-team-row ${selectedDeptId == lv4.deptId ? 'active' : ''}"
																					onclick="location.href='?deptId=${lv4.deptId}'">
																					<span
																						class="${empty lv4.children ? 'icon-team' : 'icon-folder'}">${empty lv4.children ? '👤' : '📁'}</span>
																					<span>${lv4.deptName}</span>
																					<c:if test="${not empty lv4.managerName}">
																						<span class="manager-name">${lv4.managerName}</span>
																					</c:if>
																				</div> <c:if test="${not empty lv4.children}">
																					<ul class="tree-team-list">
																						<c:forEach var="lv5" items="${lv4.children}">
																							<li>
																								<div
																									class="tree-team-row ${selectedDeptId == lv5.deptId ? 'active' : ''}"
																									onclick="location.href='?deptId=${lv5.deptId}'">
																									<span class="icon-team">👤</span> <span>${lv5.deptName}</span>
																									<c:if test="${not empty lv5.managerName}">
																										<span class="manager-name">${lv5.managerName}</span>
																									</c:if>
																								</div>
																							</li>
																						</c:forEach>
																					</ul>
																				</c:if>
																			</li>
																		</c:forEach>
																	</ul>
																</c:if>
															</li>
														</c:forEach>
													</ul>
												</c:if>
											</li>
										</c:forEach>
									</ul>
								</c:if>
							</li>
						</c:forEach>
					</ul>
				</li>
			</ul>
		</div>

		<c:if test="${isAdmin}">
			<div id="inactive-list" class="tab-content" style="display: none;">
				<ul class="org-tree">
					<c:choose>
						<c:when test="${not empty inactiveDepts}">
							<c:forEach var="idept" items="${inactiveDepts}">
								<li>
									<div
										class="tree-dept-row ${selectedDeptId == idept.dept_id ? 'active' : ''}"
										onclick="location.href='?deptId=${idept.dept_id}'">
										<span class="icon-team icon-inactive">👤</span> <span
											class="inactive-text">${idept.dept_name}</span> <small
											class="closed-date">${idept.closed_at}</small>
									</div>
								</li>
							</c:forEach>
						</c:when>
						<c:otherwise>
							<div class="empty-state">비활성화된 부서가 없습니다.</div>
						</c:otherwise>
					</c:choose>
				</ul>
			</div>
		</c:if>
	</div>

	<%-- 우측 패널 (3열 그리드 + 소속 직원) --%>
	<div class="dept-detail-panel">
		<div class="dept-info-card">
			<div class="card-title">
				📋 부서 정보
				<c:if test="${not empty selectedDept.dept_name}"> · ${selectedDept.dept_name}</c:if>
			</div>
			<form action="${pageContext.request.contextPath}/org/dept"
				method="post" id="deptForm">
				<input type="hidden" name="action" id="formAction" value="save">
				<input type="hidden" name="dept_id" value="${selectedDept.dept_id}">

				<div class="dept-form-grid"
					style="grid-template-columns: repeat(3, 1fr);">
					<div class="dept-form-group">
						<label>부서명 <span class="required">*</span></label> <input
							type="text" name="dept_name" value="${selectedDept.dept_name}"
							required ${notisAdmin ? 'disabled' : ''}>
					</div>
					<div class="dept-form-group">
						<label>상위 부서</label> <select name="parent_dept_id"
							${not isAdmin ? 'disabled' : ''}>
							<option value="0">없음 (최상위)</option>
							<c:forEach var="d" items="${allDepts}">
								<c:if test="${d.dept_id != selectedDept.dept_id}">
									<option value="${d.dept_id}"
										${selectedDept.parent_dept_id == d.dept_id ? 'selected' : ''}>${d.dept_name}</option>
								</c:if>
							</c:forEach>
						</select>
					</div>
					<div class="dept-form-group">
						<label>부서장</label> <select name="manager_id"
							${not isAdmin ? 'disabled' : ''}>
							<option value="0">미지정</option>
							<c:forEach var="emp" items="${empList}">
								<option value="${emp.empId}"
									${selectedDept.manager_id == emp.empId ? 'selected' : ''}>${emp.empName}
									(${emp.posName})</option>
							</c:forEach>
						</select>
					</div>

					<c:if test="${isAdmin}">
						<div class="dept-form-group">
							<label>출력 순서</label> <input type="number" name="sort_order"
								value="${selectedDept.sort_order > 0 ? selectedDept.sort_order : 1}">
						</div>
						<div class="dept-form-group">
							<label>계층 깊이</label> <input type="text"
								value="${selectedDept.dept_level > 0 ? selectedDept.dept_level : '-'}"
								readonly style="background-color: #f9f9f9;">
						</div>
						<div class="dept-form-group">
							<label>상태</label> <select name="is_active">
								<option value="1"
									${selectedDept.is_active == 1 ? 'selected' : ''}>활성</option>
								<option value="0"
									${selectedDept.is_active == 0 ? 'selected' : ''}>비활성</option>
							</select>
						</div>
					</c:if>
				</div>

				<div class="dept-btn-area">
					<c:choose>
						<c:when test="${isAdmin}">
							<%-- 
                 [수정 사항] 
                 1. 기존 부서여야 함 (dept_id > 0)
                 2. 현재 '활성' 상태인 부서여야 함 (is_active == 1)
                 비활성 탭에서 보고 있는 부서는 이미 폐지 대기/비활성 상태이므로 버튼을 숨깁니다.
            --%>
							<c:if
								test="${selectedDept.dept_id > 0 && selectedDept.is_active == 1}">
								<button type="button" class="btn-dept-delete"
									onclick="submitAction('delete')">부서 폐지</button>
							</c:if>

							<button type="button" class="btn-dept-cancel"
								onclick="location.href='${pageContext.request.contextPath}/org/dept'">취소</button>
							<button type="button" class="btn-dept-save"
								onclick="submitAction('save')">저장</button>
						</c:when>
						<c:otherwise>
							<span class="auth-notice">🔒 부서 정보 수정은 관리자만 가능합니다.</span>
						</c:otherwise>
					</c:choose>
				</div>
			</form>
		</div>

		<div class="dept-member-card">
			<div class="card-title">
				👥 소속 직원 <span class="member-count">(${not empty memberList ? memberList.size() : 0}명)</span>
			</div>
			<c:choose>
				<c:when test="${not empty memberList}">
					<table class="member-table">
						<thead>
							<tr>
								<th>사번</th>
								<th>이름</th>
								<th>직급</th>
								<th>상태</th>
							</tr>
						</thead>
						<tbody>
							<c:forEach var="member" items="${memberList}">
								<tr>
									<td>${member.empNo}</td>
									<td>${member.empName}</td>
									<td>${member.posName}</td>
									<td><c:choose>
											<c:when test="${member.status == '재직'}">
												<span class="badge-active">재직</span>
											</c:when>
											<c:when test="${member.status == '휴직'}">
												<span class="badge-leave">휴직</span>
											</c:when>
											<c:otherwise>
												<span class="badge-inactive">퇴직</span>
											</c:otherwise>
										</c:choose></td>
								</tr>
							</c:forEach>
						</tbody>
					</table>
				</c:when>
				<c:otherwise>
					<div class="empty-state">소속 직원이 없습니다.</div>
				</c:otherwise>
			</c:choose>
		</div>
	</div>
</div>

<script>
function switchDeptTab(el, tabId) {
    document.querySelectorAll('.dept-tab').forEach(t => t.classList.remove('active'));
    el.classList.add('active');
    document.querySelectorAll('.tab-content').forEach(c => {
        c.style.display = 'none';
        c.classList.remove('active');
    });
    const target = document.getElementById(tabId);
    if(target) {
        target.style.display = 'block';
        target.classList.add('active');
    }
}
function submitAction(action) {
    if (action === 'delete' && !confirm('해당 부서를 폐지하시겠습니까?')) return;
    document.getElementById('formAction').value = action;
    document.getElementById('deptForm').submit();
}
function searchDeptByEmp() {
    const name = document.getElementById('empSearchInput').value;
    if(!name) return;
    fetch('${pageContext.request.contextPath}/org/dept?action=findDeptByEmp&empName=' + encodeURIComponent(name))
        .then(r => r.json()).then(d => {
            if(d.deptId > 0) location.href = '?deptId=' + d.deptId;
            else alert("사원을 찾을 수 없습니다.");
        });
}
window.onload = function() {
    const urlParams = new URLSearchParams(window.location.search);
    const hasDeptId = urlParams.has('deptId');
    <c:if test="${not empty selectedDept}">
        if (hasDeptId && ${selectedDept.is_active == 0}) {
            const inactiveTabBtn = document.querySelectorAll('.dept-tab')[1];
            if(inactiveTabBtn) switchDeptTab(inactiveTabBtn, 'inactive-list');
        } else {
            const activeTabBtn = document.querySelectorAll('.dept-tab')[0];
            if(activeTabBtn) switchDeptTab(activeTabBtn, 'active-tree');
        }
    </c:if>
}
</script>