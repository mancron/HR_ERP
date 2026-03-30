<!-- emp/list.jsp -->
<%@ page contentType="text/html; charset=UTF-8"%>
<%@ page import="com.hrms.emp.dao.EmpDAO, java.util.Vector, com.hrms.emp.dto.EmpDTO" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="deptDao" class="com.hrms.org.dao.DeptDAO" scope="page"/>
<jsp:useBean id="PosDao" class="com.hrms.org.dao.PosDAO" scope="page"/>
<c:set var="deptList" value="${deptDao.deptList()}" />
<c:set var="posList" value="${PosDao.posList()}" />
<%
    /* ── 검색 파라미터 수신 ── */
    String keyword    = request.getParameter("keyword");
    String deptIdStr  = request.getParameter("dept_id");
    String posIdStr   = request.getParameter("position_id");
    String status     = request.getParameter("status");

    /* null 처리 */
    if (keyword   == null) keyword   = "";
    if (status    == null) status    = "all";

    int deptId     = 0;
    int positionId = 0;
    try { if (deptIdStr != null && !deptIdStr.equals("all")) deptId     = Integer.parseInt(deptIdStr); } catch (Exception ignore) {}
    try { if (posIdStr  != null && !posIdStr .equals("all")) positionId = Integer.parseInt(posIdStr);  } catch (Exception ignore) {}

    /* ── 필터 검색 실행 ── */
    EmpDAO empDao = new EmpDAO();
    java.util.Vector<EmpDTO> empList = empDao.searchEmpList(keyword, deptId, positionId, status);
    request.setAttribute("empList",     empList);
    request.setAttribute("keyword",     keyword);
    request.setAttribute("selDeptId",   deptId);
    request.setAttribute("selPosId",    positionId);
    request.setAttribute("selStatus",   status);
%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>HR ERP</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
<style>
  /* 화면 전체를 덮는 반투명 검은 배경 */
  .modal-overlay {
    position: fixed !important; /* 화면에 고정 */
    top: 0; left: 0; width: 100%; height: 100%;
    background-color: rgba(0, 0, 0, 0.5) !important;
    align-items: center; 
    justify-content: center;
    z-index: 9999; /* 화면 최상단으로 끌어올림 */
  }

  /* 가운데 뜨는 하얀색 메인 창 */
  .modal-content {
    background-color: #fff;
    width: 80%; /* 창 너비 */
    max-width: 1200px;
    height: 80vh; /* 창 높이 */
    border-radius: 8px;
    box-shadow: 0 4px 20px rgba(0,0,0,0.3);
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }
</style>
</head>
<body>

    <!-- 사이드바 -->
    <jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />

  <div id="main-wrapper">
    <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
    <main class="app-content">
      <h1 style="font-size: 20px; font-weight: 700;">직원 목록</h1>

      <div class="search-bar">
        <form action="${pageContext.request.contextPath}/emp/list" method="get">

          <!-- ① 이름 / 사번 키워드 검색 -->
          <input
            class="search-input"
            type="text"
            name="keyword"
            placeholder="이름 또는 사번 검색"
            value="${keyword}"
          >

          <!-- 부서 필터 -->
          <select name="dept_id" class="search-select">
            <option value="all" ${selDeptId == 0 ? 'selected' : ''}>전체 부서</option>
            <c:forEach var="dept" items="${deptList}">
              <option value="${dept.dept_id}"
                ${selDeptId == dept.dept_id ? 'selected' : ''}>
                ${dept.dept_name}
              </option>
            </c:forEach>
          </select>

          <!-- 직급 필터 -->
          <select name="position_id" class="search-select">
            <option value="all" ${selPosId == 0 ? 'selected' : ''}>전체 직급</option>
            <c:forEach var="pos" items="${posList}">
              <option value="${pos.position_id}"
                ${selPosId == pos.position_id ? 'selected' : ''}>
                ${pos.position_name}
              </option>
            </c:forEach>
          </select>

          <!-- 재직 상태 필터 -->
          <select name="status" class="search-select">
            <option value="all"    ${selStatus == 'all'    ? 'selected' : ''}>전체</option>
            <option value="work"   ${selStatus == 'work'   ? 'selected' : ''}>재직</option>
            <option value="leave"  ${selStatus == 'leave'  ? 'selected' : ''}>휴직</option>
            <option value="resign" ${selStatus == 'resign' ? 'selected' : ''}>퇴직</option>
          </select>

          <button type="submit">검색</button>
          <button type="button" onClick="">+ 직원 등록</button>
        </form>
      </div>

      <!-- 검색 결과 건수 표시 -->
      <p style="margin: 8px 0; font-size: 14px; color: #555;">
        총 <strong>${empList.size()}</strong>명
      </p>

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
            <c:choose>
              <c:when test="${empty empList}">
                <tr>
                  <td colspan="8" style="text-align:center; padding:20px; color:#888;">
                    검색 결과가 없습니다.
                  </td>
                </tr>
              </c:when>
              <c:otherwise>
                <c:forEach var="emp" items="${empList}">
                  <tr>
                    <td>${emp.emp_no}</td>
                    <td><strong>${emp.emp_name}</strong></td>
                    <td>${emp.dept_name}</td>
                    <td>${emp.position_name}</td>
                    <td>${emp.hire_date}</td>
                    <td>${emp.emp_type}</td>
                    <td>${emp.status}</td>
                    <td><button class="btn-detail" data-empno="${emp.emp_no}">상세</button></td>
                  </tr>
                </c:forEach>
              </c:otherwise>
            </c:choose>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <!-- 페이지 버튼 -->
      </div>
    </main>
  </div>
  
  <!-- 빈 modal -->
<div id="empDetailModal" class="modal-overlay" style="display: none;">
  <div class="modal-content" style="width: 80%; max-width: 1200px; height: 80vh; display: flex; flex-direction: column;">
    <div class="modal-header" style="display: flex; justify-content: space-between; padding: 15px; border-bottom: 1px solid #ddd;">
      <h2 style="margin: 0; font-size: 18px;">직원 상세 정보</h2>
      <button type="button" id="closeModalBtn" style="cursor: pointer;">X</button>
    </div>
    
    <iframe id="modalIframe" src="" style="width: 100%; height: 100%; border: none; flex-grow: 1;"></iframe>
  </div>
</div>

</body>
<script src="${pageContext.request.contextPath}/js/sidebar.js"></script>



<script>
document.addEventListener('DOMContentLoaded', function() {
  const detailButtons = document.querySelectorAll('.btn-detail');
  const modal = document.getElementById('empDetailModal');
  const iframe = document.getElementById('modalIframe'); // iframe 가져오기
  const closeBtn = document.getElementById('closeModalBtn');

  // 1. 상세 버튼 클릭 이벤트
  detailButtons.forEach(button => {
    button.addEventListener('click', function() {
      const empNo = this.getAttribute('data-empno'); 
      
      // 모달 열기
      modal.style.display = 'flex'; // 배경을 덮기 위해 flex나 block 사용
      
      // 2. iframe의 src 속성에 detail.jsp 경로와 파라미터(사번)를 넣어줍니다.
      // 이렇게 하면 브라우저가 알아서 저 안에서 detail.jsp를 렌더링합니다.
      iframe.src = '${pageContext.request.contextPath}/emp/detail?emp_no=' + empNo;
    });
  });

  // 3. 모달 닫기 로직
  closeBtn.addEventListener('click', () => {
    modal.style.display = 'none';
    iframe.src = ''; // 닫을 때 이전 데이터가 안 보이게 초기화
  });
});
</script>
</html>
