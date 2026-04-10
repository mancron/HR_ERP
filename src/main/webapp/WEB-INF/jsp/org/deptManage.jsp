<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="isAdmin" value="${isPrivileged eq 'true'}" />
<c:set var="isNewMode"
	value="${param.action eq 'new' || selectedDept == null || selectedDept.dept_id eq 0}" />

<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/org/dept.css">
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/style.css">

<%-- ══ 토스트 메시지 영역 (기존 유지) ══ --%>
<c:if test="${not empty param.error || not empty param.msg}">
	<c:set var="isError" value="${not empty param.error}" />
	<div class="status-toast ${isError ? 'toast-error' : 'toast-success'}"
		id="statusMsg">
		<span class="toast-content"> <c:choose>
				<c:when test="${param.error == 'no_auth'}">❌ 권한이 없습니다.</c:when>
				<c:when test="${param.error == 'no_dept_name'}">❌ 부서명은 필수 입력 항목입니다.</c:when>
				<c:when test="${param.error == 'self_parent'}">❌ 자기 자신을 상위 부서로 지정할 수 없습니다.</c:when>
				<c:when test="${param.error == 'circular_reference'}">❌ 순환 참조: 하위 부서를 상위로 지정할 수 없습니다.</c:when>
				<c:when test="${param.error == 'level_exceeded'}">❌ 조직도는 최대 5단계까지만 구성 가능합니다.</c:when>
				<c:when test="${param.error == 'has_members'}">❌ 재직 중인 직원이 있어 폐지할 수 없습니다.</c:when>
				<c:when test="${param.error == 'has_children'}">❌ 하위 부서가 존재하여 폐지할 수 없습니다.</c:when>
				<c:when test="${param.error == 'has_members_inactive'}">⚠️ 소속 직원이 있는 부서는 비활성화할 수 없습니다.</c:when>
				<c:when test="${param.error == 'has_children_inactive'}">⚠️ 하위 부서가 있는 부서는 비활성화할 수 없습니다.</c:when>
				<c:when test="${param.error == 'sort_order_limit'}">❌ 출력 순서는 최대 99까지만 가능합니다.</c:when>
				<c:when test="${param.error == 'fail'}">❌ 처리 중 오류가 발생했습니다. 다시 시도해 주세요.</c:when>
				<c:when test="${param.msg == 'success'}">✅ 성공적으로 반영되었습니다.</c:when>
				<c:when test="${param.msg == 'deleted'}">✅ 부서가 폐지되었습니다.</c:when>
				<c:otherwise>
					<c:choose>
						<c:when test="${not empty param.error}">⚠️ ${param.error}</c:when>
						<c:otherwise>⚠️ ${param.msg}</c:otherwise>
					</c:choose>
				</c:otherwise>
			</c:choose>
		</span> <span class="btn-toast-close" id="closeToastBtn">×</span>
	</div>
</c:if>

