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
	
	const filterForm = document.querySelector(".filter-box");

	if (filterForm) {
	    filterForm.addEventListener("submit", function (e) {

	        const start = document.querySelector("input[name='startDate']").value;
	        const end = document.querySelector("input[name='endDate']").value;

	        if (start && end && start > end) {
	            alert("시작일은 종료일보다 늦을 수 없습니다.");
	            e.preventDefault();
	        }
	    });
	}

});