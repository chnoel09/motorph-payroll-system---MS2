/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.oop.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;
import com.mycompany.oop.model.Employee;
import com.mycompany.oop.service.EmployeeService;

public class EmployeePanel extends JPanel {

    private Employee employee;

    public EmployeePanel(Employee employee) {
        this.employee = loadEmployeeProfile(employee);

        setLayout(new BorderLayout());
        setBackground(UITheme.BG);

        JPanel content = UITheme.createWorkspacePanel(new BorderLayout(0, 12));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setOpaque(false);

        NumberFormat peso = NumberFormat.getCurrencyInstance(
                new Locale("en", "PH")
        );
        String reportsTo = resolveReportsToLabel(this.employee);

        mainPanel.add(createHeaderCard());
        mainPanel.add(Box.createVerticalStrut(12));
        mainPanel.add(createSectionCard("Personal Information", new String[][]{
                {"Employee ID", String.valueOf(this.employee.getEmployeeId())},
                {"Name", this.employee.getFirstName() + " " + this.employee.getLastName()},
                {"Birthday", safe(this.employee.getBirthday())},
                {"Address", safe(this.employee.getAddress())},
                {"Phone Number", safe(this.employee.getPhoneNumber())},
                {"Email", safe(this.employee.getEmail())}
        }));

        mainPanel.add(Box.createVerticalStrut(12));

        mainPanel.add(createSectionCard("Employment Details", new String[][]{
                {"Position", safe(this.employee.getPosition())},
                {"Status", safe(this.employee.getEmploymentStatus())},
                {"Role", safe(this.employee.getRole())},
                {"Username", safe(this.employee.getUsername())},
                {"Reporting Supervisor", reportsTo}
        }));

        mainPanel.add(Box.createVerticalStrut(12));

        mainPanel.add(createSectionCard("Government Information", new String[][]{
                {"SSS Number", safe(this.employee.getSssNumber())},
                {"PhilHealth Number", safe(this.employee.getPhilhealthNumber())},
                {"TIN Number", safe(this.employee.getTinNumber())},
                {"Pag-IBIG Number", safe(this.employee.getPagibigNumber())}
        }));

        mainPanel.add(Box.createVerticalStrut(12));

        mainPanel.add(createSectionCard("Compensation Details", new String[][]{
                {"Basic Salary", peso.format(this.employee.getBasicSalary())},
                {"Rice Subsidy", peso.format(this.employee.getRiceSubsidy())},
                {"Phone Allowance", peso.format(this.employee.getPhoneAllowance())},
                {"Clothing Allowance", peso.format(this.employee.getClothingAllowance())},
                {"Total Allowance", peso.format(this.employee.getAllowance())},
                {"Gross Semi-Monthly Rate", peso.format(this.employee.getGrossSemiMonthlyRate())},
                {"Hourly Rate", peso.format(this.employee.getHourlyRate())}
        }, true));

        content.add(mainPanel, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
    }

    private JPanel createHeaderCard() {
        JPanel card = createCardPanel(new BorderLayout(12, 0));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel label = new JLabel("My Profile");
        label.setFont(new Font("Segoe UI", Font.BOLD, 22));
        label.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Employee details and compensation profile");
        subtitle.setFont(UITheme.FONT_SMALL);
        subtitle.setForeground(UITheme.TEXT_SECONDARY);

        text.add(label);
        text.add(Box.createVerticalStrut(4));
        text.add(subtitle);

        JLabel badge = createRoleBadge();

        card.add(text, BorderLayout.CENTER);
        card.add(badge, BorderLayout.EAST);
        return card;
    }

    private JPanel createSectionCard(String title, String[][] rows) {
        return createSectionCard(title, rows, false);
    }

    private JPanel createSectionCard(String title, String[][] rows, boolean rightAlignValues) {
        JPanel section = createCardPanel(new BorderLayout(0, 10));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(UITheme.FONT_SECTION);
        lblTitle.setForeground(UITheme.TEXT_PRIMARY);

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);

        for (int i = 0; i < rows.length; i++) {
            addInfoRow(body, rows[i][0], rows[i][1], i, rightAlignValues);
        }

        section.add(lblTitle, BorderLayout.NORTH);
        section.add(body, BorderLayout.CENTER);

        return section;
    }

