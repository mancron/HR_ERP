<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>HR ERP - 직원 목록</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/empList.css">
</head>
<body>

  <jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />

  <div id="main-wrapper">
    <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
    <main class="app-content">

      <h1>직원 목록</h1><br>

      <%-- ===== 검색 폼 ===== --%>
      <form action="${pageContext.request.contextPath}/emp/list" method="get">
        <div class="search-bar" >

          <input type="text" name="keyword"
                 value="${param.keyword}"
                 placeholder="이름 또는 사번 검색"
                >

          <%-- 부서 드롭다운: selDeptId(String)와 dept_id(String)를 비교해 selected 유지 --%>
          <select name="dept_id">
            <option value="all" <c:if test="${selDeptId == 'all'}">selected</c:if>>전체 부서</option>
            <c:forEach var="dept" items="${deptList}">
              <option value="${dept.dept_id}"
                      <c:if test="${selDeptId == String.valueOf(dept.dept_id)}">selected</c:if>>
                ${dept.dept_name}
              </option>
            </c:forEach>
          </select>

          <%-- 직급 드롭다운 --%>
          <select name="position_id" >
            <option value="all" <c:if test="${selPosId == 'all'}">selected</c:if>>전체 직급</option>
            <c:forEach var="pos" items="${posList}">
              <option value="${pos.position_id}"
                      <c:if test="${selPosId == String.valueOf(pos.position_id)}">selected</c:if>>
                ${pos.position_name}
              </option>
            </c:forEach>
          </select>

          <%-- 재직 상태 드롭다운 --%>
          <select name="status" >
            
            <option value="재직"  <c:if test="${selStatus == '재직'}">selected</c:if>>재직</option>
            <option value="휴직"  <c:if test="${selStatus == '휴직'}">selected</c:if>>휴직</option>
            <option value="퇴직"  <c:if test="${selStatus == '퇴직'}">selected</c:if>>퇴직</option>
            <option value="all"  <c:if test="${selStatus == 'all'}">selected</c:if>>전체 상태</option>
          </select>

          <button type="submit" >검색</button>
          <a href="${pageContext.request.contextPath}/emp/reg" class="btn-register">+ 직원 등록</a>
        </div>
      </form>

      <%-- 검색 결과 건수 --%>
      <p>
        총 <strong>${totalCount}</strong>명
      </p>

      <%-- ===== 직원 테이블 ===== --%>
      <div class="card" >
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
                  <td colspan="8">
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
                    <td>
                      <button class="btn-detail" data-empno="${emp.emp_no}">상세</button>
                    </td>
                  </tr>
                </c:forEach>
              </c:otherwise>
            </c:choose>
          </tbody>
        </table>
      </div>

      <div class="pagination">
  		<jsp:include page="/WEB-INF/jsp/emp/page.jsp" />
	  </div>

    </main>
  </div>

  <%-- ===== 직원 상세 모달 ===== --%>
  <div id="empDetailModal" class="modal-overlay">
    <div class="modal-content">
      <div class="modal-header">
        <h2>직원 상세 정보</h2>
        <button type="button" id="closeModalBtn" style="cursor:pointer;">✕</button>
      </div>
      <iframe id="modalIframe" src=""></iframe>
    </div>
  </div>

  <script src="${pageContext.request.contextPath}/js/sidebar.js"></script>
  <script>
    document.addEventListener('DOMContentLoaded', function () {
      const modal     = document.getElementById('empDetailModal');
      const iframe    = document.getElementById('modalIframe');
      const closeBtn  = document.getElementById('closeModalBtn');
      const baseUrl   = '${pageContext.request.contextPath}/emp/detail?emp_no=';

      // 상세 버튼 클릭 → 모달 열기
      document.querySelectorAll('.btn-detail').forEach(function (btn) {
        btn.addEventListener('click', function () {
          const empNo = this.getAttribute('data-empno');
          iframe.src = baseUrl + empNo;
          modal.classList.add('active');
        });
      });

      // 모달 닫기
      closeBtn.addEventListener('click', function () {
        modal.classList.remove('active');
        iframe.src = '';
      });

    });
  </script>
</body>
</html>
