<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>HR ERP - 메인 대시보드</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>

`
 `   <!-- 사이드바 -->
    <jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />

  <div id="main-wrapper">
    <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
    <main class="app-content">
            <c:choose>
                <%-- 1. viewPage가 있는 경우 (서블릿에서 전달된 경우) --%>
                <c:when test="${not empty viewPage}">
                    <jsp:include page="${viewPage}" />
                </c:when>
                
                <%-- 2. viewPage가 없는 경우 (메인 페이지 접속 시 기본 화면) --%>
                <c:otherwise>
                    <h1 style="font-size: 20px; font-weight: 700;">메인 대시보드 영역</h1>
                    <p style="margin-top: 10px; color: var(--gray-500);">위젯 및 현황판 데이터 바인딩 위치</p>
                </c:otherwise>
            </c:choose>
        </main>
  </div>


</body>

<script src="${pageContext.request.contextPath}/js/sidebar.js"></script>

</html>