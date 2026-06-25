package com.mycompany.oop.view;

import com.mycompany.oop.model.Employee;
import com.mycompany.oop.service.EmployeeService;
import com.mycompany.oop.service.RoleAccessService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OrganizationChartPanel extends JPanel implements RefreshablePanel {

    private final Employee currentUser;
    private final EmployeeService employeeService;
    private final RoleAccessService roleAccessService;

    private JPanel chartBody;
    private JComboBox<EmployeeOption> employeeSelector;
    private List<Employee> employees;
    private Employee focusedEmployee;

    public OrganizationChartPanel(Employee currentUser) {
        this.currentUser = currentUser;
        this.employeeService = new EmployeeService();
        this.roleAccessService = new RoleAccessService();
        this.focusedEmployee = currentUser;

        setLayout(new BorderLayout());
        setBackground(UITheme.BG);
        add(UITheme.createTitleBar("Org Chart"), BorderLayout.NORTH);
        add(createContent(), BorderLayout.CENTER);
    }

    private JPanel createContent() {
        employees = employeeService.getAllEmployees();

        JPanel content = UITheme.createWorkspacePanel(new BorderLayout(0, 16));
        content.add(createHeader(), BorderLayout.NORTH);

        chartBody = new JPanel();
        chartBody.setOpaque(false);
        chartBody.setLayout(new BoxLayout(chartBody, BoxLayout.Y_AXIS));
        rebuildChart();

        JScrollPane scrollPane = new JScrollPane(chartBody);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UITheme.BG);
        WorkforceFormToolkit.tuneScrollPane(scrollPane);
        content.add(scrollPane, BorderLayout.CENTER);
        return content;
    }

    private JPanel createHeader() {
        JPanel header = WorkforceFormToolkit.createSection(
                "Organization Structure",
                "Reporting hierarchy based on each employee's assigned Reports To relationship."
        );

        if (roleAccessService.isHR(currentUser.getRole())) {
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            controls.setOpaque(false);

            employeeSelector = new JComboBox<>();
            WorkforceFormToolkit.styleComboBox(employeeSelector);
            employeeSelector.setPreferredSize(new Dimension(320, 38));
            EmployeeOption[] options = getEmployeeOptions();
            employeeSelector.setModel(new DefaultComboBoxModel<>(options));
            selectFocusedEmployee(options);
            employeeSelector.addActionListener(e -> {
                EmployeeOption option = (EmployeeOption) employeeSelector.getSelectedItem();
                if (option != null) {
                    focusedEmployee = option.employee;
                    rebuildChart();
                }
            });

            controls.add(new JLabel("Inspect employee"));
            controls.add(employeeSelector);
            header.add(controls, BorderLayout.SOUTH);
        }

        return header;
    }

    private EmployeeOption[] getEmployeeOptions() {
        return employees.stream()
                .sorted(Comparator.comparingInt(Employee::getEmployeeId))
                .map(EmployeeOption::new)
                .toArray(EmployeeOption[]::new);
    }

    private void rebuildChart() {
        if (chartBody == null) {
            return;
        }

        chartBody.removeAll();
        Employee focus = focusedEmployee == null ? currentUser : focusedEmployee;
        boolean cycleDetected = hasReportingCycle(focus);
        Employee supervisor = employeeService.findSupervisorFor(focus, employees);
        List<Employee> directReports = employeeService.findDirectReports(focus, employees);

        chartBody.add(createSummaryPanel(focus, directReports));
        chartBody.add(Box.createVerticalStrut(8));
        if (cycleDetected) {
            chartBody.add(createWarningNote("Invalid reporting assignment: this would create a circular reporting relationship."));
            chartBody.add(Box.createVerticalStrut(8));
        }

        if (supervisor != null && !cycleDetected) {
            chartBody.add(createCenteredCard(supervisor, "Reporting Supervisor", CardTone.SUPERVISOR, false));
            chartBody.add(createConnector("Reports to", "↑"));
        } else if (directReports.isEmpty() || cycleDetected) {
            chartBody.add(createEmptyNote("No reporting supervisor assigned."));
            chartBody.add(Box.createVerticalStrut(12));
        }

        chartBody.add(createCenteredCard(focus, isCurrentUser(focus) ? "You" : "Selected Employee", CardTone.CURRENT, isCurrentUser(focus)));

        if (!directReports.isEmpty()) {
            chartBody.add(createConnector("Direct Reports", "↓"));
            chartBody.add(createReportsGrid(directReports));
        } else {
            chartBody.add(Box.createVerticalStrut(12));
            chartBody.add(createEmptyNote(isCurrentUser(focus)
                    ? "No direct reports assigned."
                    : "No direct reports assigned to this employee."));
        }

        chartBody.revalidate();
        chartBody.repaint();
    }

    private JPanel createSummaryPanel(Employee focus, List<Employee> directReports) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));

        JPanel metrics = new JPanel(new GridLayout(1, 3, 12, 0));
        metrics.setOpaque(false);
        metrics.add(createMetric("Selected", fullName(focus)));
        metrics.add(createMetric("Direct Reports", String.valueOf(directReports.size())));
        metrics.add(createMetric("Departments", employeeService.getDepartmentName(focus)));
        panel.add(metrics, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createMetric(String label, String value) {
        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)
        ));

        JLabel title = new JLabel(label);
        title.setFont(UITheme.FONT_SMALL);
        title.setForeground(UITheme.TEXT_SECONDARY);
        JLabel number = new JLabel(value);
        number.setFont(UITheme.FONT_BODY_BOLD);
        number.setForeground(UITheme.TEXT_PRIMARY);
        panel.add(title, BorderLayout.NORTH);
        panel.add(number, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCenteredCard(Employee employee, String chip, CardTone tone, boolean current) {
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        wrap.setOpaque(false);
        wrap.add(createEmployeeCard(employee, chip, tone, current));
        return wrap;
    }

    private JPanel createReportsGrid(List<Employee> reports) {
        JPanel grid = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        grid.setOpaque(false);
        for (Employee employee : reports) {
            grid.add(createEmployeeCard(employee, "Direct Report", CardTone.REPORT, isCurrentUser(employee)));
        }
        return grid;
    }

    private JPanel createEmployeeCard(Employee employee, String chip, CardTone tone, boolean current) {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(Color.WHITE);
        card.setPreferredSize(new Dimension(330, 108));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(tone.border, 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        JLabel avatar = new JLabel(initials(employee), SwingConstants.CENTER);
        avatar.setOpaque(true);
        avatar.setBackground(tone.avatar);
        avatar.setForeground(Color.WHITE);
        avatar.setFont(UITheme.FONT_BODY_BOLD);
        avatar.setPreferredSize(new Dimension(48, 48));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(fullName(employee));
        name.setFont(UITheme.FONT_BODY_BOLD);
        name.setForeground(UITheme.TEXT_PRIMARY);
        JLabel position = new JLabel(safe(employee.getPosition(), "Position not set"));
        position.setFont(UITheme.FONT_SMALL);
        position.setForeground(UITheme.TEXT_SECONDARY);
        JLabel details = new JLabel("Employee ID " + employee.getEmployeeId());
        details.setFont(UITheme.FONT_SMALL);
        details.setForeground(UITheme.TEXT_SECONDARY);
        JLabel department = new JLabel("Department: " + employeeService.getDepartmentName(employee));
        department.setFont(UITheme.FONT_SMALL);
        department.setForeground(UITheme.TEXT_SECONDARY);

        JPanel chipRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        chipRow.setOpaque(false);
        chipRow.add(WorkforceFormToolkit.createStatusChip(chip, tone.border));

        text.add(name);
        text.add(Box.createVerticalStrut(4));
        text.add(position);
        text.add(Box.createVerticalStrut(4));
        text.add(details);
        text.add(Box.createVerticalStrut(3));
        text.add(department);
        text.add(Box.createVerticalStrut(7));
        text.add(chipRow);

        card.add(avatar, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private JPanel createConnector(String label, String arrow) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel line = new JLabel(arrow, SwingConstants.CENTER);
        line.setAlignmentX(CENTER_ALIGNMENT);
        line.setFont(UITheme.FONT_SECTION);
        line.setForeground(UITheme.TEXT_SECONDARY);
        JLabel caption = new JLabel(label, SwingConstants.CENTER);
        caption.setAlignmentX(CENTER_ALIGNMENT);
        caption.setFont(UITheme.FONT_SMALL);
        caption.setForeground(UITheme.TEXT_SECONDARY);
        panel.add(Box.createVerticalStrut(3));
        panel.add(line);
        panel.add(caption);
        panel.add(Box.createVerticalStrut(3));
        return panel;
    }

    private JPanel createEmptyNote(String message) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        panel.setOpaque(false);
        JLabel label = new JLabel(message);
        label.setFont(UITheme.FONT_BODY);
        label.setForeground(UITheme.TEXT_SECONDARY);
        panel.add(label);
        return panel;
    }

    private JPanel createWarningNote(String message) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        panel.setOpaque(false);
        JLabel label = WorkforceFormToolkit.createStatusChip(message, UITheme.DANGER);
        panel.add(label);
        return panel;
    }

    private boolean hasReportingCycle(Employee focus) {
        if (focus == null || focus.getEmployeeId() <= 0) {
            return false;
        }
        Set<Integer> visited = new HashSet<>();
        Integer supervisorId = focus.getSupervisorEmployeeId();
        while (supervisorId != null) {
            if (!visited.add(supervisorId)) {
                return true;
            }
            if (supervisorId == focus.getEmployeeId()) {
                return true;
            }
            Employee supervisor = findEmployeeInCache(supervisorId);
            if (supervisor == null) {
                return false;
            }
            supervisorId = supervisor.getSupervisorEmployeeId();
        }
        return false;
    }

    private Employee findEmployeeInCache(int employeeId) {
        if (employees == null) {
            return null;
        }
        for (Employee employee : employees) {
            if (employee != null && employee.getEmployeeId() == employeeId) {
                return employee;
            }
        }
        return null;
    }

    @Override
    public void refreshData() {
        employees = employeeService.getAllEmployees();
        if (roleAccessService.isHR(currentUser.getRole()) && employeeSelector != null) {
            EmployeeOption[] options = getEmployeeOptions();
            employeeSelector.setModel(new DefaultComboBoxModel<>(options));
            selectFocusedEmployee(options);
        }
        rebuildChart();
    }

    private void selectFocusedEmployee(EmployeeOption[] options) {
        if (focusedEmployee == null || employeeSelector == null || options == null) {
            return;
        }
        for (EmployeeOption option : options) {
            if (option.employee != null && option.employee.getEmployeeId() == focusedEmployee.getEmployeeId()) {
                employeeSelector.setSelectedItem(option);
                return;
            }
        }
    }

    private boolean isCurrentUser(Employee employee) {
        return employee != null && currentUser != null && employee.getEmployeeId() == currentUser.getEmployeeId();
    }

    private String fullName(Employee employee) {
        if (employee == null) {
            return "Unknown employee";
        }
        return (safe(employee.getFirstName(), "") + " " + safe(employee.getLastName(), "")).trim();
    }

    private String initials(Employee employee) {
        String first = safe(employee == null ? null : employee.getFirstName(), "");
        String last = safe(employee == null ? null : employee.getLastName(), "");
        String value = (first.isEmpty() ? "" : first.substring(0, 1))
                + (last.isEmpty() ? "" : last.substring(0, 1));
        return value.isEmpty() ? "E" : value.toUpperCase();
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private enum CardTone {
        SUPERVISOR(new Color(37, 99, 235), new Color(59, 130, 246)),
        CURRENT(new Color(14, 165, 233), new Color(20, 184, 166)),
        REPORT(new Color(22, 163, 74), new Color(34, 197, 94));

        private final Color border;
        private final Color avatar;

        CardTone(Color border, Color avatar) {
            this.border = border;
            this.avatar = avatar;
        }
    }

    private static class EmployeeOption {
        private final Employee employee;

        private EmployeeOption(Employee employee) {
            this.employee = employee;
        }

        @Override
        public String toString() {
            return employee == null
                    ? "Unknown employee"
                    : "ID " + employee.getEmployeeId() + " • "
                    + employee.getLastName() + ", " + employee.getFirstName()
                    + " • " + (employee.getPosition() == null || employee.getPosition().isBlank()
                    ? "Employee"
                    : employee.getPosition());
        }
    }
}
