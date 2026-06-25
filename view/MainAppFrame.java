/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.oop.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import com.mycompany.oop.model.Employee;
import com.mycompany.oop.service.OperationalInboxService;
import com.mycompany.oop.service.RoleAccessService;
import com.mycompany.oop.service.WorkflowAwarenessService;

public class MainAppFrame extends JFrame {

    private Employee employee;
    private JPanel contentPanel;
    private CardLayout cardLayout;

    private SidebarPanel sidebarPanel;

    private HRPanel hrPanel;
    private HRPanel employeeDatabasePanel;
    private ITPanel itPanel;
    private PayrollPanel payrollPanel;
    private OperationalInboxPanel operationalInboxPanel;
    private PayslipPanel payslipPanel;
    private LeavePanel leavePanel;
    private LeaveReviewPanel leaveReviewPanel;
    private OvertimePanel overtimePanel;
    private EmployeePanel employeePanel;
    private AttendancePanel attendancePanel;
    private AttendanceAdjustmentPanel attendanceAdjustmentPanel;
    private MyWorkCalendarPanel myWorkCalendarPanel;
    private OrganizationChartPanel organizationChartPanel;
    private TeamOperationsPanel teamOperationsPanel;
    private SchedulingPanel schedulingPanel;
    private HRPayrollHistoryPanel payrollHistoryPanel;
    private DashboardPanel dashboardPanel;
    private WorkflowAwarenessService workflowAwarenessService;
    private OperationalInboxService operationalInboxService;
    private RoleAccessService roleAccessService;
    private SidebarMenuItem leaveReviewMenuItem;
    private SidebarMenuItem inboxMenuItem;
    private Map<String, SidebarMenuItem> menuItemsByCard;
    private Map<String, Component> contentComponentsByCard;
    private Map<String, JComponent> contentViewsByCard;
    private Map<String, Supplier<JComponent>> lazyContentFactoriesByCard;
    private Map<String, Boolean> lazyContentScrollByCard;
    private Set<String> loadingCards;
    private List<String> recentWorkspaceIds;
    private long lastWorkflowIndicatorRefreshMs;
    private long lastInboxIndicatorRefreshMs;
    private int cachedPendingLeaveCount;
    private int cachedInboxCount;
    private String activeCard;

