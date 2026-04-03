<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>HR ERP - 직원 등록</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/emp/detail.css">
    
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="${pageContext.request.contextPath}/js/sidebar.js"></script>
    <style>
        .reg-card {
            background: #fff;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.05);
        }
        .btn-area {
            text-align: center;
            margin-top: 30px;
        }
        .btn-save {
            background-color: #4e73df;
            color: white;
            padding: 10px 30px;
            border: none;
            border-radius: 5px;
            font-weight: bold;
            cursor: pointer;
        }
        .btn-cancel {
            background-color: #858796;
            color: white;
            padding: 10px 30px;
            border: none;
            border-radius: 5px;
            font-weight: bold;
            cursor: pointer;
            margin-right: 10px;
            text-decoration: none;
            display: inline-block;
        }
    </style>
</head>
<body data-context-path="${pageContext.request.contextPath}">

  <jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />

  <div id="main-wrapper">
    <jsp:include page="/WEB-INF/jsp/common/header.jsp" />

    <main class="app-content">
      <div class="detail-header">
        <h1>직원 등록</h1>
      </div>

      <div class="reg-card">
        <form action="${pageContext.request.contextPath}/emp/insert" method="post" id="regForm">
          
          <h3><i class="fas fa-user"></i> 기본 정보</h3>
          <table class="detail-table">
            <tr>
              <th>성명</th>
              <td><input type="text" name="emp_name" required placeholder="이름 입력"></td>
              <th>성별</th>
              <td>
                <select name="gender">
                  <option value="M">남성</option>
                  <option value="F">여성</option>
                </select>
              </td>
            </tr>
            <tr>
              <th>이메일</th>
              <td><input type="email" name="email" required placeholder="example@company.com"></td>
              <th>연락처</th>
              <td><input type="text" name="phone" placeholder="010-0000-0000"></td>
            </tr>
            <tr>
              <th>생년월일</th>
              <td><input type="date" name="birth_date"></td>
              <th>주소</th>
              <td><input type="text" name="address" style="width: 100%;"></td>
            </tr>
          </table>

          <h3 style="margin-top:40px;"><i class="fas fa-briefcase"></i> 인사 정보</h3>
          <table class="detail-table">
            <tr>
              <th>부서</th>
              <td>
                <select name="dept_id">
                  <option value="">부서 선택</option>
                  <c:forEach var="dept" items="${deptList}">
                    <option value="${dept.dept_id}">${dept.dept_name}</option>
                  </c:forEach>
                </select>
              </td>
              <th>직급</th>
              <td>
                <select name="position_id">
                  <option value="">직급 선택</option>
                  <c:forEach var="pos" items="${posList}">
                    <option value="${pos.position_id}">${pos.position_name}</option>
                  </c:forEach>
                </select>
              </td>
            </tr>
            <tr>
              <th>고용 형태</th>
              <td>
                <select name="emp_type">
                  <option value="정규직">정규직</option>
                  <option value="계약직">계약직</option>
                  <option value="인턴">인턴</option>
                </select>
              </td>
              <th>기본급</th>
              <td><input type="text" name="base_salary" placeholder="숫자만 입력"></td>
            </tr>
            <tr>
              <th>입사일</th>
              <td colspan="3"><input type="date" name="hire_date" value="<%= new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()) %>"></td>
            </tr>
          </table>

          <div class="btn-area">
            <a href="${pageContext.request.contextPath}/emp/list" class="btn-cancel">취소</a>
            <button type="submit" class="btn-save">등록하기</button>
          </div>
        </form>
      </div>
    </main>
  </div>

</body>
</html>