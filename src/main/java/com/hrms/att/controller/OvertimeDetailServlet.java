package com.hrms.att.controller;

import com.google.gson.Gson;
import com.hrms.att.dto.OvertimeDTO;
import com.hrms.att.dto.RequestDTO;
import com.hrms.att.service.OvertimeService;

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
        String approverInfo = dto.getApproverName();

        if (dto.getApproverDept() != null && dto.getApproverPosition() != null) {
            approverInfo += " (" + dto.getApproverDept() + " / " + dto.getApproverPosition() + ")";
        }

        r.setApproverName(approverInfo);
        r.setApplyDate(dto.getCreatedAt() != null ? dto.getCreatedAt().toString() : "-");
        r.setApproveDate(dto.getApprovedAt() != null ? dto.getApprovedAt().toString() : "-");
        r.setRejectReason(dto.getReject_reason() != null ? dto.getReject_reason().toString() : "-");

        resp.setContentType("application/json;charset=UTF-8");

        new Gson().toJson(r, resp.getWriter());
    }
}