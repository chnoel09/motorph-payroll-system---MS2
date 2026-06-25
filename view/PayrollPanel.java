/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.oop.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.PayrollHistoryRecord;
import com.mycompany.oop.model.PayrollLifecycleStatus;
import com.mycompany.oop.model.PayrollPeriod;
import com.mycompany.oop.model.PayrollReadinessIssue;
import com.mycompany.oop.model.PayrollReadinessReport;
import com.mycompany.oop.model.PayrollRecord;
import com.mycompany.oop.model.WorkforcePayrollReadiness;
import com.mycompany.oop.service.PayrollLifecycleService;
import com.mycompany.oop.service.PayrollPeriodLifecycleService;
import com.mycompany.oop.service.PayrollReadinessService;
import com.mycompany.oop.service.PayrollService;
import com.mycompany.oop.service.ReportService;
import com.mycompany.oop.service.RoleAccessService;
import com.mycompany.oop.service.WorkforceReadinessService;
import com.mycompany.oop.service.WorkforceReadinessService.WorkforceReadinessSummary;

public class PayrollPanel extends JPanel implements RefreshablePanel {

    private PayrollService payrollService;
    private PayrollLifecycleService payrollLifecycleService;
    private PayrollReadinessService payrollReadinessService;
    private PayrollPeriodLifecycleService payrollPeriodLifecycleService;
    private WorkforceReadinessService workforceReadinessService;
    private RoleAccessService roleAccessService;
    private Employee currentUser;
    private Consumer<String> navigationHandler;
    private JTable table;
    private JTable readinessTable;
    private JTable payrollReadyTable;
    private JComboBox<String> cutoffBox;
    private JPanel summaryWrapper;
    private JPanel operationsRailWrapper;
    private JPanel dependencyWrapper;
    private JPanel lifecycleWrapper;
    private JPanel blockerWrapper;
    private JButton processBtn;
    private JButton refreshBtn;
    private JButton reviewBtn;
    private JButton validateBtn;
    private JButton confirmAllBtn;
    private JButton lockBtn;
    private JButton newPeriodBtn;
    private JButton activateReviewBtn;
    private JTextField dateFromField;
    private JTextField dateToField;
    private JLabel emptyStateLabel;
    private JScrollPane tableScrollPane;
    private JPanel centerPanel;
    private CardLayout centerCardLayout;
    private PayrollReadinessReport currentReadinessReport;
    private PayrollLifecycleStatus currentLifecycleStatus;
    private PayrollPeriod currentPayrollPeriod;
    private WorkforceReadinessSummary currentWorkforceSummary;
    private Runnable workflowRefreshHandler;
    private Runnable payrollArtifactsRefreshHandler;
    private List<WorkforcePayrollReadiness> currentFinanceQueue = List.of();
    private List<WorkforcePayrollReadiness> currentAllReadinessRows = List.of();
    private Map<Integer, Employee> currentEmployeesById = Map.of();
    private Map<Integer, List<PayrollReadinessIssue>> currentIssuesByEmployee = Map.of();
    private boolean suppressCutoffEvents;
    private Integer focusedRegisterEmployeeId;

    public PayrollPanel() {
        this(null);
    }

    public PayrollPanel(Employee currentUser) {
        this(currentUser, null);
    }

    public PayrollPanel(Employee currentUser, Consumer<String> navigationHandler) {
        this(currentUser, navigationHandler, null);
    }

    public PayrollPanel(Employee currentUser, Consumer<String> navigationHandler, Runnable workflowRefreshHandler) {
        this(currentUser, navigationHandler, workflowRefreshHandler, null);
    }

    public PayrollPanel(Employee currentUser, Consumer<String> navigationHandler, Runnable workflowRefreshHandler,
            Runnable payrollArtifactsRefreshHandler) {

        this.currentUser = currentUser;
        this.navigationHandler = navigationHandler;
        this.workflowRefreshHandler = workflowRefreshHandler;
        this.payrollArtifactsRefreshHandler = payrollArtifactsRefreshHandler;
        payrollService = new PayrollService();
        payrollLifecycleService = new PayrollLifecycleService();
        payrollReadinessService = new PayrollReadinessService();
        payrollPeriodLifecycleService = new PayrollPeriodLifecycleService();
        workforceReadinessService = new WorkforceReadinessService();
        roleAccessService = new RoleAccessService();

        setLayout(new BorderLayout());
        setBackground(UITheme.BG);

        add(UITheme.createTitleBar("Payroll Operations"), BorderLayout.NORTH);

        JPanel content = UITheme.createWorkspacePanel(new BorderLayout(0, 14));

        summaryWrapper = new JPanel(new BorderLayout());
        summaryWrapper.setBackground(UITheme.BG);

        emptyStateLabel = new JLabel(
                "<html><div style='text-align:center;'>"
                + "No attendance cutoff available yet.<br>"
                + "Payroll preview will appear once attendance records exist."
                + "</div></html>",
                SwingConstants.CENTER
        );
        emptyStateLabel.setFont(UITheme.FONT_BODY_BOLD);
        emptyStateLabel.setForeground(UITheme.TEXT_SECONDARY);
        emptyStateLabel.setBorder(new EmptyBorder(40, 0, 40, 0));
        emptyStateLabel.setVisible(false);

        centerCardLayout = new CardLayout();
        centerPanel = new JPanel(centerCardLayout);
        centerPanel.setBackground(UITheme.BG);
        centerPanel.add(createDataPanel(), "table");
        centerPanel.add(emptyStateLabel, "empty");

        JPanel topStack = new JPanel(new BorderLayout(0, 8));
        topStack.setBackground(UITheme.BG);
        topStack.add(summaryWrapper, BorderLayout.CENTER);

        content.add(topStack, BorderLayout.NORTH);
        content.add(centerPanel, BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);

        updateEmptyState(false);
        selectInitialPayrollPeriod();
        refreshAll();
    }

    // ================= SUMMARY =================

