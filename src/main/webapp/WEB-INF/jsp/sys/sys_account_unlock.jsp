<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>HR ERP - account_unlock</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>


    <!-- 사이드바 -->
    <jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />

  <div id="main-wrapper">
    <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
    <main class="app-content">
      <h1 style="font-size: 20px; font-weight: 700;">account_unlock</h1>
      <p style="margin-top: 10px; color: var(--gray-500);">위젯 및 현황판 데이터 바인딩 위치</p>
    </main>
  </div>


</body>

<script src="${pageContext.request.contextPath}/js/sidebar.js"></script>

</html>