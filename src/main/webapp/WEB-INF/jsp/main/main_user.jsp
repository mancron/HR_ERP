<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>ERP-HRMS - 대시보드</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/main/dashboard.css">
</head>
<body>
    <jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />
    <div id="main-wrapper">
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
        <main class="app-content">

            <%-- ① 출퇴근 카드 --%>
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

            <%-- ② 요약 위젯 3개 --%>
            <div class="widget-grid">

                <%-- 잔여 연차 --%>
                <div class="widget-card">
                    <div class="widget-label">잔여 연차</div>
                    <div class="widget-value primary">
                        <c:out value="${dashboard.remainDays}" />일
                    </div>
                    <div class="widget-sub">
                        사용 <c:out value="${dashboard.usedDays}" /> /
                        부여 <c:out value="${dashboard.totalDays}" />
                    </div>
                </div>

                <%-- 이번달 실수령액 (모자이크) --%>
                <div class="widget-card">
                    <div class="widget-label">이번달 실수령액</div>
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
                            <c:otherwise>
                                <span class="text-gray">대기중</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>

                <%-- 이번달 근무시간 --%>
                <div class="widget-card">
                    <div class="widget-label">이번달 근무시간</div>
                    <div class="widget-value">
                        <fmt:formatNumber value="${dashboard.monthWorkHours}" pattern="#,##0.0" />h
                    </div>
                    <div class="widget-sub">
                        <c:if test="${dashboard.monthOvertimeHours > 0}">
                            초과 +<c:out value="${dashboard.monthOvertimeHours}" />h
                        </c:if>
                        <c:if test="${dashboard.monthOvertimeHours == 0}">
                            초과근무 없음
                        </c:if>
                    </div>
                </div>

            </div>

            <%-- ③ 하단 2열 --%>
            <div class="bottom-grid">

                <%-- 내 신청 현황 --%>
                <div class="info-card">
                    <div class="info-card-title">📋 내 신청 현황</div>
                    <c:choose>
                        <c:when test="${empty dashboard.recentRequests}">
                            <p class="empty-txt">신청 내역이 없습니다.</p>
                        </c:when>
                        <c:otherwise>
                            <ul class="req-list">
                                <c:forEach var="req" items="${dashboard.recentRequests}">
                                    <li>
                                        <span class="req-type"><c:out value="${req.type}" /></span>
                                        <c:out value="${req.startDt}" />
                                        <c:if test="${not empty req.endDt}">
                                            ~ <c:out value="${req.endDt}" />
                                        </c:if>
                                        <span class="req-status status-${req.status}">
                                            → <c:out value="${req.status}" />
                                        </span>
                                    </li>
                                </c:forEach>
                            </ul>
                        </c:otherwise>
                    </c:choose>
                    <a href="${pageContext.request.contextPath}/att/leave/req"
                       class="card-link">휴가 신청하기 →</a>
                </div>

                <%-- 최근 알림 --%>
                <div class="info-card">
                    <div class="info-card-title">🔔 최근 알림</div>
                    <c:choose>
                        <c:when test="${empty dashboard.recentNotifications}">
                            <p class="empty-txt">새 알림이 없습니다.</p>
                        </c:when>
                        <c:otherwise>
                            <ul class="noti-list">
                                <c:forEach var="noti" items="${dashboard.recentNotifications}">
                                    <li class="${noti.is_read == 0 ? 'unread' : ''}">
                                        <c:out value="${noti.message}" />
                                    </li>
                                </c:forEach>
                            </ul>
                        </c:otherwise>
                    </c:choose>
                    <a href="${pageContext.request.contextPath}/notification"
                       class="card-link">전체 알림 보기 →</a>
                </div>

            </div>

        </main>
    </div>
    <script src="${pageContext.request.contextPath}/js/sidebar.js"></script>
    <script>
        // 현재 날짜/시간 표시
        function updateClock() {
            var now = new Date();
            var days = ['일','월','화','수','목','금','토'];
            var str = now.getFullYear() + '년 ' +
                      (now.getMonth()+1) + '월 ' +
                      now.getDate() + '일 (' + days[now.getDay()] + ') ' +
                      now.getHours().toString().padStart(2,'0') + ':' +
                      now.getMinutes().toString().padStart(2,'0');
            document.getElementById('attDate').textContent = str;
        }
        updateClock();
        setInterval(updateClock, 1000);

        // 실수령액 모자이크 해제
        function revealSalary() {
            document.getElementById('salaryVal').classList.remove('blurred');
            document.getElementById('revealBtn').style.display = 'none';
        }
    </script>
</body>
</html>