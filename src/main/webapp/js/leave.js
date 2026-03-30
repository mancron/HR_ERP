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