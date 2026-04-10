package nikolai.lsp;

import nikolai.FileScanner;
import nikolai.Utils;
import org.jspecify.annotations.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public enum LSP {
    INSTANCE;

    private final File workspace;
    private TsLspClient client;
    LSP() {
        Process tsProcess;
        try {
           this.workspace = FileScanner.getPath().toFile();
            tsProcess = new ProcessBuilder(
                    Utils.readFile("lsp_args.txt"))
                    .directory(workspace)
                    .redirectErrorStream(false)
                    .start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // stderr in eigenem Thread konsumieren, sonst blockiert der Prozess!
        Thread stderrThread = getThread(tsProcess);
        stderrThread.start();
        client = new TsLspClient(tsProcess.getInputStream(), tsProcess.getOutputStream());
    }

    private static @NonNull Thread getThread(Process tsProcess) {
        Process finalTsProcess = tsProcess;
        Thread stderrThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(finalTsProcess.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.err.println("[tsserver stderr] " + line);
                }
            } catch (Exception e) {
                // ignorieren
            }
        });
        stderrThread.setDaemon(true);
        return stderrThread;
    }

    public TsLspClient.GotoDefInfo getDefinition(int line, int col, String fileName) throws Exception {
        String absolutePath =  workspace.getAbsolutePath();
        List<TsLspClient.GotoDefInfo> defs = client.gotoDef(
                workspace.getAbsolutePath(),
                fileName.substring(absolutePath.length()),
                line, col
        );

        if (defs.isEmpty()) {
           return null;
        } else {
            return defs.getFirst();
        }
    }

    /**
     * Liefert eine Liste von Autocompletion-Vorschlägen für die gegebene Position.
     */
    public List<String> getCompletions(int line, int col,String text,  String fileName) throws Exception {
        Path workspacePath = workspace.toPath();
        Path relativePath = workspacePath.relativize(Paths.get(fileName));

        List<String> completions = client.getCompletions(

                workspace.getAbsolutePath(),
                relativePath.toString(),
                text,
                line,
                col
        );

        return completions != null ? completions : new ArrayList<>();
    }
}
