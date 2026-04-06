<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="isAdmin" value="${isPrivileged}" />
<c:set var="isNewMode" value="${param.action eq 'new' || empty selectedDept.dept_id || selectedDept.dept_id eq 0}" />

<link rel="stylesheet" href="${pageContext.request.contextPath}/css/org/dept.css">

<%-- 1. 토스트 메시지 영역 --%>
<c:if test="${not empty param.error || not empty param.msg}">
    <c:set var="isError" value="${not empty param.error}" />
    <div class="status-toast ${isError ? 'toast-error' : 'toast-success'}" id="statusMsg">
        <span class="toast-content">
            <c:choose>
                <c:when test="${param.error == 'no_auth'}">❌ 권한이 없습니다.</c:when>
                <c:when test="${param.error == 'no_dept_name'}">❌ 부서명은 필수입니다.</c:when>
                <c:when test="${param.error == 'circular_reference'}">❌ 순환 참조 오류</c:when>
                <c:when test="${param.error == 'level_exceeded'}">❌ 최대 5단계까지만 구성 가능합니다.</c:when>
                <c:when test="${param.error == 'has_members'}">❌ 직원이 있어 폐지할 수 없습니다.</c:when>
                <c:when test="${param.error == 'has_children'}">❌ 하위 부서가 있어 폐지할 수 없습니다.</c:when>
                <c:when test="${param.msg == 'success'}">✅ 성공적으로 반영되었습니다.</c:when>
                <c:when test="${param.msg == 'deleted'}">✅ 부서가 폐지되었습니다.</c:when>
                <c:otherwise>⚠️ ${param.error}${param.msg}</c:otherwise>
            </c:choose>
        </span>
        <span class="btn-toast-close" onclick="this.parentElement.style.display='none'">×</span>
    </div>
</c:if>

