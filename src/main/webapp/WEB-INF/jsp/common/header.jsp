<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:if test="${empty sessionScope.userRole || empty sessionScope.loginUser}">
    <c:redirect url="/auth/login" />
</c:if>

<header class="app-header" style="display: flex; justify-content: space-between; align-items: center; padding: 12px 24px; border-bottom: 1px solid #edf2f7; background: #fff; font-family: 'Pretendard', sans-serif; box-shadow: 0 1px 3px rgba(0,0,0,0.02);">
  
  <div style="font-size: 13px; color: #64748b;">
      메인 / <strong style="color:#1e293b;">대시보드</strong>
  </div>

  <div style="display: flex; align-items: center; gap: 18px;">
      
      <%-- 세션 타이머 영역 --%>
      <div id="session-timer-wrap" style="display: flex; align-items: center; gap: 8px; background: #f1f5f9; padding: 6px 14px; border-radius: 20px; border: 1px solid #e2e8f0; transition: all 0.3s ease;">
          <i id="session-icon" class="fa-regular fa-clock" style="font-size: 13px; color: #64748b;"></i>
          <span id="session-timer" style="font-size: 13px; font-weight: 600; color: #475569; min-width: 40px; font-variant-numeric: tabular-nums;">--:--</span>
          <div style="width: 1px; height: 12px; background: #cbd5e1; margin: 0 2px;"></div>
          <button type="button" id="extend-btn" onclick="extendSession()" style="background: none; border: none; color: #2151A2; font-size: 11px; font-weight: 700; cursor: pointer; padding: 0; transition: all 0.2s;">
              연장
          </button>
      </div>
      
      <%-- 알림 영역 --%>
      <div class="notification-wrap" style="position: relative; cursor: pointer;">
          <i class="fa-regular fa-bell" style="font-size: 19px; color: #64748b;"></i>
          <span style="position: absolute; top: -5px; right: -5px; background: #E74C3C; color: white; font-size: 10px; padding: 2px 5px; border-radius: 10px; line-height: 1; font-weight: bold; border: 2px solid #fff;">3</span>
      </div>

      <%-- 사용자 프로필 영역 --%>
      <div class="user-profile" style="display: flex; align-items: center; background: #ffffff; padding: 8px 18px; border-radius: 40px; border: 1px solid #e2e8f0; box-shadow: 0 2px 4px rgba(0,0,0,0.03); transition: all 0.2s ease;">

          <%-- 아바타: Guest 등의 기본값 제거 --%>
          <div class="avatar" style="width: 34px; height: 34px; background: #2151A2; color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 15px; margin-right: 12px; font-weight: bold; box-shadow: inset 0 0 5px rgba(0,0,0,0.1);">
              ${fn:substring(loginUser.emp_name, 0, 1)}
          </div>
          
          <div style="text-align: left; line-height: 1.3; margin-right: 5px;">
              <div style="font-size: 14px; font-weight: 700; color: #1e293b;">
                  ${loginUser.emp_name}
              </div>
              <div style="font-size: 11px; color: #94a3b8; font-weight: 500;">
                  ${loginUser.dept_name} · 
                  <c:choose>
                      <c:when test="${not empty loginUser.position_name}">
                          ${loginUser.position_name}
                      </c:when>
                      <c:otherwise>
                          ${userRole}
                      </c:otherwise>
                  </c:choose>
              </div>
          </div>
      </div>

      <%-- 로그아웃 버튼 --%>
      <a href="${pageContext.request.contextPath}/auth/logout" 
         style="font-size: 12px; padding: 7px 14px; border: 1px solid #e2e8f0; border-radius: 6px; color: #475569; text-decoration: none; background: white; font-weight: 600; transition: all 0.2s;"
         onmouseover="this.style.background='#f1f5f9'; this.style.color='#1e293b';" onmouseout="this.style.background='white'; this.style.color='#475569';">
         로그아웃
      </a>
  </div>
</header>

<script>
    let initialTime = parseInt("${pageContext.session.maxInactiveInterval}") || 1800;
    let timeLeft = initialTime;
    
    const timerElement = document.getElementById('session-timer');
    const timerWrap = document.getElementById('session-timer-wrap');
    const extendBtn = document.getElementById('extend-btn');
    const sessionIcon = document.getElementById('session-icon');

    function updateTimerDisplay() {
        let min = Math.floor(timeLeft / 60);
        let sec = timeLeft % 60;
        
        timerElement.innerText = (min < 10 ? "0" + min : min) + ":" + (sec < 10 ? "0" + sec : sec);

        if (timeLeft < 300) {
            timerElement.style.color = "#E74C3C";
            timerWrap.style.borderColor = "#FADBD8";
            timerWrap.style.background = "#FDEDEC";
        } else {
            timerElement.style.color = "#475569";
            timerWrap.style.borderColor = "#e2e8f0";
            timerWrap.style.background = "#f1f5f9";
        }

        if (timeLeft <= 0) {
            clearInterval(sessionInterval);
            location.href = "${pageContext.request.contextPath}/auth/login?timeout=y";
            return;
        }
        timeLeft--;
    }

    let sessionInterval = setInterval(updateTimerDisplay, 1000);

    function extendSession() {
        // 부서 관리나 공통 액션의 ping 체크 활용
        fetch("${pageContext.request.contextPath}/org/dept?action=ping")
            .then(response => {
                // 필터에서 401을 던지면 세션 만료로 간주
                if(response.status === 401) {
                    location.href = "${pageContext.request.contextPath}/auth/login?timeout=y";
                    return;
                }
                timeLeft = initialTime;
                extendBtn.style.color = "#2ECC71";
                sessionIcon.style.transform = "scale(1.3)";
                sessionIcon.style.color = "#2ECC71";
                
                setTimeout(() => {
                    extendBtn.style.color = "#2151A2";
                    sessionIcon.style.transform = "scale(1)";
                    sessionIcon.style.color = "#64748b";
                    updateTimerDisplay();
                }, 600);
            })
            .catch(error => {
                console.error("Session Extension Error:", error);
            });
    }
</script>