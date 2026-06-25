/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.oop.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;

import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.Leave;
import com.mycompany.oop.service.LeaveService;
import com.mycompany.oop.service.WorkflowStageService;
import com.toedter.calendar.JDateChooser;

public class LeavePanel extends JPanel implements RefreshablePanel {

    private LeaveService service;
    private WorkflowStageService workflowStageService;
    private JTable table;
    private Employee employee;
    private JLabel formMessageLabel;
    private JComboBox<String> typeBox;
    private JDateChooser startChooser;
    private JDateChooser endChooser;
    private JTextField reasonField;

    public LeavePanel(Employee employee) {

        this.employee = employee;
        this.service = new LeaveService();
        this.workflowStageService = new WorkflowStageService();

        setLayout(new BorderLayout());
        setBackground(UITheme.BG);

        add(UITheme.createTitleBar("File Leave Request"), BorderLayout.NORTH);

        JPanel content = UITheme.createWorkspacePanel(new BorderLayout(0, 16));

        content.add(createFormPanel(), BorderLayout.NORTH);
        content.add(createTablePanel(), BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);
    }

    private JPanel createFormPanel() {

        JPanel formWrapper = WorkforceFormToolkit.createSection(
                "Leave Details",
                "File a request with clear dates, type, and reason."
        );

        JPanel compactCard = WorkforceFormToolkit.createCompactFormCard(720);
        JPanel cardBody = WorkforceFormToolkit.getCompactFormBody(compactCard);
        JPanel formPanel = WorkforceFormToolkit.createTwoColumnFieldGrid();

        typeBox = new JComboBox<>(new String[]{"Vacation", "Sick", "Emergency"});
        WorkforceFormToolkit.styleComboBox(typeBox);
        WorkforceFormToolkit.addFieldBlock(formPanel, "Leave Type", typeBox,
                "Required leave category.", 0, 0);

        startChooser = new JDateChooser();
        startChooser.setDateFormatString("yyyy-MM-dd");
        startChooser.setDate(new Date());
        styleDateChooser(startChooser);
        WorkforceFormToolkit.addFieldBlock(formPanel, "Start Date", startChooser,
                "Date format: yyyy-MM-dd", 1, 0);

        endChooser = new JDateChooser();
        endChooser.setDateFormatString("yyyy-MM-dd");
        endChooser.setDate(new Date());
        styleDateChooser(endChooser);
        WorkforceFormToolkit.addFieldBlock(formPanel, "End Date", endChooser,
                "Date format: yyyy-MM-dd", 0, 1);

        reasonField = new JTextField();
        WorkforceFormToolkit.styleTextField(reasonField);
        WorkforceFormToolkit.addFieldBlock(formPanel, "Reason", reasonField,
                "Brief business reason for this leave request.", 0, 2, 2);

        JButton submitBtn = UITheme.createAccentButton("Submit Leave");
        submitBtn.setFocusable(false);
        submitBtn.setPreferredSize(new Dimension(132, 34));

        formMessageLabel = WorkforceFormToolkit.createInlineMessage();
        WorkforceFormToolkit.setInfo(formMessageLabel, "Submitted leave starts with supervisor review, then HR final approval.");

        JPanel statusRow = new JPanel(new BorderLayout(12, 0));
        statusRow.setOpaque(false);
        statusRow.setBorder(new EmptyBorder(10, 0, 0, 0));
        statusRow.add(formMessageLabel, BorderLayout.CENTER);

        JPanel buttonRow = WorkforceFormToolkit.createButtonRow(FlowLayout.RIGHT);
        buttonRow.add(submitBtn);
        statusRow.add(buttonRow, BorderLayout.EAST);

        cardBody.add(formPanel, BorderLayout.CENTER);
        cardBody.add(statusRow, BorderLayout.SOUTH);
        formWrapper.add(compactCard, BorderLayout.CENTER);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        submitBtn.addActionListener(e -> {
            try {
                if (startChooser.getDate() == null || endChooser.getDate() == null) {
                    WorkforceFormToolkit.setError(formMessageLabel, "Please select both start and end dates.");
                    return;
                }

                String startDate = sdf.format(startChooser.getDate());
                String endDate = sdf.format(endChooser.getDate());

                Leave leave = service.fileLeave(
                        employee.getEmployeeId(),
                        typeBox.getSelectedItem().toString(),
                        startDate,
                        endDate,
                        reasonField.getText().trim()
                );

                WorkforceFormToolkit.setSuccess(formMessageLabel,
                        "Leave filed successfully. Stage: " + workflowStageService.leaveStage(leave, employee));

                refreshTable();

                startChooser.setDate(new Date());
                endChooser.setDate(new Date());
                reasonField.setText("");
                typeBox.setSelectedIndex(0);

            } catch (Exception ex) {
                WorkforceFormToolkit.setError(formMessageLabel, ex.getMessage());
            }
        });

        JPanel outerWrapper = new JPanel(new BorderLayout());
        outerWrapper.setBackground(UITheme.BG);
        outerWrapper.setBorder(new EmptyBorder(0, 0, 16, 0));
        outerWrapper.add(formWrapper, BorderLayout.CENTER);

        return outerWrapper;
    }

