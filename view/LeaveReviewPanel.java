/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.oop.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.Component;
import java.util.List;
import com.mycompany.oop.model.Employee;
import com.mycompany.oop.service.LeaveService;
import com.mycompany.oop.service.RoleAccessService;
import com.mycompany.oop.model.Leave;

public class LeaveReviewPanel extends JPanel {

    private LeaveService service;
    private RoleAccessService roleAccessService;
    private Employee currentUser;
    private JTable table;
    private JLabel actionRequiredLabel;

    public LeaveReviewPanel() {
        this(null);
    }

    public LeaveReviewPanel(Employee currentUser) {

        this.currentUser = currentUser;
        service = new LeaveService();
        roleAccessService = new RoleAccessService();

        setLayout(new BorderLayout());
        setBackground(UITheme.BG);

        add(createHeaderPanel(), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(UITheme.BG);
        content.setBorder(new EmptyBorder(16, 20, 16, 20));

        content.add(createTablePanel(), BorderLayout.CENTER);
        content.add(createButtonPanel(), BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel header = UITheme.createTitleBar("Leave Approval Center");

        actionRequiredLabel = new JLabel();
        actionRequiredLabel.setFont(UITheme.FONT_BODY_BOLD);
        actionRequiredLabel.setForeground(UITheme.ACCENT);
        header.add(actionRequiredLabel, BorderLayout.EAST);

        return header;
    }

    private JScrollPane createTablePanel() {

        table = new JTable();
        UITheme.styleTable(table);
        table.setDefaultRenderer(Object.class, new LeaveStatusRenderer());

        refreshTable();

        return UITheme.createTableScrollPane(table);
    }

    private void refreshTable() {

        List<Leave> leaves = service.getAllLeaves();

        String[] cols = {
                "Leave ID",
                "Employee ID",
                "Type",
                "Start Date",
                "End Date",
                "Reason",
                "Status"
        };

        Object[][] data = new Object[leaves.size()][7];

        for (int i = 0; i < leaves.size(); i++) {
            Leave l = leaves.get(i);
            data[i][0] = l.getLeaveId();
            data[i][1] = l.getEmployeeId();
            data[i][2] = l.getLeaveType();
            data[i][3] = l.getStartDate();
            data[i][4] = l.getEndDate();
            data[i][5] = l.getReason();
            data[i][6] = l.getStatus();
        }

        DefaultTableModel model = new DefaultTableModel(data, cols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table.setModel(model);
        updateActionRequiredLabel(leaves);
    }

    private void updateActionRequiredLabel(List<Leave> leaves) {
        int pendingLeaveCount = 0;

        for (Leave leave : leaves) {
            if (leave.getStatus() != null && leave.getStatus().equalsIgnoreCase("PENDING")) {
                pendingLeaveCount++;
            }
        }

        if (pendingLeaveCount > 0) {
            actionRequiredLabel.setForeground(UITheme.ACCENT);
            actionRequiredLabel.setText("Action Required: " + pendingLeaveCount + " pending");
        } else {
            actionRequiredLabel.setText("No pending approvals");
            actionRequiredLabel.setForeground(UITheme.TEXT_SECONDARY);
        }
    }

    private JPanel createButtonPanel() {

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        panel.setBackground(UITheme.BG);

        JButton approveBtn = UITheme.createAccentButton("Approve");
        JButton rejectBtn = UITheme.createCrudDangerButton("Reject");

        approveBtn.setPreferredSize(new Dimension(110, 34));
        rejectBtn.setPreferredSize(new Dimension(110, 34));

        approveBtn.addActionListener(e -> updateStatus("APPROVED"));
        rejectBtn.addActionListener(e -> updateStatus("REJECTED"));

        panel.add(approveBtn);
        panel.add(rejectBtn);

        return panel;
    }

    private void updateStatus(String status) {

        if (!roleAccessService.canApproveLeave(getCurrentRole())) {
            showAccessDeniedMessage("Only HR users can approve or reject leave requests.");
            return;
        }

        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a leave request first.",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int leaveId = Integer.parseInt(table.getValueAt(row, 0).toString());
        String currentStatus = table.getValueAt(row, 6).toString();

        if (!"PENDING".equalsIgnoreCase(currentStatus)) {
            JOptionPane.showMessageDialog(this,
                    "This leave has already been " + currentStatus.toLowerCase() + ".",
                    "Already Processed",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to " + status.toLowerCase() + " this leave request?",
                "Confirm Action",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        if ("APPROVED".equals(status)) {
            service.approveLeave(leaveId);
        } else {
            service.rejectLeave(leaveId);
        }

        JOptionPane.showMessageDialog(this,
                "Leave request " + status.toLowerCase() + " successfully.");

        refreshTable();
    }

    private String getCurrentRole() {
        return currentUser == null ? "" : currentUser.getRole();
    }

    private void showAccessDeniedMessage(String message) {
        JOptionPane.showMessageDialog(this,
                message,
                "Access Restricted",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private static class LeaveStatusRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (isSelected) {
                return component;
            }

            String status = String.valueOf(table.getValueAt(row, 6));

            if ("PENDING".equalsIgnoreCase(status)) {
                component.setBackground(new Color(255, 251, 235));
                component.setForeground(UITheme.TEXT_PRIMARY);
            } else if ("APPROVED".equalsIgnoreCase(status)) {
                component.setBackground(new Color(240, 253, 244));
                component.setForeground(new Color(22, 101, 52));
            } else if ("REJECTED".equalsIgnoreCase(status)) {
                component.setBackground(new Color(254, 242, 242));
                component.setForeground(UITheme.DANGER);
            } else {
                component.setBackground(row % 2 == 0 ? Color.WHITE : UITheme.TABLE_ALT_ROW);
                component.setForeground(UITheme.TEXT_PRIMARY);
            }

            return component;
        }
    }
}
