package view;

import com.mammb.code.piecetable.Pos;
import nikolai.Buffer;

import javax.swing.*;
import java.awt.*;

public class EditorView extends JComponent {
    private final Buffer buffer;
    private final Font font = new Font("Monospaced", Font.PLAIN, 14);
    private final int lineHeight = 16;
    private final int charWidth = 8;

    public EditorView(Buffer buffer) {
        this.buffer = buffer;
       setFocusable(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setFont(font);

        var lines = buffer.getViewPortTextLines(getVisibleLineCount());

        int y = lineHeight;
        for (String line : lines) {
            g2.drawString(line, 5, y);
            y += lineHeight;
        }

        paintCursor(g2);
    }

    private void paintCursor(Graphics2D g2) {
        Pos cursor = buffer.getCurrentCursorPos();
        int row = cursor.row();
        int col = cursor.col();

        int x = 5 + col * charWidth;
        int y = (row + 1) * lineHeight;

        g2.drawLine(x, y - lineHeight, x, y);
    }

    private int getVisibleLineCount() {
        return getHeight() / lineHeight;
    }
    public int getLineHeight() {
        return lineHeight;
    }

    public Font getEditorFont() {
        return font;
    }
}
