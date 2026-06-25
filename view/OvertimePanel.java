package com.mycompany.oop.view;

import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.OvertimeRequest;
import com.mycompany.oop.service.EmployeeService;
import com.mycompany.oop.service.OvertimeService;
import com.mycompany.oop.service.RoleAccessService;
import com.mycompany.oop.service.WorkflowStageService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OvertimePanel extends JPanel implements RefreshablePanel {

    private final Employee currentUser;
    private final OvertimeService overtimeService;
    private final EmployeeService employeeService;
    private final RoleAccessService roleAccessService;
    private final WorkflowStageService workflowStageService;
    private final Map<Integer, Employee> employeesById;

    private JTextField dateField;
    private JTextField hoursField;
    private JTextField reasonField;
    private JLabel messageLabel;
    private JTable table;

    public OvertimePanel(Employee currentUser) {
        this.currentUser = currentUser;
        this.overtimeService = new OvertimeService();
        this.employeeService = new EmployeeService();
        this.roleAccessService = new RoleAccessService();
        this.workflowStageService = new WorkflowStageService();
        this.employeesById = new HashMap<>();

        setLayout(new BorderLayout());
        setBackground(UITheme.BG);

        add(UITheme.createTitleBar("Overtime Requests"), BorderLayout.NORTH);
        add(createContent(), BorderLayout.CENTER);
    }

    private JPanel createContent() {
        loadEmployees();

        JPanel content = UITheme.createWorkspacePanel(new BorderLayout(0, 16));

        JPanel sections = new JPanel();
        sections.setOpaque(false);
        sections.setLayout(new BoxLayout(sections, BoxLayout.Y_AXIS));

        if (canSubmitOwnRequest()) {
            sections.add(createRequestForm());
            sections.add(Box.createVerticalStrut(14));
        }

        sections.add(createRequestsSection());

        JScrollPane scrollPane = new JScrollPane(sections);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UITheme.BG);
        WorkforceFormToolkit.tuneScrollPane(scrollPane);
        content.add(scrollPane, BorderLayout.CENTER);

        return content;
    }

    private JPanel createRequestForm() {
        JPanel section = WorkforceFormToolkit.createSection(
                "File Overtime Request",
                "Submit overtime for review. Approved overtime is not connected to payroll computation yet."
        );

        JPanel compactCard = WorkforceFormToolkit.createCompactFormCard(720);
        JPanel cardBody = WorkforceFormToolkit.getCompactFormBody(compactCard);
        JPanel form = WorkforceFormToolkit.createTwoColumnFieldGrid();

        dateField = new JTextField();
        WorkforceFormToolkit.styleTextField(dateField);
        WorkforceFormToolkit.applyDateHelp(dateField);
        dateField.setText(LocalDate.now().toString());
        WorkforceFormToolkit.addFieldBlock(form, "Overtime Date", dateField,
                "Date format: yyyy-MM-dd", 0, 0);

        hoursField = new JTextField();
        WorkforceFormToolkit.styleTextField(hoursField);
        WorkforceFormToolkit.addFieldBlock(form, "Overtime Hours", hoursField,
                "Use decimal hours, example: 2 or 2.5", 1, 0);

        reasonField = new JTextField();
        WorkforceFormToolkit.styleTextField(reasonField);
        WorkforceFormToolkit.addFieldBlock(form, "Reason", reasonField,
                "Required business reason for overtime.", 0, 1, 2);

        JButton submitButton = UITheme.createAccentButton("Submit Overtime");
        submitButton.setPreferredSize(new Dimension(150, 34));
        submitButton.addActionListener(e -> submitRequest());

        messageLabel = WorkforceFormToolkit.createInlineMessage();
        WorkforceFormToolkit.setInfo(messageLabel, "Overtime starts with supervisor review, then HR final approval.");

        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(10, 0, 0, 0));
        footer.add(messageLabel, BorderLayout.CENTER);

        JPanel buttons = WorkforceFormToolkit.createButtonRow(FlowLayout.RIGHT);
        buttons.add(submitButton);
        footer.add(buttons, BorderLayout.EAST);

        cardBody.add(form, BorderLayout.CENTER);
        cardBody.add(footer, BorderLayout.SOUTH);
        section.add(compactCard, BorderLayout.CENTER);
        return section;
    }

    private JPanel createRequestsSection() {
        JPanel section = WorkforceFormToolkit.createSection(
                getRequestsTitle(),
                getRequestsSubtitle()
        );

        table = new JTable();
        UITheme.styleTable(table);
        table.setDefaultRenderer(Object.class, new StatusRenderer());
        refreshTable();

        JScrollPane tableScroll = UITheme.createTableScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(0, 176));
        section.add(tableScroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        actions.setOpaque(false);

        JButton refreshButton = UITheme.createButton("Refresh");
        refreshButton.addActionListener(e -> refreshTable());
        actions.add(refreshButton);

        if (roleAccessService.canApproveOvertime(getCurrentRole())) {
            JButton approveButton = UITheme.createAccentButton("Approve");
            JButton rejectButton = UITheme.createCrudDangerButton("Reject");
            approveButton.addActionListener(e -> updateSelectedRequest("APPROVED"));
            rejectButton.addActionListener(e -> updateSelectedRequest("REJECTED"));
            actions.add(approveButton);
            actions.add(rejectButton);
        }

        section.add(actions, BorderLayout.SOUTH);
        return section;
    }

    private void submitRequest() {
        try {
            LocalDate date = LocalDate.parse(dateField.getText().trim());
            double hours = Double.parseDouble(hoursField.getText().trim());
            OvertimeRequest request = overtimeService.fileOwnOvertimeRequest(
                    currentUser, date, hours, reasonField.getText().trim());
            WorkforceFormToolkit.setSuccess(messageLabel,
                    "Overtime request submitted. Stage: " + workflowStageService.overtimeStage(request, currentUser));
            dateField.setText(LocalDate.now().toString());
            hoursField.setText("");
            reasonField.setText("");
            refreshTable();
        } catch (Exception ex) {
            WorkforceFormToolkit.setError(messageLabel, ex.getMessage());
        }
    }

    private void updateSelectedRequest(String status) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an overtime request first.",
                    "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int modelRow = table.convertRowIndexToModel(row);
        int overtimeId = Integer.parseInt(table.getModel().getValueAt(modelRow, 0).toString());
        int stageColumn = table.getColumnModel().getColumnIndex("Stage");
        String currentStatus = String.valueOf(table.getModel().getValueAt(modelRow, stageColumn));

        if (!currentStatus.toLowerCase(Locale.ENGLISH).contains("pending")
                && !currentStatus.toLowerCase(Locale.ENGLISH).contains("awaiting")) {
            JOptionPane.showMessageDialog(this, "This overtime request is already at stage: "
                            + currentStatus + ".",
                    "Already Reviewed", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String remarks = JOptionPane.showInputDialog(this,
                "Remarks for " + status.toLowerCase(Locale.ENGLISH) + " overtime request:",
                "Overtime Review", JOptionPane.PLAIN_MESSAGE);
        if (remarks == null) {
            return;
        }

        try {
            if ("APPROVED".equals(status)) {
                overtimeService.approveOvertimeRequest(overtimeId, currentUser, remarks.trim());
            } else {
                overtimeService.rejectOvertimeRequest(overtimeId, currentUser, remarks.trim());
            }
            refreshTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Review Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshTable() {
        if (table == null) {
            return;
        }

        List<OvertimeRequest> requests = overtimeService.getVisibleRequests(currentUser);
        boolean selfService = roleAccessService.isEmployee(getCurrentRole());
        String[] columns = selfService
                ? new String[]{"ID", "Date", "Hours", "Reason", "Stage", "Owner"}
                : new String[]{"ID", "Employee", "Date", "Hours", "Reason", "Remarks", "Stage", "Owner", "Reviewed By"};

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (OvertimeRequest request : requests) {
            Employee employee = employeesById.get(request.getEmployeeId());
            String stage = workflowStageService.overtimeStage(request, currentUser);
            if (selfService) {
                model.addRow(new Object[]{
                        request.getOvertimeId(),
                        request.getOvertimeDate(),
                        request.getOvertimeHours(),
                        safeText(request.getReason(), "-"),
                        stage,
                        workflowStageService.ownerForStage(stage)
                });
            } else {
                model.addRow(new Object[]{
                        request.getOvertimeId(),
                        employee == null ? "Employee #" + request.getEmployeeId() : fullName(employee),
                        request.getOvertimeDate(),
                        request.getOvertimeHours(),
                        safeText(request.getReason(), "-"),
                        safeText(request.getRemarks(), "-"),
                        stage,
                        workflowStageService.ownerForStage(stage),
                        request.getApprovedBy() == null ? "-" : "Employee #" + request.getApprovedBy()
                });
            }
        }

        if (requests.isEmpty()) {
            model.addRow(selfService
                    ? new Object[]{"-", "-", "-", getEmptyState(), "-", "-"}
                    : new Object[]{"-", getEmptyState(), "-", "-", "-", "-", "-", "-", "-"});
        }

        table.setModel(model);
        table.setAutoResizeMode(selfService ? JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS : JTable.AUTO_RESIZE_OFF);
        if (selfService) {
            UITheme.setColumnWidths(table, 64, 118, 84, 240, 220, 120);
        } else {
            UITheme.setColumnWidths(table, 70, 180, 120, 90, 260, 240, 220, 120, 140);
        }
    }

    @Override
    public void refreshData() {
        loadEmployees();
        refreshTable();
    }

    private void loadEmployees() {
        employeesById.clear();
        for (Employee employee : employeeService.getAllEmployees()) {
            employeesById.put(employee.getEmployeeId(), employee);
        }
    }

    private boolean canSubmitOwnRequest() {
        return currentUser != null;
    }

    private String getRequestsTitle() {
        if (roleAccessService.canApproveOvertime(getCurrentRole())) {
            return "Overtime Review";
        }
        return "My Overtime Status";
    }

    private String getRequestsSubtitle() {
        String role = getCurrentRole();
        if (roleAccessService.canApproveOvertime(role)) {
            return "HR final approval queue for overtime requests.";
        }
        if (roleAccessService.isSupervisor(role)) {
            return "Read-only visibility for assigned team overtime requests.";
        }
        if (roleAccessService.isFinance(role)) {
            return "Approved overtime visibility for future payroll readiness only.";
        }
        return "Track your submitted overtime request status.";
    }

    private String getEmptyState() {
        String role = getCurrentRole();
        if (roleAccessService.canApproveOvertime(role)) {
            return "No overtime requests require review.";
        }
        if (roleAccessService.isFinance(role)) {
            return "No approved overtime requests found.";
        }
        if (roleAccessService.isSupervisor(role)) {
            return "No team overtime requests found.";
        }
        return "No overtime requests submitted yet.";
    }

    private String getCurrentRole() {
        return currentUser == null ? "" : currentUser.getRole();
    }

    private String fullName(Employee employee) {
        return safeText(employee.getFirstName(), "") + " " + safeText(employee.getLastName(), "");
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (isSelected) {
                return component;
            }

            int statusColumn = table.getColumnModel().getColumnIndex("Stage");
            String status = String.valueOf(table.getValueAt(row, statusColumn));
            if (status.toUpperCase(Locale.ENGLISH).contains("PENDING")
                    || status.toUpperCase(Locale.ENGLISH).contains("AWAITING")) {
                component.setBackground(new Color(255, 251, 235));
            } else if (status.toUpperCase(Locale.ENGLISH).contains("APPROVED")) {
                component.setBackground(new Color(240, 253, 244));
            } else if (status.toUpperCase(Locale.ENGLISH).contains("REJECTED")) {
                component.setBackground(new Color(254, 242, 242));
            } else {
                component.setBackground(row % 2 == 0 ? Color.WHITE : UITheme.TABLE_ALT_ROW);
            }
            component.setForeground(UITheme.TEXT_PRIMARY);
            return component;
        }
    }
}
