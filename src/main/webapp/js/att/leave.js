function showRejectForm(btn) {
    const tr = btn.closest("tr");

    // 🔥 다른 열려있는 것 닫기
    document.querySelectorAll(".reject-row").forEach(row => row.remove());

    // 이미 열려있으면 닫기
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

                <input type="text" name="reason" class="reject-input"
                       placeholder="반려 사유를 입력하세요" required>

                <button type="button" class="btn reject-btn confirm-reject">
                    확인
                </button>

                <button type="button" class="btn cancel-reject">
                    취소
                </button>
            </form>
        </td>
    `;

    tr.after(newRow);

    // 🔥 이벤트 연결
    const form = newRow.querySelector(".reject-form");
    const input = newRow.querySelector(".reject-input");

    // 취소
    newRow.querySelector(".cancel-reject").addEventListener("click", function() {
        newRow.remove();
    });

    // 확인
    newRow.querySelector(".confirm-reject").addEventListener("click", function() {

        const reason = input.value.trim();

        if (!reason) {
            showToast("반려 사유를 입력하세요.", "error");
            return;
        }

        // 🔥 모달 확인 추가
        openConfirmModal("정말 반려하시겠습니까?", function() {
            form.submit();
        });
    });

    // 🔥 입력창 자동 포커스
    input.focus();
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

document.addEventListener("DOMContentLoaded", function() {

    initLeaveForm();

});

// 🔹 휴가 신청 UI 로직
function initLeaveForm() {

    const leaveType = document.getElementById("leaveType");
    const halfTypeDiv = document.getElementById("halfTypeDiv");
    const startDate = document.getElementById("startDate");
    const endDate = document.getElementById("endDate");
    const hiddenEndDate = document.getElementById("hiddenEndDate");

    if (leaveType) {
        leaveType.addEventListener("change", function() {

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
        startDate.addEventListener("change", function() {
            if (leaveType.value === "반차") {
                endDate.value = startDate.value;
            }
        });
    }
}

let confirmCallback = null;

function approveLeave(form) {
    openConfirmModal("휴가를 승인하시겠습니까?", function() {
        form.submit();
    });
}

function openConfirmModal(message, callback) {
    const modal = document.getElementById("confirmModal");

    document.getElementById("confirmMessage").innerText = message;

    modal.style.display = "flex";   // 🔥 핵심 수정
    confirmCallback = callback;
}

function closeConfirmModal() {
    document.getElementById("confirmModal").style.display = "none";
    confirmCallback = null;
}

document.addEventListener("DOMContentLoaded", function() {
    const confirmYes = document.getElementById("confirmYes");

    if (confirmYes) {
        confirmYes.addEventListener("click", function() {
            if (confirmCallback) confirmCallback();
            closeConfirmModal();
        });
    }

    closeConfirmModal();
    confirmCallback = null;
});

window.addEventListener("pageshow", function () {
    closeConfirmModal();
    confirmCallback = null;
});