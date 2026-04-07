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

    const form = document.getElementById("fixForm");

    const checkboxes = document.querySelectorAll("input[name='dates']:checked");

    // 🔥 체크 안했을 때 방어
    if (checkboxes.length === 0) {
        alert("처리할 항목을 선택하세요.");
        return;
    }

    const formData = new FormData(form);

    // 체크된 날짜 추가
    checkboxes.forEach(cb => {
        formData.append("dates", cb.value);
    });

    formData.append("actionType", actionType);

    fetch(`${contextPath}/att/fix`, {
        method: "POST",
        body: formData
    }).then(() => location.reload());
}

// =========================
// 리스트 렌더링
// =========================
function renderIssueModal(empId, list) {

    const modal = document.getElementById("fixModal");
    const container = document.getElementById("issueList");

    document.getElementById("fixEmpId").value = empId;

    container.innerHTML = "";

    list.forEach(item => {

        const row = document.createElement("div");
        row.className = "issue-row";

        row.innerHTML = `
            <input type="checkbox" name="dates" value="${item.date}">
            <span>${item.date}</span>
            <span>${item.type}</span>
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