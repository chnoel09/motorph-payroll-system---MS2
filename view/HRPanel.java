package com.mycompany.oop.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.PayrollPeriod;
import com.mycompany.oop.model.PayrollReadinessIssue;
import com.mycompany.oop.model.WorkforcePayrollReadiness;
import com.mycompany.oop.service.EmployeeService;
import com.mycompany.oop.service.RoleAccessService;
import com.mycompany.oop.service.WorkforceReadinessService;

public class HRPanel extends JPanel implements RefreshablePanel {
    public enum ViewMode {
        WORKFORCE_GOVERNANCE,
        EMPLOYEE_DATABASE
    }

    private final EmployeeService service;
    private final RoleAccessService roleAccessService;
    private final WorkforceReadinessService workforceReadinessService;
    private final Employee loggedInUser;
    private final ViewMode viewMode;

    private JTable employeeTable;
    private JTable validationTable;
    private JPanel contentBody;
    private List<Employee> employees;
    private Map<Integer, Employee> employeesById;
    private Map<Integer, String> departmentsById;
    private List<WorkforcePayrollReadiness> validationQueue;
    private List<WorkforcePayrollReadiness> endorsedQueue;
    private Map<Integer, List<PayrollReadinessIssue>> readinessIssuesByEmployee;
    private PayrollPeriod activePayrollPeriod;
    private Runnable workflowRefreshHandler;
    private boolean workflowActionRunning;
    private JButton detailsButton;
    private JButton returnButton;
    private JButton validateButton;
    private JButton validateAllButton;
    private JButton endorseButton;
    private JButton endorseAllButton;
    private JButton refreshButton;
    private boolean dataLoading;
    private int lastSyncedPayrollPeriodId;
    private long lastReadinessSyncMs;

    public HRPanel(Employee loggedInUser) {
        this(loggedInUser, null, ViewMode.WORKFORCE_GOVERNANCE);
    }

    public HRPanel(Employee loggedInUser, Runnable workflowRefreshHandler) {
        this(loggedInUser, workflowRefreshHandler, ViewMode.WORKFORCE_GOVERNANCE);
    }

    public HRPanel(Employee loggedInUser, Runnable workflowRefreshHandler, ViewMode viewMode) {
        this.loggedInUser = loggedInUser;
        this.viewMode = viewMode == null ? ViewMode.WORKFORCE_GOVERNANCE : viewMode;
        this.workflowRefreshHandler = workflowRefreshHandler;
        this.service = new EmployeeService();
        this.roleAccessService = new RoleAccessService();
        this.workforceReadinessService = new WorkforceReadinessService();
        this.employees = new ArrayList<>();
        this.employeesById = new HashMap<>();
        this.validationQueue = new ArrayList<>();
        this.endorsedQueue = new ArrayList<>();
        this.readinessIssuesByEmployee = new HashMap<>();

        setLayout(new BorderLayout());
        setBackground(UITheme.BG);

        add(UITheme.createTitleBar(viewTitle()), BorderLayout.NORTH);
        contentBody = createLoadingPanel("Loading " + viewTitle() + "...");
        add(contentBody, BorderLayout.CENTER);
        refreshData();
    }

    @Override
    public void refreshData() {
        refreshData(false);
    }