    private JPanel createCardPanel(LayoutManager layout) {
        JPanel card = new JPanel(layout);
        card.setBackground(UITheme.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                new EmptyBorder(14, 18, 14, 18)
        ));
        return card;
    }

    private void addInfoRow(JPanel body, String label, String value, int rowIndex, boolean rightAlignValue) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = rowIndex;
        labelConstraints.weightx = 0;
        labelConstraints.anchor = GridBagConstraints.NORTHWEST;
        labelConstraints.insets = new Insets(rowIndex == 0 ? 0 : 6, 0, 0, 18);

        JLabel lbl = new JLabel(label);
        lbl.setFont(UITheme.FONT_BODY);
        lbl.setForeground(UITheme.TEXT_SECONDARY);
        lbl.setPreferredSize(new Dimension(170, 22));

        GridBagConstraints valueConstraints = new GridBagConstraints();
        valueConstraints.gridx = 1;
        valueConstraints.gridy = rowIndex;
        valueConstraints.weightx = 1;
        valueConstraints.fill = GridBagConstraints.HORIZONTAL;
        valueConstraints.anchor = GridBagConstraints.NORTHWEST;
        valueConstraints.insets = new Insets(rowIndex == 0 ? 0 : 6, 0, 0, 0);

        body.add(lbl, labelConstraints);
        body.add(createValueLabel(value, rightAlignValue), valueConstraints);
    }

    private JLabel createValueLabel(String value, boolean rightAlign) {
        JLabel val = new JLabel(value != null && !value.isBlank() ? value : "N/A");
        val.setFont(UITheme.FONT_BODY_BOLD);
        val.setForeground(UITheme.TEXT_PRIMARY);
        val.setVerticalAlignment(SwingConstants.TOP);
        val.setHorizontalAlignment(rightAlign ? SwingConstants.RIGHT : SwingConstants.LEFT);
        return val;
    }

    private JLabel createRoleBadge() {
        JLabel badge = new JLabel(safe(employee.getRole()));
        badge.setFont(UITheme.FONT_BODY_BOLD);
        badge.setForeground(UITheme.ACCENT);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(238, 242, 247), 1),
                new EmptyBorder(7, 12, 7, 12)
        ));
        badge.setOpaque(true);
        badge.setBackground(new Color(248, 250, 252));
        return badge;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension preferred = super.getPreferredSize();
        return new Dimension(Math.max(preferred.width, 760), preferred.height);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    private Employee loadEmployeeProfile(Employee sessionEmployee) {
        if (sessionEmployee == null || sessionEmployee.getEmployeeId() <= 0) {
            return sessionEmployee;
        }
        try {
            Employee profile = new EmployeeService().findById(sessionEmployee.getEmployeeId());
            return profile == null ? sessionEmployee : profile;
        } catch (RuntimeException ex) {
            return sessionEmployee;
        }
    }

    private String resolveReportsToLabel(Employee employee) {
        if (employee == null || employee.getSupervisorEmployeeId() == null) {
            return "Unassigned";
        }

        try {
            Employee supervisor = new EmployeeService().findById(employee.getSupervisorEmployeeId());
            if (supervisor == null) {
                return "Employee #" + employee.getSupervisorEmployeeId();
            }

            return supervisor.getFirstName() + " " + supervisor.getLastName()
                    + " (#" + supervisor.getEmployeeId() + ")";
        } catch (Exception ex) {
            return "Employee #" + employee.getSupervisorEmployeeId();
        }
    }
}
