<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<link rel="stylesheet" href="${pageContext.request.contextPath}/css/eval/evaluation.css">

<div class="eval-wrapper">
    <%-- 왼쪽: 메인 평가 영역 --%>
    <div class="eval-main">
        <div class="section-title">
            <c:choose>
                <c:when test="${not empty evalData}">
                    ${evalData.empName} 평가 수정
                </c:when>
                <c:otherwise>평가 작성</c:otherwise>
            </c:choose>
        </div>

        <form action="${pageContext.request.contextPath}/eval/write" method="post" id="evalForm">
            <c:if test="${not empty evalData}">
                <input type="hidden" name="evalId" value="${evalData.evalId}">
            </c:if>

            <div class="form-grid">
                <div class="form-group">
                    <label>평가 대상자 *</label>
                    <select name="empId" required>
                        <option value="">대상자를 선택하세요</option>
                        <c:forEach var="emp" items="${targetList}">
                            <option value="${emp.empId}"
                                ${not empty evalData && evalData.empId == emp.empId ? 'selected' : ''}>
                                ${emp.empName} (${emp.pos})
                            </option>
                        </c:forEach>
                    </select>
                </div>

                <div class="form-group">
                    <label>평가 연도 *</label>
                    <select name="evalYear">
                        <c:forEach var="y" items="${yearList}">
                            <option value="${y}"
                                ${not empty evalData && evalData.evalYear == y ? 'selected' : ''}>${y}년</option>
                        </c:forEach>
                    </select>
                </div>

                <div class="form-group">
                    <label>평가 기간 *</label>
                    <select name="evalPeriod">
                        <option value="상반기" ${not empty evalData && evalData.evalPeriod == '상반기' ? 'selected' : ''}>상반기</option>
                        <option value="하반기" ${not empty evalData && evalData.evalPeriod == '하반기' ? 'selected' : ''}>하반기</option>
                        <option value="연간"   ${not empty evalData && evalData.evalPeriod == '연간'   ? 'selected' : ''}>연간</option>
                    </select>
                </div>

                <div class="form-group">
                    <label>평가 유형 *</label>
                    <select name="evalType">
                        <option value="자기평가" ${not empty evalData && evalData.evalType == '자기평가' ? 'selected' : ''}>자기평가</option>
                        <option value="상위평가" ${empty evalData || evalData.evalType == '상위평가' ? 'selected' : ''}>상위평가</option>
                        <option value="동료평가" ${not empty evalData && evalData.evalType == '동료평가' ? 'selected' : ''}>동료평가</option>
                    </select>
                </div>
            </div>

            <div style="font-weight: 700; margin-bottom: 20px;">📊 항목별 점수 (각 100점 만점)</div>

            <c:forEach var="itemName" items="${itemNames}" varStatus="loop">
                <div class="score-item">
                    <div class="score-info"><span>${itemName}</span></div>
                    <div class="slider-container">
                        <input type="hidden" name="itemNames" value="${itemName}">
                        
                        <%-- 슬라이더 값은 정수로 변환 --%>
                        <fmt:parseNumber var="intScore" value="${not empty itemScores ? itemScores[loop.index] : 80}" integerOnly="true" />
                        
                        <input type="range" name="scores" min="0" max="100"
                            value="${intScore}"
                            oninput="document.getElementById('out${loop.index}').innerText = this.value">
                        
                        <span class="current-val" id="out${loop.index}">${intScore}</span>
                        <span class="max-val">/100</span>
                    </div>
                </div>
            </c:forEach>

            <div class="result-box">
                <div>
                    <div style="font-size: 14px; color: #64748b;">종합 점수 (평균)</div>
                    <div class="avg-value" id="avgScore">
                        <c:choose>
                            <c:when test="${not empty evalData && evalData.totalScore != null}">
                                <fmt:formatNumber value="${evalData.totalScore}" pattern="0.0"/>점
                            </c:when>
                            <c:otherwise>80.0점</c:otherwise>
                        </c:choose>
                    </div>
                </div>
                <div style="text-align: right;">
                    <div style="font-size: 14px; color: #64748b;">등급</div>
                    <div class="grade-badge" id="gradeBadge" style="color: ${gradeColor};">
                        ${not empty evalData ? evalData.grade : 'A'}
                    </div>
                </div>
            </div>

            <c:if test="${not empty evalData && not empty evalData.confirmedAt}">
                <div style="font-size: 13px; color: #94a3b8; margin-bottom: 16px;">
                    확정일시: <fmt:formatDate value="${evalData.confirmedAt}" pattern="yyyy-MM-dd HH:mm"/>
                </div>
            </c:if>

            <label style="font-size: 13px; color: #64748b;">평가 코멘트</label>
            <textarea name="evalComment" placeholder="평가 의견을 입력하세요." required>${not empty evalData ? evalData.evalComment : ''}</textarea>

            <div class="btn-area">
                <button type="submit" name="status" value="작성중" class="btn btn-save">임시저장</button>
                <button type="submit" name="status" value="최종확정" class="btn btn-submit">최종 확정</button>
            </div>
        </form>
    </div>

    <%-- 오른쪽: 등급 기준표 사이드바 (부활!) --%>
    <div class="eval-side">
        <div class="section-title" style="font-size: 15px;">등급 기준표</div>
        <table class="grade-table">
            <thead>
                <tr><th>등급</th><th>점수 범위</th><th>의미</th></tr>
            </thead>
            <tbody>
                <tr class="row-s"><td><strong>S</strong></td><td>95점 이상</td><td>최우수</td></tr>
                <tr class="row-a"><td><strong>A</strong></td><td>85 ~ 94</td><td>우수</td></tr>
                <tr><td><strong>B</strong></td><td>75 ~ 84</td><td>양호</td></tr>
                <tr><td><strong>C</strong></td><td>60 ~ 74</td><td>보통</td></tr>
                <tr><td><strong>D</strong></td><td>60점 미만</td><td>미흡</td></tr>
            </tbody>
        </table>
        <div class="warning-box">
            ※ 최종 확정 후에는 수정이 제한될 수 있습니다.<br>
            신중하게 검토 후 제출해 주세요.
        </div>
    </div>
</div>

<script>
    function updateEvaluation() {
        const scores = document.getElementsByName('scores');
        let total = 0;
        let count = scores.length;
        if (count === 0) return;

        scores.forEach(input => {
            total += parseInt(input.value || 0);
        });

        const avg = (total / count).toFixed(1); 
        document.getElementById('avgScore').innerText = avg + "점";

        let grade = 'D';
        let color = '#ef4444';

        if (avg >= 95) { grade = 'S'; color = '#8b5cf6'; }
        else if (avg >= 85) { grade = 'A'; color = '#3b82f6'; }
        else if (avg >= 75) { grade = 'B'; color = '#10b981'; }
        else if (avg >= 60) { grade = 'C'; color = '#f59e0b'; }

        const gradeBadge = document.getElementById('gradeBadge');
        gradeBadge.innerText = grade;
        gradeBadge.style.color = color;
    }

    document.addEventListener('DOMContentLoaded', function() {
        const scoreInputs = document.querySelectorAll('input[name="scores"]');
        scoreInputs.forEach(input => {
            input.addEventListener('input', function() {
                const outputId = this.getAttribute('oninput').match(/'([^']+)'/)[1];
                document.getElementById(outputId).innerText = this.value;
                updateEvaluation();
            });
        });
        updateEvaluation();
    });
</script>