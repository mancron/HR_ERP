<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<c:set var="ctx"        value="${pageContext.request.contextPath}" />
<c:set var="role"       value="${sessionScope.userRole}" />
<c:set var="isMgr"      value="${sessionScope.isManager}" />

<%-- 역할 편의 변수 --%>
<c:set var="isHR"       value="${role == 'HR담당자'}" />
<c:set var="isCEO"      value="${role == '최종승인자'}" />
<c:set var="isSysAdmin" value="${role == '관리자'}" />

<nav id="sidebar">
  <div class="nav-logo">ERP-HRMS</div>

  <%-- ===================== 공통·인증 (전체 공개) ===================== --%>
  <div class="nav-group open">
    <div class="nav-group-header" onclick="toggleAccordion(this)">공통·인증</div>
    <div class="nav-group-content">
      <a href="${ctx}/main"           class="nav-item">메인 대시보드</a>
      <a href="${ctx}/auth/pw-change" class="nav-item">비밀번호 변경</a>
      <a href="${ctx}/notification"   class="nav-item">알림</a>
    </div>
  </div>

  <%-- ===================== 조직 관리 (전체 공개, 수정은 HR만) ===================== --%>
  <div class="nav-group">
    <div class="nav-group-header" onclick="toggleAccordion(this)">조직 관리</div>
    <div class="nav-group-content">
      <a href="${ctx}/org/dept"     class="nav-item">부서 관리</a>
      <c:if test="${isHR or isCEO}">
      	<a href="${ctx}/org/position" class="nav-item">직급 관리</a>
      </c:if>
    </div>
  </div>

  <%-- ===================== 직원 관리 ===================== --%>
  <div class="nav-group">
    <div class="nav-group-header" onclick="toggleAccordion(this)">직원 관리</div>
    <div class="nav-group-content">

      <%-- 직원 목록: 전체 공개 --%>
      <a href="${ctx}/emp/list" class="nav-item">직원 목록</a>

      <%-- 직원 등록: HR담당자만 --%>
      <c:if test="${isHR}">
        <a href="${ctx}/emp/reg" class="nav-item">직원 등록</a>
      </c:if>

      <%-- 휴·퇴직 승인: 전체 공개 --%>
      <a href="${ctx}/emp/approval" class="nav-item">휴·퇴직 승인 관리</a>

      <%-- 휴·퇴직 완료 내역: 전체 공개 --%>
      <a href="${ctx}/emp/approvalHistory" class="nav-item">휴·퇴직 완료 내역</a>

      <%-- 인사발령 이력: 전체 공개 --%>
      <a href="${ctx}/emp/history" class="nav-item">인사발령 이력</a>

    </div>
  </div>

  <%-- ===================== 근태 관리 ===================== --%>
  <div class="nav-group">
    <div class="nav-group-header" onclick="toggleAccordion(this)">근태 관리</div>
    <div class="nav-group-content">

      <%-- 출퇴근: 전체 공개 --%>
      <a href="${ctx}/att/record" class="nav-item">출퇴근</a>

      <%-- 휴가·초과근무 신청 / 연차 현황: CEO 제외 --%>
      <c:if test="${not isCEO}">
        <a href="${ctx}/att/leave/req"    class="nav-item">휴가 신청</a>
        <a href="${ctx}/att/overtime/req" class="nav-item">초과근무 신청</a>
      </c:if>
      
      
      

      <%-- 휴가 승인: HR담당자 / 부서장 / CEO(조회) --%>
      <c:if test="${isHR or isCEO or isMgr}">
        <a href="${ctx}/att/leave/approve" class="nav-item">휴가 승인</a>
      </c:if>

      <%-- 초과근무 승인: HR담당자 / 부서장 --%>
      <c:if test="${isHR or isMgr}">
        <a href="${ctx}/att/overtime/approve" class="nav-item">초과근무 승인</a>
      </c:if>

      <%-- 근태 현황·보정: HR담당자 / CEO --%>
      <c:if test="${isHR or isCEO}">
        <a href="${ctx}/att/status" class="nav-item">근태 현황·보정</a>
      </c:if>

      <%-- 연차 일괄 부여: HR담당자만 --%>
      <c:if test="${isHR}">
      	<a href="${ctx}/att/annual"       class="nav-item">연차 현황</a>
        <a href="${ctx}/att/annual/grant" class="nav-item">연차 일괄 부여</a>
      </c:if>

    </div>
  </div>

  <%-- ===================== 급여 관리 ===================== --%>
  <div class="nav-group">
    <div class="nav-group-header" onclick="toggleAccordion(this)">급여 관리</div>
    <div class="nav-group-content">

      <%-- 급여 명세서: 전체 공개 --%>
      <a href="${ctx}/sal/slip" class="nav-item">급여 명세서</a>

      <%-- 급여 계산·지급: HR담당자만 --%>
      <c:if test="${isHR}">
        <a href="${ctx}/sal/calc" class="nav-item">급여 계산·지급</a>
      </c:if>

      <%-- 급여 현황: HR담당자 / CEO --%>
      <c:if test="${isHR or isCEO}">
        <a href="${ctx}/sal/status" class="nav-item">급여 현황</a>
      </c:if>

      <%-- 공제율 관리: HR담당자만 --%>
      <c:if test="${isHR}">
        <a href="${ctx}/sal/deduction" class="nav-item">공제율 관리</a>
      </c:if>

    </div>
  </div>

  <%-- ===================== 인사 평가 (전체 공개, CEO는 조회만) ===================== --%>
  <div class="nav-group">
    <div class="nav-group-header" onclick="toggleAccordion(this)">인사 평가</div>
    <div class="nav-group-content">
      <a href="${ctx}/eval/write"  class="nav-item">평가 작성</a>
      <a href="${ctx}/eval/status" class="nav-item">평가 현황·확정</a>
    </div>
  </div>

  <%-- ===================== 시스템: 관리자(시스템 관리자)만 ===================== --%>
  <c:if test="${isSysAdmin}">
    <div class="nav-group">
      <div class="nav-group-header" onclick="toggleAccordion(this)">시스템</div>
      <div class="nav-group-content">
        <a href="${ctx}/sys/accountUnlock" class="nav-item">계정 잠금 해제</a>
        <a href="${ctx}/sys/holiday"       class="nav-item">공휴일 관리</a>
        <a href="${ctx}/sys/auditLog"      class="nav-item">변경 이력 조회</a>
        <a href="${ctx}/sys/passwordReset" class="nav-item">비밀번호 초기화</a>
        <a href="${ctx}/sys/roleChange"    class="nav-item">계정 권한 변경</a>
      </div>
    </div>
  </c:if>

  <%-- ===================== 데이터 조회: HR담당자 / 관리자 / CEO ===================== --%>
  <c:if test="${isHR or isCEO}">
    <div class="nav-group">
      <div class="nav-group-header" onclick="toggleAccordion(this)">데이터 조회</div>
      <div class="nav-group-content">
        <a href="${ctx}/sys/sqlQuery" class="nav-item">AI 데이터 조회</a>
      </div>
    </div>
  </c:if>

</nav>
