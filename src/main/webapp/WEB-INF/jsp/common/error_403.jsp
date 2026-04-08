<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>접근 권한 없음</title>
    <style>
        body { display:flex; justify-content:center; align-items:center;
               height:100vh; margin:0; font-family:sans-serif; background:#f5f5f5; }
        .box { text-align:center; padding:48px; background:#fff;
               border-radius:12px; box-shadow:0 2px 16px rgba(0,0,0,.1); }
        h1   { font-size:48px; color:#e74c3c; margin:0 0 8px; }
        p    { color:#555; margin:8px 0; }
        a    { display:inline-block; margin-top:24px; padding:10px 28px;
               background:#3498db; color:#fff; border-radius:6px;
               text-decoration:none; font-weight:bold; }
        a:hover { background:#217dbb; }
    </style>
</head>
<body>
    <div class="box">
        <h1>403</h1>
        <p><strong>권한이 없는 페이지입니다.</strong></p>
        <p>해당 기능에 접근할 수 있는 권한이 없습니다.</p>
        <a href="${pageContext.request.contextPath}/main">대시보드로 돌아가기</a>
    </div>
</body>
</html>