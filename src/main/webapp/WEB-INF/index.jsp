<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>HR ERP - 메인 대시보드</title>
<style>
  @import url('https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@300;400;500;600;700&display=swap');

  :root {
    --primary: #1a3a6b; --primary-light: #2355a0; --accent: #3b7dd8;
    --gray-50: #f8fafc; --gray-100: #f1f5f9; --gray-200: #e2e8f0; --gray-500: #64748b; --gray-800: #1e293b;
    --sidebar-w: 260px; --header-h: 56px;
  }

  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { font-family: 'Noto Sans KR', sans-serif; background: var(--gray-50); color: var(--gray-800); font-size: 13px; display: flex; }
  a { text-decoration: none; color: inherit; }

  /* ── 사이드바 아코디언 스타일 ── */
  #sidebar {
    position: fixed; left: 0; top: 0; width: var(--sidebar-w); height: 100vh;
    background: var(--primary); overflow-y: auto; z-index: 100;
  }
  .nav-logo { padding: 18px 16px; font-size: 16px; font-weight: 700; color: white; border-bottom: 1px solid rgba(255,255,255,.12); }
  
  .nav-group { border-bottom: 1px solid rgba(255,255,255,.05); }
  .nav-group-header {
    padding: 14px 16px; color: rgba(255,255,255,.85); font-size: 13px; font-weight: 600;
    cursor: pointer; display: flex; justify-content: space-between; align-items: center;
    transition: background 0.2s, color 0.2s;
  }
  .nav-group-header:hover { background: rgba(255,255,255,.08); color: white; }
  .nav-group-header::after { content: '▼'; font-size: 10px; transition: transform 0.3s ease; opacity: 0.6; }
  
  .nav-group.open .nav-group-header { background: rgba(0,0,0,.1); color: white; }
  .nav-group.open .nav-group-header::after { transform: rotate(-180deg); opacity: 1; }
  
  .nav-group-content { 
    max-height: 0; overflow: hidden; transition: max-height 0.3s ease; background: rgba(0,0,0,.15);
  }
  .nav-item { 
    display: block; padding: 10px 16px 10px 32px; color: rgba(255,255,255,.65); 
    font-size: 12px; transition: all .2s; border-left: 3px solid transparent; 
  }
  .nav-item:hover { color: white; background: rgba(255,255,255,.05); }
  .nav-item.active { color: white; border-left-color: var(--accent); background: rgba(255,255,255,.1); font-weight: 500; }

  /* ── 메인 컨테이너 & 헤더 ── */
  #main-wrapper { margin-left: var(--sidebar-w); width: calc(100% - var(--sidebar-w)); min-height: 100vh; display: flex; flex-direction: column; }
  .app-header { height: var(--header-h); background: #fff; border-bottom: 1px solid var(--gray-200); display: flex; align-items: center; padding: 0 24px; justify-content: space-between; }
  .app-content { padding: 24px; flex: 1; }
</style>
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

  <div id="main-wrapper">
    <header class="app-header">
      <div style="font-size: 13px; color: var(--gray-500);">메인 / <strong style="color:var(--gray-800);">대시보드</strong></div>
      <div style="font-size: 13px;">홍길동 부장 <a href="/auth/logout" style="color: var(--accent); margin-left: 10px;">로그아웃</a></div>
    </header>
    <main class="app-content">
      <h1 style="font-size: 20px; font-weight: 700;">메인 대시보드 영역</h1>
      <p style="margin-top: 10px; color: var(--gray-500);">위젯 및 현황판 데이터 바인딩 위치</p>
    </main>
  </div>

  <script>
    // 아코디언 동작 스크립트
    function toggleAccordion(headerElement) {
      const group = headerElement.parentElement;
      const content = group.querySelector('.nav-group-content');
      
      // Toggle current
      if (group.classList.contains('open')) {
        group.classList.remove('open');
        content.style.maxHeight = null;
      } else {
        group.classList.add('open');
        content.style.maxHeight = content.scrollHeight + "px";
      }
    }

    // 페이지 로드 시 'open' 클래스가 있는 그룹의 max-height 초기화
    document.addEventListener("DOMContentLoaded", () => {
      document.querySelectorAll('.nav-group.open .nav-group-content').forEach(content => {
        content.style.maxHeight = content.scrollHeight + "px";
      });
    });
  </script>
</body>
</html>