<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <title>직원 상세</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/emp/detail.css">
  
</head>
<body>
<%-- 데이터가 없으면 오류 메시지 표시 --%>
<c:if test="${empty empDetail}">
  <p style="color:red; text-align:center; margin-top: 40px;">직원 정보를 불러올 수 없습니다.</p>
</c:if>

<c:if test="${not empty empDetail}">
<form action="${pageContext.request.contextPath}/emp/update" method="post" id="empDetailForm">
  <%-- 수정 시 식별용 hidden 필드 --%>
  <input type="hidden" name="emp_id" value="${empDetail.emp_id}">
  <input type="hidden" name="emp_no" value="${empDetail.emp_no}">

  <%-- ===== 헤더 ===== --%>
  <div class="detail-header">
    <h2>직원 상세 및 수정</h2>
    <span class="status-badge status-${empDetail.status}">${empDetail.status}</span>
  </div>

  <%-- ===== 기본 정보 ===== --%>
  <h3>기본 정보</h3>
  <table class="detail-table">
    <tr>
      <th>사번</th>
      <td><input type="text" value="${empDetail.emp_no}" readonly class="readonly-input"></td>
      <th>이름</th>
      <td><input type="text" name="emp_name" value="${empDetail.emp_name}"></td>
    </tr>
    <tr>
      <th>성별</th>
      <td>
        <select name="gender">
          <option value="M" <c:if test="${empDetail.gender == 'M'}">selected</c:if>>남</option>
          <option value="F" <c:if test="${empDetail.gender == 'F'}">selected</c:if>>여</option>
        </select>
      </td>
      <th>생년월일</th>
      <td><input type="date" name="birth_date" value="${empDetail.birth_date}"></td>
    </tr>
    <tr>
      <th>연락처</th>
      <td><input type="text" name="phone" value="${empDetail.phone}"></td>
      <th>이메일</th>
      <td><input type="email" name="email" value="${empDetail.email}"></td>
    </tr>
    <tr>
      <th>주소</th>
      <td colspan="3"><input type="text" name="address" value="${empDetail.address}"></td>
    </tr>
    <tr>
      <th>긴급연락처</th>
      <td><input type="text" name="emergency_contact" value="${empDetail.emergency_contact}"></td>
      <th>계좌번호</th>
      <%-- 계좌번호는 보안상 마스킹 표시 (수정 가능하도록 실제값 전달) --%>
      <td><input type="text" name="bank_account" value="${empDetail.bank_account}"></td>
    </tr>
  </table>

  <%-- ===== 인사 정보 ===== --%>
  <h3>인사 정보</h3>
  <table class="detail-table">
    <tr>
      <th>부서</th>
      <td><input type="text" value="${empDetail.dept_name}" readonly class="readonly-input"></td>
      <th>직급</th>
      <td><input type="text" value="${empDetail.position_name}" readonly class="readonly-input"></td>
    </tr>
    <tr>
      <th>입사일</th>
      <td><input type="date" name="hire_date" value="${empDetail.hire_date}"></td>
      <th>퇴사일</th>
      <td>
        <c:choose>
          <c:when test="${not empty empDetail.resign_date}">
            <input type="date" value="${empDetail.resign_date}" readonly class="readonly-input">
          </c:when>
          <c:otherwise>
            <input type="text" value="재직 중" readonly class="readonly-input">
          </c:otherwise>
        </c:choose>
      </td>
    </tr>
    <tr>
      <th>고용형태</th>
      <td>
        <select name="emp_type">
          <option value="정규직"  <c:if test="${empDetail.emp_type == '정규직'}">selected</c:if>>정규직</option>
          <option value="계약직"  <c:if test="${empDetail.emp_type == '계약직'}">selected</c:if>>계약직</option>
          <option value="파트타임" <c:if test="${empDetail.emp_type == '파트타임'}">selected</c:if>>파트타임</option>
        </select>
      </td>
      <th>기본급</th>
      <td><input type="text" value="${empDetail.base_salary}" readonly class="readonly-input"
                 title="기본급 변경은 인사평가 연동으로 처리됩니다"></td>
    </tr>
    <tr>
      <th>등록일시</th>
      <td colspan="3"><input type="text" value="${empDetail.created_at}" readonly class="readonly-input"></td>
    </tr>
  </table>

  <%-- ===== 버튼 영역 ===== --%>
  <div class="btn-area">
    <button type="submit" class="btn-save">저장</button>
    <button type="button" class="btn-transfer"
        onclick="location.href='${pageContext.request.contextPath}/emp/transfer?emp_no=${empDetail.emp_no}'">
	   인사발령
	</button>
    <button type="button" class="btn-leave"
            onclick="location.href='${pageContext.request.contextPath}/emp/leave?emp_no=${emp.emp_no}'">
      휴/복직
    </button>
    <button type="button" class="btn-resign"
            onclick="location.href='${pageContext.request.contextPath}/emp/resign?emp_no=${emp.emp_no}'">
      퇴직 처리
    </button>
  </div>
</form>
</c:if>

</body>
</html>
