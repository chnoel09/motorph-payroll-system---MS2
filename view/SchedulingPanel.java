package com.mycompany.oop.view;

import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.EmployeeSchedule;
import com.mycompany.oop.model.EmployeeShift;
import com.mycompany.oop.model.Holiday;
import com.mycompany.oop.model.ScheduleBatch;
import com.mycompany.oop.service.AuditService;
import com.mycompany.oop.service.EmployeeService;
import com.mycompany.oop.service.RoleAccessService;
import com.mycompany.oop.service.ScheduleService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SchedulingPanel extends JPanel implements RefreshablePanel {

    private final ScheduleService scheduleService;
    private final EmployeeService employeeService;
    private final AuditService auditService;
    private final RoleAccessService roleAccessService;
    private final Employee currentUser;
    private final boolean canManage;
    private final boolean canManageReferenceData;

    private JTable shiftTable;
    private JTable holidayTable;
    private JTable batchTable;
    private JTable scheduleTable;
    private JTable scheduleBoardTable;

    private JTextField shiftNameField;
    private JComboBox<String> shiftStartHourBox;
    private JComboBox<String> shiftStartMinuteBox;
    private JComboBox<String> shiftEndHourBox;
    private JComboBox<String> shiftEndMinuteBox;
    private JTextField shiftGraceField;
    private JTextField shiftEffectiveDateField;

    private JTextField holidayNameField;
    private JTextField holidayDateField;
    private JComboBox<String> holidayTypeBox;
    private JTextField holidayMultiplierField;
    private JLabel shiftMessageLabel;
    private JLabel holidayMessageLabel;
    private JLabel batchMessageLabel;

    private JTextField batchNameField;
    private JTextField batchStartField;
    private JComboBox<String> batchStatusBox;

    private JTextField boardStartField;
    private JTextField boardEndField;
    private JComboBox<ComboItem> quickShiftBox;
    private JComboBox<ComboItem> quickHolidayBox;
    private JCheckBox quickShiftApplyBox;
    private JCheckBox quickRestDayBox;
    private JCheckBox quickHolidayApplyBox;
    private JLabel quickAssignMessageLabel;
    private JLabel selectedCellCountLabel;
    private boolean boardEmployeeSortAscending = true;
    private boolean dataLoading;

    public SchedulingPanel() {
        this(null);
    }

    public SchedulingPanel(Employee currentUser) {
        this.scheduleService = new ScheduleService();
        this.employeeService = new EmployeeService();
        this.auditService = new AuditService();
        this.roleAccessService = new RoleAccessService();
        this.currentUser = currentUser;
        this.canManageReferenceData = roleAccessService.canManageScheduling(getCurrentRole());
        this.canManage = canManageReferenceData
                || (currentUser != null && employeeService.hasAssignedTeam(currentUser.getEmployeeId()));

        setLayout(new BorderLayout());
        setBackground(UITheme.BG);

        add(UITheme.createTitleBar("Workforce Schedule"), BorderLayout.NORTH);
        add(createContentPanel(), BorderLayout.CENTER);
    }

    private JPanel createContentPanel() {
        JPanel content = UITheme.createWorkspacePanel(new BorderLayout(0, 14));
        content.add(createContextPanel(), BorderLayout.NORTH);

        if (!scheduleService.isSchedulingSchemaAvailable()) {
            content.add(createSchemaFallbackPanel(), BorderLayout.CENTER);
            return content;
        }

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        if (canManageReferenceData) {
            JPanel setupSection = createHrScheduleSetupSection();
            setupSection.setAlignmentX(LEFT_ALIGNMENT);
            body.add(setupSection);
            body.add(Box.createVerticalStrut(14));
        }

        JPanel boardSection = createSchedulingBoardSection();
        boardSection.setAlignmentX(LEFT_ALIGNMENT);
        body.add(boardSection);

        content.add(body, BorderLayout.CENTER);
        return content;
    }

    private JPanel createContextPanel() {
        JPanel panel = new JPanel(new BorderLayout(14, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JLabel title = new JLabel("Workforce Schedule Operations");
        title.setFont(UITheme.FONT_BODY_BOLD);
        title.setForeground(UITheme.ACCENT);

        JLabel message = new JLabel(canManage
                ? (canManageReferenceData
                ? "Manage work periods, shift setup, holidays, and team schedule assignments."
                : "Coordinate assigned team schedules. Shift setup and holidays remain HR-governed.")
                : "Workforce schedule information is visible to authorized users.");
        message.setFont(UITheme.FONT_BODY);
        message.setForeground(UITheme.TEXT_SECONDARY);

        panel.add(title, BorderLayout.WEST);
        panel.add(message, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSchemaFallbackPanel() {
        JPanel panel = createSectionPanel(
                "Workforce schedule setup",
                scheduleService.getSchedulingSchemaStatusMessage()
        );

        JLabel label = new JLabel("No workforce schedule data loaded");
        label.setFont(UITheme.FONT_BODY_BOLD);
        label.setForeground(UITheme.TEXT_SECONDARY);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createShiftTemplatesSection() {
        JPanel panel = createSectionPanel("Shift Setup", "Reusable work shift definitions");
        panel.add(createShiftFormPanel(), BorderLayout.NORTH);

        shiftTable = createTable();
        shiftTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                populateShiftFields();
            }
        });

        JScrollPane scrollPane = UITheme.createTableScrollPane(shiftTable);
        scrollPane.setPreferredSize(new Dimension(0, 260));
        panel.add(scrollPane, BorderLayout.CENTER);
        refreshShiftTable();
        return panel;
    }

    private JPanel createHolidaysSection() {
        JPanel panel = createSectionPanel("Holiday Setup", "Holiday reference and pay multipliers");
        panel.add(createHolidayFormPanel(), BorderLayout.NORTH);

        holidayTable = createTable();
        holidayTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                populateHolidayFields();
            }
        });

        JScrollPane scrollPane = UITheme.createTableScrollPane(holidayTable);
        scrollPane.setPreferredSize(new Dimension(0, 260));
        panel.add(scrollPane, BorderLayout.CENTER);
        refreshHolidayTable();
        return panel;
    }

    private JPanel createScheduleBatchesSection() {
        JPanel panel = createSectionPanel(
                "Schedule Coverage",
                "Create the coverage window required behind schedule assignments. Current schema stores a start date only."
        );
        panel.add(createBatchFormPanel(), BorderLayout.NORTH);

        batchTable = createTable();
        JScrollPane scrollPane = UITheme.createTableScrollPane(batchTable);
        scrollPane.setPreferredSize(new Dimension(0, 180));
        panel.add(scrollPane, BorderLayout.CENTER);
        refreshBatchTable();
        return panel;
    }

    private JPanel createUpcomingSchedulesSection() {
        JPanel panel = createSectionPanel(
                "Schedule Records",
                "Read-only schedule records generated from the scheduling board."
        );

        scheduleTable = createTable();

        JScrollPane scrollPane = UITheme.createTableScrollPane(scheduleTable);
        scrollPane.setPreferredSize(new Dimension(0, 260));
        panel.add(scrollPane, BorderLayout.CENTER);
        refreshScheduleTable();
        return panel;
    }

    private JPanel createHrScheduleSetupSection() {
        JPanel section = createSectionPanel(
                "HR Schedule Setup",
                "Create shift templates, holidays, and schedule coverage before supervisors assign schedules."
        );
        JPanel grid = new JPanel(new GridLayout(1, 3, 14, 14));
        grid.setBackground(UITheme.BG);
        grid.add(createShiftTemplatesSection());
        grid.add(createHolidaysSection());
        grid.add(createScheduleBatchesSection());
        section.add(grid, BorderLayout.CENTER);
        return section;
    }

    private JPanel createShiftFormPanel() {
        JPanel form = createFormGrid();
        shiftNameField = addField(form, "Shift Name", 0);
        JPanel startTime = createTimeSelectorPanel(true);
        WorkforceFormToolkit.addRow(form, "Start Time", startTime, 1, "24-hour time, 15-minute increments.");
        JPanel endTime = createTimeSelectorPanel(false);
        WorkforceFormToolkit.addRow(form, "End Time", endTime, 2, "End time must be after start time.");
        shiftGraceField = addField(form, "Grace Minutes", 3);
        shiftEffectiveDateField = addDateFieldWithButtons(form, "Effective Date", 4, "Optional yyyy-MM-dd effective date.");

        JPanel buttons = createButtonRow(
                () -> saveShift(false),
                () -> saveShift(true),
                this::deleteShift,
                this::clearShiftFields
        );

        shiftMessageLabel = WorkforceFormToolkit.createInlineMessage();
        WorkforceFormToolkit.setInfo(shiftMessageLabel, "Select a row to update or remove an existing shift setup.");
        JPanel actionRow = createActionStatusRow(shiftMessageLabel, buttons);

        JPanel wrapper = WorkforceFormToolkit.createCompactFormCard(820);
        JPanel cardBody = WorkforceFormToolkit.getCompactFormBody(wrapper);
        cardBody.add(form, BorderLayout.CENTER);
        cardBody.add(actionRow, BorderLayout.SOUTH);
        setInputsEnabled(form, canManageReferenceData);
        buttons.setEnabled(canManageReferenceData);
        return wrapper;
    }

    private JPanel createHolidayFormPanel() {
        JPanel form = createFormGrid();
        holidayNameField = addField(form, "Holiday Name", 0);
        holidayDateField = addDateFieldWithButtons(form, "Holiday Date", 1, "Holiday date in yyyy-MM-dd format.");
        holidayTypeBox = WorkforceFormToolkit.addComboBox(
                form,
                "Type",
                new String[]{"Regular", "Special", "Company"},
                2
        );
        holidayMultiplierField = addField(form, "Multiplier", 3);

        JPanel buttons = createButtonRow(
                () -> saveHoliday(false),
                () -> saveHoliday(true),
                this::deleteHoliday,
                this::clearHolidayFields
        );

        holidayMessageLabel = WorkforceFormToolkit.createInlineMessage();
        WorkforceFormToolkit.setInfo(holidayMessageLabel, "Holiday dates must be unique in the current database.");
        JPanel actionRow = createActionStatusRow(holidayMessageLabel, buttons);

        JPanel wrapper = WorkforceFormToolkit.createCompactFormCard(820);
        JPanel cardBody = WorkforceFormToolkit.getCompactFormBody(wrapper);
        cardBody.add(form, BorderLayout.CENTER);
        cardBody.add(actionRow, BorderLayout.SOUTH);
        setInputsEnabled(form, canManageReferenceData);
        buttons.setEnabled(canManageReferenceData);
        return wrapper;
    }

    private JPanel createBatchFormPanel() {
        JPanel form = createFormGrid();
        batchNameField = addField(form, "Coverage Name", 0);
        batchStartField = addDateFieldWithButtons(form, "Coverage Start", 1, "Schedule coverage currently stores a start date only.");
        batchStatusBox = WorkforceFormToolkit.addComboBox(
                form,
                "Status",
                new String[]{"Draft", "Published", "Finalized"},
                2
        );

        JButton createButton = UITheme.createAccentButton("Create Coverage");
        createButton.setEnabled(canManageReferenceData);
        createButton.addActionListener(e -> createScheduleBatch());

        batchMessageLabel = WorkforceFormToolkit.createInlineMessage();
        WorkforceFormToolkit.setInfo(batchMessageLabel,
                "Create schedule coverage before assigning team members.");

        JPanel buttonRow = WorkforceFormToolkit.createButtonRow(FlowLayout.RIGHT);
        buttonRow.add(createButton);

        JPanel actionRow = createActionStatusRow(batchMessageLabel, buttonRow);
        JPanel wrapper = WorkforceFormToolkit.createCompactFormCard(820);
        JPanel cardBody = WorkforceFormToolkit.getCompactFormBody(wrapper);
        cardBody.add(form, BorderLayout.CENTER);
        cardBody.add(actionRow, BorderLayout.SOUTH);
        setInputsEnabled(form, canManageReferenceData);
        return wrapper;
    }

    private JPanel createSchedulingBoardSection() {
        JPanel panel = createSectionPanel(
                "Workforce Scheduling Board",
                canManage
                        ? "Select a date range, choose a shift, then assign it to team members. Published schedules appear in employee calendars."
                        : "Published team schedules visible for employee calendar synchronization."
        );
        panel.add(createBoardToolbar(), BorderLayout.NORTH);
        scheduleBoardTable = createTable();
        scheduleBoardTable.setCellSelectionEnabled(true);
        scheduleBoardTable.setRowSelectionAllowed(true);
        scheduleBoardTable.setColumnSelectionAllowed(true);
        scheduleBoardTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        scheduleBoardTable.setRowHeight(38);
        scheduleBoardTable.setDefaultRenderer(Object.class, new ScheduleBoardCellRenderer());
        scheduleBoardTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectScheduleFromBoard();
            }
        });
        scheduleBoardTable.getColumnModel().getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectScheduleFromBoard();
            }
        });
        scheduleBoardTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = scheduleBoardTable.rowAtPoint(e.getPoint());
                int column = scheduleBoardTable.columnAtPoint(e.getPoint());
                if (row >= 0 && column == 0) {
                    scheduleBoardTable.setRowSelectionInterval(row, row);
                    scheduleBoardTable.setColumnSelectionInterval(1, Math.max(1, scheduleBoardTable.getColumnCount() - 1));
                    selectScheduleFromBoard();
                }
            }
        });
        JTableHeader header = scheduleBoardTable.getTableHeader();
        header.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int column = scheduleBoardTable.columnAtPoint(e.getPoint());
                if (column == 0) {
                    boardEmployeeSortAscending = !boardEmployeeSortAscending;
                    refreshScheduleBoard();
                }
            }
        });
        JScrollPane scrollPane = UITheme.createTableScrollPane(scheduleBoardTable);
        scrollPane.setPreferredSize(new Dimension(0, 360));
        panel.add(scrollPane, BorderLayout.CENTER);
        refreshScheduleBoard();
        return panel;
    }

    private JPanel createBoardToolbar() {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBorder(new EmptyBorder(0, 0, 10, 0));

        JPanel controlGrid = new JPanel(new GridLayout(1, 2, 14, 0));
        controlGrid.setOpaque(false);

        JPanel rangePanel = new JPanel();
        rangePanel.setBackground(Color.WHITE);
        rangePanel.setLayout(new BoxLayout(rangePanel, BoxLayout.Y_AXIS));
        rangePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel rangeTitle = new JLabel("DATE RANGE & NAVIGATION");
        rangeTitle.setFont(UITheme.FONT_BODY_BOLD);
        rangeTitle.setForeground(UITheme.TEXT_PRIMARY);

        JPanel navRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        navRow.setOpaque(false);
        JButton previousButton = UITheme.createButton("Previous Week");
        JButton todayButton = UITheme.createButton("Today");
        JButton nextButton = UITheme.createButton("Next Week");
        JButton refreshButton = UITheme.createButton("Refresh Board");
        navRow.add(previousButton);
        navRow.add(todayButton);
        navRow.add(nextButton);
        navRow.add(refreshButton);

        JPanel rangeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        rangeRow.setOpaque(false);
        boardStartField = UITheme.createDateField();
        boardStartField.setText(LocalDate.now().toString());
        WorkforceFormToolkit.applyDateHelp(boardStartField);
        boardEndField = UITheme.createDateField();
        boardEndField.setText(LocalDate.now().plusDays(13).toString());
        WorkforceFormToolkit.applyDateHelp(boardEndField);

        previousButton.addActionListener(e -> shiftBoardRange(-7));
        todayButton.addActionListener(e -> {
            LocalDate today = LocalDate.now();
            boardStartField.setText(today.toString());
            boardEndField.setText(today.plusDays(13).toString());
            refreshScheduleBoard();
        });
        nextButton.addActionListener(e -> shiftBoardRange(7));
        refreshButton.addActionListener(e -> refreshScheduleBoard());
        boardStartField.addActionListener(e -> refreshScheduleBoard());
        boardEndField.addActionListener(e -> refreshScheduleBoard());

        rangeRow.add(new JLabel("From:"));
        rangeRow.add(boardStartField);
        rangeRow.add(new JLabel("To:"));
        rangeRow.add(boardEndField);

        selectedCellCountLabel = new JLabel("Selected: 0 cells");
        selectedCellCountLabel.setFont(UITheme.FONT_BODY_BOLD);
        selectedCellCountLabel.setForeground(new Color(22, 101, 52));
        selectedCellCountLabel.setOpaque(true);
        selectedCellCountLabel.setBackground(new Color(220, 252, 231));
        selectedCellCountLabel.setBorder(new EmptyBorder(6, 10, 6, 10));

        JPanel countRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        countRow.setOpaque(false);
        countRow.add(selectedCellCountLabel);

        rangePanel.add(rangeTitle);
        rangePanel.add(Box.createVerticalStrut(12));
        rangePanel.add(navRow);
        rangePanel.add(Box.createVerticalStrut(14));
        rangePanel.add(rangeRow);
        rangePanel.add(Box.createVerticalStrut(14));
        rangePanel.add(countRow);

        JPanel applyPanel = new JPanel(new BorderLayout(16, 0));
        applyPanel.setBackground(Color.WHITE);
        applyPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JPanel applyFields = new JPanel(new GridBagLayout());
        applyFields.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 0, 6, 12);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel actionTitle = new JLabel("APPLY TO SELECTED CELLS");
        actionTitle.setFont(UITheme.FONT_BODY_BOLD);
        actionTitle.setForeground(UITheme.TEXT_PRIMARY);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        applyFields.add(actionTitle, gbc);

        quickShiftBox = new JComboBox<>(getShiftItems(true));
        quickHolidayBox = new JComboBox<>(getHolidayItems(true));
        WorkforceFormToolkit.styleComboBox(quickShiftBox);
        WorkforceFormToolkit.styleComboBox(quickHolidayBox);
        quickShiftBox.setPreferredSize(new Dimension(230, 34));
        quickHolidayBox.setPreferredSize(new Dimension(190, 34));
        quickShiftApplyBox = new JCheckBox("Apply Shift", true);
        quickRestDayBox = new JCheckBox("Apply Rest Day");
        quickHolidayApplyBox = new JCheckBox("Mark as Holiday Schedule");
        for (JCheckBox box : new JCheckBox[]{quickShiftApplyBox, quickRestDayBox, quickHolidayApplyBox}) {
            box.setOpaque(false);
            box.setFont(UITheme.FONT_BODY_BOLD);
            box.setEnabled(canManage);
        }
        quickHolidayApplyBox.setEnabled(canManageReferenceData);
        quickShiftApplyBox.addActionListener(e -> syncApplyOptionMode());
        quickRestDayBox.addActionListener(e -> syncApplyOptionMode());
        quickHolidayApplyBox.addActionListener(e -> syncApplyOptionMode());
        quickShiftBox.setEnabled(canManage);
        quickHolidayBox.setEnabled(false);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        applyFields.add(quickShiftApplyBox, gbc);
        gbc.gridx = 1;
        applyFields.add(new JLabel("Shift:"), gbc);
        gbc.gridx = 2;
        gbc.gridwidth = 2;
        applyFields.add(quickShiftBox, gbc);

        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        applyFields.add(quickRestDayBox, gbc);

        gbc.gridy = 3;
        gbc.gridx = 0;
        applyFields.add(quickHolidayApplyBox, gbc);
        gbc.gridx = 1;
        applyFields.add(new JLabel("Holiday:"), gbc);
        gbc.gridx = 2;
        gbc.gridwidth = 2;
        applyFields.add(quickHolidayBox, gbc);

        quickAssignMessageLabel = WorkforceFormToolkit.createInlineMessage();
        WorkforceFormToolkit.setInfo(quickAssignMessageLabel,
                canManage
                        ? (canManageReferenceData
                        ? "Select one or more cells in the board, choose what to apply, then click Apply Changes."
                        : "Select cells, choose a shift or rest day, then apply. Holiday schedules are HR-governed.")
                        : "Schedule assignment is restricted to HR or assigned supervisors.");
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 4;
        applyFields.add(quickAssignMessageLabel, gbc);

        JPanel actionButtons = WorkforceFormToolkit.createButtonRow(FlowLayout.RIGHT);
        JButton applyButton = UITheme.createAccentButton("Apply Changes");
        JButton clearButton = UITheme.createButton("Clear Selection");
        applyButton.addActionListener(e -> applyQuickAssignment());
        clearButton.addActionListener(e -> clearBoardSelection());
        applyButton.setEnabled(canManage);
        clearButton.setEnabled(true);
        actionButtons.add(applyButton);
        actionButtons.add(clearButton);

        applyPanel.add(applyFields, BorderLayout.CENTER);
        applyPanel.add(actionButtons, BorderLayout.EAST);

        controlGrid.add(rangePanel);
        controlGrid.add(applyPanel);

        wrapper.add(controlGrid);
        syncApplyOptionMode();
        return wrapper;
    }

    private void syncApplyOptionMode() {
        boolean restDay = quickRestDayBox != null && quickRestDayBox.isSelected();
        boolean applyShift = quickShiftApplyBox != null && quickShiftApplyBox.isSelected();
        boolean applyHoliday = quickHolidayApplyBox != null && quickHolidayApplyBox.isSelected();
        if (restDay && quickShiftApplyBox != null) {
            quickShiftApplyBox.setSelected(false);
            applyShift = false;
        }
        if (applyShift && quickRestDayBox != null) {
            quickRestDayBox.setSelected(false);
            restDay = false;
        }
        if (quickShiftBox != null) {
            quickShiftBox.setEnabled(canManage && applyShift && !restDay);
        }
        if (quickHolidayBox != null) {
            quickHolidayBox.setEnabled(canManageReferenceData && applyHoliday);
        }
        if (quickAssignMessageLabel != null) {
            WorkforceFormToolkit.setInfo(quickAssignMessageLabel,
                    restDay
                            ? "Rest day will be assigned to selected cells."
                            : (canManageReferenceData
                            ? "Holiday dates are created by HR. Select cells, choose what to apply, then click Apply Changes."
                            : "Select cells, choose a shift or rest day, then apply. Holiday schedules are HR-governed."));
        }
    }

    private void clearBoardSelection() {
        if (scheduleBoardTable != null) {
            scheduleBoardTable.clearSelection();
        }
        updateSelectedCellCount();
    }

    private JPanel createFormGrid() {
        return WorkforceFormToolkit.createFormGrid();
    }

    private JTextField addField(JPanel form, String label, int row) {
        return WorkforceFormToolkit.addTextField(form, label, row);
    }

    private JTextField addField(JPanel form, String label, int row, String helpText) {
        return WorkforceFormToolkit.addTextField(form, label, row, helpText);
    }

    private JTextField addDateFieldWithButtons(JPanel form, String label, int row, String helpText) {
        JTextField field = UITheme.createDateField();
        WorkforceFormToolkit.applyDateHelp(field);
        JPanel wrapper = new JPanel(new BorderLayout(8, 0));
        wrapper.setOpaque(false);
        wrapper.add(field, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttons.setOpaque(false);
        JButton previous = UITheme.createButton("<");
        JButton today = UITheme.createButton("Today");
        JButton next = UITheme.createButton(">");
        previous.setToolTipText("Previous day");
        today.setToolTipText("Use today's date");
        next.setToolTipText("Next day");
        previous.addActionListener(e -> shiftDateField(field, -1));
        today.addActionListener(e -> field.setText(LocalDate.now().toString()));
        next.addActionListener(e -> shiftDateField(field, 1));
        buttons.add(previous);
        buttons.add(today);
        buttons.add(next);
        wrapper.add(buttons, BorderLayout.EAST);

        WorkforceFormToolkit.addRow(form, label, wrapper, row, helpText);
        return field;
    }

    private JPanel createTimeSelectorPanel(boolean start) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        panel.setOpaque(false);
        JComboBox<String> hourBox = new JComboBox<>(hourOptions());
        JComboBox<String> minuteBox = new JComboBox<>(minuteOptions());
        WorkforceFormToolkit.styleComboBox(hourBox);
        WorkforceFormToolkit.styleComboBox(minuteBox);
        hourBox.setPreferredSize(new Dimension(72, 34));
        minuteBox.setPreferredSize(new Dimension(72, 34));
        panel.add(hourBox);
        panel.add(new JLabel(":"));
        panel.add(minuteBox);

        if (start) {
            shiftStartHourBox = hourBox;
            shiftStartMinuteBox = minuteBox;
            selectTime(shiftStartHourBox, shiftStartMinuteBox, LocalTime.of(8, 0));
        } else {
            shiftEndHourBox = hourBox;
            shiftEndMinuteBox = minuteBox;
            selectTime(shiftEndHourBox, shiftEndMinuteBox, LocalTime.of(17, 0));
        }
        return panel;
    }

    private String[] hourOptions() {
        String[] values = new String[24];
        for (int i = 0; i < values.length; i++) {
            values[i] = String.format("%02d", i);
        }
        return values;
    }

    private String[] minuteOptions() {
        return new String[]{"00", "15", "30", "45"};
    }

    private void shiftDateField(JTextField field, int days) {
        LocalDate date;
        try {
            date = field.getText() == null || field.getText().isBlank()
                    ? LocalDate.now()
                    : LocalDate.parse(field.getText().trim());
        } catch (DateTimeParseException ex) {
            date = LocalDate.now();
        }
        field.setText(date.plusDays(days).toString());
    }

    private JPanel createButtonRow(Runnable addAction, Runnable updateAction, Runnable deleteAction, Runnable clearAction) {
        JPanel row = WorkforceFormToolkit.createButtonRow(FlowLayout.RIGHT);

        JButton addButton = UITheme.createAccentButton("Add");
        JButton updateButton = UITheme.createButton("Update");
        JButton deleteButton = UITheme.createCrudDangerButton("Delete");
        JButton clearButton = UITheme.createButton("Clear");

        addButton.addActionListener(e -> addAction.run());
        updateButton.addActionListener(e -> updateAction.run());
        deleteButton.addActionListener(e -> deleteAction.run());
        clearButton.addActionListener(e -> clearAction.run());

        addButton.setEnabled(canManageReferenceData);
        updateButton.setEnabled(canManageReferenceData);
        deleteButton.setEnabled(canManageReferenceData);
        clearButton.setEnabled(canManageReferenceData);

        row.add(addButton);
        row.add(updateButton);
        row.add(deleteButton);
        row.add(clearButton);
        return row;
    }

    private JPanel createActionStatusRow(JLabel messageLabel, JPanel buttons) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.add(messageLabel, BorderLayout.CENTER);
        row.add(buttons, BorderLayout.EAST);
        return row;
    }

    private JTable createTable() {
        JTable table = new JTable();
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UITheme.styleTable(table);
        return table;
    }

    private void refreshShiftTable() {
        List<EmployeeShift> shifts = scheduleService.getEmployeeShifts();
        Object[][] rows = new Object[shifts.size()][7];

        for (int i = 0; i < shifts.size(); i++) {
            EmployeeShift shift = shifts.get(i);
            rows[i][0] = shift.getShiftId();
            rows[i][1] = shift.getShiftName();
            rows[i][2] = shift.getStartTime();
            rows[i][3] = shift.getEndTime();
            rows[i][4] = shift.getGraceMinutes();
            rows[i][5] = shift.getEffectiveDate();
            rows[i][6] = shift.getAssignedBy();
        }

        shiftTable.setModel(createReadOnlyModel(rows, new String[]{
                "ID", "Name", "Start", "End", "Grace", "Effective", "Assigned By"
        }));
        shiftTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        UITheme.setColumnWidths(shiftTable, 60, 180, 90, 90, 80, 120, 120);
    }

    private void refreshHolidayTable() {
        List<Holiday> holidays = scheduleService.getHolidays();
        Object[][] rows = new Object[holidays.size()][5];

        for (int i = 0; i < holidays.size(); i++) {
            Holiday holiday = holidays.get(i);
            rows[i][0] = holiday.getHolidayId();
            rows[i][1] = holiday.getHolidayName();
            rows[i][2] = holiday.getHolidayDate();
            rows[i][3] = holiday.getHolidayType();
            rows[i][4] = holiday.getMultiplier();
        }

        holidayTable.setModel(createReadOnlyModel(rows, new String[]{
                "ID", "Name", "Date", "Type", "Multiplier"
        }));
        holidayTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        UITheme.setColumnWidths(holidayTable, 60, 220, 120, 140, 100);
    }

    private void refreshBatchTable() {
        if (batchTable == null) {
            return;
        }

        List<ScheduleBatch> batches = scheduleService.getScheduleBatches();
        Object[][] rows = new Object[batches.size()][5];

        for (int i = 0; i < batches.size(); i++) {
            ScheduleBatch batch = batches.get(i);
            rows[i][0] = batch.getScheduleBatchId();
            rows[i][1] = formatBatchLabel(batch);
            rows[i][2] = batch.getScheduleMonth();
            rows[i][3] = batch.getStatus();
            rows[i][4] = batch.getUploadedBy();
        }

        batchTable.setModel(createReadOnlyModel(rows, new String[]{
                "ID", "Schedule Coverage", "Coverage Start", "Status", "Created By"
        }));
        batchTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        UITheme.setColumnWidths(batchTable, 70, 260, 130, 120, 120);
    }

    private void refreshScheduleTable() {
        if (scheduleTable == null) {
            return;
        }

        List<EmployeeSchedule> schedules = new ArrayList<>(getVisibleSchedules());
        schedules.sort(Comparator.comparing(EmployeeSchedule::getScheduleDate,
                Comparator.nullsLast(Comparator.naturalOrder())));

        Map<Integer, String> employeeNames = getEmployeeNameMap();
        Map<Integer, String> shiftNames = getShiftNameMap();
        Map<Integer, String> holidayNames = getHolidayNameMap();
        Object[][] rows = new Object[schedules.size()][7];

        for (int i = 0; i < schedules.size(); i++) {
            EmployeeSchedule schedule = schedules.get(i);
            rows[i][0] = schedule.getScheduleId();
            rows[i][1] = employeeNames.getOrDefault(schedule.getEmployeeId(), String.valueOf(schedule.getEmployeeId()));
            rows[i][2] = schedule.getScheduleDate();
            rows[i][3] = schedule.getShiftId() == null ? "No shift" : shiftNames.getOrDefault(schedule.getShiftId(), String.valueOf(schedule.getShiftId()));
            rows[i][4] = schedule.isRestDay() ? "Rest Day" : "Work Day";
            rows[i][5] = schedule.getHolidayId() == null ? "" : holidayNames.getOrDefault(schedule.getHolidayId(), String.valueOf(schedule.getHolidayId()));
            rows[i][6] = schedule.getStatus();
        }

        scheduleTable.setModel(createReadOnlyModel(rows, new String[]{
                "ID", "Employee", "Date", "Shift", "Schedule Type", "Holiday", "Status"
        }));
        scheduleTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        UITheme.setColumnWidths(scheduleTable, 70, 220, 120, 180, 150, 180, 120);
    }

    private void refreshScheduleBoard() {
        if (scheduleBoardTable == null) {
            return;
        }
        List<Employee> employees = getBoardEmployees();
        Map<Integer, String> shiftNames = getShiftNameMap();
        Map<String, EmployeeSchedule> schedulesByEmployeeDate = new HashMap<>();
        LocalDate start = getBoardStartDate();
        int dayCount = getBoardDayCount();
        LocalDate end = start.plusDays(dayCount - 1L);
        for (EmployeeSchedule schedule : scheduleService.getEmployeeSchedulesByDateRange(
                employees.stream().map(Employee::getEmployeeId).toList(), start, end)) {
            if (schedule.getScheduleDate() != null) {
                schedulesByEmployeeDate.put(schedule.getEmployeeId() + "|" + schedule.getScheduleDate(), schedule);
            }
        }

        String[] columns = new String[dayCount + 1];
        columns[0] = boardEmployeeSortAscending ? "Employee ↑" : "Employee ↓";
        for (int i = 0; i < dayCount; i++) {
            columns[i + 1] = formatBoardDateHeader(start.plusDays(i));
        }

        Object[][] rows = new Object[Math.max(1, employees.size())][dayCount + 1];
        if (employees.isEmpty()) {
            rows[0][0] = "No assigned team members";
            for (int i = 1; i <= dayCount; i++) {
                rows[0][i] = "-";
            }
        } else {
            for (int row = 0; row < employees.size(); row++) {
                Employee employee = employees.get(row);
                rows[row][0] = employee.getEmployeeId() + " - " + employee.getFirstName() + " " + employee.getLastName();
                for (int day = 0; day < dayCount; day++) {
                    LocalDate date = start.plusDays(day);
                    EmployeeSchedule schedule = schedulesByEmployeeDate.get(employee.getEmployeeId() + "|" + date);
                    rows[row][day + 1] = schedule == null
                            ? "Unassigned"
                            : boardCellLabel(schedule, shiftNames);
                }
            }
        }

        scheduleBoardTable.setModel(createReadOnlyModel(rows, columns));
        scheduleBoardTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = new int[dayCount + 1];
        widths[0] = 230;
        for (int i = 1; i < widths.length; i++) {
            widths[i] = 118;
        }
        UITheme.setColumnWidths(scheduleBoardTable, widths);
        updateSelectedCellCount();
    }

    private String boardCellLabel(EmployeeSchedule schedule, Map<Integer, String> shiftNames) {
        if (schedule.getHolidayId() != null) {
            return "HOL";
        }
        if (schedule.isRestDay()) {
            return "RD";
        }
        if (schedule.getShiftId() == null) {
            return "Unassigned";
        }
        return compactShiftLabel(shiftNames.getOrDefault(schedule.getShiftId(), "Shift #" + schedule.getShiftId()));
    }

    private String formatBoardDateHeader(LocalDate date) {
        String day = date.getDayOfWeek().toString().substring(0, 3);
        return date.getDayOfMonth() + " " + day;
    }

    private String compactShiftLabel(String label) {
        if (label == null || label.isBlank()) {
            return "SHIFT";
        }
        String cleaned = label
                .replaceAll("(?i)\\bshift\\b", "")
                .replaceAll("\\([^)]*\\)", "")
                .trim();
        if (cleaned.isBlank()) {
            cleaned = label.trim();
        }
        return cleaned.length() > 12
                ? cleaned.substring(0, 12).trim().toUpperCase()
                : cleaned.toUpperCase();
    }

    private void selectScheduleFromBoard() {
        int row = scheduleBoardTable == null ? -1 : scheduleBoardTable.getSelectedRow();
        int column = scheduleBoardTable == null ? -1 : scheduleBoardTable.getSelectedColumn();
        if (row < 0 || column <= 0) {
            return;
        }
        List<Employee> employees = getBoardEmployees();
        if (row >= employees.size()) {
            return;
        }
        Employee employee = employees.get(row);
        LocalDate date = getBoardStartDate().plusDays(column - 1L);
        EmployeeSchedule schedule = findScheduleByEmployeeAndDate(employee.getEmployeeId(), date);
        if (schedule != null && scheduleTable != null) {
            selectScheduleTableRow(schedule.getScheduleId());
        } else if (scheduleTable != null) {
            scheduleTable.clearSelection();
        }
        updateSelectedCellCount();
    }

    private LocalDate getBoardStartDate() {
        if (boardStartField == null || boardStartField.getText() == null || boardStartField.getText().isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(boardStartField.getText().trim());
        } catch (Exception ex) {
            WorkforceFormToolkit.setFieldError(boardStartField, "Use yyyy-MM-dd.");
            return LocalDate.now();
        }
    }

    private LocalDate getBoardEndDate(LocalDate start) {
        if (boardEndField == null || boardEndField.getText() == null || boardEndField.getText().isBlank()) {
            return start.plusDays(13);
        }
        try {
            LocalDate end = LocalDate.parse(boardEndField.getText().trim());
            if (end.isBefore(start)) {
                return start;
            }
            return end;
        } catch (Exception ex) {
            return start.plusDays(13);
        }
    }

    private int getBoardDayCount() {
        LocalDate start = getBoardStartDate();
        LocalDate end = getBoardEndDate(start);
        long days = ChronoUnit.DAYS.between(start, end) + 1L;
        return (int) Math.max(1, Math.min(31, days));
    }

    private void shiftBoardRange(int days) {
        LocalDate start = getBoardStartDate().plusDays(days);
        LocalDate end = getBoardEndDate(getBoardStartDate()).plusDays(days);
        boardStartField.setText(start.toString());
        boardEndField.setText(end.toString());
        refreshScheduleBoard();
    }

    private void selectScheduleTableRow(int scheduleId) {
        if (scheduleTable == null || scheduleId <= 0) {
            return;
        }
        for (int row = 0; row < scheduleTable.getRowCount(); row++) {
            Object value = scheduleTable.getValueAt(row, 0);
            if (value != null && String.valueOf(scheduleId).equals(value.toString())) {
                scheduleTable.setRowSelectionInterval(row, row);
                return;
            }
        }
    }

    private void updateSelectedCellCount() {
        if (selectedCellCountLabel != null) {
            int count = getSelectedBoardCells(false).size();
            selectedCellCountLabel.setText("Selected: " + count + " cell" + (count == 1 ? "" : "s"));
        }
    }

    private void applyQuickAssignment() {
        if (!canManage) {
            WorkforceFormToolkit.setError(quickAssignMessageLabel, "Schedule assignment is restricted.");
            return;
        }

        try {
            boolean applyShift = quickShiftApplyBox != null && quickShiftApplyBox.isSelected();
            boolean restDay = quickRestDayBox != null && quickRestDayBox.isSelected();
            boolean applyHoliday = quickHolidayApplyBox != null && quickHolidayApplyBox.isSelected();
            ComboItem shift = applyShift ? selectedOptionalItem(quickShiftBox) : null;
            ComboItem holiday = applyHoliday ? selectedOptionalItem(quickHolidayBox) : null;
            if (!applyShift && !restDay && !applyHoliday) {
                throw new IllegalArgumentException("Choose at least one apply option before applying changes.");
            }
            if (applyShift && quickShiftBox.getItemCount() <= 1) {
                throw new IllegalArgumentException("No shift templates are available. HR must create shift templates before supervisors can assign schedules.");
            }
            if (applyShift && shift == null) {
                throw new IllegalArgumentException("Choose a shift template before applying changes.");
            }
            if (applyHoliday && holiday == null) {
                throw new IllegalArgumentException("Choose a holiday before applying changes.");
            }

            List<BoardCell> cells = getSelectedBoardCells(false);
            if (cells.isEmpty()) {
                throw new IllegalArgumentException("Select one or more board cells before applying changes.");
            }
            if (!confirm("Apply schedule changes to " + cells.size() + " selected cell" + (cells.size() == 1 ? "" : "s") + "?")) {
                return;
            }

            int updated = 0;
            for (BoardCell cell : cells) {
                EmployeeSchedule existing = findScheduleByEmployeeAndDate(cell.employeeId, cell.date);
                int batchId = existing == null
                        ? resolvePublishingPeriodId(cell.date)
                        : existing.getScheduleBatchId();
                Integer finalShiftId = applyShift
                        ? shift.id
                        : (restDay ? null : existing == null ? null : existing.getShiftId());
                boolean finalRestDay = restDay || (existing != null && !applyShift && existing.isRestDay());
                Integer finalHolidayId = applyHoliday
                        ? holiday.id
                        : existing == null ? null : existing.getHolidayId();
                if (!finalRestDay && finalShiftId == null && existing == null) {
                    throw new IllegalArgumentException("Choose Apply Shift or Apply Rest Day for new schedule cells.");
                }
                EmployeeSchedule schedule = new EmployeeSchedule(
                        existing == null ? 0 : existing.getScheduleId(),
                        batchId,
                        cell.employeeId,
                        finalShiftId,
                        cell.date,
                        finalRestDay,
                        finalHolidayId,
                        "Assigned"
                );
                if (existing == null) {
                    scheduleService.assignEmployeeSchedule(schedule);
                    logAudit(AuditService.EMPLOYEE_SCHEDULE_ASSIGNED, "employee_schedules", cell.employeeId + ":" + cell.date);
                } else {
                    scheduleService.updateEmployeeSchedule(schedule);
                    logAudit(AuditService.EMPLOYEE_SCHEDULE_UPDATED, "employee_schedules", String.valueOf(existing.getScheduleId()));
                }
                updated++;
            }

            refreshScheduleBoard();
            refreshScheduleTable();
            WorkforceFormToolkit.setSuccess(quickAssignMessageLabel,
                    "Assigned " + updated + " schedule cell" + (updated == 1 ? "" : "s") + ". Employee calendars update after refresh.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            WorkforceFormToolkit.setError(quickAssignMessageLabel, ex.getMessage());
        }
    }

    private List<BoardCell> getSelectedBoardCells(boolean entireRowRange) {
        List<BoardCell> cells = new ArrayList<>();
        if (scheduleBoardTable == null) {
            return cells;
        }
        List<Employee> employees = getBoardEmployees();
        Set<String> uniqueKeys = new LinkedHashSet<>();
        int[] rows = scheduleBoardTable.getSelectedRows();
        int[] columns = entireRowRange
                ? boardDateColumns()
                : scheduleBoardTable.getSelectedColumns();
        for (int row : rows) {
            if (row < 0 || row >= employees.size()) {
                continue;
            }
            int employeeId = employees.get(row).getEmployeeId();
            for (int column : columns) {
                if (column <= 0) {
                    continue;
                }
                LocalDate date = getBoardStartDate().plusDays(column - 1L);
                String key = employeeId + "|" + date;
                if (uniqueKeys.add(key)) {
                    cells.add(new BoardCell(employeeId, date));
                }
            }
        }
        return cells;
    }

    private int[] boardDateColumns() {
        int columnCount = scheduleBoardTable == null ? 0 : scheduleBoardTable.getColumnCount();
        int[] columns = new int[Math.max(0, columnCount - 1)];
        for (int i = 1; i < columnCount; i++) {
            columns[i - 1] = i;
        }
        return columns;
    }

    private int resolvePublishingPeriodId(LocalDate date) {
        List<ScheduleBatch> batches = scheduleService.getScheduleBatches();
        if (batches.isEmpty()) {
            throw new IllegalArgumentException("HR must create schedule coverage before schedules can be assigned.");
        }
        for (ScheduleBatch batch : batches) {
            LocalDate period = batch.getScheduleMonth();
            if (period != null
                    && period.getYear() == date.getYear()
                    && period.getMonth() == date.getMonth()) {
                return batch.getScheduleBatchId();
            }
        }
        return batches.get(0).getScheduleBatchId();
    }

    private DefaultTableModel createReadOnlyModel(Object[][] rows, String[] columns) {
        return new DefaultTableModel(rows, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void saveShift(boolean update) {
        if (!canManageReferenceData) {
            showInfo("Access restricted", "Only HR can manage shift setup.");
            return;
        }

        try {
            clearShiftFieldStates();
            int shiftId = update ? getSelectedId(shiftTable, "Select a shift setup to update.") : 0;
            EmployeeShift shift = new EmployeeShift(
                    shiftId,
                    requiredText(shiftNameField, "Shift name is required."),
                    selectedTime(shiftStartHourBox, shiftStartMinuteBox),
                    selectedTime(shiftEndHourBox, shiftEndMinuteBox),
                    nonNegativeInt(shiftGraceField, "Grace minutes cannot be negative."),
                    optionalDate(shiftEffectiveDateField),
                    getCurrentUserId()
            );
            if (!shift.getEndTime().isAfter(shift.getStartTime())) {
                throw new IllegalArgumentException("End time must be after start time. Overnight shifts are not supported yet.");
            }

            if (update) {
                scheduleService.updateEmployeeShift(shift);
                logAudit(AuditService.SHIFT_UPDATED, "employee_shifts", String.valueOf(shiftId));
                WorkforceFormToolkit.setSuccess(shiftMessageLabel, "Shift setup updated.");
            } else {
                scheduleService.createEmployeeShift(shift);
                logAudit(AuditService.SHIFT_CREATED, "employee_shifts", shift.getShiftName());
                WorkforceFormToolkit.setSuccess(shiftMessageLabel, "Shift setup created.");
            }

            refreshShiftTable();
            refreshAssignmentSelectors();
            clearShiftFields();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showShiftError(ex.getMessage());
        }
    }

    private void deleteShift() {
        if (!canManageReferenceData) {
            showInfo("Access restricted", "Only HR can manage shift setup.");
            return;
        }

        try {
            int shiftId = getSelectedId(shiftTable, "Select a shift setup to delete.");
            if (!confirm("Delete selected shift setup?")) {
                return;
            }

            scheduleService.deleteEmployeeShift(shiftId);
            logAudit(AuditService.SHIFT_DELETED, "employee_shifts", String.valueOf(shiftId));
            refreshShiftTable();
            refreshAssignmentSelectors();
            clearShiftFields();
            WorkforceFormToolkit.setSuccess(shiftMessageLabel, "Shift setup deleted.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showShiftError(ex.getMessage());
        }
    }

    private void saveHoliday(boolean update) {
        if (!canManageReferenceData) {
            showInfo("Access restricted", "Only HR can manage holidays.");
            return;
        }

        try {
            clearHolidayFieldStates();
            int holidayId = update ? getSelectedId(holidayTable, "Select a holiday to update.") : 0;
            Holiday holiday = new Holiday(
                    holidayId,
                    requiredText(holidayNameField, "Holiday name is required."),
                    requiredDate(holidayDateField, "Holiday date is required."),
                    selectedHolidayType(),
                    positiveDouble(holidayMultiplierField, "Multiplier must be positive.")
            );

            if (update) {
                scheduleService.updateHoliday(holiday);
                logAudit(AuditService.HOLIDAY_UPDATED, "holidays", String.valueOf(holidayId));
                WorkforceFormToolkit.setSuccess(holidayMessageLabel, "Holiday updated.");
            } else {
                scheduleService.createHoliday(holiday);
                logAudit(AuditService.HOLIDAY_CREATED, "holidays", holiday.getHolidayName());
                WorkforceFormToolkit.setSuccess(holidayMessageLabel, "Holiday created.");
            }

            refreshHolidayTable();
            refreshAssignmentSelectors();
            clearHolidayFields();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showHolidayError(ex.getMessage());
        }
    }

    private void deleteHoliday() {
        if (!canManageReferenceData) {
            showInfo("Access restricted", "Only HR can manage holidays.");
            return;
        }

        try {
            int holidayId = getSelectedId(holidayTable, "Select a holiday to delete.");
            if (!confirm("Delete selected holiday?")) {
                return;
            }

            scheduleService.deleteHoliday(holidayId);
            logAudit(AuditService.HOLIDAY_DELETED, "holidays", String.valueOf(holidayId));
            refreshHolidayTable();
            refreshAssignmentSelectors();
            clearHolidayFields();
            WorkforceFormToolkit.setSuccess(holidayMessageLabel, "Holiday deleted.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showHolidayError(ex.getMessage());
        }
    }

    private void createScheduleBatch() {
        if (!canManageReferenceData) {
            WorkforceFormToolkit.setError(batchMessageLabel, "Only HR can create schedule coverage.");
            return;
        }

        try {
            WorkforceFormToolkit.setFieldNormal(batchNameField);
            WorkforceFormToolkit.setFieldNormal(batchStartField);

            String batchName = requiredText(batchNameField, "Schedule coverage name is required.");
            LocalDate periodStart = requiredDate(batchStartField, "Coverage start is required.");
            if (currentUser == null) {
                throw new IllegalArgumentException("Current user is required to create schedule coverage.");
            }
            ScheduleBatch batch = new ScheduleBatch(
                    0,
                    null,
                    currentUser.getEmployeeId(),
                    periodStart,
                    selectedText(batchStatusBox, "Schedule period status is required."),
                    LocalDateTime.now(),
                    null,
                    null
            );

            scheduleService.createScheduleBatch(batch);
            logAudit(AuditService.SCHEDULE_BATCH_CREATED, "schedule_windows", batchName);
            WorkforceFormToolkit.setSuccess(batchMessageLabel, "Schedule coverage created.");
            batchNameField.setText("");
            batchStartField.setText("");
            batchStatusBox.setSelectedIndex(0);
            refreshBatchTable();
            refreshAssignmentSelectors();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            WorkforceFormToolkit.setError(batchMessageLabel, ex.getMessage());
        }
    }

    private void populateShiftFields() {
        int row = shiftTable.getSelectedRow();
        if (row < 0) {
            return;
        }

        shiftNameField.setText(valueAt(shiftTable, row, 1));
        selectTime(shiftStartHourBox, shiftStartMinuteBox, parseTimeOrDefault(valueAt(shiftTable, row, 2), LocalTime.of(8, 0)));
        selectTime(shiftEndHourBox, shiftEndMinuteBox, parseTimeOrDefault(valueAt(shiftTable, row, 3), LocalTime.of(17, 0)));
        shiftGraceField.setText(valueAt(shiftTable, row, 4));
        shiftEffectiveDateField.setText(valueAt(shiftTable, row, 5));
    }

    private void populateHolidayFields() {
        int row = holidayTable.getSelectedRow();
        if (row < 0) {
            return;
        }

        holidayNameField.setText(valueAt(holidayTable, row, 1));
        holidayDateField.setText(valueAt(holidayTable, row, 2));
        holidayTypeBox.setSelectedItem(valueAt(holidayTable, row, 3));
        holidayMultiplierField.setText(valueAt(holidayTable, row, 4));
    }

    private void clearShiftFields() {
        shiftTable.clearSelection();
        clearShiftFieldStates();
        shiftNameField.setText("");
        selectTime(shiftStartHourBox, shiftStartMinuteBox, LocalTime.of(8, 0));
        selectTime(shiftEndHourBox, shiftEndMinuteBox, LocalTime.of(17, 0));
        shiftGraceField.setText("0");
        shiftEffectiveDateField.setText("");
    }

    private void clearHolidayFields() {
        holidayTable.clearSelection();
        clearHolidayFieldStates();
        holidayNameField.setText("");
        holidayDateField.setText("");
        holidayTypeBox.setSelectedIndex(0);
        holidayMultiplierField.setText("");
    }

    private void refreshAssignmentSelectors() {
        if (quickShiftBox != null) {
            quickShiftBox.setModel(new DefaultComboBoxModel<>(getShiftItems(true)));
        }
        if (quickHolidayBox != null) {
            quickHolidayBox.setModel(new DefaultComboBoxModel<>(getHolidayItems(true)));
        }
    }

    @Override
    public void refreshData() {
        if (dataLoading) {
            return;
        }
        if (!scheduleService.isSchedulingSchemaAvailable()) {
            return;
        }
        dataLoading = true;
        showScheduleLoadingState();
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private long startedAtMs;

            @Override
            protected Void doInBackground() {
                startedAtMs = System.currentTimeMillis();
                return null;
            }

            @Override
            protected void done() {
                try {
                    if (shiftTable != null) refreshShiftTable();
                    if (holidayTable != null) refreshHolidayTable();
                    if (batchTable != null) refreshBatchTable();
                    refreshScheduleTable();
                    refreshScheduleBoard();
                    refreshAssignmentSelectors();
                    System.out.println("[perf] SchedulingPanel refreshData took "
                            + (System.currentTimeMillis() - startedAtMs) + " ms");
                } finally {
                    dataLoading = false;
                }
            }
        };
        worker.execute();
    }

    private void showScheduleLoadingState() {
        if (shiftTable != null) {
            shiftTable.setModel(createReadOnlyModel(new Object[][]{{"", "Loading shift setup...", "", "", "", "", ""}},
                    new String[]{"ID", "Name", "Start", "End", "Grace", "Effective", "Assigned By"}));
        }
        if (holidayTable != null) {
            holidayTable.setModel(createReadOnlyModel(new Object[][]{{"", "Loading holiday setup...", "", "", ""}},
                    new String[]{"ID", "Name", "Date", "Type", "Multiplier"}));
        }
        if (batchTable != null) {
            batchTable.setModel(createReadOnlyModel(new Object[][]{{"", "Loading schedule coverage...", "", "", ""}},
                    new String[]{"ID", "Schedule Coverage", "Coverage Start", "Status", "Created By"}));
        }
        if (scheduleTable != null) {
            scheduleTable.setModel(createReadOnlyModel(new Object[][]{{"", "Loading schedule records...", "", "", "", "", ""}},
                    new String[]{"ID", "Employee", "Date", "Shift", "Schedule Type", "Holiday", "Status"}));
        }
        if (scheduleBoardTable != null) {
            scheduleBoardTable.setModel(createReadOnlyModel(new Object[][]{{"Loading workforce schedule board..."}},
                    new String[]{"Workforce Schedule"}));
        }
    }

    private String valueAt(JTable table, int row, int column) {
        Object value = table.getValueAt(row, column);
        return value == null ? "" : value.toString();
    }

    private ComboItem[] getEmployeeItems() {
        List<Employee> employees = getVisibleEmployees();
        ComboItem[] items = new ComboItem[employees.size() + 1];
        items[0] = new ComboItem(0, "Select employee");

        for (int i = 0; i < employees.size(); i++) {
            Employee employee = employees.get(i);
            items[i + 1] = new ComboItem(
                    employee.getEmployeeId(),
                    employee.getEmployeeId() + " - " + employee.getFirstName() + " " + employee.getLastName()
            );
        }

        return items;
    }

    private ComboItem[] getBatchItems() {
        List<ScheduleBatch> batches = scheduleService.getScheduleBatches();
        ComboItem[] items = new ComboItem[batches.size() + 1];
        items[0] = new ComboItem(0, "Select schedule coverage");

        for (int i = 0; i < batches.size(); i++) {
            ScheduleBatch batch = batches.get(i);
            items[i + 1] = new ComboItem(batch.getScheduleBatchId(), formatBatchLabel(batch));
        }

        return items;
    }

    private ComboItem[] getShiftItems(boolean includeBlank) {
        List<EmployeeShift> shifts = scheduleService.getEmployeeShifts();
        ComboItem[] items = new ComboItem[shifts.size() + (includeBlank ? 1 : 0)];
        int index = 0;

        if (includeBlank) {
            items[index++] = new ComboItem(0, "Select shift or keep as rest day");
        }

        for (EmployeeShift shift : shifts) {
            items[index++] = new ComboItem(
                    shift.getShiftId(),
                    shift.getShiftName() + " (" + shift.getStartTime() + "-" + shift.getEndTime() + ")"
            );
        }

        return items;
    }

    private ComboItem[] getHolidayItems(boolean includeBlank) {
        List<Holiday> holidays = scheduleService.getHolidays();
        ComboItem[] items = new ComboItem[holidays.size() + (includeBlank ? 1 : 0)];
        int index = 0;

        if (includeBlank) {
            items[index++] = new ComboItem(0, "No holiday");
        }

        for (Holiday holiday : holidays) {
            items[index++] = new ComboItem(
                    holiday.getHolidayId(),
                    holiday.getHolidayDate() + " - " + holiday.getHolidayName()
            );
        }

        return items;
    }

    private Map<Integer, String> getEmployeeNameMap() {
        Map<Integer, String> names = new HashMap<>();
        for (Employee employee : getVisibleEmployees()) {
            names.put(employee.getEmployeeId(), employee.getFirstName() + " " + employee.getLastName());
        }
        return names;
    }

    private Map<Integer, String> getShiftNameMap() {
        Map<Integer, String> names = new HashMap<>();
        for (EmployeeShift shift : scheduleService.getEmployeeShifts()) {
            names.put(shift.getShiftId(), shift.getShiftName());
        }
        return names;
    }

    private Map<Integer, String> getHolidayNameMap() {
        Map<Integer, String> names = new HashMap<>();
        for (Holiday holiday : scheduleService.getHolidays()) {
            names.put(holiday.getHolidayId(), holiday.getHolidayName());
        }
        return names;
    }

    private String formatBatchLabel(ScheduleBatch batch) {
        return "Coverage " + batch.getScheduleBatchId() + " - " + batch.getScheduleMonth() + " (" + batch.getStatus() + ")";
    }

    private EmployeeSchedule findScheduleById(int scheduleId) {
        for (EmployeeSchedule schedule : getVisibleSchedules()) {
            if (schedule.getScheduleId() == scheduleId) {
                return schedule;
            }
        }
        return null;
    }

    private EmployeeSchedule findScheduleByEmployeeAndDate(int employeeId, LocalDate date) {
        if (date == null) {
            return null;
        }
        for (EmployeeSchedule schedule : getVisibleSchedules()) {
            if (schedule.getEmployeeId() == employeeId && date.equals(schedule.getScheduleDate())) {
                return schedule;
            }
        }
        return null;
    }

    private List<Employee> getVisibleEmployees() {
        if (currentUser == null) {
            return employeeService.getAllEmployees();
        }
        if (roleAccessService.canManageScheduling(getCurrentRole())) {
            return employeeService.getAllEmployees();
        }
        return employeeService.getTeamOperationsEmployees(currentUser);
    }

    private List<Employee> getBoardEmployees() {
        List<Employee> employees = new ArrayList<>(getVisibleEmployees());
        Comparator<Employee> comparator = Comparator
                .comparing(Employee::getLastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(Employee::getFirstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparingInt(Employee::getEmployeeId);
        if (!boardEmployeeSortAscending) {
            comparator = comparator.reversed();
        }
        employees.sort(comparator);
        return employees;
    }

    private List<EmployeeSchedule> getVisibleSchedules() {
        List<Employee> employees = getVisibleEmployees();
        Map<Integer, Boolean> visibleIds = new HashMap<>();
        for (Employee employee : employees) {
            visibleIds.put(employee.getEmployeeId(), true);
        }
        return new ArrayList<>(scheduleService.getAllEmployeeSchedules()
                .stream()
                .filter(schedule -> visibleIds.containsKey(schedule.getEmployeeId()))
                .toList());
    }

    private int getSelectedId(JTable table, String message) {
        int row = table.getSelectedRow();
        if (row < 0) {
            throw new IllegalArgumentException(message);
        }

        Object value = table.getValueAt(row, 0);
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException("Selected record ID is unavailable.");
        }

        return ((Number) value).intValue();
    }

    private String requiredText(JTextField field, String message) {
        String value = field.getText().trim();
        if (value.isEmpty()) {
            WorkforceFormToolkit.setFieldError(field, message);
            throw new IllegalArgumentException(message);
        }
        WorkforceFormToolkit.setFieldNormal(field);
        return value;
    }

    private LocalTime requiredTime(JTextField field, String message) {
        String value = requiredText(field, message);
        try {
            LocalTime time = LocalTime.parse(value);
            WorkforceFormToolkit.setFieldNormal(field);
            return time;
        } catch (DateTimeParseException ex) {
            WorkforceFormToolkit.setFieldError(field, "Use time format HH:mm.");
            throw new IllegalArgumentException("Use time format HH:mm, for example 08:00.");
        }
    }

    private LocalTime selectedTime(JComboBox<String> hourBox, JComboBox<String> minuteBox) {
        String hour = hourBox == null || hourBox.getSelectedItem() == null
                ? "00"
                : hourBox.getSelectedItem().toString();
        String minute = minuteBox == null || minuteBox.getSelectedItem() == null
                ? "00"
                : minuteBox.getSelectedItem().toString();
        return LocalTime.of(Integer.parseInt(hour), Integer.parseInt(minute));
    }

    private void selectTime(JComboBox<String> hourBox, JComboBox<String> minuteBox, LocalTime time) {
        if (time == null) {
            return;
        }
        if (hourBox != null) {
            hourBox.setSelectedItem(String.format("%02d", time.getHour()));
        }
        if (minuteBox != null) {
            int minute = (time.getMinute() / 15) * 15;
            minuteBox.setSelectedItem(String.format("%02d", minute));
        }
    }

    private LocalTime parseTimeOrDefault(String value, LocalTime fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            return fallback;
        }
    }

    private LocalDate requiredDate(JTextField field, String message) {
        String value = requiredText(field, message);
        try {
            LocalDate date = LocalDate.parse(value);
            WorkforceFormToolkit.setFieldNormal(field);
            return date;
        } catch (DateTimeParseException ex) {
            WorkforceFormToolkit.setFieldError(field, "Use date format yyyy-MM-dd.");
            throw new IllegalArgumentException("Use date format yyyy-MM-dd, for example 2026-06-12.");
        }
    }

    private LocalDate optionalDate(JTextField field) {
        String value = field.getText().trim();
        if (value.isEmpty()) {
            WorkforceFormToolkit.setFieldNormal(field);
            return null;
        }

        try {
            LocalDate date = LocalDate.parse(value);
            WorkforceFormToolkit.setFieldNormal(field);
            return date;
        } catch (DateTimeParseException ex) {
            WorkforceFormToolkit.setFieldError(field, "Use yyyy-MM-dd or leave blank.");
            throw new IllegalArgumentException("Use effective date format yyyy-MM-dd, or leave it blank.");
        }
    }

    private int nonNegativeInt(JTextField field, String message) {
        String value = field.getText().trim();
        if (value.isEmpty()) {
            WorkforceFormToolkit.setFieldNormal(field);
            return 0;
        }

        try {
            int number = Integer.parseInt(value);
            if (number < 0) {
                WorkforceFormToolkit.setFieldError(field, message);
                throw new IllegalArgumentException(message);
            }
            WorkforceFormToolkit.setFieldNormal(field);
            return number;
        } catch (NumberFormatException ex) {
            WorkforceFormToolkit.setFieldError(field, "Enter a whole number.");
            throw new IllegalArgumentException("Grace minutes must be a whole number.");
        }
    }

    private double positiveDouble(JTextField field, String message) {
        String value = field.getText().trim();
        if (value.isEmpty()) {
            WorkforceFormToolkit.setFieldError(field, message);
            throw new IllegalArgumentException(message);
        }

        try {
            double number = Double.parseDouble(value);
            if (number <= 0) {
                WorkforceFormToolkit.setFieldError(field, message);
                throw new IllegalArgumentException(message);
            }
            WorkforceFormToolkit.setFieldNormal(field);
            return number;
        } catch (NumberFormatException ex) {
            WorkforceFormToolkit.setFieldError(field, "Enter a valid number.");
            throw new IllegalArgumentException("Multiplier must be a number, for example 1.30.");
        }
    }

    private String selectedHolidayType() {
        Object selected = holidayTypeBox.getSelectedItem();
        if (selected == null || selected.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("Holiday type is required.");
        }
        return selected.toString();
    }

    private String selectedText(JComboBox<String> comboBox, String message) {
        Object selected = comboBox.getSelectedItem();
        if (selected == null || selected.toString().trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return selected.toString();
    }

    private ComboItem selectedOptionalItem(JComboBox<ComboItem> comboBox) {
        Object selected = comboBox.getSelectedItem();
        if (!(selected instanceof ComboItem item) || item.id <= 0) {
            return null;
        }
        return item;
    }

    private void selectComboItem(JComboBox<ComboItem> comboBox, Integer id) {
        int targetId = id == null ? 0 : id;
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            ComboItem item = comboBox.getItemAt(i);
            if (item.id == targetId) {
                comboBox.setSelectedIndex(i);
                return;
            }
        }
    }

    private void logAudit(String action, String tableName, String recordId) {
        try {
            auditService.logAction(getCurrentUserId(), action, tableName, recordId);
        } catch (Exception ignored) {
        }
    }

    private Integer getCurrentUserId() {
        return currentUser == null ? null : currentUser.getEmployeeId();
    }

    private String getCurrentRole() {
        return currentUser == null ? "" : currentUser.getRole();
    }

    private void setInputsEnabled(Container container, boolean enabled) {
        for (Component component : container.getComponents()) {
            component.setEnabled(enabled || component instanceof JLabel);
            if (component instanceof Container child) {
                setInputsEnabled(child, enabled);
            }
        }
    }

    private boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(
                this,
                message,
                "Confirm",
                JOptionPane.YES_NO_OPTION
        ) == JOptionPane.YES_OPTION;
    }

    private void showInfo(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void showShiftError(String message) {
        WorkforceFormToolkit.setError(shiftMessageLabel, message);
    }

    private void showHolidayError(String message) {
        WorkforceFormToolkit.setError(holidayMessageLabel, message);
    }

    private void clearShiftFieldStates() {
        WorkforceFormToolkit.setFieldNormal(shiftNameField);
        WorkforceFormToolkit.setFieldNormal(shiftGraceField);
        WorkforceFormToolkit.setFieldNormal(shiftEffectiveDateField);
    }

    private void clearHolidayFieldStates() {
        WorkforceFormToolkit.setFieldNormal(holidayNameField);
        WorkforceFormToolkit.setFieldNormal(holidayDateField);
        WorkforceFormToolkit.setFieldNormal(holidayMultiplierField);
    }

    private JPanel createSectionPanel(String title, String subtitle) {
        return WorkforceFormToolkit.createSection(title, subtitle);
    }

    private JPanel createCollapsibleSection(String title, String subtitle, JPanel body, boolean expanded) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(UITheme.BG);
        wrapper.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));

        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));

        JButton toggle = new JButton((expanded ? "▾  " : "▸  ") + title);
        toggle.setBorder(BorderFactory.createEmptyBorder());
        toggle.setContentAreaFilled(false);
        toggle.setFocusPainted(false);
        toggle.setFont(UITheme.FONT_BODY_BOLD);
        toggle.setForeground(UITheme.TEXT_PRIMARY);
        toggle.setHorizontalAlignment(SwingConstants.LEFT);

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

    private class ScheduleBoardCellRenderer extends DefaultTableCellRenderer {
        private final Color selectedBg = new Color(219, 234, 254);
        private final Color selectedFg = new Color(30, 64, 175);
        private final Color unassignedBg = new Color(255, 251, 235);
        private final Color restBg = new Color(243, 244, 246);
        private final Color holidayBg = new Color(254, 243, 199);
        private final Color shiftBg = new Color(239, 246, 255);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBorder(new EmptyBorder(4, 8, 4, 8));
            setHorizontalAlignment(column == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
            setFont(column == 0 ? UITheme.FONT_SMALL.deriveFont(Font.BOLD) : UITheme.FONT_SMALL);

            if (isSelected && column > 0) {
                component.setBackground(selectedBg);
                component.setForeground(selectedFg);
                setFont(UITheme.FONT_SMALL.deriveFont(Font.BOLD));
                return component;
            }

            String text = value == null ? "" : value.toString();
            if (column == 0) {
                component.setBackground(row % 2 == 0 ? Color.WHITE : UITheme.TABLE_ALT_ROW);
                component.setForeground(UITheme.TEXT_PRIMARY);
            } else if ("Unassigned".equalsIgnoreCase(text)) {
                component.setBackground(unassignedBg);
                component.setForeground(new Color(146, 64, 14));
            } else if ("RD".equalsIgnoreCase(text) || "REST DAY".equalsIgnoreCase(text)) {
                component.setBackground(restBg);
                component.setForeground(new Color(75, 85, 99));
                setFont(UITheme.FONT_SMALL.deriveFont(Font.BOLD));
            } else if ("HOL".equalsIgnoreCase(text) || text.toUpperCase().contains("HOLIDAY")) {
                component.setBackground(holidayBg);
                component.setForeground(new Color(180, 83, 9));
                setFont(UITheme.FONT_SMALL.deriveFont(Font.BOLD));
            } else {
                component.setBackground(shiftBg);
                component.setForeground(UITheme.BLUE);
                setFont(UITheme.FONT_SMALL.deriveFont(Font.BOLD));
            }
            return component;
        }
    }

    private static class ComboItem {
        private final int id;
        private final String label;

        private ComboItem(int id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static class BoardCell {
        private final int employeeId;
        private final LocalDate date;

        private BoardCell(int employeeId, LocalDate date) {
            this.employeeId = employeeId;
            this.date = date;
        }
    }
}
