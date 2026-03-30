<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.hrms.emp.dto.EmployeeDTO" %>
<%
	// 브라우저 캐시 방지
	response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
	response.setHeader("Pragma", "no-cache");
	response.setDateHeader("Expires", 0);

    // 1. 세션에서 정보 가져오기
    String userRole = (String) session.getAttribute("userRole");
    // LoginServlet에서 세션에 담아준 EmployeeDTO 객체를 꺼냄
    EmployeeDTO loginUser = (EmployeeDTO) session.getAttribute("loginUser");
    
    // 2. 표시할 데이터 결정 (loginUser가 있으면 실명 사용, 없으면 계정명 사용)
    String displayRealName = (loginUser != null) ? loginUser.getEmpName() : (String)session.getAttribute("userName");
    String displayDept = (loginUser != null && loginUser.getDeptName() != null) ? loginUser.getDeptName() : "소속 미지정";
    
    // 기본값 처리
    if (displayRealName == null) displayRealName = "Guest";
    if (userRole == null) userRole = "USER";
%>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
<%-- 아이콘 사용을 위한 폰트어썸 CDN (필요시 추가) --%>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">
</head>
<body>
    <header class="app-header" style="display: flex; justify-content: space-between; align-items: center; padding: 10px 20px; border-bottom: 1px solid #eee; background: #fff;">
      
      <%-- 왼쪽: 현재 위치 표시 --%>
      <div style="font-size: 13px; color: #64748b;">
          메인 / <strong style="color:#1e293b;">대시보드</strong>
      </div>

      <%-- 오른쪽: 알림 및 사용자 프로필 --%>
      <div style="display: flex; align-items: center; gap: 15px;">
          
          <%-- 알림 아이콘 --%>
          <div class="notification-wrap" style="position: relative; cursor: pointer;">
              <i class="fa-regular fa-bell" style="font-size: 18px; color: #64748b;"></i>
              <span style="position: absolute; top: -5px; right: -5px; background: #E74C3C; color: white; font-size: 10px; padding: 2px 5px; border-radius: 10px;">3</span>
          </div>

          <%-- 사용자 프로필 영역 --%>
          <div class="user-profile" style="display: flex; align-items: center; background: #f8f9fa; padding: 5px 15px; border-radius: 30px; border: 1px solid #eee;">
              <%-- 아바타 (실제 이름의 첫 글자) --%>
              <div class="avatar" style="width: 32px; height: 32px; background: #2151A2; color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 14px; margin-right: 10px; font-weight: bold;">
                  <%= displayRealName.substring(0, 1) %>
              </div>
              
              <%-- 실제 이름 및 부서/권한 정보 --%>
              <div style="text-align: left; line-height: 1.2; margin-right: 10px;">
                  <%-- ID가 아닌 '홍길동' 같은 실명이 나옵니다 --%>
                  <div style="font-size: 13px; font-weight: bold; color: #333;"><%= displayRealName %></div>
                  <div style="font-size: 11px; color: #888;"><%= displayDept %> · <%= userRole %></div>
              </div>
          </div>

          <%-- 로그아웃 버튼 --%>
          <a href="${pageContext.request.contextPath}/auth/logout" 
   			style="font-size: 12px; padding: 6px 12px; border: 1px solid #d1d8e0; border-radius: 4px; color: #555; text-decoration: none; background: white; font-weight: 500;">
   			로그아웃
		  </a>

      </div>
    </header>
</body>
</html>