<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="isAdmin" value="${sessionScope.userRole == '관리자' || sessionScope.userRole == 'HR담당자'}" />

<link rel="stylesheet" href="${pageContext.request.contextPath}/css/org/dept.css">

<%-- 토스트 --%>
<c:if test="${not empty param.error || not empty param.msg}">
    <c:set var="isSuccess" value="${not empty param.msg && (param.msg == 'success' || param.msg == 'deleted')}" />
    <div class="status-toast ${isSuccess ? 'toast-success' : 'toast-error'}" id="statusMsg">
        <span class="toast-content">
            <c:choose>
                <c:when test="${param.msg == 'success'}">✅ 정보가 성공적으로 반영되었습니다.</c:when>
                <c:when test="${param.msg == 'deleted'}">✅ 부서가 성공적으로 폐지되었습니다.</c:when>
                <c:when test="${param.error == 'circular_reference'}">❌ 순환 참조: 하위 부서를 상위로 지정할 수 없습니다.</c:when>
                <c:when test="${param.error == 'self_parent'}">❌ 자기 자신을 상위 부서로 지정할 수 없습니다.</c:when>
                <c:when test="${param.error == 'level_exceeded'}">❌ 조직도는 최대 5단계까지만 구성 가능합니다.</c:when>
                <c:when test="${param.error == 'has_members'}">❌ 재직 중인 직원이 있어 폐지할 수 없습니다.</c:when>
                <c:when test="${param.error == 'has_children'}">❌ 하위 부서가 존재하는 경우 폐지할 수 없습니다.</c:when>
                <c:otherwise>⚠️ ${param.error}${param.msg}</c:otherwise>
            </c:choose>
        </span>
        <span class="btn-toast-close" onclick="this.parentElement.style.display='none'">×</span>
    </div>
</c:if>

