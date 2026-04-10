package nikolai.piecetable;

import nikolai.Selection;

import java.util.ArrayList;
import java.util.List;

/**
 * A Piece Table implementation for a text editor.
 *
 * The piece table maintains two buffers:
 *   - original: the initial text (immutable after construction)
 *   - add:      all inserted text is appended here (append-only)
 *
 * The document is represented as an ordered list of "pieces", each pointing
 * into one of the two buffers with a start offset and length.
 *
 * Undo/Redo is implemented by snapshotting the piece list and the add-buffer
 * length before each mutating operation.
 */
public class PieceTable {

    // ── inner types ──────────────────────────────────────────────────

    private enum Source { ORIGINAL, ADD }

    private static class Piece {
        final Source source;
        final int start;   // offset into the buffer
        final int length;

        Piece(Source source, int start, int length) {
            this.source = source;
            this.start  = start;
            this.length = length;
        }

        /** Return the text this piece represents. */
        String text(PieceTable table) {
            String buf = source == Source.ORIGINAL ? table.original : table.add.toString();
            return buf.substring(start, start + length);
        }

        @Override
        public String toString() {
            return "Piece(" + source + ", " + start + ", " + length + ")";
        }
    }

    /** Snapshot for undo/redo. */
    private static class Snapshot {
        final List<Piece> pieces;
        final int addLength;     // length of add-buffer at snapshot time
        final Pos cursorAfter;   // cursor position after the operation

        Snapshot(List<Piece> pieces, int addLength, Pos cursorAfter) {
            this.pieces      = pieces;
            this.addLength   = addLength;
            this.cursorAfter = cursorAfter;
        }
    }

    // ── fields ───────────────────────────────────────────────────────

    private final String original;
    private final StringBuilder add;
    private List<Piece> pieces;

    private final List<Snapshot> undoStack = new ArrayList<>();
    private final List<Snapshot> redoStack = new ArrayList<>();

    // ── constructors ─────────────────────────────────────────────────

    public PieceTable() {
        this("");
    }

    public PieceTable(String initialText) {
        this.original = initialText;
        this.add      = new StringBuilder();
        this.pieces   = new ArrayList<>();
        if (!initialText.isEmpty()) {
            pieces.add(new Piece(Source.ORIGINAL, 0, initialText.length()));
        }
    }

    // ── public API ───────────────────────────────────────────────────

    /**
     * Insert {@code text} at the given position.
     * Returns the cursor position after the inserted text.
     */
    public Pos insertText(String text, Pos pos) {
        if (text.isEmpty()) return pos;
        saveSnapshot(pos);

        int offset = posToOffset(pos);
        int addStart = add.length();
        add.append(text);
        Piece newPiece = new Piece(Source.ADD, addStart, text.length());
        splitAndInsert(offset, newPiece);

        return offsetToPos(offset + text.length());
    }

    /**
     * Delete text between {@code from} (inclusive) and {@code to} (exclusive).
     * Returns the position {@code from} (the new cursor position).
     */
    public Pos deleteText(Pos from, Pos to) {
        if (from.equals(to)) return from;
        // Ensure from < to
        if (to.isBefore(from)) { Pos tmp = from; from = to; to = tmp; }

        saveSnapshot(from);

        int startOff = posToOffset(from);
        int endOff   = posToOffset(to);
        removeRange(startOff, endOff);

        return from;
    }

    /**
     * Replace text between {@code from} and {@code to} with {@code text}.
     * Returns the cursor position after the replacement text.
     */
    public Pos replace(String text, Pos from, Pos to) {
        if (to.isBefore(from)) { Pos tmp = from; from = to; to = tmp; }
        saveSnapshot(from);

        int startOff = posToOffset(from);
        int endOff   = posToOffset(to);
        removeRange(startOff, endOff);

        if (!text.isEmpty()) {
            int addStart = add.length();
            add.append(text);
            Piece newPiece = new Piece(Source.ADD, addStart, text.length());
            splitAndInsert(startOff, newPiece);
        }

        return offsetToPos(startOff + text.length());
    }

