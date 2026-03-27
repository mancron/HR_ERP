<!-- emp/list.jsp -->
<%@ page contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="deptDao" class="com.hrms.org.dao.DeptDAO" scope="page"/>
<jsp:useBean id="PosDao" class="com.hrms.org.dao.PosDAO" scope="page"/>
<jsp:useBean id="empDao" class="com.hrms.emp.dao.EmpDAO" scope="page"/>
<c:set var="deptList" value="${deptDao.deptList()}" />
<c:set var="posList" value="${PosDao.posList()}" />
<c:set var="empList" value="${empDao.getEmpList()}" />
<%
	
%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>HR ERP</title>
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
      	<form action="list.jsp" method="get">
	      	<input class="search-input" placeholder="이름 또는 사번 검색">
	      	<select class="search-select">
	      		<option value="0">전체 부서</option>
	      	 	<!-- 부서목록 가져오기 -->
	      	 	<c:forEach var="dept" items="${deptList}">
	       			<option value="${dept.dept_id == dept.dept_id ? 'selected' : ''}">
	       				${dept.dept_name}
	       			</option>
	   			</c:forEach>
	      		<!-- /부서목옥 가져오기 -->
	      	</select>
	      	<select class="search-select">
	      	<option value="0">전체 직급</option>
	      	 	<!-- 직급목록 가져오기 -->
	      	 	<c:forEach var="pos" items="${posList}">
	       			<option value="${pos.position_id == pos.position_id ? 'selected' : ''}">
	       				${pos.position_name}
	       			</option>
	   			</c:forEach>
	      		<!-- /직급목록 가져오기 -->
	      	</select>
	      	<select name="status" class="search-select">
	      	 	<option value="1" ${param.status == '1' ? 'selected' : '' }>재직</option>
	      	 	<option value="2" ${param.status == '2' ? 'selected' : '' }>휴직</option>
	      	 	<option value="3" ${param.status == '3' ? 'selected' : '' }>퇴직</option>
	      		<option value="0">전체</option>
	      	</select>
	      	<button type="submit">검색</button>
	      	<button type="button" onClick="">+ 직원 등록</button>
      	</form>
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
      			<c:forEach var="emp" items="${empList}">
	      			<tr>
	      				<td>${emp.emp_no}</td>
	        		    <td><strong>${emp.emp_name}</strong></td>
	      				<td>${emp.dept_name}</td> 
	       	 		    <td>${emp.position_name}</td>
	       			    <td>${emp.hire_date}</td>
	            		<td>${emp.emp_type}</td>
	            		<td>${emp.status}</td>
	      				<td><button>상세</button></td>
	      			</tr>
	      		</c:forEach>
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