    private JScrollPane createTablePanel() {

        table = new JTable();
        UITheme.styleTable(table);
        table.setDefaultRenderer(Object.class, new LeaveStatusRenderer());

        refreshTable();

        JScrollPane scrollPane = UITheme.createTableScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(0, 168));
        return scrollPane;
    }

    private void refreshTable() {

        List<Leave> leaves =
                service.getLeavesByEmployee(employee.getEmployeeId());

        String[] columns = {
                "Leave ID", "Type", "Start", "End", "Reason", "Stage", "Owner"
        };

        Object[][] data = new Object[Math.max(1, leaves.size())][7];

        if (leaves.isEmpty()) {
            data[0][0] = "-";
            data[0][1] = "No leave requests submitted yet.";
            data[0][2] = "-";
            data[0][3] = "-";
            data[0][4] = "-";
            data[0][5] = "-";
            data[0][6] = "-";
        } else {
            for (int i = 0; i < leaves.size(); i++) {
                Leave l = leaves.get(i);
                String stage = workflowStageService.leaveStage(l, employee);
                data[i][0] = l.getLeaveId();
                data[i][1] = l.getLeaveType();
                data[i][2] = l.getStartDate();
                data[i][3] = l.getEndDate();
                data[i][4] = l.getReason();
                data[i][5] = stage;
                data[i][6] = workflowStageService.ownerForStage(stage);
            }
        }

        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table.setModel(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        UITheme.setColumnWidths(table, 72, 120, 104, 104, 220, 210, 120);
    }

    @Override
    public void refreshData() {
        refreshTable();
    }

    private static class LeaveStatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {

            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                component.setBackground(row % 2 == 0 ? Color.WHITE : UITheme.TABLE_ALT_ROW);
                component.setForeground(UITheme.TEXT_PRIMARY);
            }

            if (column == 5 && value != null && !isSelected) {
                String status = value.toString().trim().toUpperCase();
                if (status.contains("PENDING") || status.contains("AWAITING")) {
                    component.setForeground(UITheme.YELLOW);
                } else if (status.contains("APPROVED")) {
                    component.setForeground(UITheme.SUCCESS);
                } else if (status.contains("REJECTED")) {
                    component.setForeground(UITheme.DANGER);
                }
            }

            return component;
        }
    }

    private void styleDateChooser(JDateChooser chooser) {
        chooser.setFont(UITheme.FONT_BODY);
        chooser.setBackground(Color.WHITE);
        chooser.setPreferredSize(new Dimension(220, 38));

        JTextField editor = ((JTextField) chooser.getDateEditor().getUiComponent());
        WorkforceFormToolkit.styleDateEditor(editor);
    }
}
