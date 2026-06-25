/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.oop.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.PayrollHistoryRecord;
import com.mycompany.oop.service.PayrollService;
import com.mycompany.oop.service.ReportService;

public class PayslipPanel extends JPanel implements RefreshablePanel {

    private final PayrollService payrollService;
    private final NumberFormat peso = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
    private final Employee employee;
    private List<PayrollHistoryRecord> history;
    private JComboBox<String> cutoffBox;
    private JButton viewBtn;
    private JButton printBtn;
    private CardLayout payslipCardLayout;
    private JPanel payslipCardPanel;

    private JLabel employeeNameValue;
    private JLabel employeeIdValue;
    private JLabel cutoffValue;
    private JLabel positionValue;
    private JLabel periodRangeValue;

    private JLabel basicValue;
    private JLabel allowanceValue;
    private JLabel grossValue;

    private JLabel sssValue;
    private JLabel philhealthValue;
    private JLabel pagibigValue;
    private JLabel taxValue;
    private JLabel totalValue;

    private JLabel netValue;
    private JLabel takeHomeValue;

    public PayslipPanel(Employee employee) {

        this.employee = employee;
        payrollService = new PayrollService();

        setLayout(new BorderLayout());
        setBackground(UITheme.BG);

        add(UITheme.createTitleBar("My Payslip History"), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(UITheme.BG);
        content.setBorder(new EmptyBorder(20, 24, 20, 24));

        history = payrollService.getPayrollHistoryForEmployee(employee.getEmployeeId());

        cutoffBox = new JComboBox<>();
        populateCutoffBox();
        WorkforceFormToolkit.styleComboBox(cutoffBox);
        WorkforceFormToolkit.applyMonthHelp(cutoffBox);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel selectLabel = new JLabel("Select Cutoff:");
        selectLabel.setFont(UITheme.FONT_BODY_BOLD);
        selectLabel.setForeground(UITheme.TEXT_PRIMARY);

        topPanel.add(selectLabel);
        topPanel.add(cutoffBox);
        topPanel.add(WorkforceFormToolkit.createHelpLabel("Finalized payroll history"));

        content.add(topPanel, BorderLayout.NORTH);

        JPanel preview = createPayslipPreview(employee);
        JScrollPane scrollPane = new JScrollPane(preview);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        WorkforceFormToolkit.tuneScrollPane(scrollPane);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        payslipCardLayout = new CardLayout();
        payslipCardPanel = new JPanel(payslipCardLayout);
        payslipCardPanel.setBackground(UITheme.BG);
        payslipCardPanel.add(scrollPane, "preview");
        payslipCardPanel.add(createEmptyPayslipPanel(), "empty");

        content.add(payslipCardPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        buttonPanel.setBackground(UITheme.BG);

        viewBtn = UITheme.createAccentButton("Preview Payslip");
        printBtn = UITheme.createButton("Save PDF");
        viewBtn.setToolTipText("Open the selected payslip as a temporary Jasper PDF preview.");
        printBtn.setToolTipText("Save the selected payslip as a Jasper PDF file.");

        UITheme.sizeButtonToFit(viewBtn, 150, 36);
        UITheme.sizeButtonToFit(printBtn, 110, 36);

        buttonPanel.add(viewBtn);
        buttonPanel.add(printBtn);

        content.add(buttonPanel, BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);

        installCutoffListener();

        viewBtn.addActionListener(e -> {
            PayrollHistoryRecord record = getSelectedHistoryRecord();
            if (record != null) {
                previewJasperPayslip(employee, record);
            }
        });

        printBtn.addActionListener(e -> {
            PayrollHistoryRecord record = getSelectedHistoryRecord();
            if (record != null) {
                generateJasperPayslip(employee, record);
            }
        });

        if (!history.isEmpty()) {
            cutoffBox.setSelectedIndex(0);
            updatePreview(employee, history.get(0));
            showPayslipContent(true);
        } else {
            updatePreview(employee, null);
            updatePayslipButtonState(false);
            showPayslipContent(false);
        }
    }

    @Override
    public void refreshData() {
        if (cutoffBox == null) {
            return;
        }

        Object selected = cutoffBox.getSelectedItem();
        String selectedCutoff = selected == null ? null : selected.toString();
        history = payrollService.getPayrollHistoryForEmployee(employee.getEmployeeId());

        for (ActionListener listener : cutoffBox.getActionListeners()) {
            cutoffBox.removeActionListener(listener);
        }
        cutoffBox.removeAllItems();
        populateCutoffBox();

        int selectedIndex = -1;
        if (selectedCutoff != null) {
            for (int i = 0; i < history.size(); i++) {
                if (selectedCutoff.equalsIgnoreCase(history.get(i).getCutoffPeriod())) {
                    selectedIndex = i;
                    break;
                }
            }
        }
        if (selectedIndex < 0 && !history.isEmpty()) {
            selectedIndex = 0;
        }

        installCutoffListener();

        boolean hasHistory = selectedIndex >= 0;
        updatePayslipButtonState(hasHistory);
        if (hasHistory) {
            cutoffBox.setSelectedIndex(selectedIndex);
            updatePreview(employee, history.get(selectedIndex));
            showPayslipContent(true);
        } else {
            updatePreview(employee, null);
            showPayslipContent(false);
        }
    }

    private void populateCutoffBox() {
        for (PayrollHistoryRecord r : history) {
            cutoffBox.addItem(r.getCutoffPeriod());
        }
    }

    private void installCutoffListener() {
        cutoffBox.addActionListener(e -> {
            int index = cutoffBox.getSelectedIndex();
            PayrollHistoryRecord record = index >= 0 && index < history.size() ? history.get(index) : null;
            updatePreview(employee, record);
            updatePayslipButtonState(record != null);
        });
    }

    private PayrollHistoryRecord getSelectedHistoryRecord() {
        int index = cutoffBox.getSelectedIndex();
        return index >= 0 && index < history.size() ? history.get(index) : null;
    }

    private void updatePayslipButtonState(boolean hasHistory) {
        if (cutoffBox != null) {
            cutoffBox.setEnabled(hasHistory);
        }
        if (viewBtn != null) {
            viewBtn.setEnabled(hasHistory);
            viewBtn.setToolTipText(hasHistory
                    ? "Open the selected payslip as a temporary Jasper PDF preview."
                    : "No finalized payroll history is available for this employee.");
        }
        if (printBtn != null) {
            printBtn.setEnabled(hasHistory);
            printBtn.setToolTipText(hasHistory
                    ? "Save the selected payslip as a Jasper PDF file."
                    : "No finalized payroll history is available for this employee.");
        }
    }

    private void showPayslipContent(boolean hasHistory) {
        if (payslipCardLayout != null && payslipCardPanel != null) {
            payslipCardLayout.show(payslipCardPanel, hasHistory ? "preview" : "empty");
        }
    }

    private JPanel createEmptyPayslipPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                new EmptyBorder(48, 32, 48, 32)
        ));

