<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>신청 상세</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/emp/approvalDetail.css">
<script src="${pageContext.request.contextPath}/js/approval.js"></script>
</head>
<body>

<%-- type: leave or resign --%>

<c:if test="${empty requestDetail}">
    <p style="color:red; text-align:center; margin-top:40px;">신청 정보를 불러올 수 없습니다.</p>
</c:if>

<c:if test="${not empty requestDetail}">

    <%-- 헤더 --%>
    <div class="detail-header">
        <h2 style="display:block;">
            <c:choose>
                <c:when test="${requestType == 'leave'}">휴직·복직 신청 상세</c:when>
                <c:otherwise>퇴직 신청 상세</c:otherwise>
            </c:choose>
        </h2>
        <div class="emp-info" style="display:block;">
            <strong>${requestDetail.emp_name}</strong> (${requestDetail.emp_no})
            &nbsp;·&nbsp; ${requestDetail.dept_name} / ${requestDetail.position_name}
        </div>
    </div>

    <%-- ===== 신청 정보 ===== --%>
    <h3>신청 정보</h3>
    <table class="detail-table">
        <c:choose>
            <%-- 휴직/복직 --%>
            <c:when test="${requestType == 'leave'}">
                <tr>
                    <th>신청 유형</th>
                    <td>${requestDetail.leave_type}</td>
                    <th>상태</th>
                    <td><span class="status-badge status-${requestDetail.status}">${requestDetail.status}</span></td>
                </tr>
                <tr>
                    <th>시작일</th>
                    <td>${requestDetail.start_date}</td>
                    <th>종료일</th>
                    <td>
                        <c:choose>
                            <c:when test="${empty requestDetail.end_date}">-</c:when>
                            <c:otherwise>${requestDetail.end_date}</c:otherwise>
                        </c:choose>
                    </td>
                </tr>
            </c:when>
            <%-- 퇴직 --%>
            <c:otherwise>
                <tr>
                    <th>희망 퇴직일</th>
                    <td>${requestDetail.resign_date}</td>
                    <th>상태</th>
                    <td><span class="status-badge status-${requestDetail.status}">${requestDetail.status}</span></td>
                </tr>
            </c:otherwise>
        </c:choose>
        <tr>
            <th>신청 사유</th>
            <td colspan="3">${requestDetail.reason}</td>
        </tr>
        <tr>
            <th>신청일시</th>
            <td colspan="3">${requestDetail.created_at.toString().replace('T', ' ')}</td>
        </tr>
        <c:if test="${requestDetail.status == '반려' && not empty requestDetail.reject_reason}">
        <tr>
            <th>반려 사유</th>
            <td colspan="3" class="reject-reason">${requestDetail.reject_reason}</td>
        </tr>
        </c:if>
    </table>

    <%-- ===== 승인 현황 ===== --%>
    <h3>승인 현황</h3>
    <table class="detail-table">
        <tr>
            <th>부서장</th>
            <td>
                <c:choose>
                    <c:when test="${empty requestDetail.dept_manager_name}">미지정</c:when>
                    <c:otherwise>${requestDetail.dept_manager_name}</c:otherwise>
                </c:choose>
            </td>
            <th>부서장 승인일시</th>
            <td>
                <c:choose>
                    <c:when test="${empty requestDetail.dept_approved_at}">
                        <span class="pending-text">대기 중</span>
                    </c:when>
                    <c:otherwise>${requestDetail.dept_approved_at.toString().replace('T', ' ')}</c:otherwise>
                </c:choose>
            </td>
        </tr>
        <tr>
            <th>인사담당자</th>
            <td>
                <c:choose>
                    <c:when test="${empty requestDetail.hr_manager_name}">미지정</c:when>
                    <c:otherwise>${requestDetail.hr_manager_name}</c:otherwise>
                </c:choose>
            </td>
            <th>인사담당자 승인일시</th>
            <td>
                <c:choose>
                    <c:when test="${empty requestDetail.hr_approved_at}">
                        <span class="pending-text">대기 중</span>
                    </c:when>
                    <c:otherwise>${requestDetail.hr_approved_at.toString().replace('T', ' ')}</c:otherwise>
                </c:choose>
            </td>
        </tr>
   		<tr>
            <th>최종승인자</th>
            <td>
                <c:choose>
                    <c:when test="${empty requestDetail.president_name}">미지정</c:when>
					<c:otherwise>${requestDetail.president_name}</c:otherwise>
                </c:choose>
            </td>
            <th>최종승인일시</th>
            <td>
                <c:choose>
                    <c:when test="${empty requestDetail.president_approved_at}">
                        <span class="pending-text">대기 중</span>
                    </c:when>
                    <c:otherwise>${requestDetail.president_approved_at.toString().replace('T', ' ')}</c:otherwise>
                </c:choose>
            </td>
        </tr>
    </table>

    <%-- ===== 버튼 영역 ===== --%>
    <div class="btn-area">

        <%-- 부서장: 대기 상태일 때만 승인/반려 --%>
        <c:if test="${isDeptManager && requestDetail.status == '대기'}">
        <button type="button" class="btn-approve-action"
                onclick="location.href='${pageContext.request.contextPath}/emp/approvalAction?type=${requestType}&id=${requestDetail.request_id}&action=approve'">
                승인
            </button>
            <button type="button" class="btn-reject-action"
			    onclick="rejectWithReason('${pageContext.request.contextPath}/emp/approvalAction?type=${requestType}&id=${requestDetail.request_id}&action=reject')">
			    반려
			</button>
        </c:if>

        <%-- HR담당자/최종승인자: 부서장승인 상태일 때만 승인/반려 --%>
        <c:if test="${isHrManager && requestDetail.status == '부서장승인' && !requestDetail.hrDept}">
            <button type="button" class="btn-approve-action"
                onclick="location.href='${pageContext.request.contextPath}/emp/approvalAction?type=${requestType}&id=${requestDetail.request_id}&action=approve'">
                승인
            </button>
            <button type="button" class="btn-reject-action"
			    onclick="rejectWithReason('${pageContext.request.contextPath}/emp/approvalAction?type=${requestType}&id=${requestDetail.request_id}&action=reject')">
			    반려
			</button>
        </c:if>
        
        <%-- 일반부서: 최종승인자 --%>
		<c:if test="${isPresident && requestDetail.status == 'HR담당자승인' && !requestDetail.hrDept}">
		    <button type="button" class="btn-approve-action"
                onclick="location.href='${pageContext.request.contextPath}/emp/approvalAction?type=${requestType}&id=${requestDetail.request_id}&action=approve'">
                승인
            </button>
		    <button type="button" class="btn-reject-action"
			    onclick="rejectWithReason('${pageContext.request.contextPath}/emp/approvalAction?type=${requestType}&id=${requestDetail.request_id}&action=reject')">
			    반려
			</button>
		</c:if>
		
		<%-- 인사팀: 최종승인자 --%>
		<c:if test="${isPresident && requestDetail.status == '부서장승인' && requestDetail.hrDept}">
		    <button type="button" class="btn-approve-action"
                onclick="location.href='${pageContext.request.contextPath}/emp/approvalAction?type=${requestType}&id=${requestDetail.request_id}&action=approve'">
                승인
            </button>
		    <button type="button" class="btn-reject-action"
			    onclick="rejectWithReason('${pageContext.request.contextPath}/emp/approvalAction?type=${requestType}&id=${requestDetail.request_id}&action=reject')">
			    반려
			</button>
		</c:if>
    </div>

</c:if>
<script>
    function rejectWithReason(url) {
        const reason = prompt('반려 사유를 입력하세요.');
        if (reason === null) return;
        if (reason.trim() === '') {
            alert('반려 사유를 입력해주세요.');
            return;
        }
        location.href = url + '&rejectReason=' + encodeURIComponent(reason);
    }
</script>
</body>
</html>
