package com.mycompany.oop.view;

import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.OperationalNotification;
import com.mycompany.oop.service.OperationalInboxService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.Cursor;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class OperationalInboxPanel extends JPanel implements RefreshablePanel {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.ENGLISH);

    private final Employee currentUser;
    private final OperationalInboxService inboxService;
    private final Consumer<String> navigationHandler;
    private final Runnable inboxReadHandler;
    private JPanel contentBody;
    private boolean dataLoading;

    public OperationalInboxPanel(Employee currentUser) {
        this(currentUser, null, null);
    }

    public OperationalInboxPanel(Employee currentUser, Consumer<String> navigationHandler) {
        this(currentUser, navigationHandler, null);
    }

    public OperationalInboxPanel(Employee currentUser, Consumer<String> navigationHandler, Runnable inboxReadHandler) {
        this.currentUser = currentUser;
        this.navigationHandler = navigationHandler;
        this.inboxReadHandler = inboxReadHandler;
        this.inboxService = new OperationalInboxService();

        setLayout(new BorderLayout());
        setBackground(UITheme.BG);
        add(UITheme.createTitleBar("Operational Inbox"), BorderLayout.NORTH);
        contentBody = createLoadingPanel();
        add(contentBody, BorderLayout.CENTER);
        refreshData();
    }

    @Override
    public void refreshData() {
        if (dataLoading) {
            return;
        }
        dataLoading = true;
        if (contentBody != null) {
            remove(contentBody);
        }
        contentBody = createLoadingPanel();
        add(contentBody, BorderLayout.CENTER);
        revalidate();
        repaint();

        SwingWorker<List<OperationalNotification>, Void> worker = new SwingWorker<>() {
            private long startedAtMs;

            @Override
            protected List<OperationalNotification> doInBackground() {
                startedAtMs = System.currentTimeMillis();
                inboxService.markVisibleNotificationsRead(currentUser);
                return inboxService.getNotifications(currentUser);
            }

            @Override
            protected void done() {
                try {
                    swapContent(createContent(get()));
                    if (inboxReadHandler != null) {
                        inboxReadHandler.run();
                    }
                    System.out.println("[perf] OperationalInboxPanel refreshData took "
                            + (System.currentTimeMillis() - startedAtMs) + " ms");
                } catch (Exception ex) {
                    swapContent(createErrorPanel());
                } finally {
                    dataLoading = false;
                }
            }
        };
        worker.execute();
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

    private JPanel createLoadingPanel() {
        JPanel content = UITheme.createWorkspacePanel(new BorderLayout(0, 14));
        content.add(UITheme.createSkeletonCard("Operational Inbox", 5), BorderLayout.CENTER);
        return content;
    }

    private JPanel createErrorPanel() {
        JPanel content = UITheme.createWorkspacePanel(new BorderLayout());
        JLabel label = new JLabel("Unable to load Operational Inbox. Please refresh and try again.", JLabel.CENTER);
        label.setFont(UITheme.FONT_BODY_BOLD);
        label.setForeground(UITheme.DANGER);
        content.add(label, BorderLayout.CENTER);
        return content;
    }

    private JPanel createContent() {
        return createContent(inboxService.getNotifications(currentUser));
    }

    private JPanel createContent(List<OperationalNotification> notifications) {
        JPanel content = UITheme.createWorkspacePanel(new BorderLayout(0, 14));
        content.add(WorkforceFormToolkit.createSection(
                "Operational Inbox",
                "Workforce alerts, workflow updates, and action-oriented notifications for your role."
        ), BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        for (OperationalNotification notification : notifications) {
            list.add(createNotificationCard(notification));
            list.add(Box.createVerticalStrut(10));
        }

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UITheme.BG);
        WorkforceFormToolkit.tuneScrollPane(scrollPane);
        content.add(scrollPane, BorderLayout.CENTER);

        return content;
    }

    private JPanel createNotificationCard(OperationalNotification notification) {
        JPanel card = new JPanel(new BorderLayout(14, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 132));
        card.setAlignmentX(LEFT_ALIGNMENT);
        String target = resolveNavigationTarget(notification);

        JLabel indicator = new JLabel();
        indicator.setOpaque(true);
        indicator.setBackground(getSeverityColor(notification.getSeverity()));
        indicator.setPreferredSize(new Dimension(10, 10));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(notification.getTitle());
        title.setFont(UITheme.FONT_BODY_BOLD);
        title.setForeground(UITheme.TEXT_PRIMARY);

        JLabel message = new JLabel("<html><body style='width:620px'>" + notification.getMessage() + "</body></html>");
        message.setFont(UITheme.FONT_BODY);
        message.setForeground(UITheme.TEXT_SECONDARY);

        JLabel meta = new JLabel(formatMeta(notification));
        meta.setFont(UITheme.FONT_SMALL);
        meta.setForeground(UITheme.TEXT_SECONDARY);

        text.add(title);
        text.add(Box.createVerticalStrut(4));
        text.add(message);
        text.add(Box.createVerticalStrut(5));
        text.add(meta);

        card.add(indicator, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        card.add(createStatusPanel(notification), BorderLayout.EAST);
        if (target != null) {
            installRouting(card, target);
        }
        return card;
    }

    private JPanel createStatusPanel(OperationalNotification notification) {
        Color accent = getSeverityColor(notification.getSeverity());
        JPanel panel = new JPanel(new BorderLayout(0, 7));
        panel.setBackground(getStatusTint(notification));
        panel.setPreferredSize(new Dimension(184, 92));
        panel.setMaximumSize(new Dimension(196, 104));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(getStatusBorder(notification), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        header.setOpaque(false);
        JLabel marker = new JLabel();
        marker.setOpaque(true);
        marker.setBackground(accent);
        marker.setPreferredSize(new Dimension(9, 9));
        JLabel label = new JLabel(getStatusLabel(notification));
        label.setFont(UITheme.FONT_SMALL);
        label.setForeground(accent);
        header.add(marker);
        header.add(label);

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));

        JLabel support = createStatusDetail(getStatusSupport(notification), UITheme.TEXT_PRIMARY);
        JLabel hint = createStatusDetail(getStatusHint(notification), UITheme.TEXT_SECONDARY);
        JLabel time = createStatusDetail(getStatusTime(notification), UITheme.TEXT_SECONDARY);
        details.add(support);
        details.add(Box.createVerticalStrut(3));
        details.add(hint);
        details.add(Box.createVerticalStrut(3));
        details.add(time);

        panel.add(header, BorderLayout.NORTH);
        panel.add(details, BorderLayout.CENTER);
        return panel;
    }

    private JLabel createStatusDetail(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(UITheme.FONT_SMALL);
        label.setForeground(color);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        return label;
    }

    private String formatMeta(OperationalNotification notification) {
        String time = notification.getTimestamp() == null
                ? "Time unavailable"
                : notification.getTimestamp().format(TIME_FORMAT);
        return notification.getCategory() + " · " + formatPriority(notification.getPriority()) + " · " + time;
    }

    private Color getSeverityColor(OperationalNotification.Severity severity) {
        if (severity == null) {
            return UITheme.TEXT_SECONDARY;
        }

        return switch (severity) {
            case CRITICAL -> UITheme.DANGER;
            case WARNING -> UITheme.YELLOW;
            case SUCCESS -> UITheme.SUCCESS;
            case INFO -> UITheme.BLUE;
        };
    }

    private Color getStatusTint(OperationalNotification notification) {
        return switch (getStatusTone(notification)) {
            case "critical" -> new Color(254, 242, 242);
            case "action" -> new Color(255, 251, 235);
            case "success" -> new Color(240, 253, 244);
            case "review" -> new Color(239, 246, 255);
            default -> new Color(248, 250, 252);
        };
    }

    private Color getStatusBorder(OperationalNotification notification) {
        return switch (getStatusTone(notification)) {
            case "critical" -> new Color(254, 202, 202);
            case "action" -> new Color(253, 230, 138);
            case "success" -> new Color(187, 247, 208);
            case "review" -> new Color(191, 219, 254);
            default -> UITheme.BORDER;
        };
    }

    private String getStatusLabel(OperationalNotification notification) {
        String status = safe(notification.getStatus()).toUpperCase(Locale.ENGLISH);
        if (status.contains("APPROVED") || status.contains("COMPLETE") || status.contains("CORRECTED")) {
            return "APPROVED";
        }
        if (status.contains("REJECT")) {
            return "REJECTED";
        }
        if (status.contains("REVIEW") || status.contains("PENDING")) {
            return notification.getPriority() == OperationalNotification.Priority.ACTION_REQUIRED
                    ? "REQUIRES REVIEW"
                    : "UNDER REVIEW";
        }

        return switch (notification.getPriority() == null
                ? OperationalNotification.Priority.INFORMATIONAL
                : notification.getPriority()) {
            case CRITICAL -> "CRITICAL";
            case ACTION_REQUIRED -> "REQUIRES REVIEW";
            case REVIEW -> "UNDER REVIEW";
            case INFORMATIONAL -> notification.getSeverity() == OperationalNotification.Severity.SUCCESS
                    ? "APPROVED"
                    : "INFORMATIONAL";
        };
    }

    private String getStatusSupport(OperationalNotification notification) {
        return switch (getStatusTone(notification)) {
            case "critical" -> "Immediate attention";
            case "action" -> "Pending your action";
            case "success" -> "No action needed";
            case "review" -> "In progress";
            default -> "Active alert";
        };
    }

    private String getStatusHint(OperationalNotification notification) {
        if (resolveNavigationTarget(notification) != null) {
            return "Open workflow";
        }
        return switch (getStatusTone(notification)) {
            case "critical", "action" -> "Review needed";
            case "review" -> "View details";
            case "success" -> "Approved";
            default -> "No action needed";
        };
    }

    private String getStatusTime(OperationalNotification notification) {
        return notification.getTimestamp() == null
                ? safe(notification.getCategory())
                : notification.getTimestamp().format(TIME_FORMAT);
    }

    private String getStatusTone(OperationalNotification notification) {
        String status = safe(notification.getStatus()).toUpperCase(Locale.ENGLISH);
        if (notification.getSeverity() == OperationalNotification.Severity.CRITICAL
                || status.contains("REJECT")) {
            return "critical";
        }
        if (notification.getPriority() == OperationalNotification.Priority.ACTION_REQUIRED
                || status.contains("REVIEW")
                || status.contains("PENDING")) {
            return "action";
        }
        if (notification.getSeverity() == OperationalNotification.Severity.SUCCESS
                || status.contains("APPROVED")
                || status.contains("COMPLETE")
                || status.contains("CORRECTED")) {
            return "success";
        }
        if (notification.getPriority() == OperationalNotification.Priority.REVIEW
                || notification.getSeverity() == OperationalNotification.Severity.INFO) {
            return "review";
        }
        return "info";
    }

    private String formatPriority(OperationalNotification.Priority priority) {
        if (priority == null) {
            return "Informational";
        }

        return switch (priority) {
            case CRITICAL -> "Critical";
            case ACTION_REQUIRED -> "Action Required";
            case REVIEW -> "Review";
            case INFORMATIONAL -> "Informational";
        };
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }

    private String resolveNavigationTarget(OperationalNotification notification) {
        if (notification == null || currentUser == null) {
            return null;
        }

        String role = safe(currentUser.getRole()).toLowerCase(Locale.ENGLISH);
        String category = safe(notification.getCategory()).toLowerCase(Locale.ENGLISH);
        String combined = (safe(notification.getTitle()) + " " + safe(notification.getMessage()) + " "
                + safe(notification.getStatus()) + " " + category).toLowerCase(Locale.ENGLISH);

        if (role.contains("finance")) {
            if (combined.contains("payroll") || combined.contains("finance") || combined.contains("hr-endorsed")) {
                return "PAYROLL";
            }
        }
        if (role.contains("hr")) {
            if (combined.contains("workforce") || combined.contains("supervisor-cleared")
                    || combined.contains("hr")
                    || combined.contains("overtime") || combined.contains("attendance")) {
                return "WORKFORCE_GOVERNANCE";
            }
            if (combined.contains("leave")
                    || combined.contains("overtime") || combined.contains("attendance")) {
                return "EMP";
            }
        }
        if (role.contains("supervisor")) {
            if (combined.contains("team") || combined.contains("supervisor")
                    || combined.contains("correction") || combined.contains("overtime")
                    || combined.contains("leave") || combined.contains("attendance")
                    || combined.contains("schedule")) {
                return "TEAM_OPERATIONS";
            }
        }
        if (combined.contains("schedule") || combined.contains("calendar") || combined.contains("workforce")) {
            return "TIMEKEEPING";
        }
        if (combined.contains("overtime")) {
            return "OVERTIME";
        }
        if (combined.contains("leave")) {
            return "FILE";
        }
        if (combined.contains("attendance") || combined.contains("correction")) {
            return "TIMEKEEPING";
        }
        if (combined.contains("access") || combined.contains("system")) {
            return "IT";
        }
        return null;
    }

    private void routeToWorkflow(String target) {
        if (navigationHandler != null && target != null) {
            navigationHandler.accept(target);
        }
    }

    private void installRouting(java.awt.Component component, String target) {
        component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (component instanceof javax.swing.JComponent jComponent) {
            jComponent.setToolTipText("Open " + workspaceLabel(target));
        }
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                routeToWorkflow(target);
            }
        });
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                installRouting(child, target);
            }
        }
    }

    private String workspaceLabel(String target) {
        return switch (target) {
            case "TEAM_OPERATIONS" -> "Team Operations";
            case "WORKFORCE_GOVERNANCE" -> "Workforce Governance";
            case "EMP" -> "Employee Database";
            case "PAYROLL" -> "Payroll Operations";
            case "TIMEKEEPING" -> "Work Calendar";
            case "OVERTIME" -> "Overtime";
            case "FILE" -> "Leave";
            case "IT" -> "User Access";
            default -> "workspace";
        };
    }
}
