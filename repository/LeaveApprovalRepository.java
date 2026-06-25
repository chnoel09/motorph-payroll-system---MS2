package com.mycompany.oop.repository;

import java.util.List;

import com.mycompany.oop.model.LeaveApproval;

// Contract prepared for future normalized leave approval workflow.
public interface LeaveApprovalRepository {

    List<LeaveApproval> findByLeaveId(int leaveId);

    void addLeaveApproval(LeaveApproval approval);
}
