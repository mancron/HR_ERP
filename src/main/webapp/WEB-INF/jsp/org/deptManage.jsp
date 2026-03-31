<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="isAdmin" value="${sessionScope.userRole == '관리자' || sessionScope.userRole == 'HR담당자'}" />

<link rel="stylesheet" href="${pageContext.request.contextPath}/css/org/dept.css">

<%-- 에러 메시지 --%>
<c:if test="${not empty errorMsg}">
    <div class="error-toast">${errorMsg}</div>
</c:if>

<div class="dept-container">

    <%-- ══════════════════════════════════════════
         좌측: 조직도 트리 패널
         ══════════════════════════════════════════ --%>
    <div class="dept-tree-panel">
        <div class="panel-header">
            <span class="panel-title">🟢 조직도</span>
            <%-- 관리자 권한이 있을 때만 부서 추가 버튼 노출 --%>
            <c:if test="${isAdmin}">
                <button class="btn-add-dept"
                    onclick="location.href='${pageContext.request.contextPath}/org/dept?action=new'">
                    + 부서 추가
                </button>
            </c:if>
        </div>

        <ul class="org-tree">
            <li>
                <div class="tree-company">
                    <span class="icon-company">🏢</span>
                    <span>${companyName}</span>
                </div>

                <ul class="tree-group">
                    <c:forEach var="parent" items="${deptTree}">
                        <li>
                            <div class="tree-dept-row ${selectedDeptId == parent.deptId ? 'active' : ''}"
                                onclick="location.href='${pageContext.request.contextPath}/org/dept?deptId=${parent.deptId}'">
                                <span class="icon-folder">📁</span>
                                <span>${parent.deptName}</span>
                            </div>

                            <c:if test="${not empty parent.children}">
                                <ul class="tree-team-list">
                                    <c:forEach var="child" items="${parent.children}">
                                        <li>
                                            <div class="tree-team-row ${selectedDeptId == child.deptId ? 'active' : ''}"
                                                onclick="location.href='${pageContext.request.contextPath}/org/dept?deptId=${child.deptId}'">
                                                <span class="icon-team">👤</span>
                                                <span>${child.deptName}</span>
                                                <c:if test="${not empty child.managerName}">
                                                    <span class="manager-name">${child.managerName}</span>
                                                </c:if>
                                            </div>
                                            <c:if test="${not empty child.children}">
                                                <ul class="tree-team-list">
                                                    <c:forEach var="grandchild" items="${child.children}">
                                                        <li>
                                                            <div class="tree-team-row ${selectedDeptId == grandchild.deptId ? 'active' : ''}"
                                                                onclick="location.href='${pageContext.request.contextPath}/org/dept?deptId=${grandchild.deptId}'">
                                                                <span class="icon-team">👤</span>
                                                                <span>${grandchild.deptName}</span>
                                                                <c:if test="${not empty grandchild.managerName}">
                                                                    <span class="manager-name">${grandchild.managerName}</span>
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
            </li>
        </ul>
    </div>

    <%-- ══════════════════════════════════════════
         우측: 부서 정보 + 소속 직원
         ══════════════════════════════════════════ --%>
    <div class="dept-detail-panel">

        <div class="dept-info-card">
            <div class="card-title">
                📋 부서 정보
                <c:if test="${not empty selectedDept.dept_name}">
                    · ${selectedDept.dept_name}
                </c:if>
            </div>

            <form action="${pageContext.request.contextPath}/org/dept" method="post" id="deptForm">
                <input type="hidden" name="action"  id="formAction" value="save">
                <input type="hidden" name="dept_id" value="${selectedDept.dept_id}">

                <div class="dept-form-grid">
                    <%-- 부서명 --%>
                    <div class="dept-form-group">
                        <label>부서명 <span class="required">*</span></label>
                        <input type="text" name="dept_name"
                            value="${selectedDept.dept_name}"
                            placeholder="부서명을 입력하세요" required
                            ${not isAdmin ? 'disabled' : ''}>
                    </div>

                    <%-- 상위 부서 --%>
                    <div class="dept-form-group">
                        <label>상위 부서</label>
                        <select name="parent_dept_id" ${not isAdmin ? 'disabled' : ''}>
                            <option value="0">없음 (최상위)</option>
                            <c:forEach var="d" items="${allDepts}">
                                <c:if test="${d.dept_id != selectedDept.dept_id}">
                                    <option value="${d.dept_id}"
                                        ${selectedDept.parent_dept_id == d.dept_id ? 'selected' : ''}>
                                        ${d.dept_name}
                                    </option>
                                </c:if>
                            </c:forEach>
                        </select>
                    </div>

                    <%-- 부서장 --%>
                    <div class="dept-form-group">
                        <label>부서장</label>
                        <select name="manager_id" ${not isAdmin ? 'disabled' : ''}>
                            <option value="0">미지정</option>
                            <c:forEach var="emp" items="${empList}">
                                <option value="${emp.empId}"
                                    ${selectedDept.manager_id == emp.empId ? 'selected' : ''}>
                                    ${emp.empName} (${emp.posName})
                                </option>
                            </c:forEach>
                        </select>
                    </div>

                    <%-- 출력 순서 --%>
                    <div class="dept-form-group">
                        <label>출력 순서</label>
                        <input type="number" name="sort_order" min="1"
                            value="${selectedDept.sort_order > 0 ? selectedDept.sort_order : 1}"
                            ${not isAdmin ? 'disabled' : ''}>
                    </div>

                    <%-- 계층 깊이 --%>
                    <div class="dept-form-group">
                        <label>계층 깊이</label>
                        <input type="text" value="${selectedDept.dept_level > 0 ? selectedDept.dept_level : '-'}" readonly>
                    </div>

                    <%-- 상태 --%>
                    <div class="dept-form-group">
                        <label>상태</label>
                        <select name="is_active" ${not isAdmin ? 'disabled' : ''}>
                            <option value="1" ${selectedDept.is_active == 1 ? 'selected' : ''}>활성</option>
                            <option value="0" ${selectedDept.is_active == 0 ? 'selected' : ''}>비활성</option>
                        </select>
                    </div>
                </div>

                <div class="dept-btn-area">
                    <c:choose>
                        <c:when test="${isAdmin}">
                            <c:if test="${selectedDept.dept_id > 0}">
                                <button type="button" class="btn-dept-delete" onclick="submitAction('delete')">부서 폐지</button>
                            </c:if>
                            <button type="button" class="btn-dept-cancel" onclick="location.href='${pageContext.request.contextPath}/org/dept'">취소</button>
                            <button type="button" class="btn-dept-save" onclick="submitAction('save')">저장</button>
                        </c:when>
                        <c:otherwise>
                            <span class="auth-notice" style="font-size: 13px; color: #94a3b8; font-style: italic;">
                                🔒 부서 정보 수정은 관리자만 가능합니다.
                            </span>
                        </c:otherwise>
                    </c:choose>
                </div>
            </form>
        </div>

        <%-- 소속 직원 카드 --%>
        <div class="dept-member-card">
            <div class="card-title">
                👥 소속 직원
                <span class="member-count">(${not empty memberList ? memberList.size() : 0}명)</span>
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
                                    <td>
                                        <c:choose>
                                            <c:when test="${member.status == '재직'}">
                                                <span class="badge-active">재직</span>
                                            </c:when>
                                            <c:when test="${member.status == '휴직'}">
                                                <span class="badge-leave">휴직</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge-inactive">퇴직</span>
                                            </c:otherwise>
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
    </div>
</div>

<script>
function submitAction(action) {
    if (action === 'delete') {
        if (!confirm('해당 부서를 폐지하시겠습니까?\n재직 중인 직원이 있을 경우 폐지가 제한됩니다.')) return;
    }
    document.getElementById('formAction').value = action;
    document.getElementById('deptForm').submit();
}
</script>