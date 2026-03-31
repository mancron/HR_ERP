/**
 * 직원 목록 페이지 전용 스크립트
 */
document.addEventListener('DOMContentLoaded', function () {
	const CPATH = "/hr_erp"; // 프로젝트의 contextPath
    const modal = document.getElementById('empDetailModal');
    const iframe = document.getElementById('modalIframe');
    const closeBtn = document.getElementById('closeModalBtn');
    
    //body에서 contextPath를 가져옵니다.
    const contextPath = document.body.dataset.contextPath || ''; 
    
    //경로가 중복되지 않도록 baseUrl을 생성합니다.
    const baseUrl = contextPath + '/emp/detail?emp_no=';

    

    // 모달 닫기
    if (closeBtn) {
        closeBtn.addEventListener('click', function () {
            modal.classList.remove('active');
            iframe.src = '';
        });
    }
	
	// 상세 버튼 클릭 이벤트 잡기
    $(".btn-detail").on("click", function(e) {
        e.preventDefault(); // 기본 동작 중지
		e.stopPropagation(); //중복 방지
        
        const empNo = $(this).data("empno");

		
        //서버에 세션이 살아있는지 먼저 물어보기
		fetch(CPATH + '/auth/check-session')
		    .then(response => {
		        if (!response.ok) {
		            throw new Error('서버 응답 상태 이상: ' + response.status);
		        }
		        return response.json();
		    })
		    .then(data => {
		        if (data.isAlive) {
					modal.classList.add('active');
		            $("#modalIframe").attr("src", CPATH + "/emp/detail?emp_no=" + empNo);
					
		        } else {
		            alert("세션이 만료되었습니다.");
		            window.top.location.href = CPATH + "/auth/login";
		        }
		    })
		    .catch(err => {
		        console.error("상세 에러 내용:", err);
		        alert("세션 정보가 없거나 서버와 연결이 끊겼습니다. 로그인 페이지로 이동합니다.");
		        window.top.location.href = CPATH + "/auth/login";
		    });
    });
});

