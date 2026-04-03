<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>인사발령</title>
<link rel="stylesheet"
	href="${pageContext.request.contextPath}/css/emp/transfer.css">
</head>
<body>

	<%-- 헤더 --%>
	<div class="transfer-header">
		<h2 style="display: block;">인사발령</h2>
		<div class="emp-info" style="display: block;">
			<strong>${empDetail.emp_name}</strong> (${empDetail.emp_no})
			&nbsp;·&nbsp; 현재: ${empDetail.dept_name} / ${empDetail.position_name}
		</div>
	</div>

	<form action="${pageContext.request.contextPath}/emp/transfer"
		method="post">
		<input type="hidden" name="emp_no" value="${empDetail.emp_no}">
		<input type="hidden" name="emp_id" value="${empDetail.emp_id}">
		<input type="hidden" name="prev_dept_id" value="${empDetail.dept_id}">
		<input type="hidden" name="prev_position_id" value="${empDetail.position_id}">

		<%-- 발령 정보 --%>
		<h3>발령 정보</h3>
		<table class="transfer-table">
			<tr>
				<th>발령 유형 <span class="required">*</span></th>
				<td colspan="3"><select name="transfer_type">
						<option value="발령">발령</option>
						<option value="승진">승진</option>
						<option value="전보">전보</option>
				</select></td>
			</tr>
			<tr>
				<th>현재 부서</th>
				<td><input type="text" value="${empDetail.dept_name}" readonly
					class="readonly-input"></td>
				<th>발령 부서</th>
				<td><select name="target_dept">
						<c:forEach var="dept" items="${deptList}">
							<option value="${dept.dept_id}"
								<c:if test="${dept.dept_id == empDetail.dept_id}">selected</c:if>>
								${dept.dept_name}</option>
						</c:forEach>
				</select></td>
			</tr>
			<tr>
				<th>현재 직급</th>
				<td><input type="text" value="${empDetail.position_name}"
					readonly class="readonly-input"></td>
				<th>변경 직급</th>
				<td><select name="target_position">
						<c:forEach var="pos" items="${positionList}">
							<option value="${pos.position_id}"
								<c:if test="${pos.position_id == empDetail.position_id}">selected</c:if>>
								${pos.position_name}</option>
						</c:forEach>
				</select></td>
			</tr>
			<tr>
			    <th>현재 직책</th>
			    <td>
			        <input type="text"
			               value="${isCurrentManager ? '부서장' : '일반'}"
			               readonly class="readonly-input">
			    </td>
			    <th>발령 직책 <span class="required">*</span></th>
			    <td>
			        <select name="target_role">
			            <option value="일반">일반</option>
			            <option value="부서장">부서장</option>
			        </select>
			    </td>
			</tr>
			<tr>
				<th>발령 적용일 <span class="required">*</span></th>
				<td colspan="3"><input type="date" name="transfer_date" value="${tomorrow}"></td>
			</tr>
			<tr>
				<th>발령 사유</th>
				<td colspan="3"><textarea name="reason"
						placeholder="발령 사유를 입력하세요"></textarea></td>
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