<div class="dept-container">
    <div class="dept-tree-panel">
        <div class="panel-header">
            <span class="panel-title">🌳 조직도</span>
            <c:if test="${isAdmin}">
                <button class="btn-add-dept" onclick="location.href='${pageContext.request.contextPath}/org/dept?action=new'">+ 부서 추가</button>
            </c:if>
        </div>

        <div class="dept-search-box">
            <input type="text" id="empSearchInput" placeholder="사원명으로 부서 찾기..." onkeyup="if(event.keyCode===13) searchDeptByEmp()">
            <button type="button" onclick="searchDeptByEmp()">🔍</button>
        </div>

        <c:if test="${isAdmin}">
            <div class="dept-tabs-container">
                <div class="dept-tabs-wrapper">
                    <div class="dept-tab active" onclick="switchDeptTab(this,'active-tree')">활성</div>
                    <div class="dept-tab" onclick="switchDeptTab(this,'inactive-list')">비활성</div>
                </div>
            </div>
        </c:if>

        <div id="active-tree" class="tab-content active">
            <ul class="org-tree">
                <li>
                    <div class="tree-company">🏢 ${not empty companyName ? companyName : '(주)예시회사'}</div>
                    <ul class="tree-group">
                        <c:forEach var="lv1" items="${deptTree}" varStatus="s1">
                            <li class="tree-node ${s1.last ? 'last-node' : ''}">
                                <div class="tree-row ${selectedDeptId == lv1.deptId ? 'active' : ''}" onclick="location.href='?deptId=${lv1.deptId}'">
                                    <span class="tree-icon">${not empty lv1.children ? '📁' : '📄'}</span>
                                    <span class="tree-text">${lv1.deptName}</span>
                                    <c:if test="${not empty lv1.managerName}"><span class="tree-manager">${lv1.managerName}</span></c:if>
                                </div>
                                <c:if test="${not empty lv1.children}">
                                    <ul class="tree-sub">
                                        <c:forEach var="lv2" items="${lv1.children}" varStatus="s2">
                                            <li class="tree-node ${s2.last ? 'last-node' : ''}">
                                                <div class="tree-row ${selectedDeptId == lv2.deptId ? 'active' : ''}" onclick="location.href='?deptId=${lv2.deptId}'">
                                                    <span class="tree-icon">${not empty lv2.children ? '📁' : '📄'}</span>
                                                    <span class="tree-text">${lv2.deptName}</span>
                                                    <c:if test="${not empty lv2.managerName}"><span class="tree-manager">${lv2.managerName}</span></c:if>
                                                </div>
                                                <c:if test="${not empty lv2.children}">
                                                    <ul class="tree-sub">
                                                        <c:forEach var="lv3" items="${lv2.children}" varStatus="s3">
                                                            <li class="tree-node ${s3.last ? 'last-node' : ''}">
                                                                <div class="tree-row ${selectedDeptId == lv3.deptId ? 'active' : ''}" onclick="location.href='?deptId=${lv3.deptId}'">
                                                                    <span class="tree-icon">${not empty lv3.children ? '📁' : '📄'}</span>
                                                                    <span class="tree-text">${lv3.deptName}</span>
                                                                    <c:if test="${not empty lv3.managerName}"><span class="tree-manager">${lv3.managerName}</span></c:if>
                                                                </div>
                                                                <c:if test="${not empty lv3.children}">
                                                                    <ul class="tree-sub">
                                                                        <c:forEach var="lv4" items="${lv3.children}" varStatus="s4">
                                                                            <li class="tree-node ${s4.last ? 'last-node' : ''}">
                                                                                <div class="tree-row ${selectedDeptId == lv4.deptId ? 'active' : ''}" onclick="location.href='?deptId=${lv4.deptId}'">
                                                                                    <span class="tree-icon">${not empty lv4.children ? '📁' : '📄'}</span>
                                                                                    <span class="tree-text">${lv4.deptName}</span>
                                                                                    <c:if test="${not empty lv4.managerName}"><span class="tree-manager">${lv4.managerName}</span></c:if>
                                                                                </div>
                                                                                <c:if test="${not empty lv4.children}">
                                                                                    <ul class="tree-sub">
                                                                                        <c:forEach var="lv5" items="${lv4.children}" varStatus="s5">
                                                                                            <li class="tree-node ${s5.last ? 'last-node' : ''}">
                                                                                                <div class="tree-row ${selectedDeptId == lv5.deptId ? 'active' : ''}" onclick="location.href='?deptId=${lv5.deptId}'">
                                                                                                    <span class="tree-icon">📄</span>
                                                                                                    <span class="tree-text">${lv5.deptName}</span>
                                                                                                    <c:if test="${not empty lv5.managerName}"><span class="tree-manager">${lv5.managerName}</span></c:if>
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
            <div id="inactive-list" class="tab-content" style="display:none;">
                <c:choose>
                    <c:when test="${not empty inactiveDepts}">
                        <ul class="org-tree">
                            <c:forEach var="idept" items="${inactiveDepts}">
                                <li class="tree-node">
                                    <div class="tree-row ${selectedDeptId == idept.dept_id ? 'active' : ''}" onclick="location.href='?deptId=${idept.dept_id}'">
                                        <span class="tree-icon">🚫</span>
                                        <span class="tree-text" style="color:#94a3b8;">${idept.dept_name}</span>
                                    </div>
                                </li>
                            </c:forEach>
                        </ul>
                    </c:when>
                    <c:otherwise><div class="empty-state">비활성 부서가 없습니다.</div></c:otherwise>
                </c:choose>
            </div>
        </c:if>
    </div>

    <div class="dept-detail-panel">
        <div class="dept-info-card">
            <div class="card-title">📝 부서 정보 ${isNewMode ? '(신규 등록)' : ''}</div>
            <form action="${pageContext.request.contextPath}/org/dept" method="post" id="deptForm" onsubmit="return false">
                <%-- [NEW-M-3 해결] action 초기값을 더 명확하게 제어 --%>
                <input type="hidden" name="action" id="formAction" value="${isNewMode ? 'insert' : 'update'}">
                <input type="hidden" name="dept_id" value="${selectedDept.dept_id}">

                <div class="dept-form-grid">
                    <div class="dept-form-group full-width">
                        <label>부서명 <span class="required">*</span></label>
                        <input type="text" name="dept_name" id="deptNameInput" 
                               value="${isNewMode ? '' : selectedDept.dept_name}" 
                               placeholder="${isNewMode ? '신규 부서명을 입력하세요' : ''}"
                               ${!isAdmin ? 'readonly' : ''} oninput="clearFieldError(this)">
                        <span class="field-error-msg" id="deptNameError">부서명은 필수입니다.</span>
                    </div>

                    <div class="dept-form-group">
                        <label>상위 부서</label>
                        <c:choose>
                            <c:when test="${isAdmin}">
                                <select name="parent_dept_id">
                                    <option value="0">없음 (최상위)</option>
                                    <c:forEach var="d" items="${allDepts}">
                                        <c:if test="${(isNewMode || d.dept_id != selectedDept.dept_id) && d.is_active == 1}">
                                            <option value="${d.dept_id}" ${selectedDept.parent_dept_id == d.dept_id ? 'selected' : ''}>${d.dept_name}</option>
                                        </c:if>
                                    </c:forEach>
                                </select>
                            </c:when>
                            <c:otherwise>
                                <input type="text" value="${selectedDept.parent_dept_name}" readonly>
                            </c:otherwise>
                        </c:choose>
                    </div>

                    <div class="dept-form-group">
                        <label>부서장</label>
                        <input type="text" value="${isNewMode ? '자동지정' : (not empty selectedDept.manager_name ? selectedDept.manager_name : '미지정')}" readonly>
                    </div>

                    <c:if test="${isAdmin}">
                        <div class="dept-form-group">
                            <label>출력 순서</label>
                            <input type="number" name="sort_order" value="${selectedDept.sort_order > 0 ? selectedDept.sort_order : 1}">
                        </div>
                        <div class="dept-form-group">
                            <label>계층 깊이</label>
                            <input type="text" value="${!isNewMode && selectedDept.dept_level > 0 ? selectedDept.dept_level += '단계' : '-'}" readonly>
                        </div>
                        <div class="dept-form-group">
                            <label>상태</label>
                            <c:choose>
                                <c:when test="${!isNewMode && selectedDept.is_active == 0}">
                                    <%-- [NEW-M-3 해결] 비활성 부서 복구 시 select 값이 정상 전송되도록 보장 --%>
                                    <select name="is_active">
                                        <option value="1">활성 (복구)</option>
                                        <option value="0" selected>비활성 유지</option>
                                    </select>
                                </c:when>
                                <c:otherwise>
                                    <input type="text" value="활성" readonly style="background-color: #f1f5f9; color: #2563eb; font-weight: 600;">
                                    <input type="hidden" name="is_active" value="1">
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </c:if>
                </div>

                <div class="dept-btn-area">
                    <c:if test="${isAdmin && !isNewMode && selectedDept.dept_id > 0 && selectedDept.is_active == 1}">
                        <button type="button" class="btn-dept-delete" onclick="submitAction('delete')">부서 폐지</button>
                    </c:if>
                    <c:if test="${isAdmin}">
                        <button type="button" class="btn-dept-cancel" onclick="location.href='${pageContext.request.contextPath}/org/dept'">취소</button>
                        <button type="button" class="btn-dept-save" onclick="submitAction('save')">저장</button>
                    </c:if>
                </div>
            </form>
        </div>

        <div class="dept-member-card">
            <div class="card-title">👥 소속 직원 <span class="member-count">${not empty memberList ? memberList.size() : 0}명</span></div>
            <c:choose>
                <c:when test="${not empty memberList}">
                    <table class="member-table">
                        <thead><tr><th>사번</th><th>이름</th><th>직급</th><th>상태</th></tr></thead>
                        <tbody>
                            <c:forEach var="m" items="${memberList}">
                                <tr>
                                    <td>${m.empNo}</td>
                                    <td style="font-weight:600;">${m.empName}</td>
                                    <td>${m.posName}</td>
                                    <td>${m.status}</td>
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

