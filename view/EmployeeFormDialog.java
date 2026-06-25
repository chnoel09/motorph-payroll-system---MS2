/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.oop.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.RegularEmployee;
import com.mycompany.oop.service.EmployeeService;
import com.mycompany.oop.util.PasswordUtil;

public class EmployeeFormDialog extends JDialog {

    private JTextField idField, firstNameField, lastNameField, positionField;
    private JTextField statusField, basicSalaryField, allowanceField, hourlyRateField;

    private JTextField birthdayField, addressField, phoneNumberField, emailField;
    private JTextField riceSubsidyField, phoneAllowanceField, clothingAllowanceField, grossSemiMonthlyRateField;
    private JTextField sssField, philhealthField, tinField, pagibigField;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleBox;
    private JComboBox<DepartmentOption> departmentBox;
    private JComboBox<SupervisorOption> reportsToBox;

    private EmployeeService service;
    private boolean isAdmin;
    private boolean readOnly;
    private Employee existingEmployee;

    public EmployeeFormDialog(JFrame parent,
                              EmployeeService service,
                              Employee employee,
                              boolean isAdmin,
                              boolean readOnly) {

        super(parent, true);
        this.service = service;
        this.isAdmin = isAdmin;
        this.readOnly = readOnly;
        this.existingEmployee = employee;

        String title;
        if (readOnly) {
            title = "View Employee";
        } else if (employee == null) {
            title = "Add Employee";
        } else {
            title = "Edit Employee";
        }

        setTitle(title);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UITheme.BG);

