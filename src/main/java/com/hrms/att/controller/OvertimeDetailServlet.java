package com.hrms.att.controller;

import com.google.gson.Gson;
import com.hrms.att.dto.OvertimeDTO;
import com.hrms.att.dto.RequestDTO;
import com.hrms.att.service.OvertimeService;
import com.hrms.emp.dao.EmpDAO;
import com.hrms.emp.dto.EmpDTO;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebServlet("/att/overtime/detail")
public class OvertimeDetailServlet extends HttpServlet {

    private OvertimeService service = new OvertimeService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        int id = Integer.parseInt(req.getParameter("id"));

        OvertimeDTO dto = service.getOvertimeDetail(id);

        RequestDTO r = new RequestDTO();

        r.setId(dto.getOtId());
        r.setDate(
                dto.getOtDate() + " " +
                dto.getStartTime().toString().substring(0,5) + " ~ " +
                dto.getEndTime().toString().substring(0,5)
            );
        r.setType("초과근무");
        r.setStatus(dto.getStatus());
        r.setReason(dto.getReason());

        String empInfo = dto.getEmpName();

        if (dto.getDeptName() != null && dto.getPosition() != null) {
            empInfo += " (" + dto.getDeptName() + " / " + dto.getPosition() + ")";
        }

        r.setEmpName(empInfo);
        String approverInfo;

     // 🔥 대기 상태일 경우
     if ("대기".equals(dto.getStatus())) {

         // 👉 팀장 찾기
         OvertimeService service = new OvertimeService();
         int managerId = service.findApprover(dto.getEmpId());

         EmpDAO empDAO = new EmpDAO();
         EmpDTO manager = empDAO.getEmployeeById(managerId);

         String managerInfo = manager.getEmp_name()
                 + " (" + manager.getDept_name() + " / " + manager.getPosition_name() + ")";

         approverInfo = managerInfo + " 또는 인사팀";

     } else {

         // 👉 실제 승인자
         approverInfo = dto.getApproverName();

         if (dto.getApproverDept() != null && dto.getApproverPosition() != null) {
             approverInfo += " (" + dto.getApproverDept() + " / " + dto.getApproverPosition() + ")";
         }
     }

     r.setApproverName(approverInfo != null ? approverInfo : "-");
        r.setApplyDate(dto.getCreatedAt() != null ? dto.getCreatedAt().toString() : "-");
        r.setApproveDate(dto.getApprovedAt() != null ? dto.getApprovedAt().toString() : "-");
        r.setRejectReason(dto.getReject_reason() != null ? dto.getReject_reason().toString() : "-");

        resp.setContentType("application/json;charset=UTF-8");

        new Gson().toJson(r, resp.getWriter());
    }
}