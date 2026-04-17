<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ERP-HRMS - 알림 목록</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/sys/notification.css">
</head>
<body>

    <jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />

    <div id="main-wrapper">
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />

        <main class="app-content">
            <h1 class="page-title">알림 목록</h1>

            <%-- 필터 탭 + 전체 읽음 처리 버튼 --%>
            <div class="noti-toolbar">
                <div class="filter-tabs">
                    <a href="${pageContext.request.contextPath}/notification"
                       class="btn btn-sm ${empty filter or filter == 'all' ? 'btn-primary' : 'btn-secondary'}">
                        전체
                    </a>
                    <a href="${pageContext.request.contextPath}/notification?filter=unread"
                       class="btn btn-sm ${filter == 'unread' ? 'btn-primary' : 'btn-secondary'}">
                        미읽음
                        <c:if test="${unreadCount > 0}">
                            <span class="unread-cnt">(<c:out value="${unreadCount}" />)</span>
                        </c:if>
                    </a>
                </div>

                <c:if test="${unreadCount > 0}">
                    <form action="${pageContext.request.contextPath}/notification"
                          method="post"
                          onsubmit="return confirm('모든 알림을 읽음 처리하시겠습니까?')">
                        <input type="hidden" name="action" value="markAll">
                        <button type="submit" class="btn btn-secondary btn-sm">전체 읽음 처리</button>
                    </form>
                </c:if>
            </div>

            <%-- 알림 목록 --%>
            <div class="noti-list card">
                <c:choose>
                    <c:when test="${empty notiList}">
                        <div class="noti-empty">
                            🔔 알림이 없습니다.
                        </div>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="noti" items="${notiList}">
                            <%-- 미읽음이면 클릭 시 읽음 처리 POST 후 리다이렉트 --%>
                            <div class="noti-item ${noti.isRead == 0 ? 'unread' : ''}">
                                <div class="noti-body">
                                    <span class="badge <c:out value="${noti.badgeColor}" />">
                                        <c:out value="${noti.notiType}" />
                                    </span>
                                    <div class="noti-message">
                                        <c:out value="${noti.message}" />
                                    </div>
                                </div>
                                <div class="noti-meta">
                                    <span class="noti-time">
                                        <c:out value="${noti.createdAtStr}" />
                                    </span>
                                    <c:if test="${noti.isRead == 0}">
                                        <form action="${pageContext.request.contextPath}/notification"
                                              method="post" class="inline-form">
                                            <input type="hidden" name="action" value="markOne">
                                            <input type="hidden" name="notiId" value="${noti.notiId}">
                                            <button type="submit" class="btn-read">읽음</button>
                                        </form>
                                    </c:if>
                                    <c:if test="${noti.isRead == 1}">
                                        <span class="read-label">읽음</span>
                                    </c:if>
                                </div>
                            </div>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </div>

        </main>
    </div>

    <script src="${pageContext.request.contextPath}/js/sidebar.js"></script>
</body>
</html>