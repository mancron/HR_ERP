<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>인사발령</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/emp/transfer.css">
</head>
<body>

  <%-- 헤더 --%>
  <div class="transfer-header">
    <h2 style="display:block;">인사발령</h2>
    <div class="emp-info" style="display:block;">
      <strong>${empDetail.emp_name}</strong> (${empDetail.emp_no})
      &nbsp;·&nbsp; 현재: ${empDetail.dept_name} / ${empDetail.position_name}
    </div>
  </div>

  <form action="${pageContext.request.contextPath}/emp/transferProcess" method="post">
    <input type="hidden" name="emp_no" value="${empDetail.emp_no}">

    <%-- 발령 정보 --%>
    <h3>발령 정보</h3>
    <table class="transfer-table">
      <tr>
        <th>발령 유형 <span class="required">*</span></th>
        <td colspan="3">
          <select name="transfer_type">
            <option value="발령">발령</option>
            <option value="승진">승진</option>
            <option value="전보">전보</option>
          </select>
        </td>
      </tr>
      <tr>
        <th>현재 부서</th>
        <td><input type="text" value="${empDetail.dept_name}" readonly class="readonly-input"></td>
        <th>발령 부서</th>
        <td>
          <select name="target_dept">
            <option value="개발1팀">개발1팀</option>
            <option value="인사팀">인사팀</option>
            <option value="영업팀">영업팀</option>
          </select>
        </td>
      </tr>
      <tr>
        <th>현재 직급</th>
        <td><input type="text" value="${empDetail.position_name}" readonly class="readonly-input"></td>
        <th>변경 직급</th>
        <td>
          <select name="target_position">
            <option value="부장">부장</option>
            <option value="차장">차장</option>
            <option value="과장">과장</option>
          </select>
        </td>
      </tr>
      <tr>
        <th>발령 적용일 <span class="required">*</span></th>
        <td colspan="3">
          <input type="date" name="transfer_date" value="2025-04-01">
        </td>
      </tr>
      <tr>
        <th>발령 사유</th>
        <td colspan="3">
          <textarea name="reason" placeholder="발령 사유를 입력하세요"></textarea>
        </td>
      </tr>
    </table>

    <%-- 버튼 --%>
    <div class="btn-area">
      <button type="button" class="btn-cancel" onclick="history.back();">취소</button>
      <button type="submit" class="btn-submit">발령 처리</button>
    </div>
  </form>

</body>
</html>
