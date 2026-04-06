function openDetail(id) {

    fetch(contextPath + currentPath + "/detail?id=" + id)
        .then(res => res.json())
        .then(data => {

            document.getElementById("modalEmp").innerText = data.empName;
            document.getElementById("modalDate").innerText = data.date;
            document.getElementById("modalType").innerText = data.type;
            document.getElementById("modalReason").innerText = data.reason;
            document.getElementById("modalApplyDate").innerText = data.applyDate;

            document.getElementById("modalStatus").innerText = data.status;
            document.getElementById("modalApprover").innerText = data.approverName;
            document.getElementById("modalApproveDate").innerText = data.approveDate;

            const rejectRow = document.getElementById("modalRejectRow");

            if (data.status === "반려") {
                rejectRow.style.display = "table-row";
                document.getElementById("modalReject").innerText = data.rejectReason;
            } else {
                rejectRow.style.display = "none";
            }

            document.getElementById("requestModal").style.display = "flex";
        });
}

function cancelRequest(id) {

    if (!confirm("신청을 취소하시겠습니까?")) return;

    fetch(contextPath + currentPath + "/cancel", {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: "id=" + id
    })
    .then(res => res.text())
    .then(() => {
        alert("취소되었습니다.");
        location.reload();
    })
    .catch(err => console.error(err));
}

function closeModal() {
    document.getElementById("requestModal").style.display = "none";
}