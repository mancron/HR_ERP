<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>

<head>
  <meta charset="UTF-8">
  <title>Insert title here</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>



<body>
  <nav id="sidebar">
    <div class="nav-logo">🏢 HR ERP</div>

    <div class="nav-group open">
      <div class="nav-group-header" onclick="toggleAccordion(this)">공통·인증</div>
      <div class="nav-group-content">
        <a href="${pageContext.request.contextPath}/main" class="nav-item">메인 대시보드</a>
        <a href="${pageContext.request.contextPath}/auth/pw-change" class="nav-item">비밀번호 변경</a>
        <a href="${pageContext.request.contextPath}/notification" class="nav-item">알림</a>
      </div>
    </div>

    <div class="nav-group">
      <div class="nav-group-header" onclick="toggleAccordion(this)">조직 관리</div>
      <div class="nav-group-content">
        <a href="${pageContext.request.contextPath}/org/dept" class="nav-item">부서 관리</a>
        <a href="${pageContext.request.contextPath}/org/position" class="nav-item">직급 관리</a>
      </div>
    </div>

    <div class="nav-group">
      <div class="nav-group-header" onclick="toggleAccordion(this)">직원 관리</div>
      <div class="nav-group-content">
        <a href="${pageContext.request.contextPath}/emp/list" class="nav-item">직원 목록</a>
        <a href="${pageContext.request.contextPath}/emp/reg" class="nav-item">직원 등록</a>
        <a href="${pageContext.request.contextPath}/emp/history" class="nav-item">인사발령 이력</a>
      </div>
    </div>
    <c:set var="isAdmin" value="${sessionScope.userRole == '관리자' || sessionScope.userRole == 'HR관리자'}" />
      <div class="nav-group">
        <div class="nav-group-header" onclick="toggleAccordion(this)">근태 관리</div>
        <div class="nav-group-content">
          <a href="${pageContext.request.contextPath}/att/record" class="nav-item">출퇴근</a>
          <a href="${pageContext.request.contextPath}/att/leave/req" class="nav-item">휴가 신청</a>
          <c:if test="${isAdmin}">
          	<a href="${pageContext.request.contextPath}/att/leave/approve" class="nav-item">휴가 승인</a>
          </c:if>
          <a href="/att/overtime" class="nav-item">초과근무</a>
          <c:if test="${isAdmin}">
          	<a href="/att/status" class="nav-item">근태 현황·보정</a>
          </c:if>
          <a href="/att/annual" class="nav-item">연차 현황</a>
          <c:if test="${isAdmin}">
          	<a href="/att/annual/grant" class="nav-item">연차 일괄 부여</a>
          </c:if>
        </div>
      </div>
    </div>

    <div class="nav-group">
      <div class="nav-group-header" onclick="toggleAccordion(this)">급여 관리</div>
      <div class="nav-group-content">
        <a href="/sal/calc" class="nav-item">급여 계산·지급</a>
        <a href="/sal/slip" class="nav-item">급여 명세서</a>
        <a href="/sal/status" class="nav-item">급여 현황</a>
        <a href="/sal/deduction" class="nav-item">공제율 관리</a>
      </div>
    </div>

      <div class="nav-group">
        <div class="nav-group-header" onclick="toggleAccordion(this)">인사 평가</div>
        <div class="nav-group-content">
          <a href="${pageContext.request.contextPath}/eval/write" class="nav-item">평가 작성·확정</a>
          <c:if test="${sessionScope.userRole == '관리자' || sessionScope.userRole == 'HR담당자'}">
          <a href="${pageContext.request.contextPath}/eval/status" class="nav-item">평가 현황</a>
          </c:if>
        </div>
      </div>
    </div>

    <c:if test="${sessionScope.userRole == '관리자'}">
      <div class="nav-group">
        <div class="nav-group-header" onclick="toggleAccordion(this)">시스템</div>
        <div class="nav-group-content">
          <a href="${pageContext.request.contextPath}/sys/accountUnlock" class="nav-item">계정 잠금 해제</a>
          <a href="${pageContext.request.contextPath}/sys/holiday" class="nav-item">공휴일 관리</a>
          <a href="${pageContext.request.contextPath}/sys/auditLog" class="nav-item">변경 이력 조회</a>
          <a href="${pageContext.request.contextPath}/sys/passwordReset" class="nav-item">비밀번호 초기화</a>
          <a href="${pageContext.request.contextPath}/sys/roleChange" class="nav-item">계정 권한 변경</a>
          <a href="${pageContext.request.contextPath}/sys/sqlQuery" class="nav-item">AI 데이터 조회</a>
        </div>
      </div>
    </c:if>
    
    
  </nav>

</body>

</html>