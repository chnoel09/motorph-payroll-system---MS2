/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.oop.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;
import com.mycompany.oop.model.Employee;
import com.mycompany.oop.service.EmployeeService;
import com.mycompany.oop.service.RoleAccessService;

public class ITPanel extends JPanel {

    private EmployeeService service;
    private RoleAccessService roleAccessService;
    private Employee currentUser;
    private JTable table;

    private JPanel cardsPanel;
    private JPanel content;

    private static final Color[] METRIC_ACCENTS = {
            new Color(210, 43, 43),
            new Color(37, 99, 195),
            new Color(59, 130, 246),
            new Color(34, 160, 70),
            new Color(185, 30, 30),
            new Color(130, 80, 210)
    };

    public ITPanel() {
        this(null);
    }

    public ITPanel(Employee currentUser) {

        this.currentUser = currentUser;
        service = new EmployeeService();
        roleAccessService = new RoleAccessService();

        setLayout(new BorderLayout());
        setBackground(UITheme.BG);

        add(UITheme.createTitleBar("Access Management"), BorderLayout.NORTH);

        content = new JPanel(new BorderLayout());
        content.setBackground(UITheme.BG);
        content.setBorder(new EmptyBorder(16, 20, 16, 20));

        cardsPanel = createDashboardCards();

        content.add(cardsPanel, BorderLayout.NORTH);
        content.add(createUserTable(), BorderLayout.CENTER);
        content.add(createButtonPanel(), BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);
    }

    private JPanel createDashboardCards() {

        JPanel panel = new JPanel(new GridLayout(2, 3, 14, 14));
        panel.setBackground(UITheme.BG);
        panel.setBorder(new EmptyBorder(0, 0, 16, 0));

        Map<String, Integer> counts = service.getUserRoleCounts();

        int total = service.getTotalUsers();
        int admin = counts.getOrDefault("Admin", 0);
        int hr = counts.getOrDefault("HR", 0);
        int finance = counts.getOrDefault("Finance", 0);
        int employee = counts.getOrDefault("Employee", 0);
        int it = counts.getOrDefault("IT", 0);

        panel.add(createCard("Total Accounts", total, 0));
        panel.add(createCard("Admins", admin, 1));
        panel.add(createCard("HR Access", hr, 2));
        panel.add(createCard("Finance Access", finance, 3));
        panel.add(createCard("Employees", employee, 4));
        panel.add(createCard("IT Access", it, 5));

        return panel;
    }

