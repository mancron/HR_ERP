<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/evaluation.css">

<div class="eval-wrapper">
    <div class="eval-main">
        <div class="section-title">평가 작성</div>
        
        <form action="${pageContext.request.contextPath}/eval/write" method="post" id="evalForm">
            <div class="form-grid">
                <div class="form-group">
                    <label>평가 대상자 *</label>
                    <select name="empId"><option value="1">홍길동 (부장)</option></select>
                </div>
                <div class="form-group">
                    <label>평가 연도 *</label>
                    <select name="evalYear"><option value="2024">2024년</option></select>
                </div>
                <div class="form-group">
                    <label>평가 기간 *</label>
                    <select name="evalPeriod"><option value="하반기">하반기</option></select>
                </div>
                <div class="form-group">
                    <label>평가 유형 *</label>
                    <select name="evalType"><option value="상위평가">상위평가</option></select>
                </div>
            </div>

            <div style="font-weight: 700; margin-bottom: 20px;">📊 항목별 점수 (각 100점 만점)</div>
            
            <% String[] items = {"업무성과", "직무역량", "조직기여도", "리더십"};
               for(String item : items) { %>
                <div class="score-item">
                    <div class="score-info"><span><%=item%></span></div>
                    <div class="slider-container">
                        <input type="hidden" name="itemNames" value="<%=item%>">
                        <input type="range" name="scores" min="0" max="100" value="90" oninput="updateEval()">
                        <span class="current-val">90</span><span class="max-val">/100</span>
                    </div>
                </div>
            <% } %>

            <div class="result-box">
                <div>
                    <div style="font-size: 14px; color: #64748b;">종합 점수 (평균)</div>
                    <div class="avg-value" id="avgScore">90.0점</div>
                </div>
                <div style="text-align: right;">
                    <div style="font-size: 14px; color: #64748b;">등급</div>
                    <div class="grade-badge" id="gradeBadge">S</div>
                </div>
            </div>

            <label style="font-size: 13px; color: #64748b;">평가 코멘트</label>
            <textarea name="evalComment" placeholder="평가 의견을 입력하세요."></textarea>
            
            <div class="btn-area">
                <button type="submit" name="status" value="작성중" class="btn btn-save">임시저장</button>
                <button type="submit" name="status" value="최종확정" class="btn btn-submit">최종 확정</button>
            </div>
        </form>
    </div>

    <div class="eval-side">
        <div style="font-weight: 700; margin-bottom: 15px;">등급 기준</div>
        <table class="grade-table">
            <thead>
                <tr style="color: #64748b;"><th>등급</th><th>점수</th><th>설명</th></tr>
            </thead>
            <tbody>
                <tr class="row-s"><td style="color:#ef4444; font-weight:700;">S</td><td>90점 ↑</td><td>탁월</td></tr>
                <tr class="row-a"><td style="color:#eab308; font-weight:700;">A</td><td>80점 ↑</td><td>우수</td></tr>
                <tr><td>B</td><td>70점 ↑</td><td>보통</td></tr>
                <tr><td>C</td><td>60점 ↑</td><td>미흡</td></tr>
                <tr><td>D</td><td>60점 ↓</td><td>불량</td></tr>
            </tbody>
        </table>
        <div class="warning-box">
            ⚠ 최종확정 후 수정 불가. 급여 연동은 최종확정 상태에서만 허용됩니다.
        </div>
    </div>
</div>

<script>
function updateEval() {
    const sliders = document.querySelectorAll('input[type="range"]');
    let sum = 0;
    
    sliders.forEach(s => {
        const val = parseInt(s.value);
        sum += val;
        // 슬라이더 옆 숫자 갱신
        s.parentElement.querySelector('.current-val').innerText = val;
    });

    const avg = sum / sliders.length;
    document.getElementById('avgScore').innerText = avg.toFixed(1) + '점';

    // 실시간 등급 및 색상 변경
    let grade = 'D';
    let color = '#64748b';
    if (avg >= 90) { grade = 'S'; color = '#ef4444'; }
    else if (avg >= 80) { grade = 'A'; color = '#eab308'; }
    else if (avg >= 70) { grade = 'B'; color = '#3b82f6'; }
    else if (avg >= 60) { grade = 'C'; color = '#6366f1'; }

    const badge = document.getElementById('gradeBadge');
    badge.innerText = grade;
    badge.style.color = color;
}
// 초기 로딩 시 계산 실행
window.onload = updateEval;
</script>