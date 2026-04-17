<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>ERP-HRMS - 대시보드 (관리자)</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/main/dashboard.css">
</head>
<body>
    <jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />
    <div id="main-wrapper">
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
        <main class="app-content">

            <%-- ① 출퇴근 카드 — 관리자도 직원이므로 공통 표시 --%>
            <div class="att-card">
                <div class="att-date" id="attDate"></div>
                <div class="att-info">
                    <c:choose>
                        <c:when test="${not empty todayAtt and not empty todayAtt.checkIn}">
                            출근 <strong><c:out value="${todayAtt.checkIn}" /></strong>
                            <c:if test="${not empty todayAtt.checkOut}">
                                &nbsp;→&nbsp; 퇴근 <strong><c:out value="${todayAtt.checkOut}" /></strong>
                            </c:if>
                        </c:when>
                        <c:otherwise>
                            <span style="color:var(--gray-400);">오늘 출근 기록이 없습니다.</span>
                        </c:otherwise>
                    </c:choose>
                </div>
                <div class="att-btns">
                    <form action="${pageContext.request.contextPath}/att/record" method="post">
                        <input type="hidden" name="action" value="checkin">
                        <button type="submit" class="btn btn-primary"
                                ${not empty todayAtt and not empty todayAtt.checkIn ? 'disabled' : ''}>
                            출근하기
                        </button>
                    </form>
                    <form action="${pageContext.request.contextPath}/att/record" method="post">
                        <input type="hidden" name="action" value="checkout">
                        <button type="submit" class="btn btn-secondary"
                                ${empty todayAtt or empty todayAtt.checkIn or not empty todayAtt.checkOut ? 'disabled' : ''}>
                            퇴근하기
                        </button>
                    </form>
                </div>
            </div>

            <%-- ② 시스템 관리 지표 배지 — 관리자 전용 항목만 표시
                 결재대기·급여처리는 HR담당자 업무이므로 제외 --%>
            <div class="widget-grid widget-grid-2">
                <div class="widget-card ${dashboard.lockedAccountCount > 0 ? 'danger' : ''}">
                    <div class="widget-label">잠금 계정</div>
                    <div class="widget-value"><c:out value="${dashboard.lockedAccountCount}" />건</div>
                    <a href="${pageContext.request.contextPath}/sys/accountUnlock" class="card-link">해제 →</a>
                </div>
                <div class="widget-card ${dashboard.incompleteEvalCount > 0 ? 'warn' : ''}">
                    <div class="widget-label">평가 미완료</div>
                    <div class="widget-value"><c:out value="${dashboard.incompleteEvalCount}" />건</div>
                    <a href="${pageContext.request.contextPath}/eval/status" class="card-link">확인 →</a>
                </div>
            </div>

            <%-- ③ 본인 급여·연차 — 관리자도 직원이므로 표시 --%>
            <div class="widget-grid widget-grid-3 personal-section">
                <div class="widget-card">
                    <div class="widget-label">내 잔여 연차</div>
                    <div class="widget-value primary"><c:out value="${dashboard.remainDays}" />일</div>
                    <div class="widget-sub">
                        사용 <c:out value="${dashboard.usedDays}" /> /
                        부여 <c:out value="${dashboard.totalDays}" />
                    </div>
                </div>
                <div class="widget-card">
                    <div class="widget-label">내 이번달 실수령액</div>
                    <div class="widget-value salary-wrap">
                        <span id="salaryVal" class="blurred">
                            <fmt:formatNumber value="${dashboard.netSalary}" pattern="#,###" />원
                        </span>
                        <button class="btn-reveal" onclick="revealSalary()" id="revealBtn">👁 보기</button>
                    </div>
                    <div class="widget-sub">
                        <c:choose>
                            <c:when test="${dashboard.salaryStatus == '완료'}">
                                <span class="text-green">✔ 지급완료</span>
                            </c:when>
                            <c:otherwise><span class="text-gray">대기중</span></c:otherwise>
                        </c:choose>
                    </div>
                </div>
                <div class="widget-card">
                    <div class="widget-label">내 이번달 근무시간</div>
                    <div class="widget-value">
                        <fmt:formatNumber value="${dashboard.monthWorkHours}" pattern="#,##0.0" />h
                    </div>
                    <div class="widget-sub">
                        <c:choose>
                            <c:when test="${dashboard.monthOvertimeHours > 0}">
                                초과 +<c:out value="${dashboard.monthOvertimeHours}" />h
                            </c:when>
                            <c:otherwise>초과근무 없음</c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>

            <%-- ④ 최근 감사로그 — 시스템 관리자 핵심 정보 --%>
            <div class="bottom-grid bottom-grid-1">
                <div class="info-card">
                    <div class="info-card-title">🔍 최근 변경 이력</div>
                    <c:choose>
                        <c:when test="${empty dashboard.recentAuditLogs}">
                            <p class="empty-txt">이력이 없습니다.</p>
                        </c:when>
                        <c:otherwise>
                            <ul class="audit-list">
                                <c:forEach var="log" items="${dashboard.recentAuditLogs}">
                                    <li>
                                        <span class="audit-time"><c:out value="${log.logTime}" /></span>
                                        <c:out value="${log.actorName}" /> ·
                                        <c:out value="${log.target_table}" />
                                        [<c:out value="${log.action}" />]
                                    </li>
                                </c:forEach>
                            </ul>
                        </c:otherwise>
                    </c:choose>
                    <a href="${pageContext.request.contextPath}/sys/auditLog" class="card-link">전체 보기 →</a>
                </div>
            </div>

        </main>
    </div>

    <%-- [버그수정] script src 태그와 인라인 코드 분리 --%>
    <script src="${pageContext.request.contextPath}/js/sidebar.js"></script>
    <script>
        function updateClock() {
            var now  = new Date();
            var days = ['일','월','화','수','목','금','토'];
            var str  = now.getFullYear() + '년 ' +
                       (now.getMonth() + 1) + '월 ' +
                       now.getDate() + '일 (' + days[now.getDay()] + ') ' +
                       now.getHours().toString().padStart(2, '0') + ':' +
                       now.getMinutes().toString().padStart(2, '0');
            var el = document.getElementById('attDate');
            if (el) el.textContent = str;
        }
        updateClock();
        setInterval(updateClock, 1000);

        function revealSalary() {
            var val = document.getElementById('salaryVal');
            var btn = document.getElementById('revealBtn');
            if (val) val.classList.remove('blurred');
            if (btn) btn.style.display = 'none';
        }
    </script>
</body>
</html>
