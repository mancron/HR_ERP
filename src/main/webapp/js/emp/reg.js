let emailChecked = false;

function checkEmail() {
    const prefix = document.getElementById('emailPrefix').value.trim();
    const msg = document.getElementById('emailMsg');

    if (!prefix) {
        msg.textContent = '이메일 앞부분을 입력해주세요.';
        msg.className = 'msg error';
        return;
    }

    const fullEmail = prefix + '@example.com';

    fetch(contextPath + '/emp/reg?action=checkEmail&email=' + encodeURIComponent(fullEmail))
        .then(res => res.json())
        .then(data => {
            if (data.exists) {
                msg.textContent = '이미 사용 중인 이메일입니다.';
                msg.className = 'msg error';
                emailChecked = false;
            } else {
                msg.textContent = '사용 가능한 이메일입니다.';
                msg.className = 'msg success';
                emailChecked = true;
                // hidden 필드에 완성된 이메일 세팅
                document.getElementById('email').value = fullEmail;
            }
        });
}

// 이메일 앞부분 변경 시 초기화
document.addEventListener('DOMContentLoaded', function () {
    document.getElementById('emailPrefix').addEventListener('input', function () {
        emailChecked = false;
        document.getElementById('emailMsg').textContent = '';
        document.getElementById('emailMsg').className = 'msg';
        document.getElementById('email').value = '';
    });
});