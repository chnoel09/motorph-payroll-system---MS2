package com.mycompany.oop.view;

import com.mycompany.oop.model.Employee;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class SidebarPanel extends JPanel {

    private final SidebarState state = new SidebarState();
    private final JPanel brandPanel;
    private final JLabel brandMarkLabel;
    private final JLabel logoLabel;
    private final JLabel userLabel;
    private final JLabel systemLabel;
    private final JPanel profileCard;
    private final JPanel menuPanel;
    private final JButton toggleButton;
    private final JButton logoutButton;
    private final List<SidebarMenuItem> menuItems = new ArrayList<>();
    private final List<SidebarSection> sections = new ArrayList<>();
    private final BiConsumer<String, SidebarMenuItem> navigationHandler;
    private SidebarSection currentSection;
    private Timer widthTimer;

    public SidebarPanel(Employee employee, BiConsumer<String, SidebarMenuItem> navigationHandler, Runnable logoutHandler) {
        this.navigationHandler = navigationHandler;

        setLayout(new BorderLayout());
        setBackground(UITheme.SIDEBAR_BG);
        setPreferredSize(new Dimension(SidebarState.EXPANDED_WIDTH, 0));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(26, 45, 78)));

        brandPanel = new JPanel(new BorderLayout(10, 0));
        brandPanel.setOpaque(false);
        brandPanel.setBorder(new EmptyBorder(20, 18, 14, 18));

        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));

        brandMarkLabel = MotorPHLogo.createLabel(
                52,
                30,
                "MP",
                new Font("Segoe UI", Font.BOLD, 15),
                Color.WHITE
        );
        brandMarkLabel.setPreferredSize(new Dimension(52, 30));
        brandMarkLabel.setHorizontalAlignment(SwingConstants.CENTER);

        logoLabel = new JLabel("MotorPH");
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 25));
        logoLabel.setForeground(Color.WHITE);

        userLabel = new JLabel(employee.getFirstName());
        userLabel.setFont(UITheme.FONT_BODY_BOLD);
        userLabel.setForeground(Color.WHITE);

        systemLabel = new JLabel(employee.getRole() + " • Workforce Operations");
        systemLabel.setFont(UITheme.FONT_SMALL);
        systemLabel.setForeground(new Color(147, 160, 184));

        brandText.add(logoLabel);

        toggleButton = new JButton("‹");
        toggleButton.setFont(new Font("Segoe UI", Font.BOLD, 18));
        toggleButton.setForeground(Color.WHITE);
        toggleButton.setBackground(new Color(28, 47, 82));
        toggleButton.setFocusPainted(false);
        toggleButton.setBorder(new EmptyBorder(6, 11, 6, 11));
        toggleButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggleButton.setOpaque(true);
        toggleButton.setToolTipText("Collapse sidebar");
        toggleButton.addActionListener(e -> toggleCollapsed());

        brandPanel.add(brandMarkLabel, BorderLayout.WEST);
        brandPanel.add(brandText, BorderLayout.CENTER);
        brandPanel.add(toggleButton, BorderLayout.EAST);

        profileCard = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(25, 43, 76));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        profileCard.setOpaque(false);
        profileCard.setBorder(new EmptyBorder(13, 13, 13, 13));

        JLabel avatar = new JLabel(employee.getFirstName().substring(0, 1).toUpperCase(), SwingConstants.CENTER);
        avatar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        avatar.setForeground(Color.WHITE);
        avatar.setPreferredSize(new Dimension(38, 38));
        avatar.setOpaque(true);
        avatar.setBackground(UITheme.ACCENT);

        JPanel profileText = new JPanel();
        profileText.setOpaque(false);
        profileText.setLayout(new BoxLayout(profileText, BoxLayout.Y_AXIS));
        profileText.add(userLabel);
        profileText.add(Box.createVerticalStrut(2));
        profileText.add(systemLabel);

        profileCard.add(avatar, BorderLayout.WEST);
        profileCard.add(profileText, BorderLayout.CENTER);

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(brandPanel);
        header.add(Box.createVerticalStrut(2));
        JPanel profileWrap = new JPanel(new BorderLayout());
        profileWrap.setOpaque(false);
        profileWrap.setBorder(new EmptyBorder(0, 14, 12, 14));
        profileWrap.add(profileCard, BorderLayout.CENTER);
        header.add(profileWrap);
        add(header, BorderLayout.NORTH);

        menuPanel = new JPanel();
        menuPanel.setOpaque(false);
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBorder(new EmptyBorder(8, 12, 10, 12));

        JScrollPane scrollPane = new JScrollPane(menuPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        WorkforceFormToolkit.tuneScrollPane(scrollPane);
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBorder(new EmptyBorder(10, 12, 18, 12));

        logoutButton = UITheme.createSidebarDangerButton("Logout");
        logoutButton.setIcon(new SidebarIcon(SidebarIcon.Type.LOGOUT));
        logoutButton.setIconTextGap(12);
        logoutButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        logoutButton.addActionListener(e -> logoutHandler.run());
        bottomPanel.add(logoutButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public SidebarMenuItem addMenuItem(String text, String card, SidebarIcon.Type iconType) {
        SidebarMenuItem item = new SidebarMenuItem(text, new SidebarIcon(iconType));
        item.addActionListener(e -> navigationHandler.accept(card, item));
        menuItems.add(item);
        menuPanel.add(item);
        Component spacer = Box.createVerticalStrut(5);
        menuPanel.add(spacer);
        if (currentSection != null) {
            currentSection.addChild(item);
            currentSection.addChild(spacer);
        }
        return item;
    }

    public void addSectionLabel(String text) {
        SidebarSection section = new SidebarSection(text);
        sections.add(section);
        currentSection = section;
        menuPanel.add(section.headerButton);
    }

    public void setActiveItem(SidebarMenuItem activeItem) {
        for (SidebarMenuItem item : menuItems) {
            item.setActive(item == activeItem);
        }
        for (SidebarSection section : sections) {
            if (section.contains(activeItem)) {
                section.setCollapsed(false);
            }
        }
    }

    public SidebarMenuItem getFirstItem() {
        return menuItems.isEmpty() ? null : menuItems.get(0);
    }

    private void toggleCollapsed() {
        setCollapsed(!state.isCollapsed());
    }

    public void setCollapsed(boolean collapsed) {
        state.setCollapsed(collapsed);
        animateWidth(state.getTargetWidth());
        updateCollapsedContent();
    }

    private void updateCollapsedContent() {
        boolean collapsed = state.isCollapsed();
        logoLabel.setText(collapsed ? "MP" : "MotorPH");
        brandMarkLabel.setVisible(!collapsed);
        profileCard.setVisible(!collapsed);
        toggleButton.setText(collapsed ? "›" : "‹");
        toggleButton.setToolTipText(collapsed ? "Expand sidebar" : "Collapse sidebar");
        logoutButton.setText(collapsed ? "" : "Logout");
        logoutButton.setIcon(new SidebarIcon(SidebarIcon.Type.LOGOUT));
        logoutButton.setIconTextGap(collapsed ? 0 : 12);
        logoutButton.setToolTipText("Logout");
        logoutButton.setHorizontalAlignment(collapsed ? SwingConstants.CENTER : SwingConstants.LEFT);

        for (SidebarMenuItem item : menuItems) {
            item.setCollapsed(collapsed);
        }

        for (SidebarSection section : sections) {
            section.headerButton.setVisible(!collapsed);
            section.syncVisibility();
        }

        revalidate();
        repaint();
    }

    private void animateWidth(int targetWidth) {
        if (widthTimer != null && widthTimer.isRunning()) {
            widthTimer.stop();
        }

        int startWidth = getPreferredSize().width;
        int distance = targetWidth - startWidth;
        int frames = 12;
        int[] frame = {0};

        widthTimer = new Timer(14, e -> {
            frame[0]++;
            float progress = Math.min(1f, frame[0] / (float) frames);
            int width = startWidth + Math.round(distance * progress);
            setPreferredSize(new Dimension(width, 0));
            revalidate();

            if (frame[0] >= frames) {
                ((Timer) e.getSource()).stop();
            }
        });
        widthTimer.start();
    }

    private class SidebarSection {
        private final String title;
        private final JButton headerButton;
        private final List<Component> children = new ArrayList<>();
        private boolean collapsed = true;

        private SidebarSection(String title) {
            this.title = title;
            headerButton = new JButton(title + "  ▸");
            headerButton.setFont(UITheme.FONT_SMALL.deriveFont(Font.BOLD));
            headerButton.setForeground(new Color(139, 155, 185));
            headerButton.setBackground(UITheme.SIDEBAR_BG);
            headerButton.setBorder(new EmptyBorder(sections.isEmpty() ? 6 : 14, 12, 7, 8));
            headerButton.setFocusPainted(false);
            headerButton.setOpaque(false);
            headerButton.setContentAreaFilled(false);
            headerButton.setBorderPainted(false);
            headerButton.setHorizontalAlignment(SwingConstants.LEFT);
            headerButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            headerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            headerButton.setToolTipText(title);
            headerButton.addActionListener(e -> toggle());
        }

        private void addChild(Component component) {
            children.add(component);
        }

        private boolean contains(SidebarMenuItem item) {
            return children.contains(item);
        }

        private void toggle() {
            collapsed = !collapsed;
            syncVisibility();
            menuPanel.revalidate();
            menuPanel.repaint();
        }

        private void setCollapsed(boolean collapsed) {
            this.collapsed = collapsed;
            syncVisibility();
        }

        private void syncVisibility() {
            boolean sidebarCollapsed = state.isCollapsed();
            headerButton.setText(title + (collapsed ? "  ▸" : "  ▾"));
            headerButton.setVisible(!sidebarCollapsed);
            for (Component child : children) {
                child.setVisible(sidebarCollapsed || !collapsed);
            }
        }
    }
}
