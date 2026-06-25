/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.oop.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.security.SecureRandom;

import com.mycompany.oop.service.LoginService;
import com.mycompany.oop.service.AuditService;
import com.mycompany.oop.model.Employee;

public class LoginFrame extends JFrame {

    private static final SecureRandom OTP_RANDOM = new SecureRandom();

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginBtn;
    private JLabel statusLabel;
    private LoginService loginService;
    private AuditService auditService;
    private boolean loginInProgress;
    private boolean workspaceOpening;

    public LoginFrame() {

        loginService = new LoginService();
        auditService = new AuditService();

        setTitle("MotorPH Payroll System");
        setSize(460, 500);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(UITheme.BG);

        setLayout(new GridBagLayout());

        JPanel loginCard = new JPanel(new GridBagLayout());
        loginCard.setBackground(Color.WHITE);
        loginCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                new EmptyBorder(30, 44, 30, 44)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 10, 0);
        JLabel logoImageLabel = MotorPHLogo.createLabel(
                164,
                92,
                "MotorPH",
                new Font("Segoe UI", Font.BOLD, 30),
                UITheme.ACCENT
        );
        loginCard.add(logoImageLabel, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 2, 0);
        JLabel brandLabel = new JLabel("MotorPH", SwingConstants.CENTER);
        brandLabel.setFont(new Font("Segoe UI", Font.BOLD, 30));
        brandLabel.setForeground(UITheme.ACCENT);
        loginCard.add(brandLabel, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 24, 0);
        JLabel subtitleLabel = new JLabel("Payroll System", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(UITheme.TEXT_SECONDARY);
        loginCard.add(subtitleLabel, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 6, 0);
        JLabel userLabel = new JLabel("Username");
        userLabel.setFont(UITheme.FONT_BODY_BOLD);
        userLabel.setForeground(UITheme.TEXT_PRIMARY);
        loginCard.add(userLabel, gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 16, 0);
        usernameField = new JTextField();
        usernameField.setFont(UITheme.FONT_BODY);
        usernameField.setPreferredSize(new Dimension(280, 36));
        loginCard.add(usernameField, gbc);

        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 6, 0);
        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(UITheme.FONT_BODY_BOLD);
        passLabel.setForeground(UITheme.TEXT_PRIMARY);
        loginCard.add(passLabel, gbc);

        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 24, 0);
        passwordField = new JPasswordField();
        passwordField.setFont(UITheme.FONT_BODY);
        passwordField.setPreferredSize(new Dimension(280, 36));
        loginCard.add(passwordField, gbc);

        gbc.gridy = 7;
        gbc.insets = new Insets(0, 0, 0, 0);
        loginBtn = UITheme.createAccentButton("Sign In");
        loginBtn.setPreferredSize(new Dimension(280, 40));
        loginBtn.addActionListener(e -> login());
        loginCard.add(loginBtn, gbc);

        gbc.gridy = 8;
        gbc.insets = new Insets(10, 0, 0, 0);
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_SECONDARY);
        loginCard.add(statusLabel, gbc);

        add(loginCard);

        getRootPane().setDefaultButton(loginBtn);
    }

    private void login() {
        if (loginInProgress || workspaceOpening) {
            return;
        }

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter username and password.",
                    "Login Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        loginInProgress = true;
        setLoginBusy(true, "Signing in...");
        SwingWorker<Employee, Void> worker = new SwingWorker<>() {
            @Override
            protected Employee doInBackground() {
                return loginService.login(username, password);
            }

            @Override
            protected void done() {
                try {
                    Employee emp = get();
                    handleLoginResult(username, emp);
                } catch (Exception ex) {
                    showLoginFailure(username, "Unable to sign in. Please try again.");
                }
            }
        };
        worker.execute();
    }

    private void handleLoginResult(String username, Employee emp) {
        if (emp == null) {
            showLoginFailure(username, "Invalid username or password.");
            return;
        }

        setLoginBusy(false, "Verification required.");
        String otpCode = generateOtpCode();

        System.out.println("======================================");
        System.out.println("MotorPH Two-Factor Authentication OTP");
        System.out.println("User: " + emp.getUsername());
        System.out.println("Email: " + emp.getEmail());
        System.out.println("OTP Code: " + otpCode);
        System.out.println("======================================");

        if (!verifyOtp(otpCode)) {
            setLoginBusy(false, " ");
            return;
        }

        workspaceOpening = true;
        setLoginBusy(true, "Opening workspace...");
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                auditService.logAction(null, "LOGIN_SUCCESS", "employees", String.valueOf(emp.getEmployeeId()));
                return null;
            }

            @Override
            protected void done() {
                dispose();
                MainAppFrame frame = new MainAppFrame(emp);
                frame.setVisible(true);
            }
        };
        worker.execute();
    }

    private void showLoginFailure(String username, String message) {
        auditService.logAction(null, "LOGIN_FAILED", "employees", username);
        loginInProgress = false;
        setLoginBusy(false, " ");
        JOptionPane.showMessageDialog(this,
                message,
                "Login Error",
                JOptionPane.ERROR_MESSAGE);
        passwordField.setText("");
        passwordField.requestFocusInWindow();
    }

    private void setLoginBusy(boolean busy, String message) {
        if (!busy && !workspaceOpening) {
            loginInProgress = false;
        }
        loginBtn.setEnabled(!busy);
        usernameField.setEnabled(!busy);
        passwordField.setEnabled(!busy);
        statusLabel.setText(message == null ? " " : message);
        setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private String generateOtpCode() {
        int code = OTP_RANDOM.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    private boolean verifyOtp(String otpCode) {
        JTextField otpField = new JTextField();
        otpField.setFont(UITheme.FONT_BODY);
        otpField.setPreferredSize(new Dimension(220, 34));

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(8, 8, 4, 8));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 8, 0);
        JLabel instructionLabel = new JLabel("Enter the verification code from the terminal.");
        instructionLabel.setFont(UITheme.FONT_BODY);
        instructionLabel.setForeground(UITheme.TEXT_PRIMARY);
        panel.add(instructionLabel, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 6, 0);
        JLabel otpInputLabel = new JLabel("Verification Code");
        otpInputLabel.setFont(UITheme.FONT_BODY_BOLD);
        otpInputLabel.setForeground(UITheme.TEXT_PRIMARY);
        panel.add(otpInputLabel, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(otpField, gbc);

        while (true) {
            int result = JOptionPane.showConfirmDialog(
                    this,
                    panel,
                    "Two-Factor Authentication",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (result != JOptionPane.OK_OPTION) {
                return false;
            }

            if (otpCode.equals(otpField.getText().trim())) {
                return true;
            }

            JOptionPane.showMessageDialog(this,
                    "Invalid verification code.",
                    "Authentication Error",
                    JOptionPane.ERROR_MESSAGE);
            otpField.setText("");
            otpField.requestFocusInWindow();
        }
    }
}