    public MainAppFrame(Employee employee) {
        long frameStartMs = System.currentTimeMillis();

        this.employee = employee;

        setTitle("MotorPH Payroll System");
        setSize(1280, 760);
        setMinimumSize(new Dimension(1100, 680));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.BG);
        getRootPane().setDefaultButton(null);
        workflowAwarenessService = new WorkflowAwarenessService();
        operationalInboxService = new OperationalInboxService();
        roleAccessService = new RoleAccessService();
        menuItemsByCard = new HashMap<>();
        contentComponentsByCard = new HashMap<>();
        contentViewsByCard = new HashMap<>();
        lazyContentFactoriesByCard = new HashMap<>();
        lazyContentScrollByCard = new HashMap<>();
        loadingCards = new HashSet<>();
        recentWorkspaceIds = new ArrayList<>();

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(UITheme.BG);
        contentPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        addLazyContentView("DASH", () -> {
            dashboardPanel = new DashboardPanel(employee, this::navigateFromDashboard, getRecentWorkspaceIds());
            return dashboardPanel;
        }, true);
        addLazyContentView(RoleAccessService.WORKFORCE_GOVERNANCE_VIEW, () -> {
            hrPanel = new HRPanel(employee, this::refreshWorkflowIndicatorsNow, HRPanel.ViewMode.WORKFORCE_GOVERNANCE);
            return hrPanel;
        }, false);
        addLazyContentView(RoleAccessService.EMPLOYEES_VIEW, () -> {
            employeeDatabasePanel = new HRPanel(employee, this::refreshWorkflowIndicatorsNow, HRPanel.ViewMode.EMPLOYEE_DATABASE);
            return employeeDatabasePanel;
        }, false);
        if (canAccessView(RoleAccessService.PAYROLL_VIEW)) {
            addLazyContentView("PAYROLL", () -> {
                payrollPanel = new PayrollPanel(employee, this::navigateFromDashboard,
                        this::refreshWorkflowIndicatorsNow, this::refreshPayrollArtifactsNow);
                return payrollPanel;
            }, false);
        }
        addLazyContentView("INBOX", () -> {
            operationalInboxPanel = new OperationalInboxPanel(employee, this::navigateFromDashboard,
                    this::refreshWorkflowIndicatorsNow);
            return operationalInboxPanel;
        }, true);
        addLazyContentView("PAYSLIP", () -> {
            payslipPanel = new PayslipPanel(employee);
            return payslipPanel;
        }, true);
        addLazyContentView("FILE", () -> {
            leavePanel = new LeavePanel(employee);
            return leavePanel;
        }, false);
        addLazyContentView("LEAVE", () -> {
            leaveReviewPanel = new LeaveReviewPanel(employee);
            return leaveReviewPanel;
        }, false);
        addLazyContentView("OVERTIME", () -> {
            overtimePanel = new OvertimePanel(employee);
            return overtimePanel;
        }, true);
        addLazyContentView("PROFILE", () -> {
            employeePanel = new EmployeePanel(employee);
            return employeePanel;
        }, true);
        addLazyContentView("ATTENDANCE", () -> {
            attendancePanel = new AttendancePanel(employee);
            return attendancePanel;
        }, false);
        addLazyContentView("ATTENDANCE_ADJUSTMENT", () -> {
            attendanceAdjustmentPanel = new AttendanceAdjustmentPanel(employee);
            return attendanceAdjustmentPanel;
        }, true);
        addLazyContentView("TIMEKEEPING", () -> {
            myWorkCalendarPanel = new MyWorkCalendarPanel(employee);
            return myWorkCalendarPanel;
        }, true);
        addLazyContentView("ORG_CHART", () -> {
            organizationChartPanel = new OrganizationChartPanel(employee);
            return organizationChartPanel;
        }, true);
        addLazyContentView("TEAM_OPERATIONS", () -> {
            teamOperationsPanel = new TeamOperationsPanel(employee, this::refreshWorkflowIndicatorsNow);
            return teamOperationsPanel;
        }, true);
        addLazyContentView("SCHEDULING", () -> {
            schedulingPanel = new SchedulingPanel(employee);
            return schedulingPanel;
        }, true);
        addLazyContentView("IT", () -> {
            itPanel = new ITPanel(employee);
            return itPanel;
        }, false);
        if (canAccessView(RoleAccessService.PAYROLL_HISTORY_VIEW)) {
            addLazyContentView("PAYROLL_HISTORY", () -> {
                payrollHistoryPanel = new HRPayrollHistoryPanel(employee);
                return payrollHistoryPanel;
            }, false);
        }

        sidebarPanel = new SidebarPanel(employee, this::navigateTo, this::logout);

        add(sidebarPanel, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        addAllowedNavButtons();
        sidebarPanel.setCollapsed(false);

        activeCard = "DASH";
        cardLayout.show(contentPanel, activeCard);
        refreshWorkflowIndicatorsAsync(true);
        SwingUtilities.invokeLater(() -> loadContentViewIfNeeded("DASH", 250));
        System.out.println("[perf] MainAppFrame constructor took "
                + (System.currentTimeMillis() - frameStartMs) + " ms");
    }

    private SidebarMenuItem addNavButton(String text, String card) {
        SidebarMenuItem item = sidebarPanel.addMenuItem(text, card, getIconType(text, card));
        menuItemsByCard.put(card, item);
        return item;
    }

    private void addContentView(String card, JComponent view, boolean scrollable) {
        Component component = scrollable ? createScrollableView(view) : view;
        contentViewsByCard.put(card, view);
        contentComponentsByCard.put(card, component);
        contentPanel.add(component, card);
    }

    private void replaceContentView(String card, JComponent view, boolean scrollable) {
        Component current = contentComponentsByCard.get(card);
        if (current != null) {
            contentPanel.remove(current);
        }
        addContentView(card, view, scrollable);
    }

    private void addLazyContentView(String card, Supplier<JComponent> factory, boolean scrollable) {
        lazyContentFactoriesByCard.put(card, factory);
        lazyContentScrollByCard.put(card, scrollable);
        addContentView(card, createLoadingWorkspace(card), false);
    }

    private JPanel createLoadingWorkspace(String card) {
        JPanel panel = UITheme.createWorkspacePanel(new BorderLayout(0, 12));
        panel.add(UITheme.createSkeletonCard("Loading " + getViewLabel(card), 5), BorderLayout.NORTH);
        panel.add(UITheme.createSkeletonCard(getViewLabel(card), 6), BorderLayout.CENTER);
        return panel;
    }

    private void loadContentViewIfNeeded(String card) {
        loadContentViewIfNeeded(card, 40);
    }

