<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<header class="app-header" style="display: flex; justify-content: space-between; align-items: center; padding: 10px 20px; border-bottom: 1px solid #eee; background: #fff;">
  
  <div style="font-size: 13px; color: #64748b;">
      메인 / <strong style="color:#1e293b;">대시보드</strong>
  </div>

  <div style="display: flex; align-items: center; gap: 15px;">
      
      <div class="notification-wrap" style="position: relative; cursor: pointer;">
          <i class="fa-regular fa-bell" style="font-size: 18px; color: #64748b;"></i>
          <span style="position: absolute; top: -5px; right: -5px; background: #E74C3C; color: white; font-size: 10px; padding: 2px 5px; border-radius: 10px;">3</span>
      </div>

      <div class="user-profile" style="display: flex; align-items: center; background: #f8f9fa; padding: 5px 15px; border-radius: 30px; border: 1px solid #eee;">
          <%-- 이름의 첫 글자 추출 (fn:substring 사용) --%>
          <div class="avatar" style="width: 32px; height: 32px; background: #2151A2; color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 14px; margin-right: 10px; font-weight: bold;">
              ${fn:substring(not empty loginUser.emp_name ? loginUser.emp_name : 'G', 0, 1)}
          </div>
          
          <div style="text-align: left; line-height: 1.2; margin-right: 10px;">
              <div style="font-size: 13px; font-weight: bold; color: #333;">
                  ${not empty loginUser.emp_name ? loginUser.emp_name : 'Guest'}
              </div>
              <div style="font-size: 11px; color: #888;">
                  <%-- 부서명 · 직급(없으면 권한) 출력 --%>
                  ${not empty loginUser.dept_name ? loginUser.dept_name : '소속 미지정'} · 
                  <c:choose>
                      <c:when test="${not empty loginUser.position_name}">
                          ${loginUser.position_name}
                      </c:when>
                      <c:otherwise>
                          ${not empty userRole ? userRole : 'USER'}
                      </c:otherwise>
                  </c:choose>
              </div>
          </div>
      </div>

      <a href="${pageContext.request.contextPath}/auth/logout" 
         style="font-size: 12px; padding: 6px 12px; border: 1px solid #d1d8e0; border-radius: 4px; color: #555; text-decoration: none; background: white; font-weight: 500;">
         로그아웃
      </a>
  </div>
</header>