/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.oop.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.awt.print.Printable;
import java.awt.Graphics2D;
import java.text.NumberFormat;
import java.util.Locale;

import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.PayrollHistoryRecord;

public class PayslipDialog extends JDialog {

    private JPanel printablePanel;

    public PayslipDialog(Window parent, Employee employee, PayrollHistoryRecord record) {
        this(parent, employee, record, false);
    }

    public PayslipDialog(Window parent, Employee employee, PayrollHistoryRecord record, boolean autoPrint) {
        super(parent, "Payslip Preview", ModalityType.APPLICATION_MODAL);

        setSize(850, 700);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.BG);

        NumberFormat peso = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(UITheme.BG);
        content.setBorder(new EmptyBorder(20, 24, 20, 24));

        printablePanel = new JPanel(new BorderLayout());
        printablePanel.setBackground(Color.WHITE);
        printablePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                new EmptyBorder(15, 30, 15, 30)
        ));

        JLabel title = new JLabel("MotorPH Payroll Payslip", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(UITheme.TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(10, 10, 20, 10));

        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setBackground(Color.WHITE);
        detailsPanel.setBorder(new EmptyBorder(0, 10, 15, 10));

        JLabel employeeLabel = new JLabel("Employee: " + employee.getFirstName() + " " + employee.getLastName());
        employeeLabel.setFont(UITheme.FONT_BODY);

        JLabel employeeIdLabel = new JLabel("Employee ID: " + employee.getEmployeeId());
        employeeIdLabel.setFont(UITheme.FONT_BODY);

        JLabel cutoffLabel = new JLabel("Cutoff Period: " + record.getCutoffPeriod());
        cutoffLabel.setFont(UITheme.FONT_BODY);

        detailsPanel.add(employeeLabel);
        detailsPanel.add(Box.createVerticalStrut(3));
        detailsPanel.add(employeeIdLabel);
        detailsPanel.add(Box.createVerticalStrut(3));
        detailsPanel.add(cutoffLabel);

        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 20, 8, 20);
        gbc.anchor = GridBagConstraints.WEST;

        addRow(body, gbc, 0, "EARNINGS", "", true, true);
        addRow(body, gbc, 1, "Basic Salary (Semi-Monthly)", peso.format(record.getBasicComponent()), false, true);
        addRow(body, gbc, 2, "Allowance (Semi-Monthly)", peso.format(record.getAllowanceComponent()), false, true);
        addRow(body, gbc, 3, "Gross Pay", peso.format(record.getGross()), true, true);

        addDivider(body, gbc, 4);

        addRow(body, gbc, 5, "DEDUCTIONS", "", true, true);
        addRow(body, gbc, 6, "SSS", peso.format(record.getSss()), false, false);
        addRow(body, gbc, 7, "PhilHealth", peso.format(record.getPhilhealth()), false, false);
        addRow(body, gbc, 8, "Pag-IBIG", peso.format(record.getPagibig()), false, false);
        addRow(body, gbc, 9, "Withholding Tax", peso.format(record.getTax()), false, false);
        addRow(body, gbc, 10, "Total Deductions", peso.format(record.getTotalDeductions()), true, true);

        addDivider(body, gbc, 11);

        addRow(body, gbc, 12, "NET PAY", "", true, true);
        addRow(body, gbc, 13, "Net Salary", peso.format(record.getNet()), true, true);

        JPanel takeHomePanel = new JPanel(new BorderLayout());
        takeHomePanel.setBackground(new Color(245, 245, 245));
        takeHomePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel takeHomeTitle = new JLabel("TAKE HOME PAY", SwingConstants.CENTER);
        takeHomeTitle.setFont(UITheme.FONT_BODY_BOLD);
        takeHomeTitle.setForeground(UITheme.TEXT_PRIMARY);

        JLabel takeHomeValue = new JLabel(peso.format(record.getNet()), SwingConstants.CENTER);
        takeHomeValue.setFont(new Font("Segoe UI", Font.BOLD, 24));
        takeHomeValue.setForeground(new Color(0, 102, 51));

        takeHomePanel.add(takeHomeTitle, BorderLayout.NORTH);
        takeHomePanel.add(takeHomeValue, BorderLayout.CENTER);

        JLabel confidential = new JLabel(
                "<html><center><i>CONFIDENTIAL: This document contains sensitive payroll information intended solely for the employee. Unauthorized disclosure is strictly prohibited.</i></center></html>",
                SwingConstants.CENTER
        );
        confidential.setFont(UITheme.FONT_SMALL);
        confidential.setForeground(UITheme.TEXT_SECONDARY);
        confidential.setBorder(new EmptyBorder(15, 10, 10, 10));

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(Color.WHITE);
        center.add(detailsPanel, BorderLayout.NORTH);
        center.add(body, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(new EmptyBorder(20, 40, 0, 40));
        bottom.add(takeHomePanel, BorderLayout.CENTER);

        JPanel documentBottom = new JPanel(new BorderLayout());
        documentBottom.setBackground(Color.WHITE);
        documentBottom.add(bottom, BorderLayout.NORTH);
        documentBottom.add(confidential, BorderLayout.SOUTH);

        printablePanel.add(title, BorderLayout.NORTH);
        printablePanel.add(center, BorderLayout.CENTER);
        printablePanel.add(documentBottom, BorderLayout.SOUTH);

        content.add(printablePanel, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        actionPanel.setBackground(UITheme.BG);

        JButton closeBtn = UITheme.createButton("Close");
        JButton printBtn = UITheme.createAccentButton("Print");

        closeBtn.setPreferredSize(new Dimension(120, 35));
        printBtn.setPreferredSize(new Dimension(120, 35));

        closeBtn.addActionListener(e -> dispose());
        printBtn.addActionListener(e -> printPayslip());

        actionPanel.add(closeBtn);
        actionPanel.add(printBtn);

        add(content, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.SOUTH);

        if (autoPrint) {
            SwingUtilities.invokeLater(this::printPayslip);
        }
    }

    private void printPayslip() {

        PrinterJob job = PrinterJob.getPrinterJob();

        job.setPrintable((graphics, pageFormat, pageIndex) -> {

            if (pageIndex > 0) {
                return Printable.NO_SUCH_PAGE;
            }

            Graphics2D g2 = (Graphics2D) graphics;

            g2.translate(
                    pageFormat.getImageableX(),
                    pageFormat.getImageableY()
            );

            printablePanel.printAll(g2);

            return Printable.PAGE_EXISTS;
        });

        boolean confirmed = job.printDialog();

        if (confirmed) {
            try {
                job.print();
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Printing failed.",
                        "Print Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int y,
                        String labelText, String valueText,
                        boolean boldLabel, boolean boldValue) {

        JLabel label = new JLabel(labelText);
        JLabel value = new JLabel(valueText);

        label.setFont(new Font("Segoe UI", boldLabel ? Font.BOLD : Font.PLAIN, 13));
        label.setForeground(boldLabel ? UITheme.ACCENT : UITheme.TEXT_SECONDARY);

        value.setFont(new Font("Segoe UI", boldValue ? Font.BOLD : Font.PLAIN, 13));
        value.setForeground(UITheme.TEXT_PRIMARY);

        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(value, gbc);

        gbc.anchor = GridBagConstraints.WEST;
    }

    private void addDivider(JPanel panel, GridBagConstraints gbc, int y) {
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 25, 10, 25);

        panel.add(new JSeparator(), gbc);

        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(8, 20, 8, 20);
    }
}