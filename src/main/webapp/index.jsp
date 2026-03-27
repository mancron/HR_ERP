<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>HR ERP - 메인 대시보드</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<%
	//브라우저 캐시 방지 설정 (뒤로가기 시 서버에 다시 요청하게 함)
	response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
	response.setHeader("Pragma", "no-cache"); // HTTP 1.0
	response.setDateHeader("Expires", 0); // Proxies
    // 세션이 없거나 userName이 없으면 로그인 페이지로 강제 이동
    if (session == null || session.getAttribute("userName") == null) {
        response.sendRedirect(request.getContextPath() + "/auth/login");
        return; // 아래 HTML 코드가 실행되지 않도록 즉시 종료
    }
%>
<body>

`
 `   <!-- 사이드바 -->
    <jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />

  <div id="main-wrapper">
    <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
    <main class="main-content" style="background-color: #f4f7f9; padding: 20px;">
    <%
        // 서블릿에서 "viewPage"라는 이름으로 경로를 넘겨줄 겁니다.
        String viewPage = (String) request.getAttribute("viewPage");
        if (viewPage != null) {
            // 비밀번호 변경 등 특정 메뉴를 클릭했을 때
    %>
            <jsp:include page="<%= viewPage %>" />
    <%
        } else {
            // viewPage가 없으면 기존 대시보드(기본 세팅) 내용을 그대로 보여줍니다.
    %>
            <h2>대시보드 메인</h2>
            <p>현재 진행 중인 업무 현황...</p>
            <%
        }
    %>
</main>
  </div>


</body>

<script src="${pageContext.request.contextPath}/js/sidebar.js"></script>

</html>