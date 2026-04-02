package com.hrms.auth.service;

import java.util.HashMap;
import java.util.Map;

import org.mindrot.jbcrypt.BCrypt;

import com.hrms.auth.dao.AccountDAO;
import com.hrms.auth.dto.AccountDTO;
import com.hrms.common.util.NotificationUtil;

public class AuthService {
    private AccountDAO accountDAO = new AccountDAO();

    /**
     * [리팩토링] JSP의 스크립틀릿과 <script> 로직을 서비스로 통합
     */
    public Map<String, String> getLoginViewData(String msg) {
        Map<String, String> data = new HashMap<>();
        
        // 1. 관리자 연락처
        String adminPhone = accountDAO.getAdminContact();
        if (adminPhone == null || adminPhone.isEmpty()) adminPhone = "인사팀 문의";
        data.put("adminPhone", adminPhone);

        // 2. 실패 횟수 (substring 로직)
        String failCount = "0";
        if (msg != null && msg.startsWith("login_fail_")) {
            failCount = msg.substring(msg.lastIndexOf("_") + 1);
        }
        data.put("failCount", failCount);

        // 3. [중요] <script> alert 대신 화면 상단에 띄울 강조 메시지
        String systemNotice = "";
        if ("locked".equals(msg)) {
            systemNotice = "⚠️ 계정 잠김: 보안을 위해 차단되었습니다. 담당자(" + adminPhone + ")에게 문의하세요.";
        } else if ("pw_success".equals(msg)) {
            systemNotice = "✅ 비밀번호 변경 완료: 새로운 비밀번호로 로그인해주세요.";
        }
        data.put("systemNotice", systemNotice);

        return data;
    }

    public AccountDTO login(String username, String password) throws Exception {
        // 1. 사용자 정보 로드
        AccountDTO account = accountDAO.getAccountByUsername(username);
        // 사용자가 존재하지 않는 경우
        if (account == null) {
            throw new Exception("invalid_user");
        }
        
        // 2. [변경] is_active 대신 login_attempts 수치로 잠금 여부 판단
        // DB에서 가져온 시도 횟수가 이미 5회 이상이라면 즉시 차단
        if (account.getLoginAttempts() >= 5) {
            throw new Exception("account_locked");
        }

        // 3. 비밀번호 검증 (BCrypt)
        if (BCrypt.checkpw(password, account.getPasswordHash())) {
            accountDAO.handleLoginSuccess(username);
            return account;
        } else {
            accountDAO.handleLoginFailure(username);
            AccountDTO updated = accountDAO.getAccountByUsername(username);
            int currentAttempts = (updated != null) ? updated.getLoginAttempts() : 1;

            // 지금 막 5회가 된 경우 → 알림 발송 후 잠금 예외
            if (currentAttempts >= 5) {
                try {
                    int[] adminIds = accountDAO.getAdminEmpIds();
                    if (adminIds.length > 0) {
                        NotificationUtil.sendAccountLocked(adminIds, account.getUsername());
                    }
                } catch (Exception e) {
                    e.printStackTrace(); // 알림 실패가 로그인 로직에 영향 주면 안 됨
                }
                throw new Exception("account_locked");
            }
            throw new Exception("login_fail_" + currentAttempts);
        }
    }

    public Map<String, String> getPwChangeViewData(String error) {
        Map<String, String> data = new HashMap<>();
        String errorMsg = "";

        if ("mismatch".equals(error)) {
            errorMsg = "❌ 새 비밀번호와 확인 비밀번호가 일치하지 않습니다.";
        } else if ("fail".equals(error)) {
            errorMsg = "❌ 현재 비밀번호가 일치하지 않거나 변경에 실패했습니다.";
        }

        data.put("errorMsg", errorMsg);
        return data;
    }
    
    public boolean changePassword(String userId, String currentPw, String newPw) {
        AccountDTO account = accountDAO.getAccountByUsername(userId);
        if (account != null && BCrypt.checkpw(currentPw, account.getPasswordHash())) {
            String newHashedPw = BCrypt.hashpw(newPw, BCrypt.gensalt());
            return accountDAO.updatePassword(userId, newHashedPw);
        }
        return false;
    }
}