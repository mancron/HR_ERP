<!-- emp/list.jsp -->
<%@ page contentType="text/html; charset=UTF-8"%>
<%
	
%>
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
      <h1 style="font-size: 20px; font-weight: 700;">직원 목록</h1>
      <div class="search-bar">
      	<input class="search-input" placeholder="이름 또는 사번 검색">
      	<select class="search-select">
      	 	<!-- 부서목록 가져오기 -->
      	 		<option>전체 부서</option>
      		<!-- /부서목옥 가져오기 -->
      	</select>
      	<select class="search-select">
      	 	<!-- 직급목록 가져오기 -->
      	 		<option>전체 직급</option>
      		<!-- /직급목옥 가져오기 -->
      	</select>
      	<select class="search-select">
      	 	<!-- 직급목록 가져오기 -->
      	 		<option>전체</option>
      		<!-- /직급목옥 가져오기 -->
      	</select>
      	<button>검색</button>
      	<button>+ 직원 등록</button>
      </div>
      <div class="card" style="padding:0;">
      	<table>
      		<thead>
      			<tr>
      				<th>사번</th>
      				<th>이름</th>
      				<th>부서</th>
      				<th>직급</th>
      				<th>입사일</th>
      				<th>고용형태</th>
      				<th>상태</th>
      				<th>관리</th>
      			</tr>
      		</thead>
      		<tbody>
      			<!-- 직원 내용(반복) -->
      			<tr>
      				<td></td>
      				<td></td>
      				<td></td>
      				<td></td>
      				<td></td>
      				<td></td>
      				<td></td>
      				<td><button>상세</button></td>
      			</tr>
      			<!-- /직원 내용 -->
      		</tbody>
      	</table>
      </div>
      <div class="pagination">
      	<!-- 페이지 버튼 만들기 -->
      </div>
    </main>
    
  </div>


</body>

<script src="${pageContext.request.contextPath}/js/sidebar.js"></script>

</html>