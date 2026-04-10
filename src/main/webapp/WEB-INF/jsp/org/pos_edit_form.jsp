<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/org/position.css">
</head>
<body class="modal-form-body"> 

    <%-- [핵심 수정] HR담당자가 아니면 폼 자체를 렌더링하지 않고 차단 메시지만 표시 --%>
    <c:choose>
        <c:when test="${sessionScope.userRole != 'HR담당자'}">
            <div style="padding: 50px 20px; text-align: center;">
                <div class="msg-layer msg-error" style="display: inline-block; width: auto; padding: 20px 40px;">
                    <strong>접근 권한 없음</strong><br>
                    직급 수정 권한이 없습니다. HR담당자만 이용 가능합니다.
                </div>
                <div style="margin-top: 20px;">
                    <button type="button" onclick="parent.closeModal()" class="btn-edit" style="background-color: #64748b;">닫기</button>
                </div>
            </div>
        </c:when>

        <c:otherwise>
            <%-- HR담당자일 때만 아래 기존 폼이 보입니다 --%>
            <c:if test="${not empty param.status and param.status != 'success'}">
                <c:set var="statusClass" value="msg-error" />
                <div id="statusMsg" class="msg-layer ${statusClass}">
                    <c:choose>
                        <c:when test="${param.status == 'no_change'}">변경된 내용이 없습니다.</c:when>
                        <c:when test="${param.status == 'has_emp'}">직원이 존재하여 비활성화가 불가능합니다.</c:when>
                        <c:otherwise>수정 중 오류가 발생했습니다. 다시 시도해주세요.</c:otherwise>
                    </c:choose>
                </div>
            </c:if>

            <form action="${pageContext.request.contextPath}/org/position/edit" method="post" id="editForm">
                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                <input type="hidden" name="position_id" value="${pos.position_id}">
                
                <div class="form-group">
                    <label>직급명 (수정 불가)</label>
                    <input type="text" value="${pos.position_name}" class="form-control readonly" readonly tabindex="-1">
                </div>
                
                <div class="form-group">
                    <label>기본급 (원)</label>
                    <input type="number" name="base_salary" value="${pos.base_salary}" class="form-control" required min="0" max="999999999">
                </div>
                <div class="form-group">
                    <label>식대 (원)</label>
                    <input type="number" name="meal_allowance" value="${pos.meal_allowance}" class="form-control" required min="0" max="10000000">
                </div>
                <div class="form-group">
                    <label>교통비 (원)</label>
                    <input type="number" name="transport_allowance" value="${pos.transport_allowance}" class="form-control" required min="0" max="10000000">
                </div>
                <div class="form-group">
                    <label>직책수당 (원)</label>
                    <input type="number" name="position_allowance" value="${pos.position_allowance}" class="form-control" required min="0" max="10000000">
                </div>
                
                <div class="form-group">
                    <label>상태</label>
                    <select name="is_active" class="form-control"> 
                        <option value="1" ${pos.is_active == 1 ? 'selected' : ''}>활성</option>
                        <option value="0" ${pos.is_active == 0 ? 'selected' : ''}>비활성</option>
                    </select>
                </div>
                
                <button type="submit" class="btn-save">정보 업데이트</button>
            </form>
        </c:otherwise>
    </c:choose>

    <script>
        window.onload = function() {
            const urlParams = new URLSearchParams(window.location.search);
            const status = urlParams.get('status');
            
            if (status === 'success') {
                if (parent && typeof parent.handleUpdateSuccess === 'function') {
                    parent.handleUpdateSuccess();
                } else {
                    if (parent && parent.closeModal) parent.closeModal();
                }
            }
        };
    </script>
</body>
</html>