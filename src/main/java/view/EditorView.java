package view;


import nikolai.Buffer;
import nikolai.piecetable.Pos;
import treesitter.TsxSyntax;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

public class EditorView extends JComponent {
    private Buffer buffer;
    private final Font font = new Font("Monospaced", Font.PLAIN, 14);
    private final int lineHeight = 16;
    private final int charWidth = 8;
    private final java.util.List<String> textBox = new ArrayList<>();
    private int textBoxIndex = 0;

    public EditorView(Buffer buffer) {
        this.buffer = buffer;
       setFocusable(true);
    }

    public void setTextBox( java.util.List<String> items) {
        if(items == null) {
            return;
        }
        this.textBox.clear();
        this.textBox.addAll(items);
        this.textBoxIndex = 0;
    }

    public void clearTextBox() {
        this.textBox.clear();
        this.textBoxIndex = 0;
    }

    public String getCurrentSelection() {
        return this.textBox.get(this.textBoxIndex);
    }

    public void nextText() {
        if(this.textBox.isEmpty()) {
            return;
        }
        this.textBoxIndex = Math.min(this.textBoxIndex + 1, this.textBox.size() - 1);
    }


    public void prevText() {
        this.textBoxIndex = Math.max(this.textBoxIndex - 1, 0);
    }

    public void changeBuffer(Buffer buffer) {
        if(this.buffer == buffer) {
            return;
        }
        this.buffer = buffer;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        var lines = buffer.getViewPortTextLines(getVisibleLineCount());
        var tokens = buffer.getToken();
        int y = lineHeight;
        int index = buffer.getScrollOffset();



        g2.setBackground(Color.BLACK);
        g2.fillRect(0,0, getWidth(), getHeight());

            for (String line : lines) {

                    int tokenEndPosition = 0;
                    if(!tokens.isEmpty() && tokens.size() > index) {
                        int x = 5;
                        for(var token: tokens.get(index)) {

                            if(tokenEndPosition<token.startCol()) {
                                g2.setColor(Color.WHITE);
                                x = drawTextToken(line, tokenEndPosition, token.startCol(), g2, x, y, fm);
                            }
                            tokenEndPosition = token.endCol();

                            g2.setColor(token.color());
                            var endCol = token.endCol();
                            if(token.endRow() > token.startRow()) {
                                endCol = line.length();
                                tokenEndPosition = endCol;
                            }

                            x = drawTextToken(line, token.startCol(), endCol, g2, x, y, fm);

                        }

                        if(tokenEndPosition<line.length()) {
                            g2.setColor(Color.WHITE);
                            drawTextToken(line, tokenEndPosition, line.length(), g2, x, y, fm);
                        }
                    }else{
                        g2.setColor(Color.WHITE);
                        g2.drawString(line, 5, y);
                    }
                y += lineHeight;

                index++;
            }



        paintCursor(g2);

        // TextBox
        if(!textBox.isEmpty()) {

            g2.setColor(Color.GRAY);
            g2.fillRect(50,50,200,lineHeight + textBox.size() * lineHeight);
            g2.setColor(Color.WHITE);
            int in = 0;
            for(String item:textBox) {
                if(textBoxIndex == in) {
                    g2.fillRect(50, 50 + in * lineHeight, 10,lineHeight);
                }
                g2.drawString(item, 60, 50 + lineHeight + (in * lineHeight));
                in++;
            }
        }
    }

    private static int drawTextToken(String line, int token, int endCol, Graphics2D g2, int x, int y, FontMetrics fm) {
       String tokenText;
       if(line.length()<endCol) {
           tokenText = line.substring(Math.min(token,line.length()), line.length());
       }else{
           tokenText = line.substring(Math.min(token,line.length()), endCol);
       }

        g2.drawString(tokenText, x, y);
        x += fm.stringWidth(tokenText);
        return x;
    }

    private void paintCursor(Graphics2D g2) {
        Pos cursor = buffer.getCurrentCursorPos();
        int row = cursor.line();
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
