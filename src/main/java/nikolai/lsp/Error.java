package nikolai.lsp;

import org.eclipse.lsp4j.DiagnosticSeverity;

public record Error(String file, int line, int col, String message, DiagnosticSeverity severity) {

    @Override
    public String toString() {
        return severity + " " + file + ":" + line + ":" + col + " " + message;
    }
}