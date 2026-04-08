<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>퇴직 신청</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/emp/resign.css">
<script>
    function validateForm() {
        const resignDate = document.querySelector('input[name="resign_date"]').value;
        const today      = new Date().toISOString().split('T')[0];

        if (resignDate < today) {
            alert('희망 퇴직일은 오늘 이후여야 합니다.');
            return false;
        }
        return true;
    }
</script>
</head>
<body>

    <%-- 헤더 --%>
    <div class="resign-header">
        <h2 style="display:block;">퇴직 신청</h2>
        <div class="emp-info" style="display:block;">
            <strong>${empDetail.emp_name}</strong> (${empDetail.emp_no})
            &nbsp;·&nbsp; ${empDetail.dept_name} / ${empDetail.position_name}
        </div>
    </div>

    <%-- 주의 안내 --%>
    <div class="warn-box">
	    ⚠ 퇴직 신청 안내<br>
        신청 후 취소를 원하시면 담당 인사팀에 문의하세요.<br>
        최종 승인 이후에는 취소가 불가합니다.
    </div>

    <form action="${pageContext.request.contextPath}/emp/resign" method="post" onsubmit="return validateForm();">
        <%-- 서버에서 처리할 hidden 필드 --%>
        <input type="hidden" name="emp_id" value="${empDetail.emp_id}">
        <input type="hidden" name="emp_no" value="${empDetail.emp_no}">

        <%-- 신청 정보 --%>
        <h3>신청 정보</h3>
        <table class="resign-table">
            <%-- 현재 정보 (읽기 전용) --%>
            <tr>
                <th>현재 상태</th>
                <td><input type="text" value="${empDetail.status}" readonly class="readonly-input"></td>
                <th>현재 부서</th>
                <td><input type="text" value="${empDetail.dept_name}" readonly class="readonly-input"></td>
            </tr>
            <tr>
                <th>직급</th>
                <td><input type="text" value="${empDetail.position_name}" readonly class="readonly-input"></td>
                <th>입사일</th>
                <td><input type="text" value="${empDetail.hire_date}" readonly class="readonly-input"></td>
            </tr>

            <%-- 희망 퇴직일 --%>
            <tr>
                <th>희망 퇴직일 <span class="required">*</span></th>
                <td colspan="3">
                    <input type="date" name="resign_date" value="${tomorrow}" required>
                    <div class="guide-text">실제 퇴직일은 승인 후 인사팀과 협의하여 확정됩니다.</div>
                </td>
            </tr>

            <%-- 퇴직 사유 --%>
            <tr>
                <th>퇴직 사유 <span class="required">*</span></th>
                <td colspan="3">
                    <textarea name="reason" placeholder="퇴직 사유를 입력하세요" required></textarea>
                </td>
            </tr>
        </table>

        <%-- 승인 절차 안내 --%>
        <h3>승인 절차</h3>
        <table class="resign-table">
            <tr>
                <th>1차 결재자</th>
                <td><input type="text" value="부서장 승인" readonly class="readonly-input"></td>
                <th>2차 결재자</th>
                <td><input type="text" value="인사담당자 승인" readonly class="readonly-input"></td>
                <th>최종 결재자</th>
                <td><input type="text" value="최종 결재자 승인" readonly class="readonly-input"></td>
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
            <button type="button" class="btn-cancel" onclick="history.back();">취소</button>
            <button type="submit" class="btn-submit">퇴직 신청</button>
        </div>
    </form>

</body>
</html>
