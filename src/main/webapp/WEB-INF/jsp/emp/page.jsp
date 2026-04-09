<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
  page.jsp - 페이지네이션 컴포넌트
  필요한 request attribute:
    - nowPage    : 현재 페이지 (int)
    - totalPage  : 전체 페이지 수 (int)
    - nowBlock   : 현재 블럭 (int)
    - totalBlock : 전체 블럭 수 (int)
    - pagePerBlock : 블럭당 페이지 수 (int)
    - pageStart  : 현재 블럭 시작 페이지 (int)
    - pageEnd    : 현재 블럭 끝 페이지 (int)
  현재 검색 파라미터 (선택값 유지용):
    - param.keyword, param.dept_id, param.position_id, param.status
--%>
<style>
  .pagination {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 4px;
    margin-top: 20px;
    flex-wrap: wrap;
  }
  .pagination a {
    display: inline-block;
    padding: 5px 11px;
    border: 1px solid #ddd;
    border-radius: 4px;
    color: #333;
    text-decoration: none;
    font-size: 13px;
    background: #fff;
  }
  .pagination a:hover { background: #f0f4ff; border-color: #1976d2; color: #1976d2; }
  .pagination a.active {
    background: #1976d2;
    color: #fff;
    border-color: #1976d2;
    font-weight: 700;
    pointer-events: none;
  }
  .pagination a.disabled {
    color: #ccc;
    border-color: #eee;
    pointer-events: none;
    cursor: default;
  }
</style>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">

<%-- 검색 파라미터를 URL에 유지하기 위한 공통 쿼리스트링 --%>
<c:set var="baseQuery"
       value="keyword=${param.keyword}&amp;dept_id=${param.dept_id}&amp;position_id=${param.position_id}&amp;status=${param.status}" />

<div class="pagination">

  <%-- 이전 블럭 --%>
  <c:choose>
    <c:when test="${nowBlock > 1}">
      <c:set var="prevBlockPage" value="${(nowBlock - 2) * pagePerBlock + 1}" />
      <a href="${pageContext.request.contextPath}/emp/list?${baseQuery}&nowPage=${prevBlockPage}">◀</a>
    </c:when>
    <c:otherwise>
      <a class="disabled">◀</a>
    </c:otherwise>
  </c:choose>

  <%-- 페이지 번호 버튼 --%>
  <c:forEach var="p" begin="${pageStart}" end="${pageEnd}">
    <c:choose>
      <c:when test="${p == nowPage}">
        <a class="active">${p}</a>
      </c:when>
      <c:otherwise>
        <a href="${pageContext.request.contextPath}/emp/list?${baseQuery}&nowPage=${p}">${p}</a>
      </c:otherwise>
    </c:choose>
  </c:forEach>

  <%-- 다음 블럭 --%>
  <c:choose>
    <c:when test="${totalBlock > nowBlock}">
      <c:set var="nextBlockPage" value="${nowBlock * pagePerBlock + 1}" />
      <a href="${pageContext.request.contextPath}/emp/list?${baseQuery}&nowPage=${nextBlockPage}">▶</a>
    </c:when>
    <c:otherwise>
      <a class="disabled">▶</a>
    </c:otherwise>
  </c:choose>

</div>