        add(UITheme.createTitleBar(getTitle()), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(UITheme.BG);
        content.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel formPanel = new JPanel(new GridLayout(0, 2, 14, 10));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                new EmptyBorder(18, 20, 18, 20)
        ));

        idField = createField("Employee ID:", formPanel);
        firstNameField = createField("First Name:", formPanel);
        lastNameField = createField("Last Name:", formPanel);

        birthdayField = createFieldWithHelp("Birthday:", "Date format: yyyy-MM-dd", formPanel);
        WorkforceFormToolkit.applyDateHelp(birthdayField);
        addLiveBirthdayValidation(birthdayField);

        addressField = createField("Address:", formPanel);
        phoneNumberField = createField("Phone Number:", formPanel);
        addLivePhoneValidation(phoneNumberField);
        
        emailField = createField("Email:", formPanel);
        positionField = createField("Position:", formPanel);
        statusField = createField("Employment Status:", formPanel);
        departmentBox = createDepartmentCombo("Department:", formPanel);
        reportsToBox = createSupervisorCombo("Reports To:", formPanel);

        basicSalaryField = createField("Basic Salary:", formPanel);
        addLiveNumericValidation(basicSalaryField, "Basic Salary");

        riceSubsidyField = createField("Rice Subsidy:", formPanel);
        addLiveNumericValidation(riceSubsidyField, "Rice Subsidy");

        phoneAllowanceField = createField("Phone Allowance:", formPanel);
        addLiveNumericValidation(phoneAllowanceField, "Phone Allowance");

        clothingAllowanceField = createField("Clothing Allowance:", formPanel);
        addLiveNumericValidation(clothingAllowanceField, "Clothing Allowance");

        allowanceField = createField("Total Allowance:", formPanel);
        allowanceField.setEditable(false);
        allowanceField.setBackground(new Color(235, 235, 235));

        grossSemiMonthlyRateField = createField("Gross Semi-Monthly Rate:", formPanel);
        grossSemiMonthlyRateField.setEditable(false);
        grossSemiMonthlyRateField.setBackground(new Color(235, 235, 235));

        hourlyRateField = createField("Hourly Rate:", formPanel);
        addLiveNumericValidation(hourlyRateField, "Hourly Rate");

        DocumentListener computedFieldListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updateComputedFields();
            }

            public void removeUpdate(DocumentEvent e) {
                updateComputedFields();
            }

            public void changedUpdate(DocumentEvent e) {
                updateComputedFields();
            }
        };

        riceSubsidyField.getDocument().addDocumentListener(computedFieldListener);
        phoneAllowanceField.getDocument().addDocumentListener(computedFieldListener);
        clothingAllowanceField.getDocument().addDocumentListener(computedFieldListener);
        basicSalaryField.getDocument().addDocumentListener(computedFieldListener);

        sssField = createField("SSS Number:", formPanel);
        addLiveDigitsValidation(sssField, "SSS Number");

        philhealthField = createField("PhilHealth Number:", formPanel);
        addLiveDigitsValidation(philhealthField, "PhilHealth Number");

        tinField = createField("TIN Number:", formPanel);
        addLiveDigitsValidation(tinField, "TIN Number");

        pagibigField = createField("Pag-IBIG Number:", formPanel);
        addLiveDigitsValidation(pagibigField, "Pag-IBIG Number");

        if (isAdmin) {
            usernameField = createField("Username:", formPanel);

            formPanel.add(createLabel("Password:"));
            passwordField = new JPasswordField();
            styleField(passwordField);
            formPanel.add(passwordField);

            formPanel.add(createLabel("Role:"));
            roleBox = new JComboBox<>(new String[]{
                    "Admin", "HR", "Finance", "Employee", "IT", "Supervisor", "Manager"
            });
            WorkforceFormToolkit.styleComboBox(roleBox);
            formPanel.add(roleBox);
        }

        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(UITheme.BG);
        WorkforceFormToolkit.tuneScrollPane(scrollPane);

        content.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(UITheme.BG);

        JButton cancelBtn = UITheme.createFormButton(readOnly ? "Close" : "Cancel");
        JButton saveBtn = UITheme.createAccentButton("Save");

        cancelBtn.setPreferredSize(new Dimension(90, 34));
        saveBtn.setPreferredSize(new Dimension(90, 34));

        cancelBtn.addActionListener(e -> dispose());
        saveBtn.addActionListener(e -> saveEmployee());

        buttonPanel.add(cancelBtn);

        if (!readOnly) {
            buttonPanel.add(saveBtn);
        }

        content.add(buttonPanel, BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);

        if (employee != null) {
            loadDepartmentOptions(employee.getDepartmentId());
            loadSupervisorOptions(employee.getEmployeeId(), service.resolveSupervisorEmployeeId(employee));
            populateFields(employee);
            updateComputedFields();
            idField.setEditable(false);
            idField.setBackground(new Color(235, 235, 235));
        } else {
            int nextEmployeeId = service.getNextEmployeeId();
            loadDepartmentOptions(null);
            loadSupervisorOptions(nextEmployeeId, null);
            idField.setText(String.valueOf(nextEmployeeId));
            idField.setEditable(false);
            idField.setBackground(new Color(235, 235, 235));
            updateComputedFields();
        }

        if (readOnly) {
            setFieldsEditable(false);
        }

        setSize(720, 650);
        setLocationRelativeTo(parent);
    }

    private void setFieldsEditable(boolean editable) {
        idField.setEditable(false);
        firstNameField.setEditable(editable);
        lastNameField.setEditable(editable);
        birthdayField.setEditable(editable);
        addressField.setEditable(editable);
        phoneNumberField.setEditable(editable);
        emailField.setEditable(editable);
        positionField.setEditable(editable);
        statusField.setEditable(editable);
        basicSalaryField.setEditable(editable);
        riceSubsidyField.setEditable(editable);
        phoneAllowanceField.setEditable(editable);
        clothingAllowanceField.setEditable(editable);
        hourlyRateField.setEditable(editable);
        sssField.setEditable(editable);
        philhealthField.setEditable(editable);
        tinField.setEditable(editable);
        pagibigField.setEditable(editable);

        allowanceField.setEditable(false);
        grossSemiMonthlyRateField.setEditable(false);

        if (usernameField != null) usernameField.setEditable(editable);
        if (passwordField != null) passwordField.setEditable(editable);
        if (roleBox != null) roleBox.setEnabled(editable);
        if (departmentBox != null) departmentBox.setEnabled(editable);
        if (reportsToBox != null) reportsToBox.setEnabled(editable);

        applyReadOnlyStyle(firstNameField, editable);
        applyReadOnlyStyle(lastNameField, editable);
        applyReadOnlyStyle(birthdayField, editable);
        applyReadOnlyStyle(addressField, editable);
        applyReadOnlyStyle(phoneNumberField, editable);
        applyReadOnlyStyle(emailField, editable);
        applyReadOnlyStyle(positionField, editable);
        applyReadOnlyStyle(statusField, editable);
        applyReadOnlyStyle(basicSalaryField, editable);
        applyReadOnlyStyle(riceSubsidyField, editable);
        applyReadOnlyStyle(phoneAllowanceField, editable);
        applyReadOnlyStyle(clothingAllowanceField, editable);
        applyReadOnlyStyle(hourlyRateField, editable);
        applyReadOnlyStyle(sssField, editable);
        applyReadOnlyStyle(philhealthField, editable);
        applyReadOnlyStyle(tinField, editable);
        applyReadOnlyStyle(pagibigField, editable);

        if (usernameField != null) applyReadOnlyStyle(usernameField, editable);
        if (passwordField != null) {
            passwordField.setBackground(editable ? Color.WHITE : new Color(235, 235, 235));
        }
    }

    private void applyReadOnlyStyle(JTextField field, boolean editable) {
        field.setBackground(editable ? Color.WHITE : new Color(235, 235, 235));
    }

    private void updateComputedFields() {
        try {
            double rice = parseDoubleSafe(riceSubsidyField.getText());
            double phone = parseDoubleSafe(phoneAllowanceField.getText());
            double clothing = parseDoubleSafe(clothingAllowanceField.getText());
            double basicSalary = parseDoubleSafe(basicSalaryField.getText());

            double totalAllowance = rice + phone + clothing;
            double grossSemiMonthly = (basicSalary + totalAllowance) / 2.0;

            allowanceField.setText(String.valueOf(totalAllowance));
            grossSemiMonthlyRateField.setText(String.valueOf(grossSemiMonthly));

        } catch (Exception e) {
            allowanceField.setText("0");
            grossSemiMonthlyRateField.setText("0");
        }
    }

    private double parseDoubleSafe(String value) {
        if (value == null || value.trim().isEmpty()) return 0;

        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private JTextField createField(String labelText, JPanel panel) {
        panel.add(createLabel(labelText));

        JTextField field = new JTextField();
        styleField(field);
        panel.add(field);

        return field;
    }

    private JTextField createFieldWithHelp(String labelText, String helpText, JPanel panel) {
        panel.add(createLabel(labelText));

        JTextField field = new JTextField();
        styleField(field);
        panel.add(WorkforceFormToolkit.createFieldStack(field, helpText));

        return field;
    }

    private JComboBox<SupervisorOption> createSupervisorCombo(String labelText, JPanel panel) {
        panel.add(createLabel(labelText));

        JComboBox<SupervisorOption> comboBox = new JComboBox<>();
        WorkforceFormToolkit.styleComboBox(comboBox);
        comboBox.setMaximumRowCount(12);
        comboBox.setToolTipText("Sorted HR-managed reporting line. The employee cannot report to themselves.");
        panel.add(WorkforceFormToolkit.createFieldStack(comboBox, "Sorted reporting line for team operations and org chart."));

        return comboBox;
    }

    private JComboBox<DepartmentOption> createDepartmentCombo(String labelText, JPanel panel) {
        panel.add(createLabel(labelText));

        JComboBox<DepartmentOption> comboBox = new JComboBox<>();
        WorkforceFormToolkit.styleComboBox(comboBox);
        comboBox.setMaximumRowCount(10);
        comboBox.setToolTipText("Loaded from the departments table.");
        panel.add(WorkforceFormToolkit.createFieldStack(comboBox, "Select Not assigned if department is not yet set."));

        return comboBox;
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.FONT_BODY_BOLD);
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        return lbl;
    }

    private void styleField(JTextField field) {
        WorkforceFormToolkit.styleTextField(field);
    }

    private void populateFields(Employee e) {
        idField.setText(String.valueOf(e.getEmployeeId()));
        firstNameField.setText(e.getFirstName());
        lastNameField.setText(e.getLastName());
        birthdayField.setText(e.getBirthday());
        addressField.setText(e.getAddress());
        phoneNumberField.setText(e.getPhoneNumber());
        emailField.setText(e.getEmail());
        positionField.setText(e.getPosition());
        statusField.setText(e.getEmploymentStatus());
        selectDepartmentOption(e.getDepartmentId());
        selectSupervisorOption(e.getSupervisorEmployeeId());

        basicSalaryField.setText(String.valueOf(e.getBasicSalary()));
        riceSubsidyField.setText(String.valueOf(e.getRiceSubsidy()));
        phoneAllowanceField.setText(String.valueOf(e.getPhoneAllowance()));
        clothingAllowanceField.setText(String.valueOf(e.getClothingAllowance()));
        allowanceField.setText(String.valueOf(e.getAllowance()));
        grossSemiMonthlyRateField.setText(String.valueOf(e.getGrossSemiMonthlyRate()));
        hourlyRateField.setText(String.valueOf(e.getHourlyRate()));

        sssField.setText(e.getSssNumber());
        philhealthField.setText(e.getPhilhealthNumber());
        tinField.setText(e.getTinNumber());
        pagibigField.setText(e.getPagibigNumber());

        if (isAdmin) {
            usernameField.setText(e.getUsername());
            passwordField.setText("");
            roleBox.setSelectedItem(e.getRole());
        }
    }

    private void saveEmployee() {

        if (readOnly) {
            dispose();
            return;
        }

        if (firstNameField.getText().trim().isEmpty()
                || lastNameField.getText().trim().isEmpty()
                || birthdayField.getText().trim().isEmpty()
                || addressField.getText().trim().isEmpty()
                || phoneNumberField.getText().trim().isEmpty()
                || emailField.getText().trim().isEmpty()
                || positionField.getText().trim().isEmpty()
                || statusField.getText().trim().isEmpty()
                || basicSalaryField.getText().trim().isEmpty()
                || riceSubsidyField.getText().trim().isEmpty()
                || phoneAllowanceField.getText().trim().isEmpty()
                || clothingAllowanceField.getText().trim().isEmpty()
                || allowanceField.getText().trim().isEmpty()
                || grossSemiMonthlyRateField.getText().trim().isEmpty()
                || hourlyRateField.getText().trim().isEmpty()
                || sssField.getText().trim().isEmpty()
                || philhealthField.getText().trim().isEmpty()
                || tinField.getText().trim().isEmpty()
                || pagibigField.getText().trim().isEmpty()) {

            JOptionPane.showMessageDialog(this,
                    "Please fill in all required fields.",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (isAdmin) {
            if (usernameField.getText().trim().isEmpty()
                    || (existingEmployee == null && new String(passwordField.getPassword()).trim().isEmpty())
                    || roleBox.getSelectedItem() == null) {

                JOptionPane.showMessageDialog(this,
                        "Please complete username, password, and role.",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        String email = emailField.getText().trim();
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            showValidationError(
                    "Please enter a valid email address.",
                    emailField
            );
            return;
        }

        LocalDate birthday = validateBirthdayField();
        if (birthday == null) return;

        String phoneNumber = phoneNumberField.getText().trim();
        if (!phoneNumber.matches("^(09\\d{9}|63\\d{10})$")) {
            showValidationError(
                    "Phone number must be in 09XXXXXXXXX or 63XXXXXXXXXX format.",
                    phoneNumberField
            );
            return;
        }

        if (!validateDigitsOnly(sssField, "SSS Number")) return;
        if (!validateDigitsOnly(philhealthField, "PhilHealth Number")) return;
        if (!validateDigitsOnly(tinField, "TIN Number")) return;
        if (!validateDigitsOnly(pagibigField, "Pag-IBIG Number")) return;

        try {
            int employeeId = existingEmployee == null
                    ? 0
                    : Integer.parseInt(idField.getText().trim());

            Double basicSalaryValue = validateNumericField(basicSalaryField, "Basic Salary");
            if (basicSalaryValue == null) return;

            Double riceSubsidyValue = validateNumericField(riceSubsidyField, "Rice Subsidy");
            if (riceSubsidyValue == null) return;

            Double phoneAllowanceValue = validateNumericField(phoneAllowanceField, "Phone Allowance");
            if (phoneAllowanceValue == null) return;

            Double clothingAllowanceValue = validateNumericField(clothingAllowanceField, "Clothing Allowance");
            if (clothingAllowanceValue == null) return;

            Double hourlyRateValue = validateNumericField(hourlyRateField, "Hourly Rate");
            if (hourlyRateValue == null) return;

            double basicSalary = basicSalaryValue;
            double riceSubsidy = riceSubsidyValue;
            double phoneAllowance = phoneAllowanceValue;
            double clothingAllowance = clothingAllowanceValue;
            double allowance = parseDoubleSafe(allowanceField.getText());
            double grossSemiMonthlyRate = parseDoubleSafe(grossSemiMonthlyRateField.getText());
            double hourlyRate = hourlyRateValue;

            if (basicSalary < 0 || riceSubsidy < 0 || phoneAllowance < 0
                    || clothingAllowance < 0 || allowance < 0
                    || grossSemiMonthlyRate < 0 || hourlyRate < 0) {

                JOptionPane.showMessageDialog(this,
                        "Salary values cannot be negative.",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (basicSalary > 1_000_000) {
                showValidationError(
                        "Basic salary exceeds the allowed maximum.",
                        basicSalaryField
                );
                return;
            }

            String username;
            String password;
            String role;

            if (isAdmin) {
                username = usernameField.getText().trim();
                String inputPassword = new String(passwordField.getPassword()).trim();

                if (!username.matches("^[A-Za-z0-9._-]{3,100}$")) {
                    showValidationError(
                            "Username must be 3-100 characters and may contain letters, numbers, dots, underscores, or hyphens.",
                            usernameField
                    );
                    return;
                }

                if (existingEmployee != null && inputPassword.isEmpty()) {
                    password = existingEmployee.getPassword();
                } else {
                    if (inputPassword.length() < 4) {
                        showValidationError(
                                "Password must be at least 4 characters long.",
                                passwordField
                        );
                        return;
                    }

                    password = PasswordUtil.hash(inputPassword);
                }

                role = roleBox.getSelectedItem().toString();

            } else if (existingEmployee != null) {
                username = existingEmployee.getUsername();
                password = existingEmployee.getPassword();
                role = existingEmployee.getRole();

            } else {
                String first = firstNameField.getText().trim().toLowerCase().replace(" ", "");
                String last = lastNameField.getText().trim().toLowerCase().replace(" ", "");

                String lastInitial = last.isEmpty() ? "x" : String.valueOf(last.charAt(0));
                username = first + "." + lastInitial;

                password = PasswordUtil.hash("1234");
                role = "Employee";
            }
            
            int excludeEmployeeId = existingEmployee == null ? 0 : existingEmployee.getEmployeeId();

            if (service.emailExists(email, excludeEmployeeId)) {
                showValidationError("Email already exists.", emailField);
                return;
            }

            if (service.usernameExists(username, excludeEmployeeId)) {
                showValidationError("Username already exists.", usernameField);
                return;
            }

            Employee newEmployee = new RegularEmployee(
                    employeeId,
                    firstNameField.getText().trim(),
                    lastNameField.getText().trim(),
                    positionField.getText().trim(),
                    statusField.getText().trim(),
                    basicSalary,
                    allowance,
                    hourlyRate,
                    username,
                    password,
                    role
            );

            newEmployee.setBirthday(birthday.toString());
            newEmployee.setAddress(addressField.getText().trim());
            newEmployee.setPhoneNumber(phoneNumber);
            newEmployee.setEmail(email);

            SupervisorOption supervisorOption = getSelectedSupervisorOption();
            DepartmentOption departmentOption = getSelectedDepartmentOption();
            newEmployee.setDepartmentId(departmentOption == null ? null : departmentOption.departmentId);
            newEmployee.setSupervisorEmployeeId(supervisorOption == null ? null : supervisorOption.employeeId);
            newEmployee.setImmediateSupervisor(supervisorOption == null || supervisorOption.employeeId == null
                    ? "N/A"
                    : supervisorOption.name);

            newEmployee.setRiceSubsidy(riceSubsidy);
            newEmployee.setPhoneAllowance(phoneAllowance);
            newEmployee.setClothingAllowance(clothingAllowance);
            newEmployee.setGrossSemiMonthlyRate(grossSemiMonthlyRate);

            newEmployee.setSssNumber(sssField.getText().trim());
            newEmployee.setPhilhealthNumber(philhealthField.getText().trim());
            newEmployee.setTinNumber(tinField.getText().trim());
            newEmployee.setPagibigNumber(pagibigField.getText().trim());

            if (existingEmployee == null) {
                service.addEmployee(newEmployee);

                JOptionPane.showMessageDialog(
                        this,
                        "Employee added successfully.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                service.updateEmployee(newEmployee);

                JOptionPane.showMessageDialog(
                        this,
                        "Employee updated successfully.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }

            dispose();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Numeric fields must contain valid numbers.",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);

        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Unable to save employee. Please check the entered values.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSupervisorOptions(int currentEmployeeId, Integer selectedSupervisorEmployeeId) {
        reportsToBox.removeAllItems();
        reportsToBox.addItem(SupervisorOption.unassigned());

        List<Employee> candidates = service.getSupervisorCandidates(currentEmployeeId);
        for (Employee candidate : candidates) {
            reportsToBox.addItem(SupervisorOption.fromEmployee(candidate));
        }

        if (selectedSupervisorEmployeeId != null && !hasSupervisorOption(selectedSupervisorEmployeeId)) {
            Employee currentSupervisor = service.findById(selectedSupervisorEmployeeId);
            if (currentSupervisor != null && currentSupervisor.getEmployeeId() != currentEmployeeId) {
                reportsToBox.addItem(SupervisorOption.fromEmployee(currentSupervisor));
            }
        }

        selectSupervisorOption(selectedSupervisorEmployeeId);
    }

    private void loadDepartmentOptions(Integer selectedDepartmentId) {
        departmentBox.removeAllItems();
        departmentBox.addItem(DepartmentOption.unassigned());

        for (Map.Entry<Integer, String> entry : service.getDepartments().entrySet()) {
            departmentBox.addItem(new DepartmentOption(entry.getKey(), entry.getValue()));
        }

        selectDepartmentOption(selectedDepartmentId);
    }

    private void selectDepartmentOption(Integer departmentId) {
        if (departmentBox == null) {
            return;
        }

        for (int i = 0; i < departmentBox.getItemCount(); i++) {
            DepartmentOption option = departmentBox.getItemAt(i);
            if ((departmentId == null && option.departmentId == null)
                    || (departmentId != null && departmentId.equals(option.departmentId))) {
                departmentBox.setSelectedIndex(i);
                return;
            }
        }

        departmentBox.setSelectedIndex(0);
    }

    private DepartmentOption getSelectedDepartmentOption() {
        return departmentBox == null ? null : (DepartmentOption) departmentBox.getSelectedItem();
    }

    private boolean hasSupervisorOption(Integer employeeId) {
        if (employeeId == null) {
            return false;
        }
        for (int i = 0; i < reportsToBox.getItemCount(); i++) {
            SupervisorOption option = reportsToBox.getItemAt(i);
            if (employeeId.equals(option.employeeId)) {
                return true;
            }
        }
        return false;
    }

    private void selectSupervisorOption(Integer supervisorEmployeeId) {
        if (reportsToBox == null) {
            return;
        }

        for (int i = 0; i < reportsToBox.getItemCount(); i++) {
            SupervisorOption option = reportsToBox.getItemAt(i);
            if ((supervisorEmployeeId == null && option.employeeId == null)
                    || (supervisorEmployeeId != null && supervisorEmployeeId.equals(option.employeeId))) {
                reportsToBox.setSelectedIndex(i);
                return;
            }
        }

        reportsToBox.setSelectedIndex(0);
    }

    private Integer getSelectedSupervisorEmployeeId() {
        SupervisorOption option = getSelectedSupervisorOption();
        return option == null ? null : option.employeeId;
    }

    private SupervisorOption getSelectedSupervisorOption() {
        return reportsToBox == null ? null : (SupervisorOption) reportsToBox.getSelectedItem();
    }

    private static class DepartmentOption {
        private final Integer departmentId;
        private final String name;

        private DepartmentOption(Integer departmentId, String name) {
            this.departmentId = departmentId;
            this.name = name;
        }

        private static DepartmentOption unassigned() {
            return new DepartmentOption(null, "Not assigned");
        }

        @Override
        public String toString() {
            return name == null || name.isBlank() ? "Not assigned" : name;
        }
    }

    private static class SupervisorOption {
        private final Integer employeeId;
        private final String name;
        private final String role;

        private SupervisorOption(Integer employeeId, String name, String role) {
            this.employeeId = employeeId;
            this.name = name;
            this.role = role;
        }

        private static SupervisorOption unassigned() {
            return new SupervisorOption(null, "No assigned supervisor", "");
        }

        private static SupervisorOption fromEmployee(Employee employee) {
            return new SupervisorOption(
                    employee.getEmployeeId(),
                    employee.getFirstName() + " " + employee.getLastName(),
                    employee.getPosition()
            );
        }

        @Override
        public String toString() {
            if (employeeId == null) {
                return name;
            }

            String descriptor = role == null || role.isBlank() ? "Employee" : role;
            return "ID " + employeeId + " • " + name + " • " + descriptor;
        }
    }

    private Double validateNumericField(JTextField field, String fieldName) {
        String value = field.getText().trim();

        if (value.isEmpty()) {
            showValidationError(fieldName + " is required.", field);
            return null;
        }

        try {
            double number = Double.parseDouble(value);

            if (number < 0) {
                showValidationError(fieldName + " cannot be negative.", field);
                return null;
            }

            return number;
        } catch (NumberFormatException ex) {
            showValidationError(fieldName + " must be a valid number.", field);
            return null;
        }
    }

    private LocalDate validateBirthdayField() {
        String value = birthdayField.getText().trim();

        if (!value.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            showValidationError(
                    "Birthday must follow YYYY-MM-DD format.",
                    birthdayField
            );
            return null;
        }

        try {
            LocalDate birthday = LocalDate.parse(value);

            if (birthday.isAfter(LocalDate.now())) {
                showValidationError(
                        "Birthday cannot be in the future.",
                        birthdayField
                );
                return null;
            }

            return birthday;
        } catch (DateTimeParseException ex) {
            showValidationError(
                    "Birthday must be a real calendar date.",
                    birthdayField
            );
            return null;
        }
    }

    private boolean validateDigitsOnly(JTextField field, String fieldName) {
        String value = field.getText().trim();

        if (!value.matches("\\d+")) {
            showValidationError(
                    fieldName + " must contain digits only.",
                    field
            );
            return false;
        }

        return true;
    }

    private void showValidationError(String message, JTextField field) {
        JOptionPane.showMessageDialog(this,
                message,
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);

        if (field != null) {
            field.requestFocus();
            setFieldError(field);
        }
    }

    private void addLiveDigitsValidation(JTextField field, String fieldName) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                validateField();
            }

            public void removeUpdate(DocumentEvent e) {
                validateField();
            }

            public void changedUpdate(DocumentEvent e) {
                validateField();
            }

            private void validateField() {
                String value = field.getText().trim();

                if (value.isEmpty() || value.matches("\\d+")) {
                    setFieldNormal(field);
                    field.setToolTipText(null);
                } else {
                    setFieldError(field);
                    field.setToolTipText(fieldName + " must contain digits only.");
                }
            }
        });
    }

    private void addLiveNumericValidation(JTextField field, String fieldName) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                validateField();
            }

            public void removeUpdate(DocumentEvent e) {
                validateField();
            }

            public void changedUpdate(DocumentEvent e) {
                validateField();
            }

            private void validateField() {
                String value = field.getText().trim();

                if (value.isEmpty()) {
                    setFieldNormal(field);
                    field.setToolTipText(null);
                    return;
                }

                try {
                    double number = Double.parseDouble(value);

                    if (number < 0) {
                        setFieldError(field);
                        field.setToolTipText(fieldName + " cannot be negative.");
                        return;
                    }

                    setFieldNormal(field);
                    field.setToolTipText(null);
                } catch (NumberFormatException ex) {
                    setFieldError(field);
                    field.setToolTipText(fieldName + " must be numeric.");
                }
            }
        });
    }

    private void addLiveBirthdayValidation(JTextField field) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                validateField();
            }

            public void removeUpdate(DocumentEvent e) {
                validateField();
            }

            public void changedUpdate(DocumentEvent e) {
                validateField();
            }

            private void validateField() {
                String value = field.getText().trim();

                if (value.isEmpty()) {
                    setFieldNormal(field);
                    field.setToolTipText(null);
                    return;
                }

                if (!value.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                    setFieldError(field);
                    field.setToolTipText("Birthday must follow YYYY-MM-DD format.");
                    return;
                }

                try {
                    LocalDate birthday = LocalDate.parse(value);

                    if (birthday.isAfter(LocalDate.now())) {
                        setFieldError(field);
                        field.setToolTipText("Birthday cannot be in the future.");
                    } else {
                        setFieldNormal(field);
                        field.setToolTipText(null);
                    }
                } catch (DateTimeParseException ex) {
                    setFieldError(field);
                    field.setToolTipText("Birthday must be a real calendar date.");
                }
            }
        });
    }

    private void setFieldError(JTextField field) {
        WorkforceFormToolkit.setFieldError(field, field.getToolTipText());
    }

    private void setFieldNormal(JTextField field) {
        WorkforceFormToolkit.setFieldNormal(field);
    }
    
    private void addLivePhoneValidation(JTextField field) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                validateField();
            }

            public void removeUpdate(DocumentEvent e) {
                validateField();
            }

            public void changedUpdate(DocumentEvent e) {
                validateField();
            }

            private void validateField() {
                String value = field.getText().trim();

                if (value.isEmpty() || value.matches("^(09\\d{9}|63\\d{10})$")) {
                    setFieldNormal(field);
                    field.setToolTipText(null);
                } else {
                    setFieldError(field);
                    field.setToolTipText(
                            "Use 09XXXXXXXXX or 63XXXXXXXXXX format."
                    );
                }
            }
        });
    }   
}
