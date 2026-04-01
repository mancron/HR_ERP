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

document.addEventListener("DOMContentLoaded", function() {

    const leaveType = document.getElementById("leaveType");
    const halfTypeDiv = document.getElementById("halfTypeDiv");
    const form = document.querySelector("form");

    // 🔥 반차 선택 시 표시
    if (leaveType) {
        leaveType.addEventListener("change", function() {
            halfTypeDiv.style.display = (this.value === "반차") ? "block" : "none";
        });
    }

    // 🔥 폼 검증
    if (form) {
        form.addEventListener("submit", function(e) {

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

    const filterForm = document.querySelector(".filter-box");

    if (filterForm) {
        filterForm.addEventListener("submit", function(e) {

            const start = document.querySelector("input[name='startDate']").value;
            const end = document.querySelector("input[name='endDate']").value;

            if (start && end && start > end) {
                alert("시작일은 종료일보다 늦을 수 없습니다.");
                e.preventDefault();
            }
        });
    }

});