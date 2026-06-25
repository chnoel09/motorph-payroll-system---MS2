/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.oop.view;

import com.mycompany.oop.model.PayrollHistoryRecord;
import com.mycompany.oop.model.Employee;
import com.mycompany.oop.service.PayrollService;
import com.mycompany.oop.service.RoleAccessService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import com.mycompany.oop.service.ReportService;

public class HRPayrollHistoryPanel extends JPanel implements RefreshablePanel {

    private PayrollService payrollService;
    private RoleAccessService roleAccessService;
    private Employee currentUser;
    private JTable table;
    private JComboBox<String> cutoffBox;
    private JButton loadBtn;
    private JButton refreshBtn;
    private JButton exportBtn;
    private JButton payslipBtn;
    private JButton clearBtn;
    private JLabel lastRefreshedLabel;
    private JLabel emptyStateLabel;
    private JScrollPane tableScrollPane;
    private boolean historyControlsEnabled = true;

    private JPanel summaryPanel;
    private JLabel cutoffValueLabel;
    private JLabel recordsValueLabel;
    private JLabel grossValueLabel;
    private JLabel deductionsValueLabel;
    private JLabel netValueLabel;
    private JPanel centerPanel;
    private CardLayout centerCardLayout;
    private boolean refreshingCutoffs;

    public HRPayrollHistoryPanel() {
        this(null);
    }

    public HRPayrollHistoryPanel(Employee currentUser) {
        this.currentUser = currentUser;
        payrollService = new PayrollService();
        roleAccessService = new RoleAccessService();

        setLayout(new BorderLayout());
        setBackground(UITheme.BG);

        add(UITheme.createTitleBar("Payroll History (All Employees)"), BorderLayout.NORTH);

        JPanel content = UITheme.createWorkspacePanel(new BorderLayout(0, 14));

        emptyStateLabel = new JLabel(
                "<html><div style='text-align:center;'>"
                + "No processed payroll history available yet.<br>"
                + "Process payroll first before viewing saved records."
                + "</div></html>",
                SwingConstants.CENTER
        );
        emptyStateLabel.setFont(UITheme.FONT_BODY_BOLD);
        emptyStateLabel.setForeground(UITheme.TEXT_SECONDARY);
        emptyStateLabel.setBorder(new EmptyBorder(80, 0, 80, 0));
        emptyStateLabel.setVisible(false);

        JPanel topPanel = new JPanel(new BorderLayout(0, 12));
        topPanel.setBackground(UITheme.BG);
        topPanel.add(createContextPanel(
                "Saved History",
                "Values are persisted payroll records used for reporting, export, and payslip generation."
        ), BorderLayout.NORTH);
        topPanel.add(createSummaryPanel(), BorderLayout.CENTER);

        centerCardLayout = new CardLayout();
        centerPanel = new JPanel(centerCardLayout);
        centerPanel.setBackground(UITheme.BG);
        centerPanel.add(createTablePanel(), "table");
        centerPanel.add(emptyStateLabel, "empty");
        centerPanel.add(UITheme.createSkeletonCard("Refreshing Payroll History", 6), "loading");

        content.add(topPanel, BorderLayout.NORTH);
        content.add(centerPanel, BorderLayout.CENTER);
        content.add(createButtonPanel(), BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);

        if (cutoffBox.getItemCount() > 0) {
            updateEmptyState(false);
            loadHistory();
        } else {
            clearSummary();
            updateEmptyState(true);
        }
    }

