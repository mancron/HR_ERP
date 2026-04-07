<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>HR ERP - 대시보드</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/main/dashboard.css">
</head>
<body>
    <jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />
    <div id="main-wrapper">
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
        <main class="app-content">

            <%-- ============================================================
                 사이드바의 role 변수와 동일한 방식으로 선언
                 ============================================================ --%>
            <c:set var="role"      value="${sessionScope.userRole}" />
            <c:set var="isAdmin"   value="${role == '관리자'}" />
            <c:set var="isManager" value="${role == '관리자' || role == 'HR담당자'}" />

            <%-- ──────────────────────────────────────────────────────────────
                 ① 출퇴근 카드 — 공통 (중복 제거, 1곳만 관리)
                 ────────────────────────────────────────────────────────────── --%>
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

            <%-- ──────────────────────────────────────────────────────────────
                 ② 요약 배지 행
                    · 관리자   : 결재 대기 + 잠금계정 + 평가미완료 (급여처리 제외)
                    · HR담당자 : 결재 대기 + 급여처리
                    · 일반직원 : 연차·급여·근무시간 3개 위젯
                 ────────────────────────────────────────────────────────────── --%>
            <c:choose>

                <%-- 관리자 배지 — 시스템 관리 지표만 (급여처리 제외) --%>
                <c:when test="${isAdmin}">
                    <div class="widget-grid widget-grid-4">

                        <div class="widget-card warn">
                            <div class="widget-label">휴가 결재 대기</div>
                            <div class="widget-value"><c:out value="${dashboard.pendingLeaveCount}" />건</div>
                            <a href="${pageContext.request.contextPath}/att/leave/approve" class="card-link">처리 →</a>
                        </div>

                        <div class="widget-card warn">
                            <div class="widget-label">초과근무 결재 대기</div>
                            <div class="widget-value"><c:out value="${dashboard.pendingOtCount}" />건</div>
                            <a href="${pageContext.request.contextPath}/att/overtime/approve" class="card-link">처리 →</a>
                        </div>

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
                </c:when>

                <%-- HR담당자 배지 — 결재 대기 + 급여처리 --%>
                <c:when test="${isManager}">
                    <div class="widget-grid widget-grid-3">

                        <div class="widget-card warn">
                            <div class="widget-label">휴가 결재 대기</div>
                            <div class="widget-value"><c:out value="${dashboard.pendingLeaveCount}" />건</div>
                            <a href="${pageContext.request.contextPath}/att/leave/approve" class="card-link">처리 →</a>
                        </div>

                        <div class="widget-card warn">
                            <div class="widget-label">초과근무 결재 대기</div>
                            <div class="widget-value"><c:out value="${dashboard.pendingOtCount}" />건</div>
                            <a href="${pageContext.request.contextPath}/att/overtime/approve" class="card-link">처리 →</a>
                        </div>

                        <div class="widget-card">
                            <div class="widget-label">이번달 급여 처리</div>
                            <div class="widget-value">
                                <c:out value="${dashboard.salaryDoneCount}" />/<c:out value="${dashboard.salaryTotalCount}" />명
                            </div>
                            <div class="widget-sub">완료</div>
                        </div>

                    </div>
                </c:when>

                <%-- 일반직원 위젯 3개 --%>
                <c:otherwise>
                    <div class="widget-grid widget-grid-3">

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

                        <%-- 이번달 실수령액 (모자이크 처리) --%>
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
                                <c:choose>
                                    <c:when test="${dashboard.monthOvertimeHours > 0}">
                                        초과 +<c:out value="${dashboard.monthOvertimeHours}" />h
                                    </c:when>
                                    <c:otherwise>초과근무 없음</c:otherwise>
                                </c:choose>
                            </div>
                        </div>

                    </div>
                </c:otherwise>
            </c:choose>

            <%-- ──────────────────────────────────────────────────────────────
                 ③ 개인 정보 섹션 — 관리자·HR담당자도 본인 연차·급여 확인
                    (일반직원은 위 ②에서 이미 표시했으므로 제외)
                 ────────────────────────────────────────────────────────────── --%>
            <c:if test="${isManager}">
                <div class="widget-grid widget-grid-3 personal-section">

                    <div class="widget-card">
                        <div class="widget-label">내 잔여 연차</div>
                        <div class="widget-value primary">
                            <c:out value="${dashboard.remainDays}" />일
                        </div>
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
                                <c:otherwise>
                                    <span class="text-gray">대기중</span>
                                </c:otherwise>
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
            </c:if>

            <%-- ──────────────────────────────────────────────────────────────
                 ④ 하단 그리드
                    · 관리자   : 결재 대기 목록 + 감사로그 (부서별 근태 제외)
                    · HR담당자 : 결재 대기 목록 + 부서별 근태
                    · 일반직원 : 내 신청 현황 + 최근 알림
                 ────────────────────────────────────────────────────────────── --%>
            <c:choose>

                <%-- 관리자 하단 그리드 — 결재 대기 + 감사로그 --%>
                <c:when test="${isAdmin}">
                    <div class="bottom-grid bottom-grid-2">

                        <%-- 결재 대기 목록 --%>
                        <div class="info-card">
                            <div class="info-card-title">⏳ 결재 대기</div>
                            <c:choose>
                                <c:when test="${empty dashboard.pendingLeaves and empty dashboard.pendingOts}">
                                    <p class="empty-txt">대기 중인 결재가 없습니다.</p>
                                </c:when>
                                <c:otherwise>
                                    <table class="mini-table">
                                        <thead>
                                            <tr><th>구분</th><th>이름</th><th>부서</th><th>내용</th><th>기간</th></tr>
                                        </thead>
                                        <tbody>
                                            <c:forEach var="lv" items="${dashboard.pendingLeaves}">
                                                <tr>
                                                    <td><span class="req-type">휴가</span></td>
                                                    <td><c:out value="${lv.emp_name}" /></td>
                                                    <td><c:out value="${lv.dept_name}" /></td>
                                                    <td><c:out value="${lv.leave_type}" /></td>
                                                    <td><c:out value="${lv.startDt}" />~<c:out value="${lv.endDt}" /></td>
                                                </tr>
                                            </c:forEach>
                                            <c:forEach var="ot" items="${dashboard.pendingOts}">
                                                <tr>
                                                    <td><span class="req-type">초과</span></td>
                                                    <td><c:out value="${ot.emp_name}" /></td>
                                                    <td><c:out value="${ot.dept_name}" /></td>
                                                    <td><c:out value="${ot.ot_hours}" />h</td>
                                                    <td><c:out value="${ot.otDt}" /></td>
                                                </tr>
                                            </c:forEach>
                                        </tbody>
                                    </table>
                                </c:otherwise>
                            </c:choose>
                        </div>

                        <%-- 최근 감사로그 --%>
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
                </c:when>

                <%-- HR담당자 하단 그리드 — 결재 대기 + 부서별 근태 --%>
                <c:when test="${isManager}">
                    <div class="bottom-grid bottom-grid-2">

                        <%-- 결재 대기 목록 --%>
                        <div class="info-card">
                            <div class="info-card-title">⏳ 결재 대기</div>
                            <c:choose>
                                <c:when test="${empty dashboard.pendingLeaves and empty dashboard.pendingOts}">
                                    <p class="empty-txt">대기 중인 결재가 없습니다.</p>
                                </c:when>
                                <c:otherwise>
                                    <table class="mini-table">
                                        <thead>
                                            <tr><th>구분</th><th>이름</th><th>부서</th><th>내용</th><th>기간</th></tr>
                                        </thead>
                                        <tbody>
                                            <c:forEach var="lv" items="${dashboard.pendingLeaves}">
                                                <tr>
                                                    <td><span class="req-type">휴가</span></td>
                                                    <td><c:out value="${lv.emp_name}" /></td>
                                                    <td><c:out value="${lv.dept_name}" /></td>
                                                    <td><c:out value="${lv.leave_type}" /></td>
                                                    <td><c:out value="${lv.startDt}" />~<c:out value="${lv.endDt}" /></td>
                                                </tr>
                                            </c:forEach>
                                            <c:forEach var="ot" items="${dashboard.pendingOts}">
                                                <tr>
                                                    <td><span class="req-type">초과</span></td>
                                                    <td><c:out value="${ot.emp_name}" /></td>
                                                    <td><c:out value="${ot.dept_name}" /></td>
                                                    <td><c:out value="${ot.ot_hours}" />h</td>
                                                    <td><c:out value="${ot.otDt}" /></td>
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
                </c:when>

                <%-- 일반직원 하단 그리드 --%>
                <c:otherwise>
                    <div class="bottom-grid bottom-grid-2">

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
                            <a href="${pageContext.request.contextPath}/att/leave/req" class="card-link">휴가 신청하기 →</a>
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
                            <a href="${pageContext.request.contextPath}/notification" class="card-link">전체 알림 보기 →</a>
                        </div>

                    </div>
                </c:otherwise>
            </c:choose>

        </main>
    </div>

    <script src="${pageContext.request.contextPath}/js/sidebar.js"></script>
    <script>
        /* 날짜·시간 실시간 표시 */
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

        /* 실수령액 모자이크 해제 */
        function revealSalary() {
            var val = document.getElementById('salaryVal');
            var btn = document.getElementById('revealBtn');
            if (val) val.classList.remove('blurred');
            if (btn) btn.style.display = 'none';
        }
    </script>
</body>
</html>
	