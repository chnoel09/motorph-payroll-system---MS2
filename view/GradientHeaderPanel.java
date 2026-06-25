/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.oop.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import com.mycompany.oop.model.Employee;

public class GradientHeaderPanel extends JPanel {

    public GradientHeaderPanel(Employee employee) {

        setPreferredSize(new Dimension(1000, 44));
        setLayout(new BorderLayout());
        setBackground(UITheme.SIDEBAR_BG);
        setBorder(new EmptyBorder(0, 16, 0, 16));

        JLabel title = new JLabel("MotorPH Payroll System");
        title.setForeground(UITheme.ACCENT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JLabel userInfo = new JLabel(
                employee.getFirstName() + " (" + employee.getRole() + ")");
        userInfo.setForeground(UITheme.TEXT_SIDEBAR);
        userInfo.setFont(UITheme.FONT_BODY);
        userInfo.setHorizontalAlignment(SwingConstants.RIGHT);

        add(title, BorderLayout.WEST);
        add(userInfo, BorderLayout.EAST);
    }
}


