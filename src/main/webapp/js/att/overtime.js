/**
 * 초과근무 승인/반려 JS (휴가 방식 적용)
 */

function showRejectForm(btn) {
    const tr = btn.closest("tr");

    // 🔥 다른 열린 행 제거
    document.querySelectorAll(".reject-row").forEach(row => row.remove());

    // 이미 열려있으면 닫기
    if (tr.nextElementSibling && tr.nextElementSibling.classList.contains("reject-row")) {
        tr.nextElementSibling.remove();
        return;
    }

    const otId = tr.querySelector("input[name='overtimeId']").value;

    const newRow = document.createElement("tr");
    newRow.classList.add("reject-row");

    newRow.innerHTML = `
        <td colspan="8">
            <form action="${contextPath}/overtime/updateStatus" method="post" class="reject-form">
                <input type="hidden" name="overtimeId" value="${otId}">
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

    const form = newRow.querySelector(".reject-form");
    const input = newRow.querySelector(".reject-input");

    // 취소 버튼
    newRow.querySelector(".cancel-reject").addEventListener("click", function() {
        newRow.remove();
    });

    // 확인 버튼
    newRow.querySelector(".confirm-reject").addEventListener("click", function() {

        const reason = input.value.trim();

        if (!reason) {
            alert("반려 사유를 입력하세요.");
            return;
        }

        openConfirmModal("정말 반려하시겠습니까?", function() {
            form.submit();
        });
    });

    input.focus();
}

/**
 * 승인
 */
function approveOvertime(form) {
    openConfirmModal("초과근무를 승인하시겠습니까?", function() {
        form.submit();
    });
}

/**
 * 공통 모달
 */
let confirmCallback = null;

function openConfirmModal(message, callback) {
    const modal = document.getElementById("confirmModal");

    document.getElementById("confirmMessage").innerText = message;

    modal.style.display = "flex";
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
});