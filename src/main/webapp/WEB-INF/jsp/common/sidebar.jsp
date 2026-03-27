<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Insert title here</title>
</head>
<body>

  <nav id="sidebar">
    <div class="nav-logo">🏢 HR ERP</div>

    <div class="nav-group open">
      <div class="nav-group-header" onclick="toggleAccordion(this)">공통·인증</div>
      <div class="nav-group-content">
        <a href="/main" class="nav-item active">메인 대시보드</a>
        <a href="/auth/pw-change" class="nav-item">비밀번호 변경</a>
      </div>
    </div>

    <div class="nav-group">
      <div class="nav-group-header" onclick="toggleAccordion(this)">조직 관리</div>
      <div class="nav-group-content">
        <a href="/org/dept" class="nav-item">부서 관리</a>
        <a href="/org/position" class="nav-item">직급 관리</a>
      </div>
    </div>

    <div class="nav-group">
      <div class="nav-group-header" onclick="toggleAccordion(this)">직원 관리</div>
      <div class="nav-group-content">
        <a href="/emp/list" class="nav-item">직원 목록</a>
        <a href="/emp/reg" class="nav-item">직원 등록</a>
        <a href="/emp/history" class="nav-item">인사발령 이력</a>
      </div>
    </div>

    <div class="nav-group">
      <div class="nav-group-header" onclick="toggleAccordion(this)">근태 관리</div>
      <div class="nav-group-content">
        <a href="/att/record" class="nav-item">출퇴근</a>
        <a href="/att/leave/req" class="nav-item">휴가 신청</a>
        <a href="/att/leave/approve" class="nav-item">휴가 승인</a>
        <a href="/att/overtime" class="nav-item">초과근무</a>
        <a href="/att/status" class="nav-item">근태 현황·보정</a>
        <a href="/att/annual" class="nav-item">연차 현황</a>
        <a href="/att/annual/grant" class="nav-item">연차 일괄 부여</a>
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
        <a href="/eval/write" class="nav-item">평가 작성·확정</a>
        <a href="/eval/status" class="nav-item">평가 현황</a>
      </div>
    </div>

    <div class="nav-group">
      <div class="nav-group-header" onclick="toggleAccordion(this)">시스템</div>
      <div class="nav-group-content">
        <a href="/sys/unlock" class="nav-item">계정 잠금 해제</a>
        <a href="/sys/holiday" class="nav-item">공휴일 관리</a>
        <a href="/sys/audit" class="nav-item">변경 이력 조회</a>
        <a href="/sys/pw-reset" class="nav-item">비밀번호 초기화</a>
        <a href="/sys/role" class="nav-item">계정 권한 변경</a>
      </div>
    </div>
  </nav>
  
</body>
</html>