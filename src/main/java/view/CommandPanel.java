package view;

import javax.swing.*;
import java.awt.*;

public class CommandPanel extends JPanel {

    private StringBuilder text = new StringBuilder();
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        FontMetrics fm = g.getFontMetrics();
        int ascent = fm.getAscent();

        g.setColor(Color.BLUE);
        g.fillRect(0,0,getWidth(),getHeight());


            g.setColor( Color.GREEN);

            g.drawString(text.toString(), 5,ascent+5);

    }

    public String getText() {
        return text.toString();
    }

    public void addChar(char c) {
        text.append(c);
        repaint();
    }

    public void del() {
        if(text.length() == 0) {
            return;
        }
        text.deleteCharAt(text.length()-1);
        repaint();
    }

    public void clear() {
        this.text.setLength(0);
        repaint();
    }
}