<div id="empModalOverlay" class="emp-modal-overlay">
    <div class="emp-modal-content">
        <div class="emp-modal-header"><h3>👥 검색 결과 선택</h3><span class="emp-modal-close" onclick="closeEmpModal()">&times;</span></div>
        <div class="emp-modal-body" id="empModalBody"></div>
    </div>
</div>

<script>
function switchDeptTab(el, tabId) {
    document.querySelectorAll('.dept-tab').forEach(t => t.classList.remove('active'));
    el.classList.add('active');
    document.querySelectorAll('.tab-content').forEach(c => c.style.display = 'none');
    document.getElementById(tabId).style.display = 'block';
}

function clearFieldError(input) { 
    input.classList.remove('input-error'); 
    const errEl = document.getElementById(input.id + 'Error'); 
    if (errEl) errEl.style.display = 'none'; 
}

function validateForm() {
    const nameInput = document.getElementById('deptNameInput');
    if (nameInput && !nameInput.value.trim()) {
        nameInput.classList.add('input-error');
        document.getElementById('deptNameError').style.display = 'block';
        return false;
    }
    return true;
}

//폼 전송 모드 판정 로직 강화
function submitAction(action) {
    const form = document.getElementById('deptForm');
    const actionInput = document.getElementById('formAction');
    // dept_id를 더 안전하게 가져오기
    const deptIdHidden = form.querySelector('input[name="dept_id"]');
    const deptId = deptIdHidden ? deptIdHidden.value : "";

    if (action === 'delete') {
        if (!confirm('정말 폐지하시겠습니까?')) return;
        actionInput.value = 'delete';
    } else {
        if (!validateForm()) return;
     
        // 1. 신규 모드 버튼을 누른 게 아니고 (param.action !== 'new')
        // 2. 실제 hidden field에 0이 아닌 숫자가 들어있다면
        // --> 무조건 'update'로 처리해야 복구가 정상 작동합니다.
        const isActuallyNew = (new URLSearchParams(window.location.search).get('action') === 'new');
        
        if (!isActuallyNew && deptId && deptId !== "0") {
            actionInput.value = 'update';
        } else {
            actionInput.value = 'insert';
        }
    }
    
    console.log("전송 액션:", actionInput.value);
    console.log("대상 부서 ID:", deptId);
    
    form.submit();
}

