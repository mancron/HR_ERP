package com.hrms.auth.service;

import com.hrms.auth.dao.AccountDAO;
import com.hrms.auth.dto.AccountDTO;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {
    private AccountDAO accountDAO = new AccountDAO();

    public AccountDTO login(String username, String password) {
        // 1. DB에서 해당 username의 계정 정보를 가져옴
        AccountDTO account = accountDAO.getAccountByUsername(username);

        // 2. 계정이 존재하고 활성화(1) 상태인지 확인
        if (account != null && account.getIsActive() == 1) {
            
            /* * [BCrypt 암호화 체크]
             * password: 사용자가 화면에서 입력한 평문 비번
             * account.getPasswordHash(): DB에 저장된 $2a$10$... 형태의 암호화된 비번
             */
            if (BCrypt.checkpw(password, account.getPasswordHash())) {
                return account; // 인증 성공
            }
        }
        return null; // 인증 실패
    }
    
    public boolean changePassword(String userId, String currentPw, String newPw) {
        // 1. DB에서 해당 사용자의 현재 암호화된 비밀번호 조회 (AccountDAO 사용)
        String hashedPwInDB = accountDAO.getPasswordByUserId(userId);

        if (hashedPwInDB != null) {
            // 2. 사용자가 입력한 현재 비밀번호와 DB의 해시값 비교
            if (BCrypt.checkpw(currentPw, hashedPwInDB)) {
                
                // 3. 일치하면 새 비밀번호를 BCrypt로 암호화(Hashing)
                String newHashedPw = BCrypt.hashpw(newPw, BCrypt.gensalt());
                
                // 4. 암호화된 새 비밀번호를 DB에 업데이트 요청
                return accountDAO.updatePassword(userId, newHashedPw);
            }
        }
        // 현재 비밀번호가 틀렸거나 사용자가 없는 경우
        return false;
    }
}