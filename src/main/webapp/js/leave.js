/**
 * 반차 처리
 */
document.addEventListener("DOMContentLoaded", function () {
    const leaveType = document.getElementById("leaveType");
    const halfTypeDiv = document.getElementById("halfTypeDiv");

    if (leaveType) {
        leaveType.addEventListener("change", function () {
            halfTypeDiv.style.display = (this.value === "반차") ? "block" : "none";
        });
    }
});

document.addEventListener("DOMContentLoaded", function () {

    const form = document.querySelector("form");
    const leaveType = document.getElementById("leaveType");
    const halfTypeDiv = document.getElementById("halfTypeDiv");

    leaveType.addEventListener("change", function () {
        halfTypeDiv.style.display = (this.value === "반차") ? "block" : "none";
    });

    form.addEventListener("submit", function (e) {

        const reason = document.querySelector("textarea[name='reason']").value.trim();
        const start = document.querySelector("input[name='start_date']").value;
        const end = document.querySelector("input[name='end_date']").value;

        // 사유 체크
        if (!reason) {
            alert("휴가 사유를 입력해주세요.");
            e.preventDefault();
            return;
        }

        // 날짜 체크
        if (start && end && start > end) {
            alert("시작일은 종료일보다 늦을 수 없습니다.");
            e.preventDefault();
            return;
        }
    });
});