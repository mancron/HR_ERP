/**
 * 직원 목록 페이지 전용 스크립트
 */
document.addEventListener('DOMContentLoaded', function () {
    const modal = document.getElementById('empDetailModal');
    const iframe = document.getElementById('modalIframe');
    const closeBtn = document.getElementById('closeModalBtn');
    
    // 1. body에서 contextPath를 가져옵니다.
    const contextPath = document.body.dataset.contextPath || ''; 
    
    // 2. 경로가 중복되지 않도록 baseUrl을 생성합니다.
    const baseUrl = contextPath + '/emp/detail?emp_no=';

    document.querySelectorAll('.btn-detail').forEach(function (btn) {
        btn.addEventListener('click', function () {
            const empNo = this.getAttribute('data-empno'); // [cite: 30]
            if (empNo) {
                // 최종 URL 확인용 콘솔 (F12에서 확인 가능)
                console.log("Request URL:", baseUrl + empNo); 
                iframe.src = baseUrl + empNo; // [cite: 31]
                modal.classList.add('active');
            }
        });
    });

    // 모달 닫기
    if (closeBtn) {
        closeBtn.addEventListener('click', function () {
            modal.classList.remove('active');
            iframe.src = '';
        });
    }
});