package nikolai;

import com.mammb.code.piecetable.Pos;
import com.mammb.code.piecetable.Range;
import com.mammb.code.piecetable.TextEdit;
import io.github.treesitter.jtreesitter.Tree;
import treesitter.TsxSyntax;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Buffer {

    private int scrollOffset = 0;
    private int maxLines = 20;
    private int preferredCol = 0;
    private TextEdit edit;

    private final String fileName;
    private Tree tree;

    public List<List<TsxSyntax.SyntaxToken>> getToken() {
        return token;
    }

    private List<List<TsxSyntax.SyntaxToken>> token = new ArrayList<>();

    private Pos cursor = new Pos(0,0);

    public Pos getCurrentCursorPos() {
        return new Pos(cursor.row() - scrollOffset, cursor.col());
    }

    public Buffer(Path path)  {
       this.fileName = path.toString();
       this.edit = TextEdit.of(path);

       updateToken();
    }

    public void clear() {
       this.edit = TextEdit.of();
        cursor = new Pos(0,0);
        scrollOffset = 0;
    }

    public String getCurrentLine() {
      return  getTextAtLine(this.cursor.row()).trim();
    }

    public String getTextAtLine(int row) {
        return edit.getText(new Pos(row,0), new Pos(row+1,0));
    }

    public Buffer(String fileName)  {
        this.fileName = fileName;
        this.edit = TextEdit.of();
        updateToken();
    }

    public void parseText() {
            this.tree = TsxSyntax.parse(getText()).get();
    }

    public void updateToken() {
        parseText();
        this.token = TsxSyntax.collectTokens(this.tree);
    }

    public void moveCursor(Direction dir, int steps) {
        if(steps < 0) throw new IllegalArgumentException("steps is negative");

        int row = cursor.row();
        int col = cursor.col();

        switch(dir) {
            case UP -> row -= steps;
            case DOWN -> row += steps;

            case LEFT -> {
                for(int i=0; i<steps; i++) {
                    if(col > 0) {
                        col--;
                    } else if(row > 0) {
                        row--;
                        String prevLine = edit.getText(new Pos(row,0), new Pos(row+1,0));
                        col = prevLine.length();
                    }
                }
            }

            case RIGHT -> {
                for(int i=0; i<steps; i++) {
                    String line = edit.getText(new Pos(row,0), new Pos(row+1,0));
                    if(col < line.length()) {
                        col++;
                    } else if(row < edit.rows() - 1) {
                        row++;
                        col = 0;
                    }
                }
            }
        }

        // clamp row
        row = Math.max(0, Math.min(row, Math.max(0, edit.rows()-1)));

        // preferredCol nur bei UP/DOWN
        if(dir == Direction.UP || dir == Direction.DOWN) {
            preferredCol = col;
        }

        // clamp col zur Zeilenlänge
        String line = "";
        if(edit.rows() > 0) {
            line = edit.getText(new Pos(row,0), new Pos(row+1,0));
        }
        if(dir == Direction.UP || dir == Direction.DOWN) {
            col = Math.min(preferredCol, line.length());
        } else {
            col = Math.max(0, Math.min(col, line.length()));
        }

        cursor = new Pos(row, col);

        revalidateViewPort();
    }

    public List<String> getViewPortTextLines(int maxLines) {


        if(maxLines<0) {
            throw new IllegalArgumentException("maxLines is negative");
        }

        this.maxLines = maxLines;

        int maxRows = edit.rows();
        if (maxRows == 0) {
            return List.of("");
        }
        scrollOffset = Math.min(scrollOffset, maxRows);
        int endRow = Math.min( scrollOffset + maxLines,  maxRows);
        return   edit.getTexts(new Pos(scrollOffset,0), new Pos(endRow,0) );
    }

    public void backspace() {
        this.cursor = this.edit.backspace(List.of(cursor)).getFirst();
        revalidateViewPort();
    }

    public void addChar(char c) {
        this.cursor = edit.insert(List.of(cursor), String.valueOf(c)).getFirst();
        revalidateViewPort();
    }

    public void addText(String text) {
        this.cursor = this.edit.insert(List.of(cursor), text).getFirst();
        revalidateViewPort();
    }

    private void revalidateViewPort() {
        if (cursor.row() < scrollOffset) {
            scrollOffset = cursor.row();
        } else if (cursor.row() >= scrollOffset + maxLines) {
            scrollOffset = cursor.row() - maxLines + 1;
        }
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public int getTotalRows() {
        return edit.rows();
    }

    public String getText() {
        return edit.getText(0,0,getTotalRows(),0);
    }

    public String getFileName() {
        return this.fileName;
    }

}