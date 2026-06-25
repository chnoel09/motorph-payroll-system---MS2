package com.mycompany.oop.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SidebarMenuItem extends JButton {

    private final String label;
    private boolean collapsed;
    private boolean active;
    private int badgeCount;
    private Color currentBackground = UITheme.SIDEBAR_BG;
    private Timer hoverTimer;

    public SidebarMenuItem(String label, Icon icon) {
        this.label = label;

        setFont(UITheme.FONT_NAV);
        setForeground(UITheme.TEXT_SIDEBAR);
        setBackground(UITheme.SIDEBAR_BG);
        setFocusPainted(false);
        setBorder(new EmptyBorder(0, 15, 0, 14));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setHorizontalAlignment(SwingConstants.LEFT);
        setIcon(icon);
        setIconTextGap(13);
        setPreferredSize(new Dimension(220, 44));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        setToolTipText(label);
        setCollapsed(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                animateBackground(active ? UITheme.SIDEBAR_ACTIVE : UITheme.SIDEBAR_HOVER);
                setForeground(Color.WHITE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                animateBackground(active ? UITheme.SIDEBAR_ACTIVE : UITheme.SIDEBAR_BG);
                setForeground(active ? Color.WHITE : UITheme.TEXT_SIDEBAR);
            }
        });
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
        setText(collapsed ? "" : label);
        setHorizontalAlignment(collapsed ? SwingConstants.CENTER : SwingConstants.LEFT);
        setIconTextGap(collapsed ? 0 : 13);
        setBorder(new EmptyBorder(0, collapsed ? 0 : 15, 0, collapsed ? 0 : 14));
        setPreferredSize(new Dimension(collapsed ? 52 : 220, 44));
        revalidate();
        repaint();
    }

    public void setActive(boolean active) {
        this.active = active;
        currentBackground = active ? UITheme.SIDEBAR_ACTIVE : UITheme.SIDEBAR_BG;
        setForeground(active ? new Color(255, 255, 255) : UITheme.TEXT_SIDEBAR);
        repaint();
    }

    public boolean isActive() {
        return active;
    }

    public void setBadgeCount(int badgeCount) {
        this.badgeCount = Math.max(0, badgeCount);
        repaint();
    }

    private void animateBackground(Color target) {
        if (hoverTimer != null && hoverTimer.isRunning()) {
            hoverTimer.stop();
        }

        Color start = currentBackground;
        int frames = 8;
        int[] frame = {0};

        hoverTimer = new Timer(16, e -> {
            frame[0]++;
            float ratio = Math.min(1f, frame[0] / (float) frames);
            currentBackground = blend(start, target, ratio);
            repaint();

            if (frame[0] >= frames) {
                ((Timer) e.getSource()).stop();
            }
        });
        hoverTimer.start();
    }

    private Color blend(Color start, Color end, float ratio) {
        int r = start.getRed() + Math.round((end.getRed() - start.getRed()) * ratio);
        int g = start.getGreen() + Math.round((end.getGreen() - start.getGreen()) * ratio);
        int b = start.getBlue() + Math.round((end.getBlue() - start.getBlue()) * ratio);
        return new Color(r, g, b);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(currentBackground);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

        if (active) {
            g2.setColor(UITheme.ACCENT);
            g2.fillRoundRect(0, 8, 4, getHeight() - 16, 5, 5);
        }

        if (badgeCount > 0) {
            String badgeText = badgeCount > 99 ? "99+" : String.valueOf(badgeCount);
            g2.setFont(UITheme.FONT_SMALL.deriveFont(Font.BOLD));
            FontMetrics metrics = g2.getFontMetrics();
            int badgeWidth = Math.max(22, metrics.stringWidth(badgeText) + 12);
            int badgeHeight = 18;
            int badgeX = collapsed ? getWidth() - badgeWidth - 3 : getWidth() - badgeWidth - 14;
            int badgeY = 7;

            g2.setColor(UITheme.ACCENT);
            g2.fillRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, 18, 18);
            g2.setColor(Color.WHITE);
            g2.drawString(
                    badgeText,
                    badgeX + (badgeWidth - metrics.stringWidth(badgeText)) / 2,
                    badgeY + ((badgeHeight - metrics.getHeight()) / 2) + metrics.getAscent()
            );
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
