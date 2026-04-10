<%@ page contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>직원 상세</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/emp/detail.css">
<script>
    const USER_ROLE = '${userRole}';
</script>
<script src="${pageContext.request.contextPath}/js/emp/detail.js"></script>
</head>
<body>

<c:if test="${empty empDetail}">
    <p style="color:red; text-align:center; margin-top:40px;">직원 정보를 불러올 수 없습니다.</p>
</c:if>

<c:if test="${not empty empDetail}">
<form action="${pageContext.request.contextPath}/emp/update"
      method="post" id="empDetailForm">
    <input type="hidden" name="emp_id" value="${empDetail.emp_id}">
    <input type="hidden" name="emp_no"  value="${empDetail.emp_no}">

    <%-- 헤더 --%>
    <div class="detail-header">
        <span class="status-badge status-${empDetail.status}">${empDetail.status}</span>
        <div class="btn-area">
        <c:if test="${userRole == 'HR담당자'}">
            <button type="button" class="btn-transfer"
                onclick="location.href='${pageContext.request.contextPath}/emp/transfer?emp_no=${empDetail.emp_no}'">
                인사발령</button>
        </c:if>
        <c:if test="${loginEmpId == empDetail.emp_id}">
            <button type="button" class="btn-leave"
                onclick="location.href='${pageContext.request.contextPath}/emp/leave?emp_no=${empDetail.emp_no}'">
                휴·복직 신청</button>
            <button type="button" class="btn-resign"
                onclick="location.href='${pageContext.request.contextPath}/emp/resign?emp_no=${empDetail.emp_no}'">
                퇴직 신청</button>
        </c:if>
        </div>
    </div>

    <%-- ===== 기본 정보 ===== --%>
    <h3>기본 정보</h3>
    <table class="detail-table">
        <tr>
            <th>사번</th>
            <td><input type="text" value="${empDetail.emp_no}" readonly class="readonly-input"></td>
            <th>이름</th>
            <%-- 편집 가능 필드: 초기에는 readonly --%>
            <td><input type="text" name="emp_name" value="${empDetail.emp_name}" readonly class="readonly-input"></td>
        </tr>
        <tr>
            <th>성별</th>
            <td><select name="gender" disabled class="readonly-input">
                <option value="M" <c:if test="${empDetail.gender == 'M'}">selected</c:if>>남</option>
                <option value="F" <c:if test="${empDetail.gender == 'F'}">selected</c:if>>여</option>
            </select></td>
            <th>생년월일</th>
            <td><input type="date" name="birth_date" value="${empDetail.birth_date}" readonly class="readonly-input"></td>
        </tr>
        <tr>
            <th>연락처</th>
            <td><input type="text" name="phone" value="${empDetail.phone}" readonly class="readonly-input"></td>
            <th>이메일</th>
            <td><input type="email" name="email" value="${empDetail.email}" readonly class="readonly-input"></td>
        </tr>
        <tr>
            <th>긴급연락처</th>
            <td><input type="text" name="emergency_contact" value="${empDetail.emergency_contact}" readonly class="readonly-input"></td>
            <th>계좌번호</th>
            <td><input type="text" name="bank_account" value="${empDetail.bank_account}" readonly class="readonly-input"></td>
        </tr>
        <tr>
            <th>주소</th>
            <td colspan="3"><input type="text" name="address" value="${empDetail.address}" readonly class="readonly-input"></td>
        </tr>
    </table>

    <%-- ===== 인사 정보 ===== --%>
    <h3>인사 정보</h3>
    <table class="detail-table">
        <tr>
            <th>부서</th>
            <td><input type="text" value="${empDetail.dept_name}" readonly class="readonly-input"></td>
            <th>직급</th>
            <td><input type="text" value="${empDetail.position_name}" readonly class="readonly-input"></td>
        </tr>
        <tr>
            <th>고용형태</th>
            <%-- 최종승인자/HR담당자만 수정 모드에서 활성화됨 --%>
            <td><select name="emp_type" disabled class="readonly-input">
                <option value="정규직"  <c:if test="${empDetail.emp_type == '정규직'}">selected</c:if>>정규직</option>
                <option value="계약직"  <c:if test="${empDetail.emp_type == '계약직'}">selected</c:if>>계약직</option>
                <option value="파트타임" <c:if test="${empDetail.emp_type == '파트타임'}">selected</c:if>>파트타임</option>
            </select></td>
            <th>기본급</th>
            <td><input type="text" name="base_salary" value="${empDetail.base_salary}" readonly class="readonly-input"
                       title="기본급 변경은 인사평가 연동으로 처리됩니다"></td>
        </tr>
        <tr>
            <th>입사일</th>
            <td><input type="date" name="hire_date" value="${empDetail.hire_date}" readonly class="readonly-input"></td>
            <th>퇴사일</th>
            <td>
                <c:choose>
                    <c:when test="${not empty empDetail.resign_date}">
                        <input type="date" value="${empDetail.resign_date}" readonly class="readonly-input">
                    </c:when>
                    <c:otherwise>
                        <input type="text" value="재직 중" readonly class="readonly-input">
                    </c:otherwise>
                </c:choose>
            </td>
        </tr>
        <tr>
            <th>등록일시</th>
            <td colspan="3"><input type="text" value="${empDetail.created_at}" readonly class="readonly-input"></td>
        </tr>
    </table>
    <br>

    <%-- ===== 버튼 영역 ===== --%>
    <div class="btn-area">
        <%-- data-mode="view" → 수정 버튼, data-mode="edit" → 저장 버튼으로 전환 --%>
        <button type="button" id="btnEditSave" class="btn-edit" data-mode="view" onclick="toggleEditSave()">&nbsp;&nbsp;수정&nbsp;&nbsp;</button>
    </div>

</form>
</c:if>

</body>
</html>