    private void refreshData(boolean forceReadinessSync) {
        if (dataLoading) {
            return;
        }
        dataLoading = true;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        if (contentBody != null) {
            remove(contentBody);
        }
        contentBody = createLoadingPanel("Loading " + viewTitle() + "...");
        add(contentBody, BorderLayout.CENTER);
        revalidate();
        repaint();

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private long startedAtMs;

            @Override
            protected Void doInBackground() {
                startedAtMs = System.currentTimeMillis();
                loadData(forceReadinessSync);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    swapContent(createContent());
                    System.out.println("[perf] HRPanel refreshData took "
                            + (System.currentTimeMillis() - startedAtMs) + " ms");
                } catch (Exception ex) {
                    swapContent(createErrorPanel("Unable to load " + viewTitle() + ". Please refresh and try again."));
                } finally {
                    dataLoading = false;
                    setCursor(Cursor.getDefaultCursor());
                    updateWorkflowButtons();
                }
            }
        };
        worker.execute();
    }

    private JPanel createContent() {
        JPanel content = UITheme.createWorkspacePanel(new BorderLayout(0, 14));
        content.add(createHeader(), BorderLayout.NORTH);

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        if (viewMode == ViewMode.EMPLOYEE_DATABASE) {
            stack.add(createEmployeeDirectorySection());
        } else {
            stack.add(createValidationQueueSection());
        }

        JScrollPane scrollPane = new JScrollPane(stack);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UITheme.BG);
        WorkforceFormToolkit.tuneScrollPane(scrollPane);
        content.add(scrollPane, BorderLayout.CENTER);
        return content;
    }

    private void swapContent(JPanel nextContent) {
        if (contentBody != null) {
            remove(contentBody);
        }
        contentBody = nextContent;
        add(contentBody, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private JPanel createLoadingPanel(String message) {
        JPanel panel = UITheme.createWorkspacePanel(new BorderLayout(0, 12));
        panel.add(UITheme.createSkeletonCard(message, 4), BorderLayout.NORTH);
        if (viewMode == ViewMode.EMPLOYEE_DATABASE) {
            panel.add(UITheme.createSkeletonCard("Employee Database", 6), BorderLayout.CENTER);
        } else {
            JPanel pipeline = new JPanel(new GridBagLayout());
            pipeline.setOpaque(false);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weighty = 1.0;
            gbc.insets = new Insets(0, 0, 0, 10);
            gbc.gridx = 0;
            gbc.weightx = 0.45;
            pipeline.add(UITheme.createSkeletonCard("HR Validation Queue", 5), gbc);
            gbc.gridx = 1;
            gbc.weightx = 0.12;
            pipeline.add(UITheme.createSkeletonCard("Actions", 5), gbc);
            gbc.gridx = 2;
            gbc.weightx = 0.43;
            gbc.insets = new Insets(0, 0, 0, 0);
            pipeline.add(UITheme.createSkeletonCard("Endorsed to Finance", 5), gbc);
            panel.add(pipeline, BorderLayout.CENTER);
        }
        return panel;
    }

    private JPanel createErrorPanel(String message) {
        JPanel panel = UITheme.createWorkspacePanel(new BorderLayout());
        JLabel label = new JLabel(message, JLabel.CENTER);
        label.setFont(UITheme.FONT_BODY_BOLD);
        label.setForeground(UITheme.DANGER);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createHeader() {
        if (viewMode == ViewMode.EMPLOYEE_DATABASE) {
            return WorkforceFormToolkit.createSection(
                    "Employee Database",
                    "Manage employee records, normalized accounts, and reporting lines."
            );
        }
        return WorkforceFormToolkit.createSection(
                "Workforce Validation Queue",
                "Validate supervisor-cleared employees, return unresolved items, or endorse ready employees to Finance."
        );
    }

    private String viewTitle() {
        return viewMode == ViewMode.EMPLOYEE_DATABASE ? "Employee Database" : "Workforce Governance";
    }

    private JPanel createValidationQueueSection() {
        JPanel section = WorkforceFormToolkit.createSection(
                "HR Validation → Finance Validation",
                "Supervisor-cleared rows move through HR before Finance can validate payroll readiness."
        );

        if (activePayrollPeriod == null) {
            section.add(createEmptyState("No active payroll period is open for HR workforce validation."), BorderLayout.CENTER);
            return section;
        }

        JPanel summary = new JPanel(new BorderLayout(8, 8));
        summary.setOpaque(false);
        JLabel periodLabel = new JLabel("Active period: " + activePayrollPeriod.getCutoffPeriod()
                + " • " + activePayrollPeriod.getPeriodStart() + " to " + activePayrollPeriod.getPeriodEnd()
                + " • " + activePayrollPeriod.getStatus()
                + "     HR Validation Pending: " + validationQueue.size()
                + " • Endorsed to Finance: " + endorsedQueue.size());
        periodLabel.setFont(UITheme.FONT_SMALL);
        periodLabel.setForeground(UITheme.TEXT_SECONDARY);

        summary.add(periodLabel, BorderLayout.WEST);
        section.add(summary, BorderLayout.NORTH);

        validationTable = createHrValidationTable();
        JPanel left = createPipelineTablePanel(
                "HR Validation Queue",
                validationQueue.size() + " Pending",
                UITheme.DANGER,
                UITheme.createTableScrollPane(validationTable));
        JPanel center = createHrActionStagePanel();
        JPanel right = createPipelineTablePanel(
                "Endorsed to Finance",
                endorsedQueue.size() + " Endorsed",
                UITheme.BLUE,
                UITheme.createTableScrollPane(createEndorsedToFinanceTable()));

        JPanel pipeline = new JPanel(new GridBagLayout());
        pipeline.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 0, 0, 8);
        gbc.gridx = 0;
        gbc.weightx = 0.50;
        pipeline.add(left, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.07;
        pipeline.add(center, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0.43;
        gbc.insets = new Insets(0, 0, 0, 0);
        pipeline.add(right, gbc);
        section.add(pipeline, BorderLayout.CENTER);
        updateWorkflowButtons();
        return section;
    }

    private JPanel createHrActionStagePanel() {
        JPanel actions = new JPanel();
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        actions.setBackground(Color.WHITE);
        actions.setBorder(BorderFactory.createEmptyBorder(6, 2, 6, 2));
        actions.setMinimumSize(new Dimension(92, 220));
        actions.setPreferredSize(new Dimension(104, 0));
        JLabel title = new JLabel("ACTIONS", JLabel.CENTER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(UITheme.FONT_BODY_BOLD);
        title.setForeground(UITheme.TEXT_PRIMARY);
        detailsButton = UITheme.createCompactWorkflowButton("Review", false);
        validateButton = UITheme.createCompactWorkflowButton("Validate", true);
        validateAllButton = UITheme.createCompactWorkflowButton("Validate All", false);
        endorseButton = UITheme.createCompactWorkflowButton(">> Endorse", false);
        endorseAllButton = UITheme.createCompactWorkflowButton(">> All", false);
        returnButton = UITheme.createCompactWorkflowButton("<< Return", false);
        refreshButton = UITheme.createCompactWorkflowButton("Refresh", false);
        detailsButton.addActionListener(e -> showSelectedWorkforceDetails());
        returnButton.addActionListener(e -> returnSelectedToSupervisor());
        validateButton.addActionListener(e -> validateSelectedReadiness());
        validateAllButton.addActionListener(e -> validateAllReadiness());
        endorseButton.addActionListener(e -> endorseSelectedToFinance());
        endorseAllButton.addActionListener(e -> endorseAllToFinance());
        refreshButton.addActionListener(e -> refreshData(true));
        for (JButton button : List.of(detailsButton, returnButton, validateButton,
                validateAllButton, endorseButton, endorseAllButton, refreshButton)) {
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        actions.add(title);
        actions.add(Box.createVerticalStrut(8));
        actions.add(detailsButton);
        actions.add(Box.createVerticalStrut(5));
        actions.add(validateButton);
        actions.add(Box.createVerticalStrut(5));
        actions.add(validateAllButton);
        actions.add(Box.createVerticalStrut(5));
        actions.add(endorseButton);
        actions.add(Box.createVerticalStrut(5));
        actions.add(endorseAllButton);
        actions.add(Box.createVerticalStrut(5));
        actions.add(returnButton);
        actions.add(Box.createVerticalStrut(5));
        actions.add(refreshButton);
        return actions;
    }

    private JTable createHrValidationTable() {
        DefaultTableModel model = createReadOnlyModel(new String[]{
                "Employee", "Issue Summary", "Status"
        });

        for (WorkforcePayrollReadiness readiness : validationQueue) {
            Employee employee = employeesById.get(readiness.getEmployeeId());
            List<PayrollReadinessIssue> issues = readinessIssuesByEmployee.getOrDefault(readiness.getEmployeeId(), List.of());
            model.addRow(new Object[]{
                    readiness.getEmployeeId() + " - " + (employee == null ? "Employee #" + readiness.getEmployeeId() : fullName(employee)),
                    recommendedAction(readiness, issues),
                    formatReadinessStatus(readiness.getReadinessStatus())
            });
        }

        if (model.getRowCount() == 0) {
            model.addRow(new Object[]{"-", "No workforce readiness items are awaiting HR action.", "Stable"});
        }

        JTable table = new JTable(model);
        UITheme.styleTable(table);
        table.setRowHeight(34);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        UITheme.setColumnWidths(table, 230, 260, 140);
        table.getSelectionModel().addListSelectionListener(e -> updateWorkflowButtons());
        return table;
    }

    private JTable createEndorsedToFinanceTable() {
        DefaultTableModel model = createReadOnlyModel(new String[]{"Employee", "Status", "Endorsed On"});
        for (WorkforcePayrollReadiness readiness : endorsedQueue) {
            Employee employee = employeesById.get(readiness.getEmployeeId());
            model.addRow(new Object[]{
                    readiness.getEmployeeId() + " - " + (employee == null ? "Employee #" + readiness.getEmployeeId() : fullName(employee)),
                    formatReadinessStatus(readiness.getReadinessStatus()),
                    readiness.getHrValidatedAt() == null ? "-" : readiness.getHrValidatedAt()
            });
        }
        if (model.getRowCount() == 0) {
            model.addRow(new Object[]{"-", "No employees endorsed to Finance yet.", "-"});
        }
        JTable table = new JTable(model);
        UITheme.styleTable(table);
        table.setRowHeight(34);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        UITheme.setColumnWidths(table, 230, 160, 160);
        return table;
    }

    private JPanel createPipelineTablePanel(String title, String badge, Color badgeColor, JScrollPane tableScroll) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel titleLabel = new JLabel(title.toUpperCase(Locale.ENGLISH));
        titleLabel.setFont(UITheme.FONT_BODY_BOLD);
        titleLabel.setForeground(UITheme.BLUE);
        JLabel badgeLabel = new JLabel(badge);
        badgeLabel.setOpaque(true);
        badgeLabel.setBackground(badgeColor);
        badgeLabel.setForeground(Color.WHITE);
        badgeLabel.setFont(UITheme.FONT_BODY_BOLD);
        badgeLabel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        header.add(titleLabel, BorderLayout.WEST);
        header.add(badgeLabel, BorderLayout.EAST);
        tableScroll.setPreferredSize(new Dimension(0, 260));
        panel.add(header, BorderLayout.NORTH);
        panel.add(tableScroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createEmployeeDirectorySection() {
        JPanel section = WorkforceFormToolkit.createSection(
                "Employee Directory",
                "Employee records remain available here, but workforce validation is the primary HR queue."
        );

        employeeTable = new JTable();
        UITheme.styleTable(employeeTable);
        employeeTable.setModel(createEmployeeDirectoryModel());
        configureEmployeeDirectoryTable(employeeTable);

        section.add(UITheme.createTableScrollPane(employeeTable), BorderLayout.CENTER);
        section.add(createButtonPanel(), BorderLayout.SOUTH);
        return section;
    }

    private DefaultTableModel createEmployeeDirectoryModel() {
        NumberFormat peso = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        DefaultTableModel model = createReadOnlyModel(new String[]{
                "Employee ID", "First Name", "Last Name", "Position", "Department", "Reports To",
                "Status", "Role", "Email", "Phone Number", "Birthday", "Basic Salary", "Hourly Rate",
                "SSS Number", "PhilHealth Number", "TIN", "Pag-IBIG", "Rice Subsidy",
                "Phone Allowance", "Clothing Allowance"
        });
        for (Employee e : employees) {
            model.addRow(new Object[]{
                    e.getEmployeeId(),
                    safeText(e.getFirstName(), "-"),
                    safeText(e.getLastName(), "-"),
                    safeText(e.getPosition(), "-"),
                    formatDepartment(e),
                    formatSupervisor(e),
                    safeText(e.getEmploymentStatus(), "-"),
                    safeText(e.getRole(), "-"),
                    safeText(e.getEmail(), "-"),
                    safeText(e.getPhoneNumber(), "-"),
                    safeText(e.getBirthday(), "-"),
                    peso.format(e.getBasicSalary()),
                    peso.format(e.getHourlyRate()),
                    safeText(e.getSssNumber(), "-"),
                    safeText(e.getPhilhealthNumber(), "-"),
                    safeText(e.getTinNumber(), "-"),
                    safeText(e.getPagibigNumber(), "-"),
                    peso.format(e.getRiceSubsidy()),
                    peso.format(e.getPhoneAllowance()),
                    peso.format(e.getClothingAllowance())
            });
        }
        return model;
    }

    private void configureEmployeeDirectoryTable(JTable table) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getTableHeader().setResizingAllowed(true);
        table.getTableHeader().setReorderingAllowed(false);
        UITheme.setColumnWidths(table,
                95, 130, 130, 170, 160, 210, 110, 120, 210, 140, 120,
                130, 120, 140, 160, 130, 130, 125, 135, 150);
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            column.setResizable(true);
            column.setMinWidth(60);
            column.setMaxWidth(Integer.MAX_VALUE);
        }
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        panel.setBackground(Color.WHITE);

        JButton viewBtn = UITheme.createButton("View");
        JButton addBtn = UITheme.createButton("Add");
        JButton editBtn = UITheme.createButton("Edit");
        JButton deleteBtn = UITheme.createCrudDangerButton("Delete");

        viewBtn.addActionListener(e -> viewSelectedEmployee());
        addBtn.addActionListener(e -> openEmployeeFormDialog(null, false));
        editBtn.addActionListener(e -> editSelectedEmployee());
        deleteBtn.addActionListener(e -> deleteSelectedEmployee());

        boolean canManageEmployees = roleAccessService.canManageEmployees(getCurrentRole());
        addBtn.setEnabled(canManageEmployees);
        editBtn.setEnabled(canManageEmployees);
        deleteBtn.setEnabled(canManageEmployees);

        panel.add(viewBtn);
        panel.add(addBtn);
        panel.add(editBtn);
        panel.add(deleteBtn);
        return panel;
    }

    private JPanel createCollapsibleSection(String title, String subtitle, JPanel body, boolean expanded) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UITheme.BG);
        wrapper.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JButton toggle = new JButton((expanded ? "▾  " : "▸  ") + title);
        toggle.setBorder(BorderFactory.createEmptyBorder());
        toggle.setContentAreaFilled(false);
        toggle.setFocusPainted(false);
        toggle.setFont(UITheme.FONT_BODY_BOLD);
        toggle.setForeground(UITheme.TEXT_PRIMARY);
        toggle.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);

        JLabel hint = new JLabel(subtitle);
        hint.setFont(UITheme.FONT_SMALL);
        hint.setForeground(UITheme.TEXT_SECONDARY);

        body.setVisible(expanded);
        toggle.addActionListener(e -> {
            boolean show = !body.isVisible();
            body.setVisible(show);
            toggle.setText((show ? "▾  " : "▸  ") + title);
            wrapper.revalidate();
            wrapper.repaint();
        });

        header.add(toggle, BorderLayout.WEST);
        header.add(hint, BorderLayout.EAST);
        wrapper.add(header, BorderLayout.NORTH);
        wrapper.add(body, BorderLayout.CENTER);
        return wrapper;
    }

    private void loadData(boolean forceReadinessSync) {
        long startedAtMs = System.currentTimeMillis();
        long employeeStartMs = System.currentTimeMillis();
        employees = service.getAllEmployees();
        departmentsById = service.getDepartments();
        if (viewMode == ViewMode.EMPLOYEE_DATABASE) {
            for (Employee employee : employees) {
                service.enrichWithGovernmentIds(employee);
            }
        }
        System.out.println("[perf] HRPanel employee load took "
                + (System.currentTimeMillis() - employeeStartMs) + " ms");
        employeesById.clear();
        for (Employee employee : employees) {
            employeesById.put(employee.getEmployeeId(), employee);
        }

        validationQueue = new ArrayList<>();
        endorsedQueue = new ArrayList<>();
        readinessIssuesByEmployee = new HashMap<>();
        long periodStartMs = System.currentTimeMillis();
        activePayrollPeriod = workforceReadinessService.findLatestActivePayrollPeriod();
        System.out.println("[perf] HRPanel active period lookup took "
                + (System.currentTimeMillis() - periodStartMs) + " ms");
        if (activePayrollPeriod != null) {
            syncReadinessIfNeeded(forceReadinessSync);
            long hrQueueStartMs = System.currentTimeMillis();
            validationQueue = workforceReadinessService.getHrValidationQueue(activePayrollPeriod.getPeriodId());
            System.out.println("[perf] HRPanel validation queue load took "
                    + (System.currentTimeMillis() - hrQueueStartMs) + " ms");
            long endorsedQueueStartMs = System.currentTimeMillis();
            endorsedQueue = workforceReadinessService.getFinanceValidationQueue(activePayrollPeriod.getPeriodId());
            System.out.println("[perf] HRPanel endorsed queue load took "
                    + (System.currentTimeMillis() - endorsedQueueStartMs) + " ms");
        }
        System.out.println("[perf] HRPanel loadData took "
                + (System.currentTimeMillis() - startedAtMs) + " ms");
    }

    private void syncReadinessIfNeeded(boolean forceReadinessSync) {
        if (activePayrollPeriod == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!forceReadinessSync) {
            System.out.println("[perf] HRPanel readiness sync skipped on navigation");
            return;
        }
        long syncStartMs = System.currentTimeMillis();
        workforceReadinessService.syncReadinessForPeriod(activePayrollPeriod);
        lastSyncedPayrollPeriodId = activePayrollPeriod.getPeriodId();
        lastReadinessSyncMs = now;
        System.out.println("[perf] HRPanel readiness sync took "
                + (System.currentTimeMillis() - syncStartMs) + " ms");
    }

    private WorkforcePayrollReadiness getSelectedReadiness() {
        if (validationTable == null || validationQueue == null || validationQueue.isEmpty()) {
            return null;
        }
        int row = validationTable.getSelectedRow();
        if (row < 0 || row >= validationQueue.size()) {
            return null;
        }
        return validationQueue.get(row);
    }

    private void showSelectedWorkforceDetails() {
        WorkforcePayrollReadiness readiness = getSelectedReadiness();
        if (readiness == null) {
            showInfo("Workforce Details", "Select an employee readiness row first.");
            return;
        }

        Employee employee = employeesById.get(readiness.getEmployeeId());
        Employee supervisor = readiness.getSupervisorEmployeeId() == null
                ? null
                : employeesById.get(readiness.getSupervisorEmployeeId());
        List<PayrollReadinessIssue> issues = loadIssuesForEmployee(readiness.getEmployeeId());

        StringBuilder details = new StringBuilder();
        details.append(employee == null ? "Employee #" + readiness.getEmployeeId() : fullName(employee)).append("\n");
        details.append("Position: ").append(employee == null ? "-" : safeText(employee.getPosition(), "Not set")).append("\n");
        details.append("Supervisor: ").append(supervisor == null ? "No assigned supervisor" : fullName(supervisor)).append("\n");
        details.append("Readiness: ").append(formatReadinessStatus(readiness.getReadinessStatus())).append("\n");
        details.append("Owner: ").append(formatOwner(readiness.getCurrentOwnerRole())).append("\n\n");
        details.append("Profile completeness: ").append(profileCompleteness(employee)).append("\n");
        details.append("Government IDs: ").append(governmentIdCompleteness(employee)).append("\n\n");
        details.append("Blocker summary:\n");
        if (issues.isEmpty()) {
            details.append("- No active blocker details found.\n");
        } else {
            for (PayrollReadinessIssue issue : issues) {
                details.append("- ").append(issue.getIssue()).append(" — ")
                        .append(issue.getRecommendedAction()).append("\n");
            }
        }
        if (readiness.getReturnReason() != null && !readiness.getReturnReason().isBlank()) {
            details.append("\nReturn reason:\n- ").append(readiness.getReturnReason()).append("\n");
        }

        JTextArea area = new JTextArea(details.toString(), 18, 70);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(UITheme.FONT_BODY);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Workforce Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void validateSelectedReadiness() {
        WorkforcePayrollReadiness readiness = getSelectedReadiness();
        if (readiness == null) {
            showInfo("Validate Workforce Readiness", "Select an employee readiness row first.");
            return;
        }

        runWorkflowAction("Validating readiness...", () -> {
            workforceReadinessService.validateWorkforceReadiness(readiness.getReadinessId(), loggedInUser);
            return "Workforce readiness validated.";
        }, "Validate Workforce Readiness");
    }

    private void endorseSelectedToFinance() {
        WorkforcePayrollReadiness readiness = getSelectedReadiness();
        if (readiness == null) {
            showInfo("Endorse to Finance", "Select an employee readiness row first.");
            return;
        }

        runWorkflowAction("Endorsing to Finance...", () -> {
            workforceReadinessService.endorseToFinance(readiness.getReadinessId(), loggedInUser);
            return "Employee endorsed to Finance Payroll Operations.";
        }, "Endorse to Finance");
    }

    private void validateAllReadiness() {
        List<WorkforcePayrollReadiness> eligibleRows = validationQueue == null ? List.of() : validationQueue.stream()
                .filter(this::isValidationEligible)
                .toList();
        if (eligibleRows.isEmpty()) {
            showInfo("Validate All", "No HR-owned rows are eligible for validation.");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "This will process " + eligibleRows.size() + " eligible employee"
                        + (eligibleRows.size() == 1 ? "" : "s") + ". Continue?",
                "Validate All",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        runWorkflowAction("Validating all...", () -> {
            int success = 0;
            int skipped = 0;
            for (WorkforcePayrollReadiness readiness : eligibleRows) {
                try {
                    workforceReadinessService.validateWorkforceReadiness(readiness.getReadinessId(), loggedInUser);
                    success++;
                } catch (Exception ex) {
                    skipped++;
                }
            }
            return "Completed " + success + ", skipped " + skipped + ".";
        }, "Validate All");
    }

    private void endorseAllToFinance() {
        List<WorkforcePayrollReadiness> eligibleRows = validationQueue == null ? List.of() : validationQueue.stream()
                .filter(this::isEndorseEligible)
                .toList();
        if (eligibleRows.isEmpty()) {
            showInfo("Endorse All", "No validated HR-owned rows are eligible for Finance endorsement.");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "This will process " + eligibleRows.size() + " eligible employee"
                        + (eligibleRows.size() == 1 ? "" : "s") + ". Continue?",
                "Endorse All",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        runWorkflowAction("Endorsing all...", () -> {
            int success = 0;
            int skipped = 0;
            for (WorkforcePayrollReadiness readiness : eligibleRows) {
                try {
                    workforceReadinessService.endorseToFinance(readiness.getReadinessId(), loggedInUser);
                    success++;
                } catch (Exception ex) {
                    skipped++;
                }
            }
            return "Completed " + success + ", skipped " + skipped + ".";
        }, "Endorse All");
    }

    private void returnSelectedToSupervisor() {
        WorkforcePayrollReadiness readiness = getSelectedReadiness();
        if (readiness == null) {
            showInfo("Return to Supervisor", "Select an employee readiness row first.");
            return;
        }

        String reason = JOptionPane.showInputDialog(this,
                "Reason for returning this item to the reporting supervisor:",
                "Return to Supervisor",
                JOptionPane.PLAIN_MESSAGE);
        if (reason == null) {
            return;
        }

        runWorkflowAction("Returning to supervisor...", () -> {
            workforceReadinessService.returnToSupervisor(readiness.getReadinessId(), loggedInUser, reason);
            return "Employee returned to supervisor workforce review.";
        }, "Return to Supervisor");
    }

    private void viewSelectedEmployee() {
        Employee emp = getSelectedEmployee();
        if (emp == null) {
            showWarning("No Selection", "Please select an employee to view.");
            return;
        }
        showEmployeeDetailsDialog(emp);
    }

    private void editSelectedEmployee() {
        if (!roleAccessService.canManageEmployees(getCurrentRole())) {
            showAccessDeniedMessage("Only HR users can manage employee records.");
            return;
        }
        Employee emp = getSelectedEmployee();
        if (emp == null) {
            showWarning("No Selection", "Please select an employee to edit.");
            return;
        }
        openEmployeeFormDialog(emp, false);
    }

    private void deleteSelectedEmployee() {
        if (!roleAccessService.canManageEmployees(getCurrentRole())) {
            showAccessDeniedMessage("Only HR users can manage employee records.");
            return;
        }
        Employee emp = getSelectedEmployee();
        if (emp == null) {
            showWarning("No Selection", "Please select an employee to delete.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete this employee?", "Confirm",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            service.deleteEmployee(emp.getEmployeeId());
            refreshData();
        }
    }

    private Employee getSelectedEmployee() {
        if (employeeTable == null || employeeTable.getSelectedRow() < 0) {
            return null;
        }
        int viewRow = employeeTable.getSelectedRow();
        int modelRow = employeeTable.convertRowIndexToModel(viewRow);
        int id = Integer.parseInt(employeeTable.getModel().getValueAt(modelRow, 0).toString());
        return service.findById(id);
    }

    private void showEmployeeDetailsDialog(Employee employee) {
        Employee fullEmployee = service.findById(employee.getEmployeeId());
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UITheme.BG);
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        content.add(createDetailsSection("Basic Information", new String[][]{
                {"Employee ID", String.valueOf(fullEmployee.getEmployeeId())},
                {"First Name", safeText(fullEmployee.getFirstName(), "-")},
                {"Last Name", safeText(fullEmployee.getLastName(), "-")},
                {"Birthday", safeText(fullEmployee.getBirthday(), "-")}
        }));
        content.add(Box.createVerticalStrut(8));
        content.add(createDetailsSection("Contact Information", new String[][]{
                {"Email", safeText(fullEmployee.getEmail(), "-")},
                {"Phone Number", safeText(fullEmployee.getPhoneNumber(), "-")},
                {"Address", safeText(fullEmployee.getAddress(), "-")}
        }));
        content.add(Box.createVerticalStrut(8));
        content.add(createDetailsSection("Employment Details", new String[][]{
                {"Position", safeText(fullEmployee.getPosition(), "-")},
                {"Status", safeText(fullEmployee.getEmploymentStatus(), "-")},
                {"Role", safeText(fullEmployee.getRole(), "-")}
        }));
        content.add(Box.createVerticalStrut(8));
        content.add(createDetailsSection("Department and Reporting", new String[][]{
                {"Department", service.getDepartmentName(fullEmployee)},
                {"Reports To", formatSupervisor(fullEmployee)}
        }));
        content.add(Box.createVerticalStrut(8));
        content.add(createDetailsSection("Government IDs", new String[][]{
                {"SSS Number", safeText(fullEmployee.getSssNumber(), "-")},
                {"PhilHealth Number", safeText(fullEmployee.getPhilhealthNumber(), "-")},
                {"TIN", safeText(fullEmployee.getTinNumber(), "-")},
                {"Pag-IBIG", safeText(fullEmployee.getPagibigNumber(), "-")}
        }));
        content.add(Box.createVerticalStrut(8));
        content.add(createDetailsSection("Compensation and Allowances", new String[][]{
                {"Basic Salary", formatMoney(fullEmployee.getBasicSalary())},
                {"Hourly Rate", formatMoney(fullEmployee.getHourlyRate())},
                {"Rice Subsidy", formatMoney(fullEmployee.getRiceSubsidy())},
                {"Phone Allowance", formatMoney(fullEmployee.getPhoneAllowance())},
                {"Clothing Allowance", formatMoney(fullEmployee.getClothingAllowance())},
                {"Gross Semi-Monthly Rate", formatMoney(fullEmployee.getGrossSemiMonthlyRate())}
        }));

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setPreferredSize(new Dimension(620, 560));
        WorkforceFormToolkit.tuneScrollPane(scrollPane);
        JOptionPane.showMessageDialog(this, scrollPane, "Employee Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private JPanel createDetailsSection(String title, String[][] rows) {
        JPanel section = new JPanel(new GridBagLayout());
        section.setBackground(Color.WHITE);
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 4, 3, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UITheme.FONT_BODY_BOLD);
        titleLabel.setForeground(UITheme.BLUE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        section.add(titleLabel, gbc);

        gbc.gridwidth = 1;
        for (int i = 0; i < rows.length; i++) {
            JLabel label = new JLabel(rows[i][0] + ":");
            label.setFont(UITheme.FONT_SMALL.deriveFont(java.awt.Font.BOLD));
            label.setForeground(UITheme.TEXT_SECONDARY);
            gbc.gridx = 0;
            gbc.gridy = i + 1;
            gbc.weightx = 0.25;
            section.add(label, gbc);

            JLabel value = new JLabel(rows[i][1]);
            value.setFont(UITheme.FONT_BODY);
            value.setForeground(UITheme.TEXT_PRIMARY);
            gbc.gridx = 1;
            gbc.weightx = 0.75;
            section.add(value, gbc);
        }

        return section;
    }

    private void openEmployeeFormDialog(Employee emp, boolean readOnly) {
        if (!readOnly && !roleAccessService.canManageEmployees(getCurrentRole())) {
            showAccessDeniedMessage("Only HR users can manage employee records.");
            return;
        }

        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        boolean isAdmin = loggedInUser != null && "admin".equalsIgnoreCase(loggedInUser.getRole());
        EmployeeFormDialog dialog = new EmployeeFormDialog(parent, service, emp, isAdmin, readOnly);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        if (!readOnly) {
            refreshData();
        }
    }

    private DefaultTableModel createReadOnlyModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private JPanel createEmptyState(String message) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));
        JLabel label = new JLabel(message);
        label.setFont(UITheme.FONT_BODY);
        label.setForeground(UITheme.TEXT_SECONDARY);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private String formatReadinessStatus(String status) {
        String normalized = safeText(status, "").toUpperCase(Locale.ENGLISH);
        return switch (normalized) {
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
            default -> normalized.isBlank() ? "Not started" : normalized.replace('_', ' ');
        };
    }

    private String formatOwner(String owner) {
        String normalized = safeText(owner, "").toUpperCase(Locale.ENGLISH);
        return switch (normalized) {
            case WorkforcePayrollReadiness.OWNER_SUPERVISOR -> "Supervisor";
            case WorkforcePayrollReadiness.OWNER_HR -> "HR Workforce Governance";
            case WorkforcePayrollReadiness.OWNER_FINANCE -> "Finance";
            default -> normalized.isBlank() ? "-" : normalized;
        };
    }

    private String recommendedAction(WorkforcePayrollReadiness readiness, List<PayrollReadinessIssue> issues) {
        String status = safeText(readiness.getReadinessStatus(), "").toUpperCase(Locale.ENGLISH);
        if (WorkforcePayrollReadiness.STATUS_SUPERVISOR_CLEARED.equals(status)
                || WorkforcePayrollReadiness.STATUS_PENDING_HR_VALIDATION.equals(status)) {
            return "Validate workforce readiness or return unresolved items to Supervisor.";
        }
        if (WorkforcePayrollReadiness.STATUS_HR_VALIDATED.equals(status)) {
            return "Endorse to Finance when ready.";
        }
        if (WorkforcePayrollReadiness.STATUS_RETURNED_TO_SUPERVISOR.equals(status)) {
            return "Monitor supervisor resolution.";
        }
        if (issues != null && !issues.isEmpty()) {
            return issues.get(0).getRecommendedAction();
        }
        return "Review workforce details.";
    }

    private List<PayrollReadinessIssue> loadIssuesForEmployee(int employeeId) {
        if (activePayrollPeriod == null || employeeId <= 0) {
            return List.of();
        }
        long detailStartMs = System.currentTimeMillis();
        readinessIssuesByEmployee = new HashMap<>(
                workforceReadinessService.getReadinessIssuesByEmployee(activePayrollPeriod));
        System.out.println("[perf] HRPanel blocker detail load took "
                + (System.currentTimeMillis() - detailStartMs) + " ms");
        return readinessIssuesByEmployee.getOrDefault(employeeId, List.of());
    }

    private String profileCompleteness(Employee employee) {
        if (employee == null) {
            return "Employee record unavailable";
        }
        List<String> missing = new ArrayList<>();
        if (isBlank(employee.getFirstName())) missing.add("first name");
        if (isBlank(employee.getLastName())) missing.add("last name");
        if (isBlank(employee.getPosition())) missing.add("position");
        if (isBlank(employee.getEmploymentStatus())) missing.add("status");
        if (isBlank(employee.getEmail())) missing.add("email");
        return missing.isEmpty() ? "Complete" : "Missing " + String.join(", ", missing);
    }

    private String governmentIdCompleteness(Employee employee) {
        if (employee == null) {
            return "Employee record unavailable";
        }
        List<String> missing = new ArrayList<>();
        if (isBlank(employee.getSssNumber())) missing.add("SSS");
        if (isBlank(employee.getPhilhealthNumber())) missing.add("PhilHealth");
        if (isBlank(employee.getTinNumber())) missing.add("TIN");
        if (isBlank(employee.getPagibigNumber())) missing.add("Pag-IBIG");
        return missing.isEmpty() ? "Complete" : "Missing " + String.join(", ", missing);
    }

    private String fullName(Employee employee) {
        return safeText(employee.getFirstName(), "") + " " + safeText(employee.getLastName(), "");
    }

    private String formatSupervisor(Employee employee) {
        if (employee == null || employee.getSupervisorEmployeeId() == null) {
            return "Not assigned";
        }
        Employee supervisor = employeesById == null ? null : employeesById.get(employee.getSupervisorEmployeeId());
        if (supervisor == null) {
            supervisor = service.findById(employee.getSupervisorEmployeeId());
        }
        return supervisor == null ? "Not assigned" : supervisor.getEmployeeId() + " - " + fullName(supervisor);
    }

    private String formatDepartment(Employee employee) {
        if (employee == null || employee.getDepartmentId() == null) {
            return "Not assigned";
        }
        if (departmentsById == null) {
            departmentsById = service.getDepartments();
        }
        return departmentsById.getOrDefault(employee.getDepartmentId(), "Not assigned");
    }

    private String formatMoney(double value) {
        return NumberFormat.getCurrencyInstance(new Locale("en", "PH")).format(value);
    }

    private String getCurrentRole() {
        return loggedInUser == null ? "" : loggedInUser.getRole();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void updateWorkflowButtons() {
        boolean hasSelection = getSelectedReadiness() != null;
        boolean enabled = hasSelection && !workflowActionRunning;
        if (detailsButton != null) detailsButton.setEnabled(enabled);
        if (returnButton != null) returnButton.setEnabled(enabled);
        if (validateButton != null) validateButton.setEnabled(enabled && isValidationEligible(getSelectedReadiness()));
        if (endorseButton != null) endorseButton.setEnabled(enabled && isEndorseEligible(getSelectedReadiness()));
        if (validateAllButton != null) {
            validateAllButton.setEnabled(!workflowActionRunning && validationQueue != null
                    && validationQueue.stream().anyMatch(this::isValidationEligible));
        }
        if (endorseAllButton != null) {
            endorseAllButton.setEnabled(!workflowActionRunning && validationQueue != null
                    && validationQueue.stream().anyMatch(this::isEndorseEligible));
        }
        if (refreshButton != null) refreshButton.setEnabled(!workflowActionRunning);
    }

    private boolean isValidationEligible(WorkforcePayrollReadiness readiness) {
        if (readiness == null) {
            return false;
        }
        String owner = safeText(readiness.getCurrentOwnerRole(), "").toUpperCase(Locale.ENGLISH);
        String status = safeText(readiness.getReadinessStatus(), "").toUpperCase(Locale.ENGLISH);
        return WorkforcePayrollReadiness.OWNER_HR.equals(owner)
                && (WorkforcePayrollReadiness.STATUS_SUPERVISOR_CLEARED.equals(status)
                || WorkforcePayrollReadiness.STATUS_PENDING_HR_VALIDATION.equals(status));
    }

    private boolean isEndorseEligible(WorkforcePayrollReadiness readiness) {
        if (readiness == null) {
            return false;
        }
        String owner = safeText(readiness.getCurrentOwnerRole(), "").toUpperCase(Locale.ENGLISH);
        String status = safeText(readiness.getReadinessStatus(), "").toUpperCase(Locale.ENGLISH);
        return WorkforcePayrollReadiness.OWNER_HR.equals(owner)
                && WorkforcePayrollReadiness.STATUS_HR_VALIDATED.equals(status);
    }

    private void runWorkflowAction(String busyMessage, WorkflowTask task, String title) {
        setWorkflowBusy(true, busyMessage);
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return task.run();
            }

            @Override
            protected void done() {
                setWorkflowBusy(false, null);
                try {
                    String message = get();
                    notifyWorkflowRefresh();
                    refreshData();
                    showInfo(title, message);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause();
                    showWarning(title, cause == null ? ex.getMessage() : cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void setWorkflowBusy(boolean busy, String message) {
        workflowActionRunning = busy;
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
        if (refreshButton != null) {
            refreshButton.setText(busy && message != null ? message : "Refresh");
        }
        updateWorkflowButtons();
    }

    private void notifyWorkflowRefresh() {
        if (workflowRefreshHandler != null) {
            workflowRefreshHandler.run();
        }
    }

    private interface WorkflowTask {
        String run();
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void showAccessDeniedMessage(String message) {
        showInfo("Access Restricted", message);
    }

    private void showInfo(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void showWarning(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE);
    }
}
