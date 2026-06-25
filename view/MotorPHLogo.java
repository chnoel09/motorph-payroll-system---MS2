package com.mycompany.oop.view;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

final class MotorPHLogo {

    static final String RESOURCE_PATH = "/images/motorph_logo.png";

    private MotorPHLogo() {
    }

    static JLabel createLabel(int maxWidth, int maxHeight, String fallbackText, Font fallbackFont, Color fallbackColor) {
        JLabel label = new JLabel(fallbackText == null ? "MotorPH" : fallbackText, SwingConstants.CENTER);
        label.setFont(fallbackFont);
        label.setForeground(fallbackColor);

        ImageIcon icon = loadIcon(maxWidth, maxHeight);
        if (icon != null) {
            label.setText("");
            label.setIcon(icon);
        }

        return label;
    }

    static ImageIcon loadIcon(int maxWidth, int maxHeight) {
        URL logoUrl = MotorPHLogo.class.getResource(RESOURCE_PATH);
        if (logoUrl == null) {
            return null;
        }

        ImageIcon original = new ImageIcon(logoUrl);
        int sourceWidth = original.getIconWidth();
        int sourceHeight = original.getIconHeight();
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            return null;
        }

        double scale = Math.min(maxWidth / (double) sourceWidth, maxHeight / (double) sourceHeight);
        int targetWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
        int targetHeight = Math.max(1, (int) Math.round(sourceHeight * scale));
        Image scaled = original.getImage().getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }
}
