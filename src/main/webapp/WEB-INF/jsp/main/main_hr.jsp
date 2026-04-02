<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>HR ERP - 대시보드 (HR담당자)</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/main/dashboard.css">
</head>
<body>
    <jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />
    <div id="main-wrapper">
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
        <main class="app-content">
				<%-- 출퇴근 카드 --%>
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
            <%-- ① 요약 배지 --%>
            <div class="widget-grid">
                <div class="widget-card warn">
                    <div class="widget-label">휴가 결재 대기</div>
                    <div class="widget-value"><c:out value="${dashboard.pendingLeaveCount}" />건</div>
                    <a href="${pageContext.request.contextPath}/att/leave/approve" class="card-link">처리하기 →</a>
                </div>
                <div class="widget-card warn">
                    <div class="widget-label">초과근무 결재 대기</div>
                    <div class="widget-value"><c:out value="${dashboard.pendingOtCount}" />건</div>
                    <a href="${pageContext.request.contextPath}/att/overtime" class="card-link">처리하기 →</a>
                </div>
                <div class="widget-card">
                    <div class="widget-label">이번달 급여 처리</div>
                    <div class="widget-value">
                        <c:out value="${dashboard.salaryDoneCount}" /> /
                        <c:out value="${dashboard.salaryTotalCount}" />명
                    </div>
                    <div class="widget-sub">완료</div>
                </div>
            </div>

            <%-- ② 결재 대기 목록 + 부서별 근태 --%>
            <div class="bottom-grid">

                <%-- 결재 대기 목록 --%>
                <div class="info-card">
                    <div class="info-card-title">⏳ 휴가 결재 대기</div>
                    <c:choose>
                        <c:when test="${empty dashboard.pendingLeaves}">
                            <p class="empty-txt">대기 중인 휴가 신청이 없습니다.</p>
                        </c:when>
                        <c:otherwise>
                            <table class="mini-table">
                                <thead>
                                    <tr><th>이름</th><th>부서</th><th>유형</th><th>기간</th><th>일수</th></tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="lv" items="${dashboard.pendingLeaves}">
                                        <tr>
                                            <td><c:out value="${lv.emp_name}" /></td>
                                            <td><c:out value="${lv.dept_name}" /></td>
                                            <td><c:out value="${lv.leave_type}" /></td>
                                            <td><c:out value="${lv.startDt}" />~<c:out value="${lv.endDt}" /></td>
                                            <td><c:out value="${lv.days}" />일</td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </c:otherwise>
                    </c:choose>
                </div>

                <%-- 부서별 오늘 근태 --%>
                <div class="info-card">
                    <div class="info-card-title">📊 부서별 오늘 근태</div>
                    <c:choose>
                        <c:when test="${empty dashboard.deptAttendance}">
                            <p class="empty-txt">데이터가 없습니다.</p>
                        </c:when>
                        <c:otherwise>
                            <table class="mini-table">
                                <thead>
                                    <tr><th>부서</th><th>전체</th><th>출근</th><th>지각</th><th>미출근</th></tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="dept" items="${dashboard.deptAttendance}">
                                        <tr>
                                            <td><c:out value="${dept.dept_name}" /></td>
                                            <td><c:out value="${dept.totalEmp}" /></td>
                                            <td><c:out value="${dept.attendCount}" /></td>
                                            <td class="${dept.lateCount > 0 ? 'text-warn' : ''}">
                                                <c:out value="${dept.lateCount}" />
                                            </td>
                                            <td class="${dept.absentCount > 0 ? 'text-danger' : ''}">
                                                <c:out value="${dept.absentCount}" />
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </c:otherwise>
                    </c:choose>
                </div>

            </div>

        </main>
    </div>
    <script src="${pageContext.request.contextPath}/js/sidebar.js"></script>
    <script>
    function updateClock() {
        var now = new Date();
        var days = ['일','월','화','수','목','금','토'];
        var str = now.getFullYear() + '년 ' +
                  (now.getMonth()+1) + '월 ' +
                  now.getDate() + '일 (' + days[now.getDay()] + ') ' +
                  now.getHours().toString().padStart(2,'0') + ':' +
                  now.getMinutes().toString().padStart(2,'0');
        document.getElementById('attDate') &&
        (document.getElementById('attDate').textContent = str);
    }
    updateClock();
    setInterval(updateClock, 1000);
    </script>
</body>
</html>