    private JPanel createCard(String title, int value, int accentIndex) {

        Color accent = METRIC_ACCENTS[accentIndex % METRIC_ACCENTS.length];

        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(accent);
                g2.fillRect(0, 0, getWidth(), 3);
                g2.dispose();
            }
        };

        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                new EmptyBorder(16, 18, 14, 18)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UITheme.FONT_CARD_LABEL);
        titleLabel.setForeground(UITheme.TEXT_SECONDARY);

        JLabel valueLabel = new JLabel(String.valueOf(value));
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(UITheme.TEXT_PRIMARY);
        valueLabel.setBorder(new EmptyBorder(6, 0, 0, 0));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private JScrollPane createUserTable() {

        table = new JTable();
        UITheme.styleTable(table);

        refreshTable();

        return UITheme.createTableScrollPane(table);
    }

    private void refreshTable() {

        List<Employee> list = service.getAllEmployees();

        String[] columns = {"Employee ID", "Username", "Role", "Status"};
        Object[][] data = new Object[list.size()][4];

        for (int i = 0; i < list.size(); i++) {
            Employee e = list.get(i);
            data[i][0] = e.getEmployeeId();
            data[i][1] = e.getUsername();
            data[i][2] = e.getRole();
            data[i][3] = e.getEmploymentStatus();
        }

        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table.setModel(model);
    }

    private void refreshPanel() {
        refreshTable();

        content.remove(cardsPanel);
        cardsPanel = createDashboardCards();
        content.add(cardsPanel, BorderLayout.NORTH);

        content.revalidate();
        content.repaint();
    }

    private JPanel createButtonPanel() {

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        panel.setBackground(UITheme.BG);

        JButton resetBtn = UITheme.createButton("Reset Password");
        JButton roleBtn = UITheme.createAccentButton("Change Role");

        resetBtn.setPreferredSize(new Dimension(160, 36));
        roleBtn.setPreferredSize(new Dimension(160, 36));

        resetBtn.addActionListener(e -> resetPassword());
        roleBtn.addActionListener(e -> changeRole());

        resetBtn.setEnabled(roleAccessService.canResetUserPassword(getCurrentRole()));
        roleBtn.setEnabled(roleAccessService.canChangeUserRole(getCurrentRole()));

        panel.add(resetBtn);
        panel.add(roleBtn);

        return panel;
    }

    private void resetPassword() {
        if (!roleAccessService.canResetUserPassword(getCurrentRole())) {
            showAccessDeniedMessage("Only IT and Admin users can reset account passwords.");
            return;
        }

        Employee selectedEmployee = getSelectedEmployee();
        if (selectedEmployee == null) {
            return;
        }

        JPasswordField passwordField = new JPasswordField();
        JPasswordField confirmPasswordField = new JPasswordField();
        passwordField.setFont(UITheme.FONT_BODY);
        confirmPasswordField.setFont(UITheme.FONT_BODY);

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 6));
        form.add(new JLabel("New password for " + safeText(selectedEmployee.getUsername(), "selected user") + ":"));
        form.add(passwordField);
        form.add(new JLabel("Confirm new password:"));
        form.add(confirmPasswordField);

        int inputResult = JOptionPane.showConfirmDialog(
                this,
                form,
                "Reset Password",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (inputResult != JOptionPane.OK_OPTION) {
            return;
        }

        String newPassword = new String(passwordField.getPassword()).trim();
        String confirmPassword = new String(confirmPasswordField.getPassword()).trim();

        if (newPassword.isEmpty()) {
            showValidationMessage("New password is required.");
            return;
        }

        if (newPassword.length() < 4) {
            showValidationMessage("New password must be at least 4 characters long.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showValidationMessage("Password confirmation does not match.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Reset the password for " + safeText(selectedEmployee.getUsername(), "this user") + "?",
                "Confirm Password Reset",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        boolean success = service.resetPassword(selectedEmployee.getEmployeeId(), newPassword);

        if (success) {
            JOptionPane.showMessageDialog(this,
                    "Password reset successfully.",
                    "Password Reset",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshPanel();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Password reset failed because the selected user could not be found.",
                    "Password Reset Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void changeRole() {
        if (!roleAccessService.canChangeUserRole(getCurrentRole())) {
            showAccessDeniedMessage("Only Admin users can change account roles.");
            return;
        }

        Employee selectedEmployee = getSelectedEmployee();
        if (selectedEmployee == null) {
            return;
        }

        if (isCurrentUser(selectedEmployee)) {
            JOptionPane.showMessageDialog(this,
                    "You cannot change your own role from this screen.",
                    "Role Change Restricted",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String newRole = (String) JOptionPane.showInputDialog(
                this,
                "Select a new role for " + safeText(selectedEmployee.getUsername(), "this user") + ":",
                "Change Role",
                JOptionPane.PLAIN_MESSAGE,
                null,
                new String[]{"Admin", "HR", "Finance", "Employee", "IT"},
                safeText(selectedEmployee.getRole(), "Employee")
        );

        if (newRole == null) {
            return;
        }

        String oldRole = safeText(selectedEmployee.getRole(), "");
        if (oldRole.equalsIgnoreCase(newRole.trim())) {
            JOptionPane.showMessageDialog(this,
                    "No role change was made because the selected role is already assigned.",
                    "No Change",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Change " + safeText(selectedEmployee.getUsername(), "this user")
                        + " from " + oldRole + " to " + newRole + "?",
                "Confirm Role Change",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        boolean success = service.changeRole(selectedEmployee.getEmployeeId(), newRole);

        if (success) {
            JOptionPane.showMessageDialog(this,
                    "User role changed successfully.",
                    "Role Updated",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshPanel();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Role change failed because the selected user could not be found.",
                    "Role Change Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private Employee getSelectedEmployee() {
        int row = table.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a user account first.",
                    "No User Selected",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }

        int id = Integer.parseInt(table.getValueAt(row, 0).toString());
        Employee employee = service.findById(id);

        if (employee == null) {
            JOptionPane.showMessageDialog(this,
                    "Selected user account could not be found.",
                    "User Not Found",
                    JOptionPane.ERROR_MESSAGE);
        }

        return employee;
    }

    private boolean isCurrentUser(Employee employee) {
        return currentUser != null
                && employee != null
                && currentUser.getEmployeeId() == employee.getEmployeeId();
    }

    private String getCurrentRole() {
        return currentUser == null ? "" : currentUser.getRole();
    }

    private void showAccessDeniedMessage(String message) {
        JOptionPane.showMessageDialog(this,
                message,
                "Access Restricted",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showValidationMessage(String message) {
        JOptionPane.showMessageDialog(this,
                message,
                "Validation Error",
                JOptionPane.WARNING_MESSAGE);
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
