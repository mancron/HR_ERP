<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<header class="app-header">
    <div style="font-size:13px; color:var(--gray-500);">
        메인 / <strong style="color:var(--gray-800);">대시보드</strong>
    </div>
    <div style="display:flex; align-items:center; gap:16px;">

        <%-- 알림 배지 버튼 --%>
        <a href="${pageContext.request.contextPath}/sys/notification"
           style="position:relative; text-decoration:none; color:var(--gray-600);"
           title="알림">
            🔔
            <span id="noti-badge"
                  style="display:none; position:absolute; top:-6px; right:-8px;
                         background:#ef4444; color:#fff; font-size:10px; font-weight:700;
                         min-width:16px; height:16px; border-radius:8px;
                         display:flex; align-items:center; justify-content:center;
                         padding:0 4px;">
                0
            </span>
        </a>

        <%-- 사용자 정보 --%>
        <span style="font-size:13px;">
            <c:out value="${sessionScope.userName}" /> ·
            <c:out value="${sessionScope.userRole}" />
        </span>
        <a href="${pageContext.request.contextPath}/auth/logout"
           style="font-size:13px; color:var(--accent);">로그아웃</a>
    </div>
</header>

<script>
(function() {
    var badge = document.getElementById('noti-badge');

    function fetchUnreadCount() {
        fetch('${pageContext.request.contextPath}/api/notification/count')
            .then(function(res) { return res.json(); })
            .then(function(data) {
                var count = data.count || 0;
                if (count > 0) {
                    badge.textContent = count > 99 ? '99+' : count;
                    badge.style.display = 'flex';
                } else {
                    badge.style.display = 'none';
                }
            })
            .catch(function() {}); // 실패해도 조용히 무시
    }

    // 최초 로드 시 즉시 실행
    fetchUnreadCount();

    // 30초마다 폴링
    setInterval(fetchUnreadCount, 30000);
})();
</script>