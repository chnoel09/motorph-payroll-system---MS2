/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.oop.view;

import com.mycompany.oop.model.DashboardSummary;
import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.TodayAttendanceSummary;
import com.mycompany.oop.service.DashboardService;
import com.mycompany.oop.service.EmployeeService;
import com.mycompany.oop.service.RoleAccessService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class DashboardPanel extends JPanel implements RefreshablePanel, Scrollable {

    private final Employee user;
    private final DashboardService dashboardService;
    private final RoleAccessService roleAccessService;
    private final EmployeeService employeeService;
    private final Consumer<String> navigationHandler;
    private List<String> recentWorkspaceIds;
    private JPanel metricGridPanel;
    private JPanel quickAccessBody;
    private DashboardSummary summary;
    private TodayAttendanceSummary todayAttendanceSummary;
    private int metricColumnCount = 4;

    private static final Color[] CARD_ACCENTS = {
            UITheme.ACCENT,
            UITheme.BLUE,
            UITheme.YELLOW,
            UITheme.BLACK,
            UITheme.BLUE_LIGHT,
            UITheme.ACCENT_HOVER
    };

    public DashboardPanel(Employee user) {
        this(user, null);
    }

    public DashboardPanel(Employee user, Consumer<String> navigationHandler) {
        this(user, navigationHandler, List.of());
    }

    public DashboardPanel(Employee user, Consumer<String> navigationHandler, List<String> recentWorkspaceIds) {
        this.user = user;
        this.navigationHandler = navigationHandler;
        this.dashboardService = new DashboardService();
        this.roleAccessService = new RoleAccessService();
        this.employeeService = new EmployeeService();
        this.recentWorkspaceIds = sanitizeRecentWorkspaceIds(recentWorkspaceIds);
        this.summary = dashboardService.generateSummary();
        this.todayAttendanceSummary = dashboardService.getTodayAttendanceSummary(user);

        setLayout(new BorderLayout());
        setBackground(UITheme.BG);

        JPanel content = UITheme.createWorkspacePanel(new BorderLayout(0, 0));
        JPanel dashboard = new JPanel(new GridBagLayout());
        dashboard.setOpaque(false);

        GridBagConstraints rowConstraints = new GridBagConstraints();
        rowConstraints.gridx = 0;
        rowConstraints.weightx = 1;
        rowConstraints.fill = GridBagConstraints.HORIZONTAL;
        rowConstraints.anchor = GridBagConstraints.NORTHWEST;
        rowConstraints.insets = new Insets(0, 0, 0, 14);

        rowConstraints.gridy = 0;
        dashboard.add(createHeroCard(), rowConstraints);

        rowConstraints.gridy = 1;
        rowConstraints.insets = new Insets(10, 0, 0, 14);
        metricGridPanel = createMetricGrid();
        metricGridPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int columns = getMetricColumnCount();
                if (columns != metricColumnCount) {
                    metricColumnCount = columns;
                    refreshMetricGrid();
                }
            }
        });
        dashboard.add(metricGridPanel, rowConstraints);

        rowConstraints.gridy = 2;
        dashboard.add(createQuickAccess(), rowConstraints);

        content.add(dashboard, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
    }

    private JPanel createHeroCard() {
        JPanel hero = createCardPanel(new BorderLayout(18, 0), 14, 18, 14, 18);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel chip = new JLabel(getRoleWorkspaceChip());
        chip.setFont(UITheme.FONT_BODY_BOLD);
        chip.setForeground(UITheme.ACCENT);

        JLabel title = new JLabel("Welcome back, " + safeText(user.getFirstName(), "Employee"));
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel(getRoleSubtitle());
        subtitle.setFont(UITheme.FONT_SMALL);
        subtitle.setForeground(UITheme.TEXT_SECONDARY);

        left.add(chip);
        left.add(Box.createVerticalStrut(4));
        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(subtitle);

        JPanel roleBadge = new JPanel(new BorderLayout(10, 0));
        roleBadge.setBackground(new Color(248, 250, 252));
        roleBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                new EmptyBorder(7, 11, 7, 13)
        ));

        JLabel roleIcon = createIconLabel(getRoleIcon(), UITheme.ACCENT, 18);
        JPanel roleText = new JPanel();
        roleText.setOpaque(false);
        roleText.setLayout(new BoxLayout(roleText, BoxLayout.Y_AXIS));

        JLabel roleTitle = new JLabel("Signed in as");
        roleTitle.setFont(UITheme.FONT_SMALL);
        roleTitle.setForeground(UITheme.TEXT_SECONDARY);

        JLabel roleValue = new JLabel(safeText(user.getRole(), "Employee"));
        roleValue.setFont(UITheme.FONT_BODY_BOLD);
        roleValue.setForeground(UITheme.TEXT_PRIMARY);

        roleText.add(roleTitle);
        roleText.add(Box.createVerticalStrut(2));
        roleText.add(roleValue);

        roleBadge.add(roleIcon, BorderLayout.WEST);
        roleBadge.add(roleText, BorderLayout.CENTER);

        hero.add(left, BorderLayout.CENTER);
        hero.add(roleBadge, BorderLayout.EAST);
        return hero;
    }

    private JPanel createMetricGrid() {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        populateMetricGrid(grid);
        return grid;
    }

    private void populateMetricGrid(JPanel grid) {
        MetricSpec[] metrics = getRoleMetrics();
        int columns = getMetricColumnCount();

        for (int i = 0; i < metrics.length; i++) {
            MetricSpec metric = metrics[i];
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = i % columns;
            constraints.gridy = i / columns;
            constraints.weightx = 1;
            constraints.weighty = 1;
            constraints.fill = GridBagConstraints.BOTH;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.insets = new Insets(
                    i < columns ? 0 : 12,
                    constraints.gridx == 0 ? 0 : 12,
                    0,
                    0
            );
            grid.add(createMetricTile(metric, CARD_ACCENTS[i % CARD_ACCENTS.length]), constraints);
        }
    }

    private JPanel createMetricTile(MetricSpec metric, Color accent) {
        JPanel card = createCardPanel(new BorderLayout(0, 10), 13, 14, 13, 14);
        card.setMinimumSize(new Dimension(0, 146));
        card.setPreferredSize(new Dimension(0, 146));
        if (!metric.live) {
            card.setBackground(new Color(248, 250, 252));
        }

        Color displayAccent = metric.live ? accent : new Color(148, 163, 184);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel icon = createIconLabel(metric.iconType, displayAccent, 17);
        icon.setOpaque(true);
        icon.setBackground(metric.live ? tint(accent) : new Color(241, 245, 249));
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        icon.setPreferredSize(new Dimension(34, 34));
        icon.setBorder(BorderFactory.createLineBorder(new Color(230, 235, 244)));

        top.add(icon, BorderLayout.WEST);
        if (metric.actionCard != null && navigationHandler != null) {
            JLabel action = new JLabel("Open");
            action.setFont(UITheme.FONT_SMALL.deriveFont(Font.BOLD));
            action.setForeground(UITheme.BLUE);
            top.add(action, BorderLayout.EAST);
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            card.setToolTipText("Open " + getViewLabel(metric.actionCard));
            card.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    navigationHandler.accept(metric.actionCard);
                }
            });
        }

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(metric.label);
        title.setFont(UITheme.FONT_CARD_LABEL);
        title.setForeground(UITheme.TEXT_SECONDARY);
        title.setAlignmentX(LEFT_ALIGNMENT);

        JLabel value = new JLabel(metric.value);
        value.setFont(new Font("Segoe UI", Font.BOLD, 19));
        value.setForeground(metric.live ? UITheme.TEXT_PRIMARY : new Color(100, 116, 139));
        value.setAlignmentX(LEFT_ALIGNMENT);

        text.add(title);
        text.add(Box.createVerticalStrut(5));
        text.add(value);
        if (metric.note != null && !metric.note.isBlank()) {
            JLabel noteLabel = createWrappingLabel(metric.note, UITheme.FONT_SMALL, new Color(100, 116, 139), 210);
            noteLabel.setAlignmentX(LEFT_ALIGNMENT);
            text.add(Box.createVerticalStrut(4));
            text.add(noteLabel);
        }

        card.add(top, BorderLayout.NORTH);
        card.add(text, BorderLayout.CENTER);

        if (metric.detailLeft != null && metric.detailRight != null) {
            card.add(createMetricDetailRow(metric.detailLeft, metric.detailRight), BorderLayout.SOUTH);
        }

        return card;
    }

    private JPanel createMetricDetailRow(String left, String right) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);

        JLabel leftLabel = new JLabel(left);
        leftLabel.setFont(UITheme.FONT_SMALL);
        leftLabel.setForeground(UITheme.TEXT_SECONDARY);

        JLabel rightLabel = new JLabel(right);
        rightLabel.setFont(UITheme.FONT_SMALL);
        rightLabel.setForeground(UITheme.TEXT_SECONDARY);
        rightLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(leftLabel, BorderLayout.WEST);
        row.add(rightLabel, BorderLayout.EAST);
        return row;
    }

    public void refreshQuickAccess(List<String> recentWorkspaceIds) {
        this.recentWorkspaceIds = sanitizeRecentWorkspaceIds(recentWorkspaceIds);
        if (quickAccessBody == null) {
            return;
        }

        quickAccessBody.removeAll();
        quickAccessBody.add(createQuickAccessContent(), BorderLayout.CENTER);
        quickAccessBody.revalidate();
        quickAccessBody.repaint();
    }

    @Override
    public void refreshData() {
        summary = dashboardService.generateSummary();
        todayAttendanceSummary = dashboardService.getTodayAttendanceSummary(user);
        refreshMetricGrid();
        refreshQuickAccess(recentWorkspaceIds);
    }

    private JPanel createQuickAccess() {
        JPanel wrapper = createSectionPanel("Quick Access", "");

        quickAccessBody = new JPanel(new BorderLayout(0, 0));
        quickAccessBody.setOpaque(false);
        quickAccessBody.add(createQuickAccessContent(), BorderLayout.CENTER);

        wrapper.add(quickAccessBody, BorderLayout.CENTER);
        return wrapper;
    }

    private void refreshMetricGrid() {
        if (metricGridPanel == null) {
            return;
        }

        metricColumnCount = getMetricColumnCount();
        metricGridPanel.removeAll();
        populateMetricGrid(metricGridPanel);
        metricGridPanel.revalidate();
        metricGridPanel.repaint();
    }

    private JPanel createQuickActionGrid(QuickActionSpec[] actions) {
        JPanel grid = new WrappingPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        grid.setOpaque(false);
        grid.setAlignmentX(LEFT_ALIGNMENT);

        for (QuickActionSpec action : actions) {
            grid.add(createQuickActionCard(action.label, action.card, action.iconType));
        }

        return grid;
    }

    private JPanel createQuickAccessContent() {
        QuickActionSpec[] actions = getQuickAccessItems();
        if (actions.length == 0) {
            JPanel empty = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
            empty.setOpaque(false);
            JLabel label = new JLabel("No recent workspaces yet.");
            label.setFont(UITheme.FONT_SMALL);
            label.setForeground(UITheme.TEXT_SECONDARY);
            empty.add(label);
            return empty;
        }

        return createQuickActionGrid(actions);
    }

    private JPanel createQuickActionCard(String label, String cardName, SidebarIcon.Type iconType) {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setPreferredSize(new Dimension(174, 52));
        card.setBackground(new Color(248, 250, 252));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                new EmptyBorder(10, 12, 10, 12)
        ));
        card.setCursor(navigationHandler == null ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setToolTipText("Open " + label);

        JLabel icon = createIconLabel(iconType, UITheme.BLUE, 18);
        icon.setOpaque(true);
        icon.setBackground(new Color(239, 246, 255));
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        icon.setPreferredSize(new Dimension(32, 32));
        icon.setBorder(BorderFactory.createLineBorder(new Color(219, 234, 254), 1));

        JLabel title = new JLabel("<html><div style='width:108px'>" + escapeHtml(label) + "</div></html>");
        title.setFont(UITheme.FONT_BODY_BOLD);
        title.setForeground(new Color(71, 85, 105));

        JPanel iconWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        iconWrap.setOpaque(false);
        iconWrap.add(icon);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        title.setAlignmentX(LEFT_ALIGNMENT);
        text.add(Box.createVerticalGlue());
        text.add(title);
        text.add(Box.createVerticalGlue());

        card.add(iconWrap, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);

        if (navigationHandler != null) {
            card.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    navigationHandler.accept(cardName);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    card.setBackground(Color.WHITE);
                    card.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(UITheme.BLUE_LIGHT, 1),
                            new EmptyBorder(10, 12, 10, 12)
                    ));
                    title.setForeground(UITheme.TEXT_PRIMARY);
                    title.setFont(UITheme.FONT_BODY_BOLD);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    card.setBackground(new Color(248, 250, 252));
                    card.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(UITheme.BORDER, 1),
                            new EmptyBorder(10, 12, 10, 12)
                    ));
                    title.setForeground(new Color(71, 85, 105));
                    title.setFont(UITheme.FONT_BODY_BOLD);
                }
            });
        }

        return card;
    }

    private JPanel createSectionPanel(String title, String subtitle) {
        JPanel wrapper = createCardPanel(new BorderLayout(0, 14), 18, 20, 20, 20);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UITheme.FONT_SECTION);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);

        header.add(titleLabel, BorderLayout.WEST);
        if (subtitle != null && !subtitle.isBlank()) {
            JLabel subtitleLabel = new JLabel(subtitle);
            subtitleLabel.setFont(UITheme.FONT_SMALL);
            subtitleLabel.setForeground(UITheme.TEXT_SECONDARY);
            header.add(subtitleLabel, BorderLayout.EAST);
        }
        wrapper.add(header, BorderLayout.NORTH);

        return wrapper;
    }

    private JLabel createWrappingLabel(String text, Font font, Color color, int width) {
        JLabel label = new JLabel("<html><body style='width:" + width + "px'>" + escapeHtml(text) + "</body></html>");
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private MetricSpec[] getRoleMetrics() {
        boolean payrollTotalsLive = hasLivePayrollTotals();
        MetricSpec todayAttendance = createTodayAttendanceMetric();
        return switch (getRoleKey()) {
            case "admin" -> new MetricSpec[]{
                    todayAttendance,
                    new MetricSpec("User Access", "Active", "Role management", SidebarIcon.Type.SETTINGS, true),
                    new MetricSpec("Active Accounts", String.valueOf(summary.getActiveEmployees()), SidebarIcon.Type.PROFILE),
                    new MetricSpec("Total Accounts", String.valueOf(summary.getTotalEmployees()), SidebarIcon.Type.USERS)
            };
            case "hr" -> new MetricSpec[]{
                    todayAttendance,
                    new MetricSpec("Active Employees", String.valueOf(summary.getActiveEmployees()), "Current workforce", SidebarIcon.Type.USERS, true),
                    new MetricSpec("Pending Leaves", String.valueOf(summary.getPendingLeaves()), "Awaiting review", SidebarIcon.Type.CALENDAR, true),
                    new MetricSpec("Workforce Governance", "Active", "HR operations", SidebarIcon.Type.USERS, true)
            };
            case "finance" -> new MetricSpec[]{
                    todayAttendance,
                    new MetricSpec("Payroll Operations", "Active", "Readiness and processing", SidebarIcon.Type.WALLET, true),
                    new MetricSpec("Gross Payroll", formatCurrency(summary.getTotalGross()), SidebarIcon.Type.WALLET),
                    new MetricSpec("Net Payroll", payrollTotalsLive ? formatCurrency(summary.getTotalNet()) : "Pending", "Pending computation", SidebarIcon.Type.PAYSLIP, payrollTotalsLive)
            };
            case "it" -> new MetricSpec[]{
                    todayAttendance,
                    new MetricSpec("Access Management", "Active", "User security", SidebarIcon.Type.SETTINGS, true),
                    new MetricSpec("Total Users", String.valueOf(summary.getTotalEmployees()), SidebarIcon.Type.USERS),
                    new MetricSpec("Admin Accounts", String.valueOf(summary.getAdminCount()), SidebarIcon.Type.SETTINGS)
            };
            case "supervisor" -> new MetricSpec[]{
                    todayAttendance,
                    new MetricSpec("Team Operations", hasTeamAuthority() ? "Ready" : "Not assigned",
                            hasTeamAuthority() ? "Review only" : "No direct reports", SidebarIcon.Type.USERS, hasTeamAuthority()),
                    new MetricSpec("My Work Calendar", "Available", SidebarIcon.Type.CALENDAR),
                    new MetricSpec("Attendance Review", hasTeamAuthority() ? "Available" : "Personal only",
                            hasTeamAuthority() ? "Team follow-up" : "Use Attendance", SidebarIcon.Type.PROFILE, hasTeamAuthority())
            };
            default -> new MetricSpec[]{
                    todayAttendance,
                    new MetricSpec("My Work Calendar", "Primary", "Schedule and timekeeping", SidebarIcon.Type.CALENDAR, true),
                    new MetricSpec("My Gross Salary", formatCurrency(user.computeGrossSalary()), SidebarIcon.Type.WALLET),
                    new MetricSpec("Employment Status", safeText(user.getEmploymentStatus(), "Not set"), SidebarIcon.Type.PROFILE)
            };
        };
    }

    private MetricSpec createTodayAttendanceMetric() {
        String status = getAttendanceDashboardStatus(todayAttendanceSummary == null
                ? "Unavailable"
                : safeText(todayAttendanceSummary.getStatus(), "Not Clocked In"));
        String message = todayAttendanceSummary == null
                ? "No attendance summary available."
                : getAttendanceDashboardMessage(todayAttendanceSummary.getStatus(), todayAttendanceSummary.getMessage());
        String timeIn = "In " + displayTime(todayAttendanceSummary == null ? "" : todayAttendanceSummary.getTimeIn());
        String timeOut = "Out " + displayTime(todayAttendanceSummary == null ? "" : todayAttendanceSummary.getTimeOut())
                + " | " + formatHours(todayAttendanceSummary == null ? 0.0 : todayAttendanceSummary.getHoursWorked());

        return new MetricSpec("Today's Attendance", status, message, SidebarIcon.Type.CALENDAR, true,
                RoleAccessService.ATTENDANCE_VIEW, timeIn, timeOut);
    }

    private String getAttendanceDashboardStatus(String status) {
        String normalized = status == null ? "" : status.trim();
        return switch (normalized) {
            case "No Assigned Schedule" -> "No Schedule";
            case "Missing Time In" -> "Not Clocked In";
            case "Missing Time Out" -> "Timed In";
            case "Rest Day Attendance" -> "Rest Day";
            default -> safeText(normalized, "Unavailable");
        };
    }

    private String getAttendanceDashboardMessage(String status, String message) {
        String normalized = status == null ? "" : status.trim();
        return switch (normalized) {
            case "No Assigned Schedule" -> "No schedule assigned.";
            case "Missing Time In" -> "Time-in not recorded.";
            case "Missing Time Out" -> "Time-out pending.";
            default -> safeText(message, "Open Attendance for details.");
        };
    }

    private int getMetricColumnCount() {
        int width = metricGridPanel == null ? getWidth() : metricGridPanel.getWidth();
        if (width > 0 && width < 620) {
            return 2;
        }
        return 4;
    }

    private QuickActionSpec[] getQuickAccessItems() {
        List<QuickActionSpec> actions = new ArrayList<>();
        for (String viewId : recentWorkspaceIds) {
            if (!roleAccessService.canAccessView(user, viewId)
                    || RoleAccessService.DASHBOARD_VIEW.equals(viewId)) {
                continue;
            }
            actions.add(new QuickActionSpec(getViewLabel(viewId), viewId, getIconType(viewId)));
            if (actions.size() == 5) {
                break;
            }
        }
        return actions.toArray(new QuickActionSpec[0]);
    }

    private List<String> sanitizeRecentWorkspaceIds(List<String> workspaceIds) {
        List<String> sanitized = new ArrayList<>();
        if (workspaceIds == null) {
            return sanitized;
        }

        for (String viewId : workspaceIds) {
            if (sanitized.size() == 5) {
                break;
            }
            if (viewId == null
                    || RoleAccessService.DASHBOARD_VIEW.equals(viewId)
                    || sanitized.contains(viewId)
                    || !roleAccessService.canAccessView(user, viewId)) {
                continue;
            }
            sanitized.add(viewId);
        }
        return sanitized;
    }

    private String getRoleSubtitle() {
        return switch (getRoleKey()) {
            case "admin" -> "Operational workspace for access, workforce visibility, and system administration.";
            case "hr" -> "Operational workspace for workforce records, approvals, schedules, and readiness.";
            case "finance" -> "Operational workspace for payroll readiness, processing, deductions, and history.";
            case "it" -> "Operational workspace for user access, roles, and security visibility.";
            case "supervisor" -> hasTeamAuthority()
                    ? "Operational workspace for assigned employees, attendance, and approvals."
                    : "Personal workspace for schedule, attendance, leave, and overtime.";
            default -> "Personal workspace for schedule, attendance, leave, and overtime.";
        };
    }

    private String getRoleWorkspaceChip() {
        return switch (getRoleKey()) {
            case "admin" -> "Operational Workspace";
            case "hr" -> "HR Operations Workspace";
            case "finance" -> "Payroll Operations Workspace";
            case "supervisor" -> hasTeamAuthority() ? "My Workforce + Team Operations" : "My Workforce";
            case "it" -> "Access Operations Workspace";
            default -> "Personal Workspace";
        };
    }

    private SidebarIcon.Type getRoleIcon() {
        return switch (getRoleKey()) {
            case "admin", "it" -> SidebarIcon.Type.SETTINGS;
            case "hr" -> SidebarIcon.Type.USERS;
            case "finance" -> SidebarIcon.Type.WALLET;
            default -> SidebarIcon.Type.PROFILE;
        };
    }

    private SidebarIcon.Type getIconType(String viewId) {
        if (RoleAccessService.WORKFORCE_GOVERNANCE_VIEW.equals(viewId)
                || RoleAccessService.EMPLOYEES_VIEW.equals(viewId)) return SidebarIcon.Type.USERS;
        if (RoleAccessService.PAYROLL_VIEW.equals(viewId)) return SidebarIcon.Type.WALLET;
        if (RoleAccessService.PAYROLL_HISTORY_VIEW.equals(viewId)) return SidebarIcon.Type.HISTORY;
        if (RoleAccessService.OPERATIONAL_INBOX_VIEW.equals(viewId)) return SidebarIcon.Type.HISTORY;
        if (RoleAccessService.ORG_CHART_VIEW.equals(viewId)) return SidebarIcon.Type.USERS;
        if (RoleAccessService.LEAVE_REVIEW_VIEW.equals(viewId)
                || RoleAccessService.FILE_LEAVE_VIEW.equals(viewId)
                || RoleAccessService.ATTENDANCE_VIEW.equals(viewId)
                || RoleAccessService.ATTENDANCE_ADJUSTMENT_VIEW.equals(viewId)
                || RoleAccessService.OVERTIME_VIEW.equals(viewId)
                || RoleAccessService.TIMEKEEPING_CALENDAR_VIEW.equals(viewId)
                || RoleAccessService.SCHEDULING_VIEW.equals(viewId)) {
            return SidebarIcon.Type.CALENDAR;
        }
        if (RoleAccessService.TEAM_OPERATIONS_VIEW.equals(viewId)) return SidebarIcon.Type.USERS;
        if (RoleAccessService.USER_MANAGEMENT_VIEW.equals(viewId)) return SidebarIcon.Type.SETTINGS;
        if (RoleAccessService.PAYSLIP_VIEW.equals(viewId)) return SidebarIcon.Type.PAYSLIP;
        if (RoleAccessService.PROFILE_VIEW.equals(viewId)) return SidebarIcon.Type.PROFILE;
        return SidebarIcon.Type.DASHBOARD;
    }

    private String getViewLabel(String viewId) {
        if (RoleAccessService.WORKFORCE_GOVERNANCE_VIEW.equals(viewId)) return "Workforce Governance";
        if (RoleAccessService.EMPLOYEES_VIEW.equals(viewId)) return "Employee Database";
        if (RoleAccessService.PAYROLL_VIEW.equals(viewId)) return "Payroll Operations";
        if (RoleAccessService.PAYROLL_HISTORY_VIEW.equals(viewId)) return "Payroll History";
        if (RoleAccessService.OPERATIONAL_INBOX_VIEW.equals(viewId)) return "Operational Inbox";
        if (RoleAccessService.ORG_CHART_VIEW.equals(viewId)) return "Org Chart";
        if (RoleAccessService.LEAVE_REVIEW_VIEW.equals(viewId)) return "Leave Review";
        if (RoleAccessService.OVERTIME_VIEW.equals(viewId)) return "Overtime";
        if (RoleAccessService.SCHEDULING_VIEW.equals(viewId)) return "Workforce Schedule";
        if (RoleAccessService.USER_MANAGEMENT_VIEW.equals(viewId)) return "User Access";
        if (RoleAccessService.PROFILE_VIEW.equals(viewId)) return "My Profile";
        if (RoleAccessService.ATTENDANCE_VIEW.equals(viewId)) return "Attendance";
        if (RoleAccessService.ATTENDANCE_ADJUSTMENT_VIEW.equals(viewId)) return "Attendance Adjustments";
        if (RoleAccessService.TIMEKEEPING_CALENDAR_VIEW.equals(viewId)) return "Work Calendar";
        if (RoleAccessService.TEAM_OPERATIONS_VIEW.equals(viewId)) return "Team Operations";
        if (RoleAccessService.PAYSLIP_VIEW.equals(viewId)) return "Payslip";
        if (RoleAccessService.FILE_LEAVE_VIEW.equals(viewId)) return "Leave";
        return viewId;
    }

    private JPanel createCardPanel(java.awt.LayoutManager layout, int top, int left, int bottom, int right) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                new EmptyBorder(top, left, bottom, right)
        ));
        return panel;
    }

    private JLabel createIconLabel(SidebarIcon.Type iconType, Color color, int size) {
        JLabel label = new JLabel(new SidebarIcon(iconType, size));
        label.setForeground(color);
        return label;
    }

    private Color tint(Color color) {
        int red = Math.min(255, color.getRed() + 42);
        int green = Math.min(255, color.getGreen() + 82);
        int blue = Math.min(255, color.getBlue() + 92);
        return new Color(red, green, blue);
    }

    private String getRoleKey() {
        return user.getRole() == null ? "" : user.getRole().trim().toLowerCase();
    }

    private boolean hasTeamAuthority() {
        return user != null && employeeService.hasAssignedTeam(user.getEmployeeId());
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String displayTime(String value) {
        return value == null || value.isBlank() ? "--" : value;
    }

    private String formatHours(double value) {
        return String.format(Locale.ENGLISH, "%.2fh", Math.max(0.0, value));
    }

    private String formatCurrency(double value) {
        NumberFormat peso = NumberFormat.getCurrencyInstance(
                Locale.of("en", "PH")
        );
        return peso.format(value);
    }

    private boolean hasLivePayrollTotals() {
        return summary.getTotalGross() <= 0
                || summary.getTotalDeductions() > 0
                || Double.compare(summary.getTotalNet(), summary.getTotalGross()) != 0;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 24;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return Math.max(120, visibleRect.height - 80);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    private static class MetricSpec {
        private final String label;
        private final String value;
        private final String note;
        private final SidebarIcon.Type iconType;
        private final boolean live;
        private final String actionCard;
        private final String detailLeft;
        private final String detailRight;

        private MetricSpec(String label, String value, SidebarIcon.Type iconType) {
            this(label, value, "", iconType, true);
        }

        private MetricSpec(String label, String value, String note, SidebarIcon.Type iconType, boolean live) {
            this(label, value, note, iconType, live, null, null, null);
        }

        private MetricSpec(String label, String value, String note, SidebarIcon.Type iconType, boolean live,
                String actionCard, String detailLeft, String detailRight) {
            this.label = label;
            this.value = value;
            this.note = note;
            this.iconType = iconType;
            this.live = live;
            this.actionCard = actionCard;
            this.detailLeft = detailLeft;
            this.detailRight = detailRight;
        }
    }

    private static class QuickActionSpec {
        private final String label;
        private final String card;
        private final SidebarIcon.Type iconType;

        private QuickActionSpec(String label, String card, SidebarIcon.Type iconType) {
            this.label = label;
            this.card = card;
            this.iconType = iconType;
        }
    }

    private static class WrappingPanel extends JPanel {
        private WrappingPanel(FlowLayout layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension preferred = super.getPreferredSize();
            int availableWidth = getParent() == null ? getWidth() : getParent().getWidth();
            if (availableWidth <= 0) {
                return preferred;
            }

            FlowLayout layout = (FlowLayout) getLayout();
            Insets insets = getInsets();
            int maxWidth = Math.max(1, availableWidth - insets.left - insets.right);
            int rowWidth = 0;
            int rowHeight = 0;
            int totalHeight = insets.top + insets.bottom;
            int rows = 0;

            for (java.awt.Component component : getComponents()) {
                if (!component.isVisible()) {
                    continue;
                }

                Dimension size = component.getPreferredSize();
                int nextWidth = rowWidth == 0 ? size.width : rowWidth + layout.getHgap() + size.width;
                if (rowWidth > 0 && nextWidth > maxWidth) {
                    totalHeight += rowHeight + (rows == 0 ? 0 : layout.getVgap());
                    rows++;
                    rowWidth = size.width;
                    rowHeight = size.height;
                } else {
                    rowWidth = nextWidth;
                    rowHeight = Math.max(rowHeight, size.height);
                }
            }

            if (rowWidth > 0) {
                totalHeight += rowHeight + (rows == 0 ? 0 : layout.getVgap());
            }

            return new Dimension(Math.min(preferred.width, availableWidth), Math.max(preferred.height, totalHeight));
        }
    }

}
