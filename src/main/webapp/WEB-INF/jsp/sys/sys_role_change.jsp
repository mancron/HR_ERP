<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>HR ERP - 계정 권한 변경</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/sys/role_change.css">
</head>
<body>

    <jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />

    <div id="main-wrapper">
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />

        <main class="app-content">
            <h1 class="page-title">계정 권한 변경</h1>
            <p class="page-desc">직원의 시스템 접근 권한(role)을 변경합니다. 변경 시 감사 로그가 기록됩니다.</p>

            <%-- 성공/에러 메시지 --%>
            <c:if test="${not empty successMsg}">
                <div class="alert alert-success">✅ <c:out value="${successMsg}" /></div>
            </c:if>
            <c:if test="${not empty errorMsg}">
                <div class="alert alert-error">⚠ <c:out value="${errorMsg}" /></div>
            </c:if>

            <div class="card table-card">
                <div class="table-wrap">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>사번</th>
                                <th>이름</th>
                                <th>부서</th>
                                <th>현재 권한</th>
                                <th>변경</th>
                                <th>처리</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:choose>
                                <c:when test="${empty accountList}">
                                    <tr>
                                        <td colspan="6" class="empty-row">계정 정보가 없습니다.</td>
                                    </tr>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach var="account" items="${accountList}">
                                        <%-- 자기 자신 여부 판단 --%>
                                        <c:set var="isSelf" value="${account.empId == myEmpId}" />
                                        <tr class="${isSelf ? 'row-self' : ''}">
                                            <td><c:out value="${account.empNo}" /></td>
                                            <td>
                                                <c:out value="${account.empName}" />
                                                <c:if test="${isSelf}">
                                                    <span class="self-badge">나</span>
                                                </c:if>
                                            </td>
                                            <td><c:out value="${account.deptName}" /></td>
                                            <td>
                                                <span class="role-badge
                                                    ${account.currentRole == '관리자'  ? 'badge-red'    :
                                                      account.currentRole == 'HR담당자' ? 'badge-purple' :
                                                                                          'badge-gray'}">
                                                    <c:out value="${account.currentRole}" />
                                                </span>
                                            </td>
                                            <td>
												<%-- 자기 자신은 select 비활성화 --%>
												<select name="newRole" id="select_${account.accountId}" class="form-control select-sm">
												    <c:forEach var="role" items="${validRoles}">
												        <option value="${role}"
												                ${role == account.currentRole ? 'selected' : ''}>
												            <c:out value="${role}" />
												        </option>
												    </c:forEach>
												</select>
                                            </td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${isSelf}">
                                                        <%-- 자기 자신은 버튼 비활성화 --%>
                                                        <button type="button" class="btn btn-primary btn-sm" disabled
                                                                title="자기 자신의 권한은 변경할 수 없습니다.">변경</button>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <form action="${pageContext.request.contextPath}/sys/roleChange"
                                                              method="post"
                                                              onsubmit="return confirmChange(this, '<c:out value="${account.empName}" />')">
                                                            <input type="hidden" name="accountId"   value="${account.accountId}">
                                                            <input type="hidden" name="targetEmpId" value="${account.empId}">
                                                            <input type="hidden" name="oldRole"     value="${account.currentRole}">
                                                            <%-- newRole은 JS로 select 값을 hidden에 복사 후 전송 --%>
                                                            <input type="hidden" name="newRole"     id="hidden_${account.accountId}">
                                                            <button type="submit" class="btn btn-primary btn-sm">변경</button>
                                                        </form>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                        </tbody>
                    </table>
                </div>
            </div>

            <div class="info-box">
                ✔ 권한 변경 시 audit_log(target_table='account', column_name='role')에 자동 기록됩니다.
            </div>

        </main>
    </div>

    <script src="${pageContext.request.contextPath}/js/sidebar.js"></script>
    <script>
        function confirmChange(form, empName) {
            // form 안의 accountId로 select 값을 hidden에 복사
            const accountId = form.querySelector('input[name="accountId"]').value;
            const selectEl  = document.getElementById('select_' + accountId);
            const hiddenEl  = document.getElementById('hidden_' + accountId);
            const newRole   = selectEl.value;
            const oldRole   = form.querySelector('input[name="oldRole"]').value;

            if (newRole === oldRole) {
                alert('현재 권한과 동일합니다. 변경할 권한을 선택해주세요.');
                return false;
            }

            hiddenEl.value = newRole;
            return confirm(empName + ' 직원의 권한을 [' + oldRole + '] → [' + newRole + ']으로 변경하시겠습니까?');
        }
    </script>
</body>
</html>