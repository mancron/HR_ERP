<%@ page contentType="text/html; charset=UTF-8"%>
<div id="requestModal" class="modal">
    <div class="modal-content">

        <span class="close" onclick="closeModal()">&times;</span>

        <h3>상세보기</h3>

        <div class="modal-section">
            <h4>신청 정보</h4>

            <table class="detail-table">
                <tr><th>신청자</th><td id="modalEmp"></td></tr>
                <tr><th>신청일</th><td id="modalApplyDate"></td></tr>
                <tr><th>기간</th><td id="modalDate"></td></tr>
                <tr><th>유형</th><td id="modalType"></td></tr>
                <tr><th>사유</th><td id="modalReason"></td></tr>
            </table>
        </div>

        <div class="modal-section">
            <h4>승인 정보</h4>

            <table class="detail-table">
                <tr><th>상태</th><td id="modalStatus"></td></tr>
                <tr><th>승인자</th><td id="modalApprover"></td></tr>
                <tr><th>처리일</th><td id="modalApproveDate"></td></tr>
                <tr id="modalRejectRow">
                    <th>반려 사유</th>
                    <td id="modalReject"></td>
                </tr>
            </table>
        </div>

    </div>
</div>