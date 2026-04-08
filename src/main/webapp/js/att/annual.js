// ================================
// 연차 조정 모달 관련 스크립트
// ================================

let currentTotal = 0;

// 모달 열기
function openAdjustModal(empId, totalDays) {
    currentTotal = totalDays;

    document.getElementById("empId").value = empId;

    const input = document.querySelector("input[name='totalDays']");
    input.value = totalDays;

    // ✅ 추가 (현재 연차 표시)
    const currentEl = document.getElementById("currentTotal");
    if (currentEl) currentEl.innerText = totalDays;

    updateDiff();

    document.getElementById("adjustModal").style.display = "block";
}

// 모달 닫기
function closeModal() {
    document.getElementById("adjustModal").style.display = "none";
}

// 변경량 계산
function updateDiff() {
    const input = document.querySelector("input[name='totalDays']");
    const diffEl = document.getElementById("diff");

    if (!input || !diffEl) return;

    const newValue = parseFloat(input.value);

    if (isNaN(newValue)) {
        diffEl.innerText = "";
        return;
    }

    const diff = newValue - currentTotal;

    if (diff === 0) {
        diffEl.innerText = "변경 없음";
        diffEl.style.color = "gray";
    } else if (diff > 0) {
        diffEl.innerText = "+" + diff + "일 증가";
        diffEl.style.color = "blue";
    } else {
        diffEl.innerText = diff + "일 감소";
        diffEl.style.color = "red";
    }
}

// 입력 이벤트
document.addEventListener("DOMContentLoaded", function () {

    const input = document.querySelector("input[name='totalDays']");

    if (input) {
        input.addEventListener("input", updateDiff);
    }

});