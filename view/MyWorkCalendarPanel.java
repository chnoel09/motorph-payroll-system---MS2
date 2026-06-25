package com.mycompany.oop.view;

import com.mycompany.oop.model.AttendanceAwareness;
import com.mycompany.oop.model.AttendanceAdjustment;
import com.mycompany.oop.model.AttendanceRecord;
import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.EmployeeSchedule;
import com.mycompany.oop.model.EmployeeShift;
import com.mycompany.oop.model.Holiday;
import com.mycompany.oop.model.Leave;
import com.mycompany.oop.model.OvertimeRequest;
import com.mycompany.oop.service.AttendanceAdjustmentService;
import com.mycompany.oop.service.AttendanceAwarenessService;
import com.mycompany.oop.service.AttendanceService;
import com.mycompany.oop.service.LeaveService;
import com.mycompany.oop.service.OvertimeService;
import com.mycompany.oop.service.ScheduleService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MyWorkCalendarPanel extends JPanel implements RefreshablePanel {

    private static final Color WORK_DAY = new Color(239, 246, 255);
    private static final Color REST_DAY = new Color(248, 250, 252);
    private static final Color HOLIDAY = new Color(254, 243, 199);
    private static final Color LEAVE_PENDING = new Color(255, 247, 237);
    private static final Color LEAVE_APPROVED = new Color(236, 253, 245);
    private static final Color LEAVE_REJECTED = new Color(254, 242, 242);

    private final Employee currentUser;
    private final ScheduleService scheduleService;
    private final AttendanceService attendanceService;
    private final AttendanceAdjustmentService attendanceAdjustmentService;
    private final LeaveService leaveService;
    private final OvertimeService overtimeService;
    private final AttendanceAwarenessService attendanceAwarenessService;
    private YearMonth visibleMonth;
    private JPanel overviewPanel;
    private JPanel calendarGrid;
    private JLabel monthLabel;

    public MyWorkCalendarPanel(Employee currentUser) {
        this.currentUser = currentUser;
        this.scheduleService = new ScheduleService();
        this.attendanceService = new AttendanceService();
        this.attendanceAdjustmentService = new AttendanceAdjustmentService();
        this.leaveService = new LeaveService();
        this.overtimeService = new OvertimeService();
        this.attendanceAwarenessService = new AttendanceAwarenessService();
        this.visibleMonth = YearMonth.now();

        setLayout(new BorderLayout());
        setBackground(UITheme.BG);

        add(UITheme.createTitleBar("My Work Calendar"), BorderLayout.NORTH);
        add(createContentPanel(), BorderLayout.CENTER);
    }

    private JPanel createContentPanel() {
        JPanel content = UITheme.createWorkspacePanel(new BorderLayout(0, 14));
        JPanel topStack = new JPanel();
        topStack.setOpaque(false);
        topStack.setLayout(new BoxLayout(topStack, BoxLayout.Y_AXIS));
        topStack.add(createHeaderPanel());
        topStack.add(Box.createVerticalStrut(12));
        overviewPanel = new JPanel(new BorderLayout());
        overviewPanel.setOpaque(false);
        topStack.add(overviewPanel);
        content.add(topStack, BorderLayout.NORTH);

        calendarGrid = new JPanel(new GridLayout(0, 7, 10, 10));
        calendarGrid.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(calendarGrid);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UITheme.BG);
        WorkforceFormToolkit.tuneScrollPane(scrollPane);
        content.add(scrollPane, BorderLayout.CENTER);

        refreshCalendar();
        return content;
    }

    private JPanel createHeaderPanel() {
        JPanel header = WorkforceFormToolkit.createSection(
                "Workforce Calendar",
                "Your schedule, time entries, attendance concerns, leave, overtime, and holidays in one workspace."
        );

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);

        JButton previousButton = UITheme.createButton("Previous");
        JButton todayButton = UITheme.createButton("This Month");
        JButton nextButton = UITheme.createButton("Next");
        JButton leaveButton = UITheme.createButton("File Leave");
        JButton overtimeButton = UITheme.createButton("File OT");
        JButton correctionButton = UITheme.createButton("Request Correction");
        monthLabel = new JLabel();
        monthLabel.setFont(UITheme.FONT_BODY_BOLD);
        monthLabel.setForeground(UITheme.TEXT_PRIMARY);
        monthLabel.setBorder(new EmptyBorder(0, 0, 0, 12));

        previousButton.addActionListener(e -> {
            visibleMonth = visibleMonth.minusMonths(1);
            refreshCalendar();
        });
        todayButton.addActionListener(e -> {
            visibleMonth = YearMonth.now();
            refreshCalendar();
        });
        nextButton.addActionListener(e -> {
            visibleMonth = visibleMonth.plusMonths(1);
            refreshCalendar();
        });
        leaveButton.addActionListener(e -> showLeaveRequestDialog(LocalDate.now()));
        overtimeButton.addActionListener(e -> showOvertimeRequestDialog(LocalDate.now(), 0.0));
        correctionButton.addActionListener(e -> showCorrectionRequestDialog(LocalDate.now(), "Incomplete Attendance"));

        controls.add(monthLabel);
        controls.add(previousButton);
        controls.add(todayButton);
        controls.add(nextButton);
        controls.add(leaveButton);
        controls.add(overtimeButton);
        controls.add(correctionButton);

        header.add(controls, BorderLayout.SOUTH);
        return header;
    }

    private void refreshCalendar() {
        calendarGrid.removeAll();
        monthLabel.setText(visibleMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " " + visibleMonth.getYear());

        addWeekdayHeaders();

        CalendarData data = loadCalendarData();
        refreshOverview(data);
        int leadingBlankDays = visibleMonth.atDay(1).getDayOfWeek().getValue() % 7;
        for (int i = 0; i < leadingBlankDays; i++) {
            calendarGrid.add(createBlankDayCard());
        }

        for (int day = 1; day <= visibleMonth.lengthOfMonth(); day++) {
            LocalDate date = visibleMonth.atDay(day);
            calendarGrid.add(createDayCard(date, data));
        }

        calendarGrid.revalidate();
        calendarGrid.repaint();
    }

    private void showCorrectionRequestDialog(LocalDate defaultDate, String defaultType) {
        if (currentUser == null) {
            return;
        }

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(8, 8, 8, 8));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        JTextField dateField = new JTextField((defaultDate == null ? LocalDate.now() : defaultDate).toString(), 16);
        WorkforceFormToolkit.styleTextField(dateField);
        WorkforceFormToolkit.applyDateHelp(dateField);

        JComboBox<String> typeBox = new JComboBox<>(new String[]{
                "Missing Time In", "Missing Time Out", "Incorrect Time", "Incomplete Attendance", "Other"
        });
        WorkforceFormToolkit.styleComboBox(typeBox);
        if (defaultType != null && !defaultType.isBlank()) {
            typeBox.setSelectedItem(defaultType);
        }

        JTextField remarksField = new JTextField(24);
        WorkforceFormToolkit.styleTextField(remarksField);

        addDialogRow(form, gbc, 0, "Attendance Date", dateField);
        addDialogRow(form, gbc, 1, "Correction Type", typeBox);
        addDialogRow(form, gbc, 2, "Remarks", remarksField);

        JLabel help = new JLabel("Your request starts with supervisor review, then HR correction approval.");
        help.setFont(UITheme.FONT_SMALL);
        help.setForeground(UITheme.TEXT_SECONDARY);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        form.add(help, gbc);

        int result = JOptionPane.showConfirmDialog(this, form, "Request Attendance Correction",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            attendanceAdjustmentService.requestOwnAdjustment(
                    currentUser,
                    LocalDate.parse(dateField.getText().trim()),
                    String.valueOf(typeBox.getSelectedItem()),
                    remarksField.getText().trim());
            JOptionPane.showMessageDialog(this,
                    "Attendance correction submitted. Stage: Pending Supervisor Review.",
                    "Correction Submitted",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshCalendar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Correction Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showLeaveRequestDialog(LocalDate defaultDate) {
        if (currentUser == null) {
            return;
        }

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(8, 8, 8, 8));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        JComboBox<String> typeBox = new JComboBox<>(new String[]{"Vacation", "Sick", "Emergency", "Personal"});
        WorkforceFormToolkit.styleComboBox(typeBox);
        JTextField startField = new JTextField((defaultDate == null ? LocalDate.now() : defaultDate).toString(), 16);
        JTextField endField = new JTextField((defaultDate == null ? LocalDate.now() : defaultDate).toString(), 16);
        WorkforceFormToolkit.styleTextField(startField);
        WorkforceFormToolkit.styleTextField(endField);
        WorkforceFormToolkit.applyDateHelp(startField);
        WorkforceFormToolkit.applyDateHelp(endField);
        JTextField reasonField = new JTextField(24);
        WorkforceFormToolkit.styleTextField(reasonField);

        addDialogRow(form, gbc, 0, "Leave Type", typeBox);
        addDialogRow(form, gbc, 1, "Start Date", startField);
        addDialogRow(form, gbc, 2, "End Date", endField);
        addDialogRow(form, gbc, 3, "Reason", reasonField);

        JLabel help = new JLabel("Your leave request starts with supervisor review, then HR final approval.");
        help.setFont(UITheme.FONT_SMALL);
        help.setForeground(UITheme.TEXT_SECONDARY);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        form.add(help, gbc);

        int result = JOptionPane.showConfirmDialog(this, form, "File Leave from Calendar",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            leaveService.fileLeave(
                    currentUser.getEmployeeId(),
                    String.valueOf(typeBox.getSelectedItem()),
                    startField.getText().trim(),
                    endField.getText().trim(),
                    reasonField.getText().trim());
            JOptionPane.showMessageDialog(this,
                    "Leave request submitted. Stage: Pending Supervisor Approval.",
                    "Leave Submitted",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshCalendar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Leave Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showOvertimeRequestDialog(LocalDate defaultDate, double suggestedHours) {
        if (currentUser == null) {
            return;
        }

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(8, 8, 8, 8));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        JTextField dateField = new JTextField((defaultDate == null ? LocalDate.now() : defaultDate).toString(), 16);
        WorkforceFormToolkit.styleTextField(dateField);
        WorkforceFormToolkit.applyDateHelp(dateField);
        JTextField hoursField = new JTextField(suggestedHours > 0 ? String.format(Locale.ENGLISH, "%.2f", suggestedHours) : "", 16);
        WorkforceFormToolkit.styleTextField(hoursField);
        JTextField reasonField = new JTextField(24);
        WorkforceFormToolkit.styleTextField(reasonField);

        addDialogRow(form, gbc, 0, "Overtime Date", dateField);
        addDialogRow(form, gbc, 1, "Overtime Hours", hoursField);
        addDialogRow(form, gbc, 2, "Reason", reasonField);

        JLabel help = new JLabel(suggestedHours > 0
                ? "Potential OT was suggested from time out after scheduled shift end."
                : "Overtime starts with supervisor review, then HR final approval.");
        help.setFont(UITheme.FONT_SMALL);
        help.setForeground(UITheme.TEXT_SECONDARY);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        form.add(help, gbc);

        int result = JOptionPane.showConfirmDialog(this, form, "File Overtime from Calendar",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            overtimeService.fileOwnOvertimeRequest(
                    currentUser,
                    LocalDate.parse(dateField.getText().trim()),
                    Double.parseDouble(hoursField.getText().trim()),
                    reasonField.getText().trim());
            JOptionPane.showMessageDialog(this,
                    "Overtime request submitted. Stage: Pending Supervisor Approval.",
                    "Overtime Submitted",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshCalendar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Overtime Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addDialogRow(JPanel form, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(UITheme.FONT_BODY_BOLD);
        labelComponent.setForeground(UITheme.TEXT_SECONDARY);
        form.add(labelComponent, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(field, gbc);
    }

    @Override
    public void refreshData() {
        refreshCalendar();
    }

    private void addWeekdayHeaders() {
        String[] weekdays = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String weekday : weekdays) {
            JLabel label = new JLabel(weekday, SwingConstants.CENTER);
            label.setFont(UITheme.FONT_BODY_BOLD);
            label.setForeground(UITheme.TEXT_SECONDARY);
            calendarGrid.add(label);
        }
    }

    private JPanel createBlankDayCard() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(0, 150));
        return panel;
    }

    private JPanel createDayCard(LocalDate date, CalendarData data) {
        EmployeeSchedule schedule = data.schedulesByDate.get(date);
        Holiday holiday = data.holidaysByDate.get(date);
        Leave leave = data.leavesByDate.get(date);
        OvertimeRequest overtime = data.overtimeByDate.get(date);
        AttendanceAwareness awareness = data.awarenessByDate.get(date);
        AttendanceRecord attendance = data.attendanceByDate.get(date);
        double suggestedOtHours = calculateSuggestedOtHours(schedule, attendance, data);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(getDayBackground(schedule, holiday, leave));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                new EmptyBorder(10, 10, 10, 10)
        ));
        card.setPreferredSize(new Dimension(0, 150));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setToolTipText("Open workforce actions for " + date);
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showDayActionDialog(date, data, suggestedOtHours);
            }
        });

        JLabel dayLabel = new JLabel(String.valueOf(date.getDayOfMonth()));
        dayLabel.setFont(UITheme.FONT_BODY_BOLD);
        dayLabel.setForeground(date.equals(LocalDate.now()) ? UITheme.ACCENT : UITheme.TEXT_PRIMARY);
        card.add(dayLabel);
        card.add(Box.createVerticalStrut(8));

        if (schedule == null) {
            card.add(createMutedLine("No schedule assigned"));
        } else if (schedule.isRestDay()) {
            card.add(createChip("Rest Day", UITheme.TEXT_SECONDARY, Color.WHITE));
        } else {
            EmployeeShift shift = schedule.getShiftId() == null ? null : data.shiftsById.get(schedule.getShiftId());
            if (shift == null) {
                card.add(createChip("Assigned Shift", UITheme.BLUE, Color.WHITE));
            } else {
                card.add(createChip(shift.getShiftName(), UITheme.BLUE, Color.WHITE));
                card.add(createMutedLine(shift.getStartTime() + " - " + shift.getEndTime()));
            }
            card.add(createMutedLine("Status: " + safeText(schedule.getStatus(), "Assigned")));
        }

        if (holiday != null) {
            card.add(Box.createVerticalStrut(5));
            card.add(createChip("Holiday: " + holiday.getHolidayName(), UITheme.YELLOW, Color.WHITE));
        }

        if (leave != null) {
            card.add(Box.createVerticalStrut(5));
            card.add(createChip(formatLeaveStatus(leave), getLeaveColor(leave), Color.WHITE));
        }

        if (overtime != null) {
            card.add(Box.createVerticalStrut(5));
            card.add(createChip("OT: " + overtime.getOvertimeHours() + "h " + safeText(overtime.getStatus(), "PENDING"),
                    getOvertimeColor(overtime), Color.WHITE));
        }

        if (attendance != null) {
            card.add(Box.createVerticalStrut(5));
            card.add(createMutedLine("Time: " + safeText(attendance.getTimeIn(), "--:--")
                    + " - " + safeText(attendance.getTimeOut(), "--:--")));
        }

        if (suggestedOtHours > 0 && overtime == null) {
            card.add(Box.createVerticalStrut(5));
            card.add(createChip("Potential OT: " + String.format(Locale.ENGLISH, "%.2fh", suggestedOtHours),
                    UITheme.YELLOW, Color.WHITE));
        }

        AttendanceAdjustment adjustment = data.adjustmentsByDate.get(date);
        if (adjustment != null) {
            card.add(Box.createVerticalStrut(5));
            card.add(createChip("Correction: " + adjustment.getStatus(), getAdjustmentColor(adjustment), Color.WHITE));
        }

        if (awareness != null && awareness.requiresReview()) {
            card.add(Box.createVerticalStrut(5));
            card.add(createChip(awareness.getStatus(), getAwarenessColor(awareness), Color.WHITE));
        }

        return card;
    }

    private void showDayActionDialog(LocalDate date, CalendarData data, double suggestedOtHours) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));
        JLabel context = new JLabel("<html><body style='width:420px'>"
                + "<b>" + date + "</b><br>"
                + buildDayContextText(date, data, suggestedOtHours)
                + "</body></html>");
        context.setFont(UITheme.FONT_BODY);
        context.setForeground(UITheme.TEXT_PRIMARY);
        panel.add(context, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton leaveButton = UITheme.createButton("File Leave");
        JButton otButton = UITheme.createButton(suggestedOtHours > 0 ? "File Suggested OT" : "File OT");
        JButton correctionButton = UITheme.createButton("Request Correction");
        actions.add(leaveButton);
        actions.add(otButton);
        actions.add(correctionButton);
        panel.add(actions, BorderLayout.SOUTH);

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Workforce Actions", Dialog.ModalityType.APPLICATION_MODAL);
        leaveButton.addActionListener(e -> {
            dialog.dispose();
            showLeaveRequestDialog(date);
        });
        otButton.addActionListener(e -> {
            dialog.dispose();
            showOvertimeRequestDialog(date, suggestedOtHours);
        });
        correctionButton.addActionListener(e -> {
            dialog.dispose();
            showCorrectionRequestDialog(date, suggestedCorrectionType(data.attendanceByDate.get(date)));
        });

        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private String buildDayContextText(LocalDate date, CalendarData data, double suggestedOtHours) {
        List<String> lines = new ArrayList<>();
        EmployeeSchedule schedule = data.schedulesByDate.get(date);
        AttendanceRecord attendance = data.attendanceByDate.get(date);
        Leave leave = data.leavesByDate.get(date);
        OvertimeRequest overtime = data.overtimeByDate.get(date);
        AttendanceAdjustment adjustment = data.adjustmentsByDate.get(date);

        lines.add("Schedule: " + (schedule == null ? "No schedule assigned" : getScheduleLabel(schedule, data)));
        lines.add("Attendance: " + (attendance == null
                ? "No time entry"
                : safeText(attendance.getTimeIn(), "--:--") + " - " + safeText(attendance.getTimeOut(), "--:--")));
        if (leave != null) {
            lines.add("Leave: " + formatLeaveStatus(leave));
        }
        if (overtime != null) {
            lines.add("Overtime: " + overtime.getOvertimeHours() + "h " + safeText(overtime.getStatus(), "Pending"));
        } else if (suggestedOtHours > 0) {
            lines.add("Potential OT: " + String.format(Locale.ENGLISH, "%.2f hour(s)", suggestedOtHours));
        }
        if (adjustment != null) {
            lines.add("Correction: " + safeText(adjustment.getStatus(), "Pending"));
        }
        return String.join("<br>", lines);
    }

    private String suggestedCorrectionType(AttendanceRecord attendance) {
        if (attendance == null) {
            return "Missing Time In";
        }
        if (attendance.getTimeIn() == null || attendance.getTimeIn().isBlank()) {
            return "Missing Time In";
        }
        if (attendance.getTimeOut() == null || attendance.getTimeOut().isBlank()) {
            return "Missing Time Out";
        }
        return "Incorrect Time";
    }

    private Color getDayBackground(EmployeeSchedule schedule, Holiday holiday, Leave leave) {
        if (leave != null) {
            String status = safeText(leave.getStatus(), "").toUpperCase(Locale.ENGLISH);
            if ("APPROVED".equals(status)) return LEAVE_APPROVED;
            if ("REJECTED".equals(status)) return LEAVE_REJECTED;
            return LEAVE_PENDING;
        }

        if (holiday != null) return HOLIDAY;
        if (schedule != null && schedule.isRestDay()) return REST_DAY;
        if (schedule != null) return WORK_DAY;
        return Color.WHITE;
    }

    private void refreshOverview(CalendarData data) {
        if (overviewPanel == null) {
            return;
        }

        overviewPanel.removeAll();
        JPanel grid = new JPanel(new GridLayout(1, 4, 12, 0));
        grid.setOpaque(false);
        grid.add(createOverviewCard("Upcoming Schedule", getUpcomingScheduleText(data), UITheme.BLUE));
        grid.add(createOverviewCard("Recent Attendance", getRecentAttendanceText(data), UITheme.SUCCESS));
        grid.add(createOverviewCard("Workforce Activity", getWorkforceActivityText(data), UITheme.YELLOW));
        grid.add(createLegendCard());
        overviewPanel.add(grid, BorderLayout.CENTER);
        overviewPanel.revalidate();
        overviewPanel.repaint();
    }

    private JPanel createOverviewCard(String title, String value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UITheme.FONT_SMALL);
        titleLabel.setForeground(UITheme.TEXT_SECONDARY);

        JLabel valueLabel = new JLabel("<html><body style='width:190px'>" + value + "</body></html>");
        valueLabel.setFont(UITheme.FONT_BODY_BOLD);
        valueLabel.setForeground(accent);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createLegendCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                new EmptyBorder(10, 12, 10, 12)
        ));
        JLabel title = new JLabel("Calendar Legend");
        title.setFont(UITheme.FONT_SMALL);
        title.setForeground(UITheme.TEXT_SECONDARY);
        title.setAlignmentX(LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(6));
        card.add(createLegendLine("Work", UITheme.BLUE));
        card.add(createLegendLine("Leave", UITheme.SUCCESS));
        card.add(createLegendLine("Holiday", UITheme.YELLOW));
        card.add(createLegendLine("Review", UITheme.DANGER));
        return card;
    }

    private JLabel createLegendLine(String label, Color color) {
        JLabel line = new JLabel(label);
        line.setFont(UITheme.FONT_SMALL);
        line.setForeground(color);
        line.setAlignmentX(LEFT_ALIGNMENT);
        return line;
    }

    private String getUpcomingScheduleText(CalendarData data) {
        LocalDate today = LocalDate.now();
        LocalDate monthEnd = visibleMonth.atEndOfMonth();
        LocalDate cursor = today.isBefore(visibleMonth.atDay(1)) ? visibleMonth.atDay(1) : today;

        while (!cursor.isAfter(monthEnd)) {
            EmployeeSchedule schedule = data.schedulesByDate.get(cursor);
            if (schedule != null) {
                return cursor + "<br>" + getScheduleLabel(schedule, data);
            }
            cursor = cursor.plusDays(1);
        }
        return "No upcoming schedule<br>assigned this month.";
    }

    private String getRecentAttendanceText(CalendarData data) {
        AttendanceRecord latest = null;
        LocalDate latestDate = null;
        for (Map.Entry<LocalDate, AttendanceRecord> entry : data.attendanceByDate.entrySet()) {
            if (latestDate == null || entry.getKey().isAfter(latestDate)) {
                latestDate = entry.getKey();
                latest = entry.getValue();
            }
        }

        if (latest == null || latestDate == null) {
            return "No time entries<br>for this month yet.";
        }

        return latestDate + "<br>"
                + safeText(latest.getTimeIn(), "--:--") + " - " + safeText(latest.getTimeOut(), "--:--");
    }

    private String getWorkforceActivityText(CalendarData data) {
        List<String> activity = new ArrayList<>();
        long pendingLeaves = data.leavesByDate.values().stream()
                .filter(leave -> "PENDING".equalsIgnoreCase(safeText(leave.getStatus(), "")))
                .distinct()
                .count();
        long pendingOvertime = data.overtimeByDate.values().stream()
                .filter(overtime -> "PENDING".equalsIgnoreCase(safeText(overtime.getStatus(), "")))
                .count();
        long pendingCorrections = data.adjustmentsByDate.values().stream()
                .filter(adjustment -> !adjustment.isResolved())
                .count();

        if (pendingLeaves > 0) {
            activity.add(pendingLeaves + " pending leave");
        }
        if (pendingOvertime > 0) {
            activity.add(pendingOvertime + " pending overtime");
        }
        if (pendingCorrections > 0) {
            activity.add(pendingCorrections + " attendance correction");
        }
        if (activity.isEmpty()) {
            return "No open workforce<br>items this month.";
        }
        return String.join("<br>", activity);
    }

    private String getScheduleLabel(EmployeeSchedule schedule, CalendarData data) {
        if (schedule.isRestDay()) {
            return "Rest Day";
        }

        EmployeeShift shift = schedule.getShiftId() == null ? null : data.shiftsById.get(schedule.getShiftId());
        if (shift == null) {
            return "Assigned Shift";
        }
        return shift.getShiftName() + " " + shift.getStartTime() + "-" + shift.getEndTime();
    }

    private double calculateSuggestedOtHours(EmployeeSchedule schedule, AttendanceRecord attendance, CalendarData data) {
        if (schedule == null || schedule.isRestDay() || attendance == null
                || attendance.getTimeOut() == null || attendance.getTimeOut().isBlank()
                || schedule.getShiftId() == null) {
            return 0.0;
        }

        EmployeeShift shift = data.shiftsById.get(schedule.getShiftId());
        if (shift == null || shift.getEndTime() == null) {
            return 0.0;
        }

        try {
            LocalTime actualOut = LocalTime.parse(attendance.getTimeOut());
            LocalTime scheduledOut = shift.getEndTime();
            if (!actualOut.isAfter(scheduledOut)) {
                return 0.0;
            }
            long minutes = Duration.between(scheduledOut, actualOut).toMinutes();
            return minutes <= 0 ? 0.0 : Math.round((minutes / 60.0) * 100.0) / 100.0;
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    private JLabel createChip(String text, Color foreground, Color background) {
        JLabel label = new JLabel("<html><body style='width:112px'>" + safeText(text, "") + "</body></html>");
        label.setFont(UITheme.FONT_SMALL);
        label.setForeground(foreground);
        label.setOpaque(true);
        label.setBackground(background);
        label.setToolTipText(text);
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(3, 7, 3, 7)
        ));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private JLabel createMutedLine(String text) {
        JLabel label = new JLabel("<html><body style='width:118px'>" + text + "</body></html>");
        label.setFont(UITheme.FONT_SMALL);
        label.setForeground(UITheme.TEXT_SECONDARY);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private String formatLeaveStatus(Leave leave) {
        return safeText(leave.getStatus(), "Pending") + " Leave";
    }

    private Color getLeaveColor(Leave leave) {
        String status = safeText(leave.getStatus(), "").toUpperCase(Locale.ENGLISH);
        if ("APPROVED".equals(status)) return UITheme.SUCCESS;
        if ("REJECTED".equals(status)) return UITheme.DANGER;
        return UITheme.YELLOW;
    }

    private CalendarData loadCalendarData() {
        CalendarData data = new CalendarData();
        if (currentUser == null) {
            return data;
        }

        int employeeId = currentUser.getEmployeeId();
        LocalDate start = visibleMonth.atDay(1);
        LocalDate end = visibleMonth.atEndOfMonth();

        for (EmployeeSchedule schedule : scheduleService.getEmployeeSchedulesByDateRange(employeeId, start, end)) {
            if (schedule.getScheduleDate() != null) {
                data.schedulesByDate.put(schedule.getScheduleDate(), schedule);
            }
        }

        for (EmployeeShift shift : scheduleService.getEmployeeShifts()) {
            data.shiftsById.put(shift.getShiftId(), shift);
        }

        for (Holiday holiday : scheduleService.getHolidays()) {
            if (holiday.getHolidayDate() != null
                    && !holiday.getHolidayDate().isBefore(start)
                    && !holiday.getHolidayDate().isAfter(end)) {
                data.holidaysByDate.put(holiday.getHolidayDate(), holiday);
            }
        }

        for (Leave leave : leaveService.getLeavesByEmployee(employeeId)) {
            addLeaveDates(data, leave, start, end);
        }

        for (OvertimeRequest overtime : overtimeService.getRequestsByEmployee(employeeId)) {
            if (overtime.getOvertimeDate() != null
                    && !overtime.getOvertimeDate().isBefore(start)
                    && !overtime.getOvertimeDate().isAfter(end)) {
                data.overtimeByDate.put(overtime.getOvertimeDate(), overtime);
            }
        }

        for (AttendanceRecord attendance : attendanceService.getAttendanceHistory(employeeId)) {
            try {
                LocalDate date = LocalDate.parse(attendance.getDate());
                if (!date.isBefore(start) && !date.isAfter(end)) {
                    data.attendanceByDate.put(date, attendance);
                }
            } catch (Exception ignored) {
            }
        }

        for (AttendanceAdjustment adjustment : attendanceAdjustmentService.getRequestsByEmployee(employeeId)) {
            if (adjustment.getAttendanceDate() != null
                    && !adjustment.getAttendanceDate().isBefore(start)
                    && !adjustment.getAttendanceDate().isAfter(end)) {
                data.adjustmentsByDate.put(adjustment.getAttendanceDate(), adjustment);
            }
        }

        for (AttendanceAwareness awareness : attendanceAwarenessService.getEmployeeAwareness(employeeId, start, end)) {
            if (awareness.getDate() != null) {
                data.awarenessByDate.put(awareness.getDate(), awareness);
            }
        }

        return data;
    }

    private void addLeaveDates(CalendarData data, Leave leave, LocalDate monthStart, LocalDate monthEnd) {
        try {
            LocalDate start = LocalDate.parse(leave.getStartDate());
            LocalDate end = LocalDate.parse(leave.getEndDate());
            LocalDate cursor = start.isBefore(monthStart) ? monthStart : start;
            LocalDate cappedEnd = end.isAfter(monthEnd) ? monthEnd : end;

            while (!cursor.isAfter(cappedEnd)) {
                data.leavesByDate.put(cursor, leave);
                cursor = cursor.plusDays(1);
            }
        } catch (Exception ignored) {
        }
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Color getAwarenessColor(AttendanceAwareness awareness) {
        return switch (awareness.getSeverity()) {
            case CRITICAL -> UITheme.DANGER;
            case WARNING -> UITheme.YELLOW;
            case INFO -> UITheme.BLUE;
            default -> UITheme.SUCCESS;
        };
    }

    private Color getOvertimeColor(OvertimeRequest overtime) {
        String status = safeText(overtime.getStatus(), "").toUpperCase(Locale.ENGLISH);
        if ("APPROVED".equals(status)) return UITheme.SUCCESS;
        if ("REJECTED".equals(status)) return UITheme.DANGER;
        return UITheme.YELLOW;
    }

    private Color getAdjustmentColor(AttendanceAdjustment adjustment) {
        return adjustment != null && adjustment.isResolved() ? UITheme.SUCCESS : UITheme.YELLOW;
    }

    private static class CalendarData {
        private final Map<LocalDate, EmployeeSchedule> schedulesByDate = new HashMap<>();
        private final Map<LocalDate, Holiday> holidaysByDate = new HashMap<>();
        private final Map<LocalDate, Leave> leavesByDate = new HashMap<>();
        private final Map<LocalDate, OvertimeRequest> overtimeByDate = new HashMap<>();
        private final Map<LocalDate, AttendanceRecord> attendanceByDate = new HashMap<>();
        private final Map<LocalDate, AttendanceAdjustment> adjustmentsByDate = new HashMap<>();
        private final Map<LocalDate, AttendanceAwareness> awarenessByDate = new HashMap<>();
        private final Map<Integer, EmployeeShift> shiftsById = new HashMap<>();
    }
}
