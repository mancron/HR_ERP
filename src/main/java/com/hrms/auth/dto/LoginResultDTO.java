package com.hrms.auth.dto;

import com.hrms.emp.dto.EmpDTO;

public class LoginResultDTO {
    private AccountDTO account;
    private EmpDTO empInfo;
    private boolean isManager;

    public AccountDTO getAccount() {
        return account;
    }

    public void setAccount(AccountDTO account) {
        this.account = account;
    }

    public EmpDTO getEmpInfo() {
        return empInfo;
    }

    public void setEmpInfo(EmpDTO empInfo) {
        this.empInfo = empInfo;
    }

    public boolean isManager() {
        return isManager;
    }

    public void setManager(boolean manager) {
        this.isManager = manager;
    }
}