<div class="dept-container">

    <%-- ══ 왼쪽 조직도 패널 ══ --%>
    <div class="dept-tree-panel">

        <div class="panel-header">
            <span class="panel-title">🌳 조직도</span>
            <c:if test="${isAdmin}">
                <button class="btn-add-dept"
                        onclick="location.href='${pageContext.request.contextPath}/org/dept?action=new'">
                    + 부서 추가
                </button>
            </c:if>
        </div>

        <div class="dept-search-box">
            <input type="text" id="empSearchInput"
                   placeholder="사원명으로 부서 찾기..."
                   onkeyup="if(event.keyCode===13) searchDeptByEmp()">
            <button type="button" onclick="searchDeptByEmp()">🔍</button>
        </div>

        <c:if test="${isAdmin}">
            <div class="dept-tabs-container">
                <div class="dept-tabs-wrapper">
                    <div class="dept-tab active" onclick="switchDeptTab(this,'active-tree')">활성</div>
                    <div class="dept-tab"        onclick="switchDeptTab(this,'inactive-list')">비활성</div>
                </div>
            </div>
        </c:if>

        <%-- 활성 트리 --%>
        <div id="active-tree" class="tab-content active">
            <ul class="org-tree">
                <li>
                    <div class="tree-company">🏢 ${not empty companyName ? companyName : '(주)예시회사'}</div>
                    <ul class="tree-group">
                        <c:forEach var="lv1" items="${deptTree}">
                            <li class="tree-node">
                                <div class="tree-row ${selectedDeptId == lv1.deptId ? 'active' : ''}"
                                     onclick="location.href='?deptId=${lv1.deptId}'">
                                    <span class="tree-icon">${not empty lv1.children ? '📁' : '📄'}</span>
                                    <span class="tree-text">${lv1.deptName}</span>
                                </div>
                                <c:if test="${not empty lv1.children}">
                                    <ul class="tree-sub">
                                        <c:forEach var="lv2" items="${lv1.children}">
                                            <li class="tree-node">
                                                <div class="tree-row ${selectedDeptId == lv2.deptId ? 'active' : ''}"
                                                     onclick="location.href='?deptId=${lv2.deptId}'">
                                                    <span class="tree-icon">${not empty lv2.children ? '📁' : '📄'}</span>
                                                    <span class="tree-text">${lv2.deptName}</span>
                                                </div>
                                                <c:if test="${not empty lv2.children}">
                                                    <ul class="tree-sub">
                                                        <c:forEach var="lv3" items="${lv2.children}">
                                                            <li class="tree-node">
                                                                <div class="tree-row ${selectedDeptId == lv3.deptId ? 'active' : ''}"
                                                                     onclick="location.href='?deptId=${lv3.deptId}'">
                                                                    <span class="tree-icon">${not empty lv3.children ? '📁' : '📄'}</span>
                                                                    <span class="tree-text">${lv3.deptName}</span>
                                                                </div>
                                                                <c:if test="${not empty lv3.children}">
                                                                    <ul class="tree-sub">
                                                                        <c:forEach var="lv4" items="${lv3.children}">
                                                                            <li class="tree-node">
                                                                                <div class="tree-row ${selectedDeptId == lv4.deptId ? 'active' : ''}"
                                                                                     onclick="location.href='?deptId=${lv4.deptId}'">
                                                                                    <span class="tree-icon">${not empty lv4.children ? '📁' : '📄'}</span>
                                                                                    <span class="tree-text">${lv4.deptName}</span>
                                                                                </div>
                                                                                <c:if test="${not empty lv4.children}">
                                                                                    <ul class="tree-sub">
                                                                                        <c:forEach var="lv5" items="${lv4.children}">
                                                                                            <li class="tree-node">
                                                                                                <div class="tree-row ${selectedDeptId == lv5.deptId ? 'active' : ''}"
                                                                                                     onclick="location.href='?deptId=${lv5.deptId}'">
                                                                                                    <span class="tree-icon">📄</span>
                                                                                                    <span class="tree-text">${lv5.deptName}</span>
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

        <%-- 비활성 목록 (관리자만) --%>
        <c:if test="${isAdmin}">
            <div id="inactive-list" class="tab-content" style="display:none;">
                <c:choose>
                    <c:when test="${not empty inactiveDepts}">
                        <ul class="org-tree">
                            <c:forEach var="idept" items="${inactiveDepts}">
                                <li class="tree-node">
                                    <div class="tree-row ${selectedDeptId == idept.dept_id ? 'active' : ''}"
                                         onclick="location.href='?deptId=${idept.dept_id}'">
                                        <span class="tree-icon">🚫</span>
                                        <span class="tree-text" style="color:#94a3b8;">${idept.dept_name}</span>
                                    </div>
                                </li>
                            </c:forEach>
                        </ul>
                    </c:when>
                    <c:otherwise>
                        <div class="empty-state" style="padding:20px;font-size:13px;">비활성 부서가 없습니다.</div>
                    </c:otherwise>
                </c:choose>
            </div>
        </c:if>

    </div><%-- /dept-tree-panel --%>


    <%-- ══ 오른쪽 상세 패널 ══ --%>
    <div class="dept-detail-panel">

        <div class="dept-info-card">
            <div class="card-title">
                📝 부서 정보
                <c:if test="${not empty selectedDept.dept_name}">
                    <span style="color:#94a3b8;font-weight:400;margin:0 4px;">·</span>
                    <span style="color:#2563eb;">${selectedDept.dept_name}</span>
                </c:if>
            </div>

            <form action="${pageContext.request.contextPath}/org/dept" method="post" id="deptForm">
                <input type="hidden" name="action"  id="formAction" value="save">
                <input type="hidden" name="dept_id" value="${selectedDept.dept_id}">

                <div class="dept-form-grid">

                    <%-- 부서명: 전체 너비 --%>
                    <div class="dept-form-group full-width">
                        <label>부서명 <span class="required">*</span></label>
                        <input type="text" name="dept_name"
                               value="${selectedDept.dept_name}"
                               placeholder="부서명을 입력하세요"
                               required
                               ${!isAdmin ? 'readonly' : ''}>
                    </div>

                    <%-- 상위 부서 --%>
                    <div class="dept-form-group">
                        <label>상위 부서</label>
                        <c:choose>
                            <c:when test="${isAdmin}">
                                <select name="parent_dept_id">
                                    <option value="0">없음 (최상위)</option>
                                    <c:forEach var="d" items="${allDepts}">
                                        <c:if test="${d.dept_id != selectedDept.dept_id && d.is_active == 1}">
                                            <option value="${d.dept_id}"
                                                    ${selectedDept.parent_dept_id == d.dept_id ? 'selected' : ''}>
                                                ${d.dept_name}
                                            </option>
                                        </c:if>
                                    </c:forEach>
                                </select>
                            </c:when>
                            <c:otherwise>
                                <c:set var="parentName" value="없음 (최상위)" />
                                <c:forEach var="d" items="${allDepts}">
                                    <c:if test="${d.dept_id == selectedDept.parent_dept_id}">
                                        <c:set var="parentName" value="${d.dept_name}" />
                                    </c:if>
                                </c:forEach>
                                <input type="text" value="${parentName}" readonly>
                            </c:otherwise>
                        </c:choose>
                    </div>

                    <%-- 관리자: 출력순서 / 계층깊이 / 상태 --%>
                    <c:if test="${isAdmin}">
                        <div class="dept-form-group">
                            <label>출력 순서</label>
                            <input type="number" name="sort_order" min="1"
                                   value="${selectedDept.sort_order > 0 ? selectedDept.sort_order : 1}">
                        </div>

                        <div class="dept-form-group">
                            <label>계층 깊이</label>
                            <c:choose>
                                <c:when test="${selectedDept.dept_level > 0}">
                                    <input type="text" value="${selectedDept.dept_level}단계" readonly>
                                </c:when>
                                <c:otherwise>
                                    <input type="text" value="-" readonly>
                                </c:otherwise>
                            </c:choose>
                        </div>

                        <div class="dept-form-group">
                            <label>상태</label>
                            <select name="is_active">
                                <option value="1" ${selectedDept.is_active == 1 ? 'selected' : ''}>활성</option>
                                <option value="0" ${selectedDept.is_active == 0 ? 'selected' : ''}>비활성</option>
                            </select>
                        </div>

                        <%-- 관리자/HR: 생성일 & 폐지일 표시 --%>
                        <c:if test="${not empty selectedDept.created_at}">
                            <div class="dept-form-group">
                                <label>생성일</label>
                                <input type="text" value="${selectedDept.created_at}" readonly>
                            </div>
                        </c:if>

                        <c:if test="${not empty selectedDept.closed_at}">
                            <div class="dept-form-group">
                                <label>폐지일</label>
                                <input type="text" value="${selectedDept.closed_at}" readonly
                                       style="color:#ef4444;">
                            </div>
                        </c:if>
                    </c:if>

                </div><%-- /dept-form-grid --%>

                <div class="dept-btn-area">
                    <%-- 폐지 버튼: 관리자 + 활성 부서일 때만 --%>
                    <c:if test="${isAdmin && selectedDept.dept_id > 0 && selectedDept.is_active == 1}">
                        <button type="button" class="btn-dept-delete"
                                onclick="submitAction('delete')">부서 폐지</button>
                    </c:if>

                    <%-- 취소 버튼: 관리자만 (일반 사용자는 불필요) --%>
                    <c:if test="${isAdmin}">
                        <button type="button" class="btn-dept-cancel"
                                onclick="location.href='${pageContext.request.contextPath}/org/dept'">
                            취소
                        </button>
                        <button type="button" class="btn-dept-save"
                                onclick="submitAction('save')">저장</button>
                    </c:if>
                </div>

            </form>
        </div>

        <%-- 소속 직원 --%>
        <div class="dept-member-card">
            <div class="card-title">
                👥 소속 직원
                <span class="member-count">${not empty memberList ? memberList.size() : 0}명</span>
            </div>
            <c:choose>
                <c:when test="${not empty memberList}">
                    <table class="member-table">
                        <thead>
                            <tr><th>사번</th><th>이름</th><th>직급</th><th>상태</th></tr>
                        </thead>
                        <tbody>
                            <c:forEach var="m" items="${memberList}">
                                <tr>
                                    <td style="color:#64748b;font-size:13px;">${m.empNo}</td>
                                    <td style="font-weight:600;">${m.empName}</td>
                                    <td>${m.posName}</td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${m.status == '재직'}"><span class="badge-active">재직</span></c:when>
                                            <c:when test="${m.status == '휴직'}"><span class="badge-leave">휴직</span></c:when>
                                            <c:otherwise><span class="badge-inactive">퇴직</span></c:otherwise>
                                        </c:choose>
                                    </td>
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

    </div><%-- /dept-detail-panel --%>
