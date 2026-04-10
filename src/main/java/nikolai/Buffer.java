package nikolai;

import io.github.treesitter.jtreesitter.Tree;
import nikolai.piecetable.PieceTable;
import nikolai.piecetable.Pos;
import treesitter.TsxSyntax;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Buffer {

    private int scrollOffset = 0;
    private int maxLines = 20;
    private int preferredCol = 0;
    private PieceTable edit;
    private boolean newFile;
    private final List<Selection> selections = new ArrayList<>();
    private List<TsxSyntax.SyntaxToken> sortedTokenList;
    private int currentTokenPos = 0;

    private final String fileName;
    private Tree tree;

    public List<List<TsxSyntax.SyntaxToken>> getToken() {
        return token;
    }

    private List<List<TsxSyntax.SyntaxToken>> token = new ArrayList<>();

    private Pos cursor = new Pos(0,0);

    public Pos getCurrentCursorPos() {
        return new Pos(cursor.line() - scrollOffset, cursor.col());
    }

    public Buffer(Path path)  {
       this.fileName = path.toString();
       this.edit = new PieceTable(Utils.leseDatei(path));

       updateToken();
    }

    public void clear() {
       this.edit = new PieceTable();
        cursor = new Pos(0,0);
        scrollOffset = 0;
    }

    public List<Selection> getSelections() {
        return this.selections;
    }

    public void addSelection(Selection selection) {
        this.selections.add(selection);
    }

    public String getCurrentLine() {
      return  getTextAtLine(this.cursor.line()).trim();
    }

    public Pos getCursor() {
        return this.cursor;
    }
    public String getTextAtLine(int row) {
        return edit.getTextAtLine(row);
    }

    public List<String> getTextFromSelections(List<Selection> selections) {
        return this.edit.getTextFromSelections(selections);
    }

    public String getTextFromCurrentToken() {
        var token = this.getCurrentToken();
        if(token == null) {
            return null;
        }
        var posStart = new Pos(token.startRow(), token.startCol());
        var posEnd = new Pos(token.endRow(), token.endCol());
        return this.getTextFromSelections(List.of(new Selection(posStart, posEnd))).getFirst();
    }

    public Buffer(String fileName)  {
        this.newFile = true;
        this.fileName = fileName;
        this.edit = new PieceTable();
        updateToken();
    }

    private List<TsxSyntax.SyntaxToken> getSortedTokenAtLine(int line) {
        if(line >= token.size()) {
            return new ArrayList<>();
        }
        var tokens = token.get(line);
        return tokens.stream()
                .sorted(Comparator.comparingInt(TsxSyntax.SyntaxToken::startCol))
                .toList();
    }

    public void moveToToken(Direction dir) {
        if (this.sortedTokenList == null) {
            this.sortedTokenList = buildSortedTokenList();
            this.currentTokenPos = findNextTokenIndex(this.sortedTokenList, cursor.line(), cursor.col());

            TsxSyntax.SyntaxToken token = this.sortedTokenList.get(this.currentTokenPos);

            boolean inside =
                    cursor.line() == token.startRow() &&
                            cursor.col() >= token.startCol() &&
                            cursor.col() <= token.endCol();

            if (inside) {
                if (dir == Direction.RIGHT) {
                    this.currentTokenPos = Math.min(this.currentTokenPos + 1, this.sortedTokenList.size() - 1);
                } else if (dir == Direction.LEFT) {
                    this.currentTokenPos = Math.max(this.currentTokenPos - 1, 0);
                }
            }
        } else {
            switch (dir) {
                case RIGHT -> this.currentTokenPos = Math.min(this.currentTokenPos + 1, this.sortedTokenList.size() - 1);
                case LEFT  -> this.currentTokenPos = Math.max(this.currentTokenPos - 1, 0);
            }
        }

        var nextToken = this.sortedTokenList.get(this.currentTokenPos);
        cursor = new Pos(nextToken.startRow(), nextToken.startCol());

        revalidateViewPort();
    }




    private List<TsxSyntax.SyntaxToken> buildSortedTokenList() {
        int totalSize = this.token.stream().mapToInt(List::size).sum();

        List<TsxSyntax.SyntaxToken> result = new ArrayList<>(totalSize);

        for (List<TsxSyntax.SyntaxToken> tokens : this.token) {
            result.addAll(tokens);
        }

        result.sort(Comparator
                .comparingInt(TsxSyntax.SyntaxToken::startRow)
                .thenComparingInt(TsxSyntax.SyntaxToken::startCol));

        return result;
    }

    private int findNextTokenIndex(List<TsxSyntax.SyntaxToken> tokens, int cursorRow, int cursorCol) {
        if (tokens == null || tokens.isEmpty()) return -1;

        int left = 0;
        int right = tokens.size() - 1;

        while (left <= right) {
            int mid = (left + right) >>> 1;
            TsxSyntax.SyntaxToken token = tokens.get(mid);

            // Cursor liegt vor diesem Token
            if (cursorRow < token.startRow() ||
                    (cursorRow == token.startRow() && cursorCol < token.startCol())) {
                right = mid - 1;
            }
            // Cursor liegt nach diesem Token
            else if (cursorRow > token.endRow() ||
                    (cursorRow == token.endRow() && cursorCol > token.endCol())) {
                left = mid + 1;
            }
            else {
                // Cursor liegt innerhalb oder genau auf startCol
                return mid;
            }
        }

        // Kein direkter Treffer → nächstes Token bestimmen
        if (left >= tokens.size()) {
            return tokens.size() - 1; // nach allen Tokens
        }

        if (left < 0) {
            return 0; // vor allen Tokens
        }

        return left; // nächstes Token
    }

    public void saveFile(String fileName) {

        Utils.speichereDatei(this.edit.getText(), fileName);

        this.newFile = false;
    }

    public boolean isNewFile(){
        return this.newFile;
    }

    public void parseText() {
            this.tree = TsxSyntax.parse(getText()).get();
    }

    public void updateToken() {
        parseText();
        this.token = TsxSyntax.collectTokens(this.tree);
        this.sortedTokenList = buildSortedTokenList();
        this.currentTokenPos = findNextTokenIndex(this.sortedTokenList, cursor.line(), cursor.col());
    }

    public void moveCursor(Direction dir, int steps) {
        if(steps < 0) throw new IllegalArgumentException("steps is negative");

        int row = cursor.line();
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
                        String prevLine = edit.getTextAtLine(row);
                        col = prevLine.length();
                    }
                }
            }

            case RIGHT -> {
                for(int i=0; i<steps; i++) {
                    String line = edit.getTextAtLine(row);
                    if(col < line.length()) {
                        col++;
                    } else if(row < edit.getTotalLines() - 1) {
                        row++;
                        col = 0;
                    }
                }
            }
        }

        // clamp row
        row = Math.max(0, Math.min(row, Math.max(0, edit.getTotalLines()-1)));

        // preferredCol nur bei UP/DOWN
        if(dir == Direction.UP || dir == Direction.DOWN) {
            preferredCol = col;
        }

        // clamp col zur Zeilenlänge
        String line = "";
        if(edit.getTotalLines() > 0) {
            line = edit.getTextAtLine(row);
        }
        if(dir == Direction.UP || dir == Direction.DOWN) {
            col = Math.min(preferredCol, line.length());
        } else {
            col = Math.max(0, Math.min(col, line.length()));
        }

        cursor = new Pos(row, col);

        if(this.sortedTokenList != null) {
            this.currentTokenPos = findNextTokenIndex(this.sortedTokenList, cursor.line(), cursor.col());
        }
        revalidateViewPort();
    }

    public void setCursor(Pos cursor) {
        this.cursor = cursor;
        revalidateViewPort();
    }

    public String getTextFromStartlineTo(Pos pos) {
      return  this.edit.getTextAtLine(pos.line()).substring(0,pos.col());
    }


    public List<String> getViewPortTextLines(int maxLines) {


        if(maxLines<0) {
            throw new IllegalArgumentException("maxLines is negative");
        }

        this.maxLines = maxLines;

        int maxRows = edit.getTotalLines();
        if (maxRows == 0) {
            return List.of("");
        }
        scrollOffset = Math.min(scrollOffset, maxRows);
        int endRow = Math.min( scrollOffset + maxLines,  maxRows);
        return  edit.getLines(new Pos(scrollOffset,0), new Pos(endRow,0) );
    }

    public void backspace() {
        this.cursor = this.edit.backspace(cursor);
        revalidateViewPort();
    }

    public void addChar(char c) {
        this.cursor = edit.insertText( String.valueOf(c),cursor);
        revalidateViewPort();
    }

    public void addReturn() {
        this.cursor = edit.addNewline(  this.cursor);
    }



    public void addText(String text) {
        this.cursor = this.edit.insertText(text,  this.cursor);
        revalidateViewPort();
    }

    private void revalidateViewPort() {
        if (cursor.line() < scrollOffset) {
            scrollOffset = cursor.line();
        } else if (cursor.line() >= scrollOffset + maxLines) {
            scrollOffset = cursor.line() - maxLines + 1;
        }
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public int getTotalRows() {
        return edit.getTotalLines();
    }

    public String getText() {
        return edit.getText();
    }

    public String getFileName() {
        return this.fileName;
    }


    public void jumpToBegin() {
        this.cursor = new Pos(0,0);
        revalidateViewPort();
    }


    public TsxSyntax.SyntaxToken getCurrentToken() {
        if (this.sortedTokenList == null) {
           return null;
        }
       return sortedTokenList.get( this.currentTokenPos );
    }

    public void jumoToEnd() {
        var lastLine = edit.getTotalLines()-1;
        var lastCol = edit.getSizeOfLine(lastLine);
        this.cursor = new Pos( lastLine, lastCol);
        revalidateViewPort();
    }

}