<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
	// 브라우저 캐시 방지 설정 (뒤로가기 시 서버에 다시 요청하게 함)
	response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
	response.setHeader("Pragma", "no-cache"); // HTTP 1.0
	response.setDateHeader("Expires", 0); // Proxies

    // 세션에서 정보 가져오기
    String userName = (String) session.getAttribute("userName");
    String userRole = (String) session.getAttribute("userRole");
    String userDept = "emplyee 연동 필요";
    
    if (userName == null) userName = "Guest";
    if (userRole == null) userRole = "USER";
%>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Insert title here</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <header class="app-header" style="display: flex; justify-content: space-between; align-items: center; padding: 0 20px;">
      
      <div style="font-size: 13px; color: var(--gray-500);">
          메인 / <strong style="color:var(--gray-800);">대시보드</strong>
      </div>

      <div style="display: flex; align-items: center; gap: 15px;">
          
          <div class="notification-wrap" style="position: relative; cursor: pointer;">
              <i class="fa-regular fa-bell" style="font-size: 18px; color: var(--gray-600);"></i>
              <%-- 알림 배지 (숫자 3) --%>
              <span style="position: absolute; top: -5px; right: -5px; background: #E74C3C; color: white; font-size: 10px; padding: 2px 5px; border-radius: 10px;">3</span>
          </div>

          <div class="user-profile" style="display: flex; align-items: center; background: #f8f9fa; padding: 5px 15px; border-radius: 30px; border: 1px solid #eee;">
              <%-- 아바타 (첫 글자 따기 또는 기본 이미지) --%>
              <div class="avatar" style="width: 32px; height: 32px; background: #2151A2; color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 14px; margin-right: 10px;">
                  <%= userName.substring(0, 1) %>
              </div>
              
              <%-- 이름 및 소속 정보 --%>
              <div style="text-align: left; line-height: 1.2; margin-right: 10px;">
                  <div style="font-size: 13px; font-weight: bold; color: #333;"><%= userName %></div>
                  <div style="font-size: 11px; color: #888;"><%= userRole %> · <%= userDept %></div>
              </div>
          </div>

          <a href="${pageContext.request.contextPath}/auth/logout" 
   			style="font-size: 12px; padding: 6px 12px; border: 1px solid #d1d8e0; border-radius: 4px; color: #555; text-decoration: none; background: white;">
   			로그아웃
			</a>

      </div>
    </header>
</body>
</html>