function openEmpModal(list) {
    const body = document.getElementById('empModalBody');
    body.innerHTML = '';
    list.forEach(emp => {
        const div = document.createElement('div');
        div.className = 'emp-select-item';
        div.innerHTML = `
            <span class="dept-label">\${emp.deptName || '부서미정'}</span>
            <span class="info-label">\${emp.posName} | \${emp.empName} (\${emp.empNo})</span>
        `;
        div.onclick = () => { 
            // [NEW-M-4 해결] 모달 내에서도 부서 ID 체크
            if (emp.deptId && parseInt(emp.deptId) > 0) {
                location.href = '?deptId=' + emp.deptId;
            } else { 
                alert('해당 사원은 현재 소속된 부서 정보가 없습니다.'); 
                closeEmpModal(); 
            }
        };
        body.appendChild(div);
    });
    document.getElementById('empModalOverlay').style.display = 'flex';
}

function closeEmpModal() { document.getElementById('empModalOverlay').style.display = 'none'; }

// [NEW-M-4 해결] 단일 검색 결과 시 부서미배정자 예외 처리
function searchDeptByEmp() {
    const name = document.getElementById('empSearchInput').value.trim();
    if (!name) return;
    
    fetch('${pageContext.request.contextPath}/org/dept?action=findDeptByEmp&empName=' + encodeURIComponent(name))
        .then(r => r.json())
        .then(d => {
            if (d.status === "success") {
                // 부서 ID가 유효한 경우에만 이동
                if (d.deptId && parseInt(d.deptId) > 0) {
                    location.href = '?deptId=' + d.deptId;
                } else {
                    alert('해당 사원은 현재 소속된 부서가 없습니다.');
                }
            }
            else if (d.status === "multiple") {
                openEmpModal(d.list);
            }
            else {
                alert('사원을 찾을 수 없습니다.');
            }
        }).catch(() => alert('검색 중 오류 발생'));
}

window.onclick = e => { if (e.target == document.getElementById('empModalOverlay')) closeEmpModal(); }

window.addEventListener('DOMContentLoaded', () => {
    <c:if test="${not empty selectedDept && selectedDept.is_active == 0}">
        const inactiveTab = document.querySelectorAll('.dept-tab')[1];
        if (inactiveTab) switchDeptTab(inactiveTab, 'inactive-list');
    </c:if>
    const toast = document.getElementById('statusMsg');
    if (toast) { 
        setTimeout(() => { 
            toast.style.transition = 'opacity .4s'; 
            toast.style.opacity = '0'; 
            setTimeout(() => toast.style.display = 'none', 400); 
        }, 3500); 
    }
});
</script>