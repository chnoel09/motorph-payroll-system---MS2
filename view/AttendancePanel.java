/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.oop.view;

import com.mycompany.oop.model.AttendanceRecord;
import com.mycompany.oop.model.AttendanceAwareness;
import com.mycompany.oop.model.Employee;
import com.mycompany.oop.service.AttendanceAwarenessService;
import com.mycompany.oop.service.AttendanceService;
import com.mycompany.oop.service.ReportService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttendancePanel extends JPanel implements RefreshablePanel {

    private Employee employee;
    private AttendanceService attendanceService;
    private AttendanceAwarenessService attendanceAwarenessService;

    private JLabel statusLabel;
    private JLabel awarenessLabel;
    private JTable table;
    private JButton timeInBtn;
    private JButton timeOutBtn;

    private JComboBox<String> cutoffComboBox;
    private JButton applyFilterBtn;
    private JButton resetFilterBtn;
    private JButton timecardReportBtn;
    private boolean dataLoading;

    public AttendancePanel(Employee employee) {
        this.employee = employee;
        this.attendanceService = new AttendanceService();
        this.attendanceAwarenessService = new AttendanceAwarenessService();

        setLayout(new BorderLayout());
        setBackground(UITheme.BG);

        add(UITheme.createTitleBar("Detailed Timekeeping History"), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(UITheme.BG);
        content.setBorder(new EmptyBorder(16, 20, 16, 20));

        content.add(createTodayPanel(), BorderLayout.NORTH);
        content.add(createHistorySection(), BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);

        refreshAttendanceAsync(true);
    }

    private JPanel createTodayPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UITheme.BORDER),
                        new EmptyBorder(18, 22, 18, 22)
                )
        );

        JLabel title = new JLabel("Timekeeping Checkpoint");
        title.setFont(UITheme.FONT_SECTION);
        title.setForeground(UITheme.TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(0, 0, 10, 0));

        statusLabel = new JLabel("Not clocked in today");
        statusLabel.setFont(UITheme.FONT_BODY_BOLD);
        statusLabel.setForeground(UITheme.DANGER);

        awarenessLabel = new JLabel("For schedule, leave, overtime, and correction context, use My Work Calendar.");
        awarenessLabel.setFont(UITheme.FONT_SMALL);
        awarenessLabel.setForeground(UITheme.TEXT_SECONDARY);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        buttonPanel.setBackground(Color.WHITE);

        timeInBtn = UITheme.createAccentButton("Time In");
        timeOutBtn = UITheme.createButton("Time Out");

        UITheme.sizeButtonToFit(timeInBtn, 120, 36);
        UITheme.sizeButtonToFit(timeOutBtn, 120, 36);

        timeInBtn.addActionListener(e -> {
            attendanceService.timeIn(employee.getEmployeeId());
            refreshAttendanceAsync(true);
        });

        timeOutBtn.addActionListener(e -> {
            attendanceService.timeOut(employee.getEmployeeId());
            refreshAttendanceAsync(true);
        });

        buttonPanel.add(timeInBtn);
        buttonPanel.add(timeOutBtn);

        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setBackground(Color.WHITE);
        topSection.add(title, BorderLayout.NORTH);
        topSection.add(statusLabel, BorderLayout.CENTER);
        topSection.add(awarenessLabel, BorderLayout.SOUTH);

        panel.add(topSection, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        JPanel outerWrapper = new JPanel(new BorderLayout());
        outerWrapper.setBackground(UITheme.BG);
        outerWrapper.setBorder(new EmptyBorder(0, 0, 16, 0));
        outerWrapper.add(panel, BorderLayout.CENTER);

        return outerWrapper;
    }

    private JPanel createHistorySection() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 12));
        wrapper.setBackground(UITheme.BG);

        wrapper.add(createFilterPanel(), BorderLayout.NORTH);
        wrapper.add(createHistoryPanel(), BorderLayout.CENTER);

        return wrapper;
    }

    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panel.setBackground(UITheme.BG);

        JLabel cutoffLabel = new JLabel("Timekeeping Period:");
        cutoffLabel.setFont(UITheme.FONT_BODY_BOLD);
        cutoffLabel.setForeground(UITheme.TEXT_PRIMARY);

        cutoffComboBox = new JComboBox<>();
        cutoffComboBox.setPreferredSize(new Dimension(220, 38));
        WorkforceFormToolkit.styleComboBox(cutoffComboBox);
        WorkforceFormToolkit.applyMonthHelp(cutoffComboBox);
        JLabel cutoffHelp = WorkforceFormToolkit.createHelpLabel("Detailed attendance history filter");

        applyFilterBtn = UITheme.createAccentButton("Apply");
        resetFilterBtn = UITheme.createButton("Show All");
        timecardReportBtn = UITheme.createButton("Timecard Report");
        timecardReportBtn.setToolTipText("Generate timecard report for the selected period");

        UITheme.sizeButtonToFit(applyFilterBtn, 100, 34);
        UITheme.sizeButtonToFit(resetFilterBtn, 110, 34);
        UITheme.sizeButtonToFit(timecardReportBtn, 150, 34);

        applyFilterBtn.addActionListener(e -> refreshAttendanceAsync(false));
        resetFilterBtn.addActionListener(e -> {
            cutoffComboBox.setSelectedIndex(0);
            refreshAttendanceAsync(false);
        });
        timecardReportBtn.addActionListener(e -> generateTimecardReport());

        panel.add(cutoffLabel);
        panel.add(cutoffComboBox);
        panel.add(cutoffHelp);
        panel.add(applyFilterBtn);
        panel.add(resetFilterBtn);
        panel.add(timecardReportBtn);

        return panel;
    }

    private JScrollPane createHistoryPanel() {
        table = new JTable();
        UITheme.styleTable(table);

        return UITheme.createTableScrollPane(table);
    }

    private void loadCutoffOptions() {
        if (cutoffComboBox == null) {
            return;
        }

        cutoffComboBox.removeAllItems();
        cutoffComboBox.addItem("All Records");

        List<String> cutoffs = attendanceService.getAvailableCutoffsForEmployee(employee.getEmployeeId());
        for (String cutoff : cutoffs) {
            cutoffComboBox.addItem(cutoff);
        }
    }

    private void refreshAttendance() {
        List<AttendanceRecord> history;
        String selectedCutoff = cutoffComboBox != null && cutoffComboBox.getSelectedItem() != null
                ? cutoffComboBox.getSelectedItem().toString()
                : "All Records";

        if ("All Records".equals(selectedCutoff)) {
            history = attendanceService.getAttendanceHistory(employee.getEmployeeId());
        } else {
            history = attendanceService.getAttendanceHistoryByCutoff(employee.getEmployeeId(), selectedCutoff);
        }

        Map<String, AttendanceAwareness> awarenessByDate = getAwarenessByDate(history);
        updateAttendanceTable(history, awarenessByDate);
    }

    private void updateAttendanceTable(List<AttendanceRecord> history, Map<String, AttendanceAwareness> awarenessByDate) {
        String[] columns = {"Date", "Time In", "Time Out", "Hours Worked", "Awareness"};
        Object[][] data = new Object[history.size()][5];

        for (int i = 0; i < history.size(); i++) {
            AttendanceRecord record = history.get(i);
            AttendanceAwareness awareness = awarenessByDate.get(record.getDate());
            data[i][0] = record.getDate();
            data[i][1] = record.getTimeIn();
            data[i][2] = (record.getTimeOut() == null || record.getTimeOut().isEmpty())
                    ? "--"
                    : record.getTimeOut();
            data[i][3] = String.format("%.2f", attendanceService.getHoursWorked(record));
            data[i][4] = awareness == null ? "Not evaluated" : awareness.getStatus();
        }

        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table.setModel(model);
    }

    @Override
    public void refreshData() {
        refreshAttendanceAsync(true);
    }

    private void refreshAttendanceAsync(boolean reloadCutoffs) {
        if (dataLoading) {
            return;
        }
        dataLoading = true;
        setAttendanceControlsEnabled(false);
        showAttendanceLoadingState();
        String selectedCutoff = cutoffComboBox != null && cutoffComboBox.getSelectedItem() != null
                ? cutoffComboBox.getSelectedItem().toString()
                : "All Records";

        SwingWorker<AttendanceLoadData, Void> worker = new SwingWorker<>() {
            private long startedAtMs;

            @Override
            protected AttendanceLoadData doInBackground() {
                startedAtMs = System.currentTimeMillis();
                List<String> cutoffs = reloadCutoffs
                        ? attendanceService.getAvailableCutoffsForEmployee(employee.getEmployeeId())
                        : List.of();
                List<AttendanceRecord> history = "All Records".equals(selectedCutoff)
                        ? attendanceService.getAttendanceHistory(employee.getEmployeeId())
                        : attendanceService.getAttendanceHistoryByCutoff(employee.getEmployeeId(), selectedCutoff);
                Map<String, AttendanceAwareness> awarenessByDate = getAwarenessByDate(history);
                List<AttendanceRecord> fullHistory = attendanceService.getAttendanceHistory(employee.getEmployeeId());
                AttendanceAwareness todayAwareness = attendanceAwarenessService.getDailyAwareness(
                        employee.getEmployeeId(), LocalDate.now());
                return new AttendanceLoadData(cutoffs, history, awarenessByDate, fullHistory, todayAwareness,
                        selectedCutoff, reloadCutoffs);
            }

            @Override
            protected void done() {
                try {
                    applyAttendanceLoadData(get());
                    System.out.println("[perf] AttendancePanel refreshData took "
                            + (System.currentTimeMillis() - startedAtMs) + " ms");
                } catch (Exception ex) {
                    showAttendanceErrorState();
                } finally {
                    dataLoading = false;
                    setAttendanceControlsEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void applyAttendanceLoadData(AttendanceLoadData data) {
        if (data.reloadCutoffs()) {
            cutoffComboBox.removeAllItems();
            cutoffComboBox.addItem("All Records");
            for (String cutoff : data.cutoffs()) {
                cutoffComboBox.addItem(cutoff);
            }
            cutoffComboBox.setSelectedItem(data.selectedCutoff());
            if (cutoffComboBox.getSelectedItem() == null) {
                cutoffComboBox.setSelectedIndex(0);
            }
        }
        applyStatusSection(data.fullHistory(), data.todayAwareness());
        updateAttendanceTable(data.history(), data.awarenessByDate());
    }

    private void showAttendanceLoadingState() {
        if (table == null) {
            return;
        }
        table.setModel(new DefaultTableModel(new Object[][]{
                {"", "Loading attendance records...", "", "", "Loading"},
                {"", "Preparing timekeeping awareness...", "", "", "Loading"}
        }, new String[]{"Date", "Time In", "Time Out", "Hours Worked", "Awareness"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
    }

    private void showAttendanceErrorState() {
        if (table == null) {
            return;
        }
        table.setModel(new DefaultTableModel(new Object[][]{
                {"", "Unable to load attendance records.", "", "", "Error"}
        }, new String[]{"Date", "Time In", "Time Out", "Hours Worked", "Awareness"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
    }

    private void setAttendanceControlsEnabled(boolean enabled) {
        if (applyFilterBtn != null) applyFilterBtn.setEnabled(enabled);
        if (resetFilterBtn != null) resetFilterBtn.setEnabled(enabled);
        if (timecardReportBtn != null) timecardReportBtn.setEnabled(enabled);
    }

    private void refreshStatusSection() {
        List<AttendanceRecord> fullHistory =
                attendanceService.getAttendanceHistory(employee.getEmployeeId());
        AttendanceAwareness todayAwareness = attendanceAwarenessService.getDailyAwareness(
                employee.getEmployeeId(), LocalDate.now());
        applyStatusSection(fullHistory, todayAwareness);
    }

    private void applyStatusSection(List<AttendanceRecord> fullHistory, AttendanceAwareness todayAwareness) {
        if (fullHistory.isEmpty()) {
            statusLabel.setText("Not clocked in today");
            statusLabel.setForeground(UITheme.DANGER);
            timeInBtn.setEnabled(true);
            timeOutBtn.setEnabled(false);
            updateTodayAwareness(todayAwareness);
            return;
        }

        AttendanceRecord latest = fullHistory.get(fullHistory.size() - 1);
        String todayStr = LocalDate.now().toString();

        if (latest.getDate().equals(todayStr)) {

            if (latest.getTimeIn() != null && !latest.getTimeIn().isEmpty()
                    && (latest.getTimeOut() == null || latest.getTimeOut().isEmpty())) {

                statusLabel.setText("Timed in today at " + latest.getTimeIn());
                statusLabel.setForeground(UITheme.SUCCESS);
                timeInBtn.setEnabled(false);
                timeOutBtn.setEnabled(true);

            } else if (latest.getTimeOut() != null && !latest.getTimeOut().isEmpty()) {

                statusLabel.setText("Done for today (" +
                        latest.getTimeIn() + " - " + latest.getTimeOut() + ")");
                statusLabel.setForeground(UITheme.TEXT_SECONDARY);
                timeInBtn.setEnabled(false);
                timeOutBtn.setEnabled(false);

            } else {
                statusLabel.setText("Not clocked in today");
                statusLabel.setForeground(UITheme.DANGER);
                timeInBtn.setEnabled(true);
                timeOutBtn.setEnabled(false);
            }

        } else {
            statusLabel.setText("Not clocked in today");
            statusLabel.setForeground(UITheme.DANGER);
            timeInBtn.setEnabled(true);
            timeOutBtn.setEnabled(false);
        }

        updateTodayAwareness(todayAwareness);
    }

    private void updateTodayAwareness() {
        AttendanceAwareness awareness = attendanceAwarenessService.getDailyAwareness(
                employee.getEmployeeId(), LocalDate.now());
        updateTodayAwareness(awareness);
    }

    private void updateTodayAwareness(AttendanceAwareness awareness) {
        awarenessLabel.setText(awareness.getStatus() + ": " + awareness.getMessage());
        awarenessLabel.setForeground(getAwarenessColor(awareness));
    }

    private Map<String, AttendanceAwareness> getAwarenessByDate(List<AttendanceRecord> history) {
        Map<String, AttendanceAwareness> awarenessByDate = new HashMap<>();
        if (history == null || history.isEmpty()) {
            return awarenessByDate;
        }

        LocalDate start = null;
        LocalDate end = null;
        for (AttendanceRecord record : history) {
            try {
                LocalDate date = LocalDate.parse(record.getDate());
                start = start == null || date.isBefore(start) ? date : start;
                end = end == null || date.isAfter(end) ? date : end;
            } catch (Exception ignored) {
            }
        }

        if (start == null || end == null) {
            return awarenessByDate;
        }

        for (AttendanceAwareness awareness : attendanceAwarenessService.getEmployeeAwareness(
                employee.getEmployeeId(), start, end)) {
            if (awareness.getDate() != null) {
                awarenessByDate.put(awareness.getDate().toString(), awareness);
            }
        }

        return awarenessByDate;
    }

    private Color getAwarenessColor(AttendanceAwareness awareness) {
        return switch (awareness.getSeverity()) {
            case CRITICAL -> UITheme.DANGER;
            case WARNING -> UITheme.YELLOW;
            case INFO -> UITheme.BLUE;
            default -> UITheme.SUCCESS;
        };
    }

    private void generateTimecardReport() {
        DateRange range = resolveReportDateRange();
        if (range == null) {
            return;
        }

        if (!hasReportData(range.start(), range.end())) {
            JOptionPane.showMessageDialog(this,
                    "No attendance records exist for the selected period.",
                    "Timecard Unavailable",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            File previewFile = new File(
                    System.getProperty("java.io.tmpdir"),
                    "timecard_" + employee.getEmployeeId() + "_"
                            + sanitizeFilePart(range.label()) + ".pdf");

            new ReportService().generateTimecard(
                    employee.getEmployeeId(),
                    range.start(),
                    range.end(),
                    range.label(),
                    previewFile.getAbsolutePath());
            openGeneratedReport(previewFile);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to generate the timecard preview.",
                    "Report Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private DateRange resolveReportDateRange() {
        String selectedCutoff = cutoffComboBox != null && cutoffComboBox.getSelectedItem() != null
                ? cutoffComboBox.getSelectedItem().toString()
                : "All Records";

        if (!"All Records".equals(selectedCutoff)) {
            LocalDate start = attendanceService.getCutoffStartDate(selectedCutoff);
            LocalDate end = attendanceService.getCutoffEndDate(selectedCutoff);
            return new DateRange(start, end, selectedCutoff);
        }

        DateRange displayedRange = getDisplayedAttendanceDateRange();
        if (displayedRange != null) {
            return displayedRange;
        }

        JOptionPane.showMessageDialog(this,
                "Select a timekeeping period before generating a timecard report.",
                "Select Period",
                JOptionPane.INFORMATION_MESSAGE);
        return null;
    }

    private DateRange getDisplayedAttendanceDateRange() {
        if (table == null || table.getModel() == null || table.getRowCount() == 0) {
            return null;
        }

        LocalDate start = null;
        LocalDate end = null;

        for (int row = 0; row < table.getRowCount(); row++) {
            Object value = table.getValueAt(row, 0);
            if (value == null) {
                continue;
            }

            try {
                LocalDate date = LocalDate.parse(value.toString());
                start = start == null || date.isBefore(start) ? date : start;
                end = end == null || date.isAfter(end) ? date : end;
            } catch (Exception ignored) {
            }
        }

        if (start == null || end == null) {
            return null;
        }

        return new DateRange(start, end, start + " to " + end);
    }

    private boolean hasReportData(LocalDate start, LocalDate end) {
        return attendanceService.getAttendanceHistory(employee.getEmployeeId()).stream()
                .anyMatch(record -> isWithinRange(record, start, end));
    }

    private boolean isWithinRange(AttendanceRecord record, LocalDate start, LocalDate end) {
        try {
            LocalDate date = LocalDate.parse(record.getDate());
            return !date.isBefore(start) && !date.isAfter(end);
        } catch (Exception e) {
            return false;
        }
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

    private record DateRange(LocalDate start, LocalDate end, String label) {
    }

    private record AttendanceLoadData(
            List<String> cutoffs,
            List<AttendanceRecord> history,
            Map<String, AttendanceAwareness> awarenessByDate,
            List<AttendanceRecord> fullHistory,
            AttendanceAwareness todayAwareness,
            String selectedCutoff,
            boolean reloadCutoffs) {
    }
}
