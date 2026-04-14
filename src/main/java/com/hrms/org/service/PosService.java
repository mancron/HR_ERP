package com.hrms.org.service;

import java.util.Arrays;
import com.hrms.org.dao.PosDAO;
import com.hrms.org.dto.PosDTO;

public class PosService {
    private PosDAO posDao = new PosDAO();

    /**
     * 직급 정보 상세 조회
     */
    public PosDTO getPositionDetail(int id) {
        return posDao.getPositionById(id);
    }

    /**
     * 직급 수정 비즈니스 로직 프로세스
     * @return status (success, no_change, has_emp, fail, not_found)
     */
    public String updatePositionProcess(PosDTO newDto, Integer actorId) {
        // 1. 기존 데이터 조회
        PosDTO oldDto = posDao.getPositionById(newDto.getPosition_id());
        if (oldDto == null) {
            return "not_found";
        }

        // 2. 변경 사항 검사 (BigDecimal 정밀도 통일 및 NPE 방어)
        String[] columns = {"base_salary", "meal_allowance", "transport_allowance", "position_allowance", "is_active"};
        
        // stripTrailingZeros().toPlainString()으로 9000.00과 9000을 동일하게 취급하도록 포맷팅
        String[] oldValues = {
            oldDto.getBase_salary() != null ? oldDto.getBase_salary().stripTrailingZeros().toPlainString() : "0",
            String.valueOf(oldDto.getMeal_allowance()),
            String.valueOf(oldDto.getTransport_allowance()),
            String.valueOf(oldDto.getPosition_allowance()),
            String.valueOf(oldDto.getIs_active())
        };

        String[] newValues = {
            newDto.getBase_salary() != null ? newDto.getBase_salary().stripTrailingZeros().toPlainString() : "0",
            String.valueOf(newDto.getMeal_allowance()),
            String.valueOf(newDto.getTransport_allowance()),
            String.valueOf(newDto.getPosition_allowance()),
            String.valueOf(newDto.getIs_active())
        };

        // 데이터 변화가 전혀 없다면 불필요한 DB 업데이트 및 로그 기록 방지
        if (Arrays.equals(oldValues, newValues)) {
            return "no_change";
        }

        // 3. 비활성화 시 인원 체크 (데이터 무결성 방어)
        if (newDto.getIs_active() == 0) {
            int empCount = posDao.getEmployeeCountByPosition(newDto.getPosition_id());
            if (empCount > 0) {
                return "has_emp";
            }
        }

        // 4. DB 업데이트 및 감사 로그 기록 (DAO에서 트랜잭션 처리)
        boolean result = posDao.updatePositionWithLog(newDto, actorId, columns, oldValues, newValues);        
        return result ? "success" : "fail";
    }
}