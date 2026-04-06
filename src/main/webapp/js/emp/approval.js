/**
 * approval.js
 * 결재 관련 공통 JavaScript (approval.jsp / approvalHistory.jsp 공용)
 */

/**
 * 신청 상세 모달 열기 (세션 체크 포함)
 * @param {string} url - approvalDetail 요청 URL
 * @param {string} contextPath - 애플리케이션 컨텍스트 경로
 */
function openApprovalModal(url, contextPath) {
    const ctx = contextPath || document.body.dataset.contextPath || '';

    fetch(ctx + '/auth/check-session')
        .then(response => {
            if (!response.ok) throw new Error('서버 응답 오류: ' + response.status);
            return response.json();
        })
        .then(data => {
            if (data.isAlive) {
                document.getElementById('approvalModalIframe').src = url;
                document.getElementById('approvalDetailModal').classList.add('active');
            } else {
                alert('세션이 만료되었습니다.');
                window.top.location.href = ctx + '/auth/login';
            }
        })
        .catch(err => {
            console.error('세션 체크 오류:', err);
            alert('세션 정보가 없거나 서버와 연결이 끊겼습니다. 로그인 페이지로 이동합니다.');
            window.top.location.href = ctx + '/auth/login';
        });
}

/**
 * 모달 닫기
 */
function closeApprovalModal() {
    document.getElementById('approvalDetailModal').classList.remove('active');
    document.getElementById('approvalModalIframe').src = '';
}

/**
 * 반려 사유 입력 후 이동
 * @param {string} url - approvalAction 기본 URL (type, id, action=reject 포함)
 */
function rejectWithReason(url) {
    const reason = prompt('반려 사유를 입력하세요.');
    if (reason === null) return;
    if (reason.trim() === '') {
        alert('반려 사유를 입력해주세요.');
        return;
    }
    location.href = url + '&rejectReason=' + encodeURIComponent(reason);
}

/**
 * DOM 준비 후 공통 이벤트 바인딩
 */
document.addEventListener('DOMContentLoaded', function () {
    const closeBtn = document.getElementById('closeApprovalModalBtn');
    if (closeBtn) {
        closeBtn.addEventListener('click', closeApprovalModal);
    }
});