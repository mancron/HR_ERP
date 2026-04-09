<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <style>
        body { font-family: 'Pretendard', sans-serif; padding: 20px; color: #334155; line-height: 1.5; }
        .form-group { margin-bottom: 15px; }
        label { display: block; margin-bottom: 5px; font-weight: 600; font-size: 13px; color: #64748b; }
        input { width: 100%; padding: 10px; border: 1px solid #e2e8f0; border-radius: 6px; box-sizing: border-box; font-size: 14px; }
        input:focus { outline: none; border-color: #3b82f6; box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1); }
        .btn-save { width: 100%; padding: 12px; background: #3b82f6; color: white; border: none; border-radius: 6px; cursor: pointer; font-weight: 600; margin-top: 10px; }
        .btn-save:hover { background: #2563eb; }
        .readonly { background-color: #f8fafc; color: #94a3b8; }
    </style>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <form action="${pageContext.request.contextPath}/org/position/edit" method="post">
        <input type="hidden" name="position_id" value="${pos.position_id}">
        
        <div class="form-group">
            <label>직급명</label>
            <input type="text" value="${pos.position_name}" class="readonly" readonly>
        </div>
        <div class="form-group">
            <label>기본급 (원)</label>
            <input type="number" name="base_salary" value="${pos.base_salary}" required>
        </div>
        <div class="form-group">
            <label>식대 (원)</label>
            <input type="number" name="meal_allowance" value="${pos.meal_allowance}" required>
        </div>
        <div class="form-group">
            <label>교통비 (원)</label>
            <input type="number" name="transport_allowance" value="${pos.transport_allowance}" required>
        </div>
        <div class="form-group">
            <label>직책수당 (원)</label>
            <input type="number" name="position_allowance" value="${pos.position_allowance}" required>
        </div>
        <div class="form-group">
            <label>상태</label>
            <select name="is_active" style="width:100%; padding:10px; border:1px solid #e2e8f0; border-radius:6px;">
                <option value="1" ${pos.is_active == 1 ? 'selected' : ''}>활성</option>
                <option value="0" ${pos.is_active == 0 ? 'selected' : ''}>비활성</option>
            </select>
        </div>
        
        <button type="submit" class="btn-save">정보 업데이트</button>
    </form>
</body>
</html>