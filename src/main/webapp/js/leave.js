document.addEventListener("DOMContentLoaded", function () {

    const leaveType = document.getElementById("leaveType");
    const halfTypeDiv = document.getElementById("halfTypeDiv");
    const form = document.querySelector("form");

    // 🔥 반차 선택 시 표시
    if (leaveType) {
        leaveType.addEventListener("change", function () {
            halfTypeDiv.style.display = (this.value === "반차") ? "block" : "none";
        });
    }

    // 🔥 폼 검증
    if (form) {
        form.addEventListener("submit", function (e) {

            const reason = document.querySelector("textarea[name='reason']").value.trim();
            const start = document.querySelector("input[name='start_date']").value;
            const end = document.querySelector("input[name='end_date']").value;

            if (!reason) {
                alert("휴가 사유를 입력해주세요.");
                e.preventDefault();
                return;
            }

            if (start && end && start > end) {
                alert("시작일은 종료일보다 늦을 수 없습니다.");
                e.preventDefault();
                return;
            }
        });
    }

});