    private JPanel createSummaryPanel() {
        summaryPanel = new JPanel(new GridLayout(1, 5, 12, 0));
        summaryPanel.setBackground(UITheme.BG);
        summaryPanel.setBorder(new EmptyBorder(0, 0, 2, 0));

        cutoffValueLabel = new JLabel("--");
        recordsValueLabel = new JLabel("0");
        grossValueLabel = new JLabel("₱0.00");
        deductionsValueLabel = new JLabel("₱0.00");
        netValueLabel = new JLabel("₱0.00");

        summaryPanel.add(createMetricCard("Selected Cutoff", cutoffValueLabel, UITheme.ACCENT));
        summaryPanel.add(createMetricCard("Records Loaded", recordsValueLabel, UITheme.BLACK));
        summaryPanel.add(createMetricCard("Total Gross", grossValueLabel, UITheme.BLUE));
        summaryPanel.add(createMetricCard("Total Deductions", deductionsValueLabel, UITheme.YELLOW));
        summaryPanel.add(createMetricCard("Total Net", netValueLabel, UITheme.ACCENT));

        return summaryPanel;
    }

    private JPanel createContextPanel(String chipText, String message) {
        JPanel panel = new JPanel(new BorderLayout(14, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JLabel chip = new JLabel(chipText);
        chip.setFont(UITheme.FONT_BODY_BOLD);
        chip.setForeground(UITheme.ACCENT);

        JLabel copy = new JLabel(message);
        copy.setFont(UITheme.FONT_BODY);
        copy.setForeground(UITheme.TEXT_SECONDARY);

        panel.add(chip, BorderLayout.WEST);
        panel.add(copy, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createMetricCard(String title, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(accent);
                g2.fillRect(0, 0, getWidth(), 3);
                g2.dispose();
            }
        };
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                new EmptyBorder(13, 15, 12, 15)
        ));
        card.setPreferredSize(new Dimension(0, 82));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UITheme.FONT_CARD_LABEL);
        titleLabel.setForeground(UITheme.TEXT_SECONDARY);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        valueLabel.setForeground(UITheme.TEXT_PRIMARY);
        valueLabel.setBorder(new EmptyBorder(8, 0, 0, 0));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private JScrollPane createTablePanel() {
        table = new JTable();
        UITheme.styleTable(table);
        table.setSelectionBackground(new Color(254, 226, 226));
        table.setSelectionForeground(UITheme.TEXT_PRIMARY);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updatePayslipButtonState();
            }
        });
        tableScrollPane = UITheme.createTableScrollPane(table);
        return tableScrollPane;
    }

    private JPanel createButtonPanel() {
        JPanel panel = UITheme.createActionBar();

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setBackground(Color.WHITE);

        List<String> processedCutoffs = payrollService.getProcessedCutoffs();
        cutoffBox = new JComboBox<>(processedCutoffs.toArray(new String[0]));
        cutoffBox.setFont(UITheme.FONT_BODY);
        cutoffBox.setPreferredSize(new Dimension(190, 34));
        cutoffBox.addActionListener(e -> {
            if (!refreshingCutoffs) {
                loadHistory();
            }
        });

        loadBtn = UITheme.createAccentButton("Load");
        refreshBtn = UITheme.createCompactWorkflowButton("Refresh History", false);
        payslipBtn = UITheme.createAccentButton("Save Payslip PDF");
        exportBtn = UITheme.createBlueButton("Export CSV");
        clearBtn = UITheme.createSidebarDangerButton("Clear Cutoff");

        loadBtn.setPreferredSize(new Dimension(100, 34));
        UITheme.sizeButtonToFit(refreshBtn, 135, 30);
        payslipBtn.setPreferredSize(new Dimension(155, 34));
        exportBtn.setPreferredSize(new Dimension(120, 34));
        clearBtn.setPreferredSize(new Dimension(130, 34));
        clearBtn.setHorizontalAlignment(SwingConstants.CENTER);
        clearBtn.setEnabled(roleAccessService.canGeneratePayroll(getCurrentRole()));
        payslipBtn.setEnabled(false);
        updatePayslipButtonState();

        loadBtn.addActionListener(e -> loadHistory());

        refreshBtn.addActionListener(e -> refreshData());

        payslipBtn.addActionListener(e -> generatePayslip());

        exportBtn.addActionListener(e -> exportHistory());

        clearBtn.addActionListener(e -> clearCutoff());
        
        JLabel cutoffLabel = new JLabel("Cutoff:");
        cutoffLabel.setFont(UITheme.FONT_BODY);
        cutoffLabel.setForeground(UITheme.TEXT_SECONDARY);

        lastRefreshedLabel = new JLabel("Last refreshed: --");
        lastRefreshedLabel.setFont(UITheme.FONT_SMALL);
        lastRefreshedLabel.setForeground(UITheme.TEXT_SECONDARY);

        actions.add(cutoffLabel);
        actions.add(cutoffBox);
        actions.add(loadBtn);
        actions.add(refreshBtn);
        actions.add(payslipBtn);
        actions.add(exportBtn);
        actions.add(clearBtn);
        actions.add(lastRefreshedLabel);

        panel.add(actions, BorderLayout.EAST);

        return panel;
    }

    private void loadHistory() {
        if (cutoffBox.getItemCount() == 0 || cutoffBox.getSelectedItem() == null) {
            clearSummary();
            updateEmptyState(true);
            return;
        }

        String cutoff = cutoffBox.getSelectedItem().toString();
        setControlsEnabled(false);
        centerCardLayout.show(centerPanel, "loading");

        SwingWorker<List<PayrollHistoryRecord>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<PayrollHistoryRecord> doInBackground() {
                return payrollService.getPayrollHistoryByCutoff(cutoff);
            }

            @Override
            protected void done() {
                setControlsEnabled(true);

                try {
                    List<PayrollHistoryRecord> records = get();
                    updateTable(records);
                    updateSummary(cutoff, records);
                    updateEmptyState(records.isEmpty());
                    updateLastRefreshed();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(HRPayrollHistoryPanel.this,
                            "An error occurred while loading payroll history.",
                            "Load Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    @Override
    public void refreshData() {
        if (cutoffBox == null) {
            return;
        }

        Object selected = cutoffBox.getSelectedItem();
        String selectedCutoff = selected == null ? null : selected.toString();
        List<String> processedCutoffs = payrollService.getProcessedCutoffs();

        refreshingCutoffs = true;
        cutoffBox.removeAllItems();
        for (String cutoff : processedCutoffs) {
            cutoffBox.addItem(cutoff);
        }
        if (selectedCutoff != null && processedCutoffs.contains(selectedCutoff)) {
            cutoffBox.setSelectedItem(selectedCutoff);
        } else if (cutoffBox.getItemCount() > 0) {
            cutoffBox.setSelectedIndex(0);
        }
        refreshingCutoffs = false;

        if (cutoffBox.getItemCount() > 0) {
            loadHistory();
        } else {
            clearSummary();
            updateEmptyState(true);
        }
    }

    private void updateLastRefreshed() {
        if (lastRefreshedLabel != null) {
            lastRefreshedLabel.setText("Last refreshed: "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }
    }

    private void updateTable(List<PayrollHistoryRecord> records) {
        NumberFormat peso = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));

        String[] cols = {
                "Emp ID", "Cutoff", "Date Range", "Number of Days", "Hours Worked", "Gross", "SSS", "PhilHealth",
                "Pag-IBIG", "Tax", "Total Deductions", "Net"
        };

        Object[][] data = new Object[records.size()][12];

        for (int i = 0; i < records.size(); i++) {
            PayrollHistoryRecord record = records.get(i);
            LocalDate[] period = resolvePeriod(record);

            data[i][0] = record.getEmployeeId();
            data[i][1] = record.getCutoffPeriod();
            data[i][2] = formatPeriod(period);
            data[i][3] = formatWorkdays(period);
            data[i][4] = String.format("%.2f", record.getHoursWorked());
            data[i][5] = peso.format(record.getGross());
            data[i][6] = peso.format(record.getSss());
            data[i][7] = peso.format(record.getPhilhealth());
            data[i][8] = peso.format(record.getPagibig());
            data[i][9] = peso.format(record.getTax());
            data[i][10] = peso.format(record.getTotalDeductions());
            data[i][11] = peso.format(record.getNet());
        }

        DefaultTableModel model = new DefaultTableModel(data, cols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table.setModel(model);
        if (!records.isEmpty()) {
            table.setRowSelectionInterval(0, 0);
        }
        updatePayslipButtonState();
    }

    private LocalDate[] resolvePeriod(PayrollHistoryRecord record) {
        LocalDate start = parseDate(record.getPeriodStart());
        LocalDate end = parseDate(record.getPeriodEnd());

        if (start != null && end != null) {
            return new LocalDate[]{start, end};
        }

        try {
            return new LocalDate[]{
                    LocalDate.parse(payrollService.getCutoffStartDate(record.getCutoffPeriod())),
                    LocalDate.parse(payrollService.getCutoffEndDate(record.getCutoffPeriod()))
            };
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        try {
            return value == null || value.trim().isEmpty() ? null : LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String formatPeriod(LocalDate[] period) {
        if (period == null || period[0] == null || period[1] == null) {
            return "--";
        }

        return period[0] + " to " + period[1];
    }

    private String formatWorkdays(LocalDate[] period) {
        if (period == null || period[0] == null || period[1] == null || period[1].isBefore(period[0])) {
            return "--";
        }

        int days = 0;
        LocalDate current = period[0];

        while (!current.isAfter(period[1])) {
            DayOfWeek day = current.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                days++;
            }
            current = current.plusDays(1);
        }

        // Hours beyond standard daily hours may be handled as overtime in a future enhancement.
        return String.valueOf(days);
    }

    private void updateSummary(String cutoff, List<PayrollHistoryRecord> records) {
        NumberFormat peso = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));

        double totalGross = 0.0;
        double totalDeductions = 0.0;
        double totalNet = 0.0;

        for (PayrollHistoryRecord record : records) {
            totalGross += record.getGross();
            totalDeductions += record.getTotalDeductions();
            totalNet += record.getNet();
        }

        cutoffValueLabel.setText(cutoff == null || cutoff.trim().isEmpty() ? "--" : cutoff);
        recordsValueLabel.setText(String.valueOf(records.size()));
        grossValueLabel.setText(peso.format(totalGross));
        deductionsValueLabel.setText(peso.format(totalDeductions));
        netValueLabel.setText(peso.format(totalNet));
    }

    private void clearSummary() {
        cutoffValueLabel.setText("--");
        recordsValueLabel.setText("0");
        grossValueLabel.setText("₱0.00");
        deductionsValueLabel.setText("₱0.00");
        netValueLabel.setText("₱0.00");
    }

    private void exportHistory() {
        if (cutoffBox.getSelectedItem() == null) {
            return;
        }

        String cutoff = cutoffBox.getSelectedItem().toString();

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("payroll_history_" + cutoff + ".csv"));

        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        String filePath = chooser.getSelectedFile().getAbsolutePath();

        boolean success = payrollService.exportPayrollHistoryByCutoff(cutoff, filePath);

        if (success) {
            JOptionPane.showMessageDialog(this,
                    "Payroll history exported successfully.");
        } else {
            JOptionPane.showMessageDialog(this,
                    "No payroll history found for the selected cutoff.",
                    "Export Failed",
                    JOptionPane.WARNING_MESSAGE);
        }
    }
    
    private void generatePayslip() {
        if (!roleAccessService.canGeneratePayroll(getCurrentRole())) {
            showAccessDeniedMessage("Only Finance users can generate payslips from payroll history.");
            return;
        }

        int selectedRow = table.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a payroll record first.",
                    "No Record Selected",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {

            int modelRow = table.convertRowIndexToModel(selectedRow);
            int employeeId = Integer.parseInt(
                    table.getModel().getValueAt(modelRow, 0).toString()
            );
            String cutoff = table.getModel().getValueAt(modelRow, 1).toString();

            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(
                    new File("payslip_" + employeeId + ".pdf")
            );

            int result = chooser.showSaveDialog(this);

            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }

            String outputPath =
                    chooser.getSelectedFile().getAbsolutePath();

            ReportService reportService = new ReportService();

            reportService.generatePayslip(
                employeeId,
                cutoff,
                outputPath
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Payslip generated successfully."
            );

        } catch (Exception ex) {

            ex.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Failed to generate payslip.",
                    "Report Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void clearCutoff() {
        if (!roleAccessService.canGeneratePayroll(getCurrentRole())) {
            showAccessDeniedMessage("Only Finance users can clear payroll history.");
            return;
        }

        if (cutoffBox.getSelectedItem() == null) {
            return;
        }

        String cutoff = cutoffBox.getSelectedItem().toString();

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete all saved payroll history for " + cutoff + "?",
                "Confirm Clear Cutoff",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        payrollService.deleteCutoff(cutoff);

        cutoffBox.removeAllItems();
        List<String> processedCutoffs = payrollService.getProcessedCutoffs();
        for (String item : processedCutoffs) {
            cutoffBox.addItem(item);
        }

        if (cutoffBox.getItemCount() > 0) {
            updateEmptyState(false);
            loadHistory();
        } else {
            table.setModel(new DefaultTableModel());
            clearSummary();
            updateEmptyState(true);
        }

        JOptionPane.showMessageDialog(this,
                "Payroll history for " + cutoff + " has been cleared.");
    }

    private void setControlsEnabled(boolean enabled) {
        historyControlsEnabled = enabled;
        if (cutoffBox != null) cutoffBox.setEnabled(enabled);
        if (loadBtn != null) loadBtn.setEnabled(enabled);
        if (refreshBtn != null) refreshBtn.setEnabled(enabled);
        if (exportBtn != null) exportBtn.setEnabled(enabled);
        if (clearBtn != null) clearBtn.setEnabled(enabled && roleAccessService.canGeneratePayroll(getCurrentRole()));
        updatePayslipButtonState();
    }

    private void updateEmptyState(boolean showEmpty) {
        emptyStateLabel.setVisible(showEmpty);

        if (centerCardLayout != null && centerPanel != null) {
            centerCardLayout.show(centerPanel, showEmpty ? "empty" : "table");
        }

        if (summaryPanel != null) {
            summaryPanel.setVisible(true);
        }

        if (showEmpty) {
            clearSummary();
        }

        if (loadBtn != null) loadBtn.setEnabled(!showEmpty);
        if (refreshBtn != null) refreshBtn.setEnabled(true);
        if (exportBtn != null) exportBtn.setEnabled(!showEmpty);
        if (clearBtn != null) clearBtn.setEnabled(!showEmpty && roleAccessService.canGeneratePayroll(getCurrentRole()));
        
        updatePayslipButtonState();
    }

    private void updatePayslipButtonState() {
        if (payslipBtn == null) {
            return;
        }

        boolean canGenerate = roleAccessService.canGeneratePayroll(getCurrentRole());
        boolean hasSelectedRecord = table != null
                && table.getModel() != null
                && table.getModel().getRowCount() > 0
                && table.getSelectedRow() >= 0
                && (emptyStateLabel == null || !emptyStateLabel.isVisible());
        boolean enabled = historyControlsEnabled && canGenerate && hasSelectedRecord;

        payslipBtn.setEnabled(enabled);
        if (!canGenerate) {
            payslipBtn.setToolTipText("Only Finance users can save payslip PDFs from payroll history.");
        } else if (!hasSelectedRecord) {
            payslipBtn.setToolTipText("Select a persisted payroll history row first.");
        } else {
            payslipBtn.setToolTipText("Save a Jasper PDF payslip for the selected payroll history row.");
        }
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
}
