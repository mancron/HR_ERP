<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ERP-HRMS - 변경 이력 조회 (audit_log)</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/sys/audit.css">
    <style>

    </style>
</head>
<body>

    <jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />

    <div id="main-wrapper">
        <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
        
        <main class="app-content">
            <h1 style="font-size: 20px; font-weight: 700; margin-bottom: 20px;">변경 이력 조회 (audit_log)</h1>
            
            <div class="search-container">
                <form action="${pageContext.request.contextPath}/sys/auditLog" method="get" class="search-form">
                    <div class="form-group">
                        <label for="target_table">대상 테이블</label>
                        <select name="target_table" id="target_table" class="form-control">
                            <option value="">전체</option>
                            <option value="employee" ${param.target_table == 'employee' ? 'selected' : ''}>employee (직원)</option>
                            <option value="account" ${param.target_table == 'account' ? 'selected' : ''}>account (계정)</option>
                            <option value="annual_leave" ${param.target_table == 'annual_leave' ? 'selected' : ''}>annual_leave (연차)</option>
                            <option value="salary" ${param.target_table == 'salary' ? 'selected' : ''}>salary (급여)</option>
                        </select>
                    </div>
                    
                    <div class="form-group">
                        <label for="start_date">조회 기간</label>
                        <input type="date" name="start_date" id="start_date" class="form-control" value="${param.start_date}">
                        <span>~</span>
                        <input type="date" name="end_date" id="end_date" class="form-control" value="${param.end_date}">
                    </div>
                    
                    <div class="form-group" style="margin-left: auto;">
                        <button type="button" class="btn btn-secondary" onclick="location.href='${pageContext.request.contextPath}/sys/auditLog'">초기화</button>
                        <button type="submit" class="btn btn-primary">조회</button>
                    </div>
                </form>
            </div>

            <div class="table-container">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Log ID</th>
                            <th>작업자</th>
                            <th>대상 테이블</th>
                            <th>대상 ID</th>
                            <th>작업</th>
                            <th>컬럼명</th>
                            <th>변경 전</th>
                            <th>변경 후</th>
                            <th>변경 일시</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${empty logList}">
                                <tr>
                                    <td colspan="9" class="empty-row">조회된 변경 이력 내역이 없습니다.</td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="log" items="${logList}">
                                    <tr>
                                        <td>${log.logId}</td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${not empty log.actorName}">${log.actorName}</c:when>
                                                <c:otherwise><span style="color: #999;">시스템</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>${log.targetTable}</td>
                                        <td>${log.targetId}</td>
                                        <td>
                                            <span style="color: ${log.action == 'INSERT' ? 'green' : (log.action == 'DELETE' ? 'red' : 'blue')}; font-weight: bold;">
                                                ${log.action}
                                            </span>
                                        </td>
                                        <td>${log.columnName != null ? log.columnName : '-'}</td>
                                        <td>${log.oldValue != null ? log.oldValue : '-'}</td>
                                        <td>${log.newValue != null ? log.newValue : '-'}</td>
                                        <td>
                                            ${log.createdAt}
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>

        </main>
    </div>

    <script src="${pageContext.request.contextPath}/js/sidebar.js"></script>
    <script>
        // 날짜 검증 로직 (종료일이 시작일보다 앞설 수 없음)
        document.querySelector('.search-form').addEventListener('submit', function(e) {
            const startDate = document.getElementById('start_date').value;
            const endDate = document.getElementById('end_date').value;
            
            if (startDate && endDate && startDate > endDate) {
                e.preventDefault();
                alert('종료일은 시작일보다 빠를 수 없습니다.');
            }
        });
    </script>
</body>
</html>