    private JPanel createPayrollRunHeader(PayrollViewData viewData) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, UITheme.BORDER),
                new EmptyBorder(6, 10, 6, 10)
        ));

        NumberFormat peso = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        panel.add(createContextRow(
                createInlineContext("Payroll Period", safeText(viewData.lifecycleStatus.getCutoffPeriod(), selectedCutoff())),
                createInlineContext("Date Range", safeText(viewData.lifecycleStatus.getPeriodStart(), dateFromField.getText())
                        + " to " + safeText(viewData.lifecycleStatus.getPeriodEnd(), dateToField.getText())),
                createInlineContext("Lifecycle Status", formatLifecycleStatus(viewData.lifecycleStatus))
        ));
        panel.add(createContextRow(
                createInlineContext("Workforce Readiness", formatReadinessStatus(viewData.readinessReport.getStatus())),
                createInlineContext("Readiness Rows", String.valueOf(viewData.workforceSummary.getTotalEmployees())),
                createInlineContext("Finance Owner", "Finance")
        ));
        panel.add(createContextRow(
                createInlineContext("Last Processed", formatProcessedTimestamp(viewData.lifecycleStatus.getProcessedAt())),
                createInlineContext("Register Net", peso.format(viewData.summary.totalNet))
        ));
        return panel;
    }

    private JPanel createContextRow(JComponent... components) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 2));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        for (JComponent component : components) {
            row.add(component);
        }
        return row;
    }

    private JLabel createInlineContext(String title, String value) {
        JLabel label = new JLabel("<html><span style='color:#64748b'>" + title + ":</span> <b>"
                + safeText(value, "-") + "</b></html>");
        label.setFont(UITheme.FONT_SMALL);
        label.setForeground(UITheme.TEXT_PRIMARY);
        label.setToolTipText(value);
        return label;
    }

    private JPanel createMetricCard(String title, String value, Color accent) {

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
                new EmptyBorder(12, 14, 11, 14)
        ));
        card.setPreferredSize(new Dimension(0, 76));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UITheme.FONT_CARD_LABEL);
        titleLabel.setForeground(UITheme.TEXT_SECONDARY);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, value != null && value.length() > 20 ? 12 : 15));
        valueLabel.setForeground(UITheme.TEXT_PRIMARY);
        valueLabel.setBorder(new EmptyBorder(5, 0, 0, 0));
        valueLabel.setToolTipText(value);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    // ================= TABLE =================

    private JScrollPane createTablePanel() {
        table = new JTable();
        UITheme.styleTable(table);
        table.setSelectionBackground(new Color(254, 226, 226));
        table.setSelectionForeground(UITheme.TEXT_PRIMARY);
        tableScrollPane = UITheme.createTableScrollPane(table);
        tableScrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                new EmptyBorder(0, 0, 0, 0)
        ));
        return tableScrollPane;
    }

    private JPanel createDataPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UITheme.BG);

        JPanel readiness = createReadinessSection();
        operationsRailWrapper = new JPanel(new BorderLayout());
        operationsRailWrapper.setOpaque(false);
        operationsRailWrapper.setMinimumSize(new Dimension(340, 260));
        operationsRailWrapper.setPreferredSize(new Dimension(430, 0));
        JScrollPane operationsScroll = new JScrollPane(createOperationsRail(null, null));
        operationsScroll.setBorder(BorderFactory.createEmptyBorder());
        operationsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        operationsScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        operationsScroll.getViewport().setBackground(UITheme.BG);
        operationsRailWrapper.add(operationsScroll, BorderLayout.CENTER);

        readiness.setMinimumSize(new Dimension(480, 260));
        operationsRailWrapper.setMinimumSize(new Dimension(320, 260));

        JSplitPane reviewArea = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, readiness, operationsRailWrapper);
        reviewArea.setResizeWeight(0.74);
        reviewArea.setContinuousLayout(true);
        reviewArea.setOneTouchExpandable(false);
        reviewArea.setDividerSize(7);
        reviewArea.setBorder(BorderFactory.createEmptyBorder());
        reviewArea.setBackground(UITheme.BG);
        reviewArea.setOpaque(false);

        JPanel register = createPayrollRegisterSection();
        panel.add(reviewArea, BorderLayout.CENTER);
        panel.add(register, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createReadinessSection() {
        JPanel section = createWorksheetSection(
                "Finance Payroll Validation Queue",
                "Finance validates HR-endorsed employees before payroll processing."
        );

        readinessTable = new JTable();
        UITheme.styleTable(readinessTable);
        readinessTable.setRowHeight(24);
        readinessTable.setIntercellSpacing(new Dimension(1, 1));
        readinessTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                syncRegisterSelectionToFinanceQueue();
                updatePayrollActionAvailability(true);
            }
        });
        JScrollPane scrollPane = UITheme.createTableScrollPane(readinessTable);
        scrollPane.setMinimumSize(new Dimension(360, 260));
        scrollPane.setPreferredSize(new Dimension(520, 430));
        section.add(scrollPane, BorderLayout.CENTER);
        return section;
    }

    private JPanel createPayrollReadySection() {
        JPanel section = createWorksheetSection(
                "Payroll Ready",
                "Finance-confirmed rows ready for processing."
        );
        payrollReadyTable = new JTable();
        UITheme.styleTable(payrollReadyTable);
        payrollReadyTable.setRowHeight(24);
        payrollReadyTable.setIntercellSpacing(new Dimension(1, 1));
        JScrollPane scrollPane = UITheme.createTableScrollPane(payrollReadyTable);
        scrollPane.setMinimumSize(new Dimension(300, 260));
        scrollPane.setPreferredSize(new Dimension(420, 430));
        section.add(scrollPane, BorderLayout.CENTER);
        return section;
    }

    private JPanel createPayrollRegisterSection() {
        JPanel section = createWorksheetSection(
                "Payroll Preview",
                "Recalculated from current attendance and compensation; finalized values are in Payroll History."
        );
        section.add(createTablePanel(), BorderLayout.CENTER);
        tableScrollPane.setPreferredSize(new Dimension(0, 210));
        return section;
    }

    private JPanel createWorksheetSection(String title, String subtitle) {
        JPanel section = new JPanel(new BorderLayout(0, 6));
        section.setBackground(Color.WHITE);
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                new EmptyBorder(8, 10, 10, 10)
        ));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UITheme.FONT_BODY_BOLD);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(UITheme.FONT_SMALL);
        subtitleLabel.setForeground(UITheme.TEXT_SECONDARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(titleLabel);
        header.add(Box.createVerticalStrut(2));
        header.add(subtitleLabel);
        section.add(header, BorderLayout.NORTH);
        return section;
    }

    private void updateTable(Object[][] data) {
        String[] cols = {"Employee ID", "Employee", "Hours", "Gross", "Adjustments", "Deductions", "Net Pay", "Payroll Status"};

        DefaultTableModel model = new DefaultTableModel(data, cols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table.setModel(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(24);
        table.setIntercellSpacing(new Dimension(1, 1));
        UITheme.setColumnWidths(table, 90, 220, 75, 125, 115, 125, 125, 150);
        enableUserColumnResizing(table);
        installRegisterStatusRenderer(table);
    }

    // ================= BUTTONS =================

    private JPanel createButtonPanel() {

        JPanel panel = UITheme.createActionBar();

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));

        List<String> cutoffs = getPayrollPeriodCutoffOptions();
        cutoffBox = new JComboBox<>(cutoffs.toArray(new String[0]));
        WorkforceFormToolkit.styleComboBox(cutoffBox);
        cutoffBox.setPreferredSize(new Dimension(220, 34));
        cutoffBox.setMaximumSize(new Dimension(260, 34));
        WorkforceFormToolkit.applyMonthHelp(cutoffBox);
        dateFromField = createDateField();
        dateToField = createDateField();
        dateFromField.setMaximumSize(new Dimension(260, 34));
        dateToField.setMaximumSize(new Dimension(260, 34));
        syncDateFieldsToCutoff();

        processBtn = UITheme.createAccentButton("Process Ready Payroll");
        validateBtn = UITheme.createButton("Confirm Payroll Ready");
        confirmAllBtn = UITheme.createButton("Confirm All Ready");
        newPeriodBtn = UITheme.createButton("New Payroll Period");
        activateReviewBtn = UITheme.createButton("Open Workforce Review");
        reviewBtn = UITheme.createButton("Review");
        JButton returnHrBtn = UITheme.createButton("<< Return HR");
        JButton historyBtn = UITheme.createButton("Payroll History");
        JButton summaryReportBtn = UITheme.createButton("Payroll Summary Report");
        lockBtn = UITheme.createButton("Lock");
        refreshBtn = UITheme.createButton("Refresh");

        for (JButton button : List.of(processBtn, validateBtn, confirmAllBtn, newPeriodBtn, activateReviewBtn,
                reviewBtn, returnHrBtn, historyBtn, summaryReportBtn, lockBtn, refreshBtn)) {
            UITheme.sizeButtonToFit(button, 210, button == validateBtn ? 34 : 32);
        }

        summaryReportBtn.setToolTipText("Generate payroll summary report for the selected payroll period");
        activateReviewBtn.setToolTipText("Open or sync workforce review rows for the selected payroll period");
        returnHrBtn.setToolTipText("Return to HR is not enabled in this release.");
        returnHrBtn.setEnabled(false);
        processBtn.setToolTipText("Processes only employees confirmed as Payroll Ready.");
        reviewBtn.setToolTipText("Select an employee in the Finance Payroll Validation Queue first.");
        validateBtn.setToolTipText("Select a Finance-owned HR-endorsed employee first.");
        newPeriodBtn.setToolTipText("Create a new payroll period");
        historyBtn.setToolTipText("Open payroll history");
        lockBtn.setToolTipText("Lock payroll run");
        processBtn.addActionListener(e -> processPayroll());
        validateBtn.addActionListener(e -> confirmPayrollReadiness());
        confirmAllBtn.addActionListener(e -> confirmAllPayrollReady());
        reviewBtn.addActionListener(e -> openPayrollDetails());
        newPeriodBtn.addActionListener(e -> createPayrollPeriod());
        activateReviewBtn.addActionListener(e -> activateWorkforceReview());
        historyBtn.addActionListener(e -> openPayrollHistory());
        summaryReportBtn.addActionListener(e -> generatePayrollSummaryReport());
        lockBtn.addActionListener(e -> showLockPayrollMessage());
        refreshBtn.addActionListener(e -> refreshAll());
        cutoffBox.addActionListener(e -> {
            if (suppressCutoffEvents) {
                return;
            }
            syncDateFieldsToCutoff();
            refreshAll();
        });

        JPanel periodGroup = createFooterGroup("Payroll Period");
        periodGroup.setLayout(new BoxLayout(periodGroup, BoxLayout.Y_AXIS));
        addVerticalFooterField(periodGroup, "Cutoff", cutoffBox);
        addVerticalFooterField(periodGroup, "Date From", dateFromField);
        addVerticalFooterField(periodGroup, "Date To", dateToField);
        periodGroup.add(WorkforceFormToolkit.createHelpLabel("Dates: yyyy-MM-dd"));

        JPanel actionGroup = createFooterGroup("Finance Actions");
        actionGroup.setLayout(new GridLayout(0, 1, 0, 7));
        actionGroup.add(newPeriodBtn);
        actionGroup.add(activateReviewBtn);
        actionGroup.add(reviewBtn);
        actionGroup.add(validateBtn);
        actionGroup.add(confirmAllBtn);
        returnHrBtn.setVisible(false);
        actionGroup.add(returnHrBtn);
        actionGroup.add(processBtn);
        actionGroup.add(summaryReportBtn);
        actionGroup.add(historyBtn);
        actionGroup.add(refreshBtn);
        actionGroup.add(lockBtn);

        stack.add(periodGroup);
        stack.add(Box.createVerticalStrut(10));
        stack.add(actionGroup);

        panel.add(stack, BorderLayout.CENTER);

        return panel;
    }

    private JTextField createDateField() {
        return UITheme.createDateField();
    }

    private JLabel createActionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UITheme.FONT_BODY);
        label.setForeground(UITheme.TEXT_SECONDARY);
        return label;
    }

    private JPanel createFooterGroup(String title) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(),
                title,
                0,
                0,
                UITheme.FONT_BODY_BOLD,
                UITheme.TEXT_PRIMARY
        ));
        return row;
    }

    private void addFooterField(JPanel row, String label, JComponent field) {
        JLabel fieldLabel = createActionLabel(label + ":");
        row.add(fieldLabel);
        row.add(field);
    }

    private void addVerticalFooterField(JPanel group, String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(280, 56));

        JLabel fieldLabel = createActionLabel(label + ":");
        fieldLabel.setFont(UITheme.FONT_SMALL);
        row.add(fieldLabel, BorderLayout.NORTH);
        row.add(field, BorderLayout.CENTER);

        group.add(row);
        group.add(Box.createVerticalStrut(6));
    }

    private void syncDateFieldsToCutoff() {
        if (cutoffBox == null || cutoffBox.getSelectedItem() == null
                || dateFromField == null || dateToField == null) {
            return;
        }

        String cutoff = cutoffBox.getSelectedItem().toString();
        PayrollPeriod period = findPeriodByCutoffLabel(cutoff);
        if (period != null) {
            dateFromField.setText(period.getPeriodStart() == null ? "" : period.getPeriodStart().toString());
            dateToField.setText(period.getPeriodEnd() == null ? "" : period.getPeriodEnd().toString());
            return;
        }
        try {
            dateFromField.setText(payrollService.getCutoffStartDate(cutoff));
            dateToField.setText(payrollService.getCutoffEndDate(cutoff));
        } catch (Exception e) {
            dateFromField.setText("");
            dateToField.setText("");
        }
    }

    private void processPayroll() {
        if (cutoffBox.getSelectedItem() == null) return;

        if (!roleAccessService.canGeneratePayroll(getCurrentRole())) {
            showAccessDeniedMessage("Only Finance users can process payroll.");
            return;
        }

        String disabledReason = getProcessPayrollDisabledReason();
        if (disabledReason != null) {
            JOptionPane.showMessageDialog(this,
                    disabledReason,
                    "Payroll Validation Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String cutoff = cutoffBox.getSelectedItem().toString();
        String dateFrom = dateFromField.getText().trim();
        String dateTo = dateToField.getText().trim();
        List<WorkforcePayrollReadiness> readyRows = getProcessableReadyRows();
        if (readyRows.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No payroll-ready employees are available for processing.",
                    "Payroll Validation Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        PayrollLifecycleStatus lifecycleStatus = payrollLifecycleService.getLifecycleStatus(cutoff, dateFrom, dateTo);
        if (lifecycleStatus.isProcessed()) {
            DuplicateDecision decision = handleProcessedCutoff(cutoff);
            if (decision == DuplicateDecision.REFRESH) {
                refreshAll();
                return;
            }
            if (decision == DuplicateDecision.OPEN_HISTORY) {
                openPayrollHistory();
                return;
            }
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Process payroll for " + readyRows.size() + " payroll-ready employee"
                        + (readyRows.size() == 1 ? "" : "s") + " only?\n\n"
                        + "Unresolved employees will not be included in payroll history.",
                "Process Ready Payroll",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        setControlsEnabled(false);
        List<Integer> readyEmployeeIds = readyRows.stream()
                .map(WorkforcePayrollReadiness::getEmployeeId)
                .distinct()
                .toList();

        SwingWorker<Integer, Void> worker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() {
                return payrollService.processAndSavePayrollForEmployees(
                        cutoff, dateFrom, dateTo, readyEmployeeIds, getCurrentEmployeeId());
            }

            @Override
            protected void done() {
                setControlsEnabled(true);
                try {
                    int processed = get();
                    JOptionPane.showMessageDialog(PayrollPanel.this,
                            "Processed payroll for " + processed + " payroll-ready employee"
                                    + (processed == 1 ? "" : "s") + " in " + cutoff + ".");
                    refreshAll();
                    notifyPayrollArtifactsRefresh();

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(PayrollPanel.this,
                            "An error occurred while processing ready payroll.",
                            "Processing Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private void confirmPayrollReadiness() {
        if (!roleAccessService.canGeneratePayroll(getCurrentRole())) {
            showAccessDeniedMessage("Only Finance users can confirm payroll readiness.");
            return;
        }

        WorkforcePayrollReadiness selected = getSelectedFinanceReadiness();
        if (selected == null) {
            JOptionPane.showMessageDialog(this,
                    "Select an HR-endorsed employee in the Finance Payroll Validation Queue first.",
                    "Payroll Readiness",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        focusedRegisterEmployeeId = selected.getEmployeeId();
        runPayrollWorkflowAction("Confirming payroll readiness...", () -> {
            workforceReadinessService.confirmFinanceReadiness(selected.getReadinessId(), currentUser);
            return "Payroll readiness confirmed for the selected employee.";
        }, "Payroll Readiness Confirmed");
    }

    private void confirmAllPayrollReady() {
        if (!roleAccessService.canGeneratePayroll(getCurrentRole())) {
            showAccessDeniedMessage("Only Finance users can confirm payroll readiness.");
            return;
        }
        List<WorkforcePayrollReadiness> eligibleRows = currentFinanceQueue == null ? List.of() : currentFinanceQueue.stream()
                .filter(this::isConfirmableFinanceReadiness)
                .toList();
        if (eligibleRows.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No Finance-owned HR-endorsed employees are eligible for payroll-ready confirmation.",
                    "Confirm All Ready",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "This will process " + eligibleRows.size() + " eligible employee"
                        + (eligibleRows.size() == 1 ? "" : "s") + ". Continue?",
                "Confirm All Ready",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        focusedRegisterEmployeeId = eligibleRows.get(0).getEmployeeId();
        runPayrollWorkflowAction("Confirming all ready...", () -> {
            int success = 0;
            int skipped = 0;
            for (WorkforcePayrollReadiness readiness : eligibleRows) {
                try {
                    workforceReadinessService.confirmFinanceReadiness(readiness.getReadinessId(), currentUser);
                    success++;
                } catch (Exception ex) {
                    skipped++;
                }
            }
            return "Completed " + success + ", skipped " + skipped + ".";
        }, "Confirm All Ready");
    }

    private void createPayrollPeriod() {
        if (!roleAccessService.canGeneratePayroll(getCurrentRole())) {
            showAccessDeniedMessage("Only Finance users can create payroll periods.");
            return;
        }

        JTextField cutoffField = new JTextField();
        WorkforceFormToolkit.styleTextField(cutoffField);
        cutoffField.setToolTipText("Example: 2026-05 A");
        JTextField startField = UITheme.createDateField();
        JTextField endField = UITheme.createDateField();
        if (cutoffBox != null && cutoffBox.getSelectedItem() != null) {
            cutoffField.setText(cutoffBox.getSelectedItem().toString());
        }
        if (dateFromField != null) startField.setText(dateFromField.getText());
        if (dateToField != null) endField.setText(dateToField.getText());

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 6));
        form.add(new JLabel("Cutoff label"));
        form.add(cutoffField);
        form.add(new JLabel("Period start (yyyy-MM-dd)"));
        form.add(startField);
        form.add(new JLabel("Period end (yyyy-MM-dd)"));
        form.add(endField);
        form.add(WorkforceFormToolkit.createHelpLabel(
                "Opening a payroll period starts workforce review. Supervisors clear team issues, HR validates readiness, then Finance processes payroll."));

        int choice = JOptionPane.showConfirmDialog(this, form, "New Payroll Period",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            PayrollPeriod period = payrollPeriodLifecycleService.createDraftPeriod(
                    cutoffField.getText(),
                    LocalDate.parse(startField.getText().trim()),
                    LocalDate.parse(endField.getText().trim()));
            refreshCutoffOptions();
            selectPayrollPeriod(period);
            refreshAll();
            JOptionPane.showMessageDialog(this,
                    "Payroll period created as Draft. Activate Workforce Review when ready.",
                    "Payroll Period Created",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Payroll Period",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void activateWorkforceReview() {
        if (!roleAccessService.canGeneratePayroll(getCurrentRole())) {
            showAccessDeniedMessage("Only Finance users can activate workforce review.");
            return;
        }
        if (currentPayrollPeriod == null || currentPayrollPeriod.getPeriodId() <= 0) {
            JOptionPane.showMessageDialog(this,
                    "Create or select a payroll period before activating workforce review.",
                    "Payroll Period Required",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        runPayrollWorkflowAction("Opening workforce review...", () -> {
            currentWorkforceSummary = payrollPeriodLifecycleService.activateWorkforceReview(
                    currentPayrollPeriod.getPeriodId());
            return "Workforce review is open. Readiness rows were synchronized for the selected period.";
        }, "Workforce Review Activated");
    }

    private void showLockPayrollMessage() {
        JOptionPane.showMessageDialog(this,
                "Payroll run locking is visible in the lifecycle flow, but lock persistence is not enabled in this phase.",
                "Lock Payroll Run",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // ================= REFRESH ALL =================

    private void refreshAll() {
        if (cutoffBox.getItemCount() == 0 || cutoffBox.getSelectedItem() == null) {
            renderNoPayrollPeriodState();
            return;
        }

        updateEmptyState(false);

        String cutoff = cutoffBox.getSelectedItem().toString();
        String dateFrom = dateFromField.getText().trim();
        String dateTo = dateToField.getText().trim();
        setControlsEnabled(false);
        showPayrollLoadingState();

        SwingWorker<PayrollViewData, Void> worker = new SwingWorker<>() {
            private long startedAtMs;

            @Override
            protected PayrollViewData doInBackground() {
                startedAtMs = System.currentTimeMillis();
                return buildPayrollViewData(cutoff, dateFrom, dateTo);
            }

            @Override
            protected void done() {
                try {
                    PayrollViewData viewData = get();

                    updateTable(viewData.tableData);
                    currentReadinessReport = viewData.readinessReport;
                    currentLifecycleStatus = viewData.lifecycleStatus;
                    currentPayrollPeriod = viewData.payrollPeriod;
                    currentWorkforceSummary = viewData.workforceSummary;
                    currentEmployeesById = viewData.employeesById;
                    currentIssuesByEmployee = viewData.issuesByEmployee;
                    currentAllReadinessRows = viewData.allReadinessRows;
                    setControlsEnabled(true);
                    updateFinanceReadinessTable(viewData);
                    updatePayrollReadyTable(viewData);
                    updatePayrollActionAvailability(true);

                    summaryWrapper.removeAll();
                    summaryWrapper.add(createPayrollRunHeader(viewData), BorderLayout.CENTER);
                    summaryWrapper.revalidate();
                    summaryWrapper.repaint();

                    updateOperationsRail(viewData.readinessReport, viewData.lifecycleStatus);
                    System.out.println("[perf] PayrollPanel refreshAll took "
                            + (System.currentTimeMillis() - startedAtMs) + " ms");

                } catch (Exception ex) {
                    setControlsEnabled(true);
                    JOptionPane.showMessageDialog(PayrollPanel.this,
                            "An error occurred while loading payroll data.",
                            "Load Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private void showPayrollLoadingState() {
        currentFinanceQueue = List.of();
        currentAllReadinessRows = List.of();
        updateFinanceReadinessLoadingTable();
        updatePayrollReadyLoadingTable();
        if (table != null) {
            table.setModel(new DefaultTableModel(new Object[][]{
                    {"-", "Loading payroll register preview...", "-", "-", "-", "-", "-", "Loading"}
            }, new String[]{"Employee ID", "Employee", "Hours", "Gross", "Adjustments", "Deductions", "Net Pay", "Payroll Status"}) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            });
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            UITheme.setColumnWidths(table, 90, 220, 75, 125, 115, 125, 125, 150);
            enableUserColumnResizing(table);
        }
        summaryWrapper.removeAll();
        summaryWrapper.add(createLoadingHeader("Loading payroll governance pipeline..."), BorderLayout.CENTER);
        summaryWrapper.revalidate();
        summaryWrapper.repaint();
    }

    private JPanel createLoadingHeader(String message) {
        return UITheme.createSkeletonCard(message, 2);
    }

    private void renderNoPayrollPeriodState() {
        currentPayrollPeriod = null;
        currentWorkforceSummary = WorkforceReadinessSummary.empty(0);
        currentFinanceQueue = List.of();
        currentAllReadinessRows = List.of();
        currentEmployeesById = Map.of();
        currentIssuesByEmployee = Map.of();
        currentReadinessReport = new PayrollReadinessReport();
        currentLifecycleStatus = new PayrollLifecycleStatus("No payroll period",
                PayrollLifecycleStatus.Status.DRAFT, 0, "", "", "Not processed");

        updateEmptyState(false);
        updateNoPeriodReadinessTable();
        updateNoPeriodPayrollReadyTable();
        if (table != null) {
            table.setModel(new DefaultTableModel(new Object[][]{
                    {"-", "No payroll period opened", "-", "-", "-", "-", "-", "Create a payroll period"}
            }, new String[]{"Employee ID", "Employee", "Hours", "Gross", "Adjustments", "Deductions", "Net Pay", "Payroll Status"}) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            });
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            UITheme.setColumnWidths(table, 90, 220, 75, 125, 115, 125, 125, 150);
            enableUserColumnResizing(table);
        }

        summaryWrapper.removeAll();
        summaryWrapper.add(createNoPeriodHeader(), BorderLayout.CENTER);
        summaryWrapper.revalidate();
        summaryWrapper.repaint();

        updateOperationsRail(currentReadinessReport, currentLifecycleStatus);
        setControlsEnabled(true);
    }

    private JPanel createNoPeriodHeader() {
        JPanel panel = new JPanel(new BorderLayout(10, 2));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                new EmptyBorder(10, 12, 10, 12)
        ));
        JLabel title = new JLabel("No payroll period has been opened yet.");
        title.setFont(UITheme.FONT_BODY_BOLD);
        title.setForeground(UITheme.TEXT_PRIMARY);
        JLabel help = new JLabel("Create a payroll period to start workforce review.");
        help.setFont(UITheme.FONT_SMALL);
        help.setForeground(UITheme.TEXT_SECONDARY);
        panel.add(title, BorderLayout.NORTH);
        panel.add(help, BorderLayout.CENTER);
        return panel;
    }

    private void updateNoPeriodReadinessTable() {
        if (readinessTable == null) {
            return;
        }
        String[] cols = {"Employee", "Workforce Issue", "Severity", "Workflow Owner", "Status", "Recommended Action"};
        Object[][] data = {{
                "Finance Operations",
                "No payroll period has been opened yet",
                "Informational",
                "Finance",
                "Draft",
                "Create a payroll period, then activate workforce review."
        }};
        readinessTable.setModel(new DefaultTableModel(data, cols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        readinessTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        UITheme.setColumnWidths(readinessTable, 180, 250, 140, 170, 220, 340);
        enableUserColumnResizing(readinessTable);
    }

    private void updateNoPeriodPayrollReadyTable() {
        if (payrollReadyTable == null) {
            return;
        }
        payrollReadyTable.setModel(new DefaultTableModel(new Object[][]{
                {"-", "No payroll period opened", "-"}
        }, new String[]{"Employee", "Confirmed On", "Status"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        payrollReadyTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        UITheme.setColumnWidths(payrollReadyTable, 190, 160, 150);
    }

    @Override
    public void refreshData() {
        refreshCutoffOptions();
        refreshAll();
    }

    private void refreshCutoffOptions() {
        if (cutoffBox == null) {
            return;
        }

        Object selected = cutoffBox.getSelectedItem();
        List<String> cutoffs = getPayrollPeriodCutoffOptions();
        suppressCutoffEvents = true;
        try {
            cutoffBox.removeAllItems();
            for (String cutoff : cutoffs) {
                cutoffBox.addItem(cutoff);
            }
            if (selected != null) {
                cutoffBox.setSelectedItem(selected);
            }
            if (cutoffBox.getSelectedItem() == null && cutoffBox.getItemCount() > 0) {
                cutoffBox.setSelectedIndex(0);
            }
        } finally {
            suppressCutoffEvents = false;
        }
        syncDateFieldsToCutoff();
    }

    private void selectPayrollPeriod(PayrollPeriod period) {
        if (period == null || cutoffBox == null) {
            return;
        }
        cutoffBox.setSelectedItem(period.getCutoffPeriod());
        dateFromField.setText(period.getPeriodStart() == null ? "" : period.getPeriodStart().toString());
        dateToField.setText(period.getPeriodEnd() == null ? "" : period.getPeriodEnd().toString());
    }

    private void selectInitialPayrollPeriod() {
        PayrollPeriod latestActivePeriod = workforceReadinessService.findLatestActivePayrollPeriod();
        if (latestActivePeriod == null) {
            return;
        }

        suppressCutoffEvents = true;
        try {
            selectPayrollPeriod(latestActivePeriod);
        } finally {
            suppressCutoffEvents = false;
        }
        System.out.println("[payroll] Initial Finance period: " + latestActivePeriod.getCutoffPeriod());
    }

    private List<String> getPayrollPeriodCutoffOptions() {
        List<String> cutoffs = new ArrayList<>();
        for (PayrollPeriod period : payrollPeriodLifecycleService.getPayrollPeriods()) {
            if (period != null && period.getCutoffPeriod() != null && !cutoffs.contains(period.getCutoffPeriod())) {
                cutoffs.add(period.getCutoffPeriod());
            }
        }
        return cutoffs;
    }

    private PayrollPeriod findPeriodByCutoffLabel(String cutoffPeriod) {
        if (cutoffPeriod == null) {
            return null;
        }
        for (PayrollPeriod period : payrollPeriodLifecycleService.getPayrollPeriods()) {
            if (period != null && cutoffPeriod.equals(period.getCutoffPeriod())) {
                return period;
            }
        }
        return null;
    }

    private PayrollPeriod resolvePayrollPeriod(String cutoffPeriod, String dateFrom, String dateTo) {
        try {
            PayrollPeriod exact = payrollPeriodLifecycleService.findPeriod(
                    cutoffPeriod, LocalDate.parse(dateFrom), LocalDate.parse(dateTo));
            return exact == null ? findPeriodByCutoffLabel(cutoffPeriod) : exact;
        } catch (Exception ex) {
            return findPeriodByCutoffLabel(cutoffPeriod);
        }
    }

    private PayrollViewData buildPayrollViewData(String cutoffPeriod, String dateFrom, String dateTo) {
        long startedAtMs = System.currentTimeMillis();
        long employeeStartMs = System.currentTimeMillis();
        List<Employee> list = payrollService.getEmployees();
        System.out.println("[perf] PayrollPanel employee load took "
                + (System.currentTimeMillis() - employeeStartMs) + " ms");
        Map<Integer, Employee> employeesById = new LinkedHashMap<>();
        for (Employee employee : list) {
            if (employee != null) {
                employeesById.put(employee.getEmployeeId(), employee);
            }
        }
        NumberFormat peso = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));

        Object[][] data = new Object[list.size()][8];
        PayrollSummaryData summary = new PayrollSummaryData();
        PayrollLifecycleStatus lifecycleStatus = payrollLifecycleService.getLifecycleStatus(
                cutoffPeriod, dateFrom, dateTo);
        PayrollPeriod payrollPeriod = resolvePayrollPeriod(cutoffPeriod, dateFrom, dateTo);
        long summaryStartMs = System.currentTimeMillis();
        WorkforceReadinessSummary workforceSummary = payrollPeriod == null
                ? WorkforceReadinessSummary.empty(0)
                : workforceReadinessService.getReadinessSummary(payrollPeriod.getPeriodId());
        if (payrollPeriod != null
                && payrollPeriodLifecycleService.isActiveForReadiness(payrollPeriod)
                && workforceSummary.getTotalEmployees() == 0) {
            workforceSummary = workforceReadinessService.syncReadinessForPeriod(payrollPeriod);
        }
        System.out.println("[perf] PayrollPanel stage count computation took "
                + (System.currentTimeMillis() - summaryStartMs) + " ms");
        Map<Integer, List<PayrollReadinessIssue>> issuesByEmployee = Map.of();
        long financeQueueStartMs = System.currentTimeMillis();
        List<WorkforcePayrollReadiness> financeQueue = payrollPeriod == null
                ? List.of()
                : workforceReadinessService.getFinanceValidationQueue(payrollPeriod.getPeriodId());
        System.out.println("[perf] PayrollPanel finance queue load took "
                + (System.currentTimeMillis() - financeQueueStartMs) + " ms");
        List<WorkforcePayrollReadiness> allReadinessRows = payrollPeriod == null
                ? List.of()
                : workforceReadinessService.getEmployeeReadinessList(payrollPeriod.getPeriodId());
        Map<Integer, WorkforcePayrollReadiness> readinessByEmployee = new LinkedHashMap<>();
        for (WorkforcePayrollReadiness readiness : allReadinessRows) {
            readinessByEmployee.put(readiness.getEmployeeId(), readiness);
        }
        long registerStartMs = System.currentTimeMillis();
        PayrollReadinessReport readinessReport = payrollReadinessService.evaluateReadiness(
                LocalDate.parse(dateFrom), LocalDate.parse(dateTo));

        for (int i = 0; i < list.size(); i++) {
            Employee e = list.get(i);

            double hours = payrollService.getHoursForDateRange(e.getEmployeeId(), dateFrom, dateTo);
            PayrollRecord record = payrollService.processPayrollForEmployee(e, hours);

            data[i][0] = e.getEmployeeId();
            data[i][1] = e.getFirstName() + " " + e.getLastName();
            data[i][2] = String.format("%.1f", hours);
            data[i][3] = peso.format(record.getGross());
            data[i][4] = peso.format(0.0);
            data[i][5] = peso.format(record.getTotalDeductions());
            data[i][6] = peso.format(record.getNet());
            data[i][7] = formatRegisterStatus(lifecycleStatus, readinessByEmployee.get(e.getEmployeeId()));

            summary.employeeCount++;
            summary.totalGross += record.getGross();
            summary.totalDeductions += record.getTotalDeductions();
            summary.totalNet += record.getNet();
            summary.totalSSS += record.getSss();
            summary.totalPhilhealth += record.getPhilhealth();
            summary.totalPagibig += record.getPagibig();
            summary.totalTax += record.getTax();
        }
        System.out.println("[perf] PayrollPanel register preview load took "
                + (System.currentTimeMillis() - registerStartMs) + " ms");
        System.out.println("[perf] PayrollPanel buildPayrollViewData took "
                + (System.currentTimeMillis() - startedAtMs) + " ms");

        return new PayrollViewData(data, summary, readinessReport, lifecycleStatus, payrollPeriod, workforceSummary,
                financeQueue, allReadinessRows, employeesById, issuesByEmployee);
    }

    private JPanel createOperationsRail(PayrollReadinessReport report, PayrollLifecycleStatus lifecycleStatus) {
        JPanel rail = new JPanel();
        rail.setOpaque(false);
        rail.setLayout(new BoxLayout(rail, BoxLayout.Y_AXIS));
        PayrollReadinessReport safeReport = report == null ? new PayrollReadinessReport() : report;
        PayrollLifecycleStatus safeLifecycle = lifecycleStatus == null
                ? new PayrollLifecycleStatus(selectedCutoff(), PayrollLifecycleStatus.Status.DRAFT, 0,
                        dateFromField == null ? "" : dateFromField.getText(),
                        dateToField == null ? "" : dateToField.getText(),
                        "Not processed")
                : lifecycleStatus;

        dependencyWrapper = new JPanel(new BorderLayout());
        dependencyWrapper.setOpaque(false);
        dependencyWrapper.setAlignmentX(LEFT_ALIGNMENT);
        dependencyWrapper.add(createDependencyPanel(safeReport), BorderLayout.CENTER);

        lifecycleWrapper = new JPanel(new BorderLayout());
        lifecycleWrapper.setOpaque(false);
        lifecycleWrapper.setAlignmentX(LEFT_ALIGNMENT);
        lifecycleWrapper.add(createLifecycleFlowPanel(safeLifecycle, safeReport), BorderLayout.CENTER);

        blockerWrapper = new JPanel(new BorderLayout());
        blockerWrapper.setOpaque(false);
        blockerWrapper.setAlignmentX(LEFT_ALIGNMENT);
        updateBlockingHelper(safeReport);

        JPanel actions = createButtonPanel();
        actions.setAlignmentX(LEFT_ALIGNMENT);

        rail.add(dependencyWrapper);
        rail.add(Box.createVerticalStrut(10));
        rail.add(lifecycleWrapper);
        rail.add(Box.createVerticalStrut(10));
        rail.add(blockerWrapper);
        rail.add(Box.createVerticalStrut(10));
        rail.add(actions);
        return rail;
    }

    private void updateOperationsRail(PayrollReadinessReport report, PayrollLifecycleStatus lifecycleStatus) {
        if (dependencyWrapper == null || lifecycleWrapper == null || blockerWrapper == null) {
            return;
        }
        PayrollReadinessReport safeReport = report == null ? new PayrollReadinessReport() : report;
        PayrollLifecycleStatus safeLifecycle = lifecycleStatus == null
                ? new PayrollLifecycleStatus(selectedCutoff(), PayrollLifecycleStatus.Status.DRAFT, 0,
                        dateFromField == null ? "" : dateFromField.getText(),
                        dateToField == null ? "" : dateToField.getText(),
                        "Not processed")
                : lifecycleStatus;

        dependencyWrapper.removeAll();
        dependencyWrapper.add(createDependencyPanel(safeReport), BorderLayout.CENTER);
        dependencyWrapper.revalidate();
        dependencyWrapper.repaint();

        lifecycleWrapper.removeAll();
        lifecycleWrapper.add(createLifecycleFlowPanel(safeLifecycle, safeReport), BorderLayout.CENTER);
        lifecycleWrapper.revalidate();
        lifecycleWrapper.repaint();

        updateBlockingHelper(safeReport);
    }

    private void updateBlockingHelper(PayrollReadinessReport report) {
        if (blockerWrapper == null) {
            return;
        }
        blockerWrapper.removeAll();
        if (report.getBlockedCount() > 0) {
            blockerWrapper.add(createBlockingHelper(), BorderLayout.CENTER);
        }
        blockerWrapper.setVisible(report.getBlockedCount() > 0);
        blockerWrapper.revalidate();
        blockerWrapper.repaint();
    }

    private JLabel createBlockingHelper() {
        JLabel label = new JLabel("Resolve workforce blocking issues before payroll processing.");
        label.setFont(UITheme.FONT_SMALL);
        label.setForeground(UITheme.DANGER);
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(254, 202, 202), 1),
                new EmptyBorder(7, 9, 7, 9)
        ));
        label.setOpaque(true);
        label.setBackground(new Color(254, 242, 242));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private JPanel createDependencyPanel(PayrollReadinessReport report) {
        JPanel panel = createRailSection("Workforce Dependencies");
        WorkforceReadinessSummary summary = currentWorkforceSummary == null
                ? WorkforceReadinessSummary.empty(currentPayrollPeriod == null ? 0 : currentPayrollPeriod.getPeriodId())
                : currentWorkforceSummary;
        panel.add(createChecklistRow("Readiness Rows", String.valueOf(summary.getTotalEmployees()),
                summary.getTotalEmployees() > 0 ? UITheme.BLUE : UITheme.TEXT_SECONDARY));
        panel.add(createChecklistRow("Supervisor Review Pending", String.valueOf(summary.getSupervisorReviewPendingCount()),
                summary.getSupervisorReviewPendingCount() > 0 ? UITheme.YELLOW : UITheme.SUCCESS));
        panel.add(createChecklistRow("HR Validation Pending", String.valueOf(summary.getHrValidationPendingCount()),
                summary.getHrValidationPendingCount() > 0 ? UITheme.YELLOW : UITheme.SUCCESS));
        panel.add(createChecklistRow("Finance Validation Pending", String.valueOf(summary.getFinanceValidationPendingCount()),
                summary.getFinanceValidationPendingCount() > 0 ? UITheme.YELLOW : UITheme.SUCCESS));
        panel.add(createChecklistRow("Payroll-Ready Employees", String.valueOf(summary.getPayrollReadyCount()),
                summary.getPayrollReadyCount() > 0 ? UITheme.SUCCESS : UITheme.TEXT_SECONDARY));
        panel.add(createChecklistRow("Payroll Blocking Issues", String.valueOf(report.getBlockedCount()),
                report.getBlockedCount() > 0 ? UITheme.DANGER : UITheme.SUCCESS));
        panel.add(createChecklistRow("Awaiting HR Workforce Resolution", String.valueOf(report.getNeedsReviewCount()),
                report.getNeedsReviewCount() > 0 ? UITheme.YELLOW : UITheme.SUCCESS));
        panel.add(createChecklistRow("Pending Attendance Corrections", String.valueOf(countIssues(report, "adjustment")),
                countIssues(report, "adjustment") > 0 ? UITheme.YELLOW : UITheme.SUCCESS));
        panel.add(createChecklistRow("Pending Overtime Approval", String.valueOf(countIssues(report, "overtime")),
                countIssues(report, "overtime") > 0 ? UITheme.YELLOW : UITheme.SUCCESS));
        panel.add(createChecklistRow("Schedule Assignment Gaps", String.valueOf(countIssues(report, "schedule")),
                countIssues(report, "schedule") > 0 ? UITheme.YELLOW : UITheme.SUCCESS));
        return panel;
    }

    private JPanel createLifecycleFlowPanel(PayrollLifecycleStatus lifecycleStatus, PayrollReadinessReport report) {
        JPanel panel = createRailSection("Payroll Lifecycle Flow");
        JPanel flow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        flow.setOpaque(false);
        String[] steps = {"Draft", "Workforce Review", "HR Finalization", "Finance Validation", "Processed", "Locked"};
        int activeIndex = activeLifecycleIndex(lifecycleStatus, report);
        for (int i = 0; i < steps.length; i++) {
            flow.add(createLifecycleStep(steps[i], i, activeIndex));
            if (i < steps.length - 1) {
                JLabel arrow = new JLabel("→");
                arrow.setFont(UITheme.FONT_SMALL);
                arrow.setForeground(UITheme.TEXT_SECONDARY);
                flow.add(arrow);
            }
        }
        panel.add(flow);
        return panel;
    }

    private JPanel createRailSection(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                new EmptyBorder(8, 10, 8, 10)
        ));
        JLabel label = new JLabel(title);
        label.setFont(UITheme.FONT_BODY_BOLD);
        label.setForeground(UITheme.TEXT_PRIMARY);
        label.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createVerticalStrut(6));
        return panel;
    }

    private JPanel createChecklistRow(String label, String value, Color accent) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(3, 0, 3, 0));
        JLabel name = new JLabel(label);
        name.setFont(UITheme.FONT_SMALL);
        name.setForeground(UITheme.TEXT_SECONDARY);
        JLabel count = new JLabel(value);
        count.setFont(UITheme.FONT_SMALL);
        count.setForeground(accent);
        row.add(name, BorderLayout.WEST);
        row.add(count, BorderLayout.EAST);
        row.setAlignmentX(LEFT_ALIGNMENT);
        return row;
    }

    private JLabel createLifecycleStep(String text, int index, int activeIndex) {
        boolean active = index == activeIndex;
        boolean completed = index < activeIndex;
        JLabel label = new JLabel(text);
        label.setFont(UITheme.FONT_SMALL);
        label.setForeground(active ? Color.WHITE : (completed ? UITheme.SUCCESS : UITheme.TEXT_SECONDARY));
        label.setOpaque(true);
        label.setBackground(active ? UITheme.ACCENT : new Color(248, 250, 252));
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(active ? UITheme.ACCENT : (completed ? UITheme.SUCCESS : UITheme.BORDER), 1),
                new EmptyBorder(4, 6, 4, 6)
        ));
        return label;
    }

    private int activeLifecycleIndex(PayrollLifecycleStatus lifecycleStatus, PayrollReadinessReport report) {
        if (lifecycleStatus.getStatus() == PayrollLifecycleStatus.Status.LOCKED) return 5;
        if (lifecycleStatus.getStatus() == PayrollLifecycleStatus.Status.PROCESSED) return 4;
        if (lifecycleStatus.getStatus() == PayrollLifecycleStatus.Status.PROCESSING) return 4;
        if (lifecycleStatus.getStatus() == PayrollLifecycleStatus.Status.READY_FOR_PROCESSING) return 3;
        if (lifecycleStatus.getStatus() == PayrollLifecycleStatus.Status.READY_FOR_FINANCE_VALIDATION) return 3;
        if (lifecycleStatus.getStatus() == PayrollLifecycleStatus.Status.READY_FOR_HR_VALIDATION) return 2;
        if (lifecycleStatus.getStatus() == PayrollLifecycleStatus.Status.OPEN_WORKFORCE_REVIEW) return 1;
        if (report.getStatus() == PayrollReadinessReport.Status.READY) return 3;
        if (report.getNeedsReviewCount() > 0) return 2;
        if (report.getBlockedCount() > 0) return 1;
        return 0;
    }

    private int countIssues(PayrollReadinessReport report, String token) {
        int count = 0;
        String normalizedToken = token == null ? "" : token.toLowerCase(Locale.ENGLISH);
        for (PayrollReadinessIssue issue : report.getIssues()) {
            String combined = (issue.getIssue() + " " + issue.getRecommendedAction()).toLowerCase(Locale.ENGLISH);
            if (combined.contains(normalizedToken)) {
                count++;
            }
        }
        return count;
    }

    private void updateReadinessTable(PayrollReadinessReport report) {
        String[] cols = {"Employee", "Workforce Issue", "Severity", "Workflow Owner", "Status", "Recommended Action"};
        List<SummarizedReadinessIssue> issues = summarizeReadinessIssues(report.getIssues());
        Object[][] data = new Object[Math.max(1, issues.size())][6];

        if (issues.isEmpty()) {
            data[0][0] = "All employees";
            data[0][1] = "Payroll data validated";
            data[0][2] = "Validated";
            data[0][3] = "Finance";
            data[0][4] = "Ready for Payroll Processing";
            data[0][5] = "No workforce readiness issues found for this period.";
        } else {
            for (int i = 0; i < issues.size(); i++) {
                SummarizedReadinessIssue issue = issues.get(i);
                data[i][0] = issue.getEmployeeName();
                data[i][1] = issue.getIssue();
                data[i][2] = formatSeverity(issue.getSeverity());
                data[i][3] = workflowOwnerForIssue(issue);
                data[i][4] = operationalStatusForIssue(issue);
                data[i][5] = issue.getRecommendedAction();
            }
        }

        readinessTable.setModel(new DefaultTableModel(data, cols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        readinessTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        UITheme.setColumnWidths(readinessTable, 180, 250, 140, 170, 220, 340);
    }

    private void updateFinanceReadinessTable(PayrollViewData viewData) {
        String[] cols = {"Employee ID", "Employee", "Position", "Department", "Readiness Status",
                "HR Validation Status", "Payroll Issue Count", "Current Owner", "Recommended Action", "Payroll Impact"};
        List<WorkforcePayrollReadiness> queue = viewData == null ? List.of() : viewData.financeQueue.stream()
                .filter(this::isFinanceOwnedStatus)
                .filter(readiness -> !isPayrollReadyStatus(readiness))
                .toList();
        currentFinanceQueue = queue;
        Object[][] data = new Object[Math.max(1, queue.size())][cols.length];

        if (queue.isEmpty()) {
            data[0][0] = "-";
            data[0][1] = "No employees awaiting finance validation.";
            data[0][2] = "-";
            data[0][3] = "-";
            data[0][4] = currentPayrollPeriod == null ? "No Active Period" : "Empty";
            data[0][5] = currentPayrollPeriod == null ? "-" : "Waiting for HR endorsement";
            data[0][6] = "-";
            data[0][7] = "Finance";
            data[0][8] = currentPayrollPeriod == null
                    ? "Create and activate a payroll period."
                    : "No actionable Finance-owned employees.";
            data[0][9] = "-";
        } else {
            for (int i = 0; i < queue.size(); i++) {
                WorkforcePayrollReadiness readiness = queue.get(i);
                Employee employee = viewData.employeesById.get(readiness.getEmployeeId());
                List<PayrollReadinessIssue> issues = viewData.issuesByEmployee.getOrDefault(
                        readiness.getEmployeeId(), List.of());
                data[i][0] = readiness.getEmployeeId();
                data[i][1] = employeeName(employee, readiness.getEmployeeId());
                data[i][2] = safeText(employee == null ? null : employee.getPosition(), "-");
                data[i][3] = "-";
                data[i][4] = formatReadinessLifecycle(readiness.getReadinessStatus());
                data[i][5] = formatHrValidationStatus(readiness);
                data[i][6] = issues.size();
                data[i][7] = formatOwner(readiness.getCurrentOwnerRole());
                data[i][8] = recommendedFinanceAction(readiness, issues);
                data[i][9] = payrollImpact(readiness, issues);
            }
        }

        readinessTable.setModel(new DefaultTableModel(data, cols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        readinessTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        UITheme.setColumnWidths(readinessTable, 90, 180, 160, 110, 190, 170, 125, 140, 230, 180);
        enableUserColumnResizing(readinessTable);
        configureFinanceQueueSelection(!queue.isEmpty());
        if (!queue.isEmpty() && focusedRegisterEmployeeId != null) {
            selectFinanceQueueEmployee(focusedRegisterEmployeeId);
            selectRegisterPreviewEmployee(focusedRegisterEmployeeId);
        }
        updatePayrollActionAvailability(true);
    }

    private void updatePayrollReadyTable(PayrollViewData viewData) {
        if (payrollReadyTable == null) {
            return;
        }
        String[] cols = {"Employee", "Confirmed On", "Status"};
        List<WorkforcePayrollReadiness> readyQueue = viewData == null
                ? List.of()
                : getProcessableReadyRows(viewData.allReadinessRows);
        Object[][] data = new Object[Math.max(1, readyQueue.size())][cols.length];
        if (readyQueue.isEmpty()) {
            data[0][0] = "-";
            data[0][1] = "No Finance-confirmed employees yet.";
            data[0][2] = "Waiting";
        } else {
            for (int i = 0; i < readyQueue.size(); i++) {
                WorkforcePayrollReadiness readiness = readyQueue.get(i);
                Employee employee = viewData.employeesById.get(readiness.getEmployeeId());
                data[i][0] = readiness.getEmployeeId() + " - " + employeeName(employee, readiness.getEmployeeId());
                data[i][1] = readiness.getFinanceValidatedAt() == null ? "-" : readiness.getFinanceValidatedAt();
                data[i][2] = "Payroll Ready";
            }
        }
        payrollReadyTable.setModel(new DefaultTableModel(data, cols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        payrollReadyTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        UITheme.setColumnWidths(payrollReadyTable, 190, 160, 150);
    }

    private boolean isPayrollReadyStatus(WorkforcePayrollReadiness readiness) {
        if (readiness == null || readiness.getReadinessStatus() == null) {
            return false;
        }
        String status = readiness.getReadinessStatus().trim().toUpperCase(Locale.ROOT);
        return WorkforcePayrollReadiness.STATUS_FINANCE_VALIDATED.equals(status)
                || WorkforcePayrollReadiness.STATUS_PAYROLL_PROCESSED.equals(status)
                || WorkforcePayrollReadiness.STATUS_PAYROLL_LOCKED.equals(status);
    }

    private boolean isProcessablePayrollReadyStatus(WorkforcePayrollReadiness readiness) {
        if (readiness == null || readiness.getReadinessStatus() == null || readiness.getCurrentOwnerRole() == null) {
            return false;
        }
        String owner = readiness.getCurrentOwnerRole().trim().toUpperCase(Locale.ROOT);
        String status = readiness.getReadinessStatus().trim().toUpperCase(Locale.ROOT);
        return WorkforcePayrollReadiness.OWNER_FINANCE.equals(owner)
                && WorkforcePayrollReadiness.STATUS_FINANCE_VALIDATED.equals(status);
    }

    private boolean isFinanceOwnedStatus(WorkforcePayrollReadiness readiness) {
        if (readiness == null || readiness.getReadinessStatus() == null || readiness.getCurrentOwnerRole() == null) {
            return false;
        }
        String owner = readiness.getCurrentOwnerRole().trim().toUpperCase(Locale.ROOT);
        String status = readiness.getReadinessStatus().trim().toUpperCase(Locale.ROOT);
        return WorkforcePayrollReadiness.OWNER_FINANCE.equals(owner)
                && (WorkforcePayrollReadiness.STATUS_ENDORSED_TO_FINANCE.equals(status)
                || WorkforcePayrollReadiness.STATUS_FINANCE_VALIDATED.equals(status)
                || WorkforcePayrollReadiness.STATUS_PAYROLL_PROCESSED.equals(status)
                || WorkforcePayrollReadiness.STATUS_PAYROLL_LOCKED.equals(status));
    }

    private boolean isConfirmableFinanceReadiness(WorkforcePayrollReadiness readiness) {
        if (readiness == null || readiness.getReadinessStatus() == null || readiness.getCurrentOwnerRole() == null) {
            return false;
        }
        String owner = readiness.getCurrentOwnerRole().trim().toUpperCase(Locale.ROOT);
        String status = readiness.getReadinessStatus().trim().toUpperCase(Locale.ROOT);
        return WorkforcePayrollReadiness.OWNER_FINANCE.equals(owner)
                && WorkforcePayrollReadiness.STATUS_ENDORSED_TO_FINANCE.equals(status);
    }

    private void enableUserColumnResizing(JTable targetTable) {
        if (targetTable == null || targetTable.getTableHeader() == null) {
            return;
        }
        targetTable.getTableHeader().setResizingAllowed(true);
        targetTable.getTableHeader().setReorderingAllowed(false);
        targetTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        targetTable.setShowVerticalLines(true);
        targetTable.getColumnModel().setColumnMargin(6);
        for (int i = 0; i < targetTable.getColumnModel().getColumnCount(); i++) {
            TableColumn column = targetTable.getColumnModel().getColumn(i);
            column.setResizable(true);
            column.setMinWidth(45);
            column.setMaxWidth(Integer.MAX_VALUE);
        }
    }

    private void installRegisterStatusRenderer(JTable targetTable) {
        if (targetTable == null) {
            return;
        }
        targetTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    String status = "";
                    int modelRow = table.convertRowIndexToModel(row);
                    if (modelRow >= 0 && table.getModel().getColumnCount() > 7) {
                        Object statusValue = table.getModel().getValueAt(modelRow, 7);
                        status = statusValue == null ? "" : statusValue.toString();
                    }
                    c.setBackground(registerStatusColor(status, row));
                    c.setForeground(UITheme.TEXT_PRIMARY);
                }
                if (c instanceof JComponent component) {
                    component.setBorder(new EmptyBorder(0, 14, 0, 14));
                    component.setToolTipText(value == null ? "" : value.toString());
                }
                return c;
            }
        });
    }

    private Color registerStatusColor(String status, int row) {
        String normalized = status == null ? "" : status.toLowerCase(Locale.ENGLISH);
        if (normalized.contains("blocked") || normalized.contains("not ready")
                || normalized.contains("pending supervisor")) {
            return new Color(254, 242, 242);
        }
        if (normalized.contains("waiting hr") || normalized.contains("hr validation")) {
            return new Color(255, 251, 235);
        }
        if (normalized.contains("finance review") || normalized.contains("endorsed")) {
            return new Color(239, 246, 255);
        }
        if (normalized.contains("payroll ready")) {
            return new Color(236, 253, 245);
        }
        if (normalized.contains("processed") || normalized.contains("locked")) {
            return new Color(248, 250, 252);
        }
        return row % 2 == 0 ? Color.WHITE : UITheme.TABLE_ALT_ROW;
    }

    private void updateFinanceReadinessLoadingTable() {
        if (readinessTable == null) {
            return;
        }
        String[] cols = {"Employee ID", "Employee", "Position", "Department", "Readiness Status",
                "HR Validation Status", "Payroll Issue Count", "Current Owner", "Recommended Action", "Payroll Impact"};
        Object[][] data = {
                {
                "-",
                "Loading Finance Payroll Validation Queue...",
                "-",
                "-",
                "Loading",
                "Loading",
                "-",
                "Finance",
                "Please wait",
                "Loading"
                },
                {"-", "Preparing workforce rows...", "-", "-", "Loading", "Loading", "-", "Finance", "Please wait", "Loading"},
                {"-", "Checking Finance-owned items...", "-", "-", "Loading", "Loading", "-", "Finance", "Please wait", "Loading"}
        };
        readinessTable.setModel(new DefaultTableModel(data, cols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        readinessTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        UITheme.setColumnWidths(readinessTable, 90, 180, 160, 110, 190, 170, 125, 140, 230, 180);
        enableUserColumnResizing(readinessTable);
        configureFinanceQueueSelection(false);
    }

    private void updatePayrollReadyLoadingTable() {
        if (payrollReadyTable == null) {
            return;
        }
        payrollReadyTable.setModel(new DefaultTableModel(new Object[][]{
                {"-", "Loading payroll-ready queue...", "Loading"},
                {"-", "Preparing confirmed rows...", "Loading"}
        }, new String[]{"Employee", "Confirmed On", "Status"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        payrollReadyTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        UITheme.setColumnWidths(payrollReadyTable, 190, 160, 150);
    }

    private List<SummarizedReadinessIssue> summarizeReadinessIssues(List<PayrollReadinessIssue> issues) {
        Map<String, SummarizedReadinessIssue> grouped = new LinkedHashMap<>();
        List<SummarizedReadinessIssue> rows = new ArrayList<>();

        for (PayrollReadinessIssue issue : issues) {
            String status = extractReadinessStatus(issue.getIssue());
            if ("No Assigned Schedule".equals(status)) {
                String key = issue.getEmployeeId() + "|" + status + "|" + issue.getRecommendedAction();
                SummarizedReadinessIssue existing = grouped.get(key);
                if (existing == null) {
                    existing = new SummarizedReadinessIssue(
                            issue.getEmployeeName(),
                            "1 schedule assignment gap",
                            issue.getSeverity(),
                            issue.getRecommendedAction(),
                            1
                    );
                    grouped.put(key, existing);
                    rows.add(existing);
                } else {
                    existing.incrementScheduleGapCount();
                }
                continue;
            }

            rows.add(new SummarizedReadinessIssue(
                    issue.getEmployeeName(),
                    issue.getIssue(),
                    issue.getSeverity(),
                    issue.getRecommendedAction(),
                    1
            ));
        }

        return rows;
    }

    private String extractReadinessStatus(String issue) {
        if (issue == null) {
            return "";
        }

        int separator = issue.indexOf(": ");
        return separator >= 0 ? issue.substring(separator + 2).trim() : issue.trim();
    }

    private void setControlsEnabled(boolean enabled) {
        if (cutoffBox != null) cutoffBox.setEnabled(enabled);
        if (dateFromField != null) dateFromField.setEnabled(enabled);
        if (dateToField != null) dateToField.setEnabled(enabled);
        if (refreshBtn != null) refreshBtn.setEnabled(enabled);
        if (newPeriodBtn != null) newPeriodBtn.setEnabled(enabled && roleAccessService.canGeneratePayroll(getCurrentRole()));
        if (activateReviewBtn != null) activateReviewBtn.setEnabled(enabled && canOpenWorkforceReview());
        if (lockBtn != null) lockBtn.setEnabled(enabled && currentLifecycleStatus != null
                && currentLifecycleStatus.isProcessed() && !hasPayrollBlockers());
        updatePayrollActionAvailability(enabled);
    }

    private void updateEmptyState(boolean showEmpty) {
        emptyStateLabel.setVisible(showEmpty);

        if (centerCardLayout != null && centerPanel != null) {
            centerCardLayout.show(centerPanel, showEmpty ? "empty" : "table");
        }

        summaryWrapper.setVisible(!showEmpty);
        if (operationsRailWrapper != null) operationsRailWrapper.setVisible(!showEmpty);

        if (refreshBtn != null) refreshBtn.setEnabled(!showEmpty);
        if (newPeriodBtn != null) newPeriodBtn.setEnabled(roleAccessService.canGeneratePayroll(getCurrentRole()));
        if (activateReviewBtn != null) activateReviewBtn.setEnabled(!showEmpty && canOpenWorkforceReview());
        if (lockBtn != null) lockBtn.setEnabled(!showEmpty && currentLifecycleStatus != null
                && currentLifecycleStatus.isProcessed() && !hasPayrollBlockers());
        updatePayrollActionAvailability(!showEmpty);
    }

    private void updatePayrollActionAvailability(boolean enabled) {
        boolean financeUser = roleAccessService.canGeneratePayroll(getCurrentRole());
        WorkforcePayrollReadiness selected = getSelectedFinanceReadiness();
        boolean reviewable = enabled && selected != null;
        if (reviewBtn != null) {
            reviewBtn.setEnabled(reviewable);
            reviewBtn.setToolTipText(reviewable
                    ? "Review the selected Finance validation employee."
                    : "Select an employee in the Finance Payroll Validation Queue first.");
        }
        boolean confirmable = enabled && financeUser && canModifyFinanceReadiness()
                && isConfirmableFinanceReadiness(selected);
        if (validateBtn != null) {
            validateBtn.setEnabled(confirmable);
            validateBtn.setToolTipText(confirmable
                    ? "Confirm the selected HR-endorsed employee as payroll-ready."
                    : "Select a Finance-owned HR-endorsed employee first.");
        }
        boolean confirmAllEligible = enabled && financeUser && canModifyFinanceReadiness() && currentFinanceQueue != null
                && currentFinanceQueue.stream().anyMatch(this::isConfirmableFinanceReadiness);
        if (confirmAllBtn != null) {
            confirmAllBtn.setEnabled(confirmAllEligible);
            confirmAllBtn.setToolTipText(confirmAllEligible
                    ? "Confirm all eligible HR-endorsed employees as payroll-ready."
                    : "No eligible Finance-owned HR-endorsed employees.");
        }

        String processDisabledReason = getProcessPayrollDisabledReason();
        boolean canProcess = enabled && financeUser && processDisabledReason == null;
        if (processBtn != null) {
            processBtn.setEnabled(canProcess);
            processBtn.setToolTipText(canProcess
                    ? "Processes only employees confirmed as Payroll Ready."
                    : processDisabledReason == null
                            ? "No payroll-ready employees are available for processing."
                            : processDisabledReason);
        }
    }

    private boolean hasPayrollBlockers() {
        return currentReadinessReport != null && currentReadinessReport.getBlockedCount() > 0;
    }

    private boolean canModifyFinanceReadiness() {
        return currentLifecycleStatus == null
                || !(currentLifecycleStatus.getStatus() == PayrollLifecycleStatus.Status.PROCESSED
                || currentLifecycleStatus.getStatus() == PayrollLifecycleStatus.Status.LOCKED
                || currentLifecycleStatus.getStatus() == PayrollLifecycleStatus.Status.PROCESSING);
    }

    private boolean canOpenWorkforceReview() {
        if (!roleAccessService.canGeneratePayroll(getCurrentRole())
                || currentPayrollPeriod == null
                || currentPayrollPeriod.getPeriodId() <= 0) {
            return false;
        }
        String status = payrollPeriodLifecycleService.normalizeStatus(currentPayrollPeriod.getStatus());
        return !PayrollPeriodLifecycleService.STATUS_LOCKED.equals(status)
                && !PayrollPeriodLifecycleService.STATUS_PROCESSED.equals(status)
                && !PayrollPeriodLifecycleService.STATUS_PROCESSING.equals(status);
    }

    private String getProcessPayrollDisabledReason() {
        if (currentPayrollPeriod == null || currentPayrollPeriod.getPeriodId() <= 0) {
            return "Create and activate a payroll period before payroll processing.";
        }
        String periodStatus = payrollPeriodLifecycleService.normalizeStatus(currentPayrollPeriod.getStatus());
        if (PayrollPeriodLifecycleService.STATUS_LOCKED.equals(periodStatus)
                || PayrollPeriodLifecycleService.STATUS_PROCESSED.equals(periodStatus)) {
            return "Payroll has already been finalized for this period.";
        }
        if (getProcessableReadyRows().isEmpty()) {
            return "No payroll-ready employees are available for processing.";
        }
        return null;
    }

    private List<WorkforcePayrollReadiness> getProcessableReadyRows() {
        return getProcessableReadyRows(currentAllReadinessRows);
    }

    private List<WorkforcePayrollReadiness> getProcessableReadyRows(List<WorkforcePayrollReadiness> readinessRows) {
        return readinessRows == null ? List.of() : readinessRows.stream()
                .filter(this::isProcessablePayrollReadyStatus)
                .toList();
    }

    private WorkforcePayrollReadiness getSelectedFinanceReadiness() {
        if (readinessTable == null || currentFinanceQueue == null || currentFinanceQueue.isEmpty()) {
            return null;
        }
        int selectedRow = readinessTable.getSelectedRow();
        if (selectedRow < 0) {
            return null;
        }
        int modelRow = readinessTable.convertRowIndexToModel(selectedRow);
        Integer selectedEmployeeId = getFinanceQueueEmployeeIdAtModelRow(modelRow);
        return findFinanceReadinessByEmployeeId(selectedEmployeeId);
    }

    private void syncRegisterSelectionToFinanceQueue() {
        WorkforcePayrollReadiness selected = getSelectedFinanceReadiness();
        if (selected == null) {
            return;
        }
        focusedRegisterEmployeeId = selected.getEmployeeId();
        selectRegisterPreviewEmployee(selected.getEmployeeId());
    }

    private void selectRegisterPreviewEmployee(int employeeId) {
        if (table == null || table.getModel() == null) {
            return;
        }
        for (int modelRow = 0; modelRow < table.getModel().getRowCount(); modelRow++) {
            Object value = table.getModel().getValueAt(modelRow, 0);
            if (value != null && String.valueOf(employeeId).equals(value.toString())) {
                int viewRow = table.convertRowIndexToView(modelRow);
                if (viewRow >= 0) {
                    table.getSelectionModel().setSelectionInterval(viewRow, viewRow);
                    Rectangle rect = table.getCellRect(viewRow, 0, true);
                    table.scrollRectToVisible(rect);
                }
                return;
            }
        }
    }

    private boolean selectFinanceQueueEmployee(int employeeId) {
        if (readinessTable == null || readinessTable.getModel() == null) {
            return false;
        }
        for (int modelRow = 0; modelRow < readinessTable.getModel().getRowCount(); modelRow++) {
            Integer rowEmployeeId = getFinanceQueueEmployeeIdAtModelRow(modelRow);
            if (rowEmployeeId != null && rowEmployeeId == employeeId) {
                int viewRow = readinessTable.convertRowIndexToView(modelRow);
                if (viewRow >= 0) {
                    readinessTable.getSelectionModel().setSelectionInterval(viewRow, viewRow);
                    Rectangle rect = readinessTable.getCellRect(viewRow, 0, true);
                    readinessTable.scrollRectToVisible(rect);
                    return true;
                }
            }
        }
        readinessTable.clearSelection();
        return false;
    }

    private void configureFinanceQueueSelection(boolean selectable) {
        if (readinessTable == null) {
            return;
        }
        readinessTable.clearSelection();
        readinessTable.setRowSelectionAllowed(selectable);
        readinessTable.setEnabled(selectable);
    }

    private Integer getFinanceQueueEmployeeIdAtModelRow(int modelRow) {
        if (readinessTable == null || readinessTable.getModel() == null
                || modelRow < 0 || modelRow >= readinessTable.getModel().getRowCount()) {
            return null;
        }
        Object value = readinessTable.getModel().getValueAt(modelRow, 0);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private WorkforcePayrollReadiness findFinanceReadinessByEmployeeId(Integer employeeId) {
        if (employeeId == null || currentFinanceQueue == null) {
            return null;
        }
        for (WorkforcePayrollReadiness readiness : currentFinanceQueue) {
            if (readiness != null && readiness.getEmployeeId() == employeeId) {
                return readiness;
            }
        }
        return null;
    }

    private void openPayrollDetails() {
        WorkforcePayrollReadiness readiness = getSelectedFinanceReadiness();
        if (readiness == null) {
            JOptionPane.showMessageDialog(this,
                    "Select an employee in the Finance Payroll Validation Queue first.",
                    "Payroll Details",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Employee employee = currentEmployeesById.get(readiness.getEmployeeId());
        List<PayrollReadinessIssue> issues = currentIssuesByEmployee.getOrDefault(
                readiness.getEmployeeId(), List.of());
        StringBuilder details = new StringBuilder();
        details.append("<html><body style='width:420px'>")
                .append("<b>").append(employeeName(employee, readiness.getEmployeeId())).append("</b><br>")
                .append("Position: ").append(safeText(employee == null ? null : employee.getPosition(), "-")).append("<br>")
                .append("Readiness: ").append(formatReadinessLifecycle(readiness.getReadinessStatus())).append("<br>")
                .append("HR status: ").append(formatHrValidationStatus(readiness)).append("<br>")
                .append("Payroll impact: ").append(payrollImpact(readiness, issues)).append("<br><br>")
                .append("<b>Payroll validation notes</b><br>");
        if (issues.isEmpty()) {
            details.append("No payroll blockers are currently detected for this employee.");
        } else {
            for (PayrollReadinessIssue issue : issues) {
                details.append("- ").append(issue.getIssue()).append(" (")
                        .append(formatSeverity(issue.getSeverity())).append(")<br>");
            }
        }
        details.append("</body></html>");

        JOptionPane.showMessageDialog(this,
                new JLabel(details.toString()),
                "Payroll Details",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private String getCurrentRole() {
        return currentUser == null ? "" : currentUser.getRole();
    }

    private Integer getCurrentEmployeeId() {
        return currentUser == null ? null : currentUser.getEmployeeId();
    }

    private DuplicateDecision handleProcessedCutoff(String cutoff) {
        boolean finance = roleAccessService.canGeneratePayroll(getCurrentRole());
        Object[] options = finance
                ? new Object[]{"View Payroll History", "Refresh", "Finance Override", "Cancel"}
                : new Object[]{"View Payroll History", "Refresh", "Cancel"};

        int choice = JOptionPane.showOptionDialog(
                this,
                "Payroll already processed for this cutoff.\nChoose a safe action before continuing.",
                "Payroll Finalized",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]);

        if (choice == 0) {
            return DuplicateDecision.OPEN_HISTORY;
        }
        if (choice == 1) {
            return DuplicateDecision.REFRESH;
        }
        if (finance && choice == 2) {
            int overrideChoice = JOptionPane.showConfirmDialog(
                    this,
                    "Finance override will replace existing payroll history for " + cutoff + ".\nContinue?",
                    "Confirm Finance Override",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            return overrideChoice == JOptionPane.YES_OPTION
                    ? DuplicateDecision.OVERRIDE
                    : DuplicateDecision.CANCEL;
        }
        return DuplicateDecision.CANCEL;
    }

    private void openPayrollHistory() {
        if (navigationHandler != null) {
            navigationHandler.accept("PAYROLL_HISTORY");
            return;
        }

        JOptionPane.showMessageDialog(this,
                "Open Payroll History from the sidebar to review saved payroll records.",
                "Payroll History",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void runPayrollWorkflowAction(String busyMessage, PayrollWorkflowTask task, String title) {
        setControlsEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return task.run();
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    String message = get();
                    notifyWorkflowRefresh();
                    refreshAll();
                    JOptionPane.showMessageDialog(PayrollPanel.this,
                            message,
                            title,
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    setControlsEnabled(true);
                    Throwable cause = ex.getCause();
                    JOptionPane.showMessageDialog(PayrollPanel.this,
                            cause == null ? ex.getMessage() : cause.getMessage(),
                            title,
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void generatePayrollSummaryReport() {
        if (!roleAccessService.canGeneratePayroll(getCurrentRole())) {
            showAccessDeniedMessage("Only Finance users can generate payroll summary reports.");
            return;
        }

        if (cutoffBox == null || cutoffBox.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this,
                    "Select a payroll period before generating the payroll summary report.",
                    "Payroll Period Required",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String cutoff = cutoffBox.getSelectedItem().toString().trim();
        String dateFrom = dateFromField == null ? "" : dateFromField.getText().trim();
        String dateTo = dateToField == null ? "" : dateToField.getText().trim();

        if (cutoff.isBlank() || dateFrom.isBlank() || dateTo.isBlank()) {
            JOptionPane.showMessageDialog(this,
                    "Select a cutoff period and date range before generating the payroll summary report.",
                    "Payroll Period Required",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        LocalDate periodStart;
        LocalDate periodEnd;
        try {
            periodStart = LocalDate.parse(dateFrom);
            periodEnd = LocalDate.parse(dateTo);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Use a valid payroll date range before generating the payroll summary report.",
                    "Payroll Period Required",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!hasPayrollHistoryForPeriod(cutoff, dateFrom, dateTo)) {
            JOptionPane.showMessageDialog(this,
                    "No finalized payroll history exists for the selected payroll period.",
                    "Payroll Summary Unavailable",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        setControlsEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        SwingWorker<File, Void> worker = new SwingWorker<>() {
            @Override
            protected File doInBackground() {
                File previewFile = new File(
                        System.getProperty("java.io.tmpdir"),
                        "payroll_summary_" + sanitizeFilePart(cutoff) + ".pdf");

                new ReportService().generatePayrollSummary(
                        cutoff,
                        periodStart,
                        periodEnd,
                        previewFile.getAbsolutePath());
                return previewFile;
            }

            @Override
            protected void done() {
                setControlsEnabled(true);
                setCursor(Cursor.getDefaultCursor());
                try {
                    openGeneratedReport(get());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(PayrollPanel.this,
                            "Failed to generate the payroll summary preview.",
                            "Report Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private boolean hasPayrollHistoryForPeriod(String cutoff, String periodStart, String periodEnd) {
        for (PayrollHistoryRecord record : payrollService.getPayrollHistoryByCutoff(cutoff)) {
            String recordStart = record.getPeriodStart() == null ? "" : record.getPeriodStart().trim();
            String recordEnd = record.getPeriodEnd() == null ? "" : record.getPeriodEnd().trim();
            if (recordStart.equals(periodStart) && recordEnd.equals(periodEnd)) {
                return true;
            }
        }
        return false;
    }

    private void openGeneratedReport(File reportFile) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            JOptionPane.showMessageDialog(this,
                    "Report generated, but this system cannot open PDF files automatically:\n"
                            + reportFile.getAbsolutePath(),
                    "Open PDF Manually",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            Desktop.getDesktop().open(reportFile);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Report generated, but the PDF could not be opened automatically:\n"
                            + reportFile.getAbsolutePath(),
                    "Open PDF Manually",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private String sanitizeFilePart(String value) {
        String safeValue = value == null || value.isBlank() ? "period" : value.trim();
        return safeValue.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void notifyWorkflowRefresh() {
        if (workflowRefreshHandler != null) {
            workflowRefreshHandler.run();
        }
    }

    private void notifyPayrollArtifactsRefresh() {
        notifyWorkflowRefresh();
        if (payrollArtifactsRefreshHandler != null) {
            payrollArtifactsRefreshHandler.run();
        }
    }

    private interface PayrollWorkflowTask {
        String run();
    }

    private void showAccessDeniedMessage(String message) {
        JOptionPane.showMessageDialog(this,
                message,
                "Access Restricted",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private String formatReadinessStatus(PayrollReadinessReport.Status status) {
        if (status == null) {
            return "Workforce Validation Required";
        }

        return switch (status) {
            case READY -> "Validated";
            case NEEDS_REVIEW -> "Awaiting Workforce Resolution";
            case BLOCKED_INCOMPLETE -> "Payroll Blocking Issues";
        };
    }

    private String formatFinanceQueueEmptyStatus() {
        if (currentWorkforceSummary == null || currentWorkforceSummary.getTotalEmployees() == 0) {
            return "Workforce Review Not Activated";
        }
        if (currentWorkforceSummary.getReadyForFinanceCount() == 0) {
            return "Waiting for HR Endorsement";
        }
        return "Ready for Finance Validation";
    }

    private String employeeName(Employee employee, int employeeId) {
        if (employee == null) {
            return "Employee #" + employeeId;
        }
        return safeText((employee.getFirstName() + " " + employee.getLastName()).trim(), "Employee #" + employeeId);
    }

    private String formatOwner(String owner) {
        if (owner == null || owner.isBlank()) {
            return "-";
        }
        return switch (owner.trim().toUpperCase(Locale.ROOT)) {
            case WorkforcePayrollReadiness.OWNER_SUPERVISOR -> "Supervisor";
            case WorkforcePayrollReadiness.OWNER_HR -> "HR";
            case WorkforcePayrollReadiness.OWNER_FINANCE -> "Finance";
            default -> owner;
        };
    }

    private String formatReadinessLifecycle(String status) {
        if (status == null || status.isBlank()) {
            return "Draft";
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case WorkforcePayrollReadiness.STATUS_OPEN_WORKFORCE_ISSUES -> "Workforce Issues";
            case WorkforcePayrollReadiness.STATUS_PENDING_SUPERVISOR_REVIEW -> "Pending Supervisor Review";
            case WorkforcePayrollReadiness.STATUS_RETURNED_TO_SUPERVISOR -> "Returned to Supervisor";
            case WorkforcePayrollReadiness.STATUS_SUPERVISOR_CLEARED,
                    WorkforcePayrollReadiness.STATUS_PENDING_HR_VALIDATION -> "Waiting HR Validation";
            case WorkforcePayrollReadiness.STATUS_HR_VALIDATED -> "HR Validated";
            case WorkforcePayrollReadiness.STATUS_ENDORSED_TO_FINANCE -> "Finance Review Pending";
            case WorkforcePayrollReadiness.STATUS_FINANCE_VALIDATED -> "Payroll Ready";
            case WorkforcePayrollReadiness.STATUS_PAYROLL_PROCESSED -> "Processed";
            case WorkforcePayrollReadiness.STATUS_PAYROLL_LOCKED -> "Locked";
            default -> status;
        };
    }

    private String formatHrValidationStatus(WorkforcePayrollReadiness readiness) {
        if (readiness == null) {
            return "-";
        }
        String status = readiness.getReadinessStatus() == null
                ? ""
                : readiness.getReadinessStatus().trim().toUpperCase(Locale.ROOT);
        if (readiness.getHrValidatedAt() != null
                || WorkforcePayrollReadiness.STATUS_ENDORSED_TO_FINANCE.equals(status)
                || WorkforcePayrollReadiness.STATUS_FINANCE_VALIDATED.equals(status)
                || WorkforcePayrollReadiness.STATUS_PAYROLL_PROCESSED.equals(status)
                || WorkforcePayrollReadiness.STATUS_PAYROLL_LOCKED.equals(status)) {
            return "HR Endorsed";
        }
        return "Awaiting HR Endorsement";
    }

    private String recommendedFinanceAction(WorkforcePayrollReadiness readiness, List<PayrollReadinessIssue> issues) {
        String status = readiness == null || readiness.getReadinessStatus() == null
                ? ""
                : readiness.getReadinessStatus().trim().toUpperCase(Locale.ROOT);
        if (containsBlockingIssue(issues)) {
            return "Resolve payroll blocking issue before processing.";
        }
        if (WorkforcePayrollReadiness.STATUS_ENDORSED_TO_FINANCE.equals(status)) {
            return "Confirm Payroll Readiness";
        }
        if (WorkforcePayrollReadiness.STATUS_FINANCE_VALIDATED.equals(status)) {
            return "Ready for Processing";
        }
        if (WorkforcePayrollReadiness.STATUS_PAYROLL_PROCESSED.equals(status)
                || WorkforcePayrollReadiness.STATUS_PAYROLL_LOCKED.equals(status)) {
            return "Review processed register";
        }
        return "Await HR endorsement";
    }

    private String payrollImpact(WorkforcePayrollReadiness readiness, List<PayrollReadinessIssue> issues) {
        if (containsBlockingIssue(issues)) {
            return "Blocks payroll";
        }
        String status = readiness == null || readiness.getReadinessStatus() == null
                ? ""
                : readiness.getReadinessStatus().trim().toUpperCase(Locale.ROOT);
        if (isProcessablePayrollReadyStatus(readiness)) {
            return "Payroll Ready";
        }
        if (WorkforcePayrollReadiness.STATUS_PAYROLL_PROCESSED.equals(status)) {
            return "Processed";
        }
        if (WorkforcePayrollReadiness.STATUS_PAYROLL_LOCKED.equals(status)) {
            return "Processed";
        }
        if (WorkforcePayrollReadiness.STATUS_ENDORSED_TO_FINANCE.equals(status)) {
            return "Finance Review Pending";
        }
        if (issues != null && !issues.isEmpty()) {
            return "Review before processing";
        }
        return "Draft Preview - Not Ready";
    }

    private boolean containsBlockingIssue(List<PayrollReadinessIssue> issues) {
        if (issues == null) {
            return false;
        }
        for (PayrollReadinessIssue issue : issues) {
            if (issue != null && issue.getSeverity() == PayrollReadinessIssue.Severity.BLOCKED) {
                return true;
            }
        }
        return false;
    }

    private String formatSeverity(PayrollReadinessIssue.Severity severity) {
        if (severity == null) {
            return "Workforce Review Required";
        }

        return switch (severity) {
            case INFO -> "Informational";
            case NEEDS_REVIEW -> "Workforce Review Required";
            case BLOCKED -> "Payroll Blocking Issue";
        };
    }

    private Color getReadinessColor(PayrollReadinessReport.Status status) {
        if (status == null) {
            return UITheme.YELLOW;
        }

        return switch (status) {
            case READY -> UITheme.SUCCESS;
            case NEEDS_REVIEW -> UITheme.YELLOW;
            case BLOCKED_INCOMPLETE -> UITheme.DANGER;
        };
    }

    private String formatLifecycleStatus(PayrollLifecycleStatus lifecycleStatus) {
        if (lifecycleStatus == null || lifecycleStatus.getStatus() == null) {
            return "Draft";
        }

        return switch (lifecycleStatus.getStatus()) {
            case DRAFT -> "Draft";
            case OPEN_WORKFORCE_REVIEW -> "Open Workforce Review";
            case READY_FOR_HR_VALIDATION -> "Ready for HR Validation";
            case READY_FOR_FINANCE_VALIDATION -> "Ready for Finance Validation";
            case READY_FOR_PROCESSING -> "Ready for Processing";
            case PROCESSING -> "Processing";
            case PROCESSED -> "Payroll Finalized";
            case LOCKED -> "Locked Payroll Run";
        };
    }

    private String formatRegisterStatus(PayrollLifecycleStatus lifecycleStatus, WorkforcePayrollReadiness readiness) {
        if (lifecycleStatus != null && lifecycleStatus.getStatus() != null) {
            if (lifecycleStatus.getStatus() == PayrollLifecycleStatus.Status.LOCKED) {
                return "Processed";
            }
            if (lifecycleStatus.getStatus() == PayrollLifecycleStatus.Status.PROCESSED
                    || lifecycleStatus.getStatus() == PayrollLifecycleStatus.Status.PROCESSING) {
                return "Processed";
            }
        }
        if (readiness == null) {
            return "Draft Preview - Not Ready";
        }
        String status = readiness.getReadinessStatus() == null
                ? ""
                : readiness.getReadinessStatus().trim().toUpperCase(Locale.ROOT);
        if (WorkforcePayrollReadiness.STATUS_OPEN_WORKFORCE_ISSUES.equals(status)
                || WorkforcePayrollReadiness.STATUS_RETURNED_TO_SUPERVISOR.equals(status)) {
            return "Excluded / Blocked";
        }
        if (WorkforcePayrollReadiness.STATUS_PENDING_SUPERVISOR_REVIEW.equals(status)) {
            return "Pending Supervisor Review";
        }
        if (WorkforcePayrollReadiness.STATUS_SUPERVISOR_CLEARED.equals(status)
                || WorkforcePayrollReadiness.STATUS_PENDING_HR_VALIDATION.equals(status)
                || WorkforcePayrollReadiness.STATUS_HR_VALIDATED.equals(status)) {
            return "Waiting HR Validation";
        }
        if (WorkforcePayrollReadiness.STATUS_ENDORSED_TO_FINANCE.equals(status)) {
            return "Finance Review Pending";
        }
        if (isProcessablePayrollReadyStatus(readiness)) {
            return "Payroll Ready";
        }
        if (WorkforcePayrollReadiness.STATUS_FINANCE_VALIDATED.equals(status)) {
            return "Draft Preview - Not Ready";
        }
        return formatReadinessLifecycle(status);
    }

    private String formatProcessedTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isBlank()
                || "Timestamp unavailable".equalsIgnoreCase(timestamp.trim())) {
            return "No processing timestamp recorded";
        }
        return timestamp;
    }

    private String selectedCutoff() {
        return cutoffBox == null || cutoffBox.getSelectedItem() == null
                ? "Not selected"
                : cutoffBox.getSelectedItem().toString();
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String workflowOwnerForIssue(SummarizedReadinessIssue issue) {
        String text = (issue.getIssue() + " " + issue.getRecommendedAction()).toLowerCase(Locale.ENGLISH);
        if (text.contains("schedule") || text.contains("leave") || text.contains("attendance adjustment")
                || text.contains("employee profile")) {
            return "HR Workforce Governance";
        }
        if (text.contains("incomplete attendance") || text.contains("missing time")
                || text.contains("late") || text.contains("undertime")) {
            return "HR / Supervisor Coordination";
        }
        return "Finance Validation";
    }

    private String operationalStatusForIssue(SummarizedReadinessIssue issue) {
        if (issue.getSeverity() == PayrollReadinessIssue.Severity.BLOCKED) {
            return "Payroll Blocking Issue";
        }
        if (issue.getSeverity() == PayrollReadinessIssue.Severity.NEEDS_REVIEW) {
            return "Awaiting Workforce Resolution";
        }
        return "Validated";
    }

    private Color getLifecycleColor(PayrollLifecycleStatus lifecycleStatus) {
        if (lifecycleStatus == null || lifecycleStatus.getStatus() == null) {
            return UITheme.TEXT_SECONDARY;
        }

        return switch (lifecycleStatus.getStatus()) {
            case DRAFT -> UITheme.TEXT_SECONDARY;
            case OPEN_WORKFORCE_REVIEW, READY_FOR_HR_VALIDATION -> UITheme.YELLOW;
            case READY_FOR_FINANCE_VALIDATION, READY_FOR_PROCESSING -> UITheme.SUCCESS;
            case PROCESSING -> UITheme.ACCENT;
            case PROCESSED -> UITheme.BLUE;
            case LOCKED -> UITheme.DANGER;
        };
    }

    // ================= HELPER DATA CLASSES =================

    private enum DuplicateDecision {
        OPEN_HISTORY,
        REFRESH,
        OVERRIDE,
        CANCEL
    }

    private static class PayrollViewData {
        Object[][] tableData;
        PayrollSummaryData summary;
        PayrollReadinessReport readinessReport;
        PayrollLifecycleStatus lifecycleStatus;
        PayrollPeriod payrollPeriod;
        WorkforceReadinessSummary workforceSummary;
        List<WorkforcePayrollReadiness> financeQueue;
        List<WorkforcePayrollReadiness> allReadinessRows;
        Map<Integer, Employee> employeesById;
        Map<Integer, List<PayrollReadinessIssue>> issuesByEmployee;

        PayrollViewData(Object[][] tableData, PayrollSummaryData summary,
                PayrollReadinessReport readinessReport, PayrollLifecycleStatus lifecycleStatus,
                PayrollPeriod payrollPeriod, WorkforceReadinessSummary workforceSummary,
                List<WorkforcePayrollReadiness> financeQueue,
                List<WorkforcePayrollReadiness> allReadinessRows,
                Map<Integer, Employee> employeesById,
                Map<Integer, List<PayrollReadinessIssue>> issuesByEmployee) {
            this.tableData = tableData;
            this.summary = summary;
            this.readinessReport = readinessReport;
            this.lifecycleStatus = lifecycleStatus;
            this.payrollPeriod = payrollPeriod;
            this.workforceSummary = workforceSummary == null
                    ? WorkforceReadinessSummary.empty(payrollPeriod == null ? 0 : payrollPeriod.getPeriodId())
                    : workforceSummary;
            this.financeQueue = financeQueue == null ? List.of() : financeQueue;
            this.allReadinessRows = allReadinessRows == null ? List.of() : allReadinessRows;
            this.employeesById = employeesById == null ? Map.of() : employeesById;
            this.issuesByEmployee = issuesByEmployee == null ? Map.of() : issuesByEmployee;
        }
    }

    private static class PayrollSummaryData {
        double totalGross = 0;
        double totalDeductions = 0;
        double totalNet = 0;
        int employeeCount = 0;
        double totalSSS = 0;
        double totalPhilhealth = 0;
        double totalPagibig = 0;
        double totalTax = 0;
    }

    private static class SummarizedReadinessIssue {
        private final String employeeName;
        private String issue;
        private final PayrollReadinessIssue.Severity severity;
        private final String recommendedAction;
        private int count;

        private SummarizedReadinessIssue(String employeeName, String issue,
                PayrollReadinessIssue.Severity severity, String recommendedAction, int count) {
            this.employeeName = employeeName;
            this.issue = issue;
            this.severity = severity;
            this.recommendedAction = recommendedAction;
            this.count = count;
        }

        private void incrementScheduleGapCount() {
            count++;
            issue = count + " schedule assignment gaps";
        }

        private String getEmployeeName() {
            return employeeName;
        }

        private String getIssue() {
            return issue;
        }

        private PayrollReadinessIssue.Severity getSeverity() {
            return severity;
        }

        private String getRecommendedAction() {
            return recommendedAction;
        }
    }
}