</div><%-- /dept-container --%>


<script>
function switchDeptTab(el, tabId) {
    document.querySelectorAll('.dept-tab').forEach(t => {
        t.classList.remove('active');
        t.style.background = 'transparent';
        t.style.boxShadow = 'none';
        t.style.color = '';
    });
    el.classList.add('active');
    el.style.background = '#fff';
    el.style.boxShadow = '0 1px 4px rgba(0,0,0,.1)';
    el.style.color = '#2563eb';
    document.querySelectorAll('.tab-content').forEach(c => {
        c.style.display = 'none'; c.classList.remove('active');
    });
    const t = document.getElementById(tabId);
    if (t) { t.style.display = 'block'; t.classList.add('active'); }
}

function submitAction(action) {
    if (action === 'delete' && !confirm('정말 이 부서를 폐지하시겠습니까?')) return;
    document.getElementById('formAction').value = action;
    document.getElementById('deptForm').submit();
}

function searchDeptByEmp() {
    const name = document.getElementById('empSearchInput').value.trim();
    if (!name) { alert('검색할 사원명을 입력하세요.'); return; }
    fetch('${pageContext.request.contextPath}/org/dept?action=findDeptByEmp&empName=' + encodeURIComponent(name))
        .then(r => r.json())
        .then(d => {
            if (d.deptId > 0) location.href = '?deptId=' + d.deptId;
            else alert('해당 이름의 사원을 찾을 수 없습니다.');
        })
        .catch(() => alert('검색 중 오류가 발생했습니다.'));
}

window.addEventListener('DOMContentLoaded', () => {
    /* 비활성 부서 선택 시 탭 자동 전환 */
    <c:if test="${not empty selectedDept && selectedDept.is_active == 0}">
        const inactiveTab = document.querySelectorAll('.dept-tab')[1];
        if (inactiveTab) switchDeptTab(inactiveTab, 'inactive-list');
    </c:if>

    /* 토스트 자동 페이드아웃 */
    const toast = document.getElementById('statusMsg');
    if (toast) {
        setTimeout(() => {
            toast.style.transition = 'opacity .5s';
            toast.style.opacity = '0';
            setTimeout(() => toast.style.display = 'none', 500);
        }, 3500);
    }

    /* 조직도 연결선: 각 ul의 마지막 li 아래로 세로선이 튀어나오지 않도록
       마지막 li에 클래스를 추가해 CSS로 세로선 높이를 조정 */
    document.querySelectorAll('.tree-group, .tree-sub').forEach(ul => {
        const items = ul.querySelectorAll(':scope > .tree-node');
        if (items.length > 0) items[items.length - 1].classList.add('last-node');
    });
});
</script>
