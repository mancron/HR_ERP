package com.hrms.auth.service;

import java.util.HashMap;
import java.util.Map;

import org.mindrot.jbcrypt.BCrypt;

import com.hrms.auth.dao.AccountDAO;
import com.hrms.auth.dto.AccountDTO;
import com.hrms.auth.dto.LoginResultDTO;
import com.hrms.common.util.NotificationUtil;
import com.hrms.emp.dao.EmpDAO;
import com.hrms.emp.dto.EmpDTO;
import com.hrms.org.dao.DeptDAO;
import com.hrms.org.dao.PosDAO;

public class AuthService {
    private AccountDAO accountDAO = new AccountDAO();

    /**
     * 로그인 화면에 표시할 데이터를 정리 (비활성화 메시지 노출 보장)
     */
    public Map<String, String> getLoginViewData(String msg) {
        Map<String, String> data = new HashMap<>();
        
        // 1. 관리자 연락처 로드
        String adminPhone = accountDAO.getAdminContact();
        if (adminPhone == null || adminPhone.isEmpty()) adminPhone = "051-890-0000";
        data.put("adminPhone", adminPhone);

        // 2. 실패 횟수 추출
        String failCount = "0";
        if (msg != null && msg.startsWith("login_fail_")) {
            failCount = msg.substring(msg.lastIndexOf("_") + 1);
        }
        data.put("failCount", failCount);

        // 3. 시스템 안내 메시지 (systemNotice)
        String systemNotice = "";
        
        if (msg == null || msg.isEmpty()) {
            systemNotice = "";
        } 
        // [중요] retired_user 체크를 가장 위로 올림
        else if ("retired_user".equals(msg)) {
            systemNotice = "🚫 비활성화된 계정입니다.";
        } 
        else if ("account_locked".equals(msg) || "locked".equals(msg)) {
            systemNotice = "⚠️ 5회 연속 실패 시 보안을 위해 계정이 잠깁니다. 담당자(" + adminPhone + ")에게 문의하세요.";
        } 
        else if ("pw_success".equals(msg)) {
            systemNotice = "✅ 비밀번호 변경 완료: 새로운 비밀번호로 로그인해주세요.";
        } 
        else if ("invalid_user".equals(msg)) {
            systemNotice = "❌ 정보를 찾을 수 없습니다. 아이디와 비밀번호를 다시 확인해주세요.";
        } 
        else if (msg.startsWith("login_fail_")) {
            systemNotice = "❌ 로그인 실패: 비밀번호가 일치하지 않습니다. (현재 " + failCount + "회 실패)";
        }
        
        data.put("systemNotice", systemNotice);

        return data;
    }

    /**
     * 로그인 로직: 비활성화 체크 -> 잠금 체크 -> 비밀번호 검증
     */
    /**
     * 로그인 로직: 비활성화 체크 -> 잠금 체크 -> 비밀번호 검증 및 정보 조립
     */
    public LoginResultDTO login(String username, String password) throws Exception {
        AccountDTO account = accountDAO.getAccountByUsername(username);
        
        if (account == null) {
            throw new Exception("invalid_user");
        }
        
        // 1. 비활성화 계정 체크 (is_active = 0)
        if (account.getIsActive() == 0) {
            throw new Exception("retired_user");
        }

        // 2. 계정 잠금 체크 (시도 횟수 5회 이상)
        if (account.getLoginAttempts() >= 5) {
            throw new Exception("account_locked");
        }

        // 3. 비밀번호 검증
        if (BCrypt.checkpw(password, account.getPasswordHash())) {
            accountDAO.handleLoginSuccess(username);
            
            // --- [핵심] 서블릿에 있던 로직을 서비스로 이동하여 데이터 조립 ---
            EmpDAO empDao = new EmpDAO();
            EmpDTO empInfo = empDao.getEmployeeById(account.getEmpId());
            
            if (empInfo != null) {
                if (empInfo.getDept_name() == null || empInfo.getDept_name().isEmpty()) {
                    empInfo.setDept_name(new DeptDAO().getDeptNameById(empInfo.getDept_id()));
                }
                if (empInfo.getPosition_name() == null || empInfo.getPosition_name().isEmpty()) {
                    empInfo.setPosition_name(new PosDAO().getPositionNameById(empInfo.getPosition_id()));
                }
            }

            // 추가된 부서장 여부 체크 로직
            DeptDAO deptDao = new DeptDAO();
            boolean isManager = deptDao.isManager(account.getEmpId());

            // 4. 결과를 LoginResultDTO 하나로 포장
            LoginResultDTO result = new LoginResultDTO();
            result.setAccount(account);
            result.setEmpInfo(empInfo);
            result.setManager(isManager);

            return result;
            
        } else {
            // 실패 시 처리 (기존 로직 동일)
            accountDAO.handleLoginFailure(username);
            AccountDTO updated = accountDAO.getAccountByUsername(username);
            int currentAttempts = (updated != null) ? updated.getLoginAttempts() : 1;

            if (currentAttempts >= 5) {
                try {
                    int[] adminIds = accountDAO.getAdminEmpIds();
                    if (adminIds.length > 0) {
                        NotificationUtil.sendAccountLocked(adminIds, account.getUsername());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
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
        } else if ("weak_password".equals(error)) {
            errorMsg = "❌ 비밀번호 정책(영문, 숫자, 특수문자 포함 8자 이상)을 확인해 주세요.";
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