<div class="dept-container">
	<%-- ══ 왼쪽: 조직도 트리 패널 ══ --%>
	<div class="dept-tree-panel">
		<div class="panel-header">
			<span class="panel-title">🌳 조직도</span>
			<c:if test="${isAdmin}">
				<button class="btn-add-dept"
					onclick="location.href='${pageContext.request.contextPath}/org/dept?action=new'">+
					부서 추가</button>
			</c:if>
		</div>

		<div class="dept-search-box">
			<input type="text" id="empSearchInput" placeholder="사원명으로 부서 찾기..."
				onkeyup="if(event.keyCode===13) searchDeptByEmp()">
			<button type="button" onclick="searchDeptByEmp()">🔍</button>
		</div>

		<c:if test="${isAdmin}">
			<div class="dept-tabs-container">
				<div class="dept-tabs-wrapper">
					<div class="dept-tab active" data-tab="active-tree">활성</div>
					<div class="dept-tab" data-tab="inactive-list">비활성</div>
				</div>
			</div>
		</c:if>

		<%-- [리팩토링] 활성 트리: JS 재귀 함수가 이 안을 채웁니다. --%>
		<div id="active-tree" class="tab-content active">
			<ul class="org-tree" id="deptTreeContainer">
				<%-- 자바스크립트에서 동적으로 생성됨 --%>
			</ul>
		</div>

		<%-- 비활성 목록 (기존 유지) --%>
		<c:if test="${isAdmin}">
			<div id="inactive-list" class="tab-content inactive-tab-hidden">
				<c:choose>
					<c:when test="${not empty inactiveDepts}">
						<ul class="org-tree">
							<c:forEach var="idept" items="${inactiveDepts}">
								<li class="tree-node">
									<div
										class="tree-row ${selectedDeptId == idept.dept_id ? 'active' : ''}"
										onclick="location.href='?deptId=${idept.dept_id}'">
										<span class="tree-icon">🚫</span> <span
											class="tree-text text-muted">${idept.dept_name}</span>
									</div>
								</li>
							</c:forEach>
						</ul>
					</c:when>
					<c:otherwise>
						<div class="empty-state">비활성 부서가 없습니다.</div>
					</c:otherwise>
				</c:choose>
			</div>
		</c:if>
	</div>

	<%-- ══ 오른쪽: 상세 정보 패널 (기존 유지) ══ --%>
	<div class="dept-detail-panel">
		<div class="dept-info-card">
			<div class="card-title">
				📝 부서 정보
				<c:if test="${isNewMode}">
					<span class="new-mode-badge">(신규 등록)</span>
				</c:if>
			</div>

			<form action="${pageContext.request.contextPath}/org/dept"
				method="post" id="deptForm">
				<input type="hidden" name="action" id="formAction"
					value="${isNewMode ? 'insert' : 'update'}"> <input
					type="hidden" name="dept_id"
					value="${isNewMode ? '' : selectedDept.dept_id}">

				<div class="dept-form-grid">
					<div class="dept-form-group full-width">
						<label>부서명 <span class="required">*</span></label> <input
							type="text" name="dept_name" id="deptNameInput"
							value="${isNewMode ? '' : selectedDept.dept_name}"
							placeholder="${isNewMode ? '신규 부서명을 입력하세요' : ''}"
							${!isAdmin ? 'readonly' : ''}> <span
							class="field-error-msg" id="deptNameError">부서명은 필수입니다.</span>
					</div>

					<div class="dept-form-group">
						<label>상위 부서</label>
						<c:choose>
							<c:when test="${isAdmin}">
								<select name="parent_dept_id" id="parentDeptSelect">
									<option value="0">없음 (최상위)</option>
									<c:forEach var="d" items="${allDepts}">
										<%-- 1. 활성 부서인지 확인 --%>
										<c:if test="${d.is_active == 1}">
											<c:set var="isForbidden" value="false" />

											<%-- 2. 본인인지 확인 (수정 모드일 때만) --%>
											<c:if
												test="${!isNewMode && d.dept_id == selectedDept.dept_id}">
												<c:set var="isForbidden" value="true" />
											</c:if>

											<%-- 3. 서블릿에서 보낸 하위 부서 ID 리스트(childIds)에 포함되는지 확인 --%>
											<c:forEach var="cid" items="${childIds}">
												<c:if test="${d.dept_id == cid}">
													<c:set var="isForbidden" value="true" />
												</c:if>
											</c:forEach>

											<%-- 금지된 대상(본인/하위)이 아닌 경우만 옵션으로 노출 --%>
											<c:if test="${!isForbidden}">
												<option value="${d.dept_id}"
													${selectedDept.parent_dept_id == d.dept_id ? 'selected' : ''}>
													${d.dept_name}</option>
											</c:if>
										</c:if>
									</c:forEach>
								</select>
							</c:when>
							<c:otherwise>
								<%-- 일반 사용자용 조회 모드 (기존 유지) --%>
								<c:set var="parentName" value="없음 (최상위)" />
								<c:forEach var="d" items="${allDepts}">
									<c:if test="${d.dept_id == selectedDept.parent_dept_id}">
										<c:set var="parentName" value="${d.dept_name}" />
									</c:if>
								</c:forEach>
								<input type="text" value="${parentName}" readonly>
								<input type="hidden" name="parent_dept_id"
									value="${selectedDept.parent_dept_id}">
							</c:otherwise>
						</c:choose>
					</div>

					<div class="dept-form-group">
						<label>부서장</label> <input type="text" class="readonly-text"
							value="${isNewMode ? '자동지정' : (not empty selectedDept.manager_name ? selectedDept.manager_name : '미지정')}"
							readonly title="부서장은 인사발령 메뉴에서 변경합니다">
					</div>

					<c:if test="${isAdmin}">
						<div class="dept-form-group">
							<label>출력 순서</label> <input type="number" name="sort_order"
								id="sortOrderInput" min="1" max="99"
								value="${selectedDept.sort_order > 0 ? selectedDept.sort_order : 1}">
						</div>
						<div class="dept-form-group">
							<label>계층 깊이</label> <input type="text" id="deptLevelDisplay"
								readonly
								value="${!isNewMode && selectedDept.dept_level > 0 ? selectedDept.dept_level : (isNewMode ? '1' : '-')}단계">
						</div>
						<div class="dept-form-group">
							<label>상태</label>
							<c:choose>
								<c:when test="${!isNewMode && selectedDept.is_active == 0}">
									<select name="is_active"><option value="1">활성
											(복구)</option>
										<option value="0" selected>비활성 유지</option></select>
								</c:when>
								<c:otherwise>
									<input type="text" value="활성" readonly
										class="status-active-input">
									<input type="hidden" name="is_active" value="1">
								</c:otherwise>
							</c:choose>
						</div>
						<c:if test="${!isNewMode && not empty selectedDept.created_at}">
							<div class="dept-form-group">
								<label>생성일</label> <input type="text"
									value="${selectedDept.created_at}" readonly>
							</div>
						</c:if>
						<c:if test="${!isNewMode && not empty selectedDept.closed_at}">
							<div class="dept-form-group">
								<label>폐지일</label>
								<fmt:parseDate value="${selectedDept.closed_at}"
									pattern="yyyy-MM-dd" var="parsedClosedAt" />
								<fmt:formatDate value="${parsedClosedAt}"
									pattern="yyyy-MM-dd 00:00:00" var="formattedClosedAt" />
								<input type="text" value="${formattedClosedAt}" readonly
									class="closed-date-input">
							</div>
						</c:if>
					</c:if>
				</div>

				<div class="dept-btn-area">
					<c:if
						test="${isAdmin && !isNewMode && selectedDept.dept_id > 0 && selectedDept.is_active == 1}">
						<button type="button" class="btn-dept-delete"
							onclick="submitAction('delete')">부서 폐지</button>
					</c:if>
					<c:if test="${isAdmin}">
						<button type="button" class="btn-dept-cancel"
							onclick="location.href='${pageContext.request.contextPath}/org/dept'">취소</button>
						<button type="button" class="btn-dept-save"
							onclick="submitAction('submit')">저장</button>
					</c:if>
				</div>
			</form>
		</div>

		<%-- 소속 직원 목록 카드 (기존 유지) --%>
		<div class="dept-member-card">
			<div class="card-title">
				👥 소속 직원 <span class="member-count">${not empty memberList ? memberList.size() : 0}명</span>
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
							<c:forEach var="m" items="${memberList}">
								<tr>
									<td class="emp-no-cell">${m.empNo}</td>
									<td class="emp-name-cell">${m.empName}</td>
									<td>${m.posName}</td>
									<td><c:choose>
											<c:when test="${m.status == '재직'}">
												<span class="badge-active">재직</span>
											</c:when>
											<c:when test="${m.status == '휴직'}">
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
					<div class="empty-state">${isNewMode ? '부서 등록 후 직원을 배정할 수 있습니다.' : '소속 직원이 없습니다.'}</div>
				</c:otherwise>
			</c:choose>
		</div>
	</div>
