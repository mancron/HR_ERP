/**
 * 사이드바 아코디언 및 활성화 상태 제어
 */

// 1. 아코디언 토글 함수 (클릭 시 동작)
function toggleAccordion(headerElement) {
    const group = headerElement.parentElement;
    const content = group.querySelector('.nav-group-content');
    
    if (group.classList.contains('open')) {
        group.classList.remove('open');
        content.style.maxHeight = null;
    } else {
        // 다른 그룹을 닫고 싶다면 여기서 모든 .nav-group의 open을 제거하면 됩니다.
        group.classList.add('open');
        content.style.maxHeight = content.scrollHeight + "px";
    }
}

// 2. 페이지 로드 시 상태 초기화 (단일 이벤트 리스너로 통합)
document.addEventListener("DOMContentLoaded", () => {
    const currentPath = window.location.pathname;
    const navItems = document.querySelectorAll('.nav-item');

    // 모든 그룹 초기화 (open 제거 및 maxHeight 초기화)
    document.querySelectorAll('.nav-group').forEach(group => {
        group.classList.remove('open');
        const content = group.querySelector('.nav-group-content');
        if (content) content.style.maxHeight = null;
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
            }
        } else {
            item.classList.remove('active');
        }
    });
});