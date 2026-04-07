<%@ page contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>HR ERP - 직원 등록</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/emp/reg.css">
<script src="${pageContext.request.contextPath}/js/sidebar.js"></script>
</head>
<body data-context-path="${pageContext.request.contextPath}">

<jsp:include page="/WEB-INF/jsp/common/sidebar.jsp" />

<div id="main-wrapper">
    <jsp:include page="/WEB-INF/jsp/common/header.jsp" />
    <main class="app-content">

        <h1>직원 등록</h1><br>

        <form action="${pageContext.request.contextPath}/emp/reg" method="post" id="regForm">
            <div class="card">
                <table class="reg-table">

					<%-- 사번 --%>
                    <tr>
					    <th>사번</th>
					    <td colspan="3">
					        <input type="text" class="emp-no-preview" value="${nextEmpNo}" readonly>
					    </td>
					</tr>
					
                    <%-- 로그인 ID --%>
                    <tr>
                        <th>로그인 ID <span class="required">*</span></th>
                        <td colspan="3">
                            <div class="input-with-btn">
                                <input type="text" id="username" name="username"
                                       placeholder="로그인 ID 입력" required>
                                <button type="button" class="btn-check"
                                        onclick="checkUsername()">중복확인</button>
                            </div>
                            <span id="usernameMsg" class="msg"></span>
                        </td>
                    </tr>

                    <%-- 직원명 --%>
                    <tr>
                        <th>직원명 <span class="required">*</span></th>
                        <td colspan="3">
                            <input type="text" name="emp_name" placeholder="직원명 입력" required>
                        </td>
                    </tr>
                    
                    

                    <%-- 소속부서 / 직급 --%>
                    <tr>
                        <th>소속부서 <span class="required">*</span></th>
                        <td>
                            <select name="dept_id" required>
                                <option value="">부서 선택</option>
                                <c:forEach var="dept" items="${deptList}">
                                    <option value="${dept.dept_id}">${dept.dept_name}</option>
                                </c:forEach>
                            </select>
                        </td>
                        <th>직급 <span class="required">*</span></th>
                        <td>
                            <select name="position_id" id="positionSelect"
                                    onchange="setBaseSalary(this)" required>
                                <option value="">직급 선택</option>
                                <c:forEach var="pos" items="${positionList}">
                                    <option value="${pos.position_id}"
                                            data-salary="${pos.base_salary}">
                                        ${pos.position_name}
                                    </option>
                                </c:forEach>
                            </select>
                        </td>
                    </tr>

                    <%-- 입사일 / 고용형태 --%>
                    <tr>
                        <th>입사일 <span class="required">*</span></th>
                        <td>
                            <input type="date" name="hire_date" required>
                        </td>
                        <th>고용형태 <span class="required">*</span></th>
                        <td>
                            <label><input type="radio" name="emp_type" value="정규직" checked> 정규직</label>
                            <label><input type="radio" name="emp_type" value="계약직"> 계약직</label>
                            <label><input type="radio" name="emp_type" value="파트타임"> 파트타임</label>
                        </td>
                    </tr>

                    <%-- 개인 기본급 --%>
                    <tr>
                        <th>개인 기본급 <span class="required">*</span></th>
                        <td colspan="3">
                            <input type="number" name="base_salary" id="baseSalary"
                                   placeholder="직급 선택 시 자동 세팅" min="0" required>
                            <span class="hint">직급 선택 시 자동 세팅되며 수동으로 수정 가능합니다.</span>
                        </td>
                    </tr>

                    <%-- 생년월일 / 성별 --%>
                    <tr>
                        <th>생년월일</th>
                        <td>
                            <input type="date" name="birth_date">
                        </td>
                        <th>성별</th>
                        <td>
                            <label><input type="radio" name="gender" value="M"> 남</label>
                            <label><input type="radio" name="gender" value="F"> 여</label>
                        </td>
                    </tr>

                    <%-- 연락처 / 긴급연락처 --%>
                    <tr>
                        <th>연락처</th>
                        <td>
                            <input type="text" name="phone" placeholder="010-0000-0000">
                        </td>
                        <th>긴급연락처</th>
                        <td>
                            <input type="text" name="emergency_contact" placeholder="010-0000-0000">
                        </td>
                    </tr>

                    <%-- 회사 이메일 --%>
                    <tr>
					    <th>회사 이메일</th>
					    <td colspan="3">
					        <div class="input-with-btn">
					            <input type="text" id="emailPrefix" name="email_prefix"
					                   placeholder="이메일 앞부분 입력">
					            <span class="email-domain">@company.com</span>
					            <button type="button" class="btn-check"
					                    onclick="checkEmail()">중복확인</button>
					        </div>
					        <span id="emailMsg" class="msg"></span>
					        <%-- 실제 전송용 hidden 필드 --%>
					        <input type="hidden" id="email" name="email">
					    </td>
					</tr>

                    <%-- 급여 이체 계좌번호 --%>
                    <tr>
                        <th>급여 이체 계좌번호</th>
                        <td colspan="3">
                            <input type="text" name="bank_account" placeholder="000-000-000000"
                                   style="width: 300px;">
                        </td>
                    </tr>

                    <%-- 주소 --%>
                    <tr>
					    <th>주소</th>
					    <td colspan="3" class="full-width">
					        <input type="text" name="address" placeholder="주소 입력">
					    </td>
					</tr>

                </table>
            </div>

            <%-- 버튼 --%>
            <div class="btn-area">
                <button type="button" class="btn-cancel"
                        onclick="location.href='${pageContext.request.contextPath}/emp/list'">
                    취소
                </button>
                <button type="submit" class="btn-submit" onclick="return validateForm()">
                    등록
                </button>
            </div>

        </form>

    </main>
</div>

<script>
    const contextPath = document.body.dataset.contextPath || '';
    let usernameChecked = false;

    // ── username 중복 확인 ──
    function checkUsername() {
        const username = document.getElementById('username').value.trim();
        const msg = document.getElementById('usernameMsg');

        if (!username) {
            msg.textContent = '아이디를 입력해주세요.';
            msg.className = 'msg error';
            return;
        }

        fetch(contextPath + '/emp/reg?action=checkUsername&username=' + encodeURIComponent(username))
            .then(res => res.json())
            .then(data => {
                if (data.exists) {
                    msg.textContent = '이미 사용 중인 아이디입니다.';
                    msg.className = 'msg error';
                    usernameChecked = false;
                } else {
                    msg.textContent = '사용 가능한 아이디입니다.';
                    msg.className = 'msg success';
                    usernameChecked = true;
                }
            })
            .catch(() => {
                msg.textContent = '중복 확인 중 오류가 발생했습니다.';
                msg.className = 'msg error';
            });
    }

    // ── 직급 선택 시 기본급 자동 세팅 ──
    function setBaseSalary(select) {
        const salary = select.options[select.selectedIndex].dataset.salary;
        document.getElementById('baseSalary').value = salary || '';
    }

    // ── 폼 제출 전 유효성 검사 ──
    function validateForm() {
	    if (!usernameChecked) {
	        alert('아이디 중복 확인을 해주세요.');
	        return false;
	    }
	    if (!emailChecked) {
	        alert('이메일 중복 확인을 해주세요.');
	        return false;
	    }
	    return true;
	}

    // ── username 입력 변경 시 중복확인 초기화 ──
    document.getElementById('username').addEventListener('input', function () {
        usernameChecked = false;
        const msg = document.getElementById('usernameMsg');
        msg.textContent = '';
        msg.className = 'msg';
    });
</script>

<script src="${pageContext.request.contextPath}/js/emp/reg.js"></script>
</body>
</html>