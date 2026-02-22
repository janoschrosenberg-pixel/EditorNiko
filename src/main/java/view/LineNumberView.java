package view;

import nikolai.Buffer;

import javax.swing.*;
import java.awt.*;

public class LineNumberView extends JComponent {

    private Buffer buffer;
    private final EditorView editorView;
    private final int padding = 6;

    public LineNumberView(Buffer buffer, EditorView editorView) {
        this.buffer = buffer;
        this.editorView = editorView;
        setFont(editorView.getEditorFont());
    }

    public void changeBuffer(Buffer buffer){
        this.buffer = buffer;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(   new Color(0xC51162));
        g.fillRect(0,0,getWidth(),getHeight());

        Graphics2D g2 = (Graphics2D) g;
        g2.setFont(getFont());

        int lineHeight = editorView.getLineHeight();
        int visibleLines = getHeight() / lineHeight;
        int startLine = buffer.getScrollOffset();

        FontMetrics fm = g2.getFontMetrics();
        int y = lineHeight;

        g.setColor(   new Color(0xAEEA00));
        for (int i = 0; i < visibleLines; i++) {
            int lineNumber = startLine + i + 1;
            if (lineNumber > buffer.getTotalRows()) break;

            String text = String.valueOf(lineNumber);
            int x = getWidth() - padding - fm.stringWidth(text);

            g2.drawString(text, x, y);
            y += lineHeight;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        int digits = Math.max(3,
                String.valueOf(buffer.getTotalRows()).length());
        int width = digits * 8 + padding * 2;
        return new Dimension(width, 0);
    }
}
