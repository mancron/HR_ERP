// 🔥 contextPath 자동 처리
const contextPath = "/" + window.location.pathname.split("/")[1];

function openFixModal(empId) {

    const month = document.querySelector("input[name='month']").value;

    fetch(`${contextPath}/att/issues?empId=${empId}&month=${month}`)
        .then(res => res.json())
        .then(data => renderIssueModal(empId, data));
}

// =========================
// 모달 닫기
// =========================
function closeModal() {
    document.querySelectorAll(".modal").forEach(m => m.style.display = "none");
}

function submitFix(actionType) {

    const empId = document.getElementById("fixEmpId").value;
    const checkboxes = document.querySelectorAll("input[name='dates']:checked");

    if (checkboxes.length === 0) {
        alert("처리할 항목을 선택하세요.");
        return;
    }

    // 🔥 잘못된 처리 방지
    for (let cb of checkboxes) {

        const row = cb.parentElement;
        const type = row.querySelector(".issue-type").innerText;

        if (actionType === "CHECKOUT_FIX" && type === "결근 후보") {
            alert("결근 후보는 퇴근 보정 불가");
            return;
        }

        if (actionType === "CHECKIN_FIX" && type === "미퇴근") {
            alert("미퇴근은 출근 보정 대상이 아닙니다.");
            return;
        }
    }

    const params = new URLSearchParams();

    params.append("empId", empId);
    params.append("actionType", actionType);

    checkboxes.forEach(cb => {
        params.append("dates", cb.value);
    });

    fetch(`${contextPath}/att/fix`, {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: params.toString()
    }).then(() => {
        const params = new URLSearchParams(window.location.search);
        const month = params.get("month");

        if (month) {
            location.href = `${contextPath}/att/status?month=${month}`;
        } else {
            location.reload();
        }
    });
}

// =========================
// 리스트 렌더링
// =========================
function renderIssueModal(empId, list) {

    document.getElementById("fixEmpId").value = empId;

    const modal = document.getElementById("fixModal");
    const container = document.getElementById("issueList");

    document.getElementById("fixEmpId").value = empId;

    if (list.length === 0) {
        container.innerHTML = "<div style='padding:10px;'>이슈 없음</div>";
        modal.style.display = "block";
        return;
    }

    list.forEach(item => {

        const row = document.createElement("div");
        row.className = "issue-row";

        let disabledCheckout = "";
        let disabledCheckin = "";

        // 👉 타입별 제한
        if (item.type === "결근 후보") {
            disabledCheckout = "disabled"; // 퇴근 보정 불가
        }
        if (item.type === "미퇴근") {
            disabledCheckin = "disabled"; // 출근 보정 불필요
        }

        row.innerHTML = `
		    <input type="checkbox" name="dates" value="${item.date}">
		    <span>${item.date}</span>
		    <span class="issue-type ${item.type}">${item.type}</span>
		    ${item.checkIn ? `<span class="time">${item.checkIn}</span>` : ""}
		`;

        container.appendChild(row);
    });

    modal.style.display = "block";
}

// =========================
// 날짜 선택
// =========================
function selectIssue(date, empId) {
    document.getElementById("absentEmpId").value = empId;
    document.getElementById("selectedDate").value = date;
}

function closeMonthFromInput() {

    const monthInput = document.querySelector("input[name='month']").value;

    if (!monthInput) {
        alert("월을 선택하세요.");
        return;
    }

    const [year, month] = monthInput.split("-");

    closeMonth(year, parseInt(month));
}

function closeMonth(year, month) {

    if (!confirm("정말 마감하시겠습니까?")) return;

    fetch(`${contextPath}/att/close`, {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: `year=${year}&month=${month}`
    }).then(() => {
        alert("마감 완료");
        location.reload();
    });
}