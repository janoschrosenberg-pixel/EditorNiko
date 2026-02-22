package view;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class StatusPanel extends JPanel {
   private List<StatusProp> statusText = new ArrayList<>();
   private Color backGroundColor = new Color(0x6200EA);

    public void setProps(List<StatusProp> props) {
        this.statusText = props;
    }

    public void setBackGroundColor(Color color) {
        this.backGroundColor = color;
    }
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        FontMetrics fm = g.getFontMetrics();
        int ascent = fm.getAscent();

        g.setColor(backGroundColor);
        g.fillRect(0,0,getWidth(),getHeight());

        int x = 5;
        for (int i=0; i<statusText.size();i++) {
            g.setColor( statusText.get(i).color());
            var name = statusText.get(i).name();
            var value = statusText.get(i).value();

            var text = name+": "+value+" ";

            g.drawString(text, x,ascent+5);

            x+= fm.stringWidth(text);
        }

    }
}