    /**
     * Insert a newline character at the given position.
     * Returns the position at the beginning of the new line.
     */
    public Pos addNewline(Pos pos) {
        return insertText("\n", pos);
    }

    /**
     * Delete one character before the cursor (backspace behaviour).
     * Returns the new cursor position after deletion.
     */
    public Pos backspace(Pos cursor) {
        if (cursor.line() == 0 && cursor.col() == 0) return cursor;
        int offset = posToOffset(cursor);
        if (offset == 0) return cursor;

        Pos from = offsetToPos(offset - 1);
        return deleteText(from, cursor);
    }

    /**
     * Return the entire document text.
     */
    public String getText() {
        StringBuilder sb = new StringBuilder();
        for (Piece p : pieces) {
            sb.append(p.text(this));
        }
        return sb.toString();
    }

    /**
     * Return all lines of the document (split by '\n').
     * A trailing newline produces an extra empty line.
     */
    public List<String> getLines() {
        return splitLines(getText());
    }

    /**
     * Return lines from position {@code from} to position {@code to}.
     */
    public List<String> getLines(Pos from, Pos to) {
        if (to.isBefore(from)) { Pos tmp = from; from = to; to = tmp; }
        int startOff = posToOffset(from);
        int endOff   = posToOffset(to);
        String full  = getText();
        String sub   = full.substring(startOff, endOff);
        return splitLines(sub);
    }

    /**
     * Return the total number of lines in the document.
     */
    public int getTotalLines() {
        return getLines().size();
    }

    /**
     * Return the length (number of characters) of the given line (0-based).
     * The returned size does NOT include the trailing newline.
     */
    public int getSizeOfLine(int line) {
        List<String> lines = getLines();
        if (line < 0 || line >= lines.size()) {
            throw new IndexOutOfBoundsException("Line " + line + " out of range [0, " + lines.size() + ")");
        }
        return lines.get(line).length();
    }

    /**
     * Undo the last operation. Returns the cursor position after undo,
     * or {@code null} if there is nothing to undo.
     */
    public Pos undo() {
        if (undoStack.isEmpty()) return null;

        // Save current state to redo stack
        Snapshot current = undoStack.remove(undoStack.size() - 1);
        redoStack.add(new Snapshot(copyPieces(), add.length(), current.cursorAfter));

        // Restore previous state
        this.pieces = current.pieces;
        // Trim the add-buffer back (safe because undo restores older state)
        add.setLength(current.addLength);

        return current.cursorAfter;
    }

    /**
     * Redo the last undone operation. Returns the cursor position after redo,
     * or {@code null} if there is nothing to redo.
     */
    public Pos redo() {
        if (redoStack.isEmpty()) return null;

        Snapshot redo = redoStack.remove(redoStack.size() - 1);
        undoStack.add(new Snapshot(copyPieces(), add.length(), redo.cursorAfter));

        this.pieces = redo.pieces;
        add.setLength(redo.addLength);

        return redo.cursorAfter;
    }

    // ── private helpers ──────────────────────────────────────────────

    /** Save the current piece-list and add-buffer state for undo. */
    private void saveSnapshot(Pos cursorBefore) {
        undoStack.add(new Snapshot(copyPieces(), add.length(), cursorBefore));
        redoStack.clear();   // new edit invalidates redo history
    }

    /** Deep-copy the pieces list. */
    private List<Piece> copyPieces() {
        return new ArrayList<>(pieces);  // Piece is immutable, shallow copy suffices
    }

    /**
     * Convert a {@link Pos} (line, col) to a linear character offset.
     */
    private int posToOffset(Pos pos) {
        String text = getText();
        int line = 0;
        int offset = 0;
        while (line < pos.line() && offset < text.length()) {
            if (text.charAt(offset) == '\n') {
                line++;
            }
            offset++;
        }
        offset += pos.col();
        return Math.min(offset, text.length());
    }

