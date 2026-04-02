function showRejectForm(btn) {
    const tr = btn.closest("tr");

    if (tr.nextElementSibling && tr.nextElementSibling.classList.contains("reject-row")) {
        tr.nextElementSibling.remove();
        return;
    }

    const leaveId = tr.querySelector("input[name='leaveId']").value;

    const newRow = document.createElement("tr");
    newRow.classList.add("reject-row");

    newRow.innerHTML = `
        <td colspan="9">
            <form action="${contextPath}/leave/updateStatus" method="post" class="reject-form">
                <input type="hidden" name="leaveId" value="${leaveId}">
                <input type="hidden" name="status" value="반려">

                <input type="text" name="reason" placeholder="반려 사유를 입력하세요" required>

                <button type="submit" class="btn reject-btn">확인</button>
                <button type="button" class="btn" onclick="this.closest('tr').remove()">취소</button>
            </form>
        </td>
    `;

    tr.after(newRow);
}

function formatDate(dateStr) {
    if (!dateStr) return "-";

    // 🔥 문자열 그대로 처리
    if (dateStr.length === 10) {
        return dateStr; // yyyy-MM-dd 그대로 사용
    }

    const d = new Date(dateStr);

    if (isNaN(d)) return dateStr; // 파싱 실패 대비

    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, "0");
    const dd = String(d.getDate()).padStart(2, "0");

    const hh = String(d.getHours()).padStart(2, "0");
    const min = String(d.getMinutes()).padStart(2, "0");

    return `${yyyy}-${mm}-${dd} ${hh}:${min}`;
}

function formatDateOnly(dateStr) {
    if (!dateStr) return "-";
    return dateStr.substring(0, 10);
}

function openModal(leaveId) {

    fetch(contextPath + "/leave/detail?leaveId=" + leaveId)
        .then(res => res.json())
        .then(data => {

            document.getElementById("modalEmp").innerText =
                `${data.empName} (${data.deptName} / ${data.position})`;

            document.getElementById("modalApplyDate").innerText =
                formatDate(data.createdAt);

            document.getElementById("modalPeriod").innerText =
                `${formatDateOnly(data.startDate)} ~ ${formatDateOnly(data.endDate)}`;

            document.getElementById("modalType").innerText =
                data.leaveType;

            document.getElementById("modalReason").innerText =
                data.reason;

            document.getElementById("modalStatus").innerText =
                data.status;

            // 🔥 승인자 처리
            if (data.status === "대기") {
                document.getElementById("modalApprover").innerText = "-";
            } else {
                const approverName = data.approverName || "-";
                const approverDept = data.approverDept || "-";
                const approverPosition = data.approverPosition || "-";

                document.getElementById("modalApprover").innerText =
                    `${approverName} (${approverDept} / ${approverPosition})`;
            }

            // 🔥 처리일 처리
            document.getElementById("modalApproveDate").innerText =
                data.approvedAt ? formatDate(data.approvedAt) : "-";

            const rejectRow = document.getElementById("modalRejectRow");

            if (data.status === "반려") {
                document.getElementById("modalReject").innerText = data.rejectReason;
                rejectRow.style.display = "table-row";
            } else {
                rejectRow.style.display = "none";
            }

            document.getElementById("leaveModal").style.display = "flex";
        });
}

function closeModal() {
    document.getElementById("leaveModal").style.display = "none";
}

document.addEventListener("DOMContentLoaded", function () {

    initLeaveForm();
    initToast();

});

// 🔹 휴가 신청 UI 로직
function initLeaveForm() {

    const leaveType = document.getElementById("leaveType");
    const halfTypeDiv = document.getElementById("halfTypeDiv");
    const startDate = document.getElementById("startDate");
    const endDate = document.getElementById("endDate");
	const hiddenEndDate = document.getElementById("hiddenEndDate");

    if (leaveType) {
        leaveType.addEventListener("change", function () {

            const isHalf = this.value === "반차";

            // 1. UI 표시
            halfTypeDiv.style.display = isHalf ? "block" : "none";

            if (isHalf) {

                // 🔥 시작일 없으면 먼저 선택하게 유도
                if (!startDate.value) {
                    showToast("시작일을 먼저 선택하세요.", "error");
                    leaveType.value = "연차";
                    return;
                }

                // 🔥 종료일 = 시작일 강제
                endDate.value = startDate.value;
				hiddenEndDate.value = startDate.value;
                // 🔥 선택 자체를 막아버림
                endDate.setAttribute("disabled", true);

            } else {
                endDate.removeAttribute("disabled");
            }
        });
    }

    // 🔥 시작일 바꾸면 무조건 동기화
    if (startDate) {
        startDate.addEventListener("change", function () {
            if (leaveType.value === "반차") {
                endDate.value = startDate.value;
            }
        });
    }
}

// 🔹 Toast 처리
function initToast() {

    const toastData = document.getElementById("toast-data");
    if (!toastData) return;

    const errorMsg = toastData.dataset.error;
    const successMsg = toastData.dataset.msg;

    if (errorMsg && errorMsg.trim() !== "") {
        showToast(errorMsg, "error");
    } else if (successMsg && successMsg.trim() !== "") {
        showToast(successMsg, "success");
    }
}

// 🔹 Toast 생성 함수
function showToast(message, type) {

    const toast = document.createElement("div");
    toast.className = `toast toast-${type}`;
    toast.innerText = message;

    document.body.appendChild(toast);

    setTimeout(() => {
        toast.style.display = "block";
    }, 10);

    setTimeout(() => {
        toast.remove();
    }, 3000);
}