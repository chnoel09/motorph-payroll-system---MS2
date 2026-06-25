package com.mycompany.oop.view;

import com.mycompany.oop.model.AttendanceAwareness;
import com.mycompany.oop.model.AttendanceAdjustment;
import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.EmployeeSchedule;
import com.mycompany.oop.model.EmployeeShift;
import com.mycompany.oop.model.Leave;
import com.mycompany.oop.model.OvertimeRequest;
import com.mycompany.oop.model.PayrollPeriod;
import com.mycompany.oop.model.PayrollReadinessIssue;
import com.mycompany.oop.model.WorkforcePayrollReadiness;
import com.mycompany.oop.service.AttendanceAwarenessService;
import com.mycompany.oop.service.AttendanceAdjustmentService;
import com.mycompany.oop.service.EmployeeService;
import com.mycompany.oop.service.LeaveService;
import com.mycompany.oop.service.OvertimeService;
import com.mycompany.oop.service.ScheduleService;
import com.mycompany.oop.service.WorkforceReadinessService;
import com.mycompany.oop.service.WorkflowStageService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class TeamOperationsPanel extends JPanel implements RefreshablePanel {

    private final Employee currentUser;
    private final EmployeeService employeeService;
    private final ScheduleService scheduleService;
    private final LeaveService leaveService;
    private final OvertimeService overtimeService;
    private final AttendanceAwarenessService attendanceAwarenessService;
    private final AttendanceAdjustmentService attendanceAdjustmentService;
    private final WorkflowStageService workflowStageService;
    private final WorkforceReadinessService workforceReadinessService;
    private final Runnable workflowRefreshHandler;

    private List<Employee> teamMembers;
    private Map<Integer, Employee> employeesById;
    private Map<Integer, EmployeeShift> shiftsById;
    private PayrollPeriod activePayrollPeriod;
    private List<WorkforcePayrollReadiness> readinessQueue;
    private List<WorkforcePayrollReadiness> sentToHrQueue;
    private Map<Integer, List<PayrollReadinessIssue>> readinessIssuesByEmployee;
    private JTable readinessQueueTable;
    private JPanel contentBody;
    private JButton readyButton;
    private JButton sendAllButton;
    private JButton returnButton;
    private JButton reviewButton;
    private JButton refreshButton;
    private boolean workflowActionRunning;
    private boolean dataLoading;
    private int lastSyncedPayrollPeriodId;
    private long lastReadinessSyncMs;
    private boolean readinessIssueCacheLoaded;
    private Timer loadingSkeletonTimer;

    public TeamOperationsPanel(Employee currentUser) {
        this(currentUser, null);
    }

    public TeamOperationsPanel(Employee currentUser, Runnable workflowRefreshHandler) {
        this.currentUser = currentUser;
        this.employeeService = new EmployeeService();
        this.scheduleService = new ScheduleService();
        this.leaveService = new LeaveService();
        this.overtimeService = new OvertimeService();
        this.attendanceAwarenessService = new AttendanceAwarenessService();
        this.attendanceAdjustmentService = new AttendanceAdjustmentService();
        this.workflowStageService = new WorkflowStageService();
        this.workforceReadinessService = new WorkforceReadinessService();
        this.workflowRefreshHandler = workflowRefreshHandler;
        this.teamMembers = new ArrayList<>();
        this.employeesById = new HashMap<>();
        this.shiftsById = new HashMap<>();
        this.readinessQueue = new ArrayList<>();
        this.sentToHrQueue = new ArrayList<>();
        this.readinessIssuesByEmployee = new HashMap<>();

        setLayout(new BorderLayout());
        setBackground(UITheme.BG);

        add(UITheme.createTitleBar("Team Operations"), BorderLayout.NORTH);
        contentBody = createSubtleLoadingPanel();
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
        scheduleLoadingSkeleton("Loading team operations...");

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
                    long renderStartMs = System.currentTimeMillis();
                    swapContent(createContent());
                    System.out.println("[perf] TeamOperationsPanel content render took "
                            + (System.currentTimeMillis() - renderStartMs) + " ms");
                    System.out.println("[perf] TeamOperationsPanel refreshData took "
                            + (System.currentTimeMillis() - startedAtMs) + " ms");
                } catch (Exception ex) {
                    swapContent(createErrorPanel("Unable to load Team Operations. Please refresh and try again."));
                } finally {
                    cancelLoadingSkeleton();
                    dataLoading = false;
                    setCursor(Cursor.getDefaultCursor());
                    updateActionButtons();
                }
            }
        };
        worker.execute();
    }

    private JPanel createContent() {
        JPanel content = UITheme.createWorkspacePanel(new BorderLayout(0, 16));
        content.add(createHeader(), BorderLayout.NORTH);

        JPanel sections = new JPanel();
        sections.setOpaque(false);
        sections.setLayout(new BoxLayout(sections, BoxLayout.Y_AXIS));

        sections.add(createSupervisorReadinessQueueSection());
        sections.add(Box.createVerticalStrut(12));
        sections.add(createContextIntro());
        sections.add(Box.createVerticalStrut(8));
        long accordionStartMs = System.currentTimeMillis();
        sections.add(createLazyCollapsibleSection("Team Attendance Snapshot",
                "Daily attendance context for readiness review.",
                this::createTodayTeamAttendanceSection,
                false));
        sections.add(Box.createVerticalStrut(8));
        sections.add(createLazyCollapsibleSection("Schedule Gaps",
                "Dates without assigned schedules that may block readiness.",
                this::createScheduleGapsSection,
                false));
        sections.add(Box.createVerticalStrut(8));
        sections.add(createLazyCollapsibleSection("Pending Overtime",
                "Team overtime requests that may require HR approval.",
                this::createOvertimeRequestsSection,
                false));
        sections.add(Box.createVerticalStrut(8));
        sections.add(createLazyCollapsibleSection("Attendance Corrections",
                "Correction requests that may affect payroll readiness.",
                this::createAttendanceAdjustmentsSection,
                false));
        sections.add(Box.createVerticalStrut(8));
        sections.add(createLazyCollapsibleSection("Team Leave Visibility",
                "Leave context for coverage and HR final approval.",
                this::createLeaveRequestsSection,
                false));
        System.out.println("[perf] TeamOperationsPanel supporting accordion preparation took "
                + (System.currentTimeMillis() - accordionStartMs) + " ms");

        JScrollPane scrollPane = new JScrollPane(sections);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UITheme.BG);
        WorkforceFormToolkit.tuneScrollPane(scrollPane);
        content.add(scrollPane, BorderLayout.CENTER);

        return content;
    }

    private void swapContent(JPanel nextContent) {
        cancelLoadingSkeleton();
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
        panel.add(UITheme.createSkeletonCard(message, 5), BorderLayout.NORTH);
        JPanel pipeline = new JPanel(new GridBagLayout());
        pipeline.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 0, 0, 10);
        gbc.gridx = 0;
        gbc.weightx = 0.48;
        pipeline.add(UITheme.createSkeletonCard("Supervisor Review Queue", 5), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.10;
        pipeline.add(UITheme.createSkeletonCard("Actions", 4), gbc);
        gbc.gridx = 2;
        gbc.weightx = 0.42;
        gbc.insets = new Insets(0, 0, 0, 0);
        pipeline.add(UITheme.createSkeletonCard("Sent to HR", 5), gbc);
        panel.add(pipeline, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSubtleLoadingPanel() {
        JPanel panel = UITheme.createWorkspacePanel(new BorderLayout());
        panel.add(UITheme.createSkeletonCard("Preparing Team Operations", 2), BorderLayout.NORTH);
        return panel;
    }

    private void scheduleLoadingSkeleton(String message) {
        cancelLoadingSkeleton();
        loadingSkeletonTimer = new Timer(180, event -> {
            if (!dataLoading) {
                return;
            }
            if (contentBody != null) {
                remove(contentBody);
            }
            contentBody = createLoadingPanel(message);
            add(contentBody, BorderLayout.CENTER);
            revalidate();
            repaint();
        });
        loadingSkeletonTimer.setRepeats(false);
        loadingSkeletonTimer.start();
    }

    private void cancelLoadingSkeleton() {
        if (loadingSkeletonTimer != null) {
            loadingSkeletonTimer.stop();
            loadingSkeletonTimer = null;
        }
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
        String subtitle = isSupervisor()
                ? "Review your team's workforce issues for the active payroll period. Clear operational blockers before HR validation."
                : "Workforce operations visibility for team attendance, schedules, leave, overtime, and corrections.";
        return WorkforceFormToolkit.createSection("Team Operations", subtitle);
    }

    private JPanel createContextIntro() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        JLabel label = new JLabel("Supporting context is collapsed below. Use it to inspect details after reviewing the main queue.");
        label.setFont(UITheme.FONT_SMALL);
        label.setForeground(UITheme.TEXT_SECONDARY);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSupervisorReadinessQueueSection() {
        JPanel section = WorkforceFormToolkit.createSection(
                "Supervisor Review → HR Validation",
                "Assigned-team rows move from Supervisor Review into HR Validation after endorsement."
        );

        if (activePayrollPeriod == null) {
            section.add(createEmptyState("No active payroll period is open for workforce review."), BorderLayout.CENTER);
            return section;
        }

        JPanel summary = new JPanel(new BorderLayout(8, 8));
        summary.setOpaque(false);
        JLabel helper = new JLabel("Review your team's workforce issues for the active payroll period. Clear operational blockers before HR validation.");
        helper.setFont(UITheme.FONT_SMALL);
        helper.setForeground(UITheme.TEXT_SECONDARY);

        JLabel period = new JLabel("Active period: " + activePayrollPeriod.getCutoffPeriod()
                + " • " + activePayrollPeriod.getPeriodStart() + " to " + activePayrollPeriod.getPeriodEnd()
                + " • " + activePayrollPeriod.getStatus()
                + "     Supervisor Review Pending: " + readinessQueue.size()
                + " • Sent to HR: " + sentToHrQueue.size());
        period.setFont(UITheme.FONT_SMALL);
        period.setForeground(UITheme.TEXT_SECONDARY);

        summary.add(helper, BorderLayout.NORTH);
        summary.add(period, BorderLayout.CENTER);
        section.add(summary, BorderLayout.NORTH);

        readinessQueueTable = createSupervisorQueueTable();
        JPanel left = createPipelineTablePanel(
                "Supervisor Review Queue",
                readinessQueue.size() + " Pending",
                UITheme.DANGER,
                UITheme.createTableScrollPane(readinessQueueTable));
        JPanel center = createSupervisorActionStagePanel();
        JPanel right = createPipelineTablePanel(
                "Sent to HR / Waiting Validation",
                sentToHrQueue.size() + " Sent",
                UITheme.BLUE,
                UITheme.createTableScrollPane(createSentToHrTable()));

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
        updateActionButtons();
        return section;
    }

    private JPanel createSupervisorActionStagePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(6, 2, 6, 2));
        panel.setMinimumSize(new Dimension(92, 210));
        panel.setPreferredSize(new Dimension(104, 0));

        JLabel title = new JLabel("ACTIONS", JLabel.CENTER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(UITheme.FONT_BODY_BOLD);
        title.setForeground(UITheme.TEXT_PRIMARY);
        reviewButton = UITheme.createCompactWorkflowButton("Review", false);
        readyButton = UITheme.createCompactWorkflowButton(">> Send", true);
        sendAllButton = UITheme.createCompactWorkflowButton(">> All", false);
        returnButton = UITheme.createCompactWorkflowButton("<< Return", false);
        refreshButton = UITheme.createCompactWorkflowButton("Refresh", false);
        reviewButton.addActionListener(e -> showSelectedWorkforceDetails());
        readyButton.addActionListener(e -> markSelectedReadyForHr());
        sendAllButton.addActionListener(e -> sendReadyEmployeesToHr());
        returnButton.addActionListener(e -> {
            WorkforcePayrollReadiness selected = getSelectedReadiness();
            if (selected == null) {
                JOptionPane.showMessageDialog(this,
                        "Select an employee readiness row first.",
                        "Return / Correct",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            keepPendingWithRemarks(selected);
        });
        refreshButton.addActionListener(e -> refreshData(true));
        for (JButton button : List.of(reviewButton, readyButton, sendAllButton, returnButton, refreshButton)) {
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        panel.add(title);
        panel.add(Box.createVerticalStrut(8));
        panel.add(reviewButton);
        panel.add(Box.createVerticalStrut(5));
        panel.add(readyButton);
        panel.add(Box.createVerticalStrut(5));
        panel.add(sendAllButton);
        panel.add(Box.createVerticalStrut(5));
        panel.add(returnButton);
        panel.add(Box.createVerticalStrut(5));
        panel.add(refreshButton);
        return panel;
    }

    private JTable createSupervisorQueueTable() {
        long modelStartMs = System.currentTimeMillis();
        DefaultTableModel model = createReadOnlyModel(new String[]{
                "Employee", "Issue Summary", "Status"
        });

        for (WorkforcePayrollReadiness readiness : readinessQueue) {
            Employee employee = employeesById.get(readiness.getEmployeeId());
            List<PayrollReadinessIssue> issues = readinessIssuesByEmployee.getOrDefault(readiness.getEmployeeId(), List.of());
            model.addRow(new Object[]{
                    readiness.getEmployeeId() + " - " + (employee == null ? "Employee #" + readiness.getEmployeeId() : fullName(employee)),
                    recommendedAction(readiness, issues),
                    formatReadinessStatus(readiness.getReadinessStatus())
            });
        }

        if (model.getRowCount() == 0) {
            model.addRow(new Object[]{"-", "No supervisor-owned rows awaiting review.", "Stable"});
        }
        System.out.println("[perf] TeamOperationsPanel supervisor table model build took "
                + (System.currentTimeMillis() - modelStartMs) + " ms");

        JTable table = new JTable(model);
        UITheme.styleTable(table);
        table.setRowHeight(34);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        UITheme.setColumnWidths(table, 230, 260, 140);
        table.setDefaultRenderer(Object.class, new ReadinessQueueRenderer());
        table.getSelectionModel().addListSelectionListener(e -> updateActionButtons());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showSelectedWorkforceDetails();
                }
            }
        });
        return table;
    }

    private JTable createSentToHrTable() {
        long modelStartMs = System.currentTimeMillis();
        DefaultTableModel model = createReadOnlyModel(new String[]{"Employee", "Status", "Sent On"});
        for (WorkforcePayrollReadiness readiness : sentToHrQueue) {
            Employee employee = employeesById.get(readiness.getEmployeeId());
            model.addRow(new Object[]{
                    readiness.getEmployeeId() + " - " + (employee == null ? "Employee #" + readiness.getEmployeeId() : fullName(employee)),
                    formatReadinessStatus(readiness.getReadinessStatus()),
                    readiness.getSupervisorClearedAt() == null ? "-" : readiness.getSupervisorClearedAt()
            });
        }
        if (model.getRowCount() == 0) {
            model.addRow(new Object[]{"-", "No employees sent to HR yet.", "-"});
        }
        System.out.println("[perf] TeamOperationsPanel sent table model build took "
                + (System.currentTimeMillis() - modelStartMs) + " ms");
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

    private JPanel createCollapsibleSection(String title, String subtitle, JPanel body, boolean expanded) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(UITheme.BG);
        wrapper.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));

        JPanel header = new JPanel(new BorderLayout(10, 0));
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

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(hint);

        body.setVisible(expanded);
        toggle.addActionListener(e -> {
            boolean show = !body.isVisible();
            body.setVisible(show);
            toggle.setText((show ? "▾  " : "▸  ") + title);
            wrapper.revalidate();
            wrapper.repaint();
        });

        header.add(toggle, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        wrapper.add(header, BorderLayout.NORTH);
        wrapper.add(body, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createLazyCollapsibleSection(String title, String subtitle, Supplier<JPanel> bodyFactory,
            boolean expanded) {
        JPanel placeholder = new JPanel(new BorderLayout());
        placeholder.setOpaque(false);
        final boolean[] loaded = {false};

        if (expanded) {
            JPanel body = bodyFactory.get();
            loaded[0] = true;
            return createCollapsibleSection(title, subtitle, body, true);
        }

        placeholder.add(createEmptyState("Expand to load supporting context."), BorderLayout.CENTER);
        JPanel wrapper = createCollapsibleSection(title, subtitle, placeholder, false);
        JButton toggle = findToggleButton(wrapper);
        if (toggle != null) {
            toggle.addActionListener(e -> {
                if (loaded[0] || !placeholder.isVisible()) {
                    return;
                }
                long sectionStartMs = System.currentTimeMillis();
                placeholder.removeAll();
                placeholder.add(bodyFactory.get(), BorderLayout.CENTER);
                loaded[0] = true;
                placeholder.revalidate();
                placeholder.repaint();
                System.out.println("[perf] TeamOperationsPanel supporting section '" + title + "' load took "
                        + (System.currentTimeMillis() - sectionStartMs) + " ms");
            });
        }
        return wrapper;
    }

    private JButton findToggleButton(JPanel wrapper) {
        if (wrapper == null || wrapper.getComponentCount() == 0
                || !(wrapper.getComponent(0) instanceof JPanel header)) {
            return null;
        }
        for (Component component : header.getComponents()) {
            if (component instanceof JButton button) {
                return button;
            }
        }
        return null;
    }

    private JPanel createTeamMembersSection() {
        JPanel section = WorkforceFormToolkit.createSection(
                "Team Members",
                "Assigned employees and current workforce status."
        );

        if (teamMembers.isEmpty()) {
            section.add(createEmptyState("No assigned team members yet."), BorderLayout.CENTER);
            return section;
        }

        DefaultTableModel model = createReadOnlyModel(new String[]{
                "Employee ID", "Name", "Position", "Status", "Department"
        });

        for (Employee employee : teamMembers) {
            model.addRow(new Object[]{
                    employee.getEmployeeId(),
                    fullName(employee),
                    safeText(employee.getPosition(), "Not set"),
                    safeText(employee.getEmploymentStatus(), "Not set"),
                    getDepartmentLabel(employee)
            });
        }

        section.add(createTableScroll(model, 180), BorderLayout.CENTER);
        return section;
    }

    private JPanel createTeamScheduleSection() {
        JPanel section = WorkforceFormToolkit.createSection(
                "Upcoming Team Schedule",
                "Assigned schedules for the next 30 days after schedule gaps have been reviewed."
        );

        if (teamMembers.isEmpty()) {
            section.add(createEmptyState("No assigned team members yet."), BorderLayout.CENTER);
            return section;
        }

        if (!scheduleService.isSchedulingSchemaAvailable()) {
            section.add(createEmptyState("Team schedule visibility is not yet available in the operational database."),
                    BorderLayout.CENTER);
            return section;
        }
        loadShiftCacheIfNeeded();

        DefaultTableModel model = createReadOnlyModel(new String[]{
                "Employee", "Date", "Shift", "Schedule Type", "Status"
        });

        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(30);
        for (EmployeeSchedule schedule : scheduleService.getEmployeeSchedulesByDateRange(getTeamEmployeeIds(), start, end)) {
            Employee employee = employeesById.get(schedule.getEmployeeId());
            EmployeeShift shift = schedule.getShiftId() == null ? null : shiftsById.get(schedule.getShiftId());

            model.addRow(new Object[]{
                    employee == null ? "Employee #" + schedule.getEmployeeId() : fullName(employee),
                    schedule.getScheduleDate(),
                    shift == null ? "No shift setup" : shift.getShiftName() + " (" + shift.getStartTime() + " - " + shift.getEndTime() + ")",
                    schedule.isRestDay() ? "Rest Day" : "Assigned Shift",
                    safeText(schedule.getStatus(), "Assigned")
            });
        }

        addEmptyRowIfNeeded(model, "No upcoming team schedules assigned.");
        section.add(createTableScroll(model, 210), BorderLayout.CENTER);
        return section;
    }

    private JPanel createTodayTeamAttendanceSection() {
        JPanel section = WorkforceFormToolkit.createSection(
                "Team Attendance Snapshot",
                "Daily timekeeping context for assigned team members."
        );

        if (teamMembers.isEmpty()) {
            section.add(createEmptyState("No assigned team members yet."), BorderLayout.CENTER);
            return section;
        }

        DefaultTableModel model = createReadOnlyModel(new String[]{
                "Employee", "Status", "Alert", "Action Needed"
        });

        LocalDate today = LocalDate.now();
        for (Employee employee : teamMembers) {
            List<AttendanceAwareness> todayItems = attendanceAwarenessService.getEmployeeAwareness(
                    employee.getEmployeeId(), today, today);
            AttendanceAwareness issue = todayItems.stream()
                    .filter(AttendanceAwareness::requiresReview)
                    .findFirst()
                    .orElse(null);

            model.addRow(new Object[]{
                    fullName(employee),
                    issue == null ? "Normal" : issue.getStatus(),
                    issue == null ? "No attendance concern detected" : issue.getMessage(),
                    issue == null ? "No action needed" : "Review timekeeping"
            });
        }

        section.add(createTableScroll(model, 180), BorderLayout.CENTER);
        return section;
    }

    private JPanel createScheduleGapsSection() {
        JPanel section = WorkforceFormToolkit.createSection(
                "Schedule Gaps",
                "Assigned-team dates without a work schedule in the next 14 days."
        );

        if (teamMembers.isEmpty()) {
            section.add(createEmptyState("No assigned team members yet."), BorderLayout.CENTER);
            return section;
        }

        if (!scheduleService.isSchedulingSchemaAvailable()) {
            section.add(createEmptyState("Schedule gap visibility is not yet available in the operational database."),
                    BorderLayout.CENTER);
            return section;
        }

        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(14);
        Set<String> assignedDates = new HashSet<>();
        for (EmployeeSchedule schedule : scheduleService.getEmployeeSchedulesByDateRange(getTeamEmployeeIds(), start, end)) {
            if (schedule.getScheduleDate() != null) {
                assignedDates.add(schedule.getEmployeeId() + "|" + schedule.getScheduleDate());
            }
        }

        DefaultTableModel model = createReadOnlyModel(new String[]{
                "Employee", "Date", "Gap", "Recommended Follow-up"
        });

        for (Employee employee : teamMembers) {
            LocalDate cursor = start;
            while (!cursor.isAfter(end)) {
                if (!assignedDates.contains(employee.getEmployeeId() + "|" + cursor)) {
                    model.addRow(new Object[]{
                            fullName(employee),
                            cursor,
                            "No assigned schedule",
                            "Coordinate schedule assignment"
                    });
                }
                cursor = cursor.plusDays(1);
            }
        }

        addEmptyRowIfNeeded(model, "No schedule gaps found for the next 14 days.");
        section.add(createTableScroll(model, 190), BorderLayout.CENTER);
        return section;
    }

    private JPanel createLeaveRequestsSection() {
        JPanel section = WorkforceFormToolkit.createSection(
                "Team Leave Visibility",
                isSupervisor()
                        ? "Team leave visibility for planning. Final approval remains with HR."
                        : "Team leave status for workforce coverage planning."
        );

        if (teamMembers.isEmpty()) {
            section.add(createEmptyState("No assigned team members yet."), BorderLayout.CENTER);
            return section;
        }

        DefaultTableModel model = createReadOnlyModel(new String[]{
                "Employee", "Leave Type", "Start", "End", "Stage", "Owner"
        });

        for (Leave leave : leaveService.getLeavesByEmployees(getTeamEmployeeIds())) {
            Employee employee = employeesById.get(leave.getEmployeeId());
            String stage = workflowStageService.leaveStage(leave, currentUser);
            model.addRow(new Object[]{
                    employee == null ? "Employee #" + leave.getEmployeeId() : fullName(employee),
                    safeText(leave.getLeaveType(), "Leave"),
                    safeText(leave.getStartDate(), "Not set"),
                    safeText(leave.getEndDate(), "Not set"),
                    stage,
                    workflowStageService.ownerForStage(stage)
            });
        }

        addEmptyRowIfNeeded(model, "No team leave requests found.");
        section.add(createTableScroll(model, 190), BorderLayout.CENTER);
        return section;
    }

    private JPanel createAttendanceAwarenessSection() {
        JPanel section = WorkforceFormToolkit.createSection(
                "Attendance Awareness",
                "Recent timekeeping visibility for the last 7 days. This does not change attendance computation."
        );

        if (teamMembers.isEmpty()) {
            section.add(createEmptyState("No assigned team members yet."), BorderLayout.CENTER);
            return section;
        }

        DefaultTableModel model = createReadOnlyModel(new String[]{
                "Employee", "Issues", "Latest Alert", "Date", "Status"
        });

        LocalDate since = LocalDate.now().minusDays(7);
        for (Employee employee : teamMembers) {
            List<AttendanceAwareness> recent = attendanceAwarenessService.getEmployeeAwareness(
                    employee.getEmployeeId(), since, LocalDate.now());
            List<AttendanceAwareness> issues = recent.stream()
                    .filter(AttendanceAwareness::requiresReview)
                    .toList();
            AttendanceAwareness latest = issues.isEmpty() ? null : issues.get(issues.size() - 1);
            model.addRow(new Object[]{
                    fullName(employee),
                    issues.size(),
                    latest == null ? "No unresolved concerns" : latest.getStatus(),
                    latest == null ? "-" : latest.getDate(),
                    latest == null ? "Normal" : latest.getMessage()
            });
        }

        section.add(createTableScroll(model, 190), BorderLayout.CENTER);
        return section;
    }

    private JPanel createOvertimeRequestsSection() {
        JPanel section = WorkforceFormToolkit.createSection(
                "Pending Overtime",
                isSupervisor()
                        ? "Pending team overtime visibility only. Final approval remains with HR."
                        : "Pending overtime requests for workforce coverage review."
        );

        if (teamMembers.isEmpty()) {
            section.add(createEmptyState("No assigned team members yet."), BorderLayout.CENTER);
            return section;
        }

        DefaultTableModel model = createReadOnlyModel(new String[]{
                "Employee", "Date", "Hours", "Stage", "Owner", "Reason"
        });

        for (OvertimeRequest request : overtimeService.getRequestsByEmployees(getTeamEmployeeIds())) {
            if (!isPending(request.getStatus())) {
                continue;
            }
            Employee employee = employeesById.get(request.getEmployeeId());
            String stage = workflowStageService.overtimeStage(request, currentUser);
            model.addRow(new Object[]{
                    employee == null ? "Employee #" + request.getEmployeeId() : fullName(employee),
                    request.getOvertimeDate(),
                    request.getOvertimeHours(),
                    stage,
                    workflowStageService.ownerForStage(stage),
                    safeText(request.getReason(), "-")
            });
        }

        addEmptyRowIfNeeded(model, "No pending team overtime requests found.");
        section.add(createTableScroll(model, 190), BorderLayout.CENTER);
        return section;
    }

    private JPanel createAttendanceAdjustmentsSection() {
        JPanel section = WorkforceFormToolkit.createSection(
                "Attendance Corrections",
                isSupervisor()
                        ? "Team correction request visibility only. HR processes corrections."
                        : "Attendance correction requests for workforce governance review."
        );

        if (teamMembers.isEmpty()) {
            section.add(createEmptyState("No assigned team members yet."), BorderLayout.CENTER);
            return section;
        }

        DefaultTableModel model = createReadOnlyModel(new String[]{
                "Employee", "Date", "Type", "Stage", "Owner", "Remarks"
        });

        for (AttendanceAdjustment adjustment : attendanceAdjustmentService.getRequestsByEmployees(getTeamEmployeeIds())) {
            Employee employee = employeesById.get(adjustment.getEmployeeId());
            String stage = workflowStageService.attendanceAdjustmentStage(adjustment, currentUser);
            model.addRow(new Object[]{
                    employee == null ? "Employee #" + adjustment.getEmployeeId() : fullName(employee),
                    adjustment.getAttendanceDate(),
                    safeText(adjustment.getAdjustmentType(), "-"),
                    stage,
                    workflowStageService.ownerForStage(stage),
                    safeText(adjustment.getRemarks(), "-")
            });
        }

        addEmptyRowIfNeeded(model, "No team attendance adjustment requests found.");
        section.add(createTableScroll(model, 190), BorderLayout.CENTER);
        return section;
    }

    private void loadData(boolean forceReadinessSync) {
        long startedAtMs = System.currentTimeMillis();
        long teamStartMs = System.currentTimeMillis();
        teamMembers = employeeService.getTeamOperationsEmployees(currentUser);
        System.out.println("[perf] TeamOperationsPanel team load took "
                + (System.currentTimeMillis() - teamStartMs) + " ms");
        employeesById.clear();
        shiftsById.clear();
        readinessQueue = new ArrayList<>();
        sentToHrQueue = new ArrayList<>();
        readinessIssuesByEmployee = new HashMap<>();
        readinessIssueCacheLoaded = false;

        for (Employee employee : teamMembers) {
            employeesById.put(employee.getEmployeeId(), employee);
        }

        long periodStartMs = System.currentTimeMillis();
        activePayrollPeriod = workforceReadinessService.findLatestActivePayrollPeriod();
        System.out.println("[perf] TeamOperationsPanel active period lookup took "
                + (System.currentTimeMillis() - periodStartMs) + " ms");
        if (activePayrollPeriod != null && currentUser != null) {
            syncReadinessIfNeeded(forceReadinessSync);
            long supervisorQueueStartMs = System.currentTimeMillis();
            List<WorkforcePayrollReadiness> supervisorRows = workforceReadinessService.getSupervisorReadiness(
                    activePayrollPeriod.getPeriodId(), currentUser.getEmployeeId());
            System.out.println("[perf] TeamOperationsPanel supervisor queue load took "
                    + (System.currentTimeMillis() - supervisorQueueStartMs) + " ms");

            long summaryStartMs = System.currentTimeMillis();
            Set<Integer> teamIds = new HashSet<>(getTeamEmployeeIds());
            readinessQueue = supervisorRows.stream()
                    .filter(this::isSupervisorActionable)
                    .filter(readiness -> teamIds.contains(readiness.getEmployeeId()))
                    .toList();
            System.out.println("[perf] TeamOperationsPanel supervisor queue summary took "
                    + (System.currentTimeMillis() - summaryStartMs) + " ms");

            long sentQueueStartMs = System.currentTimeMillis();
            sentToHrQueue = supervisorRows.stream()
                    .filter(this::isSentForward)
                    .filter(readiness -> teamIds.contains(readiness.getEmployeeId()))
                    .toList();
            System.out.println("[perf] TeamOperationsPanel sent-to-HR queue load took "
                    + (System.currentTimeMillis() - sentQueueStartMs) + " ms");
        }
        System.out.println("[perf] TeamOperationsPanel loadData took "
                + (System.currentTimeMillis() - startedAtMs) + " ms");
    }

    private void syncReadinessIfNeeded(boolean forceReadinessSync) {
        if (activePayrollPeriod == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!forceReadinessSync) {
            System.out.println("[perf] TeamOperationsPanel readiness sync skipped on navigation");
            return;
        }
        long syncStartMs = System.currentTimeMillis();
        workforceReadinessService.syncReadinessForPeriod(activePayrollPeriod);
        lastSyncedPayrollPeriodId = activePayrollPeriod.getPeriodId();
        lastReadinessSyncMs = now;
        System.out.println("[perf] TeamOperationsPanel readiness sync took "
                + (System.currentTimeMillis() - syncStartMs) + " ms");
    }

    private List<Integer> getTeamEmployeeIds() {
        List<Integer> employeeIds = new ArrayList<>();
        for (Employee employee : teamMembers) {
            employeeIds.add(employee.getEmployeeId());
        }
        return employeeIds;
    }

    private JScrollPane createTableScroll(DefaultTableModel model, int height) {
        JTable table = new JTable(model);
        UITheme.styleTable(table);
        table.setFocusable(false);
        table.setRowSelectionAllowed(false);

        JScrollPane scrollPane = UITheme.createTableScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(0, height));
        return scrollPane;
    }

    private DefaultTableModel createReadOnlyModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void addEmptyRowIfNeeded(DefaultTableModel model, String message) {
        if (model.getRowCount() == 0) {
            Object[] row = new Object[model.getColumnCount()];
            row[0] = message;
            for (int i = 1; i < row.length; i++) {
                row[i] = "";
            }
            model.addRow(row);
        }
    }

    private WorkforcePayrollReadiness getSelectedReadiness() {
        if (readinessQueueTable == null || readinessQueue == null || readinessQueue.isEmpty()) {
            return null;
        }
        int row = readinessQueueTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        int modelRow = readinessQueueTable.convertRowIndexToModel(row);
        if (modelRow < 0 || modelRow >= readinessQueue.size()) {
            return null;
        }
        return readinessQueue.get(modelRow);
    }

    private void updateActionButtons() {
        WorkforcePayrollReadiness selected = getSelectedReadiness();
        boolean actionable = selected != null && isSupervisorActionable(selected) && !workflowActionRunning;
        if (readyButton != null) {
            readyButton.setEnabled(actionable);
            readyButton.setToolTipText(actionable
                    ? "Mark selected employee ready for HR validation"
                    : "Selected item is already sent forward or unavailable for supervisor action");
        }
        boolean hasBulkCandidates = readinessQueue != null
                && readinessQueue.stream().anyMatch(this::isSupervisorActionable)
                && !workflowActionRunning;
        if (sendAllButton != null) {
            sendAllButton.setEnabled(hasBulkCandidates);
            sendAllButton.setToolTipText(hasBulkCandidates
                    ? "Send visible eligible supervisor-owned rows to HR"
                    : "No eligible supervisor-owned rows are ready to send");
        }
        if (returnButton != null) {
            returnButton.setEnabled(actionable);
            returnButton.setToolTipText(actionable
                    ? "Return the selected item for correction with remarks"
                    : "Select a supervisor-owned pending row to return or correct");
        }
        if (reviewButton != null) {
            reviewButton.setEnabled(selected != null && !workflowActionRunning);
        }
        if (refreshButton != null) {
            refreshButton.setEnabled(!workflowActionRunning);
        }
    }

    private void showSelectedWorkforceDetails() {
        WorkforcePayrollReadiness readiness = getSelectedReadiness();
        if (readiness == null) {
            JOptionPane.showMessageDialog(this,
                    "Select an employee readiness row first.",
                    "Workforce Details",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        showWorkforceReviewDialog(readiness);
    }

    private void showWorkforceReviewDialog(WorkforcePayrollReadiness readiness) {
        Employee employee = employeesById.get(readiness.getEmployeeId());
        List<PayrollReadinessIssue> issues = loadIssuesForEmployee(readiness.getEmployeeId());
        String employeeName = employee == null ? "Employee #" + readiness.getEmployeeId() : fullName(employee);

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel header = new JLabel("<html><b>" + employeeName + "</b> &nbsp; #" + readiness.getEmployeeId()
                + "<br><span style='color:#64748b'>Readiness: "
                + formatReadinessStatus(readiness.getReadinessStatus())
                + " • Owner: " + formatOwner(readiness.getCurrentOwnerRole()) + "</span></html>");
        header.setFont(UITheme.FONT_BODY);
        panel.add(header, BorderLayout.NORTH);

        JPanel summary = new JPanel(new GridLayout(0, 2, 8, 6));
        summary.add(createReviewMetric("Attendance issues", String.valueOf(countIssueMatches(issues, "attendance", "timekeeping"))));
        summary.add(createReviewMetric("Schedule gaps", String.valueOf(countIssueMatches(issues, "schedule", "shift"))));
        summary.add(createReviewMetric("Pending overtime", String.valueOf(countPendingOvertime(readiness.getEmployeeId()))));
        summary.add(createReviewMetric("Attendance corrections", String.valueOf(countPendingCorrections(readiness.getEmployeeId()))));
        summary.add(createReviewMetric("Leave visibility", String.valueOf(countVisibleLeaves(readiness.getEmployeeId()))));
        summary.add(createReviewMetric("Total blockers", String.valueOf(issues.size())));
        panel.add(summary, BorderLayout.CENTER);

        JTextArea blockerText = new JTextArea(buildBlockerSummary(issues));
        blockerText.setRows(7);
        blockerText.setLineWrap(true);
        blockerText.setWrapStyleWord(true);
        blockerText.setEditable(false);
        blockerText.setFont(UITheme.FONT_SMALL);
        blockerText.setForeground(UITheme.TEXT_PRIMARY);
        blockerText.setBackground(new Color(248, 250, 252));
        blockerText.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(new JScrollPane(blockerText), BorderLayout.SOUTH);

        Object[] options = {"Mark Workforce Ready", "Return / Keep Pending", "Close"};
        int choice = JOptionPane.showOptionDialog(this,
                panel,
                "Supervisor Workforce Review",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]);

        if (choice == 0) {
            markReadyForHr(readiness, issues);
        } else if (choice == 1) {
            keepPendingWithRemarks(readiness);
        }
    }

    private void markSelectedReadyForHr() {
        WorkforcePayrollReadiness readiness = getSelectedReadiness();
        if (readiness == null) {
            JOptionPane.showMessageDialog(this,
                    "Select an employee readiness row first.",
                    "Mark Ready for HR Review",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        List<PayrollReadinessIssue> issues = loadIssuesForEmployee(readiness.getEmployeeId());
        markReadyForHr(readiness, issues);
    }

    private void markReadyForHr(WorkforcePayrollReadiness readiness, List<PayrollReadinessIssue> issues) {
        if (!isSupervisorActionable(readiness)) {
            JOptionPane.showMessageDialog(this,
                    "This employee has already been sent forward in the workforce workflow.",
                    "Already Sent",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String warning = issues == null || issues.isEmpty()
                ? "Mark this employee ready for HR workforce validation?"
                : "This employee still has " + issues.size() + " workforce blocker"
                + (issues.size() == 1 ? "" : "s")
                + ". Marking ready will formally clear the item for HR visibility.\n\nContinue?";

        int choice = JOptionPane.showConfirmDialog(this,
                warning,
                "Mark Ready for HR Review",
                JOptionPane.YES_NO_OPTION,
                issues == null || issues.isEmpty() ? JOptionPane.QUESTION_MESSAGE : JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        runWorkflowAction("Sending employee to HR...", () -> {
            workforceReadinessService.markReadyForHrReview(readiness.getReadinessId(), currentUser);
            return "Employee was sent to HR Workforce Validation.";
        }, "Ready for HR Validation");
    }

    private void keepPendingWithRemarks(WorkforcePayrollReadiness readiness) {
        String remarks = JOptionPane.showInputDialog(this,
                "Optional remarks for keeping this workforce item pending:",
                "Return / Keep Pending",
                JOptionPane.PLAIN_MESSAGE);
        if (remarks == null) {
            return;
        }

        runWorkflowAction("Updating workforce item...", () -> {
            workforceReadinessService.keepPendingWithRemarks(readiness.getReadinessId(), currentUser, remarks);
            return "Workforce item remains pending for supervisor follow-up.";
        }, "Workforce Review Pending");
    }

    private void sendReadyEmployeesToHr() {
        if (activePayrollPeriod != null) {
            long bulkIssueStartMs = System.currentTimeMillis();
            readinessIssuesByEmployee = new HashMap<>(
                    workforceReadinessService.getReadinessIssuesByEmployee(activePayrollPeriod));
            readinessIssueCacheLoaded = true;
            System.out.println("[perf] TeamOperationsPanel bulk blocker detail load took "
                    + (System.currentTimeMillis() - bulkIssueStartMs) + " ms");
        }
        List<WorkforcePayrollReadiness> readyRows = readinessQueue.stream()
                .filter(this::isBulkReadyCandidate)
                .toList();
        if (readyRows.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No blocker-free supervisor-owned rows are ready to send to HR.",
                    "Send Ready Employees to HR",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Send " + readyRows.size() + " ready employee"
                        + (readyRows.size() == 1 ? "" : "s")
                        + " to HR Workforce Validation?",
                "Send Ready Employees to HR",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        runWorkflowAction("Sending ready employees to HR...", () -> {
            int success = 0;
            List<String> failures = new ArrayList<>();
            for (WorkforcePayrollReadiness readiness : readyRows) {
                try {
                    workforceReadinessService.markReadyForHrReview(readiness.getReadinessId(), currentUser);
                    success++;
                } catch (Exception ex) {
                    failures.add("Employee #" + readiness.getEmployeeId() + ": " + ex.getMessage());
                }
            }

            String message = success + " employee" + (success == 1 ? "" : "s")
                    + " sent to HR Workforce Validation.";
            if (!failures.isEmpty()) {
                message += "\n\nSkipped:\n- " + String.join("\n- ", failures);
            }
            return message;
        }, "Send Ready Employees to HR");
    }

    private void runWorkflowAction(String busyMessage, WorkflowTask task, String title) {
        setWorkflowBusy(true);
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return task.run();
            }

            @Override
            protected void done() {
                setWorkflowBusy(false);
                try {
                    String message = get();
                    long badgeStartMs = System.currentTimeMillis();
                    notifyWorkflowRefresh();
                    System.out.println("[perf] TeamOperationsPanel badge/workflow refresh callback took "
                            + (System.currentTimeMillis() - badgeStartMs) + " ms");
                    refreshQueuesAfterAction();
                    JOptionPane.showMessageDialog(TeamOperationsPanel.this,
                            message,
                            title,
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(TeamOperationsPanel.this,
                            ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage(),
                            title,
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        };
        if (refreshButton != null) {
            refreshButton.setText(busyMessage);
        }
        worker.execute();
    }

    private void refreshQueuesAfterAction() {
        if (dataLoading) {
            return;
        }
        dataLoading = true;
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private long startedAtMs;

            @Override
            protected Void doInBackground() {
                startedAtMs = System.currentTimeMillis();
                loadData(false);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    long renderStartMs = System.currentTimeMillis();
                    swapContent(createContent());
                    System.out.println("[perf] TeamOperationsPanel post-action render took "
                            + (System.currentTimeMillis() - renderStartMs) + " ms");
                    System.out.println("[perf] TeamOperationsPanel post-action queue refresh took "
                            + (System.currentTimeMillis() - startedAtMs) + " ms");
                } catch (Exception ex) {
                    swapContent(createErrorPanel("Unable to refresh Team Operations. Please refresh and try again."));
                } finally {
                    dataLoading = false;
                    updateActionButtons();
                }
            }
        };
        worker.execute();
    }

    private void setWorkflowBusy(boolean busy) {
        workflowActionRunning = busy;
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
        updateActionButtons();
        if (!busy && refreshButton != null) {
            refreshButton.setText("Refresh");
        }
    }

    private boolean isSupervisorActionable(WorkforcePayrollReadiness readiness) {
        if (readiness == null) {
            return false;
        }
        String status = safeText(readiness.getReadinessStatus(), "").toUpperCase(Locale.ENGLISH);
        String owner = safeText(readiness.getCurrentOwnerRole(), "").toUpperCase(Locale.ENGLISH);
        return WorkforcePayrollReadiness.OWNER_SUPERVISOR.equals(owner)
                && (WorkforcePayrollReadiness.STATUS_OPEN_WORKFORCE_ISSUES.equals(status)
                || WorkforcePayrollReadiness.STATUS_PENDING_SUPERVISOR_REVIEW.equals(status)
                || WorkforcePayrollReadiness.STATUS_RETURNED_TO_SUPERVISOR.equals(status));
    }

    private boolean isBulkReadyCandidate(WorkforcePayrollReadiness readiness) {
        return isSupervisorActionable(readiness)
                && readinessIssuesByEmployee.getOrDefault(readiness.getEmployeeId(), List.of()).isEmpty();
    }

    private List<PayrollReadinessIssue> loadIssuesForEmployee(int employeeId) {
        if (activePayrollPeriod == null || employeeId <= 0) {
            return List.of();
        }
        long detailStartMs = System.currentTimeMillis();
        if (!readinessIssueCacheLoaded) {
            readinessIssuesByEmployee = new HashMap<>(
                    workforceReadinessService.getReadinessIssuesByEmployee(activePayrollPeriod));
            readinessIssueCacheLoaded = true;
            System.out.println("[perf] TeamOperationsPanel blocker detail load took "
                    + (System.currentTimeMillis() - detailStartMs) + " ms");
        } else {
            System.out.println("[perf] TeamOperationsPanel blocker detail cache lookup took "
                    + (System.currentTimeMillis() - detailStartMs) + " ms");
        }
        return readinessIssuesByEmployee.getOrDefault(employeeId, List.of());
    }

    private void loadShiftCacheIfNeeded() {
        if (!shiftsById.isEmpty() || !scheduleService.isSchedulingReferenceSchemaAvailable()) {
            return;
        }
        for (EmployeeShift shift : scheduleService.getEmployeeShifts()) {
            shiftsById.put(shift.getShiftId(), shift);
        }
    }

    private boolean isSentForward(WorkforcePayrollReadiness readiness) {
        if (readiness == null) {
            return false;
        }
        String status = safeText(readiness.getReadinessStatus(), "").toUpperCase(Locale.ENGLISH);
        String owner = safeText(readiness.getCurrentOwnerRole(), "").toUpperCase(Locale.ENGLISH);
        return WorkforcePayrollReadiness.OWNER_HR.equals(owner)
                && (WorkforcePayrollReadiness.STATUS_SUPERVISOR_CLEARED.equals(status)
                || WorkforcePayrollReadiness.STATUS_PENDING_HR_VALIDATION.equals(status)
                || WorkforcePayrollReadiness.STATUS_HR_VALIDATED.equals(status));
    }

    private JLabel createReviewMetric(String label, String value) {
        JLabel metric = new JLabel("<html><span style='color:#64748b'>" + label + ":</span> <b>"
                + value + "</b></html>");
        metric.setFont(UITheme.FONT_SMALL);
        return metric;
    }

    private String buildBlockerSummary(List<PayrollReadinessIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return "No current blocker details found. Review supporting context below the queue, then mark ready when appropriate.";
        }
        StringBuilder details = new StringBuilder("Blocker summary:\n");
        for (PayrollReadinessIssue issue : issues) {
            details.append("- ").append(issue.getIssue()).append("\n  Action: ")
                    .append(issue.getRecommendedAction()).append("\n");
        }
        return details.toString();
    }

    private int countIssueMatches(List<PayrollReadinessIssue> issues, String... terms) {
        if (issues == null || terms == null) {
            return 0;
        }
        int count = 0;
        for (PayrollReadinessIssue issue : issues) {
            String text = (safeText(issue.getIssue(), "") + " " + safeText(issue.getRecommendedAction(), ""))
                    .toLowerCase(Locale.ENGLISH);
            for (String term : terms) {
                if (text.contains(term.toLowerCase(Locale.ENGLISH))) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    private int countPendingOvertime(int employeeId) {
        int count = 0;
        for (OvertimeRequest request : overtimeService.getRequestsByEmployee(employeeId)) {
            if (isPending(request.getStatus()) && isInActivePeriod(request.getOvertimeDate())) {
                count++;
            }
        }
        return count;
    }

    private int countPendingCorrections(int employeeId) {
        int count = 0;
        for (AttendanceAdjustment adjustment : attendanceAdjustmentService.getRequestsByEmployee(employeeId)) {
            if (!adjustment.isResolved() && isInActivePeriod(adjustment.getAttendanceDate())) {
                count++;
            }
        }
        return count;
    }

    private int countVisibleLeaves(int employeeId) {
        int count = 0;
        for (Leave leave : leaveService.getLeavesByEmployee(employeeId)) {
            if (overlapsActivePeriod(leave.getStartDate(), leave.getEndDate())) {
                count++;
            }
        }
        return count;
    }

    private boolean isInActivePeriod(LocalDate date) {
        return activePayrollPeriod != null
                && date != null
                && !date.isBefore(activePayrollPeriod.getPeriodStart())
                && !date.isAfter(activePayrollPeriod.getPeriodEnd());
    }

    private boolean overlapsActivePeriod(String start, String end) {
        if (activePayrollPeriod == null) {
            return false;
        }
        try {
            LocalDate leaveStart = LocalDate.parse(start);
            LocalDate leaveEnd = LocalDate.parse(end);
            return !leaveEnd.isBefore(activePayrollPeriod.getPeriodStart())
                    && !leaveStart.isAfter(activePayrollPeriod.getPeriodEnd());
        } catch (Exception ignored) {
            return false;
        }
    }

    private void notifyWorkflowRefresh() {
        if (workflowRefreshHandler != null) {
            workflowRefreshHandler.run();
        }
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

    private boolean isSupervisor() {
        return currentUser != null
                && "supervisor".equals(safeText(currentUser.getRole(), "").toLowerCase(Locale.ENGLISH));
    }

    private String fullName(Employee employee) {
        return safeText(employee.getFirstName(), "") + " " + safeText(employee.getLastName(), "");
    }

    private String getDepartmentLabel(Employee employee) {
        return "Not available";
    }

    private boolean isPending(String status) {
        String normalized = safeText(status, "").toUpperCase(Locale.ENGLISH);
        return normalized.isBlank() || "PENDING".equals(normalized) || "SUBMITTED".equals(normalized);
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
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

    private String recommendedAction(List<PayrollReadinessIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return "Review context, then mark ready for HR validation.";
        }
        return issues.get(0).getRecommendedAction();
    }

    private String recommendedAction(WorkforcePayrollReadiness readiness, List<PayrollReadinessIssue> issues) {
        if (isSentForward(readiness)) {
            return "Sent / Waiting for HR";
        }
        if (readiness != null
                && WorkforcePayrollReadiness.STATUS_OPEN_WORKFORCE_ISSUES.equals(
                safeText(readiness.getReadinessStatus(), "").toUpperCase(Locale.ENGLISH))) {
            return "Open review details and resolve or override blockers.";
        }
        return recommendedAction(issues);
    }

    private interface WorkflowTask {
        String run();
    }

    private class ReadinessQueueRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (isSelected) {
                return component;
            }
            int modelRow = row >= 0 ? table.convertRowIndexToModel(row) : -1;
            WorkforcePayrollReadiness readiness = modelRow >= 0 && modelRow < readinessQueue.size()
                    ? readinessQueue.get(modelRow)
                    : null;
            List<PayrollReadinessIssue> issues = readiness == null
                    ? List.of()
                    : readinessIssuesByEmployee.getOrDefault(readiness.getEmployeeId(), List.of());

            if (isSentForward(readiness)) {
                component.setBackground(new Color(236, 253, 245));
                component.setForeground(UITheme.TEXT_PRIMARY);
            } else if (!issues.isEmpty() || (readiness != null
                    && WorkforcePayrollReadiness.STATUS_OPEN_WORKFORCE_ISSUES.equals(
                    safeText(readiness.getReadinessStatus(), "").toUpperCase(Locale.ENGLISH)))) {
                component.setBackground(new Color(255, 251, 235));
                component.setForeground(UITheme.TEXT_PRIMARY);
            } else {
                component.setBackground(row % 2 == 0 ? Color.WHITE : UITheme.TABLE_ALT_ROW);
                component.setForeground(UITheme.TEXT_PRIMARY);
            }
            return component;
        }
    }
}