    /**
     * Convert a linear character offset to a {@link Pos} (line, col).
     */
    private Pos offsetToPos(int offset) {
        String text = getText();
        offset = Math.min(offset, text.length());
        int line = 0;
        int col  = 0;
        for (int i = 0; i < offset; i++) {
            if (text.charAt(i) == '\n') {
                line++;
                col = 0;
            } else {
                col++;
            }
        }
        return new Pos(line, col);
    }

    /**
     * Split the piece at the given offset and insert {@code newPiece} there.
     */
    private void splitAndInsert(int offset, Piece newPiece) {
        List<Piece> result = new ArrayList<>();
        int current = 0;
        boolean inserted = false;

        for (Piece p : pieces) {
            int pEnd = current + p.length;

            if (!inserted && offset <= pEnd) {
                if (offset == current) {
                    // Insert before this piece
                    result.add(newPiece);
                    result.add(p);
                    inserted = true;
                } else if (offset == pEnd) {
                    // Insert after this piece
                    result.add(p);
                    result.add(newPiece);
                    inserted = true;
                } else {
                    // Split this piece
                    int splitAt = offset - current;
                    result.add(new Piece(p.source, p.start, splitAt));
                    result.add(newPiece);
                    result.add(new Piece(p.source, p.start + splitAt, p.length - splitAt));
                    inserted = true;
                }
            } else {
                result.add(p);
            }

            current = pEnd;
        }

        if (!inserted) {
            result.add(newPiece);
        }

        this.pieces = result;
    }

    /**
     * Remove characters in the range [startOff, endOff) from the piece list.
     */
    private void removeRange(int startOff, int endOff) {
        List<Piece> result = new ArrayList<>();
        int current = 0;

        for (Piece p : pieces) {
            int pEnd = current + p.length;

            if (pEnd <= startOff || current >= endOff) {
                // Entirely outside the deletion range – keep as-is
                result.add(p);
            } else {
                // Some or all of this piece overlaps the deletion range
                if (current < startOff) {
                    // Keep the part before the deletion
                    int keep = startOff - current;
                    result.add(new Piece(p.source, p.start, keep));
                }
                if (pEnd > endOff) {
                    // Keep the part after the deletion
                    int skip = endOff - current;
                    result.add(new Piece(p.source, p.start + skip, p.length - skip));
                }
            }

            current = pEnd;
        }

        this.pieces = result;
    }

    /**
     * Split a string into lines. We use a manual split so that a trailing
     * newline correctly produces a final empty string (matching editor behaviour).
     */
    private static List<String> splitLines(String text) {
        List<String> lines = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines.add(text.substring(start, i));
                start = i + 1;
            }
        }
        lines.add(text.substring(start));  // last line (or empty doc → [""])
        return lines;
    }

    /**
     * Return the selected text for each selection in the list.
     * Each selection is resolved independently; the returned list has
     * the same size and order as the input list.
     */
    public List<String> getTextFromSelections(List<Selection> selections) {
        String full = getText();
        List<String> result = new ArrayList<>();
        for (Selection sel : selections) {
            Pos from = sel.start();
            Pos to   = sel.end();
            if (to.isBefore(from)) { Pos tmp = from; from = to; to = tmp; }
            int startOff = posToOffset(from);
            int endOff   = posToOffset(to);
            result.add(full.substring(startOff, endOff));
        }
        return result;
    }


    /**
     * Return the text content of the given line (0-based), without the trailing newline.
     */
    public String getTextAtLine(int line) {
        List<String> lines = getLines();
        if (line < 0 || line >= lines.size()) {
            throw new IndexOutOfBoundsException("Line " + line + " out of range [0, " + lines.size() + ")");
        }
        return lines.get(line);
    }
}
