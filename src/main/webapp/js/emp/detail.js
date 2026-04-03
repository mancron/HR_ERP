const EDITABLE_FIELDS = {
    default: ['emp_name', 'gender', 'birth_date', 'phone', 'email',
              'emergency_contact', 'bank_account', 'address'],
    admin:   ['emp_name', 'gender', 'birth_date', 'phone', 'email',
              'emergency_contact', 'bank_account', 'address',
              'emp_type', 'base_salary']
};

function getEditableFields() {
    return (USER_ROLE === '관리자' || USER_ROLE === 'HR담당자')
        ? EDITABLE_FIELDS.admin
        : EDITABLE_FIELDS.default;
}

function toggleEditSave() {
    const btn = document.getElementById('btnEditSave');
    const isViewMode = btn.dataset.mode === 'view';

    if (isViewMode) {
        btn.dataset.mode = 'edit';
        btn.textContent = '저장';
        btn.classList.replace('btn-edit', 'btn-save');

        getEditableFields().forEach(name => {
            document.querySelectorAll('[name="' + name + '"]').forEach(el => {
                el.removeAttribute('readonly');
                el.disabled = false;
                el.classList.remove('readonly-input');
            });
        });
    } else {
        document.getElementById('empDetailForm').submit();
    }
}