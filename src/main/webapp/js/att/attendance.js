/**
 * 출퇴근 화면 시계 기능
 */
let confirmCallback = null;

function updateClock() {
    const now = new Date();

    const days = ["일","월","화","수","목","금","토"];

    document.getElementById("currentDate").innerText =
        `${now.getFullYear()}년 ${now.getMonth()+1}월 ${now.getDate()}일 (${days[now.getDay()]})`;

    const h = String(now.getHours()).padStart(2, '0');
    const m = String(now.getMinutes()).padStart(2, '0');
    const s = String(now.getSeconds()).padStart(2, '0');

    document.getElementById("currentTime").innerText = `${h}:${m}:${s}`;
}

function updateWorkTime(checkInTime) {

    if (!checkInTime) return;

    const [h, m, s] = checkInTime.split(":").map(Number);

    setInterval(() => {
        const now = new Date();

        const start = new Date();
        start.setHours(h, m, s);

        const diff = now - start;

        const hours = Math.floor(diff / (1000*60*60));
        const minutes = Math.floor((diff / (1000*60)) % 60);

        document.getElementById("workInfo").innerText =
            `${hours}시간 ${minutes}분`;
    }, 1000);
}

window.onload = function() {
    updateClock();
    setInterval(updateClock, 1000);

    const checkIn = document.getElementById("checkInValue").value;
    updateWorkTime(checkIn);
};

// =========================
// 🔹 모달 열기
// =========================
function openConfirmModal(message, callback) {
    const modal = document.getElementById("confirmModal");
    const messageBox = document.getElementById("confirmMessage");

    if (!modal || !messageBox) return; // 🔥 안전 처리

    messageBox.innerText = message;
    modal.style.display = "flex";
    confirmCallback = callback;
}


// =========================
// 🔹 모달 닫기
// =========================
function closeConfirmModal() {
    const modal = document.getElementById("confirmModal");
    if (modal) modal.style.display = "none";
}


// =========================
// 🔹 퇴근 확인
// =========================
function confirmCheckout(form) {
    openConfirmModal("퇴근하시겠습니까?\n이후 수정이 어려울 수 있습니다.", function () {
        form.submit();
    });
}

// 🔥 이거 추가 (핵심)
window.confirmCheckout = confirmCheckout;


// =========================
// 🔹 초기 실행
// =========================
document.addEventListener("DOMContentLoaded", function () {

    // 시계 시작
    updateClock();
    setInterval(updateClock, 1000);

    // 근무시간 시작
    const checkInInput = document.getElementById("checkInValue");
    if (checkInInput) {
        updateWorkTime(checkInInput.value);
    }

    // 모달 확인 버튼 이벤트
    const confirmBtn = document.getElementById("confirmYes");

    if (confirmBtn) {
        confirmBtn.addEventListener("click", function () {
            if (confirmCallback) confirmCallback();
            closeConfirmModal();
        });
    }

});