</div>

<%-- ══ 검색 결과 모달 (기존 유지) ══ --%>
<div id="empModalOverlay" class="emp-modal-overlay">
	<div class="emp-modal-content">
		<div class="emp-modal-header">
			<h3>👥 검색 결과 선택</h3>
			<span class="emp-modal-close" id="empModalClose">&times;</span>
		</div>
		<div class="emp-modal-body" id="empModalBody"></div>
	</div>
</div>

<script>
// 1. 트리 데이터 준비 (JSTL 데이터를 자바스크립트 객체로 변환)
const deptData = [
    <c:forEach var="lv1" items="${deptTree}" varStatus="s1">
    {
        deptId: "${lv1.deptId}", deptName: "${lv1.deptName}", managerName: "${lv1.managerName}",
        children: [
            <c:forEach var="lv2" items="${lv1.children}" varStatus="s2">
            {
                deptId: "${lv2.deptId}", deptName: "${lv2.deptName}", managerName: "${lv2.managerName}",
                children: [
                    <c:forEach var="lv3" items="${lv2.children}" varStatus="s3">
                    {
                        deptId: "${lv3.deptId}", deptName: "${lv3.deptName}", managerName: "${lv3.managerName}",
                        children: [
                            <c:forEach var="lv4" items="${lv3.children}" varStatus="s4">
                            {
                                deptId: "${lv4.deptId}", deptName: "${lv4.deptName}", managerName: "${lv4.managerName}",
                                children: [
                                    <c:forEach var="lv5" items="${lv4.children}" varStatus="s5">
                                    { deptId: "${lv5.deptId}", deptName: "${lv5.deptName}", managerName: "${lv5.managerName}", children: [] }${!s5.last ? ',' : ''}
                                    </c:forEach>
                                ]
                            }${!s4.last ? ',' : ''}
                            </c:forEach>
                        ]
                    }${!s3.last ? ',' : ''}
                    </c:forEach>
                ]
            }${!s2.last ? ',' : ''}
            </c:forEach>
        ]
    }${!s1.last ? ',' : ''}
    </c:forEach>
];

const selectedDeptId = "${selectedDeptId}";
const companyName = "${not empty companyName ? companyName : '(주)예시회사'}";

