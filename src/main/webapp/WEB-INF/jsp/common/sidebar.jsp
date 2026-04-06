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
  <%-- 권한 및 부서장 여부 식별용 변수 세팅 --%>
  <c:set var="role" value="${sessionScope.userRole}" />
  <c:set var="isSysAdmin" value="${role == '관리자'}" />
  <c:set var="isHrAdmin" value="${role == 'HR담당자'}" />
  <c:set var="isCeo" value="${role == '최종승인자'}" />
  <%-- 세션에 부서장 여부(isManager)가 boolean으로 들어있다고 가정 --%>
  <c:set var="isManager" value="${sessionScope.isManager != null ? sessionScope.isManager : false}" />

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

    <c:if test="${isHrAdmin}">
      <div class="nav-group">
        <div class="nav-group-header" onclick="toggleAccordion(this)">조직 관리</div>
        <div class="nav-group-content">
          <a href="${pageContext.request.contextPath}/org/dept" class="nav-item">부서 관리</a>
          <a href="${pageContext.request.contextPath}/org/position" class="nav-item">직급 관리</a>
        </div>
      </div>
    </c:if>

    <div class="nav-group">
      <div class="nav-group-header" onclick="toggleAccordion(this)">직원 관리</div>
      <div class="nav-group-content">
        <a href="${pageContext.request.contextPath}/emp/list" class="nav-item">직원 목록</a>
        <a href="${pageContext.request.contextPath}/emp/history" class="nav-item">인사발령 이력</a>
        <a href="${pageContext.request.contextPath}/emp/approvalHistory" class="nav-item">휴직·복직·퇴직 내역</a>
        
        <%-- 직원 등록은 HR 전용 --%>
        <c:if test="${isHrAdmin}">
          <a href="${pageContext.request.contextPath}/emp/reg" class="nav-item">직원 등록</a>
        </c:if>
        
        <%-- 휴/복/퇴 승인은 HR, 최고경영자, 부서장만 접근 가능 --%>
        <c:if test="${isHrAdmin || isCeo || isManager}">
          <a href="${pageContext.request.contextPath}/emp/approval" class="nav-item">휴직·복직·퇴직 승인</a>
        </c:if>
      </div>
    </div>

    <div class="nav-group">
      <div class="nav-group-header" onclick="toggleAccordion(this)">근태 관리</div>
      <div class="nav-group-content">
        <a href="${pageContext.request.contextPath}/att/record" class="nav-item">출퇴근</a>
        <a href="${pageContext.request.contextPath}/att/leave/req" class="nav-item">휴가 신청</a>
        <a href="${pageContext.request.contextPath}/att/overtime" class="nav-item">초과근무</a>
        <a href="${pageContext.request.contextPath}/att/annual" class="nav-item">연차 현황</a>
        
        <%-- 휴가 승인은 HR담당자와 부서장 권한을 가진 사람(관리자 포함)만 --%>
        <c:if test="${isHrAdmin || isManager}">
          <a href="${pageContext.request.contextPath}/att/leave/approve" class="nav-item">휴가 승인</a>
        </c:if>
        
        <%-- 전사 근태 보정 및 연차 일괄 부여는 HR 전용 --%>
        <c:if test="${isHrAdmin}">
          <a href="${pageContext.request.contextPath}/att/status" class="nav-item">근태 현황·보정</a>
          <a href="${pageContext.request.contextPath}/att/annual/grant" class="nav-item">연차 일괄 부여</a>
        </c:if>
      </div>
    </div>

    <div class="nav-group">
      <div class="nav-group-header" onclick="toggleAccordion(this)">급여 관리</div>
      <div class="nav-group-content">
        <a href="${pageContext.request.contextPath}/sal/slip" class="nav-item">급여 명세서</a>
        
        <%-- 급여 계산 및 전사 현황은 HR 전용 --%>
        <c:if test="${isHrAdmin}">
          <a href="${pageContext.request.contextPath}/sal/calc" class="nav-item">급여 계산·지급</a>
          <a href="${pageContext.request.contextPath}/sal/status" class="nav-item">급여 현황</a>
          <a href="${pageContext.request.contextPath}/sal/deduction" class="nav-item">공제율 관리</a>
        </c:if>
      </div>
    </div>

    <div class="nav-group">
      <div class="nav-group-header" onclick="toggleAccordion(this)">인사 평가</div>
      <div class="nav-group-content">
        <a href="${pageContext.request.contextPath}/eval/status" class="nav-item">평가 현황·확정</a>
        
        <%-- 평가 작성은 최종승인자(사장) 제외 (사장은 실무 평가를 직접 작성하지 않음) --%>
        <c:if test="${!isCeo}">
          <a href="${pageContext.request.contextPath}/eval/write" class="nav-item">평가 작성</a>
        </c:if>
      </div>
    </div>

<%-- 시스템 관리: 관리자 전용 --%>
<c:if test="${isSysAdmin}">
    <div class="nav-group">
        <div class="nav-group-header" onclick="toggleAccordion(this)">시스템</div>
        <div class="nav-group-content">
            <a href="${pageContext.request.contextPath}/sys/accountUnlock"  class="nav-item">계정 잠금 해제</a>
            <a href="${pageContext.request.contextPath}/sys/holiday"        class="nav-item">공휴일 관리</a>
            <a href="${pageContext.request.contextPath}/sys/auditLog"       class="nav-item">변경 이력 조회</a>
            <a href="${pageContext.request.contextPath}/sys/passwordReset"  class="nav-item">비밀번호 초기화</a>
            <a href="${pageContext.request.contextPath}/sys/roleChange"     class="nav-item">계정 권한 변경</a>
        </div>
    </div>
</c:if>

<%-- AI 데이터 조회: 관리자 + HR담당자 + 최종승인자 --%>
<c:if test="${isSysAdmin || isHrAdmin || isCeo}">
    <div class="nav-group">
        <div class="nav-group-header" onclick="toggleAccordion(this)">데이터 조회</div>
        <div class="nav-group-content">
            <a href="${pageContext.request.contextPath}/sys/sqlQuery" class="nav-item">AI 데이터 조회</a>
        </div>
    </div>
</c:if>
    
  </nav>

</body>

</html>