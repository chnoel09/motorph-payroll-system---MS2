package com.mycompany.oop.service;

import java.util.ArrayList;
import java.util.List;

import com.mycompany.oop.model.LeaveApproval;
import com.mycompany.oop.repository.LeaveApprovalDatabaseRepository;
import com.mycompany.oop.repository.LeaveApprovalRepository;

// Prepared for the future normalized leave approval workflow migration.
// This service is intentionally not wired into the current leave flow yet.
public class LeaveApprovalService {

    private LeaveApprovalRepository leaveApprovalRepository;

    public LeaveApprovalService() {
        this.leaveApprovalRepository = new LeaveApprovalDatabaseRepository();
    }

    public LeaveApprovalService(LeaveApprovalRepository leaveApprovalRepository) {
        this.leaveApprovalRepository = leaveApprovalRepository;
    }

    public void recordApprovalAction(LeaveApproval approval) {
        if (leaveApprovalRepository != null && approval != null) {
            leaveApprovalRepository.addLeaveApproval(approval);
        }
    }

    public List<LeaveApproval> getApprovalHistory(int leaveId) {
        if (leaveApprovalRepository == null) {
            return new ArrayList<>();
        }

        return leaveApprovalRepository.findByLeaveId(leaveId);
    }
}
