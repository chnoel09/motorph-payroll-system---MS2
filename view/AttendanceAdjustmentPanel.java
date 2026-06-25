package com.mycompany.oop.view;

import com.mycompany.oop.model.AttendanceAdjustment;
import com.mycompany.oop.model.Employee;
import com.mycompany.oop.service.AttendanceAdjustmentService;
import com.mycompany.oop.service.EmployeeService;
import com.mycompany.oop.service.RoleAccessService;
import com.mycompany.oop.service.WorkflowStageService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AttendanceAdjustmentPanel extends JPanel implements RefreshablePanel {

    private final Employee currentUser;
    private final AttendanceAdjustmentService adjustmentService;
    private final EmployeeService employeeService;
    private final RoleAccessService roleAccessService;
    private final WorkflowStageService workflowStageService;
    private final Map<Integer, Employee> employeesById = new HashMap<>();

    private JTextField dateField;
    private JComboBox<String> typeBox;
    private JTextField remarksField;
    private JLabel messageLabel;
    private JTable table;

    public AttendanceAdjustmentPanel(Employee currentUser) {
        this.currentUser = currentUser;
        this.adjustmentService = new AttendanceAdjustmentService();
        this.employeeService = new EmployeeService();
        this.roleAccessService = new RoleAccessService();
        this.workflowStageService = new WorkflowStageService();

        setLayout(new BorderLayout());
        setBackground(UITheme.BG);
        add(UITheme.createTitleBar("Attendance Adjustments"), BorderLayout.NORTH);
        add(createContent(), BorderLayout.CENTER);
    }

    private JPanel createContent() {
        loadEmployees();
        JPanel content = UITheme.createWorkspacePanel(new BorderLayout(0, 16));

        JPanel sections = new JPanel();
        sections.setOpaque(false);
        sections.setLayout(new BoxLayout(sections, BoxLayout.Y_AXIS));

        if (canRequestOwnAdjustment()) {
            sections.add(createRequestSection());
            sections.add(Box.createVerticalStrut(14));
        }
        sections.add(createHistorySection());

        JScrollPane scroll = new JScrollPane(sections);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(UITheme.BG);
        WorkforceFormToolkit.tuneScrollPane(scroll);
        content.add(scroll, BorderLayout.CENTER);

        return content;
    }

    private JPanel createRequestSection() {
        JPanel section = WorkforceFormToolkit.createSection(
                "Request Attendance Correction",
                "Request review for missing or incorrect time records. This does not automatically change attendance."
        );

        JPanel compactCard = WorkforceFormToolkit.createCompactFormCard(720);
        JPanel cardBody = WorkforceFormToolkit.getCompactFormBody(compactCard);
        JPanel form = WorkforceFormToolkit.createTwoColumnFieldGrid();

        dateField = new JTextField();
        WorkforceFormToolkit.styleTextField(dateField);
        WorkforceFormToolkit.applyDateHelp(dateField);
        dateField.setText(LocalDate.now().toString());
        WorkforceFormToolkit.addFieldBlock(form, "Attendance Date", dateField,
                "Date format: yyyy-MM-dd", 0, 0);

        typeBox = new JComboBox<>(new String[]{
                "Missing Time In", "Missing Time Out", "Incorrect Time", "Incomplete Attendance", "Other"
        });
        WorkforceFormToolkit.styleComboBox(typeBox);
        WorkforceFormToolkit.addFieldBlock(form, "Adjustment Type", typeBox,
                "Select the timekeeping concern.", 1, 0);

        remarksField = new JTextField();
        WorkforceFormToolkit.styleTextField(remarksField);
        WorkforceFormToolkit.addFieldBlock(form, "Remarks", remarksField,
                "Describe the correction needed.", 0, 1, 2);

        messageLabel = WorkforceFormToolkit.createInlineMessage();
        WorkforceFormToolkit.setInfo(messageLabel, "Corrections start with supervisor review, then HR correction approval.");

        JButton submitButton = UITheme.createAccentButton("Submit Request");
        submitButton.setPreferredSize(new Dimension(140, 34));
        submitButton.addActionListener(e -> submitRequest());

        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setOpaque(false);
        footer.add(messageLabel, BorderLayout.CENTER);
        JPanel buttons = WorkforceFormToolkit.createButtonRow(FlowLayout.RIGHT);
        buttons.add(submitButton);
        footer.add(buttons, BorderLayout.EAST);

        cardBody.add(form, BorderLayout.CENTER);
        cardBody.add(footer, BorderLayout.SOUTH);
        section.add(compactCard, BorderLayout.CENTER);
        return section;
    }

    private JPanel createHistorySection() {
        JPanel section = WorkforceFormToolkit.createSection(getSectionTitle(), getSectionSubtitle());

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

        if (roleAccessService.canProcessAttendanceAdjustments(getCurrentRole())) {
            JButton reviewedButton = UITheme.createButton("Mark Reviewed");
            JButton correctedButton = UITheme.createAccentButton("Mark Corrected");
            reviewedButton.addActionListener(e -> processSelected(false));
            correctedButton.addActionListener(e -> processSelected(true));
            actions.add(reviewedButton);
            actions.add(correctedButton);
        }

        section.add(actions, BorderLayout.SOUTH);
        return section;
    }

    private void submitRequest() {
        try {
            adjustmentService.requestOwnAdjustment(
                    currentUser,
                    LocalDate.parse(dateField.getText().trim()),
                    String.valueOf(typeBox.getSelectedItem()),
                    remarksField.getText().trim()
            );
            WorkforceFormToolkit.setSuccess(messageLabel, "Attendance correction request submitted. Stage: Pending Supervisor Review.");
            dateField.setText(LocalDate.now().toString());
            typeBox.setSelectedIndex(0);
            remarksField.setText("");
            refreshTable();
        } catch (Exception ex) {
            WorkforceFormToolkit.setError(messageLabel, ex.getMessage());
        }
    }

    private void processSelected(boolean corrected) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an adjustment request first.",
                    "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int modelRow = table.convertRowIndexToModel(row);
        Object idValue = table.getModel().getValueAt(modelRow, 0);
        if ("-".equals(String.valueOf(idValue))) {
            return;
        }

        int adjustmentId = Integer.parseInt(idValue.toString());
        try {
            if (corrected) {
                adjustmentService.markCorrected(adjustmentId, currentUser);
            } else {
                adjustmentService.markReviewed(adjustmentId, currentUser);
            }
            refreshTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Adjustment Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshTable() {
        List<AttendanceAdjustment> adjustments = adjustmentService.getVisibleRequests(currentUser);
        boolean selfService = roleAccessService.isEmployee(getCurrentRole());
        String[] columns = selfService
                ? new String[]{"ID", "Date", "Type", "Remarks", "Stage", "Owner"}
                : new String[]{"ID", "Employee", "Date", "Type", "Remarks", "Stage", "Owner", "Processed By"};

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (AttendanceAdjustment adjustment : adjustments) {
            Employee employee = employeesById.get(adjustment.getEmployeeId());
            String stage = workflowStageService.attendanceAdjustmentStage(adjustment, currentUser);
            if (selfService) {
                model.addRow(new Object[]{
                        adjustment.getAdjustmentId(),
                        adjustment.getAttendanceDate(),
                        safeText(adjustment.getAdjustmentType(), "-"),
                        safeText(adjustment.getRemarks(), "-"),
                        stage,
                        workflowStageService.ownerForStage(stage)
                });
            } else {
                model.addRow(new Object[]{
                        adjustment.getAdjustmentId(),
                        employee == null ? "Employee #" + adjustment.getEmployeeId() : fullName(employee),
                        adjustment.getAttendanceDate(),
                        safeText(adjustment.getAdjustmentType(), "-"),
                        safeText(adjustment.getRemarks(), "-"),
                        stage,
                        workflowStageService.ownerForStage(stage),
                        adjustment.getAdjustedBy() == null ? "-" : "Employee #" + adjustment.getAdjustedBy()
                });
            }
        }

        if (adjustments.isEmpty()) {
            model.addRow(selfService
                    ? new Object[]{"-", "-", "-", getEmptyState(), "-", "-"}
                    : new Object[]{"-", getEmptyState(), "-", "-", "-", "-", "-", "-"});
        }

        table.setModel(model);
        table.setAutoResizeMode(selfService ? JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS : JTable.AUTO_RESIZE_OFF);
        if (selfService) {
            UITheme.setColumnWidths(table, 64, 118, 150, 260, 230, 120);
        } else {
            UITheme.setColumnWidths(table, 70, 180, 120, 160, 300, 230, 120, 140);
        }
    }

    @Override
    public void refreshData() {
        loadEmployees();
        refreshTable();
    }

    private void loadEmployees() {
        for (Employee employee : employeeService.getAllEmployees()) {
            employeesById.put(employee.getEmployeeId(), employee);
        }
    }

    private boolean canRequestOwnAdjustment() {
        String role = getCurrentRole();
        return roleAccessService.isEmployee(role) || roleAccessService.isSupervisor(role);
    }

    private String getSectionTitle() {
        return roleAccessService.canProcessAttendanceAdjustments(getCurrentRole())
                ? "Adjustment Review"
                : "Adjustment History";
    }

    private String getSectionSubtitle() {
        String role = getCurrentRole();
        if (roleAccessService.canProcessAttendanceAdjustments(role)) {
            return "Review attendance correction requests and mark records as corrected when handled.";
        }
        if (roleAccessService.isSupervisor(role)) {
            return "Read-only visibility for assigned team attendance correction requests.";
        }
        return "Track your submitted attendance correction requests.";
    }

    private String getEmptyState() {
        String role = getCurrentRole();
        if (roleAccessService.canProcessAttendanceAdjustments(role)) {
            return "No attendance adjustment requests require review.";
        }
        if (roleAccessService.isSupervisor(role)) {
            return "No team attendance adjustment requests found.";
        }
        return "No attendance adjustment requests submitted yet.";
    }

    private String getCurrentRole() {
        return currentUser == null ? "" : currentUser.getRole();
    }

    private String fullName(Employee employee) {
        return (safeText(employee.getFirstName(), "") + " " + safeText(employee.getLastName(), "")).trim();
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
            String status = String.valueOf(table.getValueAt(row, statusColumn)).toUpperCase(Locale.ENGLISH);
            if (status.contains("PENDING") || status.contains("AWAITING")) {
                component.setBackground(new Color(255, 251, 235));
            } else if (status.contains("CORRECTED")) {
                component.setBackground(new Color(240, 253, 244));
            } else {
                component.setBackground(row % 2 == 0 ? Color.WHITE : UITheme.TABLE_ALT_ROW);
            }
            component.setForeground(UITheme.TEXT_PRIMARY);
            return component;
        }
    }
}
