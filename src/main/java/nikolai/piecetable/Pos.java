package nikolai.piecetable;

public record Pos(int line, int col) {
    /**
     * Returns true if this position is before the other position.
     */
    public boolean isBefore(Pos other) {
        return line < other.line || (line == other.line && col < other.col);
    }

    /**
     * Returns true if this position is after the other position.
     */
    public boolean isAfter(Pos other) {
        return other.isBefore(this);
    }
}
