package com.mycompany.oop.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseWheelEvent;

public final class WorkforceFormToolkit {

    public static final String DATE_HELP = "Use yyyy-MM-dd, example: 2026-05-17";
    public static final String TIME_HELP = "Use HH:mm, example: 09:30 or 17:00";
    public static final String MONTH_HELP = "Use yyyy-MM for month periods, or yyyy-MM-dd when a full date is required";

    private static final Color FIELD_ERROR = new Color(220, 38, 38);
    private static final Color FIELD_MUTED = new Color(248, 250, 252);
    private static final Dimension FIELD_SIZE = new Dimension(220, 38);

    private WorkforceFormToolkit() {
    }

    public static JPanel createSection(String title, String subtitle) {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                new EmptyBorder(16, 18, 16, 18)
        ));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UITheme.FONT_BODY_BOLD);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(UITheme.FONT_SMALL);
        subtitleLabel.setForeground(UITheme.TEXT_SECONDARY);

        header.add(titleLabel);
        header.add(Box.createVerticalStrut(3));
        header.add(subtitleLabel);
        panel.add(header, BorderLayout.NORTH);
        return panel;
    }

    public static JPanel createFormGrid() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        return form;
    }

    public static JPanel createCompactFormCard(int maxWidth) {
        JPanel outer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        outer.setOpaque(false);

        JPanel card = new JPanel(new BorderLayout(0, 10)) {
            @Override
            public Dimension getPreferredSize() {
                Dimension preferred = super.getPreferredSize();
                return new Dimension(Math.min(maxWidth, Math.max(preferred.width, 320)), preferred.height);
            }
        };
        card.setOpaque(false);
        card.setMaximumSize(new Dimension(maxWidth, Integer.MAX_VALUE));
        outer.add(card);
        return outer;
    }

    public static JPanel getCompactFormBody(JPanel compactCard) {
        if (compactCard == null || compactCard.getComponentCount() == 0
                || !(compactCard.getComponent(0) instanceof JPanel card)) {
            return compactCard;
        }
        return card;
    }

    public static JPanel createTwoColumnFieldGrid() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        return form;
    }

    public static void addFieldBlock(JPanel form, String label, JComponent field,
            String helpText, int gridx, int gridy) {
        addFieldBlock(form, label, field, helpText, gridx, gridy, 1);
    }

    public static void addFieldBlock(JPanel form, String label, JComponent field,
            String helpText, int gridx, int gridy, int gridwidth) {
        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));

        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setFont(UITheme.FONT_SMALL);
        fieldLabel.setForeground(UITheme.TEXT_SECONDARY);
        fieldLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        stack.add(fieldLabel);
        stack.add(Box.createVerticalStrut(5));

        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension fieldSize = field.getPreferredSize();
        int maxWidth = gridwidth > 1 ? 620 : 300;
        field.setMaximumSize(new Dimension(maxWidth, Math.max(38, fieldSize.height)));
        if (field instanceof JTextField || field instanceof JComboBox<?>) {
            field.setPreferredSize(new Dimension(gridwidth > 1 ? 560 : 260, Math.max(38, fieldSize.height)));
        }
        stack.add(field);

        if (helpText != null && !helpText.isBlank()) {
            JLabel helpLabel = createHelpLabel(helpText);
            helpLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            stack.add(Box.createVerticalStrut(3));
            stack.add(helpLabel);
        }

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = gridx;
        constraints.gridy = gridy;
        constraints.gridwidth = gridwidth;
        constraints.weightx = gridwidth > 1 ? 1 : 0.5;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, 10, gridx == 0 && gridwidth == 1 ? 12 : 0);
        form.add(stack, constraints);
    }

    public static JTextField addTextField(JPanel form, String label, int row) {
        return addTextField(form, label, row, null);
    }

    public static JTextField addTextField(JPanel form, String label, int row, String helpText) {
        JTextField field = new JTextField();
        styleTextField(field);
        addRow(form, label, field, row, helpText);
        return field;
    }

    public static <T> JComboBox<T> addComboBox(JPanel form, String label, T[] values, int row) {
        return addComboBox(form, label, values, row, null);
    }

    public static <T> JComboBox<T> addComboBox(JPanel form, String label, T[] values, int row, String helpText) {
        JComboBox<T> comboBox = new JComboBox<>(values);
        styleComboBox(comboBox);
        addRow(form, label, comboBox, row, helpText);
        return comboBox;
    }

    public static void addRow(JPanel form, String label, JComponent field, int row) {
        addRow(form, label, field, row, null);
    }

    public static void addRow(JPanel form, String label, JComponent field, int row, String helpText) {
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setFont(UITheme.FONT_SMALL);
        fieldLabel.setForeground(UITheme.TEXT_SECONDARY);

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(0, 0, 8, 12);
        form.add(fieldLabel, labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(0, 0, 8, 0);
        form.add(createFieldStack(field, helpText), fieldConstraints);
    }

    public static JLabel createHelpLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UITheme.FONT_SMALL);
        label.setForeground(new Color(100, 116, 139));
        return label;
    }

    public static JLabel createStatusChip(String text, Color color) {
        JLabel chip = new JLabel(text == null || text.isBlank() ? "Status" : text);
        chip.setFont(UITheme.FONT_SMALL);
        chip.setForeground(color == null ? UITheme.TEXT_SECONDARY : color);
        chip.setOpaque(true);
        chip.setBackground(new Color(248, 250, 252));
        chip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                new EmptyBorder(4, 9, 4, 9)
        ));
        return chip;
    }

    public static JLabel createEmptyStateLabel(String message) {
        JLabel label = new JLabel(message == null || message.isBlank() ? "No records to display." : message);
        label.setFont(UITheme.FONT_BODY);
        label.setForeground(UITheme.TEXT_SECONDARY);
        label.setBorder(new EmptyBorder(18, 16, 18, 16));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    public static JPanel createFieldStack(JComponent field, String helpText) {
        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        stack.add(field);

        if (helpText != null && !helpText.isBlank()) {
            JLabel helpLabel = createHelpLabel(helpText);
            helpLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            stack.add(Box.createVerticalStrut(3));
            stack.add(helpLabel);
        }

        return stack;
    }

    public static JLabel createInlineMessage() {
        JLabel label = new JLabel(" ");
        label.setFont(UITheme.FONT_SMALL);
        label.setForeground(UITheme.TEXT_SECONDARY);
        return label;
    }

    public static void setInfo(JLabel label, String message) {
        label.setForeground(UITheme.TEXT_SECONDARY);
        label.setText(message == null || message.isBlank() ? " " : message);
    }

    public static void setSuccess(JLabel label, String message) {
        label.setForeground(UITheme.SUCCESS);
        label.setText(message);
    }

    public static void setError(JLabel label, String message) {
        label.setForeground(FIELD_ERROR);
        label.setText(message);
    }

    public static void styleTextField(JTextField field) {
        field.setFont(UITheme.FONT_BODY);
        field.setBackground(Color.WHITE);
        field.setPreferredSize(FIELD_SIZE);
        field.setMinimumSize(FIELD_SIZE);
        setFieldNormal(field);
    }

    public static void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setFont(UITheme.FONT_BODY);
        comboBox.setBackground(Color.WHITE);
        comboBox.setPreferredSize(FIELD_SIZE);
        comboBox.setMinimumSize(FIELD_SIZE);
        comboBox.setFocusable(false);
        comboBox.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1));
        comboBox.setMaximumRowCount(8);
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setBorder(new EmptyBorder(7, 10, 7, 10));
                label.setFont(UITheme.FONT_BODY);
                return label;
            }
        });
    }

    public static void styleDateEditor(JTextField editor) {
        styleTextField(editor);
        applyDateHelp(editor);
    }

    public static void applyDateHelp(JTextField field) {
        field.setToolTipText(DATE_HELP);
    }

    public static void applyTimeHelp(JTextField field) {
        field.setToolTipText(TIME_HELP);
    }

    public static void applyMonthHelp(JComponent component) {
        component.setToolTipText(MONTH_HELP);
    }

    public static void tuneScrollPane(JScrollPane scrollPane) {
        scrollPane.getVerticalScrollBar().setUnitIncrement(36);
        scrollPane.getVerticalScrollBar().setBlockIncrement(300);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(32);
        scrollPane.getHorizontalScrollBar().setBlockIncrement(300);
        scrollPane.setWheelScrollingEnabled(true);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.addMouseWheelListener(event -> forwardWheelAtScrollEdge(scrollPane, event));
    }

    private static void forwardWheelAtScrollEdge(JScrollPane scrollPane, MouseWheelEvent event) {
        JScrollBar bar = scrollPane.getVerticalScrollBar();
        if (bar == null || !bar.isVisible()) {
            forwardWheelToParent(scrollPane, event);
            return;
        }

        int value = bar.getValue();
        int max = bar.getMaximum() - bar.getVisibleAmount();
        boolean scrollingUpAtTop = event.getWheelRotation() < 0 && value <= 0;
        boolean scrollingDownAtBottom = event.getWheelRotation() > 0 && value >= max;

        if (scrollingUpAtTop || scrollingDownAtBottom) {
            forwardWheelToParent(scrollPane, event);
        }
    }

    private static void forwardWheelToParent(JScrollPane scrollPane, MouseWheelEvent event) {
        Container parent = scrollPane.getParent();
        while (parent != null && !(parent instanceof JScrollPane)) {
            parent = parent.getParent();
        }
        if (parent instanceof JScrollPane parentScroll) {
            parentScroll.dispatchEvent(SwingUtilities.convertMouseEvent(scrollPane, event, parentScroll));
        }
    }

    public static void setFieldError(JTextField field, String tooltip) {
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FIELD_ERROR, 2),
                new EmptyBorder(5, 9, 5, 9)
        ));
        field.setToolTipText(tooltip);
    }

    public static void setFieldNormal(JTextField field) {
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1),
                new EmptyBorder(6, 10, 6, 10)
        ));
    }

    public static void setFieldReadOnly(JTextField field, boolean readOnly) {
        field.setEditable(!readOnly);
        field.setBackground(readOnly ? FIELD_MUTED : Color.WHITE);
    }

    public static JPanel createButtonRow(int alignment) {
        JPanel row = new JPanel(new FlowLayout(alignment, 8, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(6, 0, 0, 0));
        return row;
    }
}
