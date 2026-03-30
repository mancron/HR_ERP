package com.hrms.sys.service;

import com.hrms.sys.dao.AuditLogDAO;
import com.hrms.sys.dto.AuditLogDTO;

import java.util.List;

public class AuditLogService {
    
    private final AuditLogDAO auditLogDAO;

    public AuditLogService() {
        this.auditLogDAO = new AuditLogDAO();
    }

    public List<AuditLogDTO> getAuditLogs(String targetTable, String startDate, String endDate) {
        // DB 명세서상 민감 정보는 INSERT 시점에 이미 마스킹 처리되어 저장됨.
        // 조회 로직에서는 DAO를 호출하여 데이터를 그대로 반환하는 역할만 수행한다.
        return auditLogDAO.selectAuditLogs(targetTable, startDate, endDate);
    }
}