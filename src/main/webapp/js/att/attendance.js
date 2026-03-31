/**
 * 출퇴근 화면 시계 기능
 */

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