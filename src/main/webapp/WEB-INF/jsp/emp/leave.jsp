<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>휴직/복직 신청</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/emp/leave.css">
<script>
    // 휴직/복직 유형에 따라 종료일 필드 표시 여부 제어
    function toggleEndDate() {
        const leaveType = document.getElementById('leaveType').value;
        const endDateRow = document.getElementById('endDateRow');
        // 복직은 종료일 불필요
        endDateRow.style.display = (leaveType === '복직') ? 'none' : '';
    }

    window.onload = function() {
        toggleEndDate();
    };
</script>
</head>
<body>

    <%-- 헤더 --%>
    <div class="leave-header">
        <h2 style="display:block;">휴직·복직 신청</h2>
        <div class="emp-info" style="display:block;">
            <strong>${empDetail.emp_name}</strong> (${empDetail.emp_no})
            &nbsp;·&nbsp; ${empDetail.dept_name} / ${empDetail.position_name}
        </div>
    </div>

    <form action="${pageContext.request.contextPath}/emp/leave" method="post">
        <%-- 서버에서 처리할 hidden 필드 --%>
        <input type="hidden" name="emp_id" value="${empDetail.emp_id}">
        <input type="hidden" name="emp_no" value="${empDetail.emp_no}">
        <input type="hidden" name="mode" value="${mode}">
		<input type="hidden" name="request_id" value="${existing.request_id}">

        <%-- 신청 정보 --%>
        <h3>신청 정보</h3>
        <table class="leave-table">
            <%-- 신청 유형 --%>
            <tr>
                <th>신청 유형 <span class="required">*</span></th>
                <td colspan="3">
                    <select name="leave_type" id="leaveType" onchange="toggleEndDate()">
				    <option value="휴직" <c:if test="${existing.leave_type == '휴직'}">selected</c:if>>휴직</option>
				    <option value="복직" <c:if test="${existing.leave_type == '복직'}">selected</c:if>>복직</option>
					</select>
                </td>
            </tr>

            <%-- 현재 상태 (읽기 전용) --%>
            <tr>
                <th>현재 상태</th>
                <td><input type="text" value="${empDetail.status}" readonly class="readonly-input"></td>
                <th>현재 부서</th>
                <td><input type="text" value="${empDetail.dept_name}" readonly class="readonly-input"></td>
            </tr>

            <%-- 시작일 / 종료일 --%>
            <tr>
                <th>시작일 <span class="required">*</span></th>
                <td><input type="date" name="start_date" value="${not empty existing.start_date ? existing.start_date : tomorrow}" required></td>
                <th>종료일</th>
                <td id="endDateRow">
                    <input type="date" name="end_date" value="${existing.end_date}">
                </td>
            </tr>

            <%-- 신청 사유 --%>
            <tr>
                <th>신청 사유 <span class="required">*</span></th>
                <td colspan="3">
                    <textarea name="reason" required>${existing.reason}</textarea>
                </td>
            </tr>
        </table>

        <%-- 승인 절차 안내 --%>
        <h3>승인 절차</h3>
        <table class="leave-table">
            <tr>
                <th>1차 결재자</th>
                <td><input type="text" value="부서장 승인" readonly class="readonly-input"></td>
                <th>2차 결재자</th>
                <td><input type="text" value="인사담당자 승인" readonly class="readonly-input"></td>
                <th>최종 결재자</th>
                <td><input type="text" value="관리자 승인" readonly class="readonly-input"></td>
            </tr>
            <tr>
                <th>부서장</th>
                <td><input type="text" value="${deptManagerName}" readonly class="readonly-input"></td>
                <th>처리 기간</th>
                <td><input type="text" value="영업일 기준 3~5일" readonly class="readonly-input"></td>
            </tr>
        </table>

        <%-- 버튼 --%>
        <div class="btn-area">
            <c:choose>
		        <c:when test="${mode == 'edit'}">
		            <%-- 수정 모드: 부모창 모달 닫기 --%>
		            <button type="button" class="btn-cancel" onclick="
		                window.parent.document.getElementById('approvalModalIframe').src = '';
		                window.parent.document.getElementById('approvalDetailModal').classList.remove('active');">취소</button>
				 </c:when>
				 <c:otherwise>
		            <%-- 신청 모드: 이전 페이지 --%>
		            <button type="button" class="btn-cancel" onclick="history.back();">취소</button>
				 </c:otherwise>
			</c:choose>
            <button type="submit" class="btn-submit">신청</button>
        </div>
    </form>

</body>
</html>
