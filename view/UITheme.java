package com.mycompany.oop.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class UITheme {

    // Sidebar
    public static final Color SIDEBAR_BG = new Color(18, 32, 60);
    public static final Color SIDEBAR_HOVER = new Color(31, 49, 82);
    public static final Color SIDEBAR_ACTIVE = new Color(39, 59, 96);

    // Primary MotorPH accent
    public static final Color ACCENT = new Color(210, 43, 43);
    public static final Color ACCENT_HOVER = new Color(185, 30, 30);

    // Subtle supporting accents
    public static final Color BLUE = new Color(37, 99, 195);
    public static final Color BLUE_LIGHT = new Color(59, 130, 246);
    public static final Color YELLOW = new Color(245, 158, 11);
    public static final Color BLACK = new Color(17, 24, 39);

    // Backgrounds
    public static final Color BG = new Color(248, 249, 252);
    public static final Color CARD_BG = Color.WHITE;

    // Text
    public static final Color TEXT_PRIMARY = new Color(20, 30, 50);
    public static final Color TEXT_SECONDARY = new Color(100, 110, 125);
    public static final Color TEXT_WHITE = new Color(255, 255, 255);
    public static final Color TEXT_SIDEBAR = new Color(215, 222, 235);

    // Borders
    public static final Color BORDER = new Color(225, 230, 238);

    // Table
    public static final Color TABLE_HEADER_BG = SIDEBAR_BG;
    public static final Color TABLE_ALT_ROW = new Color(248, 250, 252);

    // Status
    public static final Color DANGER = new Color(210, 43, 43);
    public static final Color DANGER_HOVER = new Color(185, 30, 30);
    public static final Color SUCCESS = new Color(34, 160, 70);
        
    // Legacy aliases
    public static final Color MAIN_GRAY = BG;
    public static final Color DASHBOARD_BG = BG;

        // Fonts
    public static final Font FONT_PAGE_TITLE = new Font("Segoe UI", Font.BOLD, 28);
    public static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_CARD_VALUE = new Font("Segoe UI", Font.BOLD, 28);
    public static final Font FONT_CARD_LABEL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_NAV = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_TABLE = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_TABLE_HEADER = new Font("Segoe UI", Font.BOLD, 12);

    public static void initTheme() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
    }

    public static JPanel createTitleBar(String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(CARD_BG);
        panel.setPreferredSize(new Dimension(0, 56));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(0, 28, 0, 28)
        ));

        JLabel label = new JLabel(title);
        label.setFont(FONT_SECTION);
        label.setForeground(TEXT_PRIMARY);

        panel.add(label, BorderLayout.WEST);
        return panel;
    }

    public static JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BUTTON);
        btn.setFocusPainted(false);
        btn.setBackground(Color.WHITE);
        btn.setForeground(TEXT_PRIMARY);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(8, 18, 8, 18)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(243, 244, 246));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(Color.WHITE);
            }
        });

        return btn;
    }

    public static JButton createAccentButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BUTTON);
        btn.setFocusPainted(false);
        btn.setBackground(ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setBorder(new EmptyBorder(9, 22, 9, 22));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(ACCENT_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(ACCENT);
            }
        });

        return btn;
    }

    public static JButton createCompactWorkflowButton(String text, boolean primary) {
        JButton btn = primary ? createAccentButton(text) : createButton(text);
        btn.setFont(FONT_SMALL.deriveFont(Font.BOLD));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? ACCENT : BORDER),
                new EmptyBorder(4, 6, 4, 6)
        ));
        sizeButtonToFit(btn, 88, 24);
        return btn;
    }

    public static JButton createBlueButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BUTTON);
        btn.setFocusPainted(false);
        btn.setBackground(BLUE);
        btn.setForeground(Color.WHITE);
        btn.setBorder(new EmptyBorder(9, 22, 9, 22));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(BLUE_LIGHT);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(BLUE);
            }
        });

        return btn;
    }

    public static JButton createSidebarButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_NAV);
        btn.setForeground(TEXT_SIDEBAR);
        btn.setBackground(SIDEBAR_BG);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(12, 18, 12, 16));
        btn.setPreferredSize(new Dimension(220, 44));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty("sidebar.active", false);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(false);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(SIDEBAR_HOVER);
                btn.setForeground(Color.WHITE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                boolean active = Boolean.TRUE.equals(btn.getClientProperty("sidebar.active"));

                if (active) {
                    btn.setBackground(SIDEBAR_ACTIVE);
                    btn.setForeground(Color.WHITE);
                } else {
                    btn.setBackground(SIDEBAR_BG);
                    btn.setForeground(TEXT_SIDEBAR);
                }
            }
        });

        return btn;
    }

    public static JButton createSidebarDangerButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_NAV);
        btn.setForeground(new Color(252, 165, 165));
        btn.setBackground(SIDEBAR_BG);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(2, 0, 2, 0),
                new EmptyBorder(12, 18, 12, 16)
        ));
        btn.setPreferredSize(new Dimension(220, 44));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(false);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(92, 40, 45));
                btn.setForeground(Color.WHITE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(SIDEBAR_BG);
                btn.setForeground(new Color(252, 165, 165));
            }
        });

        return btn;
    }

    public static JButton createCrudDangerButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BUTTON);
        btn.setFocusPainted(false);
        btn.setBackground(new Color(254, 242, 242));
        btn.setForeground(DANGER);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(254, 202, 202)),
                new EmptyBorder(8, 18, 8, 18)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(DANGER);
                btn.setForeground(Color.WHITE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(254, 242, 242));
                btn.setForeground(DANGER);
            }
        });

        return btn;
    }

    public static JButton createFormButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BUTTON);
        btn.setFocusPainted(false);
        btn.setBackground(Color.WHITE);
        btn.setForeground(TEXT_PRIMARY);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(8, 22, 8, 22)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static void sizeButtonToFit(JButton button, int minimumWidth, int height) {
        if (button == null) {
            return;
        }

        Dimension preferred = button.getPreferredSize();
        int width = Math.max(minimumWidth, preferred == null ? minimumWidth : preferred.width + 8);
        Dimension size = new Dimension(width, height);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
    }

    public static JPanel createRaisedPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(CARD_BG);
        panel.setBorder(BorderFactory.createLineBorder(BORDER));
        return panel;
    }

    public static JPanel createInsetPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder());
        return panel;
    }

    public static JPanel createDashboardCard() {
        JPanel card = new JPanel();
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(22, 22, 22, 22)
        ));
        return card;
    }

    public static JPanel createWorkspacePanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(18, 24, 18, 24));
        return panel;
    }

    public static JPanel createSkeletonCard(String title, int rows) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(16, 18, 16, 18)
        ));

        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        JLabel label = new JLabel(title == null || title.isBlank() ? "Loading" : title);
        label.setFont(FONT_BODY_BOLD);
        label.setForeground(TEXT_SECONDARY);
        header.add(label, BorderLayout.WEST);
        header.add(createSkeletonBar(120, 12), BorderLayout.EAST);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(createSkeletonBar(Integer.MAX_VALUE, 18));
        body.add(Box.createVerticalStrut(12));
        int safeRows = Math.max(1, rows);
        for (int i = 0; i < safeRows; i++) {
            body.add(createSkeletonBar(Integer.MAX_VALUE, 14));
            if (i < safeRows - 1) {
                body.add(Box.createVerticalStrut(9));
            }
        }

        card.add(header, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private static JComponent createSkeletonBar(int width, int height) {
        JPanel bar = new JPanel();
        bar.setBackground(new Color(229, 234, 242));
        int safeWidth = width == Integer.MAX_VALUE ? 640 : Math.max(40, width);
        Dimension preferred = new Dimension(safeWidth, Math.max(8, height));
        bar.setPreferredSize(preferred);
        bar.setMinimumSize(new Dimension(40, preferred.height));
        bar.setMaximumSize(new Dimension(width == Integer.MAX_VALUE ? Integer.MAX_VALUE : safeWidth, preferred.height));
        return bar;
    }

    public static JPanel createActionBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)
        ));
        return panel;
    }

    public static JTextField createDateField() {
        JTextField field = new JTextField();
        field.setFont(FONT_BODY);
        field.setPreferredSize(new Dimension(110, 34));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(6, 10, 6, 10)
        ));
        field.setToolTipText(WorkforceFormToolkit.DATE_HELP);
        return field;
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(40);
        table.setFont(FONT_TABLE);
        table.setGridColor(new Color(238, 242, 247));
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(209, 250, 229));
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setBackground(Color.WHITE);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(0, 42));
        header.setReorderingAllowed(false);

        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {

                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        t, value, isSelected, hasFocus, row, column);

                label.setBackground(TABLE_HEADER_BG);
                label.setForeground(Color.WHITE);
                label.setFont(FONT_TABLE_HEADER);
                label.setBorder(new EmptyBorder(0, 14, 0, 14));
                label.setHorizontalAlignment(SwingConstants.LEFT);
                return label;
            }
        });

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {

                Component c = super.getTableCellRendererComponent(
                        t, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : TABLE_ALT_ROW);
                    c.setForeground(TEXT_PRIMARY);
                }

                if (c instanceof JComponent component) {
                    component.setBorder(new EmptyBorder(0, 14, 0, 14));
                }

                return c;
            }
        });
    }

    public static void setColumnWidths(JTable table, int... widths) {
        if (table == null || widths == null) {
            return;
        }

        for (int i = 0; i < widths.length && i < table.getColumnModel().getColumnCount(); i++) {
            if (widths[i] <= 0) {
                continue;
            }
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    public static JScrollPane createTableScrollPane(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        WorkforceFormToolkit.tuneScrollPane(scroll);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        return scroll;
    }
}