        JPanel message = new JPanel();
        message.setOpaque(false);
        message.setLayout(new BoxLayout(message, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("No finalized payslip is available for this employee yet.");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(UITheme.TEXT_PRIMARY);

        JLabel body = new JLabel("Payslips appear here after payroll has been processed and saved to payroll history.");
        body.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.setFont(UITheme.FONT_BODY);
        body.setForeground(UITheme.TEXT_SECONDARY);

        message.add(title);
        message.add(Box.createVerticalStrut(8));
        message.add(body);
        panel.add(message);
        return panel;
    }

    private JPanel createPayslipPreview(Employee employee) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 14));
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                new EmptyBorder(18, 28, 18, 28)
        ));

        wrapper.add(createBrandHeader(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setBackground(Color.WHITE);
        body.add(createEmployeeDetailsPanel(employee), BorderLayout.NORTH);
        body.add(createBreakdownPanel(), BorderLayout.CENTER);
        body.add(createSummaryPanel(), BorderLayout.SOUTH);

        wrapper.add(body, BorderLayout.CENTER);
        wrapper.add(createConfidentialLabel(), BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel createBrandHeader() {
        JPanel header = new JPanel(new BorderLayout(14, 0));
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(0, 0, 12, 0));

        JLabel logo = createLogoLabel();
        header.add(logo, BorderLayout.WEST);

        JPanel titleStack = new JPanel();
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));
        titleStack.setBackground(Color.WHITE);

        JLabel company = new JLabel("MotorPH");
        company.setFont(new Font("Segoe UI", Font.BOLD, 22));
        company.setForeground(UITheme.TEXT_PRIMARY);

        JLabel title = new JLabel("Employee Payslip");
        title.setFont(UITheme.FONT_BODY_BOLD);
        title.setForeground(UITheme.TEXT_SECONDARY);

        JLabel contact = new JLabel("7 Jupiter Avenue cor. F. Sandoval Jr., Bagong Nayon, Quezon City");
        contact.setFont(UITheme.FONT_SMALL);
        contact.setForeground(UITheme.TEXT_SECONDARY);

        titleStack.add(company);
        titleStack.add(Box.createVerticalStrut(2));
        titleStack.add(title);
        titleStack.add(Box.createVerticalStrut(2));
        titleStack.add(contact);

        header.add(titleStack, BorderLayout.CENTER);
        return header;
    }

    private JLabel createLogoLabel() {
        URL logoUrl = getClass().getResource("/images/motorph_logo.png");
        if (logoUrl == null) {
            JLabel fallback = new JLabel("MotorPH", SwingConstants.CENTER);
            fallback.setOpaque(true);
            fallback.setBackground(new Color(210, 43, 43));
            fallback.setForeground(Color.WHITE);
            fallback.setFont(new Font("Segoe UI", Font.BOLD, 16));
            fallback.setPreferredSize(new Dimension(96, 56));
            return fallback;
        }

        ImageIcon original = new ImageIcon(logoUrl);
        Image scaled = original.getImage().getScaledInstance(96, 56, Image.SCALE_SMOOTH);
        JLabel logo = new JLabel(new ImageIcon(scaled));
        logo.setPreferredSize(new Dimension(104, 62));
        return logo;
    }

    private JPanel createEmployeeDetailsPanel(Employee employee) {
        JPanel panel = new JPanel(new GridLayout(0, 4, 12, 6));
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                new EmptyBorder(12, 14, 12, 14)
        ));

        employeeNameValue = createMetaValue();
        employeeIdValue = createMetaValue();
        cutoffValue = createMetaValue();
        positionValue = createMetaValue();
        periodRangeValue = createMetaValue();

        addMeta(panel, "Employee Name", employeeNameValue);
        addMeta(panel, "Employee ID", employeeIdValue);
        addMeta(panel, "Cutoff Period", cutoffValue);
        addMeta(panel, "Position", positionValue);
        addMeta(panel, "Period Range", periodRangeValue);
        addMeta(panel, "Prepared For", createStaticMetaValue(fullName(employee)));

        return panel;
    }

    private JPanel createBreakdownPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 18, 0));
        panel.setBackground(Color.WHITE);

        basicValue = createValueLabelPlain();
        allowanceValue = createValueLabelBold();
        grossValue = createValueLabelBold();

        sssValue = createValueLabelPlain();
        philhealthValue = createValueLabelPlain();
        pagibigValue = createValueLabelPlain();
        taxValue = createValueLabelPlain();
        totalValue = createValueLabelBold();

        JPanel earnings = createSectionPanel("EARNINGS");
        addRow(earnings, "Basic Pay", basicValue, false);
        addDivider(earnings);
        addRow(earnings, "Total Allowance", allowanceValue, true);
        addRow(earnings, "Gross Pay", grossValue, true);

        JPanel deductions = createSectionPanel("DEDUCTIONS");
        addRow(deductions, "SSS", sssValue, false);
        addRow(deductions, "PhilHealth", philhealthValue, false);
        addRow(deductions, "Pag-IBIG", pagibigValue, false);
        addRow(deductions, "Withholding Tax", taxValue, false);
        addDivider(deductions);
        addRow(deductions, "Total Deductions", totalValue, true);

        panel.add(earnings);
        panel.add(deductions);
        return panel;
    }

    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setBackground(new Color(245, 248, 246));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(198, 216, 205)),
                new EmptyBorder(12, 16, 12, 16)
        ));

        JLabel title = new JLabel("NET PAY");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(UITheme.TEXT_PRIMARY);

        netValue = createValueLabelBold();
        takeHomeValue = new JLabel("₱0.00", SwingConstants.RIGHT);
        takeHomeValue.setFont(new Font("Segoe UI", Font.BOLD, 24));
        takeHomeValue.setForeground(new Color(0, 102, 51));

        JPanel left = new JPanel(new GridLayout(0, 1, 0, 2));
        left.setOpaque(false);
        left.add(title);
        left.add(createSmallMutedLabel("Take-home pay after deductions"));

        panel.add(left, BorderLayout.WEST);
        panel.add(takeHomeValue, BorderLayout.EAST);
        return panel;
    }

    private JLabel createConfidentialLabel() {
        JLabel label = new JLabel(
                "<html><center><i>CONFIDENTIAL: This document contains sensitive payroll information intended solely for the employee.</i></center></html>",
                SwingConstants.CENTER
        );
        label.setFont(UITheme.FONT_SMALL);
        label.setForeground(UITheme.TEXT_SECONDARY);
        label.setBorder(new EmptyBorder(4, 10, 0, 10));
        return label;
    }

    private void updatePreview(Employee employee, PayrollHistoryRecord record) {
        employeeNameValue.setText(fullName(employee));
        employeeIdValue.setText(String.valueOf(employee.getEmployeeId()));
        positionValue.setText(safeText(employee.getPosition(), "-"));

        cutoffValue.setText(record == null ? "-" : safeText(record.getCutoffPeriod(), "-"));
        periodRangeValue.setText(record == null ? "-" : formatRange(record.getPeriodStart(), record.getPeriodEnd()));

        basicValue.setText(money(record == null ? 0.0 : record.getBasicComponent()));
        allowanceValue.setText(money(record == null ? 0.0 : record.getAllowanceComponent()));
        grossValue.setText(money(record == null ? 0.0 : record.getGross()));

        sssValue.setText(money(record == null ? 0.0 : record.getSss()));
        philhealthValue.setText(money(record == null ? 0.0 : record.getPhilhealth()));
        pagibigValue.setText(money(record == null ? 0.0 : record.getPagibig()));
        taxValue.setText(money(record == null ? 0.0 : record.getTax()));
        totalValue.setText(money(record == null ? 0.0 : record.getTotalDeductions()));

        String netText = money(record == null ? 0.0 : record.getNet());
        netValue.setText(netText);
        takeHomeValue.setText(netText);
    }

    private void generateJasperPayslip(Employee employee, PayrollHistoryRecord record) {
        if (employee == null || record == null) {
            JOptionPane.showMessageDialog(this,
                    "No finalized payroll history is available for this payslip.",
                    "Payslip Unavailable",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        String safeCutoff = record.getCutoffPeriod() == null
                ? "period"
                : record.getCutoffPeriod().replaceAll("[^a-zA-Z0-9._-]", "_");
        chooser.setSelectedFile(new File("payslip_" + employee.getEmployeeId() + "_" + safeCutoff + ".pdf"));
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            new ReportService().generatePayslip(
                    employee.getEmployeeId(),
                    record.getCutoffPeriod(),
                    chooser.getSelectedFile().getAbsolutePath());
            JOptionPane.showMessageDialog(this,
                    "Jasper payslip generated successfully.",
                    "Payslip Generated",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Jasper report generation failed for the selected payslip.",
                    "Report Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void previewJasperPayslip(Employee employee, PayrollHistoryRecord record) {
        if (employee == null || record == null) {
            JOptionPane.showMessageDialog(this,
                    "No finalized payroll history is available for this payslip.",
                    "Payslip Unavailable",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            String safeCutoff = record.getCutoffPeriod() == null
                    ? "period"
                    : record.getCutoffPeriod().replaceAll("[^a-zA-Z0-9._-]", "_");
            File previewFile = File.createTempFile(
                    "motorph_payslip_" + employee.getEmployeeId() + "_" + safeCutoff + "_",
                    ".pdf"
            );
            previewFile.deleteOnExit();

            new ReportService().generatePayslip(
                    employee.getEmployeeId(),
                    record.getCutoffPeriod(),
                    previewFile.getAbsolutePath());

            openPdfPreview(previewFile);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Unable to open the Jasper payslip preview for the selected cutoff.",
                    "Payslip Preview Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openPdfPreview(File pdfFile) throws IOException {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            JOptionPane.showMessageDialog(this,
                    "Payslip preview was generated, but this system cannot open PDF files automatically:\n"
                            + pdfFile.getAbsolutePath(),
                    "Open PDF Manually",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Desktop.getDesktop().open(pdfFile);
    }

    private JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UITheme.FONT_SECTION);
        titleLabel.setForeground(UITheme.ACCENT);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(8));
        return panel;
    }

    private void addRow(JPanel panel, String labelText, JLabel valueLabel, boolean emphasized) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(labelText);
        label.setFont(emphasized ? UITheme.FONT_BODY_BOLD : UITheme.FONT_BODY);
        label.setForeground(emphasized ? UITheme.TEXT_PRIMARY : UITheme.TEXT_SECONDARY);

        valueLabel.setFont(emphasized ? UITheme.FONT_BODY_BOLD : UITheme.FONT_BODY);
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(label, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.EAST);
        panel.add(row);
        panel.add(Box.createVerticalStrut(5));
    }

    private void addMeta(JPanel panel, String label, JLabel value) {
        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setOpaque(false);

        JLabel labelComponent = createSmallMutedLabel(label);
        stack.add(labelComponent);
        stack.add(Box.createVerticalStrut(2));
        stack.add(value);
        panel.add(stack);
    }

    private void addDivider(JPanel panel) {
        JSeparator separator = new JSeparator();
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        panel.add(Box.createVerticalStrut(6));
        panel.add(separator);
        panel.add(Box.createVerticalStrut(8));
    }

    private JLabel createMetaValue() {
        JLabel label = new JLabel("-");
        label.setFont(UITheme.FONT_BODY_BOLD);
        label.setForeground(UITheme.TEXT_PRIMARY);
        return label;
    }

    private JLabel createStaticMetaValue(String value) {
        JLabel label = createMetaValue();
        label.setText(safeText(value, "-"));
        return label;
    }

    private JLabel createSmallMutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UITheme.FONT_SMALL);
        label.setForeground(UITheme.TEXT_SECONDARY);
        return label;
    }

    private JLabel createValueLabelBold() {
        JLabel lbl = new JLabel("₱0.00");
        lbl.setFont(UITheme.FONT_BODY_BOLD);
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        return lbl;
    }

    private JLabel createValueLabelPlain() {
        JLabel lbl = new JLabel("₱0.00");
        lbl.setFont(UITheme.FONT_BODY);
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        return lbl;
    }

    private String money(double value) {
        return peso.format(safeMoney(value));
    }

    private double safeMoney(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }

    private String formatRange(String start, String end) {
        String safeStart = safeText(start, "-");
        String safeEnd = safeText(end, "-");
        if ("-".equals(safeStart) && "-".equals(safeEnd)) {
            return "-";
        }
        return safeStart + " to " + safeEnd;
    }

    private String fullName(Employee employee) {
        if (employee == null) {
            return "-";
        }
        return (safeText(employee.getFirstName(), "") + " " + safeText(employee.getLastName(), "")).trim();
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
