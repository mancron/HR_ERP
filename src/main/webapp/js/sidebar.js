/**
 * 사이드바 아코디언 및 활성화 상태 제어
 */

// 1. 아코디언 토글 함수 (클릭 시 동작)
function toggleAccordion(headerElement) {
    const group = headerElement.parentElement;
    const content = group.querySelector('.nav-group-content');
	const allGroups = Array.from(document.querySelectorAll('.nav-group'));
	const groupIndex = allGroups.indexOf(group);

	let openGroups = JSON.parse(sessionStorage.getItem('openNavGroups') || '[]');
    
    if (group.classList.contains('open')) {
        group.classList.remove('open');
        content.style.maxHeight = null;
		openGroups = openGroups.filter(i => i !== groupIndex);
    } else {
        // 다른 그룹을 닫고 싶다면 여기서 모든 .nav-group의 open을 제거하면 됩니다.
        group.classList.add('open');
        content.style.maxHeight = content.scrollHeight + "px";
		if (!openGroups.includes(groupIndex)) openGroups.push(groupIndex);
    }
	sessionStorage.setItem('openNavGroups', JSON.stringify(openGroups));
}

// 2. 페이지 로드 시 상태 초기화 (단일 이벤트 리스너로 통합)
document.addEventListener("DOMContentLoaded", () => {
    const currentPath = window.location.pathname;
    const navItems = document.querySelectorAll('.nav-item');
	const openGroups = JSON.parse(sessionStorage.getItem('openNavGroups') || '[]');
	const sidebar = document.getElementById('sidebar');
	
	// 1. 초기화 중에는 애니메이션 끄기
	sidebar.classList.add('no-transition');

    // 모든 그룹 초기화 (open 제거 및 maxHeight 초기화)
    document.querySelectorAll('.nav-group').forEach((group, index) => {
        const content = group.querySelector('.nav-group-content');
		if (openGroups.includes(index)) {
            group.classList.add('open');
            if (content) content.style.maxHeight = content.scrollHeight + "px";
        }
    });

    // 현재 경로에 맞는 메뉴 활성화 및 부모 그룹 열기
    navItems.forEach(item => {
        const itemHref = item.getAttribute('href');
        
        // ContextPath 포함 여부에 따라 유연하게 매칭 (완전 일치 또는 포함)
        if (currentPath === itemHref || currentPath.endsWith(itemHref)) {
            item.classList.add('active');
            
            // 상위 그룹 찾기
            const parentGroup = item.closest('.nav-group');
            if (parentGroup) {
                parentGroup.classList.add('open');
                // 애니메이션을 위해 maxHeight 설정
                const content = parentGroup.querySelector('.nav-group-content');
                if (content) {
                    content.style.maxHeight = content.scrollHeight + "px";
                }
				// 부모 그룹 인덱스도 sessionStorage에 저장
                const allGroups = Array.from(document.querySelectorAll('.nav-group'));
                const groupIndex = allGroups.indexOf(parentGroup);
				if (!openGroups.includes(groupIndex)) {
                openGroups.push(groupIndex);
                sessionStorage.setItem('openNavGroups', JSON.stringify(openGroups));
                }
            }
        }
    });
	// 2. 브라우저가 렌더링을 마친 후 애니메이션 다시 켜기
    // 강제로 리플로우(Reflow)를 발생시켜 transition 방지 적용을 확정지음
    void sidebar.offsetHeight; 
    sidebar.classList.remove('no-transition');
});