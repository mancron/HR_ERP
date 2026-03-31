<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>인사발령</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
<style>
  body { padding: 24px; font-family: sans-serif; font-size: 14px; background: #fff; color: #333; }
  
  /* 헤더 섹션 */
  .header { margin-bottom: 24px; }
  .header h2 { margin: 0 0 8px 0; font-size: 20px; font-weight: 700; }
  .header .emp-info { color: #666; font-size: 14px; }

  /* 카드형 폼 컨테이너 */
  .form-card {
    border: 1px solid #e0e0e0;
    border-radius: 8px;
    padding: 30px;
    background-color: #fff;
    max-width: 700px;
    box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  }

  .form-group { margin-bottom: 20px; }
  .form-row { display: flex; gap: 20px; margin-bottom: 20px; }
  .form-item { flex: 1; }

  label { display: block; margin-bottom: 8px; font-weight: 600; font-size: 13px; }
  label .required { color: #e53935; margin-left: 4px; }

  /* 입력 요소 스타일 */
  select, input[type="text"], input[type="date"], textarea {
    width: 100%;
    padding: 10px 12px;
    border: 1px solid #ced4da;
    border-radius: 6px;
    font-size: 14px;
    box-sizing: border-box;
  }
  
  input[readonly] { background-color: #f8f9fa; color: #6c757d; cursor: default; }
  textarea { resize: none; height: 100px; font-family: inherit; }

  /* 하단 버튼 */
  .btn-area {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    margin-top: 10px;
  }
  .btn {
    padding: 10px 24px;
    border-radius: 6px;
    font-weight: 600;
    cursor: pointer;
    font-size: 14px;
    border: none;
  }
  .btn-cancel { background: #fff; border: 1px solid #ced4da; color: #333; }
  .btn-submit { background: #1976d2; color: #fff; }
</style>
</head>
<body>

  <div class="header">
    <h2>인사발령</h2>
    <div class="emp-info">
      <strong>${empDetail.emp_name}</strong> (${empDetail.emp_no}) · 현재: ${empDetail.dept_name} / ${empDetail.position_name}
    </div>
  </div>

  <div class="form-card">
    <form action="${pageContext.request.contextPath}/emp/transferProcess" method="post">
      <input type="hidden" name="emp_no" value="${empDetail.emp_no}">

      <div class="form-group">
        <label>발령 유형 <span class="required">*</span></label>
        <select name="transfer_type">
          <option value="발령">발령</option>
          <option value="승진">승진</option>
          <option value="전보">전보</option>
        </select>
      </div>

      <div class="form-row">
        <div class="form-item">
          <label>이전 부서</label>
          <input type="text" value="${empDetail.dept_name}" readonly>
        </div>
        <div class="form-item">
          <label>발령 부서</label>
          <select name="target_dept">
            <option value="개발1팀">개발1팀</option>
            <option value="인사팀">인사팀</option>
            <option value="영업팀">영업팀</option>
          </select>
        </div>
      </div>

      <div class="form-row">
        <div class="form-item">
          <label>이전 직급</label>
          <input type="text" value="${empDetail.position_name}" readonly>
        </div>
        <div class="form-item">
          <label>변경 직급</label>
          <select name="target_position">
            <option value="부장">부장</option>
            <option value="차장">차장</option>
            <option value="과장">과장</option>
          </select>
        </div>
      </div>

      <div class="form-group">
        <label>발령 적용일 <span class="required">*</span></label>
        <input type="date" name="transfer_date" value="2025-04-01">
      </div>

      <div class="form-group">
        <label>발령 사유</label>
        <textarea name="reason" placeholder="발령 사유를 입력하세요"></textarea>
      </div>

      <div class="btn-area">
        <button type="button" class="btn btn-cancel" onclick="history.back();">취소</button>
        <button type="submit" class="btn btn-submit">발령 처리</button>
      </div>
    </form>
  </div>

</body>
</html>