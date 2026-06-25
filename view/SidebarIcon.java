package com.mycompany.oop.view;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

public class SidebarIcon implements Icon {

    public enum Type {
        DASHBOARD,
        USERS,
        WALLET,
        HISTORY,
        CALENDAR,
        SETTINGS,
        PAYSLIP,
        PROFILE,
        LOGOUT
    }

    private final Type type;
    private final int size;

    public SidebarIcon(Type type) {
        this(type, 18);
    }

    public SidebarIcon(Type type, int size) {
        this.type = type;
        this.size = size;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(c.getForeground());

        int s = size;
        switch (type) {
            case DASHBOARD -> paintDashboard(g2, x, y, s);
            case USERS -> paintUsers(g2, x, y, s);
            case WALLET -> paintWallet(g2, x, y, s);
            case HISTORY -> paintHistory(g2, x, y, s);
            case CALENDAR -> paintCalendar(g2, x, y, s);
            case SETTINGS -> paintSettings(g2, x, y, s);
            case PAYSLIP -> paintPayslip(g2, x, y, s);
            case PROFILE -> paintProfile(g2, x, y, s);
            case LOGOUT -> paintLogout(g2, x, y, s);
        }

        g2.dispose();
    }

    private void paintDashboard(Graphics2D g2, int x, int y, int s) {
        int gap = 3;
        int w = (s - gap) / 2;
        g2.draw(new RoundRectangle2D.Double(x + 1, y + 1, w, w + 1, 3, 3));
        g2.draw(new RoundRectangle2D.Double(x + w + gap, y + 1, w, w - 2, 3, 3));
        g2.draw(new RoundRectangle2D.Double(x + 1, y + w + gap + 1, w, w - 2, 3, 3));
        g2.draw(new RoundRectangle2D.Double(x + w + gap, y + w + gap - 1, w, w + 1, 3, 3));
    }

    private void paintUsers(Graphics2D g2, int x, int y, int s) {
        g2.draw(new Ellipse2D.Double(x + 6, y + 2, 6, 6));
        g2.draw(new Arc2D.Double(x + 3, y + 9, 12, 8, 0, 180, Arc2D.OPEN));
        g2.draw(new Ellipse2D.Double(x + 12, y + 5, 4, 4));
        g2.draw(new Arc2D.Double(x + 10, y + 10, 8, 6, 0, 140, Arc2D.OPEN));
    }

    private void paintWallet(Graphics2D g2, int x, int y, int s) {
        g2.draw(new RoundRectangle2D.Double(x + 1, y + 4, s - 2, s - 7, 4, 4));
        g2.drawLine(x + 3, y + 7, x + s - 3, y + 7);
        g2.draw(new RoundRectangle2D.Double(x + s - 8, y + 9, 6, 4, 2, 2));
    }

    private void paintHistory(Graphics2D g2, int x, int y, int s) {
        g2.draw(new Arc2D.Double(x + 2, y + 2, s - 4, s - 4, 35, 285, Arc2D.OPEN));
        Path2D arrow = new Path2D.Double();
        arrow.moveTo(x + 3, y + 5);
        arrow.lineTo(x + 3, y + 10);
        arrow.lineTo(x + 8, y + 10);
        g2.draw(arrow);
        g2.drawLine(x + s / 2, y + 6, x + s / 2, y + s / 2);
        g2.drawLine(x + s / 2, y + s / 2, x + s - 5, y + s / 2);
    }

    private void paintCalendar(Graphics2D g2, int x, int y, int s) {
        g2.draw(new RoundRectangle2D.Double(x + 2, y + 3, s - 4, s - 5, 3, 3));
        g2.drawLine(x + 2, y + 8, x + s - 2, y + 8);
        g2.drawLine(x + 6, y + 1, x + 6, y + 5);
        g2.drawLine(x + s - 6, y + 1, x + s - 6, y + 5);
        g2.fillOval(x + 6, y + 11, 2, 2);
        g2.fillOval(x + 11, y + 11, 2, 2);
    }

    private void paintSettings(Graphics2D g2, int x, int y, int s) {
        g2.drawOval(x + 5, y + 5, s - 10, s - 10);
        g2.drawOval(x + 8, y + 8, s - 16, s - 16);
        g2.drawLine(x + s / 2, y + 1, x + s / 2, y + 4);
        g2.drawLine(x + s / 2, y + s - 4, x + s / 2, y + s - 1);
        g2.drawLine(x + 1, y + s / 2, x + 4, y + s / 2);
        g2.drawLine(x + s - 4, y + s / 2, x + s - 1, y + s / 2);
    }

    private void paintPayslip(Graphics2D g2, int x, int y, int s) {
        g2.draw(new RoundRectangle2D.Double(x + 3, y + 1, s - 6, s - 2, 3, 3));
        g2.drawLine(x + 6, y + 6, x + s - 6, y + 6);
        g2.drawLine(x + 6, y + 10, x + s - 6, y + 10);
        g2.drawLine(x + 6, y + 14, x + s - 9, y + 14);
    }

    private void paintProfile(Graphics2D g2, int x, int y, int s) {
        g2.draw(new Ellipse2D.Double(x + 6, y + 3, 6, 6));
        g2.draw(new Arc2D.Double(x + 3, y + 10, 12, 8, 0, 180, Arc2D.OPEN));
    }

    private void paintLogout(Graphics2D g2, int x, int y, int s) {
        g2.drawLine(x + 2, y + 3, x + 2, y + s - 3);
        g2.drawLine(x + 2, y + 3, x + 9, y + 3);
        g2.drawLine(x + 2, y + s - 3, x + 9, y + s - 3);
        g2.drawLine(x + 8, y + s / 2, x + s - 2, y + s / 2);
        g2.drawLine(x + s - 6, y + s / 2 - 4, x + s - 2, y + s / 2);
        g2.drawLine(x + s - 6, y + s / 2 + 4, x + s - 2, y + s / 2);
    }
}
