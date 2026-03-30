<!-- detail.jsp -->
<%@ page contentType="text/html; charset=UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>HR ERP</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
<style>
	.readonly-input {
	    background-color: #f1f3f5; /* 아주 연한 회색 배경 */
	    border: 1px solid !important; /* 테두리도 눈에 덜 띄게 */
	    outline: none;             /* 클릭 시 생기는 테두리 하이라이트 방지 */
	}
</style>
</head>
<%
    // 브라우저 캐시 방지 설정
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); 
    response.setHeader("Pragma", "no-cache"); 
    response.setDateHeader("Expires", 0); 
    
    // 세션이 없으면 부모 창을 로그인 페이지로 이동
    if (session == null || session.getAttribute("userName") == null) {
%>
        <script>
            alert("세션이 만료되었거나 로그인이 필요합니다. 로그인 화면으로 이동합니다.");
            // iframe을 감싸고 있는 진짜 부모 창의 주소를 변경
            window.parent.location.href = "http://localhost/auth/login";
        </script>
<%
        return; // 자바스크립트만 출력하고 아래 HTML 코드는 렌더링하지 않고 종료
    }
%>
<body>
  <form action="${pageContext.request.contextPath}/emp/update" method="post" id="empDetailForm">
    
    <div>
        <h2>직원 상세 및 수정</h2>
        <span>재직중</span>
    </div>

    <h3>기본 정보</h3>
    <table class="detail-table">
      <tr>
        <th>사번</th>
        <td><input type="text" name="emp_no" value="${param.emp_no}" readonly class="readonly-input"></td>
        <th>이름</th>
        <td><input type="text" name="emp_name" value="홍길동"></td>
      </tr>
      <tr>
        <th>연락처</th>
        <td><input type="text" name="phone" value="010-1234-5678"></td>
        <th>이메일</th>
        <td><input type="email" name="email" value="hong@company.com"></td>
      </tr>
    </table>

    <h3>인사 정보</h3>
    <table class="detail-table">
      <tr>
        <th>부서</th>
        <td><input type="text" value="개발1팀" readonly class="readonly-input"></td>
        <th>직급</th>
        <td><input type="text" value="과장" readonly class="readonly-input"></td>
      </tr>
      <tr>
        <th>입사일</th>
        <td><input type="date" name="hire_date" value="2018-03-02"></td>
        <th>고용형태</th>
        <td>
            <select name="emp_type">
                <option value="정규직" selected>정규직</option>
                <option value="계약직">계약직</option>
            </select>
        </td>
      </tr>
    </table>

    <div>
        <button type="submit" class="btn-save" >정보 수정(저장)</button>
        
        <button type="button" onclick="location.href='${pageContext.request.contextPath}/emp/transfer?emp_no=${param.emp_no}'">인사발령</button>
        <button type="button" onclick="location.href='${pageContext.request.contextPath}/emp/leave?emp_no=${param.emp_no}'" >휴/복직</button>
        <button type="button" onclick="location.href='${pageContext.request.contextPath}/emp/resign?emp_no=${param.emp_no}'" >퇴직처리</button>
    </div>
  </form>

</body>
</html>