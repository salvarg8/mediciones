package com.mediciones.view.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.border.AbstractBorder;

public class Button3D extends JButton {
    private Color baseColor;
    private Color highlightColor;
    private boolean isPressed = false;
    private final boolean rounded;

    // Constructor para botones con borde redondeado personalizado (como en FrmInicio)
    public Button3D(String text, Color bgColor, boolean rounded) {
        super(text);
        this.baseColor = bgColor;
        this.highlightColor = bgColor.brighter().brighter().brighter();
        this.rounded = rounded;

        setFocusPainted(false);
        setContentAreaFilled(false);
        
        if (rounded) {
            setBorder(new RoundedBorder(10));
        } else {
            setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        }
        
        setFont(new Font("Arial", Font.BOLD, 16));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                isPressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    // Constructor por defecto (como en la mayoría de los formularios)
    public Button3D(String text, Color bgColor) {
        this(text, bgColor, false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        if (rounded) {
            // Estilo FrmInicio y FrmCalibracionSensor
            int x = isPressed ? 2 : 1;
            int y = isPressed ? 2 : 1;
            if (!isPressed) {
                g2d.setColor(new Color(0, 0, 0, 30));
                g2d.fillRoundRect(2, 2, w - 3, h - 3, 10, 10);
            }
            g2d.setPaint(new GradientPaint(0, 0, isPressed ? baseColor.darker() : highlightColor, 0, h, baseColor));
            g2d.fillRoundRect(x, y, w - (isPressed ? 3 : 2), h - (isPressed ? 3 : 2), 10, 10);
            g2d.setColor(getForeground());
        } else {
            // Estilo FrmClienteCRUD, FrmOperadorCRUD, etc.
            Color topColor = isPressed ? baseColor.darker() : baseColor.brighter();
            g2d.setPaint(new GradientPaint(0, 0, topColor, 0, h, isPressed ? baseColor.darker().darker() : baseColor));
            g2d.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);
            g2d.setColor(baseColor.darker());
            g2d.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
            g2d.setColor(getForeground());
        }

        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(getText(), (w - fm.stringWidth(getText())) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);

        g2d.dispose();
    }
    
    private static class RoundedBorder extends AbstractBorder {
        private final int r;
        
        public RoundedBorder(int r) { 
            this.r = r; 
        }
        
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            g.drawRoundRect(x, y, w - 1, h - 1, r, r);
        }
    }
    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        this.baseColor = bg;
        this.highlightColor = bg.brighter().brighter().brighter();
        repaint();
    }
}