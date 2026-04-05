<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<header class="app-header" style="display:flex; justify-content:space-between; align-items:center; padding:12px 24px; background:#fff; border-bottom:1px solid #e5e7eb; font-family: 'Pretendard', -apple-system, sans-serif;">
    
    <%-- 동적 브레드크럼 영역 --%>
    <div id="breadcrumb" style="font-size:14px; color:#6b7280;">
        <span id="parent-menu-text">메인</span> / <strong id="current-menu-text" style="color:#111827;">대시보드</strong>
    </div>
    
    <div style="display:flex; align-items:center; gap:20px;">
        <%-- 알림 아이콘 --%>
        <a href="${pageContext.request.contextPath}/notification" style="position:relative; text-decoration:none; font-size:18px; color:#4b5563;" title="알림">
            🔔
            <span id="noti-badge" style="display:none; position:absolute; top:-4px; right:-6px; background:#ef4444; color:#fff; font-size:10px; font-weight:700; min-width:16px; height:16px; border-radius:50%; align-items:center; justify-content:center;">0</span>
        </a>

        <%-- 세션 타이머 영역 (30분 기준 설정) --%>
        <div id="session-box" style="display:flex; align-items:center; gap:10px; background-color:#f0f4f8; padding:4px 10px 4px 14px; border-radius:50px; border:1px solid #d1d9e6; transition: background-color 0.3s;">
            <span style="font-size:13px; color:#4b5563; font-weight:500;">
                남은시간 <span id="timer-display" style="margin-left:6px; color:#2563eb; font-weight:800; min-width:42px; display:inline-block; transition: color 0.2s;">15:00</span>
            </span>
            <button type="button" onclick="extendSession()" 
                    style="background-color:#2563eb; color:#ffffff; border:none; border-radius:20px; padding:4px 20px; font-size:11px; font-weight:700; cursor:pointer;">
                연장
            </button>
        </div>

        <%--  사용자 정보 (이름 + 직급 + 권한) --%>
        <div style="font-size:13px; color:#374151; display:flex; align-items:center; gap:8px;">
            <div style="display:flex; align-items:baseline; gap:4px;">
                <span style="font-weight:600; font-size:14px;">
                    <c:choose>
                        <c:when test="${not empty sessionScope.loginUser.emp_name}">
                            <c:out value="${sessionScope.loginUser.emp_name}" />
                        </c:when>
                        <c:otherwise>
                            <c:out value="${sessionScope.userName}" />
                        </c:otherwise>
                    </c:choose>
                </span>
                <%-- 직급 표시 (부장, 차장 등) --%>
                <span style="font-size:12px; color:#6b7280;">
                    <c:out value="${sessionScope.loginUser.position_name}" />
                </span>
            </div>
            
            <span style="color:#e5e7eb;">|</span>
            
            <%-- 시스템 권한 표시 (관리자, 사원 등) --%>
            <span style="background: #f3f4f6; padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; color: #4b5563;">
                <c:out value="${sessionScope.userRole}" />
            </span>
        </div>
        
        <a href="${pageContext.request.contextPath}/auth/logout" 
           style="font-size:13px; color:#ef4444; text-decoration:none; font-weight:600; border:1px solid #fee2e2; padding:4px 12px; border-radius:8px;">
           로그아웃
        </a>
    </div>
</header>

<script>
(function() {
    var contextPath = '${pageContext.request.contextPath}';
    var INITIAL_TIME = 30*60;
    var timeLeft = INITIAL_TIME;
    var isExpiring = false; 

    var timerDisplay = document.getElementById('timer-display');
    var sessionBox = document.getElementById('session-box');

    function updateBreadcrumb() {
        const currentPath = window.location.pathname;
        const sidebar = document.getElementById('sidebar');
        if (!sidebar) return;

        const navItems = sidebar.querySelectorAll('.nav-item');
        let matched = false;

        navItems.forEach(item => {
            const href = item.getAttribute('href');
            if (href && currentPath.endsWith(href.replace(contextPath, '')) && href !== contextPath + '/') {
                const subMenu = item.textContent.trim();
                const parentGroup = item.closest('.nav-group');
                const mainMenu = parentGroup ? parentGroup.querySelector('.nav-group-header').textContent.trim() : "메인";

                document.getElementById('parent-menu-text').textContent = mainMenu;
                document.getElementById('current-menu-text').textContent = subMenu;
                matched = true;
            }
        });

        if (!matched) {
            document.getElementById('parent-menu-text').textContent = "메인";
            document.getElementById('current-menu-text').textContent = "대시보드";
        }
    }

    function handleSessionOut() {
        if (isExpiring) return; 
        isExpiring = true;
        clearInterval(timerInterval);
        location.href = contextPath + "/auth/login?msg=session_expired";
    }

    function updateTimer() {
        var min = Math.floor(timeLeft / 60);
        var sec = timeLeft % 60;
        if(timerDisplay) timerDisplay.textContent = (min < 10 ? '0' + min : min) + ":" + (sec < 10 ? '0' + sec : sec);

        if (timeLeft < 300) {
            if(sessionBox) sessionBox.style.backgroundColor = '#fff1f2';
            if(timerDisplay) timerDisplay.style.color = '#e11d48';
        } else {
            if(sessionBox) sessionBox.style.backgroundColor = '#f0f4f8';
            if(timerDisplay) timerDisplay.style.color = '#2563eb';
        }

        if (timeLeft <= 0) {
            handleSessionOut();
            return;
        }
        timeLeft--;
    }
    var timerInterval = setInterval(updateTimer, 1000);

    window.extendSession = function() {
        fetch(contextPath + '/api/notification/count').then(function(res) {
            if (res.ok) {
                timeLeft = INITIAL_TIME;
                timerDisplay.style.color = '#10b981';
                setTimeout(function() {
                    timerDisplay.style.color = (timeLeft < 300) ? '#e11d48' : '#2563eb';
                }, 500);
            } else { handleSessionOut(); }
        });
    };

    window.addEventListener('load', function() {
        updateBreadcrumb();
        updateTimer();
    });
})();
</script>