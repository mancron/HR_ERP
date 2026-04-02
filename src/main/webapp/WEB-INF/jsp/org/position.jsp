<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/org/position.css">

<div class="pos-container">
    <div class="pos-header">
        <h2>직급 관리</h2>
        <p>전체 직급 체계를 조회합니다.</p>
    </div>

    <div class="pos-card">
        <table class="pos-table">
            <thead>
                <tr>
                    <th style="width: 12%;">직급명</th>
                    <th style="width: 8%;">레벨</th>
                    <th style="width: 15%;">기본급</th>
                    <th style="width: 12%;">식대</th>
                    <th style="width: 12%;">교통비</th>
                    <th style="width: 10%;">인원</th>
                    <th style="width: 12%;">상태</th>
                    <c:if test="${sessionScope.userRole == '관리자' || sessionScope.userRole == 'HR담당자'}">
                        <th style="width: 17%;">관리</th>
                    </c:if>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="p" items="${posList}">
                    <c:if test="${p.is_active == 1}">
                        <tr>
                            <td><strong>${p.position_name}</strong></td>
                            <td>${p.position_level}</td>
                            <td><fmt:formatNumber value="${p.base_salary}" pattern="#,###"/></td>
                            <td><fmt:formatNumber value="${p.meal_allowance}" pattern="#,###"/></td>
                            <td><fmt:formatNumber value="${p.transport_allowance}" pattern="#,###"/></td>
                            <td>${p.emp_count}명</td>
                            <td><span class="status-badge status-active">활성</span></td>
                            <c:if test="${sessionScope.userRole == '관리자' || sessionScope.userRole == 'HR담당자'}">
                                <td>
                                    <button type="button" class="btn-edit" onclick="openModal('${p.position_id}')">수정</button>
                                </td>
                            </c:if>
                        </tr>
                    </c:if>
                </c:forEach>
            </tbody>
        </table>

        <c:if test="${sessionScope.userRole == '관리자' || sessionScope.userRole == 'HR담당자'}">
            <div class="pos-footer-notice">
                <span>ⓘ</span>
                <p>직급 수정 시 변경 전·후 값이 <strong>감사 로그(audit_log)</strong>에 자동 기록됩니다. 직원이 사용 중인 직급은 비활성화가 불가능합니다.</p>
            </div>
        </c:if>
    </div>

    <c:if test="${sessionScope.userRole == '관리자' || sessionScope.userRole == 'HR담당자'}">
        <div class="inactive-wrapper">
            <button type="button" class="btn-toggle-inactive" onclick="toggleInactive()">
                <span id="toggleIcon">▲</span> 비활성화된 직급 보기
            </button>
            <div id="inactiveContent" style="margin-top: 15px;">
                <div class="pos-card" style="background-color: #fcfcfc; border: 1px dashed #cbd5e1;">
                    <table class="pos-table">
                        <tbody>
                            <c:forEach var="p" items="${posList}">
                                <c:if test="${p.is_active == 0}">
                                    <tr>
                                        <td style="width: 12%;"><strong>${p.position_name}</strong></td>
                                        <td style="width: 8%;">${p.position_level}</td>
                                        <td style="width: 15%;"><fmt:formatNumber value="${p.base_salary}" pattern="#,###"/></td>
                                        <td style="width: 12%;"><fmt:formatNumber value="${p.meal_allowance}" pattern="#,###"/></td>
                                        <td style="width: 12%;"><fmt:formatNumber value="${p.transport_allowance}" pattern="#,###"/></td>
                                        <td style="width: 10%;">${p.emp_count}명</td>
                                        <td style="width: 12%;"><span class="status-badge">비활성</span></td>
                                        <td style="width: 17%;">
                                            <button type="button" class="btn-edit" onclick="openModal('${p.position_id}')">복구</button>
                                        </td>
                                    </tr>
                                </c:if>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </c:if>
</div>

<div id="editModal" class="modal-overlay">
    <div class="modal-content">
        <div class="modal-header">
            <h3 style="margin:0; font-size: 1.1rem; font-weight: 700; color: #1e293b;">직급 정보 수정</h3>
            <button type="button" onclick="closeModal()" class="close-btn">&times;</button>
        </div>
        
        <iframe id="editFrame" src="" frameborder="0"></iframe>
    </div>
</div>
<script>
    document.addEventListener('DOMContentLoaded', function() {
        const content = document.getElementById('inactiveContent');
        const icon = document.getElementById('toggleIcon');
        
        if (content) content.style.display = 'none';
        if (icon) icon.innerText = '▼';
    });

    function openModal(id) {
        if (!id) return;
        const modal = document.getElementById('editModal');
        const iframe = document.getElementById('editFrame');
        
        iframe.src = "${pageContext.request.contextPath}/org/position/edit?id=" + id;
        modal.style.display = 'flex';
        document.body.style.overflow = 'hidden';
    }

    function closeModal() {
        const modal = document.getElementById('editModal');
        const iframe = document.getElementById('editFrame');
        
        modal.style.display = 'none';
        iframe.src = '';
        document.body.style.overflow = 'auto';
        location.reload(); 
    }

    function toggleInactive() {
        const content = document.getElementById('inactiveContent');
        const icon = document.getElementById('toggleIcon');
        
        if (!content || !icon) return;

        const isHidden = (content.style.display === 'none' || content.style.display === '');
        
        if (isHidden) {
            content.style.display = 'block';
            icon.innerText = '▲';
        } else {
            content.style.display = 'none';
            icon.innerText = '▼';
        }
    }

    window.onclick = function(event) {
        const modal = document.getElementById('editModal');
        if (event.target === modal) {
            closeModal();
        }
    }
</script>