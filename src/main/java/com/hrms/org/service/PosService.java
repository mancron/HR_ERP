package com.hrms.org.service;

import java.util.Arrays;
import com.hrms.org.dao.PosDAO;
import com.hrms.org.dto.PosDTO;

public class PosService {
    private PosDAO posDao = new PosDAO();

    /**
     * 직급 정보 상세 조회 (보안 및 예외처리 포함 가능)
     */
    public PosDTO getPositionDetail(int id) {
        return posDao.getPositionById(id);
    }

    /**
     * 직급 수정 비즈니스 로직 프로세스
     * @return status 문자열 (success, no_change, has_emp, fail, not_found)
     */
    public String updatePositionProcess(PosDTO newDto, Integer actorId) {
        // 1. 기존 데이터 조회 (비교 및 검증용)
        PosDTO oldDto = posDao.getPositionById(newDto.getPosition_id());
        if (oldDto == null) {
            return "not_found";
        }

        // 2. 변경 사항 검사 (Medium 9)
        String[] columns = {"base_salary", "meal_allowance", "transport_allowance", "position_allowance", "is_active"};
        String[] oldValues = {
            oldDto.getBase_salary().toString(),
            String.valueOf(oldDto.getMeal_allowance()),
            String.valueOf(oldDto.getTransport_allowance()),
            String.valueOf(oldDto.getPosition_allowance()),
            String.valueOf(oldDto.getIs_active())
        };
        String[] newValues = {
            newDto.getBase_salary().toString(),
            String.valueOf(newDto.getMeal_allowance()),
            String.valueOf(newDto.getTransport_allowance()),
            String.valueOf(newDto.getPosition_allowance()),
            String.valueOf(newDto.getIs_active())
        };

        if (Arrays.equals(oldValues, newValues)) {
            return "no_change";
        }

        // 3. 비활성화 시 인원 체크 (High 5 1차 방어)
        if (newDto.getIs_active() == 0) {
            int empCount = posDao.getEmployeeCountByPosition(newDto.getPosition_id());
            if (empCount > 0) {
                return "has_emp";
            }
        }

        // 4. DB 업데이트 실행 (감사로그 포함 트랜잭션)
        boolean result = posDao.updatePositionWithLog(newDto, actorId, columns, oldValues, newValues);
        
        return result ? "success" : "fail";
    }
}