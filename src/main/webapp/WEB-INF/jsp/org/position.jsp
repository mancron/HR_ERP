<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<link rel="stylesheet" href="${pageContext.request.contextPath}/css/org/position.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">

<div class="pos-container">
    <%-- 상단 메시지 레이어 --%>
    <div id="parentStatusMsg" class="msg-layer"></div>

    <div class="pos-header">
        <h2>직급 관리</h2>
        <p>전체 직급 체계를 조회합니다.</p>
    </div>

    <%-- 활성 직급 목록 카드 --%>
    <div class="pos-card">
        <table class="pos-table">
            <thead>
                <tr>
                    <th class="col-name">직급명</th>
                    <th class="col-level">레벨</th>
                    <th class="col-sal">기본급</th>
                    <th class="col-meal">식대</th>
                    <th class="col-trans">교통비</th>
                    <th class="col-allow">직책 수당</th>
                    <th class="col-count">인원</th>
                    <th class="col-status">상태</th>
                    <c:if test="${sessionScope.userRole == 'HR담당자'}">
                        <th class="col-manage">관리</th>
                    </c:if>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="p" items="${posList}">
                    <c:if test="${p.is_active == 1}">
                        <tr>
                            <td><strong>${p.position_name}</strong></td>
                            <td>${p.position_level}</td>
                            <td><fmt:formatNumber value="${p.base_salary}" pattern="#,###" /></td>
                            <td><fmt:formatNumber value="${p.meal_allowance}" pattern="#,###" /></td>
                            <td><fmt:formatNumber value="${p.transport_allowance}" pattern="#,###" /></td>
                            <td><fmt:formatNumber value="${p.position_allowance}" pattern="#,###" /></td>
                            <td>${p.emp_count}명</td>
                            <td><span class="status-badge status-active">활성</span></td>
                            <c:if test="${sessionScope.userRole == 'HR담당자'}">
                                <td><button type="button" class="btn-edit" onclick="openModal('${p.position_id}')">수정</button></td>
                            </c:if>
                        </tr>
                    </c:if>
                </c:forEach>
            </tbody>
        </table>

        <c:if test="${sessionScope.userRole == 'HR담당자'}">
            <div class="pos-footer-notice">
                <span>ⓘ</span>
                <p>직급 수정 시 변경 전·후 값이 <strong>감사 로그(audit_log)</strong>에 자동 기록됩니다. 직원이 사용 중인 직급은 비활성화가 불가능합니다.</p>
            </div>
        </c:if>
    </div>

    <%-- 비활성화 목록 영역 (HR담당자 전용) --%>
    <c:if test="${sessionScope.userRole == 'HR담당자'}">
        <div class="inactive-wrapper">
            <button type="button" class="btn-toggle-inactive" onclick="toggleInactive()">
                <span id="toggleIcon">▼</span> 비활성화된 직급 보기
            </button>
            
            <div id="inactiveContent">
                <%-- 비활성 데이터 존재 여부 체크 --%>
                <c:set var="hasInactive" value="false" />
                <c:forEach var="p" items="${posList}">
                    <c:if test="${p.is_active == 0}"><c:set var="hasInactive" value="true" /></c:if>
                </c:forEach>

                <c:choose>
                    <c:when test="${hasInactive}">
                        <div class="pos-card">
                            <table class="pos-table">
                                <tbody>
                                    <c:forEach var="p" items="${posList}">
                                        <c:if test="${p.is_active == 0}">
                                            <tr>
                                                <td class="col-name"><strong>${p.position_name}</strong></td>
                                                <td class="col-level">${p.position_level}</td>
                                                <td class="col-sal"><fmt:formatNumber value="${p.base_salary}" pattern="#,###" /></td>
                                                <td class="col-meal"><fmt:formatNumber value="${p.meal_allowance}" pattern="#,###" /></td>
                                                <td class="col-trans"><fmt:formatNumber value="${p.transport_allowance}" pattern="#,###" /></td>
                                                <td class="col-allow"><fmt:formatNumber value="${p.position_allowance}" pattern="#,###" /></td>
                                                <td class="col-count">${p.emp_count}명</td>
                                                <td class="col-status"><span class="status-badge">비활성</span></td>
                                                <td class="col-manage"><button type="button" class="btn-edit" onclick="openModal('${p.position_id}')">복구</button></td>
                                            </tr>
                                        </c:if>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div style="padding: 20px; text-align: center; color: #94a3b8; font-size: 0.875rem;">
                            비활성화된 직급이 없습니다.
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </c:if>
</div>

<%-- 수정 모달 레이어 --%>
<div id="editModal" class="modal-overlay">
    <div class="modal-content">
        <div class="modal-header">
            <h3>직급 정보 수정</h3>
            <button type="button" onclick="closeModal()" class="close-btn">&times;</button>
        </div>
        <iframe id="editFrame" src="" frameborder="0"></iframe>
    </div>
</div>

<script>
    // 수정 완료 후 호출되는 콜백
    function handleUpdateSuccess() {
        const modal = document.getElementById('editModal');
        const iframe = document.getElementById('editFrame');
        modal.style.display = 'none';
        iframe.src = '';
        document.body.style.overflow = 'auto';

        const statusMsg = document.getElementById('parentStatusMsg');
        statusMsg.innerText = "✅ 직급 정보가 성공적으로 수정되었습니다.";
        statusMsg.className = "msg-layer msg-success"; 
        statusMsg.style.display = "block";

        window.scrollTo({ top: 0, behavior: 'smooth' });
        setTimeout(() => {
            statusMsg.style.display = "none";
            location.reload();
        }, 2000);
    }

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
    }

    function toggleInactive() {
        const content = document.getElementById('inactiveContent');
        const icon = document.getElementById('toggleIcon');
        if (!content || !icon) return;

        // 명시적으로 display 값을 변경하여 토글
        const isHidden = (window.getComputedStyle(content).display === 'none');
        
        if (isHidden) {
            content.style.setProperty('display', 'block', 'important');
            icon.innerText = '▲';
        } else {
            content.style.setProperty('display', 'none', 'important');
            icon.innerText = '▼';
        }
    }

    // 모달 바깥 클릭 시 닫기
    window.onclick = function(event) {
        const modal = document.getElementById('editModal');
        if (event.target === modal) closeModal();
    }
</script>