// 2. 트리 재귀 생성 함수 (리팩토링 핵심)
function buildTree(nodes, isRoot = false) {
    if (!nodes || nodes.length === 0) return "";
    let html = `<ul class="\${isRoot ? 'tree-group' : 'tree-sub'}">`;
    nodes.forEach((node, idx) => {
        const isLast = idx === nodes.length - 1;
        const hasChildren = node.children && node.children.length > 0;
        const activeClass = (selectedDeptId == node.deptId) ? 'active' : '';
        const icon = hasChildren ? '📁' : '📄';

        html += `
            <li class="tree-node \${isLast ? 'last-node' : ''}">
                <div class="tree-row \${activeClass}" onclick="location.href='?deptId=\${node.deptId}'">
                    <span class="tree-icon">\${icon}</span>
                    <span class="tree-text">\${node.deptName}</span>
                    \${node.managerName ? `<span class="tree-manager">\${node.managerName}</span>` : ''}
                </div>
                \${buildTree(node.children, false)}
            </li>`;
    });
    html += "</ul>";
    return html;
}

// 3. 트리 렌더링 실행
function renderTree() {
    const container = document.getElementById('deptTreeContainer');
    if (!container) return;
    // 루트 회사 노드부터 시작
    const rootHtml = `
        <li class="tree-node root-node">
            <div class="tree-company">
                <span class="tree-icon">🏢</span> <span class="tree-text">\${companyName}</span>
            </div>
            \${buildTree(deptData, true)}
        </li>`;
    container.innerHTML = rootHtml;
}

// [기존 JS 로직 유지]
const deptLevelMap = { "0": 0, <c:forEach var="d" items="${allDepts}" varStatus="s">"${d.dept_id}": ${d.dept_level}${!s.last ? ',' : ''}</c:forEach> };

function updateDeptLevel() {
    const select = document.getElementById('parentDeptSelect');
    const display = document.getElementById('deptLevelDisplay');
    if (!select || !display) return;
    display.value = (parseInt(deptLevelMap[select.value] || 0) + 1) + "단계";
}

function initTabs() {
    const tabs = document.querySelectorAll('.dept-tab');
    tabs.forEach(tab => {
        tab.addEventListener('click', function() {
            tabs.forEach(t => t.classList.remove('active'));
            this.classList.add('active');
            document.querySelectorAll('.tab-content').forEach(c => { c.classList.remove('active'); c.classList.add('inactive-tab-hidden'); });
            document.getElementById(this.dataset.tab).classList.replace('inactive-tab-hidden', 'active');
        });
    });
}

function validateForm() {
    const input = document.getElementById('deptNameInput');
    if (input && !input.value.trim()) {
        input.classList.add('input-error');
        document.getElementById('deptNameError').style.display = 'block';
        input.focus();
        return false;
    }
    return true; 
}

function submitAction(action) {
    const form = document.getElementById('deptForm');
    if (action === 'delete') { if (!confirm('정말 폐지하시겠습니까?')) return; document.getElementById('formAction').value = 'delete'; }
    else { if (!validateForm()) return; }
    form.submit();
}

function searchDeptByEmp() {
    const name = document.getElementById('empSearchInput').value.trim();
    if (!name) return;
    fetch('${pageContext.request.contextPath}/org/dept?action=findDeptByEmp&empName=' + encodeURIComponent(name))
        .then(r => r.json()).then(d => {
            if (d.status === 'success') location.href = '?deptId=' + d.deptId;
            else if (d.status === 'multiple') openEmpModal(d.list);
            else alert('사원을 찾을 수 없습니다.');
        });
}

function openEmpModal(list) {
    const body = document.getElementById('empModalBody');
    body.innerHTML = list.map(e => `
        <div class="emp-select-item" onclick="location.href='?deptId=\${e.deptId}'">
            <span class="dept-label">\${e.deptName || '부서미정'}</span>
            <span class="info-label">\${e.posName} | \${e.empName} (\${e.empNo})</span>
        </div>`).join('');
    document.getElementById('empModalOverlay').style.display = 'flex';
}

document.addEventListener('DOMContentLoaded', () => {
    renderTree(); // 리팩토링된 트리 호출
    initTabs();
    if (document.getElementById('parentDeptSelect')) document.getElementById('parentDeptSelect').onchange = updateDeptLevel;
    if (document.getElementById('closeToastBtn')) document.getElementById('closeToastBtn').onclick = () => document.getElementById('statusMsg').style.display = 'none';
    document.getElementById('empModalClose').onclick = () => document.getElementById('empModalOverlay').style.display = 'none';
    
    // 비활성 부서 조회 시 탭 전환
    <c:if test="${isAdmin && not empty selectedDept && selectedDept.is_active == 0}">
        document.querySelector('.dept-tab[data-tab="inactive-list"]').click();
    </c:if>

    // 토스트 자동 숨김
    const toast = document.getElementById('statusMsg');
    if (toast) setTimeout(() => { toast.style.opacity = '0'; setTimeout(() => toast.style.display = 'none', 400); }, 3500);
});
</script>
</body>
</html>