    private void loadContentViewIfNeeded(String card, int delayMs) {
        Supplier<JComponent> factory = lazyContentFactoriesByCard.get(card);
        if (factory == null || loadingCards.contains(card)) {
            return;
        }
        loadingCards.add(card);
        Timer timer = new Timer(Math.max(0, delayMs), event -> {
            ((Timer) event.getSource()).stop();
            long startMs = System.currentTimeMillis();
            try {
                JComponent view = factory.get();
                lazyContentFactoriesByCard.remove(card);
                loadingCards.remove(card);
                replaceContentView(card, view, lazyContentScrollByCard.getOrDefault(card, false));
                if (card.equals(activeCard)) {
                    cardLayout.show(contentPanel, card);
                }
                System.out.println("[perf] Loaded workspace " + card + " in "
                        + (System.currentTimeMillis() - startMs) + " ms");
            } catch (RuntimeException ex) {
                loadingCards.remove(card);
                replaceContentView(card, createWorkspaceLoadError(card), false);
                if (card.equals(activeCard)) {
                    cardLayout.show(contentPanel, card);
                }
                throw ex;
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private JPanel createWorkspaceLoadError(String card) {
        JPanel panel = UITheme.createWorkspacePanel(new BorderLayout());
        JLabel label = new JLabel("Unable to load " + getViewLabel(card) + ".", SwingConstants.CENTER);
        label.setFont(UITheme.FONT_BODY_BOLD);
        label.setForeground(UITheme.DANGER);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private JScrollPane createScrollableView(JComponent view) {
        JScrollPane scrollPane = new JScrollPane(view);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(UITheme.BG);
        scrollPane.getViewport().setBackground(UITheme.BG);
        WorkforceFormToolkit.tuneScrollPane(scrollPane);
        return scrollPane;
    }

    private void navigateTo(String card, SidebarMenuItem item) {
        if (!canAccessView(card)) {
            showAccessDeniedMessage();
            return;
        }

        contentPanel.revalidate();
        contentPanel.repaint();
        recordRecentWorkspace(card);
        refreshDashboardQuickAccessIfNeeded(card);
        activeCard = card;
        cardLayout.show(contentPanel, card);
        refreshWorkspaceAfterShow(card);
        sidebarPanel.setActiveItem(item);
        refreshWorkflowIndicatorsAsync(true);
    }

    private void navigateFromDashboard(String card) {
        if (!canAccessView(card)) {
            showAccessDeniedMessage();
            return;
        }

        SidebarMenuItem item = menuItemsByCard.get(card);

        if (item != null) {
            navigateTo(card, item);
            return;
        }

        contentPanel.revalidate();
        contentPanel.repaint();
        recordRecentWorkspace(card);
        refreshDashboardQuickAccessIfNeeded(card);
        activeCard = card;
        cardLayout.show(contentPanel, card);
        refreshWorkspaceAfterShow(card);
        refreshWorkflowIndicatorsAsync(true);
    }

    private void addAllowedNavButtons() {
        List<String> allowed = roleAccessService.getAllowedViewIds(employee);

        addNavGroup("PERSONAL", allowed, List.of(
                RoleAccessService.DASHBOARD_VIEW,
                RoleAccessService.PROFILE_VIEW,
                RoleAccessService.OPERATIONAL_INBOX_VIEW,
                RoleAccessService.ATTENDANCE_VIEW,
                RoleAccessService.TIMEKEEPING_CALENDAR_VIEW,
                RoleAccessService.PAYSLIP_VIEW,
                RoleAccessService.FILE_LEAVE_VIEW,
                RoleAccessService.OVERTIME_VIEW
        ));

        addNavGroup("OPERATIONS", allowed, List.of(
                RoleAccessService.TEAM_OPERATIONS_VIEW,
                RoleAccessService.WORKFORCE_GOVERNANCE_VIEW,
                RoleAccessService.EMPLOYEES_VIEW,
                RoleAccessService.LEAVE_REVIEW_VIEW,
                RoleAccessService.SCHEDULING_VIEW,
                RoleAccessService.ATTENDANCE_ADJUSTMENT_VIEW,
                RoleAccessService.PAYROLL_VIEW,
                RoleAccessService.PAYROLL_HISTORY_VIEW
        ));

        addNavGroup("SYSTEM", allowed, List.of(
                RoleAccessService.ORG_CHART_VIEW,
                RoleAccessService.USER_MANAGEMENT_VIEW
        ));
    }

    private void addNavGroup(String label, List<String> allowed, List<String> orderedViews) {
        boolean hasVisibleItem = false;
        for (String viewId : orderedViews) {
            if (allowed.contains(viewId)) {
                hasVisibleItem = true;
                break;
            }
        }

        if (!hasVisibleItem) {
            return;
        }

        sidebarPanel.addSectionLabel(label);
        for (String viewId : orderedViews) {
            if (!allowed.contains(viewId)) {
                continue;
            }
            SidebarMenuItem item = addNavButton(getViewLabel(viewId), viewId);

            if (RoleAccessService.LEAVE_REVIEW_VIEW.equals(viewId)) {
                leaveReviewMenuItem = item;
            }
            if (RoleAccessService.OPERATIONAL_INBOX_VIEW.equals(viewId)) {
                inboxMenuItem = item;
            }
        }
    }

    private boolean canAccessView(String card) {
        return roleAccessService.canAccessView(employee, card);
    }

    private void showAccessDeniedMessage() {
        JOptionPane.showMessageDialog(this,
                "This workspace is not available for your current role.",
                "Access Restricted",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateWorkflowIndicators() {
        if (leaveReviewMenuItem != null) {
            long now = System.currentTimeMillis();
            if (now - lastWorkflowIndicatorRefreshMs > 15_000L) {
                cachedPendingLeaveCount = workflowAwarenessService.getPendingLeaveCount();
                lastWorkflowIndicatorRefreshMs = now;
            }
            leaveReviewMenuItem.setBadgeCount(cachedPendingLeaveCount);
        }
        if (inboxMenuItem != null) {
            long now = System.currentTimeMillis();
            if (now - lastInboxIndicatorRefreshMs > 15_000L) {
                cachedInboxCount = operationalInboxService.getActiveCount(employee);
                lastInboxIndicatorRefreshMs = now;
            }
            inboxMenuItem.setBadgeCount(cachedInboxCount);
        }
    }

    public void refreshWorkflowIndicatorsNow() {
        OperationalInboxService.invalidateCache(employee);
        lastWorkflowIndicatorRefreshMs = 0L;
        lastInboxIndicatorRefreshMs = 0L;
        refreshWorkflowIndicatorsAsync(true);
    }

    public void refreshPayrollArtifactsNow() {
        if (payrollHistoryPanel != null) {
            payrollHistoryPanel.refreshData();
        }
        if (payslipPanel != null) {
            payslipPanel.refreshData();
        }
        if (dashboardPanel != null) {
            dashboardPanel.refreshData();
        }
        refreshWorkflowIndicatorsNow();
    }

    private void refreshWorkflowIndicatorsAsync() {
        refreshWorkflowIndicatorsAsync(false);
    }

    private void refreshWorkflowIndicatorsAsync(boolean force) {
        long now = System.currentTimeMillis();
        boolean refreshLeave = leaveReviewMenuItem != null
                && (force || now - lastWorkflowIndicatorRefreshMs > 15_000L);
        boolean refreshInbox = inboxMenuItem != null
                && (force || now - lastInboxIndicatorRefreshMs > 15_000L);

        if (!refreshLeave && !refreshInbox) {
            updateWorkflowIndicatorBadges();
            return;
        }

        new SwingWorker<int[], Void>() {
            private long startedAtMs;

            @Override
            protected int[] doInBackground() {
                startedAtMs = System.currentTimeMillis();
                int pendingLeaves = cachedPendingLeaveCount;
                int inboxCount = cachedInboxCount;
                if (refreshLeave) {
                    pendingLeaves = workflowAwarenessService.getPendingLeaveCount();
                }
                if (refreshInbox) {
                    inboxCount = operationalInboxService.getActiveCount(employee);
                }
                return new int[]{pendingLeaves, inboxCount};
            }

            @Override
            protected void done() {
                try {
                    int[] values = get();
                    if (refreshLeave) {
                        cachedPendingLeaveCount = values[0];
                        lastWorkflowIndicatorRefreshMs = System.currentTimeMillis();
                    }
                    if (refreshInbox) {
                        cachedInboxCount = values[1];
                        lastInboxIndicatorRefreshMs = System.currentTimeMillis();
                    }
                    System.out.println("[perf] Workflow indicator refresh took "
                            + (System.currentTimeMillis() - startedAtMs) + " ms");
                    updateWorkflowIndicatorBadges();
                } catch (Exception ignored) {
                    updateWorkflowIndicatorBadges();
                }
            }
        }.execute();
    }

    private void updateWorkflowIndicatorBadges() {
        if (leaveReviewMenuItem != null) {
            leaveReviewMenuItem.setBadgeCount(cachedPendingLeaveCount);
        }
        if (inboxMenuItem != null) {
            inboxMenuItem.setBadgeCount(cachedInboxCount);
        }
    }

    public List<String> getRecentWorkspaceIds() {
        return new ArrayList<>(recentWorkspaceIds);
    }

    private void recordRecentWorkspace(String card) {
        if (card == null || RoleAccessService.DASHBOARD_VIEW.equals(card) || !canAccessView(card)) {
            return;
        }

        recentWorkspaceIds.remove(card);
        recentWorkspaceIds.add(0, card);

        while (recentWorkspaceIds.size() > 5) {
            recentWorkspaceIds.remove(recentWorkspaceIds.size() - 1);
        }
    }

    private void refreshDashboardQuickAccessIfNeeded(String card) {
        if (dashboardPanel == null) {
            return;
        }

        dashboardPanel.refreshQuickAccess(getRecentWorkspaceIds());
    }

    private void refreshWorkspaceAfterShow(String card) {
        loadContentViewIfNeeded(card);
        JComponent view = contentViewsByCard.get(card);
        if (view instanceof RefreshablePanel refreshablePanel) {
            SwingUtilities.invokeLater(refreshablePanel::refreshData);
        }
        if (RoleAccessService.OPERATIONAL_INBOX_VIEW.equals(card)) {
            refreshWorkflowIndicatorsNow();
        }
    }

    private void logout() {
        dispose();
        LoginFrame loginFrame = new LoginFrame();
        loginFrame.setVisible(true);
    }

    private SidebarIcon.Type getIconType(String text, String card) {
        if ("DASH".equals(card)) return SidebarIcon.Type.DASHBOARD;
        if (RoleAccessService.WORKFORCE_GOVERNANCE_VIEW.equals(card)) return SidebarIcon.Type.USERS;
        if ("EMP".equals(card)) return SidebarIcon.Type.USERS;
        if ("PAYROLL".equals(card)) return SidebarIcon.Type.WALLET;
        if ("PAYROLL_HISTORY".equals(card)) return SidebarIcon.Type.HISTORY;
        if ("INBOX".equals(card)) return SidebarIcon.Type.HISTORY;
        if ("LEAVE".equals(card) || "FILE".equals(card) || "ATTENDANCE".equals(card)
                || "TIMEKEEPING".equals(card) || "SCHEDULING".equals(card)
                || "OVERTIME".equals(card) || "ATTENDANCE_ADJUSTMENT".equals(card)) return SidebarIcon.Type.CALENDAR;
        if ("ORG_CHART".equals(card)) return SidebarIcon.Type.USERS;
        if ("TEAM_OPERATIONS".equals(card)) return SidebarIcon.Type.USERS;
        if ("IT".equals(card)) return SidebarIcon.Type.SETTINGS;
        if ("PAYSLIP".equals(card)) return SidebarIcon.Type.PAYSLIP;
        if ("PROFILE".equals(card)) return SidebarIcon.Type.PROFILE;
        return SidebarIcon.Type.DASHBOARD;
    }

    private String getViewLabel(String card) {
        if ("DASH".equals(card)) return "Dashboard";
        if (RoleAccessService.WORKFORCE_GOVERNANCE_VIEW.equals(card)) return "Workforce Governance";
        if ("EMP".equals(card)) return "Employee Database";
        if ("PAYROLL".equals(card)) return "Payroll Operations";
        if ("PAYROLL_HISTORY".equals(card)) return "Payroll History";
        if ("INBOX".equals(card)) return "Operational Inbox";
        if ("LEAVE".equals(card)) return "Leave Review";
        if ("OVERTIME".equals(card)) return "Overtime";
        if ("SCHEDULING".equals(card)) return "Workforce Schedule";
        if ("IT".equals(card)) return "User Access";
        if ("PROFILE".equals(card)) return "My Profile";
        if ("ATTENDANCE".equals(card)) return "Attendance";
        if ("ATTENDANCE_ADJUSTMENT".equals(card)) return "Attendance Adjustments";
        if ("TIMEKEEPING".equals(card)) return "Work Calendar";
        if ("ORG_CHART".equals(card)) return "Org Chart";
        if ("TEAM_OPERATIONS".equals(card)) return "Team Operations";
        if ("PAYSLIP".equals(card)) return "Payslip";
        if ("FILE".equals(card)) return "Leave";
        return card;
    }

}
