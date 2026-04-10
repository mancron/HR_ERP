/**
 * 사이드바 아코디언 및 활성화 상태 제어
 */

function getGroupKey(group) {
    const header = group.querySelector('.nav-group-header');
    return header ? header.textContent.trim() : null;
}

function toggleAccordion(headerElement) {
    const group = headerElement.parentElement;
    const content = group.querySelector('.nav-group-content');
    const groupKey = getGroupKey(group);

    let openGroups = JSON.parse(sessionStorage.getItem('openNavGroups') || '[]');

    if (group.classList.contains('open')) {
        group.classList.remove('open');
        content.style.maxHeight = null;
        openGroups = openGroups.filter(k => k !== groupKey);
    } else {
        group.classList.add('open');
        content.style.maxHeight = content.scrollHeight + "px";
        if (!openGroups.includes(groupKey)) openGroups.push(groupKey);
    }

    sessionStorage.setItem('openNavGroups', JSON.stringify(openGroups));
}

document.addEventListener("DOMContentLoaded", () => {
    const currentPath = window.location.pathname;
    const navItems = document.querySelectorAll('.nav-item');
    const openGroups = JSON.parse(sessionStorage.getItem('openNavGroups') || '[]');
    const sidebar = document.getElementById('sidebar');

    // 초기화 중 애니메이션 끄기
    sidebar.classList.add('no-transition');

    // 저장된 그룹명 기준으로 복원
    document.querySelectorAll('.nav-group').forEach(group => {
        const groupKey = getGroupKey(group);
        const content = group.querySelector('.nav-group-content');
        group.classList.remove('open');
        if (content) content.style.maxHeight = null;

        if (groupKey && openGroups.includes(groupKey)) {
            group.classList.add('open');
            if (content) content.style.maxHeight = content.scrollHeight + "px";
        }
    });

    // 현재 경로에 맞는 메뉴 활성화 및 부모 그룹 열기
    navItems.forEach(item => {
        const itemHref = item.getAttribute('href');
        if (currentPath === itemHref || currentPath.endsWith(itemHref)) {
            item.classList.add('active');
            const parentGroup = item.closest('.nav-group');
            if (parentGroup) {
                parentGroup.classList.add('open');
                const content = parentGroup.querySelector('.nav-group-content');
                if (content) content.style.maxHeight = content.scrollHeight + "px";

                const groupKey = getGroupKey(parentGroup);
                if (groupKey) {
                    let savedGroups = JSON.parse(sessionStorage.getItem('openNavGroups') || '[]');
                    if (!savedGroups.includes(groupKey)) {
                        savedGroups.push(groupKey);
                        sessionStorage.setItem('openNavGroups', JSON.stringify(savedGroups));
                    }
                }
            }
        } else {
            item.classList.remove('active');
        }
    });

    // 렌더링 후 애니메이션 다시 켜기
    void sidebar.offsetHeight;
    sidebar.classList.remove('no-transition');
});