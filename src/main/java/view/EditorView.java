package view;

import com.mammb.code.piecetable.Pos;
import nikolai.Buffer;
import treesitter.TsxSyntax;

import javax.swing.*;
import java.awt.*;

public class EditorView extends JComponent {
    private Buffer buffer;
    private final Font font = new Font("Monospaced", Font.PLAIN, 14);
    private final int lineHeight = 16;
    private final int charWidth = 8;

    public EditorView(Buffer buffer) {
        this.buffer = buffer;
       setFocusable(true);
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
