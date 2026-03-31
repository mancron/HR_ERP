<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>HR ERP - 메인 대시보드</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>

  <!-- 사이드바 -->
  <jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />

  <div id="main-wrapper">
    <jsp:include page="/WEB-INF/jsp/common/header.jsp" />

    <main class="app-content">

      <%-- viewPage가 비어있으면 무조건 대시보드 --%>
      <c:if test="${empty viewPage}">
        <h1 style="font-size: 20px; font-weight: 700;">메인 대시보드 영역</h1>
        <p style="margin-top: 10px; color: var(--gray-500);">위젯 및 현황판 데이터 바인딩 위치</p>
      </c:if>

      <%-- viewPage가 있고, 순환 참조가 아닌 경우만 include --%>
      <c:if test="${not empty viewPage}">
        <c:choose>
          <c:when test="${viewPage == '/index.jsp' || fn:contains(viewPage, 'main')}">
            <%-- 순환 참조 방지: 대시보드로 fallback --%>
            <h1 style="font-size: 20px; font-weight: 700;">메인 대시보드 영역</h1>
            <p style="margin-top: 10px; color: var(--gray-500);">위젯 및 현황판 데이터 바인딩 위치</p>
          </c:when>
          <c:otherwise>
            <jsp:include page="${viewPage}" />
          </c:otherwise>
        </c:choose>
      </c:if>

    </main>
  </div>

</body>
<script src="${pageContext.request.contextPath}/js/sidebar.js"></script>
</html>