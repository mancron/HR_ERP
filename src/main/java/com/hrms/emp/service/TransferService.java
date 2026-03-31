package com.hrms.emp.service;

import java.sql.Connection;
import java.sql.SQLException;
import com.hrms.common.db.DatabaseConnection;
import com.hrms.emp.dao.EmpDAO;
import com.hrms.emp.dao.TransferDAO;
import com.hrms.emp.dto.TransferDTO; // 발령 데이터를 담는 DTO (가정)

public class TransferService {

    // 1. 발령 전용 DAO인 TransferDAO를 사용하도록 수정
    private TransferDAO transferDao = new TransferDAO();

    /**
     * 인사발령 실행 (트랜잭션 처리)
     */
    public boolean executeTransfer(TransferDTO dto) {
        Connection con = null;
        boolean isSuccess = false;

        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false); // [설계서: 트랜잭션 시작]

            // 2. TransferDAO에 정의한 메서드 호출 (변수명 transferDao 확인!)
            // 주의: TransferDAO 클래스 안에 아래 두 메서드 이름이 똑같이 있어야 합니다.
            int updateResult = transferDao.updateEmployeePosition(con, dto);
            int insertResult = transferDao.insertPersonnelHistory(con, dto);

            // 3. 두 작업이 모두 성공(1행 이상 영향)했는지 확인
            if (updateResult > 0 && insertResult > 0) {
                con.commit(); // [설계서: 트랜잭션 커밋]
                isSuccess = true;
                System.out.println("[TransferService] 인사발령 성공: 커밋 완료");
            } else {
                con.rollback(); // 하나라도 실패하면 롤백
                System.err.println("[TransferService] 인사발령 실패: 결과 부족 (Update:" + updateResult + ", Insert:" + insertResult + ")");
            }

        } catch (Exception e) {
            if (con != null) {
                try {
                    con.rollback();
                    System.err.println("[TransferService] 예외 발생으로 인한 롤백 수행: " + e.getMessage());
                } catch (SQLException se) {
                    se.printStackTrace();
                }
            }
            e.printStackTrace();
        } finally {
            closeConnection(con);
        }

        return isSuccess;
    }

    private void closeConnection(Connection con) {
        if (con != null) {
            try {
                if (!con.isClosed()) {
                    con.setAutoCommit(true); // 커넥션 풀 반납 전 상태 복구
                    con.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}